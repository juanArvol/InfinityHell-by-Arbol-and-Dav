package Game.World.Core;

import Game.Enemys.AI.EnemyContext;
import Game.Engine.ContextualUpdatable;
import Game.Engine.GameObjects;
import Game.Player.Player;
import java.util.List;

/**
 * Helper de actualización contextual de objetos del mundo.
 *
 * ── HRFC-014 — GAP-7: Generalización del update con contexto ────────────
 *
 * ANTES:
 *   Este updater conocía Enemy directamente:
 *     if (object instanceof Enemy enemy) { enemy.update(EnemyContext.of(player)); }
 *
 *   Eso acoplaba World al tipo concreto Enemy. Cualquier objeto futuro que
 *   necesitara contexto (NPC, invocación, compañero, turreta) requería
 *   añadir más ramas instanceof aquí.
 *
 * AHORA:
 *   Usa la interfaz ContextualUpdatable del Engine:
 *     if (object instanceof ContextualUpdatable cu) { cu.updateWithContext(ctx); }
 *
 *   El contexto pasado es EnemyContext porque el mundo todavía gestiona
 *   principalmente enemigos. Enemy implementa ContextualUpdatable y hace
 *   cast internamente: (context instanceof EnemyContext ctx) ? ctx : null.
 *
 *   Futuras entidades con contexto propio (NPCContext, CompanionContext)
 *   implementan ContextualUpdatable y filtran el tipo de contexto que esperan.
 *   Este updater no necesita cambiar.
 *
 * ── ROL ──────────────────────────────────────────────────────────────────
 *   World inyecta este updater en WorldObjectsContainer:
 *     objects.setObjectUpdater(list -> WorldEnemyUpdater.updateAll(list, player));
 */
public final class WorldEnemyUpdater {

    private WorldEnemyUpdater() {}

    /**
     * Actualiza un objeto del mundo.
     *
     * Si el objeto implementa {@link ContextualUpdatable}, recibe el contexto
     * del player para que pueda usarlo en su lógica (IA, movimiento, ataques).
     * De lo contrario se actualiza con su update() normal.
     *
     * @param object objeto a actualizar
     * @param player jugador actual (puede ser null si no hay player en escena)
     */
    public static void update(GameObjects object, Player player) {
        if (object instanceof ContextualUpdatable cu) {
            Object ctx = (player != null) ? EnemyContext.of(player) : null;
            cu.updateWithContext(ctx);
        } else {
            object.update();
        }
    }

    /**
     * Actualiza todos los objetos del mundo.
     *
     * @param objects lista de objetos del mundo
     * @param player  jugador actual (puede ser null)
     */
    public static void updateAll(List<? extends GameObjects> objects, Player player) {
        for (GameObjects obj : objects) {
            update(obj, player);
        }
    }
}
