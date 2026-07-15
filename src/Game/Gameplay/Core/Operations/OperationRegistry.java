package Game.Gameplay.Core.Operations;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Registro de GameplayOperation con prioridad ordenada y predicados de activación.
 *
 * ── RESPONSABILIDAD ÚNICA ─────────────────────────────────────────────────
 * OperationRegistry responde a una sola pregunta:
 *
 *   "Dado un OperationContext, ¿qué operaciones deben ejecutarse?"
 *
 * Permite que múltiples sistemas registren operaciones independientemente
 * sin conocerse entre sí, exactamente igual que GameplayEventChannel permite
 * múltiples interceptores de eventos.
 *
 *   GameplayEventChannel  → interceptores de eventos (pre-acción, cancelables)
 *   InfluenceRegistry     → transformadores de modificadores (durante resolución)
 *   OperationRegistry     → consecuencias de mundo (post-resolución, ejecutables)
 *
 * ── PRIORIDAD ────────────────────────────────────────────────────────────
 * Menor valor = se ejecuta primero. Rango recomendado: 0–1000.
 *
 *   0–99   : operaciones críticas de mundo (destrucción de entidades, muerte)
 *   100–299: operaciones de estado (freeze, ignite, stun)
 *   300–499: operaciones de modificadores secundarios (rebuff, propagación)
 *   500–699: operaciones de feedback (partículas, sonido, animación)
 *   700+   : operaciones de logging y diagnóstico
 *
 * ── PREDICADOS ────────────────────────────────────────────────────────────
 * Cada operación registrada puede tener un OperationPredicate asociado.
 * Si el predicado retorna false para un contexto dado, la operación no se
 * ejecuta. Si no se especifica predicado, se usa ALWAYS.
 *
 * ── ORDEN DETERMINÍSTICO ─────────────────────────────────────────────────
 * Las operaciones con la misma prioridad se ejecutan en el orden en que
 * fueron registradas (FIFO dentro del mismo nivel de prioridad).
 * La ordenación es estable.
 *
 * ── MÚLTIPLES REGISTROS ──────────────────────────────────────────────────
 * Distintos sistemas pueden registrar operaciones en el mismo OperationRegistry
 * sin interferirse:
 *
 *   // El sistema de física registra FreezeMovementOperation:
 *   registry.register(100, "freeze_physics", freezeMovementOp);
 *
 *   // El sistema de animación registra PlayFreezeAnimation:
 *   registry.register(500, "freeze_anim", playFreezeAnimOp);
 *
 *   // El sistema de sonido registra PlayFreezeSound:
 *   registry.register(600, "freeze_sound", playFreezeSoundOp);
 *
 *   // Al disparar, todas se ejecutan en orden:
 *   registry.execute(ctx);
 *
 * ── INSTANCIABILIDAD ─────────────────────────────────────────────────────
 * OperationRegistry es instanciable. No hay singleton global.
 * Cada sistema que necesite disparar operaciones tiene su propio registro,
 * o sistemas comparten registros explícitamente pasados por referencia.
 *
 * ── TAG DE IDENTIFICACIÓN ────────────────────────────────────────────────
 * Las operaciones se registran con un tag de identificación para poder
 * darlas de baja cuando el efecto que las requiere termina:
 *
 *   registry.register(100, "freeze_physics", freezeMovementOp);
 *   // cuando el sistema de freeze se desactiva:
 *   registry.unregister("freeze_physics");
 *
 * ── THREAD SAFETY ────────────────────────────────────────────────────────
 * No es thread-safe. Usar desde el game loop thread.
 *
 * @see GameplayOperation
 * @see OperationPredicate
 * @see OperationContext
 */
public final class OperationRegistry {

    /** Entrada interna: prioridad + tag + predicado + operación. */
    private static final class Entry {
        final int                priority;
        final String             tag;
        final OperationPredicate predicate;
        final GameplayOperation  operation;
        /** Número de secuencia para orden determinístico FIFO en misma prioridad. */
        final int                sequence;

        Entry(int priority, String tag, OperationPredicate predicate,
              GameplayOperation operation, int sequence) {
            this.priority  = priority;
            this.tag       = tag;
            this.predicate = predicate;
            this.operation = operation;
            this.sequence  = sequence;
        }
    }

    private final List<Entry> entries  = new ArrayList<>();
    private boolean           sorted   = true;
    private int               sequence = 0;

    // ── Registro ─────────────────────────────────────────────────────────

    /**
     * Registra una operación con prioridad, tag de identificación y predicado.
     *
     * @param priority  orden de ejecución (menor = primero)
     * @param tag       identificador único para poder dar de baja la operación
     * @param predicate condición de activación (null equivale a ALWAYS)
     * @param operation operación a ejecutar cuando el predicado sea true
     * @throws IllegalArgumentException si tag u operation son null
     */
    public void register(int priority, String tag,
                         OperationPredicate predicate, GameplayOperation operation) {
        if (tag == null || tag.isBlank())
            throw new IllegalArgumentException("tag no puede ser null o vacío.");
        if (operation == null)
            throw new IllegalArgumentException("operation no puede ser null.");

        OperationPredicate effectivePredicate =
            (predicate != null) ? predicate : OperationPredicate.ALWAYS;

        entries.add(new Entry(priority, tag, effectivePredicate, operation, sequence++));
        sorted = false;
    }

    /**
     * Registra una operación con prioridad y tag, sin predicado (equivale a ALWAYS).
     *
     * @param priority  orden de ejecución (menor = primero)
     * @param tag       identificador único
     * @param operation operación a registrar
     */
    public void register(int priority, String tag, GameplayOperation operation) {
        register(priority, tag, OperationPredicate.ALWAYS, operation);
    }

    /**
     * Registra una operación con prioridad por defecto (500), sin predicado.
     *
     * @param tag       identificador único
     * @param operation operación a registrar
     */
    public void register(String tag, GameplayOperation operation) {
        register(500, tag, OperationPredicate.ALWAYS, operation);
    }

    /**
     * Da de baja todas las operaciones con el tag indicado.
     * Si el tag no existe, la operación no tiene efecto.
     *
     * @param tag tag de las operaciones a eliminar
     */
    public void unregister(String tag) {
        entries.removeIf(e -> e.tag.equals(tag));
    }

    /**
     * Elimina todas las operaciones registradas.
     * Llamar al cambiar de escena o al destruir el sistema propietario.
     */
    public void clear() {
        entries.clear();
        sorted   = true;
        sequence = 0;
    }

    // ── Ejecución ─────────────────────────────────────────────────────────

    /**
     * Evalúa cada operación registrada contra el contexto y ejecuta las que
     * superan su predicado, en orden de prioridad.
     *
     * Si el registro está vacío, no hace nada.
     *
     * @param context contexto de operación a evaluar
     */
    public void execute(OperationContext context) {
        if (entries.isEmpty()) return;
        if (context == null) return;
        ensureSorted();
        for (Entry e : entries) {
            if (e.predicate.test(context)) {
                e.operation.execute(context);
            }
        }
    }

    /**
     * Evalúa las operaciones y ejecuta solo las que superan el predicado del
     * registro Y el predicado adicional {@code guard} proporcionado por el caller.
     *
     * Útil cuando el caller quiere filtrar adicionalmente sin modificar el registro:
     *
     *   registry.executeFiltered(ctx,
     *       ctx2 -> ctx2.getTarget() != null); // solo ejecutar si hay target
     *
     * @param context contexto de operación
     * @param guard   predicado adicional a evaluar antes de cada operación
     */
    public void executeFiltered(OperationContext context, OperationPredicate guard) {
        if (entries.isEmpty()) return;
        if (context == null) return;
        if (guard == null) { execute(context); return; }
        ensureSorted();
        for (Entry e : entries) {
            if (guard.test(context) && e.predicate.test(context)) {
                e.operation.execute(context);
            }
        }
    }

    /**
     * Retorna la lista de operaciones cuyo predicado evalúa true para el contexto
     * dado, en orden de prioridad. No ejecuta nada.
     *
     * Útil para inspección, debugging, o ejecución diferida.
     *
     * @param context contexto a evaluar
     * @return lista de operaciones que aplican (puede estar vacía)
     */
    public List<GameplayOperation> matching(OperationContext context) {
        if (entries.isEmpty() || context == null) return List.of();
        ensureSorted();
        List<GameplayOperation> result = new ArrayList<>();
        for (Entry e : entries) {
            if (e.predicate.test(context)) {
                result.add(e.operation);
            }
        }
        return result;
    }

    // ── Consultas ─────────────────────────────────────────────────────────

    /**
     * True si hay al menos una operación registrada.
     */
    public boolean hasOperations() {
        return !entries.isEmpty();
    }

    /**
     * Número de operaciones registradas.
     */
    public int size() {
        return entries.size();
    }

    /**
     * True si existe alguna operación registrada bajo el tag dado.
     *
     * @param tag tag a buscar
     */
    public boolean hasTag(String tag) {
        for (Entry e : entries) {
            if (e.tag.equals(tag)) return true;
        }
        return false;
    }

    // ── Orden interno ─────────────────────────────────────────────────────

    /**
     * Garantiza orden por prioridad ascendente.
     * Dentro del mismo nivel de prioridad, mantiene el orden de inserción (FIFO)
     * usando el número de secuencia como criterio secundario.
     */
    private void ensureSorted() {
        if (!sorted) {
            entries.sort(
                Comparator.comparingInt((Entry e) -> e.priority)
                          .thenComparingInt(e -> e.sequence)
            );
            sorted = true;
        }
    }

    // ── Object ────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "OperationRegistry[" + entries.size() + " operations]";
    }
}
