package States;

import Display.Managers.DisplayManager;
import Display.ViewportInfo;
import Game.Ammulets.AmuletRegistry;
import Game.Events.GameEventBus;
import Game.Events.GameEvents.*;
import Game.Items.ItemRegistry;
import Game.Player.Player;
import Game.UI.AmmoHUD;
import Game.UI.CrossHairHUD;
import Game.UI.LifeHUD;
import Game.UI.UIManager;
import Game.Weapons.WeaponRegistry;
import Game.World.Core.World;
import Game.World.Core.WorldManager;
import Game.World.Generator.Layer.Objects.*;
import Game.World.Generator.WorldGenerator;
import Game.World.Generator.WorldGeneratorConfig;
import Game.Settings.GameSettings;
import GameMath.Vector2D;
import Game.Enemys.Spawner.EnemySpawner;
import Graficos.Player.PlayerAssets;

import java.awt.Graphics2D;

/**
 * Estado principal del juego.
 *
 * ── CAMBIOS RESPECTO A LA VERSIÓN ANTERIOR ───────────────────────────────
 * GameBootstrap fue eliminado. Su lógica de inicialización se absorbe aquí,
 * en el método initRegistries() que se llama al principio de init().
 *
 * Razón: GameBootstrap era una clase auxiliar sin estado que solo contenía
 * llamadas de inicialización. Como GameState ya es el punto de entrada del
 * juego (creado en GameOrquester), es el lugar natural para esa lógica.
 *
 * ── ORDEN DE INICIALIZACIÓN (obligatorio) ────────────────────────────────
 *  1. ItemRegistry.init()
 *  2. WeaponRegistry.init() + registerDefaults()
 *  3. AmuletRegistry.init() + registerDefaults()
 *  4. Registrar ítems del ItemRegistry
 *  5. GameEventBus.subscribe()     ← antes de cualquier evento
 *  6. WorldManager.init()
 *  7. Player + Spawner + HUDs
 *
 * ── LOOT Y OFERTAS ────────────────────────────────────────────────────────
 * El pool de armas/balas/amuletos ofrecidos al jugador se construye con:
 *   WeaponRegistry.buildOfferPool(player.getOwnedWeapons(), count, random)
 *   WeaponRegistry.buildBulletOfferPool(player.getOwnedBulletTypes(), count, random)
 *   AmuletRegistry.buildOfferPool(count, random)  ← sin filtro, siempre disponibles
 */
public class GameState {

    private WorldManager worldManager;
    private final UIManager uiManager;

    private Player player;
    private EnemySpawner spawner;

    private final DisplayManager display;

    private boolean initialized = false;

    public GameState(DisplayManager display) {
        this.display  = display;
        this.uiManager = new UIManager(display.getVirtualWidth(), display.getVirtualHeight());
        GameSettings.getInstance().setDebugEnabled(true);
    }

    // ── Inicialización ────────────────────────────────────────────────────

    private void init(int virtualWidth, int virtualHeight) {

        // ── 1. Registros globales (antes era GameBootstrap.init()) ─────────
        initRegistries();

        // ── 2. Mundo ───────────────────────────────────────────────────────
        WorldManager.init(virtualWidth, virtualHeight);
        worldManager = WorldManager.getInstance();

        World world = worldManager.getCurrentWorld();

        // ── 3. Jugador ─────────────────────────────────────────────────────
        Vector2D spawnPos = new Vector2D(
            world.getWidth()  / 2.0,
            world.getHeight() / 2.0 - 200
        );

        player = new Player(
            spawnPos,
            PlayerAssets.idle.getSprite(),
            world
        );

        world.add(player);
        world.centerCameraOn(player, virtualWidth, virtualHeight);

        // ── 4. Spawner ─────────────────────────────────────────────────────
        spawner = new EnemySpawner(player);
        spawner.spawn(world, 0);

        // ── 5. HUDs ────────────────────────────────────────────────────────
        uiManager.add(new LifeHUD(player.getStats(), virtualWidth, virtualHeight));
        uiManager.add(new AmmoHUD(player.getCombat().getInventory(), virtualWidth, virtualHeight));
        uiManager.add(new CrossHairHUD(player, virtualWidth, virtualHeight));

        initialized = true;
    }

    /**
     * Inicializa todos los registros del juego.
     * Antes estaba en GameBootstrap.init() — ahora vive aquí.
     *
     * ── ORDEN OBLIGATORIO ─────────────────────────────────────────────────
     * Modificar el orden puede causar IllegalStateException en runtime.
     */
    private void initRegistries() {

        // Registros de ítems genéricos
        ItemRegistry.init();
        registerConsumables();

        // Registros de combate (armas, balas, amuletos)
        WeaponRegistry.init();
        WeaponRegistry.registerDefaults();

        AmuletRegistry.init();
        AmuletRegistry.registerDefaults();

        // Eventos — suscribir ANTES de cualquier evento que pueda dispararse
        subscribeEvents();
    }

    /**
     * Registra ítems consumibles/recursos del mundo (bandages, etc.).
     *
     * Nota: armas y munición ya NO van aquí. Las armas están en WeaponRegistry.
     * No hay munición consumible en el diseño actual.
     */
    private void registerConsumables() {
        ItemRegistry.register(new Game.Items.ItemDefinition.Builder("bandage", Game.Items.ItemType.CONSUMABLE)
            .displayName("Vendaje").maxStack(5).weight(0.1).build());

        // Añade aquí más consumibles/recursos según necesites
    }

    private void subscribeEvents() {
        GameEventBus.subscribe(OnPickupEvent.class, e ->
            System.out.println("[PICKUP] " + e.amount() + "x " + e.definition().displayName));

        GameEventBus.subscribe(OnEnemyDeathEvent.class, e ->
            System.out.println("[DEATH] enemigo en " + e.position()));

        GameEventBus.subscribe(OnJumpEvent.class, e ->
            System.out.println("[JUMP] impulso=" + e.impulse()));
    }

    // ── Ciclo de juego ────────────────────────────────────────────────────

    /**
     * Update en coordenadas virtuales.
     * GameLoop llama: gameState.update(display.getVirtualWidth(), display.getVirtualHeight())
     */
    public void update(int virtualWidth, int virtualHeight) {
        if (!initialized) {
            if (virtualWidth > 0 && virtualHeight > 0) {
                init(virtualWidth, virtualHeight);
            } else {
                return;
            }
        }

        worldManager.update(virtualWidth, virtualHeight);
        uiManager.update();

        // La cámara debe seguir al player CADA FRAME, después del update.
        worldManager.getCurrentWorld().centerCameraOn(player, virtualWidth, virtualHeight);
    }

    /**
     * Draw adaptado al pipeline de framebuffer virtual.
     *
     * @param g        Graphics2D del framebuffer virtual
     * @param viewport ViewportInfo actual para HUDs que transformen coordenadas
     */
    public void draw(Graphics2D g, ViewportInfo viewport) {
        if (!initialized) return;

        worldManager.draw(g);
        uiManager.draw(g);
    }

    // ── Accesores para sistemas externos ──────────────────────────────────

    public Player getPlayer() { return player; }
}
