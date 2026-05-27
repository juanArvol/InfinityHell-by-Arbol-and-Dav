package Game.Enemys;

import Game.Bullets.Bullet;
import Game.Engine.MovingObjects;
import Game.Engine.Components.PhysicsComponent;
import Game.Engine.Components.Collisions.ColliderComponent;
import Game.Engine.Components.Visuals.SizeSyncMode;
import Game.Engine.Filter.CollisionProfile;
import Game.Enemys.AI.EnemyAI;
import Game.Enemys.AI.EnemyComport;
import Game.Enemys.AI.EnemyContext;
import Game.Enemys.Components.EnemyState;
import Game.Enemys.Components.HealthComponent;
import Game.Enemys.Components.StatusEffectComponent;
import Game.Events.GameEventBus;
import Game.Events.GameEvents.OnEnemyDeathEvent;
import Game.Fisics.EnemyPhysics;
import Game.Player.Player;
import Game.Weapons.Modifiers.PoisonModifier;
import GameMath.Vector2D;

import java.awt.image.BufferedImage;

/**
 * Clase base de todos los enemigos.
 *
 * ── CAMBIOS VS. ORIGINAL ─────────────────────────────────────────────────
 *
 * 1. SIN Player hardcodeado en el constructor.
 *    Player se pasa como EnemyContext en update() desde el exterior (EnemySpawner
 *    o World), no se almacena como campo. Esto desacopla Enemy de Player.
 *
 * 2. EnemyContext como parámetro de update().
 *    update(EnemyContext ctx) en lugar de asumir el Player como field privado.
 *    Para compatibilidad con el sistema actual, el World puede llamar
 *    enemy.update(EnemyContext.of(player)) en su ciclo.
 *
 * 3. StatusEffectComponent añadido por defecto.
 *    Permite que PoisonModifier, FreezeModifier, etc. apliquen efectos
 *    sin modificar Enemy ni sus subclases.
 *
 * 4. onDeath() dispara OnEnemyDeathEvent en el bus.
 *    Audio, FX, loot drop, spawners de oleadas — todos escuchan el evento.
 *    Ya no hay System.out.println.
 *
 * 5. addEffect() como shortcut limpio para StatusEffectComponent.
 *
 * 6. getPhysics() como shortcut de getPhysicsComponent().getPhysics()
 *    (ya existía implícito en varias subclases — se formaliza).
 *
 * ── RETRO-COMPATIBILIDAD ─────────────────────────────────────────────────
 * Si el código existente llama enemy.update() sin argumentos (vía super.update()
 * de MovingObjects), ese método sigue existiendo — llama update(null) que
 * solo omite la IA. Ninguna subclase existente se rompe.
 */
public abstract class Enemy extends MovingObjects {

    private final EnemyAI   ai;
    private final HealthComponent    health;
    private final EnemyState         state;
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

        addComponent(effects);

        ColliderComponent collider = getComponent(ColliderComponent.class);
        if (collider != null) {
            collider.setProfile(CollisionProfile.ENEMY);
            collider.setSize(24, 30);
        }
    }

    /**
     * Constructor legacy — acepta Player directamente.
     * Permite que EnemyFactory y código existente no cambien.
     * Internamente envuelve el Player en EnemyContext.
     *
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
        // El player se pasará como EnemyContext en cada update desde el World.
        // Guardamos referencia temporal para compatibilidad con subclases que
        // no han migrado aún a update(EnemyContext).
        this.legacyPlayer = player;
    }

    /** Solo para transición. Se elimina cuando todas las subclases migren. */
    private Player legacyPlayer;

    // ── Update ────────────────────────────────────────────────────────────

    /**
     * Update principal — recibe el contexto del objetivo (normalmente el Player).
     * Llamar desde World.update(): enemy.update(EnemyContext.of(player))
     */
    public void update(EnemyContext ctx) {
        if (health.isDead()) {
            onDeath();
            return;
        }

        state.setMoving(false);
        state.setAttacking(false);

        // IA — solo si hay contexto (puede ser null si el target desapareció)
        if (ctx != null) {
            ai.update(this, ctx);
        } else if (legacyPlayer != null) {
            ai.update(this, EnemyContext.of(legacyPlayer));
        }

        // Física específica del tipo (gravedad terrestre, hover volador, etc.)
        updateTypePhysics();

        // Efectos de estado (poison, freeze, etc.)
        effects.update();

        super.update();
    }

    /**
     * Update sin contexto — compatibilidad con código que llama update() directamente.
     * Usa legacyPlayer si existe, o solo aplica física/efectos si no hay objetivo.
     */
    @Override
    public void update() {
        EnemyContext ctx = (legacyPlayer != null) ? EnemyContext.of(legacyPlayer) : null;
        update(ctx);
    }

    // ── Subtipo hook ─────────────────────────────────────────────────────

    /** Aplica la física del tipo concreto (gravedad para terrestres, hover para voladores). */
    protected abstract void updateTypePhysics();

    // ── Daño y muerte ─────────────────────────────────────────────────────

    public void damage(int amount) {
        health.damage(amount);
    }

    /**
     * Muerte — se llama una sola vez cuando health.isDead().
     * Dispara OnEnemyDeathEvent en el bus para que AudioSystem, FXSystem,
     * LootSystem, etc. reaccionen sin estar acoplados a Enemy.
     */
    protected void onDeath() {
        GameEventBus.post(new OnEnemyDeathEvent(this, getTransform().getPosition()));
        markForRemoval(); // señal al World para eliminarlo del ciclo
    }

    // ── Efectos de estado ─────────────────────────────────────────────────

    /**
     * Aplica un efecto de estado al enemigo.
     * Shortcut para no exponer StatusEffectComponent directamente.
     *
     * Uso desde PoisonModifier:
     *   enemy.addEffect(new PoisonModifier.PoisonEffect(3, 20, 120));
     */
    public void addEffect(StatusEffectComponent.StatusEffect effect) {
        effects.add(effect);
    }

    /**
     * Soporte directo para PoisonEffect del sistema de modificadores.
     * Convierte PoisonEffect en StatusEffect compatible con StatusEffectComponent.
     */
    public void addEffect(PoisonModifier.PoisonEffect poisonEffect) {
        effects.add(new StatusEffectComponent.StatusEffect() {
            @Override
            public boolean tick(Enemy enemy) {
                return poisonEffect.tick(enemy);
            }
            @Override
            public String effectId() { return "poison"; }
        });
    }

    public boolean hasEffect(String effectId) {
        return effects.hasEffect(effectId);
    }

    // ── Getters ───────────────────────────────────────────────────────────

    public EnemyState     getState()           { return state; }
    public HealthComponent getHealthComponent() { return health; }
    public EnemyAI        getAI()              { return ai; }

    // ── Ciclo de vida / remoción ──────────────────────────────────────────

    private boolean pendingRemoval = false;

    /** Marca este enemigo para ser eliminado del mundo en el próximo ciclo. */
    public void markForRemoval()    { pendingRemoval = true; }

    /** Devuelve true si este enemigo debe ser eliminado del mundo. */
    public boolean isPendingRemoval() { return pendingRemoval; }

    /** Shortcut — evita el verbose getPhysicsComponent().getPhysics() en las subclases. */
    public Game.Fisics.EnemyPhysics getPhysics() {
        PhysicsComponent pc = getComponent(PhysicsComponent.class);
        return pc != null ? (Game.Fisics.EnemyPhysics) pc.getPhysics() : null;
    }

    public PhysicsComponent getPhysicsComponent() {
        return getComponent(PhysicsComponent.class);
    }

    // ── Colisiones ────────────────────────────────────────────────────────

    @Override
    public void onCollisionWith(Player player)   {}
    @Override
    public void onCollisionWith(Enemy enemy)     {}
    @Override
    public void onCollisionWith(Bullet bullet)   {}
}
