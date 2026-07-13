package Game.Enemys;

import Game.Enemys.AI.EnemyAI;
import Game.Enemys.AI.EnemyComport;
import Game.Enemys.AI.EnemyContext;
import Game.Enemys.Components.EnemyState;
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
import java.awt.image.BufferedImage;

/**
 * Clase base de todos los enemigos.
 *
 * MIGRACIÓN COMPLETA: eliminado constructor @Deprecated con Player y el campo
 * legacyPlayer. Todos los enemigos se construyen ahora sin referencia al Player;
 * el contexto llega vía EnemyContext en cada llamada a update(EnemyContext).
 *
 * El update() sin argumentos delega a update(null), que ejecuta la IA sin
 * contexto de jugador. En la práctica, WorldObjectsContainer llama a
 * WorldEnemyUpdater.updateAll() que sí pasa el EnemyContext correcto.
 *
 * ── JERARQUÍA ────────────────────────────────────────────────────────────
 *   GameObjects → Entity → MovingObjects → Enemy → GroundTypeEnemy / FlyingTypeEnemy
 *                                                         ↓
 *                                                  EnemyNormal / EnemyFlying
 */
public abstract class Enemy extends MovingObjects implements WorldObjectsContainer.Destroyable {

    private final EnemyAI               ai;
    private final HealthComponent       health;
    private final EnemyState            state;
    private final StatusEffectComponent effects;

    // ── Constructor ───────────────────────────────────────────────────────

    public Enemy(
            Vector2D position,
            BufferedImage texture,
            int maxHealth,
            EnemyComport comport,
            EnemyPhysics physics
    ) {
        super(position, texture, physics, SizeSyncMode.NONE);

        this.health  = new HealthComponent(maxHealth);
        this.state   = new EnemyState();
        this.ai      = new EnemyAI(comport);
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

    /**
     * Actualiza el enemigo con el contexto del objetivo actual.
     * Llamar desde WorldEnemyUpdater para garantizar que la IA siempre
     * recibe un EnemyContext válido.
     *
     * @param ctx contexto del objetivo (posición del jugador, etc.).
     *            Si es null, la IA no ejecuta acciones este frame.
     */
    public void update(EnemyContext ctx) {
        if (health.isDead()) {
            onDeath();
            return;
        }

        state.setMoving(false);
        state.setAttacking(false);

        if (ctx != null) {
            ai.update(this, ctx);
        }

        updateTypePhysics();
        super.update();  // actualiza todos los Component (HealthComponent, StatusEffectComponent, etc.)
    }

    /**
     * update() sin argumentos — delega a update(null).
     * La IA no actúa cuando no hay contexto; la física y los componentes
     * sí se actualizan normalmente.
     *
     * WorldObjectsContainer llama este método; WorldEnemyUpdater lo
     * sobreescribe pasando el EnemyContext correcto.
     */
    @Override
    public void update() {
        update(null);
    }

    // ── Hook de subtipo ───────────────────────────────────────────────────

    protected abstract void updateTypePhysics();

    // ── Daño y muerte ─────────────────────────────────────────────────────

    protected void onDeath() {
        GameEventBus.GLOBAL.post(new OnEnemyDeathEvent(this, getTransform().getPosition()));
        markForRemoval();
    }

    // ── Efectos de estado ─────────────────────────────────────────────────

    public void addEffect(StatusEffectComponent.StatusEffect effect) {
        effects.add(effect);
    }

    public boolean hasEffect(String effectId) {
        return effects.hasEffect(effectId);
    }

    // ── Getters ───────────────────────────────────────────────────────────

    public EnemyState      getState()          { return state; }
    public HealthComponent getHealthComponent() { return health; }
    public EnemyAI         getAI()             { return ai; }

    // ── Ciclo de vida ─────────────────────────────────────────────────────

    private boolean pendingRemoval = false;

    public void    markForRemoval()   { pendingRemoval = true; }
    public boolean isPendingRemoval() { return pendingRemoval; }

    @Override
    public boolean isPendingDestruction() { return pendingRemoval; }

    public EnemyPhysics getPhysics() {
        Physics2DComponent pc = getComponent(Physics2DComponent.class);
        return pc != null ? (EnemyPhysics) pc.getPhysics() : null;
    }

    public Physics2DComponent getPhysicsComponent() {
        return getComponent(Physics2DComponent.class);
    }
}
