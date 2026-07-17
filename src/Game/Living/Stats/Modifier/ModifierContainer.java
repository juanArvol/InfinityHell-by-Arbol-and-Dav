package Game.Living.Stats.Modifier;

import Game.Living.Stats.StatModifier;
import Game.Living.Stats.StatTarget;

/**
 * Contenedor top-level de todos los ModifierBuckets de una entidad.
 *
 * ── HRFC-011 — Consolidación Final del Modelo de Contribuciones ──────────
 *
 * ModifierContainer es un detalle completamente interno del motor de stats.
 * RuntimeStats lo posee y lo usa — el código de gameplay nunca lo ve.
 * La API pública del motor trabaja con StatContributor; ModifierContainer
 * trabaja con StatModifier y source:Object, que son detalles de implementación.
 *
 * ── Responsabilidades ─────────────────────────────────────────────────────
 *   ✓ Poseer todos los ModifierBuckets, uno por StatTarget.
 *   ✓ Añadir modificadores al bucket correcto.
 *   ✓ Eliminar todos los modificadores de una fuente (removeBySource).
 *   ✓ Proporcionar acceso O(1) a cualquier bucket por StatTarget.
 *   ✓ Propagar invalidación de caché cuando el base cambia externamente.
 *
 * ── Lo que ModifierContainer NO hace ────────────────────────────────────
 *   ✗ No calcula valores de estadísticas.
 *   ✗ No conoce EntityStats ni los valores base.
 *   ✗ No conoce la existencia de RuntimeStats ni de ninguna entidad concreta.
 *   ✗ No conoce StatContributor — trabaja con source:Object por identidad ==.
 *   ✗ No administra identificadores de modificadores individuales.
 *
 * ── Mecanismo de revocación ───────────────────────────────────────────────
 *   RuntimeStats.revoke(contributor) llama removeBySource(contributor).
 *   ModifierContainer propaga la llamada a todos sus buckets, que eliminan
 *   por identidad == los StatModifiers cuya fuente sea ese contributor.
 *
 * ── Arquitectura interna ─────────────────────────────────────────────────
 *   Array fijo de ModifierBucket indexado por StatTarget.ordinal().
 *   Todos los buckets se pre-alocan en el constructor → acceso O(1)
 *   garantizado sin ninguna búsqueda de hash.
 *
 * ── Rendimiento ───────────────────────────────────────────────────────────
 *   - Cero allocations por frame en steady-state.
 *   - addModifier: O(1) amortizado.
 *   - removeBySource: O(T × k) donde T = targets (constante ~20), k = mods/bucket.
 *     Ocurre solo al expirar una fuente, no en cada frame.
 */
public final class ModifierContainer {

    /** Un bucket pre-alocado por cada valor de StatTarget. */
    private final ModifierBucket[] buckets;

    // ── Constructor ───────────────────────────────────────────────────────

    public ModifierContainer() {
        StatTarget[] targets = StatTarget.values();
        buckets = new ModifierBucket[targets.length];
        for (int i = 0; i < targets.length; i++) {
            buckets[i] = new ModifierBucket(targets[i]);
        }
    }

    // ── Acceso a buckets ──────────────────────────────────────────────────

    /**
     * Retorna el bucket correspondiente al StatTarget dado.
     * Acceso O(1) — array lookup por ordinal.
     *
     * <p>RuntimeStats llama este método para obtener el valor efectivo:
     * <pre>
     *   double speed = container.getBucket(MOVEMENT_SPEED).evaluate(base);
     * </pre>
     *
     * @param target objetivo de la estadística.
     * @return bucket pre-alocado para ese target. Nunca null.
     */
    public ModifierBucket getBucket(StatTarget target) {
        return buckets[target.ordinal()];
    }

    // ── Gestión de modificadores ──────────────────────────────────────────

    /**
     * Añade un modificador al bucket del StatTarget correspondiente.
     *
     * <p>Una misma fuente puede añadir múltiples modificadores sobre targets
     * distintos o incluso sobre el mismo target. Todos se acumulan y se
     * retiran juntos al llamar {@link #removeBySource(Object)}.
     *
     * @param modifier modificador a añadir. No debe ser null.
     */
    public void addModifier(StatModifier modifier) {
        if (modifier == null) return;
        buckets[modifier.getTarget().ordinal()].add(modifier);
    }

    /**
     * Elimina todos los modificadores cuya fuente sea el objeto dado.
     *
     * <p>Recorre todos los buckets y elimina los modificadores cuya
     * referencia de fuente coincida (==) con {@code source}.
     *
     * <p>En el modelo StatContributor (HRFC-011), la fuente es siempre
     * el contributor propietario. RuntimeStats.revoke(contributor) delega
     * aquí pasando el contributor como source.
     *
     * <p>Coste: O(T × k) — aceptable porque ocurre solo al expirar una
     * fuente (un StatusEffect, una fase, un equipo...), nunca cada frame.
     *
     * @param source objeto propietario de los modificadores a eliminar.
     *               Identidad por referencia (==). Si null, no hace nada.
     */
    public void removeBySource(Object source) {
        if (source == null) return;
        for (ModifierBucket bucket : buckets) {
            if (!bucket.isEmpty()) {
                bucket.removeBySource(source);
            }
        }
    }

    /** Elimina todos los modificadores de todos los buckets. */
    public void clearAll() {
        for (ModifierBucket bucket : buckets) {
            if (!bucket.isEmpty()) bucket.clear();
        }
    }

    /**
     * True si no hay ningún modificador activo en ningún bucket.
     * Coste: O(T).
     */
    public boolean isEmpty() {
        for (ModifierBucket bucket : buckets) {
            if (!bucket.isEmpty()) return false;
        }
        return true;
    }

    /**
     * Invalida la caché de todos los buckets.
     * Llamar cuando EntityStats cambia sus valores base (evento raro).
     * Los buckets recomputarán en la siguiente llamada a evaluate().
     */
    public void invalidateAll() {
        for (ModifierBucket bucket : buckets) {
            bucket.invalidate();
        }
    }

    // ── Diagnóstico ───────────────────────────────────────────────────────

    /** Número total de modificadores activos en todos los buckets. Solo para debug. */
    public int totalModifierCount() {
        int count = 0;
        for (ModifierBucket bucket : buckets) count += bucket.size();
        return count;
    }

    @Override
    public String toString() {
        return "ModifierContainer[total=" + totalModifierCount() + "]";
    }
}
