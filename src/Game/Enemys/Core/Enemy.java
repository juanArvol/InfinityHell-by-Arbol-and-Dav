package Game.Enemys.Core;

import Game.Enemys.AI.EnemyContext;
import Game.Enemys.Core.Controllers.EnemyAIController;
import Game.Enemys.Core.Controllers.EnemyAttackController;
import Game.Enemys.Core.Controllers.EnemyComponentRegistry;
import Game.Enemys.Core.Controllers.EnemyMovementController;
import Game.Enemys.Core.Controllers.EnemyPhaseController;
import Game.Enemys.Core.Events.OnEnemyDeathEvent;
import Game.Enemys.Core.Variables.EnemyVariables;
import Game.Engine.Colisions.Filter.CollisionProfile;
import Game.Engine.Components.Collisions.ColliderComponent;
import Game.Engine.Components.HealthComponent;
import Game.Engine.Components.Physics2DComponent;
import Game.Engine.Components.StatusEffectComponent;
import Game.Engine.Components.Visuals.SizeSyncMode;
import Game.Engine.Events.GameEventBus;
import Game.Engine.GameMath.SpaceLogic.Logic2D.Vector2D;
import Game.Engine.MovingObjects;
import Game.Living.Attributes.EntityAttributes;
import Game.Living.Combat.AttackSources;
import Game.Living.Flags.EntityFlags;
import Game.Living.Stats.EntityStats;
import Game.Living.Stats.RuntimeStats;
import Game.World.WorldObjects.WorldObjectsContainer;
import Sprites.Core.SpriteHandle;

/**
 * Núcleo único de todos los enemigos del juego.
 *
 * ── HRFC-007 — Living Entity Core ────────────────────────────────────────
 * Enemy ya no posee EnemyStats, EnemyFlags ni EnemyAttributes.
 * Ahora consume los tipos genéricos del Living Entity Core:
 *
 *   EntityStats      (era EnemyStats)
 *   EntityFlags      (era EnemyFlags)
 *   EntityAttributes (era EnemyAttributes)
 *   AttackSources    → Game.Living.Combat
 *   RuntimeStats     → Game.Living.Stats
 *
 * Enemy sigue siendo un esqueleto completamente agnóstico. No instancia
 * ningún módulo — todos llegan ya construidos desde EnemyAssembler.
 *
 * ── HRFC-009 — Consolidación RPG ─────────────────────────────────────────
 * - hasEffect(String) eliminado. Consulta tipada: hasEffect(Class<T>).
 * - removeEffects(Class<T>) para limpieza tipada de efectos.
 * - getEffectsComponent() para acceso directo al componente completo.
 *
 * ── Módulos inyectados ────────────────────────────────────────────────────
 *   EnemyAIController         — qué decide hacer cada frame
 *   EnemyMovementController   — cómo se mueve
 *   EnemyAttackController     — qué puede atacar
 *   EnemyPhaseController      — transiciones de estado
 *   EnemyComponentRegistry    — capacidades opcionales
 *   EntityStats               — estadísticas base (BaseStats)
 *   RuntimeStats              — estadísticas efectivas (base + modificadores)
 *   EntityFlags               — capabilities + states booleanos
 *   EntityAttributes          — clasificación de dominio
 *   AttackSources             — fuentes de ataque disponibles
 *
 * ── Ciclo de update() ────────────────────────────────────────────────────
 *   1. Reset de flags volátiles de EnemyState.
 *   2. PhaseController evalúa transiciones y actualiza la fase activa.
 *   3. AIController decide la acción (solo si isAbleToMove()).
 *   4. MovementController aplica la estrategia (solo si isAbleToMove()).
 *   5. AttackController actualiza patrones y dispara (solo si isAbleToAttack()).
 *   6. ComponentRegistry actualiza todos los EnemyComponents opcionales.
 *   7. super.update() — Components del engine (health, physics, renderer…).
 */
public final class Enemy extends MovingObjects implements WorldObjectsContainer.Destroyable {

    // ── Controladores — inyectados por EnemyAssembler ─────────────────────
    private final EnemyAIController         aiController;
    private final EnemyMovementController   movementController;
    private final EnemyAttackController     attackController;
    private final EnemyPhaseController      phaseController;
    private final EnemyComponentRegistry    componentRegistry;

    // ── Módulos de estado — Living Entity Core ────────────────────────────
    private final EntityStats        stats;
    private final RuntimeStats       runtimeStats;
    private final EntityFlags        flags;
    private final EntityAttributes   attributes;
    private final AttackSources      attackSources;

    // ── Estado de animación/física (interno al engine) ────────────────────
    private final EnemyState state;

    // ── Compatibilidad — mantenido hasta migración completa ───────────────
    private final EnemyVariables variables;

    // ── Components del engine ─────────────────────────────────────────────
    private final HealthComponent       health;
    private final StatusEffectComponent effects;

    // ── Ciclo de vida ─────────────────────────────────────────────────────
    private boolean pendingRemoval = false;

    // ── Constructor — todos los módulos son inyectados ────────────────────

    /**
     * Constructor completo de Enemy.
     * Llamado exclusivamente por EnemyAssembler.assemble().
     * Enemy no instancia ningún módulo.
     */
    public Enemy(Vector2D position,
                 SpriteHandle handle,
                 int maxHealth,
                 Game.Enemys.EnemyPhysics physics,
                 EnemyAIController aiController,
                 EnemyMovementController movementController,
                 EnemyAttackController attackController,
                 EnemyPhaseController phaseController,
                 EnemyComponentRegistry componentRegistry,
                 EntityStats stats,
                 EntityFlags flags,
                 EntityAttributes attributes,
                 AttackSources attackSources) {

        super(position, handle, physics, SizeSyncMode.NONE);

        if (aiController       == null) throw new IllegalArgumentException("Enemy: aiController is required");
        if (movementController == null) throw new IllegalArgumentException("Enemy: movementController is required");
        if (attackController   == null) throw new IllegalArgumentException("Enemy: attackController is required");
        if (phaseController    == null) throw new IllegalArgumentException("Enemy: phaseController is required");
        if (componentRegistry  == null) throw new IllegalArgumentException("Enemy: componentRegistry is required");
        if (stats              == null) throw new IllegalArgumentException("Enemy: stats is required");
        if (flags              == null) throw new IllegalArgumentException("Enemy: flags is required");
        if (attributes         == null) throw new IllegalArgumentException("Enemy: attributes is required");
        if (attackSources      == null) throw new IllegalArgumentException("Enemy: attackSources is required");

        this.aiController       = aiController;
        this.movementController = movementController;
        this.attackController   = attackController;
        this.phaseController    = phaseController;
        this.componentRegistry  = componentRegistry;

        this.stats         = stats;
        this.runtimeStats  = new RuntimeStats(stats);
        this.flags         = flags;
        this.attributes    = attributes;
        this.attackSources = attackSources;

        this.state     = new EnemyState();
        this.variables = new EnemyVariables();

        this.health  = new HealthComponent(maxHealth);
        this.effects = new StatusEffectComponent();

        addComponent(health);
        addComponent(effects);

        ColliderComponent collider = getComponent(ColliderComponent.class);
        if (collider != null) {
            collider.setProfile(CollisionProfile.ENEMY);
            collider.setSize(24, 30);
        }
    }

    // ── Update ────────────────────────────────────────────────────────────

    public void update(EnemyContext ctx) {
        if (health.isDead()) {
            onDeath();
            return;
        }

        // 1. Reset de flags volátiles para el frame
        state.resetFrameFlags();

        // 2. Fases — evalúa transiciones y actualiza fase activa
        phaseController.update(this);

        // 3 + 4. IA y movimiento — solo si las capabilities y states lo permiten
        if (flags.isAbleToMove()) {
            aiController.update(this, ctx);
            movementController.update(this, ctx);
        }

        // 5. Ataques — solo si las capabilities y states lo permiten
        if (flags.isAbleToAttack()) {
            boolean attacked = attackController.update(this, ctx);
            if (attacked) state.setAttacking(true);
        }

        // 6. EnemyComponents opcionales
        componentRegistry.update(this);

        // 7. Engine components
        super.update();
    }

    @Override
    public void update() { update(null); }

    // ── Muerte ────────────────────────────────────────────────────────────

    private void onDeath() {
        GameEventBus.GLOBAL.post(new OnEnemyDeathEvent(this, getTransform().getPosition()));
        markForRemoval();
    }

    // ── Daño ──────────────────────────────────────────────────────────────

    /**
     * Aplica daño al enemy.
     * Ignorado si el enemy está en estado invencible.
     */
    public void damage(int amount) {
        if (flags.isInvincible()) return;
        health.damage(amount);
    }

    // ── StatusEffects ─────────────────────────────────────────────────────

    /**
     * Añade un efecto de estado al componente de efectos del enemy.
     *
     * @param effect efecto a añadir.
     */
    public void addEffect(StatusEffectComponent.StatusEffect effect) {
        effects.add(effect);
    }

    /**
     * True si hay algún efecto activo del tipo dado.
     * Consulta completamente tipada — sin Strings como clave lógica.
     *
     * @param type clase del tipo de efecto.
     */
    public <T extends StatusEffectComponent.StatusEffect> boolean hasEffect(Class<T> type) {
        return effects.hasEffect(type);
    }

    /**
     * Elimina todos los efectos activos del tipo dado, llamando onExpire() en cada uno.
     *
     * @param type clase del tipo de efecto.
     */
    public <T extends StatusEffectComponent.StatusEffect> void removeEffects(Class<T> type) {
        effects.removeAll(type);
    }

    /**
     * Devuelve el componente de efectos de estado para acceso directo avanzado.
     * Preferir los métodos de conveniencia addEffect/hasEffect/removeEffects
     * para el código de combate normal.
     */
    public StatusEffectComponent getEffectsComponent() {
        return effects;
    }

    // ── Ciclo de vida ─────────────────────────────────────────────────────

    public void markForRemoval()      { pendingRemoval = true; }
    public boolean isPendingRemoval() { return pendingRemoval; }

    @Override
    public boolean isPendingDestruction() { return pendingRemoval; }

    // ── Getters de controladores ──────────────────────────────────────────

    public EnemyAIController       getAIController()        { return aiController; }
    public EnemyMovementController getMovementController()  { return movementController; }
    public EnemyAttackController   getAttackController()    { return attackController; }
    public EnemyPhaseController    getPhaseController()     { return phaseController; }
    public EnemyComponentRegistry  getComponentRegistry()   { return componentRegistry; }

    // ── Getters de módulos — Living Entity Core ───────────────────────────

    /**
     * Estadísticas base de la entidad (BaseStats).
     * Usadas por Assemblers y fases para configuración inicial.
     * En combate, leer desde getRuntimeStats() para obtener valores efectivos.
     */
    public EntityStats      getStats()         { return stats; }

    /**
     * Estadísticas efectivas en tiempo de ejecución.
     * Todo el código de combate (patrones, behaviors, projectiles) debe leer de aquí.
     * Los modificadores activos (buffs, debuffs, fases) se aplican automáticamente.
     */
    public RuntimeStats     getRuntimeStats()  { return runtimeStats; }

    public EntityFlags      getFlags()         { return flags; }
    public EntityAttributes getAttributes()    { return attributes; }
    public AttackSources    getAttackSources() { return attackSources; }

    // ── Estado de animación/física ────────────────────────────────────────

    public EnemyState getState() { return state; }

    /**
     * @deprecated Usar getStats() / getFlags() / getAttributes() / getRuntimeStats().
     */
    @Deprecated
    public EnemyVariables getVariables() { return variables; }

    // ── Components del engine ─────────────────────────────────────────────

    public HealthComponent getHealthComponent() { return health; }

    public Game.Enemys.EnemyPhysics getPhysics() {
        Physics2DComponent pc = getComponent(Physics2DComponent.class);
        return pc != null ? (Game.Enemys.EnemyPhysics) pc.getPhysics() : null;
    }

    public Physics2DComponent getPhysicsComponent() {
        return getComponent(Physics2DComponent.class);
    }
}
