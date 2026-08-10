package Game.Player;

import Game.Engine.Entity.Components.HealthComponent;
import Game.Engine.Entity.Flags.EntityFlags;
import Game.Engine.Entity.Stats.EntityStats;
import Game.Engine.Entity.Stats.RuntimeStats;
import Game.Gameplay.UI.HealthView;

/**
 * Fachada de dominio del Player hacia los sistemas genéricos de Entity.
 *
 * ── HRFC — Player Reengineering ───────────────────────────────────────────
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 *
 * PlayerStats es el puente específico del dominio Player hacia los sistemas
 * de estadísticas, salud y flags genéricos del Engine. No es una fuente
 * alternativa de stats — es una fachada de acceso contextual.
 *
 * Lo que gestiona PlayerStats:
 *
 *   1. HealthView (delegación pura a HealthComponent)
 *      Para que la UI (LifeHUD) consulte la vida sin conocer HealthComponent.
 *
 *   2. Invulnerabilidad post-golpe (lógica específica del Player)
 *      Los enemigos no tienen frames de invulnerabilidad post-golpe.
 *      Un Boss podría tenerlos con reglas distintas. No pertenece a
 *      HealthComponent ni a EntityFlags — es política del dominio Player.
 *
 *   3. Acceso de conveniencia a EntityStats, RuntimeStats, EntityFlags
 *      Para que sistemas externos puedan ir a PlayerStats como punto de
 *      entrada al dominio del Player sin conocer la composición interna.
 *
 * Lo que NO gestiona PlayerStats:
 *
 *   ✗ HP propio (vive en EntityStats.health() vía HealthComponent)
 *   ✗ HP máximo propio (ídem)
 *   ✗ Velocidad (vive en EntityStats.movement() + RuntimeStats)
 *   ✗ Daño (vive en EntityStats.combat() + RuntimeStats)
 *   ✗ SpeedMultiplier / DamageMultiplier como campos propios
 *     → Los modificadores de stats se aplican vía RuntimeStats.apply(contributor)
 *       usando el sistema StatContributor. No se duplican aquí.
 *   ✗ maxInventorySlots
 *     → Pertenece a la construcción del Inventory, no a las stats del Player.
 *
 * ── FLUJO DE DAÑO ─────────────────────────────────────────────────────────
 *
 *   Damage Source
 *        │
 *        ▼
 *   player.receiveDamage(amount)
 *        │
 *        ├── ¿isInvulnerable()? → sí → rechazar (PlayerStats.isInvulnerable)
 *        │
 *        ▼
 *   HealthComponent.damage(amount) → EntityStats.health().currentHp
 *        │
 *        ▼
 *   PlayerStats.triggerInvulnerability()
 *
 * ── HEALTHVIEW ────────────────────────────────────────────────────────────
 *
 *   LifeHUD → PlayerStats (HealthView) → HealthComponent → EntityStats.health()
 *
 *   getLife() y getLifeMax() son delegaciones puras — sin cálculo propio.
 *
 * ── RELACIÓN ARQUITECTÓNICA ───────────────────────────────────────────────
 *
 *   PlayerStats
 *       ├── EntityStats      (acceso de conveniencia — fuente de verdad base)
 *       ├── RuntimeStats     (acceso de conveniencia — fuente de verdad efectiva)
 *       ├── HealthComponent  (delegación para HealthView e invulnerabilidad)
 *       └── EntityFlags      (acceso de conveniencia — capabilities/states)
 */
public class PlayerStats implements HealthView {

    // ── Sistemas genéricos — inyectados por PlayerAssembler ───────────────
    //
    // PlayerStats no construye estos objetos. Los recibe ya construidos.
    // Son las fuentes de verdad canónicas del Engine.

    private EntityStats    entityStats;
    private RuntimeStats   runtimeStats;
    private EntityFlags    entityFlags;
    private HealthComponent healthComponent;

    // ── Binding ───────────────────────────────────────────────────────────

    /**
     * Vincula los sistemas genéricos del Engine a esta fachada.
     *
     * Llamar desde PlayerAssembler una vez que los sistemas están construidos.
     * Todos los parámetros son requeridos.
     *
     * @param entityStats    estadísticas base. No puede ser null.
     * @param runtimeStats   estadísticas efectivas. No puede ser null.
     * @param entityFlags    flags de capabilities/states/impairments. No puede ser null.
     * @param health         componente de salud. No puede ser null.
     */
    public void bind(EntityStats entityStats,
                     RuntimeStats runtimeStats,
                     EntityFlags entityFlags,
                     HealthComponent health) {
        if (entityStats  == null) throw new IllegalArgumentException("entityStats es requerido");
        if (runtimeStats == null) throw new IllegalArgumentException("runtimeStats es requerido");
        if (entityFlags  == null) throw new IllegalArgumentException("entityFlags es requerido");
        if (health       == null) throw new IllegalArgumentException("health es requerido");
        this.entityStats    = entityStats;
        this.runtimeStats   = runtimeStats;
        this.entityFlags    = entityFlags;
        this.healthComponent = health;
    }

    /**
     * Vincula solo el HealthComponent (para casos donde los demás sistemas
     * ya están disponibles vía Player.getStats() / getRuntimeStats() / getFlags()).
     *
     * Mantiene retrocompatibilidad con código que solo llama bindHealth().
     *
     * @param health componente de salud. No puede ser null.
     */
    public void bindHealth(HealthComponent health) {
        if (health == null) throw new IllegalArgumentException("health es requerido");
        this.healthComponent = health;
    }

    // ── HealthView — delegación pura ──────────────────────────────────────

    /**
     * HP actual del jugador.
     * Delegación pura a HealthComponent — sin cálculo propio.
     */
    @Override
    public int getLife() {
        if (healthComponent == null) return 0;
        return healthComponent.getCurrent();
    }

    /**
     * HP máximo del jugador.
     * Delegación pura a HealthComponent — sin cálculo propio.
     * Retorna 1 si no está vinculado para evitar división por cero en HUDs.
     */
    @Override
    public int getLifeMax() {
        if (healthComponent == null) return 1;
        return healthComponent.getMaxHP();
    }

    // ── Invulnerabilidad post-daño (política específica del Player) ────────

    private static final int INV_FRAMES_ON_HIT = 20;
    private int invulnerabilityFrames = 0;

    /**
     * Actualiza el contador de invulnerabilidad.
     * Llamar una vez por frame desde Player.update(), después de super.update().
     */
    public void update() {
        if (invulnerabilityFrames > 0) {
            invulnerabilityFrames--;
        }
    }

    /**
     * Activa los frames de invulnerabilidad tras recibir un golpe.
     * Llamar desde player.receiveDamage() después de aplicar el daño.
     */
    public void triggerInvulnerability() {
        invulnerabilityFrames = INV_FRAMES_ON_HIT;
    }

    /**
     * True si el Player está en frames de invulnerabilidad post-golpe.
     */
    public boolean isInvulnerable() {
        return invulnerabilityFrames > 0;
    }

    // ── Acceso de conveniencia a sistemas genéricos ───────────────────────
    //
    // Estos métodos permiten que sistemas externos usen PlayerStats como
    // punto de entrada al dominio del Player. No duplican datos — delegan.
    //
    // Para stats de gameplay: preferir getEntityStats() / getRuntimeStats()
    // sobre cualquier campo local. No existen campos locales de stats.

    /**
     * Estadísticas base del Player (fuente de verdad permanente).
     * Null si bind() no fue llamado.
     */
    public EntityStats getEntityStats() { return entityStats; }

    /**
     * Estadísticas efectivas con modificadores activos (fuente de verdad runtime).
     * Null si bind() no fue llamado.
     */
    public RuntimeStats getRuntimeStats() { return runtimeStats; }

    /**
     * Flags de capabilities, states e impairments del Player.
     * Null si bind() no fue llamado.
     */
    public EntityFlags getEntityFlags() { return entityFlags; }

    /**
     * Componente de salud. Null si bindHealth() / bind() no fue llamado.
     */
    public HealthComponent getHealthComponent() { return healthComponent; }
}
