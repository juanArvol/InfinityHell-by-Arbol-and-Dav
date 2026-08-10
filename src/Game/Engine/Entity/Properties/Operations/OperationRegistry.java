package Game.Engine.Entity.Properties.Operations;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Registro de GameplayOperation con prioridad ordenada y predicados de activación.
 *
 * ── RESPONSABILIDAD ÚNICA ─────────────────────────────────────────────────
 * OperationRegistry responde a:
 *   "Dado un OperationContext, ¿qué operaciones deben ejecutarse?"
 *
 * ── PRIORIDAD ────────────────────────────────────────────────────────────
 * Menor valor = se ejecuta primero.
 *   0–99   : operaciones críticas de mundo (destrucción de entidades, muerte)
 *   100–299: operaciones de estado (freeze, ignite, stun)
 *   300–499: operaciones de modificadores secundarios
 *   500–699: operaciones de feedback (partículas, sonido, animación)
 *   700+   : operaciones de logging y diagnóstico
 *
 * ── THREAD SAFETY ────────────────────────────────────────────────────────
 * No es thread-safe. Usar desde el game loop thread.
 */
public final class OperationRegistry {

    private static final class Entry {
        final int                priority;
        final String             tag;
        final OperationPredicate predicate;
        final GameplayOperation  operation;
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

    public void register(int priority, String tag, GameplayOperation operation) {
        register(priority, tag, OperationPredicate.ALWAYS, operation);
    }

    public void register(String tag, GameplayOperation operation) {
        register(500, tag, OperationPredicate.ALWAYS, operation);
    }

    public void unregister(String tag) {
        entries.removeIf(e -> e.tag.equals(tag));
    }

    public void clear() {
        entries.clear();
        sorted   = true;
        sequence = 0;
    }

    // ── Ejecución ─────────────────────────────────────────────────────────

    public void execute(OperationContext context) {
        if (entries.isEmpty() || context == null) return;
        ensureSorted();
        for (Entry e : entries) {
            if (e.predicate.test(context)) {
                e.operation.execute(context);
            }
        }
    }

    public void executeFiltered(OperationContext context, OperationPredicate guard) {
        if (entries.isEmpty() || context == null) return;
        if (guard == null) { execute(context); return; }
        ensureSorted();
        for (Entry e : entries) {
            if (guard.test(context) && e.predicate.test(context)) {
                e.operation.execute(context);
            }
        }
    }

    public List<GameplayOperation> matching(OperationContext context) {
        if (entries.isEmpty() || context == null) return List.of();
        ensureSorted();
        List<GameplayOperation> result = new ArrayList<>();
        for (Entry e : entries) {
            if (e.predicate.test(context)) result.add(e.operation);
        }
        return result;
    }

    // ── Consultas ─────────────────────────────────────────────────────────

    public boolean hasOperations()    { return !entries.isEmpty(); }
    public int size()                 { return entries.size(); }

    public boolean hasTag(String tag) {
        for (Entry e : entries) { if (e.tag.equals(tag)) return true; }
        return false;
    }

    private void ensureSorted() {
        if (!sorted) {
            entries.sort(
                Comparator.comparingInt((Entry e) -> e.priority)
                          .thenComparingInt(e -> e.sequence)
            );
            sorted = true;
        }
    }

    @Override
    public String toString() {
        return "OperationRegistry[" + entries.size() + " operations]";
    }
}
