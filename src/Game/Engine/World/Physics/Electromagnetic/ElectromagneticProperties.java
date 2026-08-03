package Game.Engine.World.Physics.Electromagnetic;

import Game.Engine.World.Physics.Core.PropertyDescriptor;

/**
 * Catálogo de propiedades electromagnéticas — campo magnético y superconductividad.
 *
 * ── HRFC-024 — Auditoría de Consistencia Arquitectónica ──────────────────
 *
 * ── RESPONSABILIDAD ──────────────────────────────────────────────────────
 * Este catálogo agrupa las propiedades que describen el comportamiento
 * electromagnético de un objeto más allá de la carga eléctrica básica,
 * que pertenece al catálogo ElectricalProperties.
 *
 * MAGNETIC_FIELD describe la intensidad del dipolo magnético local,
 * consumida por OhmEvaluator para modelar la fuerza entre dipolos.
 *
 * SUPERCONDUCTIVITY_THRESHOLD describe la temperatura crítica por debajo
 * de la cual el material pierde toda resistencia eléctrica, consumida
 * también por OhmEvaluator con la restricción THRESHOLD_BELOW activa.
 *
 * ── CONSUMIDORES CORRECTOS ────────────────────────────────────────────────
 *   view.get(ElectromagneticProperties.MAGNETIC_FIELD);
 *   view.has(ElectromagneticProperties.SUPERCONDUCTIVITY_THRESHOLD);
 *
 */
public final class ElectromagneticProperties {

    private ElectromagneticProperties() {}

    // ── Campo magnético ───────────────────────────────────────────────────

    /**
     * Intensidad de campo magnético local.
     * Consumida por OhmEvaluator para calcular la fuerza entre pares
     * de objetos dentro del radio de acción magnético.
     * 0 = sin campo magnético.
     */
    public static final PropertyDescriptor MAGNETIC_FIELD =
        new PropertyDescriptor("magnetic_field", 0.0,
            Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, false,
            "Intensidad de campo magnético");

    // ── Superconductividad ────────────────────────────────────────────────

    /**
     * Temperatura crítica de superconductividad.
     * Cuando la temperatura del objeto cae por debajo de este umbral,
     * OhmEvaluator cancela la disipación eléctrica (resistencia = 0).
     * Double.POSITIVE_INFINITY = material nunca alcanza superconductividad.
     */
    public static final PropertyDescriptor SUPERCONDUCTIVITY_THRESHOLD =
        new PropertyDescriptor("superconductivity_threshold", Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, false,
            "Temperatura por debajo de la cual el material es superconductor");
}
