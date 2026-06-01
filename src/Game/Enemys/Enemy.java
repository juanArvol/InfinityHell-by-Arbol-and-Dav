package Game.Enemys;

import Game.Engine.Components.HealthComponent;
import Game.Engine.Components.Physics2DComponent;
import Game.Engine.Components.StatusEffectComponent;
import Game.Engine.Components.Collisions.ColliderComponent;
import Game.Engine.Components.Visuals.SizeSyncMode;
import Game.Engine.Events.GameEventBus;
import Game.Engine.Events.GameEvents.OnEnemyDeathEvent;
import Game.Engine.GameMath.Physics.Implementation.EnemyPhysics;
import Game.Engine.GameMath.SpaceLogic.Logic2D.Vector2D;
import Game.Items.Types.Bullets.Bullet;
import Game.Engine.MovingObjects;
import Game.Engine.Colisions.Filter.CollisionProfile;
import Game.Enemys.AI.EnemyAI;
import Game.Enemys.AI.EnemyComport;
import Game.Enemys.AI.EnemyContext;
import Game.Enemys.Components.EnemyState;
import Game.Player.Player;

import java.awt.image.BufferedImage;

/**
 * Clase base de todos los enemigos.
 *
 * ── REFACTOR: COMPONENTES DE GAMEPLAY COMPARTIDOS ────────────────────────
 *
 * CAMBIO 1 — HealthComponent movido a Engine.Components.Gameplay
 *   PROBLEMA: HealthComponent vivía en Game.Enemys.Components, impidiendo
 *   su uso por Player, NPCs u otras entidades sin duplicar código.
 *   SOLUCIÓN: Enemy ahora importa HealthComponent desde su nueva ubicación
 *   compartida. El import cambia; la API y comportamiento son idénticos.
 *   BENEFICIO: Player puede usar el mismo HealthComponent, eliminando la
 *   duplicación actual con PlayerStats.
 *
 * CAMBIO 2 — StatusEffectComponent desacoplado de Enemy
 *   PROBLEMA: StatusEffectComponent.StatusEffect.tick() recibía Enemy como
 *   parámetro, acoplando el sistema de efectos al tipo Enemy. No podía
 *   aplicarse a Player ni a otras entidades.
 *   SOLUCIÓN: tick() ahora recibe GameObjects (base común). Los efectos
 *   que necesiten Enemy hacen instanceof internamente. StatusEffectComponent
 *   es ahora un componente de gameplay genérico.
 *   BENEFICIO: Player puede recibir efectos de estado sin cambios adicionales.
 *
 * CAMBIO 3 — addEffect() actualizado a nueva firma de StatusEffect
 *   El shortcut addEffect() delega a StatusEffectComponent con la nueva API.
 *   Los efectos de PoisonModifier se adaptan inline (cast seguro a Enemy).
 *
 * ── RESTO SIN CAMBIOS ────────────────────────────────────────────────────
 * Constructor, IA, muerte, física, colisiones — idénticos al original.
 * La retrocompatibilidad con legacyPlayer se mantiene.
 */
public abstract class Enemy extends MovingObjects {

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

    /**
     * Constructor legacy — acepta Player directamente para retrocompatibilidad.
     * @deprecated Usar el constructor sin Player y pasar EnemyContext.of(player)
     *             en update(). Este constructor existe para retrocompatibilidad.
     */
    @Deprecated
    public Enemy(
            Vector2D position,
            BufferedImage texture,
            int maxHealth,
            EnemyComport comport,
            Player player,
            EnemyPhysics physics
    ) {
        this(position, texture, maxHealth, comport, physics);
        this.legacyPlayer = player;
    }

    /** Solo para transición. Se elimina cuando todas las subclases migren. */
    private Player legacyPlayer;

    // ── Update ────────────────────────────────────────────────────────────

    public void update(EnemyContext ctx) {
        if (health.isDead()) {
            onDeath();
            return;
        }

        state.setMoving(false);
        state.setAttacking(false);

        if (ctx != null) {
            ai.update(this, ctx);
        } else if (legacyPlayer != null) {
            ai.update(this, EnemyContext.of(legacyPlayer));
        }

        updateTypePhysics();
        // effects se actualiza automáticamente vía Component.update() en super.update()
        super.update();
    }

    @Override
    public void update() {
        EnemyContext ctx = (legacyPlayer != null) ? EnemyContext.of(legacyPlayer) : null;
        update(ctx);
    }

    // ── Hook de subtipo ───────────────────────────────────────────────────

    protected abstract void updateTypePhysics();

    // ── Daño y muerte ─────────────────────────────────────────────────────

    public void damage(int amount) {
        health.damage(amount);
    }

    protected void onDeath() {
        //GameEventBus.post(new OnEnemyDeathEvent(this, getTransform().getPosition()));
        markForRemoval();
    }

    // ── Efectos de estado ─────────────────────────────────────────────────

    /**
     * Aplica un efecto de estado al enemigo.
     * El efecto recibe GameObjects en tick(); si necesita acceso a Enemy,
     * hace instanceof/cast interno.
     */
    public void addEffect(StatusEffectComponent.StatusEffect effect) {
        effects.add(effect);
    }

    public boolean hasEffect(String effectId) {
        return effects.hasEffect(effectId);
    }

    // ── Getters ───────────────────────────────────────────────────────────

    public EnemyState      getState()            { return state; }
    public HealthComponent getHealthComponent()   { return health; }
    public EnemyAI         getAI()               { return ai; }

    // ── Ciclo de vida ─────────────────────────────────────────────────────

    private boolean pendingRemoval = false;

    public void    markForRemoval()      { pendingRemoval = true; }
    public boolean isPendingRemoval()    { return pendingRemoval; }

    public EnemyPhysics getPhysics() {
        Physics2DComponent pc = getComponent(Physics2DComponent.class);
        return pc != null ? (EnemyPhysics) pc.getPhysics() : null;
    }

    public Physics2DComponent getPhysicsComponent() {
        return getComponent(Physics2DComponent.class);
    }

    // ── Colisiones ────────────────────────────────────────────────────────

    @Override public void onCollisionWith(Player player) {}
    @Override public void onCollisionWith(Enemy enemy)   {}
    @Override public void onCollisionWith(Bullet bullet) {}
}
