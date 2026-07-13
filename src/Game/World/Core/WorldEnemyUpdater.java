package Game.World.Core;

import Game.Enemys.AI.EnemyContext;
import Game.Enemys.Enemy;
import Game.Engine.GameObjects;
import Game.Player.Player;
import java.util.List;

/**
 * Helper de actualización de enemigos con contexto.
 *
 * ── ROL ──────────────────────────────────────────────────────────────────
 * Desacopla el bucle de update de World.java del conocimiento de Enemy y Player.
 * World inyecta este helper en WorldObjectsContainer como objectUpdater:
 *
 *   objects.setObjectUpdater(list -> WorldEnemyUpdater.updateAll(list, player));
 *
 * Con esto, cada Enemy recibe EnemyContext correcto en cada frame, activando
 * la IA para perseguir al jugador. Objetos que no son Enemy se actualizan
 * con su update() normal.
 *
 * ── DISEÑO ───────────────────────────────────────────────────────────────
 * WorldObjectsContainer no puede importar Player ni Enemy directamente
 * (acoplaría un contenedor genérico a tipos concretos del juego). Este helper
 * centraliza ese conocimiento en un solo lugar del paquete Game.World.Core,
 * donde ya es aceptable que Player y Enemy sean conocidos.
 *
 * ── RESPONSABILIDADES ────────────────────────────────────────────────────
 *   updateAll()  — bucle principal: Enemy recibe EnemyContext, el resto update()
 */
public final class WorldEnemyUpdater {

    private WorldEnemyUpdater() {}

    /**
     * Actualiza un GameObject. Si es Enemy, le pasa EnemyContext del player.
     * Cualquier otro tipo se actualiza con su update() normal.
     *
     * @param object el objeto a actualizar
     * @param player el jugador actual (puede ser null — Enemy no actuará si es null)
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
     *
     * @param objects lista de objetos activos del mundo (ya flushada)
     * @param player  el jugador actual (puede ser null)
     */
    public static void updateAll(List<? extends GameObjects> objects, Player player) {
        for (GameObjects obj : objects) {
            update(obj, player);
        }
    }
}
