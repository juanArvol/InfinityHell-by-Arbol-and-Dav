package Game.Player;

import Game.Engine.Colisions.Filter.CollisionProfile;
import Game.Engine.Entity.Attributes.EntityAttributes;
import Game.Engine.Entity.Combat.AttackSources;
import Game.Engine.Entity.Components.Collisions.ColliderComponent;
import Game.Engine.Entity.Components.Collisions.MaterialComponent;
import Game.Engine.Entity.Components.HealthComponent;
import Game.Engine.Entity.Components.PhysicsComponent;
import Game.Engine.Entity.Components.StatusEffectComponent;
import Game.Engine.Entity.Components.Visuals.AnimationControllerComponent;
import Game.Engine.Entity.Components.Visuals.HitBoxComponent;
import Game.Engine.Entity.Flags.EntityFlags;
import Game.Engine.Entity.Stats.EntityStats;
import Game.Engine.Entity.Stats.RuntimeStats;
import Game.Engine.GameEventBus;
import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Engine.Physics.Core.PhysicalState;
import Game.Engine.Physics.Electrical.ElectricalProperties;
import Game.Engine.Physics.Fluid.FluidProperties;
import Game.Engine.Physics.Mechanical.MechanicalProperties;
import Game.Engine.Physics.Thermal.ThermalProperties;
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
 *   // Loadout estándar:
 *   Player player = PlayerAssembler.assemble(
 *       spawn, world::add, eventBus);
 *
 *   // Loadout custom usando API declarativa:
 *   PlayerLoadout loadout = PlayerLoadout
 *       .initialWeapons(WeaponType.PISTOLA, WeaponType.ESCOPETA)
 *       .initialBullets(BulletType.NORMALBULLET, BulletType.BULLETJUMP)
 *       .initialAmulets()
 *       .build();
 *   Player player = PlayerAssembler.assemble(
 *       spawn, world::add, eventBus, loadout);
 */
public final class PlayerAssembler {

    // ── Configuración base ────────────────────────────────────────────────

    private static final int    BASE_HP      = 1;
    private static final int    BASE_HP_MAX  = 200;
    private static final double BASE_GRAVITY = 702;
    private static final int    INVENTORY_SLOTS = 20;

    // ── Collider ──────────────────────────────────────────────────────────

    private static final int COLLIDER_W  = 15;
    private static final int COLLIDER_H  = 24;
    private static final int COLLIDER_OX = 4;
    private static final int COLLIDER_OY = 0;

    // ── Propiedades físicas del jugador ───────────────────────────────────
    // HRFC FASE 1 — Universal Physical Properties Integration

    /** Material biológico del jugador (propiedades físicas intrínsecas). */
    private static final MaterialComponent PLAYER_MATERIAL = 
        MaterialComponent.builder()
            .thermalConductivity(0.6)      // cuerpo humano - conductor moderado
            .heatCapacity(3500.0)          // alta capacidad calorífica (agua + proteínas)
            .thermalDiffusivity(0.15)      // disipación moderada
            .electricalConductivity(0.4)   // conductor moderado (fluidos corporales)
            .humidityAbsorption(0.3)       // piel absorbe humedad moderadamente
            .density(1050.0)               // similar al agua
            .compressibility(0.05)         // tejidos bastante incompresibles
            .elasticity(0.4)               // tejidos algo elásticos
            .hardness(0.2)                 // cuerpo humano es blando
            .build();

    /** Temperatura corporal normal del jugador (°C en unidades del juego). */
    private static final double BODY_TEMPERATURE = 36.5;

    /** Hidratación normal del jugador (0.0 = deshidratado, 1.0 = saturado). */
    private static final double NORMAL_HYDRATION = 0.6;

    // ── API ───────────────────────────────────────────────────────────────

    /**
     * Ensambla un Player completo con el loadout por defecto.
     *
     * ── MINI-HRFC — Corrección de Arquitectura del Loadout ────────────────
     * 
     * Este método de conveniencia construye el loadout estándar explícitamente.
     * La configuración no viene de un preset en PlayerLoadout, sino que se
     * declara aquí como decisión del contexto de ensamblado.
     *
     * @param spawn         posición inicial en el mundo
     * @param bulletSpawner callback para añadir balas al mundo
     * @param eventBus      bus de eventos del juego
     * @return Player completamente configurado y listo para el ciclo de juego
     */
    

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
     * ── HRFC — ProjectilePool Integration Consolidation ──────────────────
     *
     * ProjectilePool ahora es obligatorio para gameplay. Las armas del jugador
     * requieren el pool configurado con ProjectileContextResolver para que los
     * proyectiles reciban las capacidades contextuales necesarias (SpatialQuery,
     * ProjectileSpawning, etc.).
     *
     * El pool debe venir pre-configurado desde el composition root (GameWorldBootstrap)
     * con el resolver y los capability providers ya registrados.
     *
     * @param spawn         posición inicial en el mundo
     * @param bulletSpawner callback para añadir balas al mundo
     * @param eventBus      bus de eventos del juego
     * @param loadout       configuración de equipamiento inicial
     * @param projectilePool pool de proyectiles configurado con resolver (obligatorio)
     * @return Player completamente configurado y listo para el ciclo de juego
     */
    public static Player assemble(Vector2D spawn,
                                  Consumer<Bullet> bulletSpawner,
                                  GameEventBus eventBus,
                                  PlayerLoadout loadout,
                                  Game.Items.Types.Bullets.Definition.ProjectilePool projectilePool) {
        if (spawn         == null) throw new IllegalArgumentException("spawn es requerido");
        if (bulletSpawner == null) throw new IllegalArgumentException("bulletSpawner es requerido");
        if (eventBus      == null) throw new IllegalArgumentException("eventBus es requerido");
        if (loadout       == null) throw new IllegalArgumentException("loadout es requerido");
        if (projectilePool == null) throw new IllegalArgumentException("projectilePool es requerido");

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
        
        // ── HRFC FASE 1 — Universal Physical Properties Integration ───────
        // PhysicsComponent: Fuente única de verdad del estado físico.
        // El Player participa automáticamente en todos los dominios físicos
        // (thermal, electrical, fluid, mechanical) declarativamente.
        PhysicalState physicalState = PhysicalState.builder()
            // Estado físico inicial
            .register(ThermalProperties.TEMPERATURE, BODY_TEMPERATURE)  // 36.5°C
            .register(ElectricalProperties.CHARGE, 0.0)                 // neutro
            .register(FluidProperties.HUMIDITY, NORMAL_HYDRATION)       // 60% hidratación
            .register(MechanicalProperties.PRESSURE, 0.0)               // presión ambiente
            
            // Propiedades del material biológico
            .registerMaterial(PLAYER_MATERIAL::registerInto)
            
            .build();
        
        player.addComponent(new PhysicsComponent(physicalState));
        
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
        //
        // ── HRFC — ProjectilePool Integration Consolidation ──────────────
        //
        // Inyectamos el ProjectilePool configurado desde el bootstrap a cada arma.
        // Esto garantiza que todos los proyectiles del jugador se adquieren via
        // pool.acquire() con ProjectileContext correcto, en lugar del fallback
        // BulletFactory.build() que no asigna contexto.
        for (WeaponType weaponType : loadout.getWeapons()) {
            WeaponComport comport = weaponType.createComport();
            ModifiedWeapon weapon = new ModifiedWeapon(
                weaponType,       // Identidad declarativa conservada
                comport,
                amulets,
                projectilePool,   // Pool configurado con resolver
                player,
                eventBus
            );
            playerInventory.addWeapon(weapon);
        }
        
        // Añadir todas las balas del loadout al inventario
        for (BulletType bulletType : loadout.getBullets()) {
            playerInventory.addBullet(bulletType);
        }

        // ── MINI-HRFC — BOOTSTRAP DECLARATIVO ─────────────────────────────
        // Añadir todos los amuletos del loadout al inventario.
        // Los amuletos del loadout ya son AmuletDefinition (no IDs).
        // Se añaden directamente al AmuletInventory sin resolución adicional.
        for (Game.Items.Types.Ammulets.AmuletDefinition amulet : loadout.getAmulets()) {
            amulets.add(amulet);
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
