package Game.Player;

import Game.Engine.Colisions.Filter.CollisionProfile;
import Game.Engine.Entity.Attributes.EntityAttributes;
import Game.Engine.Entity.Combat.AttackSources;
import Game.Engine.Entity.Components.Collisions.ColliderComponent;
import Game.Engine.Entity.Components.HealthComponent;
import Game.Engine.Entity.Components.StatusEffectComponent;
import Game.Engine.Entity.Components.Visuals.AnimationControllerComponent;
import Game.Engine.Entity.Components.Visuals.HitBoxComponent;
import Game.Engine.Entity.EntityInfoProvider;
import Game.Engine.Entity.Flags.EntityFlags;
import Game.Engine.Entity.Stats.EntityStats;
import Game.Engine.Entity.Stats.RuntimeStats;
import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Engine.MovingObjects;
import Game.Engine.RenderEngine.Sprites.SizeSyncMode;
import Game.Items.Savement.EquippedItems;
import Game.Items.Savement.Inventory;
import Game.Items.Types.Bullets.Bullet;
import Sprites.Entity.Player.PlayerAssets;
import java.awt.Color;
import java.util.function.Consumer;

/**
 * Jugador.
 *
 * ── HRFC-013 — Consolidación Definitiva del Dominio Entity ───────────────
 *
 * Player posee un EntityStats propio que actúa como única fuente de verdad
 * para todos sus atributos base (salud, velocidad, daño, resistencias, etc.),
 * igual que Enemy. HealthComponent se construye en modo enlazado:
 * new HealthComponent(entityStats) — no almacena estado propio.
 *
 * ── HRFC-014 — Player implementa EntityInfoProvider (GAP-9) ─────────────
 *
 * Player implementa EntityInfoProvider (el mismo contrato que Enemy), lo que
 * significa que cualquier sistema puede tratarlo como una entidad con stats RPG
 * completos sin saber su tipo concreto.
 *
 * Contrato implementado:
 *   getStats()        → EntityStats   (única fuente de verdad de atributos base)
 *   getRuntimeStats() → RuntimeStats  (atributos efectivos con modificadores)
 *   getFlags()        → EntityFlags   (capabilities/states/impairments)
 *   getAttributes()   → EntityAttributes (facción, clase, elemento)
 *   getAttackSources()→ AttackSources
 *
 * Esto permite el patrón estándar de revocación en StatusEffects:
 *
 *   if (entity instanceof EntityInfoProvider living)
 *       living.getRuntimeStats().revoke(this);
 *
 * ── PlayerStats — su rol correcto ───────────────────────────────────────
 *
 * PlayerStats NO es una fuente alternativa de stats. Su rol es:
 *   - Fachada de solo lectura hacia HealthComponent para la UI (HealthView).
 *   - Invulnerabilidad post-golpe (lógica específica del Player).
 *   - Multiplicadores de gameplay que complementan EntityStats.
 *
 * Para leer stats en sistemas de gameplay: siempre usar getStats() o
 * getRuntimeStats(), nunca getPlayerStats().
 *
 * ── FLUJO DE DAÑO ────────────────────────────────────────────────────────
 *
 *   // Con invulnerabilidad (proyectil, contacto enemigo):
 *   player.receiveDamage(amount);
 *
 *   // Sin invulnerabilidad (efecto de estado, caída, lava):
 *   player.damage(amount);  // Entity → HealthComponent → HealthStats
 *
 * ── JERARQUÍA ─────────────────────────────────────────────────────────────
 *
 *   GameObjects → AbstractEntity → MovingObjects → Player
 *   Player implements EntityInfoProvider
 */
public class Player extends MovingObjects implements EntityInfoProvider {

    // ── Configuración base de salud ───────────────────────────────────────
    private static final int    BASE_HP      = 100;
    private static final int    BASE_HP_MAX  = 200;
    private static final double BASE_GRAVITY = 0.78;

    // ── EntityInfoProvider — única fuente de verdad ───────────────────────
    //
    // entityStats    → valores permanentes (base). Configura Assemblers y fases.
    // runtimeStats   → valores efectivos con modificadores. Todo el gameplay
    //                  debe leer de aquí, no de entityStats directamente.
    // entityFlags    → capabilities/states/impairments del Player.
    // entityAttributes → facción PLAYER, clase PLAYER, alineación ALLY.
    // attackSources  → fuentes de ataque (WEAPON por defecto).
    private final EntityStats      entityStats;
    private final RuntimeStats     runtimeStats;
    private final EntityFlags      entityFlags;
    private final EntityAttributes entityAttributes;
    private final AttackSources    attackSources;

    // ── Módulos específicos del Player ────────────────────────────────────
    private final PlayerController controller;
    private final PlayerCombat     combat;

    /**
     * Fachada de UI y atributos de gameplay específicos del Player.
     * NO es fuente de stats. Para stats usar getStats() / getRuntimeStats().
     */
    private final PlayerStats      playerStats;
    private final PlayerState      state;

    private final Inventory     inventory;
    private final EquippedItems equippedItems;

    /**
     * @param spawn         posición inicial en el mundo
     * @param bulletSpawner callback para añadir balas al mundo (ej: world::add)
     */
    public Player(Vector2D spawn, Consumer<Bullet> bulletSpawner) {
        super(spawn,
              PlayerAssets.handle,
              new PlayerPhysics(BASE_GRAVITY),
              SizeSyncMode.NONE);

        // ── EntityStats — fuente de verdad única ──────────────────────────
        // Toda la configuración de salud, movimiento y combate del Player
        // vive aquí. setMaxHp() inicializa también currentHp al máximo.
        entityStats = new EntityStats();
        entityStats.setMaxHp(BASE_HP_MAX);
        // Movimiento base: los sistemas leerán de runtimeStats.getMovement()
        entityStats.movement().setSpeed(3.0);

        // ── RuntimeStats — atributos efectivos ───────────────────────────
        // Construido sobre entityStats. Los StatusEffects aplican aquí.
        runtimeStats = new RuntimeStats(entityStats);

        // ── EntityFlags / Attributes / AttackSources ─────────────────────
        entityFlags      = new EntityFlags();
        entityAttributes = new EntityAttributes();
        entityAttributes.setFaction(EntityAttributes.Faction.PLAYER);
        entityAttributes.setAlignment(EntityAttributes.Alignment.ALLY);
        entityAttributes.setEntityClass(EntityAttributes.EntityClass.PLAYER);
        attackSources = new AttackSources();

        // ── HealthComponent en modo enlazado ─────────────────────────────
        // Delega sobre entityStats.health(). No almacena estado propio.
        addComponent(new HealthComponent(entityStats) {
            @Override
            protected void onDeath() {
                // Hook de muerte del Player — emitir evento, mostrar game-over.
            }
        });

        // StatusEffectComponent: efectos de estado (veneno, quemadura, etc.)
        addComponent(new StatusEffectComponent());

        // ── Módulos de gameplay ───────────────────────────────────────────
        state       = new PlayerState();
        playerStats = new PlayerStats();

        // Vincular HealthComponent a PlayerStats (fachada de UI solamente).
        // LifeHUD → PlayerStats.getLife() → HealthComponent.getCurrent()
        playerStats.bindHealth(getHealth());

        PlayerPhysics physics = (PlayerPhysics) getPhysics();
        controller = new PlayerController(physics, state);

        combat = new PlayerCombat(
            state,
            () -> getTransform().getPosition(),
            bulletSpawner
        );

        // Loadout inicial
        combat.setInitialWeapon(
            new Game.Items.Types.Weapons.WeaponSelected(
                new Game.Items.Types.Weapons.WeaponType.WeaponClass.WeaponEscopeta(),
                Game.Items.Types.Bullets.BulletType.SPRINGBULLET
            )
        );

        // ── Colisión y visual ─────────────────────────────────────────────
        ColliderComponent collider = getComponent(ColliderComponent.class);
        if (collider != null) {
            collider.setProfile(CollisionProfile.PLAYER);
            collider.setSize(15, 24);
            collider.setOffset(4, 0);
        }

        addComponent(new HitBoxComponent(Color.RED));
        addComponent(new AnimationControllerComponent(PlayerAssets.handle));
        addComponent(new PlayerRenderer(state));

        // ── Inventario y equipamiento ─────────────────────────────────────
        inventory     = new Inventory(playerStats.getMaxInventorySlots());
        equippedItems = new EquippedItems();

        // HP inicial = BASE_HP (menor que BASE_HP_MAX).
        // initCurrentHP escribe en HealthStats sin disparar hooks.
        if (BASE_HP < BASE_HP_MAX) {
            getHealth().initCurrentHP(BASE_HP);
        }
    }

    // ── Update ────────────────────────────────────────────────────────────

    @Override
    public void update() {
        // Sincronizar onGround de física → PlayerState (para controller y animaciones).
        if (physicsComponent != null) {
            state.setEnElSuelo(physicsComponent.getPhysics().getOnGround());
        }

        controller.update();
        combat.update();

        // applyGravity() y flushAccumulatedForces() los aplica CollisionsSystem
        // en FASE 0.5, después de actualizar onGround (FASE 0).

        super.update(); // actualiza Component registrados (HealthComponent, StatusEffectComponent…)

        // PlayerStats no es Component: se actualiza manualmente después de los Components
        // para que los frames de invulnerabilidad decrementen tras aplicar el daño del frame.
        playerStats.update();
    }

    // ── API de daño con invulnerabilidad ──────────────────────────────────

    /**
     * Aplica daño al Player respetando frames de invulnerabilidad post-golpe.
     * Usar para daño directo (proyectil, contacto).
     * Para daño de efecto de estado usar damage() heredado de AbstractEntity.
     */
    public void receiveDamage(int amount) {
        if (playerStats.isInvulnerable()) return;
        damage(amount);  // AbstractEntity → HealthComponent → HealthStats
        playerStats.triggerInvulnerability();
    }

    // ── EntityInfoProvider ────────────────────────────────────────────────

    /**
     * Estadísticas base del Player.
     * Única fuente de verdad de atributos permanentes.
     * En gameplay, leer siempre desde getRuntimeStats().
     */
    @Override
    public EntityStats getStats() { return entityStats; }

    /**
     * Estadísticas efectivas con todos los modificadores activos.
     * Todo el código de combate y sistemas debe leer de aquí.
     */
    @Override
    public RuntimeStats getRuntimeStats() { return runtimeStats; }

    /** Flags de capabilities/states/impairments del Player. */
    @Override
    public EntityFlags getFlags() { return entityFlags; }

    /** Atributos de dominio: facción PLAYER, clase PLAYER, alineación ALLY. */
    @Override
    public EntityAttributes getAttributes() { return entityAttributes; }

    /** Fuentes de ataque disponibles (WEAPON, etc.). */
    @Override
    public AttackSources getAttackSources() { return attackSources; }

    // ── Getters de módulos específicos del Player ─────────────────────────

    public Vector2D         getPosition()      { return getTransform().getPosition(); }
    public PlayerState      getState()         { return state; }
    public PlayerController getController()    { return controller; }
    public PlayerCombat     getCombat()        { return combat; }
    public Inventory        getInventory()     { return inventory; }
    public EquippedItems    getEquippedItems() { return equippedItems; }

    /**
     * Fachada de UI e invulnerabilidad específica del Player.
     * NO usar para stats de gameplay — usar getStats() / getRuntimeStats().
     */
    public PlayerStats getPlayerStats() { return playerStats; }

    // ── Colisiones ────────────────────────────────────────────────────────
    // onCollisionWith(GameObjects) heredado — default vacío correcto.
    // El daño recibido desde proyectiles enemigos lo gestiona el emisor.
}
