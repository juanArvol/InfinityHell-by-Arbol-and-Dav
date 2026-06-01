package Main.Bootstrap;

import Game.Player.Player;
import Game.UI.UIManager;
import Game.UI.Types.AmmoHUD;
import Game.UI.Types.CrossHairHUD;
import Game.UI.Types.LifeHUD;

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
 */
public final class UIBootstrap {

    public UIBootstrap(UIManager uiManager,
                       Player player,
                       int virtualWidth,
                       int virtualHeight) {

        // LifeHUD recibe HealthView — player.getStats() la implementa delegando
        // en HealthComponent. Cadena: LifeHUD → PlayerStats → HealthComponent.
        uiManager.add(new LifeHUD(player.getStats(), virtualWidth, virtualHeight));
        uiManager.add(new AmmoHUD(player.getCombat().getInventory(), virtualWidth, virtualHeight));
        uiManager.add(new CrossHairHUD(player, virtualWidth, virtualHeight));
    }
}
