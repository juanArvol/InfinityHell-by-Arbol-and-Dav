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
import Game.Items.Types.Ammulets.AmuletInventory;
import Game.Items.Types.Bullets.Definition.Bullet;
import Game.Items.Types.Bullets.Definition.BulletType;
import Game.Items.Types.Weapons.ModifiedWeapon;
import Game.Items.Types.Weapons.WeaponType.WeaponComport;
import Game.Items.Types.Weapons.WeaponType.WeaponType;
import Sprites.Entity.Player.PlayerAssets;
import java.awt.Color;
import java.util.function.Consumer;

/**
 * Ensamblador del Player — responsable de toda la construcción concreta.
 *
 * ── HRFC — Player Reengineering v2 ────────────────────────────────────────
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
 *   7. Construye PlayerCombat con PlayerRuntime
 *   8. Lee PlayerLoadout y materializa armas y balas en PlayerInventory
 *   9. Configura el collider (tamaño, perfil, offset)
 *   10. Añade HitBoxComponent, AnimationControllerComponent, PlayerRenderer
 *   11. Construye Inventory y EquippedItems
 *   12. Vincula PlayerStats con TODOS los sistemas Entity (gateway completo)
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
 *           .bullet(BulletType.NORMALBULLET)
 *           .bullet(BulletType.BULLETJUMP)
 *           .build());
 */
public final class PlayerAssembler {

    // ── Configuración base ────────────────────────────────────────────────

    private static final int    BASE_HP      = 100;
    private static final int    BASE_HP_MAX  = 200;
    private static final double BASE_GRAVITY = 0.78;
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
     * ── HRFC — Player Reengineering v2 ────────────────────────────────────
     *
     * CONSTRUCCIÓN SIMPLIFICADA:
     *   • Eliminado el hack Vector2D[] positionRef
     *   • Construcción en orden lógico sin dependencias circulares artificiales
     *   • PlayerRuntime y PlayerInventory se crean antes de PlayerCombat
     *   • Inyección de dependencias limpia sin referencias diferidas
     *   • PlayerStats vincula TODOS los sistemas Entity (gateway completo)
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

        // ── 1. Entity Systems — configuración de sistemas genéricos ──────
        EntityStats entityStats = new EntityStats();
        entityStats.setMaxHp(BASE_HP_MAX);
        // NOTA: BASE_SPEED eliminado - PlayerPhysics es la fuente de verdad

        RuntimeStats runtimeStats = new RuntimeStats(entityStats);
        EntityFlags entityFlags = new EntityFlags();

        EntityAttributes entityAttributes = new EntityAttributes();
        entityAttributes.setFaction(EntityAttributes.Faction.PLAYER);
        entityAttributes.setAlignment(EntityAttributes.Alignment.ALLY);
        entityAttributes.setEntityClass(EntityAttributes.EntityClass.PLAYER);

        AttackSources attackSources = new AttackSources();

        // ── 2. Player-specific modules — sin dependencias entre ellos ─────
        PlayerPhysics physics = new PlayerPhysics(BASE_GRAVITY);
        PlayerState state = new PlayerState();
        PlayerStats playerStats = new PlayerStats();
        AmuletInventory amulets = new AmuletInventory();

        // ── 3. Inventory y Runtime — independientes de Player ─────────────
        PlayerInventory playerInventory = new PlayerInventory();
        PlayerRuntime playerRuntime = new PlayerRuntime(playerInventory);

        // ── 4. Controllers y Combat — dependen de módulos básicos ─────────
        // EntityFlags es obligatorio desde HRFC v2
        PlayerController controller = new PlayerController(physics, state, entityFlags);
        
        PlayerCombat combat = new PlayerCombat(
            state,
            playerRuntime,
            null,  // positionSupplier será inyectado después
            bulletSpawner,
            eventBus
        );

        // ── 5. Player — entidad principal ────────────────────────────────
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

        // ── 6. Post-construcción — inyección de dependencias diferidas ────
        combat.setPositionSupplier(() -> player.getTransform().getPosition());

        // ── 7. Components — añadir componentes al Player ──────────────────
        HealthComponent healthComponent = new HealthComponent(entityStats) {
            @Override
            protected void onDeath() {
                // Hook de muerte: emitir evento game-over.
                // Los suscriptores reaccionan; el Player no gestiona esto directamente.
            }
        };
        player.addComponent(healthComponent);
        player.addComponent(new StatusEffectComponent());

        // ── 8. Loadout — materializar configuración inicial ───────────────
        // Construir armas desde WeaponType (sin BulletType fijo)
        //
        // ── HRFC — Weapon Type Runtime Identity ──────────────────────────
        // 
        // WeaponType se pasa explícitamente a ModifiedWeapon constructor para
        // conservar la identidad declarativa en la instancia runtime.
        for (WeaponType weaponType : loadout.getWeapons()) {
            WeaponComport comport = weaponType.createComport();
            ModifiedWeapon weapon = new ModifiedWeapon(
                weaponType,  // Identidad declarativa conservada
                comport,
                amulets,
                player,
                eventBus
            );
            playerInventory.addWeapon(weapon);
        }
        
        // Añadir todas las balas del loadout al inventario
        for (BulletType bulletType : loadout.getBullets()) {
            playerInventory.addBullet(bulletType);
        }

        // ── 9. Collision y Rendering ──────────────────────────────────────
        ColliderComponent collider = player.getComponent(ColliderComponent.class);
        if (collider != null) {
            collider.setProfile(CollisionProfile.PLAYER);
            collider.setSize(COLLIDER_W, COLLIDER_H);
            collider.setOffset(COLLIDER_OX, COLLIDER_OY);
        }

        player.addComponent(new HitBoxComponent(Color.RED));
        player.addComponent(new AnimationControllerComponent(PlayerAssets.handle));
        player.addComponent(new PlayerRenderer(state));

        // ── 10. Inventario general y Runtime ──────────────────────────────
        Inventory     inventory     = new Inventory(INVENTORY_SLOTS);
        EquippedItems equippedItems = new EquippedItems();
        player.initInventory(inventory, equippedItems);
        player.initRuntime(playerRuntime);

        // ── 11. Vinculación final de sistemas ─────────────────────────────
        // PlayerStats ahora es gateway completo a sistemas Entity
        playerStats.bind(entityStats, runtimeStats, entityFlags, entityAttributes, attackSources, healthComponent);

        // ── 12. Inicialización final ──────────────────────────────────────
        if (BASE_HP < BASE_HP_MAX) {
            healthComponent.initCurrentHP(BASE_HP);
        }

        return player;
    }

    // Constructor privado — clase utilitaria pura.
    private PlayerAssembler() {}
}
