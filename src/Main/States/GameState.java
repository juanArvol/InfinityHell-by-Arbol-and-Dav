package Main.States;

import Game.Player.Player;
import Game.UI.UIManager;
import Game.World.Core.WorldManager;
import Main.Bootstrap.GameWorldBootstrap;
import Main.Bootstrap.UIBootstrap;
import Main.Debug.DebugGameSettings;
import Main.Debug.FpsOverlay;

import java.awt.Graphics2D;

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
        // FIX A-04: el bloque draw() comprueba isFpsEnabled(). Llamar
        // setDebugEnabled no activa ese flag — son configuraciones distintas.
        settings.setFpsEnabled(true);

        // ── World (instancia directa, sin singleton) ──────────────────────────
        this.worldManager = new WorldManager(virtualWidth, virtualHeight, settings);

        // ── World bootstrap: Player, cámara, tracked object, spawn inicial ───
        GameWorldBootstrap worldBootstrap = new GameWorldBootstrap(
            worldManager, virtualWidth, virtualHeight
        );
        this.player = worldBootstrap.getPlayer();

        // ── UI bootstrap: UIManager + HUDs ────────────────────────────────────
        this.uiManager = new UIManager(virtualWidth, virtualHeight);
        new UIBootstrap(uiManager, player, virtualWidth, virtualHeight);

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
        worldManager.update(virtualWidth, virtualHeight);
        uiManager.update();
        worldManager.getCurrentWorld().centerCameraOn(player, virtualWidth, virtualHeight);
    }

    // ── Draw ──────────────────────────────────────────────────────────────────

    /**
     * FIX B-02: el parámetro ViewportInfo fue eliminado — se recibía pero nunca
     * se usaba ni se pasaba a ningún subsistema. GameLoop lo pasaba con
     * display.getViewport() sin ningún efecto. Si en el futuro se necesita
     * (safe areas, letterbox en UI), se añade de vuelta con uso real.
     */
    public void draw(Graphics2D g) {
        worldManager.draw(g);
        uiManager.draw(g);

        if (settings.isFpsEnabled()) {
            fpsOverlay.draw(g, fpsPorSegundo);
        }
    }

    public void setFps(int fps) {
        this.fpsPorSegundo = fps;
    }
}
