package Main.States;

import Display.Surface.LayerIndex;
import Display.Surface.RenderFrame;
import Game.Gameplay.Mechanics;
import Game.Player.Player;
import Game.UI.UIManager;
import Game.World.Core.WorldManager;
import Inputs.MouseInput;
import Main.Bootstrap.GameWorldBootstrap;
import Main.Bootstrap.UIBootstrap;
import Main.Debug.DebugGameSettings;
import Main.Debug.FpsOverlay;

/**
 * Estado del juego — fachada ligera del gameplay.
 *
 * RESPONSABILIDADES QUE PERMANECEN:
 *   - Punto de entrada del gameplay (instanciado por GameOrquester).
 *   - Dueño del ciclo update(): delega en worldManager y uiManager.
 *   - Dueño del ciclo draw(): delega en worldManager, uiManager y fpsOverlay.
 *   - Gestión de resize y FPS counter.
 *
 * RESPONSABILIDADES QUE SE DELEGARON:
 *   - Construcción del mundo, Player y spawn inicial → GameWorldBootstrap.
 *   - Construcción y registro de HUDs          → UIBootstrap.
 *   - Renderizado del overlay de FPS           → FpsOverlay.
 *   - Gestión de cámara                        → WorldManager + GameCamera.
 *
 * REGLA DE ORO:
 *   Si en el futuro un nuevo sistema (Inventory, Quest, Audio) necesita
 *   inicializarse, NO se añade aquí. Se crea un nuevo Bootstrap o se extiende
 *   uno existente. GameState solo conoce los sistemas que necesita en runtime
 *   (worldManager, uiManager, player, fpsOverlay).
 */
public class GameState {

    // ── Runtime dependencies ─────────────────────────────────────────────────
    private final DebugGameSettings settings;
    private final WorldManager worldManager;
    private final UIManager    uiManager;
    private final Player       player;
    private final FpsOverlay   fpsOverlay;

    private int fpsPorSegundo = 0;
    private int virtualWidth;
    private int virtualHeight;

    public GameState(int virtualWidth, int virtualHeight) {
        this.virtualWidth  = virtualWidth;
        this.virtualHeight = virtualHeight;

        // ── Settings ─────────────────────────────────────────────────────────
        this.settings = DebugGameSettings.getInstance();
        settings.setFpsEnabled(true);

        // ── Registros globales de gameplay ────────────────────────────────────
        // AmuletRegistry y WeaponRegistry deben inicializarse ANTES de que
        // GameWorldBootstrap construya el Player (que crea ModifiedWeapon) y antes
        // de que se inyecte el entityProvider desde GameWorldBootstrap.
        Game.Items.Types.Ammulets.AmuletRegistry.init();
        Game.Items.Types.Ammulets.AmuletRegistry.registerDefaults();
        Game.Items.Types.Weapons.WeaponRegistry.init();
        Game.Items.Types.Weapons.WeaponRegistry.registerDefaults();

        // ── World (instancia directa, sin singleton) ──────────────────────────
        this.worldManager = new WorldManager(virtualWidth, virtualHeight, settings);

        // ── World bootstrap: Player, cámara, tracked object, spawn inicial ───
        GameWorldBootstrap worldBootstrap = new GameWorldBootstrap(
            worldManager, virtualWidth, virtualHeight
        );
        this.player = worldBootstrap.getPlayer();

        // ── UI bootstrap: UIManager + HUDs ────────────────────────────────────
        this.uiManager = new UIManager(virtualWidth, virtualHeight);
        // El cameraSupplier es lazy: evalúa la cámara del Engine cada frame.
        // CrossHairHUD lo usa para convertir coordenadas de mundo → pantalla virtual.
        // HRFC-001: la cámara ya viene de WorldManager.getCamera() (GameCamera),
        // no de World.getCamera(). El supplier retorna siempre la misma instancia
        // porque GameCamera es la entidad única del Engine.
        new UIBootstrap(uiManager, player,
                worldManager::getCamera,
                virtualWidth, virtualHeight);

        // ── Debug overlay ─────────────────────────────────────────────────────
        this.fpsOverlay = new FpsOverlay();
    }

    // ── Resize ────────────────────────────────────────────────────────────────

    public void onVirtualDimensionsChanged(int newVirtualWidth, int newVirtualHeight) {
        this.virtualWidth  = newVirtualWidth;
        this.virtualHeight = newVirtualHeight;
        worldManager.onVirtualResize(newVirtualWidth, newVirtualHeight);
        uiManager.onResize(newVirtualWidth, newVirtualHeight);
    }

    // ── Update ────────────────────────────────────────────────────────────────

    public void update() {
        Mechanics.updateMechanics(player);
        worldManager.update();
        uiManager.update();
    }

    // ── Draw ──────────────────────────────────────────────────────────────────

    /**
     * Dibuja cada subsistema en su capa correspondiente del RenderFrame.
     *
     * ── HRFC-001: sistema de capas ────────────────────────────────────────
     *
     * Cada subsistema dibuja en su LayerIndex. El orden visual está declarado
     * en LayerIndex.ordinal(), no en el orden de llamada aquí. Cambiar el orden
     * visual significa cambiar LayerIndex, no este método.
     *
     *   WORLD_ENTITIES → mundo (entidades, tiles, efectos de mundo).
     *   HUD            → interfaz de usuario (vida, munición, crosshair).
     *   OVERLAY        → overlays informativos (FPS counter).
     *
     * GameLoop llama frame.flushLayers() tras este método para componer las
     * capas sobre el framebuffer antes de present().
     */
    public void draw(RenderFrame frame) {
        // Mundo: entidades, tiles, efectos. Con transformación de cámara.
        worldManager.draw(frame.getLayerGraphics(LayerIndex.WORLD_ENTITIES));

        // HUD: vida, munición, crosshair. Sin transformación de cámara.
        uiManager.draw(frame.getLayerGraphics(LayerIndex.HUD));

        // Overlay: FPS counter y otros debug informativos.
        if (settings.isFpsEnabled()) {
            fpsOverlay.draw(frame.getLayerGraphics(LayerIndex.OVERLAY), fpsPorSegundo);
        }
    }

    public void setFps(int fps) {
        this.fpsPorSegundo = fps;
    }

    /** Libera recursos (ExecutorService del WorldManager). Llamar al cerrar la aplicación. */
    public void shutdown() {
        worldManager.shutdown();
    }

    /**
     * Registra el combat del player como listener de eventos de ratón.
     * Debe llamarse desde GameOrquester después de crear GameState,
     * pasando la instancia de MouseInput.
     */
    public void registerMouseInput(MouseInput mouse) {
        mouse.addMouseActionListener(player.getCombat());
    }

    /**
     * Desregistra el combat del player como listener de eventos de ratón.
     * Debe llamarse desde GameOrquester antes de destruir GameState o al
     * cambiar de estado, para evitar que el antiguo PlayerCombat siga
     * recibiendo eventos de click después de que el estado fue reemplazado.
     *
     * CONTRATO:
     *   registerMouseInput(mouse)   → al activar el GameState
     *   unregisterMouseInput(mouse) → al desactivar o destruir el GameState
     */
    public void unregisterMouseInput(MouseInput mouse) {
        mouse.removeMouseActionListener(player.getCombat());
    }
}
