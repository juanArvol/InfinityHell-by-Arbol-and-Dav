package Game.Engine.World.Physics.Electrical;

import Game.Engine.World.Physics.Core.PropertyDescriptor;

/**
 * Catálogo de descriptores de propiedades del dominio eléctrico.
 *
 * ── DOMINIO FÍSICO ────────────────────────────────────────────────────────
 * Este catálogo modela exclusivamente el fenómeno de la transferencia de
 * carga eléctrica: cómo un objeto acumula carga y con qué velocidad la
 * transfiere a otros objetos conductores.
 *
 * Una propiedad pertenece a este catálogo si y solo si responde a la pregunta:
 *   ¿Describe el comportamiento eléctrico de un objeto?
 *
 * No se agrupan elementos aquí por cantidad ni por conveniencia histórica.
 * La cohesión semántica del dominio eléctrico prevalece sobre el tamaño del archivo.
 *
 * ── PROPIEDADES INCLUIDAS ─────────────────────────────────────────────────
 *
 *   CHARGE                  → carga eléctrica neta (propiedad de estado)
 *   ELECTRICAL_CONDUCTIVITY → velocidad de transferencia de carga con otros objetos
 *
 * ── CATÁLOGOS DEL SISTEMA ─────────────────────────────────────────────────
 * Cada dominio físico tiene su propio catálogo:
 *
 *   ThermalProperties      → energía térmica y transferencia de calor
 *   ElectricalProperties   → carga eléctrica y conductividad             ← este
 *   FluidProperties        → flujo de masa, humedad y viscosidad
 *   MechanicalProperties   → presión, elasticidad y propiedades del sólido
 *   KinematicProperties    → velocidad y movimiento
 *   GravityProperties      → masa y gravedad
 *   ElectromagneticProperties → campo magnético y superconductividad
 *   RadiationProperties    → radiación y absorción
 *   MaterialStateProperties → transiciones de fase y estados especiales
 *   QuantumProperties      → espín cuántico y función de onda
 */
public final class ElectricalProperties {

    private ElectricalProperties() {}

    // ── Propiedad de estado ───────────────────────────────────────────────

    /**
     * Carga eléctrica neta.
     * 0 = neutro. Positiva = carga positiva. Negativa = carga negativa.
     *
     * Es la propiedad de estado central del dominio eléctrico.
     * Cambia durante la simulación a través de OhmEvaluator y
     * AmbientDissipationEvaluator.
     */
    public static final PropertyDescriptor CHARGE =
        new PropertyDescriptor("charge", 0.0,
            Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, false,
            "Carga eléctrica neta acumulada");

    // ── Propiedad de material eléctrico ──────────────────────────────────

    /**
     * Conductividad eléctrica efectiva.
     * Velocidad de transferencia de carga eléctrica con otros objetos.
     * Rango [0, 1]: 0 = aislante perfecto, 1 = conductor perfecto.
     *
     * Usada por OhmEvaluator: determina la velocidad de transferencia entre pares.
     * También usada por AmbientDissipationEvaluator como coeficiente de disipación
     * de la relación ELECTRICAL_DISSIPATION.
     */
    public static final PropertyDescriptor ELECTRICAL_CONDUCTIVITY =
        new PropertyDescriptor("electrical_conductivity", 0.2, 0.0, 1.0, true,
            "Conductividad eléctrica del material [0=aislante, 1=conductor]");
}
