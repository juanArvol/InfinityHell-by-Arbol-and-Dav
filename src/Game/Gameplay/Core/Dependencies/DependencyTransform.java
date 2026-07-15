package Game.Gameplay.Core.Dependencies;

/**
 * Transformación que calcula cómo el cambio en una propiedad origen
 * afecta el valor de una propiedad destino.
 *
 * ── RESPONSABILIDAD ÚNICA ─────────────────────────────────────────────────
 * DependencyTransform responde a una sola pregunta:
 *
 *   "Si la propiedad A cambió de previousValue a newValue,
 *    ¿cuánto cambia la propiedad B?"
 *
 * NO modifica el mundo.
 * NO aplica modificadores.
 * NO toca PropertyMap ni ModifierContainer.
 *
 * Solo calcula: dado el delta de A, retorna el delta de B.
 *
 * ── DISEÑO: FUNCIÓN PURA ─────────────────────────────────────────────────
 * DependencyTransform es una interfaz funcional que recibe los valores
 * anterior y nuevo de la propiedad origen, y retorna el delta que debe
 * aplicarse a la propiedad destino.
 *
 * Retornar 0.0 significa que B no cambia.
 * Retornar un valor positivo o negativo modifica B en esa cantidad.
 *
 * ── EJEMPLOS CONCEPTUALES ────────────────────────────────────────────────
 *
 *   // Temperature baja 10° → Speed baja 0.5 unidades
 *   DependencyTransform coldSlowdown = (prev, next) -> (next - prev) * -0.05;
 *
 *   // Temperature sube → AttackSpeed también sube linealmente
 *   DependencyTransform heatHaste = (prev, next) -> (next - prev) * 0.02;
 *
 *   // Cualquier cambio en Radius → area de efecto escala cuadráticamente
 *   DependencyTransform radiusToArea = (prev, next) ->
 *       Math.PI * next * next - Math.PI * prev * prev;
 *
 *   // Sin efecto (sin cambio en el destino)
 *   DependencyTransform.IDENTITY
 *
 * ── TRANSFORMACIONES PREDEFINIDAS ────────────────────────────────────────
 * Las constantes y factory methods de esta interfaz cubren los casos
 * más comunes sin necesidad de lambdas custom.
 *
 * @see PropertyDependency
 * @see PropertyDependencyGraph
 */
@FunctionalInterface
public interface DependencyTransform {

    /**
     * Calcula el delta que debe aplicarse a la propiedad destino dado el
     * cambio en la propiedad origen.
     *
     * @param previousValue valor anterior de la propiedad origen
     * @param newValue      valor nuevo de la propiedad origen
     * @return delta a aplicar en la propiedad destino (puede ser 0.0)
     */
    double compute(double previousValue, double newValue);

    // ── Transformaciones predefinidas ─────────────────────────────────────

    /**
     * Sin efecto: la propiedad destino no cambia sin importar el origen.
     * Útil como valor por defecto y en composiciones condicionales.
     */
    DependencyTransform IDENTITY = (prev, next) -> 0.0;

    /**
     * Propagación directa: el delta del destino es igual al delta del origen.
     * Si Temperature cambia +10, el destino también cambia +10.
     */
    DependencyTransform DIRECT = (prev, next) -> next - prev;

    // ── Factory methods ───────────────────────────────────────────────────

    /**
     * Transformación lineal: el delta del destino es {@code factor} × delta del origen.
     *
     *   factor = 1.0  → igual que DIRECT
     *   factor = 0.5  → la mitad del cambio
     *   factor = -1.0 → inversión (A sube → B baja la misma cantidad)
     *   factor = 0.0  → igual que IDENTITY (sin efecto)
     *
     * @param factor multiplicador del delta de origen
     * @return transformación lineal
     */
    static DependencyTransform linear(double factor) {
        if (factor == 0.0) return IDENTITY;
        if (factor == 1.0) return DIRECT;
        return (prev, next) -> (next - prev) * factor;
    }

    /**
     * Transformación proporcional al valor nuevo (no al delta).
     * Útil cuando el destino debe ser una fracción fija del valor actual del origen.
     *
     *   factor = 0.1 → destino recibe el 10% del valor nuevo de origen como delta
     *
     * Ejemplo: Speed = 10% de Temperature en cada cambio.
     *
     * @param factor fracción del valor nuevo del origen
     * @return transformación proporcional al valor nuevo
     */
    static DependencyTransform proportionalToNew(double factor) {
        return (prev, next) -> next * factor;
    }

    /**
     * Transformación con clamp: aplica la transformación base y luego
     * clampea el delta resultante al rango [minDelta, maxDelta].
     *
     * @param base     transformación base a aplicar primero
     * @param minDelta límite inferior del delta de salida
     * @param maxDelta límite superior del delta de salida
     * @return transformación con clamp
     */
    static DependencyTransform clamped(DependencyTransform base, double minDelta, double maxDelta) {
        if (base == null) return IDENTITY;
        return (prev, next) -> {
            double raw = base.compute(prev, next);
            return Math.max(minDelta, Math.min(maxDelta, raw));
        };
    }

    /**
     * Transformación umbral: solo aplica cuando el valor nuevo del origen supera
     * {@code threshold}. Por debajo del umbral, retorna 0.
     *
     * Ejemplo: la transformación de temperatura a velocidad solo aplica
     * cuando Temperature < -10 (congelamiento).
     *
     *   DependencyTransform.whenBelow(-10.0, DependencyTransform.linear(-0.05))
     *
     * @param threshold umbral de activación
     * @param inner     transformación a aplicar si el nuevo valor supera el umbral
     * @return transformación con umbral inferior
     */
    static DependencyTransform whenBelow(double threshold, DependencyTransform inner) {
        if (inner == null) return IDENTITY;
        return (prev, next) -> next < threshold ? inner.compute(prev, next) : 0.0;
    }

    /**
     * Transformación umbral: solo aplica cuando el valor nuevo del origen supera
     * {@code threshold} por arriba.
     *
     * @param threshold umbral de activación
     * @param inner     transformación a aplicar si el nuevo valor supera el umbral
     * @return transformación con umbral superior
     */
    static DependencyTransform whenAbove(double threshold, DependencyTransform inner) {
        if (inner == null) return IDENTITY;
        return (prev, next) -> next > threshold ? inner.compute(prev, next) : 0.0;
    }
}
