package Game.Engine.Entity.Stats.Modifier;

import Game.Engine.Entity.Stats.StatModifier;
import Game.Engine.Entity.Stats.StatTarget;

/**
 * Administrador de modificadores de un único StatTarget.
 *
 * ── HRFC-011 — Consolidación Final del Modelo de Contribuciones ──────────
 *
 * ModifierBucket es un detalle completamente interno del motor de stats.
 * No forma parte de ninguna API pública — RuntimeStats lo usa a través de
 * ModifierContainer, y el código de gameplay nunca lo ve.
 *
 * ── Responsabilidades ─────────────────────────────────────────────────────
 *   ✓ Mantener la colección de modificadores del target.
 *   ✓ Añadir modificadores.
 *   ✓ Eliminar todos los modificadores de una fuente (removeBySource).
 *   ✓ Mantener la caché del resultado calculado (dirty flag).
 *   ✓ Calcular el valor efectivo aplicando todos los modificadores en orden
 *     de prioridad definido por ModifierOperation.priority().
 *
 * ── Lo que ModifierBucket NO hace ────────────────────────────────────────
 *   ✗ No conoce otros StatTargets.
 *   ✗ No conoce el valor base de la entidad.
 *   ✗ No implementa ninguna matemática propia — delega en ModifierOperation.
 *   ✗ No conoce la existencia de RuntimeStats, EntityStats ni Enemy.
 *   ✗ No administra identificadores de modificadores individuales.
 *   ✗ No conoce StatContributor — trabaja con source:Object por identidad ==.
 *
 * ── Mecanismo de revocación ───────────────────────────────────────────────
 *   Cada StatModifier lleva un campo source que apunta al objeto del dominio
 *   propietario de esa contribución. En el modelo StatContributor (HRFC-011),
 *   la fuente siempre es el contributor mismo.
 *
 *   removeBySource(source) recorre el bucket y elimina todos los StatModifiers
 *   cuya referencia == source. Esto retira en un solo paso todas las
 *   contribuciones de un StatusEffect, una fase, un arma o cualquier otro
 *   contributor que haya expirado.
 *
 * ── Orden de evaluación ───────────────────────────────────────────────────
 *   1. Ordenar modificadores por ModifierOperation.priority() (menor = primero).
 *   2. Aplicar acumulativamente con apply(accumulator, value).
 *   3. Si alguna operación tiene isOverriding()=true, el último valor de
 *      override retornado por apply() sustituye el acumulador final.
 *
 * ── Rendimiento ───────────────────────────────────────────────────────────
 *   - evaluate(base) con dirty=false retorna el resultado cacheado: O(1).
 *   - evaluate(base) con dirty=true itera únicamente los modificadores de
 *     este target (típicamente 0–3): O(k) donde k es mínimo.
 *   - add/removeBySource marcan dirty=true y no recalculan en ese momento.
 *   - Cero allocations durante evaluate() en steady-state.
 *   - La ordenación solo ocurre cuando el bucket está dirty.
 */
public final class ModifierBucket {

    private static final int INITIAL_CAPACITY = 4;

    /** Target que este bucket administra. Solo para diagnóstico. */
    private final StatTarget target;

    /** Modificadores activos. Capacidad inicial pequeña para minimizar memoria. */
    private StatModifier[] modifiers;
    private int size;

    /** Resultado cacheado. Válido solo cuando dirty=false. */
    private double cachedResult;

    /** true si el bucket fue modificado y cachedResult no es válido. */
    private boolean dirty = true;

    /**
     * true si el array necesita re-ordenarse por prioridad antes de evaluar.
     * Se activa al añadir un nuevo modificador.
     */
    private boolean needsSort = false;

    // ── Constructor ───────────────────────────────────────────────────────

    public ModifierBucket(StatTarget target) {
        this.target    = target;
        this.modifiers = new StatModifier[INITIAL_CAPACITY];
        this.size      = 0;
    }

    // ── API pública ───────────────────────────────────────────────────────

    /**
     * Añade un modificador a este bucket.
     *
     * <p>A diferencia de la versión anterior, no se reemplaza ningún
     * modificador existente: una misma fuente puede aportar múltiples
     * modificadores sobre el mismo target y todos se acumulan.
     * El ciclo de vida de estos modificadores está ligado a su fuente:
     * desaparecen al llamar {@link #removeBySource(Object)}.
     *
     * <p>Coste: O(1) amortizado.
     *
     * @param modifier modificador a añadir. No debe ser null.
     */
    public void add(StatModifier modifier) {
        if (modifier == null) return;
        ensureCapacity();
        modifiers[size++] = modifier;
        dirty     = true;
        needsSort = true;
    }

    /**
     * Elimina todos los modificadores cuya fuente sea el objeto dado.
     *
     * <p>La comparación usa identidad de referencia (==). En el modelo
     * StatContributor (HRFC-011), la fuente siempre es el contributor
     * propietario de las contribuciones: un StatusEffect, un arma,
     * una fase de boss, una habilidad pasiva...
     *
     * <p>Coste: O(k) donde k = modificadores activos en este bucket.
     * Ocurre solo al expirar una fuente, nunca en steady-state.
     *
     * @param source objeto propietario de los modificadores a eliminar.
     *               Usa identidad (==). Si null, no hace nada.
     */
    public void removeBySource(Object source) {
        if (source == null || size == 0) return;
        boolean changed = false;
        // Recorrer de atrás hacia adelante para compactar sin invalidar índices
        for (int i = size - 1; i >= 0; i--) {
            if (modifiers[i].getSource() == source) {
                modifiers[i] = modifiers[--size];
                modifiers[size] = null; // liberar referencia para GC
                changed = true;
            }
        }
        if (changed) dirty = true;
    }

    /** True si el bucket no tiene modificadores activos. */
    public boolean isEmpty() {
        return size == 0;
    }

    /** Número de modificadores activos en este bucket. */
    public int size() {
        return size;
    }

    /** Elimina todos los modificadores del bucket. */
    public void clear() {
        for (int i = 0; i < size; i++) modifiers[i] = null;
        size  = 0;
        dirty = true;
    }

    /**
     * Calcula y retorna el valor efectivo de la estadística.
     *
     * <p>Si el bucket no fue modificado desde la última llamada, retorna el
     * resultado cacheado en O(1) sin ningún cálculo adicional.
     *
     * <p>Si el bucket fue modificado (dirty=true), recalcula aplicando todos
     * los modificadores en orden de prioridad y cachea el resultado.
     *
     * <p>Orden de evaluación garantizado:
     * <ol>
     *   <li>Acumular en orden de prioridad (FLAT → MULTIPLIER → OVERRIDE).</li>
     *   <li>Si alguna operación tiene isOverriding()=true, el valor que
     *       devolvió apply() para el último override sustituye el resultado.</li>
     * </ol>
     *
     * @param base valor base de la estadística (de EntityStats).
     * @return valor efectivo con todos los modificadores aplicados.
     */
    public double evaluate(double base) {
        if (!dirty) return cachedResult;

        if (needsSort && size > 1) {
            sortByPriority();
            needsSort = false;
        }

        double accumulator = base;
        double lastOverride = Double.NaN;

        for (int i = 0; i < size; i++) {
            StatModifier mod = modifiers[i];
            ModifierOperation op = mod.getOperation();
            double applied = op.apply(accumulator, mod.getValue());
            if (op.isOverriding()) {
                lastOverride = applied;
            } else {
                accumulator = applied;
            }
        }

        cachedResult = Double.isNaN(lastOverride) ? accumulator : lastOverride;
        dirty = false;
        return cachedResult;
    }

    /**
     * Invalida la caché manualmente.
     * Útil cuando el valor base de EntityStats cambia fuera del bucket.
     */
    public void invalidate() {
        dirty = true;
    }

    // ── Diagnóstico ───────────────────────────────────────────────────────

    /** StatTarget que este bucket administra (solo para logs/debug). */
    public StatTarget getTarget() { return target; }

    @Override
    public String toString() {
        return "ModifierBucket[" + target + ", size=" + size + ", dirty=" + dirty + "]";
    }

    // ── Internos ──────────────────────────────────────────────────────────

    /** Insertion sort por prioridad. Eficiente para k pequeño (típicamente 0–3). */
    private void sortByPriority() {
        for (int i = 1; i < size; i++) {
            StatModifier key = modifiers[i];
            int keyPriority  = key.getOperation().priority();
            int j = i - 1;
            while (j >= 0 && modifiers[j].getOperation().priority() > keyPriority) {
                modifiers[j + 1] = modifiers[j];
                j--;
            }
            modifiers[j + 1] = key;
        }
    }

    /** Duplica la capacidad del array si está lleno. */
    private void ensureCapacity() {
        if (size < modifiers.length) return;
        StatModifier[] newArray = new StatModifier[modifiers.length * 2];
        System.arraycopy(modifiers, 0, newArray, 0, size);
        modifiers = newArray;
    }
}
