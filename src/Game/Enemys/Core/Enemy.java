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
import Game.World.WorldObjects.WorldObjectsContainer;
import Sprites.Core.SpriteHandle;

/**
 * Núcleo único de todos los enemigos del juego.
 *
 * ── HRFC-005 ─────────────────────────────────────────────────────────────
 *
 * Enemy es el único esqueleto. Zombies, voladores, Bosses, Sans —
 * todos parten exactamente de aquí. La diferencia entre ellos surge
 * exclusivamente de cómo sus Assemblers componen los controladores.
 *
 * Enemy únicamente declara que existen los conceptos de:
 *   - Vida       (HealthComponent del engine)
 *   - Estado     (EnemyState — flags de física y animación)
 *   - Variables  (EnemyVariables — velocidad, daño, rangos, etc.)
 *   - IA         (EnemyAIController — qué decide hacer)
 *   - Movimiento (EnemyMovementController — cómo se mueve)
 *   - Ataques    (EnemyAttackController — qué puede atacar)
 *   - Fases      (EnemyPhaseController — transiciones de estado)
 *   - Componentes(EnemyComponentRegistry — capacidades opcionales)
 *   - Efectos    (StatusEffectComponent del engine — veneno, hielo, etc.)
 *   - Física     (Physics2DComponent del engine)
 *
 * Enemy NUNCA implementa comportamientos específicos.
 * Enemy NUNCA distingue entre tipo de enemigo.
 * Enemy NUNCA sabe si es un Boss.
 *
 * ── Jerarquía ────────────────────────────────────────────────────────────
 *   GameObjects → Entity → MovingObjects → Enemy
 *
 * La jerarquía termina aquí. No existe GroundEnemy, FlyingEnemy ni BossEnemy
 * como subclases. La especialización surge por composición de controladores.
 *
 * ── Ciclo de update() ────────────────────────────────────────────────────
 *   1. Reset de flags volátiles (moving, attacking).
 *   2. PhaseController evalúa transiciones y actualiza la fase activa.
 *   3. AIController decide la acción del frame.
 *   4. MovementController aplica la estrategia de movimiento.
 *   5. AttackController actualiza patrones y dispara los que estén listos.
 *   6. EnemyComponentRegistry actualiza todos los componentes opcionales.
 *   7. super.update() — actualiza los Components del engine (health, effects...).
 */
public final class Enemy extends MovingObjects implements WorldObjectsContainer.Destroyable {

    // ── Controladores del framework ───────────────────────────────────────
    private final EnemyAIController         aiController;
    private final EnemyMovementController   movementController;
    private final EnemyAttackController     attackController;
    private final EnemyPhaseController      phaseController;
    private final EnemyComponentRegistry    componentRegistry;

    // ── Estado y variables ────────────────────────────────────────────────
    private final EnemyState      state;
    private final EnemyVariables  variables;

    // ── Components del engine ─────────────────────────────────────────────
    private final HealthComponent       health;
    private final StatusEffectComponent effects;

    // ── Ciclo de vida ─────────────────────────────────────────────────────
    private boolean pendingRemoval = false;

    // ── Constructor ───────────────────────────────────────────────────────

    /**
     * Constructor base de Enemy. Llamado exclusivamente por los Assemblers
     * a través de EnemyAssembler.assemble().
     *
     * @param position  posición inicial en el mundo.
     * @param handle    sprite del enemigo.
     * @param maxHealth vida máxima.
     * @param physics   física del engine ya configurada por la definición.
     */
    public Enemy(Vector2D position, SpriteHandle handle, int maxHealth,
                 Game.Enemys.EnemyPhysics physics) {
        super(position, handle, physics, SizeSyncMode.NONE);

        // Controladores del framework — vacíos; los Assemblers los configuran
        this.aiController       = new EnemyAIController(null);
        this.movementController = new EnemyMovementController();
        this.attackController   = new EnemyAttackController();
        this.phaseController    = new EnemyPhaseController();
        this.componentRegistry  = new EnemyComponentRegistry();

        // Estado y variables
        this.state     = new EnemyState();
        this.variables = new EnemyVariables();

        // Components del engine
        this.health  = new HealthComponent(maxHealth);
        this.effects = new StatusEffectComponent();

        addComponent(health);
        addComponent(effects);

        // Collider por defecto — los Assemblers pueden redefinir el tamaño
        ColliderComponent collider = getComponent(ColliderComponent.class);
        if (collider != null) {
            collider.setProfile(CollisionProfile.ENEMY);
            collider.setSize(24, 30);
        }
    }

    // ── Update ────────────────────────────────────────────────────────────

    /**
     * Actualiza el enemy con el contexto del objetivo actual.
     * Llamado por WorldEnemyUpdater en cada frame.
     *
     * @param ctx contexto del objetivo (posición del player, etc.).
     *            Si es null, la IA y los ataques no actúan este frame.
     */
    public void update(EnemyContext ctx) {
        if (health.isDead()) {
            onDeath();
            return;
        }

        // 1. Reset de flags volátiles para el frame
        state.resetFrameFlags();

        // 2. Fases — evalúa transiciones y actualiza fase activa
        phaseController.update(this);

        // 3. IA — decide la acción del frame
        aiController.update(this, ctx);

        // 4. Movimiento — aplica la estrategia activa
        movementController.update(this, ctx);

        // 5. Ataques — actualiza cooldowns y dispara los listos
        boolean attacked = attackController.update(this, ctx);
        if (attacked) state.setAttacking(true);

        // 6. EnemyComponents opcionales
        componentRegistry.update(this);

        // 7. Engine components (health, status effects, physics, renderer, etc.)
        super.update();
    }

    /**
     * update() sin argumentos — delega a update(null).
     * WorldObjectsContainer llama este método; WorldEnemyUpdater lo
     * sobreescribe pasando el EnemyContext correcto.
     */
    @Override
    public void update() {
        update(null);
    }

    // ── Muerte ────────────────────────────────────────────────────────────

    private void onDeath() {
        GameEventBus.GLOBAL.post(new OnEnemyDeathEvent(this, getTransform().getPosition()));
        markForRemoval();
    }

    // ── Daño (delegado a HealthComponent) ────────────────────────────────

    public void damage(int amount) {
        health.damage(amount);
    }

    // ── StatusEffects (delegado a StatusEffectComponent) ─────────────────

    public void addEffect(StatusEffectComponent.StatusEffect effect) {
        effects.add(effect);
    }

    public boolean hasEffect(String effectId) {
        return effects.hasEffect(effectId);
    }

    // ── Ciclo de vida ─────────────────────────────────────────────────────

    public void markForRemoval()    { pendingRemoval = true; }
    public boolean isPendingRemoval() { return pendingRemoval; }

    @Override
    public boolean isPendingDestruction() { return pendingRemoval; }

    // ── Getters de controladores ──────────────────────────────────────────

    public EnemyAIController       getAIController()        { return aiController; }
    public EnemyMovementController getMovementController()  { return movementController; }
    public EnemyAttackController   getAttackController()    { return attackController; }
    public EnemyPhaseController    getPhaseController()     { return phaseController; }
    public EnemyComponentRegistry  getComponentRegistry()   { return componentRegistry; }

    // ── Getters de estado ─────────────────────────────────────────────────

    public EnemyState      getState()     { return state; }
    public EnemyVariables  getVariables() { return variables; }

    // ── Getters de components del engine ─────────────────────────────────

    public HealthComponent getHealthComponent() { return health; }

    /**
     * Acceso tipado a la física del engine.
     * Usado por MovementStrategy y MoveCommand.
     */
    public Game.Enemys.EnemyPhysics getPhysics() {
        Physics2DComponent pc = getComponent(Physics2DComponent.class);
        return pc != null ? (Game.Enemys.EnemyPhysics) pc.getPhysics() : null;
    }

    public Physics2DComponent getPhysicsComponent() {
        return getComponent(Physics2DComponent.class);
    }

    // ── Compatibilidad hacia atrás — mantener getState() accesible ────────
    // EnemyAction.execute(Enemy) llama enemy.getState() y enemy.getPhysics().
    // Ambos siguen funcionando con los mismos nombres.
}
