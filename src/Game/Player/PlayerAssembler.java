package Game.Player;

import Game.Engine.Colisions.Filter.CollisionProfile;
import Game.Engine.Entity.Attributes.EntityAttributes;
import Game.Engine.Entity.Combat.AttackSources;
import Game.Engine.Entity.Components.Collisions.ColliderComponent;
import Game.Engine.Entity.Components.HealthComponent;
import Game.Engine.Entity.Components.StatusEffectComponent;
import Game.Engine.Entity.Components.Visuals.AnimationControllerComponent;
import Game.Engine.Entity.Components.Visuals.HitBoxComponent;
import Game.Engine.Entity.Flags.EntityFlags;
import Game.Engine.Entity.Stats.EntityStats;
import Game.Engine.Entity.Stats.RuntimeStats;
import Game.Engine.GameEventBus;
import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Items.Savement.EquippedItems;
import Game.Items.Savement.Inventory;
import Game.Items.Types.Ammulets.PlayerAmulets;
import Game.Items.Types.Bullets.Definition.Bullet;
import Game.Items.Types.Weapons.ModifiedWeapon;
import Game.Items.Types.Weapons.WeaponType.WeaponComport;
import Game.Items.Types.Weapons.WeaponType.WeaponType;
import Sprites.Entity.Player.PlayerAssets;
import java.awt.Color;
import java.util.function.Consumer;

/**
 * Ensamblador del Player — responsable de toda la construcción concreta.
 *
 * ── HRFC — Player Reengineering ───────────────────────────────────────────
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 *
 * PlayerAssembler extrae del constructor de Player toda la lógica de
 * configuración y composición inicial. Player pasa de ser un composition
 * root a ser una entidad pura con dependencias inyectadas.
 *
 * El ensamblador:
 *   1. Configura EntityStats (vida, velocidad, etc.)
 *   2. Construye RuntimeStats, EntityFlags, EntityAttributes, AttackSources
 *   3. Crea y añade HealthComponent (modo enlazado con EntityStats)
 *   4. Crea y añade StatusEffectComponent
 *   5. Construye PlayerState, PlayerStats, PlayerAmulets
 *   6. Construye PlayerPhysics y PlayerController (con EntityFlags)
 *   7. Construye PlayerCombat
 *   8. Lee PlayerLoadout y construye las ModifiedWeapons, pasa a PlayerCombat
 *   9. Configura el collider (tamaño, perfil, offset)
 *   10. Añade HitBoxComponent, AnimationControllerComponent, PlayerRenderer
 *   11. Construye Inventory y EquippedItems
 *   12. Vincula PlayerStats con los sistemas construidos
 *   13. Inicializa HP a BASE_HP
 *
 * ── SEPARACIÓN CONSTRUCTION / RUNTIME ────────────────────────────────────
 *
 *   PlayerAssembler.assemble()  →  configura todo
 *         ↓
 *   Player                      →  compone y expone
 *         ↓
 *   Player.update()             →  ciclo de coordinación
 *
 * ── USO ───────────────────────────────────────────────────────────────────
 *
 *   // Loadout por defecto:
 *   Player player = PlayerAssembler.assemble(
 *       spawn, world::add, eventBus);
 *
 *   // Loadout custom:
 *   Player player = PlayerAssembler.assemble(
 *       spawn, world::add, eventBus,
 *       PlayerLoadout.builder()
 *           .weapon(WeaponType.PISTOLA)
 *           .weapon(WeaponType.ESCOPETA)
 *           .build());
 */
public final class PlayerAssembler {

    // ── Configuración base ────────────────────────────────────────────────

    private static final int    BASE_HP      = 100;
    private static final int    BASE_HP_MAX  = 200;
    private static final double BASE_GRAVITY = 0.78;
    private static final double BASE_SPEED   = 8.0;
    private static final int    INVENTORY_SLOTS = 20;

    // ── Collider ──────────────────────────────────────────────────────────

    private static final int COLLIDER_W  = 15;
    private static final int COLLIDER_H  = 24;
    private static final int COLLIDER_OX = 4;
    private static final int COLLIDER_OY = 0;

    // ── API ───────────────────────────────────────────────────────────────

    /**
     * Ensambla un Player completo con el loadout por defecto.
     *
     * @param spawn         posición inicial en el mundo
     * @param bulletSpawner callback para añadir balas al mundo
     * @param eventBus      bus de eventos del juego
     * @return Player completamente configurado y listo para el ciclo de juego
     */
    public static Player assemble(Vector2D spawn,
                                  Consumer<Bullet> bulletSpawner,
                                  GameEventBus eventBus) {
        return assemble(spawn, bulletSpawner, eventBus, PlayerLoadout.defaultLoadout());
    }

    /**
     * Ensambla un Player completo con el loadout indicado.
     *
     * @param spawn         posición inicial en el mundo
     * @param bulletSpawner callback para añadir balas al mundo
     * @param eventBus      bus de eventos del juego
     * @param loadout       configuración de equipamiento inicial
     * @return Player completamente configurado y listo para el ciclo de juego
     */
    public static Player assemble(Vector2D spawn,
                                  Consumer<Bullet> bulletSpawner,
                                  GameEventBus eventBus,
                                  PlayerLoadout loadout) {
        if (spawn         == null) throw new IllegalArgumentException("spawn es requerido");
        if (bulletSpawner == null) throw new IllegalArgumentException("bulletSpawner es requerido");
        if (eventBus      == null) throw new IllegalArgumentException("eventBus es requerido");
        if (loadout       == null) throw new IllegalArgumentException("loadout es requerido");

        // ── 1. EntityStats — fuente de verdad única ───────────────────────
        EntityStats entityStats = new EntityStats();
        entityStats.setMaxHp(BASE_HP_MAX);
        entityStats.movement().setSpeed(BASE_SPEED);

        // ── 2. RuntimeStats — stats efectivos con modificadores ───────────
        RuntimeStats runtimeStats = new RuntimeStats(entityStats);

        // ── 3. EntityFlags / Attributes / AttackSources ───────────────────
        EntityFlags entityFlags = new EntityFlags();

        EntityAttributes entityAttributes = new EntityAttributes();
        entityAttributes.setFaction(EntityAttributes.Faction.PLAYER);
        entityAttributes.setAlignment(EntityAttributes.Alignment.ALLY);
        entityAttributes.setEntityClass(EntityAttributes.EntityClass.PLAYER);

        AttackSources attackSources = new AttackSources();

        // ── 4. PlayerPhysics ──────────────────────────────────────────────
        PlayerPhysics physics = new PlayerPhysics(BASE_GRAVITY);

        // ── 5. Estado y módulos del Player ────────────────────────────────
        PlayerState  state       = new PlayerState();
        PlayerStats  playerStats = new PlayerStats();
        PlayerAmulets amulets   = new PlayerAmulets();

        // ── 6. PlayerController (recibe EntityFlags para respetar impairments)
        PlayerController controller = new PlayerController(physics, state, entityFlags);

        // ── 7. PlayerCombat ───────────────────────────────────────────────
        // positionSupplier se actualiza en el paso 8b, una vez que Player existe.
        // Se usa un array de un elemento para capturar la referencia diferida
        // sin requerir que Player exista antes de construir PlayerCombat.
        Vector2D[] positionRef = new Vector2D[1];
        PlayerCombat combat = new PlayerCombat(
            state,
            () -> positionRef[0],
            bulletSpawner,
            eventBus
        );

        // ── 8. Player — entidad base ──────────────────────────────────────
        Player player = new Player(
            spawn,
            physics,
            entityStats,
            runtimeStats,
            entityFlags,
            entityAttributes,
            attackSources,
            state,
            playerStats,
            amulets,
            controller,
            combat
        );

        // 8b. Apuntar la referencia diferida a la posición real del Player.
        // Desde este momento, el lambda de positionSupplier devuelve la
        // posición correcta. Es thread-safe porque el game loop es single-threaded.
        positionRef[0] = player.getTransform().getPosition();

        // ── 9. HealthComponent (modo enlazado con EntityStats) ────────────
        HealthComponent healthComponent = new HealthComponent(entityStats) {
            @Override
            protected void onDeath() {
                // Hook de muerte: emitir evento game-over.
                // Los suscriptores reaccionan; el Player no gestiona esto directamente.
            }
        };
        player.addComponent(healthComponent);

        // ── 10. StatusEffectComponent ─────────────────────────────────────
        player.addComponent(new StatusEffectComponent());

        // ── 11. Loadout — construir armas desde WeaponType ────────────────
        for (WeaponType weaponType : loadout.getWeapons()) {
            WeaponComport comport = weaponType.createComport();
            ModifiedWeapon weapon = new ModifiedWeapon(
                comport,
                loadout.getBulletType(),
                amulets,
                player,
                eventBus
            );
            combat.addWeapon(weapon);
        }

        // ── 12. Colisión ──────────────────────────────────────────────────
        ColliderComponent collider = player.getComponent(ColliderComponent.class);
        if (collider != null) {
            collider.setProfile(CollisionProfile.PLAYER);
            collider.setSize(COLLIDER_W, COLLIDER_H);
            collider.setOffset(COLLIDER_OX, COLLIDER_OY);
        }

        // ── 13. Componentes visuales ──────────────────────────────────────
        player.addComponent(new HitBoxComponent(Color.RED));
        player.addComponent(new AnimationControllerComponent(PlayerAssets.handle));
        player.addComponent(new PlayerRenderer(state));

        // ── 14. Inventario ────────────────────────────────────────────────
        Inventory     inventory     = new Inventory(INVENTORY_SLOTS);
        EquippedItems equippedItems = new EquippedItems();
        player.initInventory(inventory, equippedItems);

        // ── 15. Vincular PlayerStats con los sistemas construidos ─────────
        playerStats.bind(entityStats, runtimeStats, entityFlags, healthComponent);

        // ── 16. HP inicial menor que el máximo ────────────────────────────
        if (BASE_HP < BASE_HP_MAX) {
            healthComponent.initCurrentHP(BASE_HP);
        }

        return player;
    }

    // Constructor privado — clase utilitaria pura.
    private PlayerAssembler() {}
}
