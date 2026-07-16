package Game.World.Core;

import Game.Enemys.AI.EnemyContext;
import Game.Enemys.Core.Enemy;
import Game.Engine.GameObjects;
import Game.Player.Player;
import java.util.List;

/**
 * Helper de actualización de enemigos con contexto.
 *
 * ── HRFC-005 ─────────────────────────────────────────────────────────────
 * Actualizado para referenciar Game.Enemys.Core.Enemy — el núcleo único
 * del nuevo framework. La lógica es idéntica: si el objeto es un Enemy,
 * le pasa EnemyContext; de lo contrario lo actualiza normalmente.
 *
 * ── ROL ──────────────────────────────────────────────────────────────────
 * Desacopla el bucle de update de World del conocimiento de Enemy y Player.
 * World inyecta este helper en WorldObjectsContainer como objectUpdater:
 *
 *   objects.setObjectUpdater(list -> WorldEnemyUpdater.updateAll(list, player));
 */
public final class WorldEnemyUpdater {

    private WorldEnemyUpdater() {}

    /**
     * Actualiza un GameObject. Si es Enemy, le pasa EnemyContext del player.
     * Cualquier otro tipo se actualiza con su update() normal.
     */
    public static void update(GameObjects object, Player player) {
        if (object instanceof Enemy enemy) {
            EnemyContext ctx = (player != null) ? EnemyContext.of(player) : null;
            enemy.update(ctx);
        } else {
            object.update();
        }
    }

    /**
     * Actualiza todos los objetos del mundo.
     * Los Enemy reciben EnemyContext; el resto reciben update() normal.
     */
    public static void updateAll(List<? extends GameObjects> objects, Player player) {
        for (GameObjects obj : objects) {
            update(obj, player);
        }
    }
}
