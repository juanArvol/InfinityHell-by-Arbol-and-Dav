package Game.Engine.Entity.Properties.Dependencies;

/**
 * Transformación que calcula cómo el cambio en una propiedad origen
 * afecta el valor de una propiedad destino.
 *
 * ── RESPONSABILIDAD ÚNICA ─────────────────────────────────────────────────
 * DependencyTransform responde a:
 *   "Si la propiedad A cambió de previousValue a newValue,
 *    ¿cuánto cambia la propiedad B?"
 *
 * NO modifica el mundo. Solo calcula: dado el delta de A, retorna el delta de B.
 *
 * @see PropertyDependency
 * @see PropertyDependencyGraph
 */
@FunctionalInterface
public interface DependencyTransform {

    double compute(double previousValue, double newValue);

    // ── Transformaciones predefinidas ─────────────────────────────────────

    DependencyTransform IDENTITY = (prev, next) -> 0.0;
    DependencyTransform DIRECT   = (prev, next) -> next - prev;

    // ── Factory methods ───────────────────────────────────────────────────

    static DependencyTransform linear(double factor) {
        if (factor == 0.0) return IDENTITY;
        if (factor == 1.0) return DIRECT;
        return (prev, next) -> (next - prev) * factor;
    }

    static DependencyTransform proportionalToNew(double factor) {
        return (prev, next) -> next * factor;
    }

    static DependencyTransform clamped(DependencyTransform base, double minDelta, double maxDelta) {
        if (base == null) return IDENTITY;
        return (prev, next) -> {
            double raw = base.compute(prev, next);
            return Math.max(minDelta, Math.min(maxDelta, raw));
        };
    }

    static DependencyTransform whenBelow(double threshold, DependencyTransform inner) {
        if (inner == null) return IDENTITY;
        return (prev, next) -> next < threshold ? inner.compute(prev, next) : 0.0;
    }

    static DependencyTransform whenAbove(double threshold, DependencyTransform inner) {
        if (inner == null) return IDENTITY;
        return (prev, next) -> next > threshold ? inner.compute(prev, next) : 0.0;
    }
}
