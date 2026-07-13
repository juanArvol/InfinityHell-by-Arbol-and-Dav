package Main.Bootstrap;

import Game.Engine.Camera.GameCamera;
import Game.Player.Player;
import Game.UI.Types.AmmoHUD;
import Game.UI.Types.CrossHairHUD;
import Game.UI.Types.LifeHUD;
import Game.UI.UIManager;
import java.util.function.Supplier;

/**
 * Bootstrap de la interfaz de usuario.
 *
 * Responsabilidad única: construir y registrar en UIManager todos los HUDs
 * que existen en la sesión de juego inicial.
 *
 * Lo que NO hace:
 *   - No coordina update() ni draw().
 *   - No conoce el mundo ni el WorldManager.
 *   - No decide si la UI está visible.
 *
 * Punto de extensión: cuando lleguen InventoryHUD, QuestHUD o MinimapHUD,
 * se añaden aquí. GameState no se modifica.
 *
 * ── HRFC-001: Supplier<GameCamera> ───────────────────────────────────────
 *
 * CrossHairHUD necesita la cámara para convertir coordenadas de mundo a
 * pantalla virtual. Se inyecta como Supplier<GameCamera> — la cámara del
 * Engine — para mantener el acoplamiento mínimo.
 *
 * El supplier típico es worldManager::getCamera, que devuelve siempre la
 * misma instancia de GameCamera (la cámara del Engine no se recrea entre
 * mundos, solo cambia su posición).
 */
public final class UIBootstrap {

    public UIBootstrap(UIManager uiManager,
                       Player player,
                       Supplier<GameCamera> cameraSupplier,
                       int virtualWidth,
                       int virtualHeight) {

        uiManager.add(new LifeHUD(player.getStats(), virtualWidth, virtualHeight));
        uiManager.add(new AmmoHUD(player.getCombat().getInventory(), virtualWidth, virtualHeight));
        uiManager.add(new CrossHairHUD(player, cameraSupplier, virtualWidth, virtualHeight));
    }
}
