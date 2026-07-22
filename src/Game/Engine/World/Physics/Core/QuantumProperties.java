package Game.Engine.World.Physics.Core;

/**
 * Catálogo de propiedades cuánticas — espín y función de onda.
 *
 * ── HRFC-024 — Auditoría de Consistencia Arquitectónica ──────────────────
 *
 * ── RESPONSABILIDAD ──────────────────────────────────────────────────────
 * Este catálogo agrupa las propiedades que describen el comportamiento cuántico
 * de un objeto: su número cuántico de espín y la amplitud de su función de onda.
 *
 * Estas propiedades son consumidas por PlanckEvaluator configurado para el
 * fenómeno QUANTUM_WAVE_COLLAPSE: cuando dos objetos con función de onda
 * activa se aproximan, ambas funciones colapsan progresivamente.
 *
 * ── CATÁLOGO AUTORIZADO — HRFC-024 ───────────────────────────────────────
 * QuantumProperties reside en el paquete Game.Engine.World.Physics.
 * PropertyDescriptor tiene constructor package-private.
 * Solo los catálogos de este paquete pueden crear PropertyDescriptor.
 *
 * ── CONSUMIDORES CORRECTOS ────────────────────────────────────────────────
 *   view.get(QuantumProperties.WAVE_FUNCTION);
 *   view.get(QuantumProperties.QUANTUM_SPIN);
 *   view.has(QuantumProperties.WAVE_FUNCTION);
 */
public final class QuantumProperties {

    private QuantumProperties() {}

    // ── Espín ─────────────────────────────────────────────────────────────

    /**
     * Número cuántico de espín del objeto.
     * Participa en el colapso de la función de onda cuando dos objetos
     * con WAVE_FUNCTION activa se aproximan dentro del radio cuántico.
     * 0 = espín nulo (no afecta al colapso).
     */
    public static final PropertyDescriptor QUANTUM_SPIN =
        new PropertyDescriptor("quantum_spin", 0.0,
            Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, false,
            "Número cuántico de espín del objeto");

    // ── Función de onda ───────────────────────────────────────────────────

    /**
     * Amplitud de la función de onda cuántica [0, 1].
     * 0 = función de onda completamente colapsada (estado definido).
     * 1 = superposición máxima (estado completamente indeterminado).
     * PlanckEvaluator reduce progresivamente este valor cuando dos objetos
     * con función de onda activa se aproximan dentro del radio cuántico.
     */
    public static final PropertyDescriptor WAVE_FUNCTION =
        new PropertyDescriptor("wave_function", 1.0, 0.0, 1.0, true,
            "Amplitud de la función de onda cuántica [0=colapsada, 1=superposición]");
}
