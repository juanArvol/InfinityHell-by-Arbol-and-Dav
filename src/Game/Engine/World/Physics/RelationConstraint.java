package Game.Engine.World.Physics;

/**
 * Restricción física declarativa asociada a una PhysicalRelation.
 *
 * ── HRFC-022 — Eliminación del Paradigma de Ley Ejecutable ───────────────
 *
 * ── RESPONSABILIDAD ──────────────────────────────────────────────────────
 * RelationConstraint describe una condición bajo la cual una PhysicalRelation
 * es activa o válida. El evaluador del sistema de resolución la consulta para
 * decidir si aplica la relación en un contexto concreto.
 *
 * RelationConstraint no ejecuta nada.
 * RelationConstraint no evalúa nada.
 * RelationConstraint solo describe.
 *
 * ── TIPOS DE RESTRICCIÓN ─────────────────────────────────────────────────
 *
 *   MAX_DISTANCE    → la relación solo aplica entre entidades cuya distancia
 *                     euclídea sea <= al valor de la restricción.
 *                     Valor: distancia máxima en unidades del mundo.
 *
 *   MIN_DELTA       → la relación solo aplica cuando la diferencia de valor
 *                     de la propiedad principal entre las dos entidades
 *                     supera el umbral indicado (evita cálculos en equilibrio).
 *                     Valor: umbral mínimo de diferencia.
 *
 *   PROPERTY_PRESENT → la relación solo aplica a entidades que tienen
 *                      registrada la propiedad identificada por el descriptor.
 *                      Se declara con el descriptor, no con un valor numérico.
 *
 *   THRESHOLD_ABOVE → la relación solo aplica cuando el valor de la propiedad
 *                     principal es estrictamente mayor que el umbral.
 *                     Valor: umbral inferior.
 *
 *   THRESHOLD_BELOW → la relación solo aplica cuando el valor de la propiedad
 *                     principal es estrictamente menor que el umbral.
 *                     Valor: umbral superior.
 *
 * ── USO ───────────────────────────────────────────────────────────────────
 *
 *   // Transferencia térmica solo dentro de radio 32:
 *   RelationConstraint.maxDistance(32.0)
 *
 *   // Solo si la diferencia de temperatura supera 1e-6:
 *   RelationConstraint.minDelta(1e-6)
 *
 *   // Solo si la entidad tiene COMPRESSIBILITY registrada:
 *   RelationConstraint.propertyPresent(CoreProperties.COMPRESSIBILITY)
 *
 *   // Cristalización solo cuando temperatura < 0:
 *   RelationConstraint.thresholdBelow(CoreProperties.TEMPERATURE, 0.0)
 *
 *   // Plasma solo cuando temperatura > umbral del material:
 *   RelationConstraint.thresholdAbove(CoreProperties.TEMPERATURE, 0.0)
 */
public final class RelationConstraint {

    /** Tipo de restricción. */
    public enum Type {
        MAX_DISTANCE,
        MIN_DELTA,
        PROPERTY_PRESENT,
        THRESHOLD_ABOVE,
        THRESHOLD_BELOW
    }

    private final Type               type;
    private final double             value;
    private final PropertyDescriptor descriptor;

    // ── Constructor privado — usar factories ──────────────────────────────

    private RelationConstraint(Type type, double value, PropertyDescriptor descriptor) {
        this.type       = type;
        this.value      = value;
        this.descriptor = descriptor;
    }

    // ── Factories ─────────────────────────────────────────────────────────

    /**
     * La relación solo aplica entre entidades dentro del radio indicado.
     *
     * @param distance distancia máxima en unidades del mundo.
     * @return restricción configurada.
     */
    public static RelationConstraint maxDistance(double distance) {
        return new RelationConstraint(Type.MAX_DISTANCE, distance, null);
    }

    /**
     * La relación solo aplica cuando la diferencia de la propiedad principal
     * entre las dos entidades supera el umbral.
     *
     * @param threshold umbral mínimo de diferencia.
     * @return restricción configurada.
     */
    public static RelationConstraint minDelta(double threshold) {
        return new RelationConstraint(Type.MIN_DELTA, threshold, null);
    }

    /**
     * La relación solo aplica a entidades que tienen la propiedad registrada.
     *
     * @param descriptor descriptor de la propiedad requerida. No puede ser null.
     * @return restricción configurada.
     */
    public static RelationConstraint propertyPresent(PropertyDescriptor descriptor) {
        if (descriptor == null)
            throw new IllegalArgumentException("descriptor no puede ser null");
        return new RelationConstraint(Type.PROPERTY_PRESENT, 0.0, descriptor);
    }

    /**
     * La relación solo aplica cuando el valor de la propiedad es > umbral.
     *
     * @param descriptor la propiedad a evaluar.
     * @param threshold  umbral inferior (estricto).
     * @return restricción configurada.
     */
    public static RelationConstraint thresholdAbove(PropertyDescriptor descriptor,
                                                     double threshold) {
        if (descriptor == null)
            throw new IllegalArgumentException("descriptor no puede ser null");
        return new RelationConstraint(Type.THRESHOLD_ABOVE, threshold, descriptor);
    }

    /**
     * La relación solo aplica cuando el valor de la propiedad es < umbral.
     *
     * @param descriptor la propiedad a evaluar.
     * @param threshold  umbral superior (estricto).
     * @return restricción configurada.
     */
    public static RelationConstraint thresholdBelow(PropertyDescriptor descriptor,
                                                      double threshold) {
        if (descriptor == null)
            throw new IllegalArgumentException("descriptor no puede ser null");
        return new RelationConstraint(Type.THRESHOLD_BELOW, threshold, descriptor);
    }

    // ── Accesores ─────────────────────────────────────────────────────────

    /**
     * Tipo de esta restricción.
     *
     * @return tipo. Nunca null.
     */
    public Type getType() {
        return type;
    }

    /**
     * Valor numérico de la restricción.
     * Relevante para MAX_DISTANCE, MIN_DELTA, THRESHOLD_ABOVE, THRESHOLD_BELOW.
     * Ignorado para PROPERTY_PRESENT.
     *
     * @return valor numérico.
     */
    public double getValue() {
        return value;
    }

    /**
     * Descriptor de propiedad asociado.
     * Relevante para PROPERTY_PRESENT, THRESHOLD_ABOVE y THRESHOLD_BELOW.
     * Null para MAX_DISTANCE y MIN_DELTA.
     *
     * @return descriptor, o null si no aplica.
     */
    public PropertyDescriptor getDescriptor() {
        return descriptor;
    }

    // ── Object ────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "RelationConstraint[" + type
            + (descriptor != null ? " prop=" + descriptor.getId() : "")
            + " value=" + value + "]";
    }
}
