package Game.Player;

import Game.Engine.Entity.Components.HealthComponent;
import Game.Gameplay.UI.HealthView;

/**
 * Estadísticas específicas del jugador.
 *
 * ── REFACTOR: SEPARACIÓN DEFINITIVA DE HealthComponent ───────────────────
 *
 * PROBLEMA ORIGINAL:
 *   PlayerStats contenía dos responsabilidades mezcladas:
 *
 *   (a) Gestión de vida: life, lifeMax, receiveDamage(), heal(), isDead(),
 *       invulnerabilityFrames, update(). Exactamente la misma responsabilidad
 *       que HealthComponent — dos sistemas resolviendo el mismo problema.
 *
 *   (b) Espacio para stats de gameplay (velocidad, fuerza...) que aún no
 *       existían pero cuyo lugar natural era esta clase.
 *
 *   Mantener la lógica de vida en PlayerStats significaba:
 *   - Duplicación conceptual con HealthComponent.
 *   - Player con dos fuentes de verdad para su estado de salud.
 *   - Imposibilidad de que sistemas genéricos (UI de HP, sistemas de daño)
 *     usaran HealthComponent para el Player sin refactorizar todo.
 *
 * SOLUCIÓN:
 *   Eliminar completamente la lógica de vida de PlayerStats.
 *   La salud del Player la gestiona ahora HealthComponent, que Player
 *   añade en su constructor (addComponent(new HealthComponent(maxHp))).
 *   Los shortcuts de Entity (damage, heal, isDead) lo exponen limpiamente.
 *
 *   PlayerStats queda como sistema complementario de atributos del jugador:
 *   modificadores, multiplicadores, configuración específica del Player.
 *   Complementa a HealthComponent; no compite con él.
 *
 * ── RESPONSABILIDAD ACTUAL ────────────────────────────────────────────────
 *
 * PlayerStats gestiona los atributos que modifican el comportamiento del
 * jugador: velocidad, daño base, multiplicadores, slots de equipamiento.
 * Son valores que los sistemas leen para calcular resultados, pero la
 * ejecución de esos resultados (aplicar daño, curar) la hace HealthComponent.
 *
 * Ejemplo de flujo:
 *   int rawDamage = 10;
 *   int finalDamage = (int)(rawDamage * stats.getDamageMultiplier());
 *   player.damage(finalDamage);  // → HealthComponent.damage()
 *
 * ── INVULNERABILIDAD ─────────────────────────────────────────────────────
 *
 * Los frames de invulnerabilidad post-daño son responsabilidad de PlayerStats
 * porque son específicos del jugador, no un concepto genérico de salud.
 * Un enemigo no tiene invulnerabilidad post-golpe. Un Boss podría tenerla
 * con reglas distintas. Por tanto no pertenece a HealthComponent.
 *
 * PlayerStats expone isInvulnerable() para que los sistemas de daño lo
 * consulten antes de llamar a player.damage():
 *
 *   if (!player.getPlayerStats().isInvulnerable()) {
 *       player.damage(amount);
 *       player.getPlayerStats().triggerInvulnerability();
 *   }
 *
 * ── HealthView (RESTAURADO) ───────────────────────────────────────────────
 *
 * PlayerStats implementa HealthView para que LifeHUD consulte la vida del
 * jugador a través de PlayerStats, manteniendo la cadena:
 *
 *   LifeHUD → PlayerStats → HealthComponent
 *
 * PlayerStats actúa como fachada de solo lectura del HealthComponent.
 * No muta la salud — solo la expone al sistema de UI.
 *
 * Esta cadena es coherente con la arquitectura: PlayerStats es el punto de
 * acceso a los datos del jugador para sistemas externos (UI, buff system,
 * persistence). LifeHUD no debería saber nada de HealthComponent.
 *
 * Para futuros HUDs (EnemyLifeHUD, BossLifeHUD), sus respectivos portadores
 * (Enemy, Boss) implementarán HealthView directamente o expondrán un getter
 * que retorne HealthView — el HUD siempre depende de la interfaz, no del tipo.
 *
 * Véase: Game.UI.HealthView
 */
public class PlayerStats implements HealthView {

    // ── HealthComponent (fachada de solo lectura) ─────────────────────────
    //
    // Se inyecta desde Player.init() después de que el HealthComponent
    // es añadido como componente. PlayerStats NO muta la salud — solo
    // la expone a través de HealthView.

    private HealthComponent healthComponent;

    /**
     * Vincula el HealthComponent del Player a esta fachada.
     * Llamar desde Player.init() una vez que el componente existe:
     *
     *   stats.bindHealth(getHealth());
     *
     * Este método no forma parte del contrato HealthView — es solo la
     * inicialización interna de la fachada.
     */
    public void bindHealth(HealthComponent health) {
        this.healthComponent = health;
    }

    // ── HealthView ────────────────────────────────────────────────────────

    /**
     * HP actual del jugador.
     * Delega en HealthComponent — fuente de verdad de la salud.
     *
     * CORRECCIÓN F-01: la versión anterior calculaba (int)(maxHP * healthPercent),
     * que es un roundtrip double innecesario. HealthComponent.getCurrent() ya
     * devuelve el valor entero exacto, sin pérdida de precisión.
     */
    @Override
    public int getLife() {
        if (healthComponent == null) return 0;
        return healthComponent.getCurrent();
    }

    /**
     * HP máxima del jugador.
     * Delega en HealthComponent — fuente de verdad de la salud.
     */
    @Override
    public int getLifeMax() {
        if (healthComponent == null) return 1; // evitar división por cero en HUDs
        return healthComponent.getMaxHP();
    }

    // ── Invulnerabilidad post-daño (específica del Player) ────────────────

    private static final int INV_FRAMES_ON_HIT = 20;
    private int invulnerabilityFrames = 0;

    public void update() {
        if (invulnerabilityFrames > 0) {
            invulnerabilityFrames--;
        }
    }

    /**
     * Activa los frames de invulnerabilidad tras recibir un golpe.
     * Llamar desde el sistema de daño después de aplicar el daño real.
     */
    public void triggerInvulnerability() {
        invulnerabilityFrames = INV_FRAMES_ON_HIT;
    }

    /** True si el jugador está en frames de invulnerabilidad post-golpe. */
    public boolean isInvulnerable() {
        return invulnerabilityFrames > 0;
    }

    // ── Atributos base del jugador ────────────────────────────────────────
    //
    // Estos valores son leídos por sistemas (combate, movimiento, UI) para
    // calcular los resultados finales. La ejecución ocurre en los componentes
    // correspondientes (HealthComponent, PlayerPhysics, etc.)

    private float speedMultiplier  = 1.0f;
    private float damageMultiplier = 1.0f;
    private int   maxInventorySlots = 20;

    public float getSpeedMultiplier()   { return speedMultiplier; }
    public float getDamageMultiplier()  { return damageMultiplier; }
    public int   getMaxInventorySlots() { return maxInventorySlots; }

    public void setSpeedMultiplier(float v)  { speedMultiplier  = Math.max(0f, v); }
    public void setDamageMultiplier(float v) { damageMultiplier = Math.max(0f, v); }

    // ── Modificadores temporales ──────────────────────────────────────────
    //
    // Futuros modificadores de stats (buffs de equipamiento, pociones, etc.)
    // se añaden aquí, no en HealthComponent. PlayerStats es el agregador
    // de atributos; HealthComponent es el ejecutor de salud.
    //
    // Ejemplo futuro:
    //   public void applySpeedBuff(float amount, int durationFrames) { ... }
    //   public void applyDamageBuff(float amount, int durationFrames) { ... }
}

