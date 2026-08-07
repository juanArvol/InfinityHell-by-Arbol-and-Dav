package Game.Engine;

/**
 * Contrato de objetos con ciclo de vida finito que pueden marcarse para
 * eliminación desde fuera del objeto.
 *
 * ── MIGRACIÓN ─────────────────────────────────────────────────────────────
 * Esta interfaz existía como clase interna de WorldObjectsContainer:
 *   WorldObjectsContainer.Destroyable
 *
 * Se mueve al Engine para romper la dependencia de Bullet, Enemy y WorldItem
 * sobre WorldObjectsContainer. Ahora cualquier sistema puede comprobar si
 * un objeto debe eliminarse sin conocer el contenedor donde vive.
 *
 * ── COMPATIBILIDAD ────────────────────────────────────────────────────────
 * WorldObjectsContainer.Destroyable se mantendrá como alias deprecated
 * durante la transición:
 *
 *   @Deprecated
 *   public interface Destroyable extends Game.Engine.Destroyable {}
 *
 * Así Bullet, Enemy y WorldItem no necesitan cambios hasta la Etapa 9.
 *
 * ── CONTRATO ──────────────────────────────────────────────────────────────
 * isPendingDestruction() debe ser idempotente y thread-safe para lectura.
 * Una vez que retorna true, no debe volver a retornar false en la misma
 * instancia (el objeto está "muerto" y será removido en el próximo flush).
 */
public interface Destroyable {

    /**
     * @return true si este objeto debe ser eliminado del mundo en el
     *         próximo ciclo de limpieza.
     */
    boolean isPendingDestruction();
}
