package Game.Engine;

/**
 * Contrato para objetos que necesitan un contexto externo en su update().
 *
 * ── HRFC — Unified DeltaTime Migration & Temporal Model Completion ────────
 *
 * MIGRACIÓN TEMPORAL:
 *   updateWithContext ahora recibe deltaTime como segundo parámetro para
 *   sistemas temporales framerate-independent.
 *
 * ── HRFC-014 — GAP-7: Generalización del update con contexto ────────────
 *
 * PROBLEMA ANTERIOR:
 *   WorldEnemyUpdater.update() era el único punto donde se decidía si un
 *   objeto recibía contexto en su actualización:
 *
 *     if (object instanceof Enemy enemy) {
 *         enemy.update(EnemyContext.of(player));
 *     } else {
 *         object.update();
 *     }
 *
 *   Esto acoplaba World y WorldEnemyUpdater al tipo concreto Enemy.
 *   Cualquier objeto futuro que necesitara contexto de update (NPC, invocación,
 *   turreta, compañero) requería añadir más ramas instanceof.
 *
 * SOLUCIÓN:
 *   ContextualUpdatable declara un único método:
 *
 *     void updateWithContext(Object context, double deltaTime)
 *
 *   Los objetos que necesiten contexto implementan esta interfaz.
 *   WorldObjectsContainer (o cualquier bucle de update) puede invocarla:
 *
 *     if (object instanceof ContextualUpdatable cu) {
 *         cu.updateWithContext(context, deltaTime);
 *     } else {
 *         object.update(deltaTime);
 *     }
 *
 *   El tipo de contexto es Object para que el Engine no dependa de
 *   EnemyContext ni de ningún tipo concreto del Game. El implementador
 *   hace cast internamente al tipo que espera.
 *
 * ── Uso en Enemy ──────────────────────────────────────────────────────────
 *
 *   public class Enemy extends MovingObjects implements ContextualUpdatable {
 *       {@literal @}Override
 *       public void updateWithContext(Object context, double deltaTime) {
 *           update((context instanceof EnemyContext ctx) ? ctx : null, deltaTime);
 *       }
 *   }
 *
 * ── Uso en NPC (futuro) ───────────────────────────────────────────────────
 *
 *   public class NPC extends MovingObjects implements ContextualUpdatable {
 *       {@literal @}Override
 *       public void updateWithContext(Object context, double deltaTime) {
 *           update((context instanceof NPCContext ctx) ? ctx : null, deltaTime);
 *       }
 *   }
 */
public interface ContextualUpdatable {

    /**
     * Actualiza el objeto con un contexto externo y deltaTime.
     *
     * ── HRFC — Unified DeltaTime Migration ───────────────────────────────
     *
     * El contexto es el estado del mundo relevante para este objeto
     * (posición del jugador, fase del mundo, tiempo, etc.).
     *
     * deltaTime es el tiempo del simulation step en segundos, necesario
     * para cualquier comportamiento temporal (cooldowns, timers, etc.).
     *
     * El implementador hace cast al tipo de contexto que espera.
     * Si el contexto es null o de tipo incorrecto, debe degradar a update() normal.
     *
     * @param context contexto externo para este frame. Puede ser null.
     * @param deltaTime tiempo del simulation step en segundos
     */
    void updateWithContext(Object context, double deltaTime);
}
