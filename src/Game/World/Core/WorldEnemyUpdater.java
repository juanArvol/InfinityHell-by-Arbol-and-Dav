package Game.World.Core;

import Game.Enemys.Enemy;
import Game.Enemys.AI.EnemyContext;
import Game.Engine.GameObjects;
import Game.Player.Player;

import java.util.List;

/**
 * Updater de enemigos con contexto — parche mínimo para World.
 *
 * ── POR QUÉ EXISTE ───────────────────────────────────────────────────────
 * World.update() llama gameObject.update() en bucle sin saber si es Enemy.
 * Enemy.update() sin contexto funciona (usa legacyPlayer si lo tiene),
 * pero para migrar completamente basta con que World.update() use este helper:
 *
 *   // En World.update(), reemplazar:
 *   object.update();
 *   // Por:
 *   WorldEnemyUpdater.update(object, player);
 *
 * Eso es el único cambio en World.java. Todo lo demás permanece igual.
 *
 * ── CUÁNDO ELIMINAR ESTE ARCHIVO ─────────────────────────────────────────
 * Cuando todas las subclases de Enemy hayan migrado a constructores sin Player
 * y World.update() pase EnemyContext directamente, este helper queda obsoleto
 * y se puede eliminar junto con el constructor @Deprecated de Enemy.
 */
public final class WorldEnemyUpdater {

    private WorldEnemyUpdater() {}

    /**
     * Actualiza un GameObject. Si es Enemy, le pasa el EnemyContext del player.
     * Cualquier otro tipo se actualiza normalmente.
     *
     * @param object el objeto del mundo a actualizar
     * @param player el jugador actual (puede ser null)
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
     * Actualiza toda la lista de objetos del mundo.
     * Reemplaza el bucle de update en World.update().
     *
     * Uso en World.java:
     *   WorldEnemyUpdater.updateAll(objects, player);
     */
    public static void updateAll(List<? extends GameObjects> objects, Player player) {
        for (GameObjects obj : objects) {
            update(obj, player);
        }
    }

    /**
     * Limpia objetos marcados para remoción (enemies muertos + WorldItems recogidos).
     * Centraliza la limpieza post-update.
     */
    public static void removeDeadObjects(List<GameObjects> objects) {
        objects.removeIf(obj -> {
            if (obj instanceof Enemy e) return e.isPendingRemoval();
            if (obj instanceof Game.World.WorldObjects.WorldItem wi) return wi.isPendingRemoval();
            return false;
        });
    }
}
