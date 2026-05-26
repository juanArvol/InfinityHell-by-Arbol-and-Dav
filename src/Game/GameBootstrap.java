package Game;

import Game.Bullets.BulletType;
import Game.Events.GameEventBus;
import Game.Events.GameEvents.*;
import Game.Items.*;
import Game.Physics3D.Physics3DComponent;
import Game.Weapons.Modifiers.*;
import Game.Weapons.WeaponType.WeaponClass.WeaponEscopeta;
import Game.Weapons.WeaponType.WeaponClass.WeaponPistola;
import Game.World.Generator.Layer.Objects.*;
import Game.World.Generator.WorldGenerator;
import Game.World.Generator.WorldGenerator.WorldGeneratorConfig;
import Game.World.WorldObjects.WorldItem;
import Game.World.WorldObjects.WorldObjectFactory;
import GameMath.Vector2D;

/**
 * Bootstrap del juego — inicialización en orden correcto.
 *
 * ── ORDEN OBLIGATORIO ─────────────────────────────────────────────────────
 *
 *  1. ItemRegistry.init()          ← primero siempre
 *  2. Registrar ItemDefinitions    ← antes de LootSpawnLayer o pickups
 *  3. GameEventBus.subscribe()     ← antes de cualquier evento
 *  4. WorldGenerator + config      ← genera el mundo
 *  5. Player                       ← necesita el mundo generado
 *
 * ── ESTE ARCHIVO ─────────────────────────────────────────────────────────
 * Sirve como referencia y punto de integración real. En producción, la lógica
 * de registro de items y suscripción de eventos se puede dividir en clases
 * separadas (ItemDataRegistry, AudioEventListener, UIEventListener, etc.).
 */
public final class GameBootstrap {

    private GameBootstrap() {}

    /**
     * Inicializa todos los sistemas en el orden correcto.
     * Llamar una vez al inicio de la sesión de juego.
     */
    public static void init() {

        // ── 1. Item Registry ─────────────────────────────────────────────
        ItemRegistry.init();
        registerItems();

        // ── 2. Event Bus (antes de cualquier sistema que emita) ──────────
        subscribeEvents();
    }

    // ── Registro de ítems ─────────────────────────────────────────────────

    private static void registerItems() {

        // Armas de fuego
        ItemRegistry.register(new ItemDefinition.Builder("pistol_9mm", ItemType.FIREARM)
            .displayName("Pistola 9mm")
            .weight(0.9)
            .magazineSize(15)
            .damage(15f)
            .range(300f)
            .build());

        ItemRegistry.register(new ItemDefinition.Builder("shotgun_pump", ItemType.FIREARM)
            .displayName("Escopeta")
            .weight(3.5)
            .magazineSize(6)
            .damage(17f)
            .range(150f)
            .build());

        // Munición
        ItemRegistry.register(new ItemDefinition.Builder("ammo_9mm", ItemType.AMMO)
            .displayName("Munición 9mm")
            .maxStack(50)
            .weight(0.02)
            .build());

        ItemRegistry.register(new ItemDefinition.Builder("ammo_shotgun", ItemType.AMMO)
            .displayName("Cartucho escopeta")
            .maxStack(20)
            .weight(0.08)
            .build());

        // Consumibles
        ItemRegistry.register(new ItemDefinition.Builder("bandage", ItemType.CONSUMABLE)
            .displayName("Vendaje")
            .maxStack(5)
            .weight(0.1)
            .build());
    }

    // ── Suscripción a eventos ─────────────────────────────────────────────

    private static void subscribeEvents() {
        // Placeholder — en producción estos se registran desde AudioSystem, UISystem, etc.

        GameEventBus.subscribe(OnPickupEvent.class, e ->
            System.out.println("[PICKUP] " + e.player().getClass().getSimpleName()
                + " recogió " + e.amount() + "x " + e.definition().displayName));

        GameEventBus.subscribe(OnWeaponFireEvent.class, e -> {
            if (e.sound() != null) {
                // Sounds.playSound(e.sound()); // Audio real cuando esté disponible
            }
        });

        GameEventBus.subscribe(OnJumpEvent.class, e ->
            System.out.println("[JUMP] impulso=" + e.impulse()));

        GameEventBus.subscribe(OnLandEvent.class, e ->
            System.out.println("[LAND]"));
    }

    // ── Ejemplos de uso real ──────────────────────────────────────────────

    /**
     * EJEMPLO 1: Crear arma modificada con composición runtime.
     *
     * Escopeta + ExplosiveModifier + PoisonModifier
     * → cada disparo produce 8 balas que explotan Y envenenan.
     */
    public static ModifiedWeapon ejemploEscopetaExplosiva() {
        ModifiedWeapon mw = new ModifiedWeapon(new WeaponEscopeta(), BulletType.NORMAL);
        mw.addModifier(new ExplosiveModifier(80.0, 1.2));  // radio 80, daño splash x1.2
        mw.addModifier(new PoisonModifier(2, 25, 100));    // 2dmg cada 25 ticks por 100 ticks
        return mw;
    }

    /**
     * EJEMPLO 2: Pistola con piercing (3 enemigos).
     */
    public static ModifiedWeapon ejemploPistolaPiercing() {
        ModifiedWeapon mw = new ModifiedWeapon(new WeaponPistola(), BulletType.NORMAL);
        mw.addModifier(new PiercingModifier(3));
        return mw;
    }

    /**
     * EJEMPLO 3: WorldGenerator con todas las layers nuevas.
     *
     * BackGround → Terrain → Obstacles → Vegetation → Buildings → Loot
     */
    public static WorldGenerator ejemploGeneradorCompleto() {
        WorldGeneratorConfig cfg = WorldGeneratorConfig.defaults()    // fondo + terreno + obstáculos
            .addLayer(new VegetationLayer(8, 15, 16, 32))
            .addLayer(new BuildingLayer(2, 4))
            .addLayer(new LootSpawnLayer.Builder()
                .count(5, 10)
                .addEntry("pistol_9mm",   ItemRarity.UNCOMMON,  1,  1)
                .addEntry("ammo_9mm",     ItemRarity.COMMON,    10, 30)
                .addEntry("bandage",      ItemRarity.COMMON,    1,  3)
                .addEntry("shotgun_pump", ItemRarity.RARE,      1,  1)
                .build()
            );
        return new WorldGenerator(cfg);
    }

    /**
     * EJEMPLO 4: Spawnear un WorldItem manualmente en el mundo.
     */
    public static WorldItem ejemploSpawnItem(Game.World.Core.World world) {
        ItemStack stack = ItemRegistry.createStack("pistol_9mm");
        WorldItem item = WorldObjectFactory.worldItem(new Vector2D(300, 200), stack);
        world.add(item);
        return item;
    }

    /**
     * EJEMPLO 5: Physics3D en un jugador (activar salto Z).
     *
     * En Player.java, en el constructor, añadir:
     *   Physics3DComponent jump3d = new Physics3DComponent(0.4);
     *   addComponent(jump3d);
     *
     * En PlayerController.handleJumpInput():
     *   Physics3DComponent p3d = player.getComponent(Physics3DComponent.class);
     *   if (p3d != null && KeyBoard.space) p3d.jump(10.0);
     */
    public static void ejemploActivarPhysics3DEnPlayer(Game.Player.Player player) {
        Physics3DComponent p3d = new Physics3DComponent(
            Physics3DComponent.HeightPhysicsConfig.defaults()
        );
        player.addComponent(p3d);
        // Desde ahora: p3d.jump(10.0) para saltar, p3d.getZ() para altura actual
    }
}
