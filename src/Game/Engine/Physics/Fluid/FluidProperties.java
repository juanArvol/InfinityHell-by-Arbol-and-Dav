package Game.Engine.Physics.Fluid;

import Game.Engine.Physics.Core.PropertyDescriptor;

/**
 * Catálogo de descriptores de propiedades del dominio fluídico.
 *
 * ── DOMINIO FÍSICO ────────────────────────────────────────────────────────
 * Este catálogo modela exclusivamente el fenómeno del flujo de masa:
 * cómo un objeto acumula contenido fluídico, con qué velocidad lo
 * intercambia con otros objetos, y qué resistencia opone al flujo interno.
 *
 * Una propiedad pertenece a este catálogo si y solo si responde a la pregunta:
 *   ¿Describe el comportamiento fluídico de un objeto?
 *
 * No se agrupan elementos aquí por cantidad ni por conveniencia histórica.
 * La cohesión semántica del dominio fluídico prevalece sobre el tamaño del archivo.
 *
 * ── PROPIEDADES INCLUIDAS ─────────────────────────────────────────────────
 *
 *   HUMIDITY              → contenido fluídico / humedad (propiedad de estado)
 *   HUMIDITY_ABSORPTION   → coeficiente de absorción/difusión de humedad
 *   VISCOSITY             → resistencia interna al flujo del material
 *
 * ── CATÁLOGOS DEL SISTEMA ─────────────────────────────────────────────────
 * Cada dominio físico tiene su propio catálogo:
 *
 *   ThermalProperties      → energía térmica y transferencia de calor
 *   ElectricalProperties   → carga eléctrica y conductividad
 *   FluidProperties        → flujo de masa, humedad y viscosidad          ← este
 *   MechanicalProperties   → presión, elasticidad y propiedades del sólido
 *   KinematicProperties    → velocidad y movimiento
 *   GravityProperties      → masa y gravedad
 *   ElectromagneticProperties → campo magnético y superconductividad
 *   RadiationProperties    → radiación y absorción
 *   MaterialStateProperties → transiciones de fase y estados especiales
 *   QuantumProperties      → espín cuántico y función de onda
 */
public final class FluidProperties {

    private FluidProperties() {}

    // ── Propiedad de estado ───────────────────────────────────────────────

    /**
     * Contenido fluídico (humedad).
     * Rango natural [0, 1]: 0 = completamente seco, 1 = saturado.
     *
     * Es la propiedad de estado central del dominio fluídico.
     * Cambia durante la simulación a través de BernoulliEvaluator, FickEvaluator,
     * AmbientDissipationEvaluator y FickEvaluator (saturación).
     */
    public static final PropertyDescriptor HUMIDITY =
        new PropertyDescriptor("humidity", 0.0, 0.0, 1.0, true,
            "Contenido fluídico relativo [0=seco, 1=saturado]");

    // ── Propiedades de material fluídico ─────────────────────────────────

    /**
     * Coeficiente de absorción de humedad.
     * Velocidad a la que el material absorbe o libera humedad del entorno.
     * Rango [0, 1]: 0 = impermeable, 1 = absorción instantánea.
     *
     * Usada por BernoulliEvaluator y FickEvaluator como coeficiente de difusión.
     * También usada por AmbientDissipationEvaluator en FLUID_AMBIENT_DISSIPATION.
     */
    public static final PropertyDescriptor HUMIDITY_ABSORPTION =
        new PropertyDescriptor("humidity_absorption", 0.1, 0.0, 1.0, true,
            "Coeficiente de absorción de humedad [0=impermeable, 1=máxima]");

    /**
     * Viscosidad del material.
     * Resistencia interna al flujo. Define cuánto frena el material su propia
     * difusión fluídica y el movimiento de otros objetos en su interior.
     * 0 = fluido ideal (sin resistencia interna).
     *
     * Usada por BernoulliEvaluator como factor reductor de la difusión.
     * Usada por ArchimedesEvaluator como amplificador del empuje fluídico.
     * Usada por StokesEvaluator como coeficiente de arrastre viscoso.
     */
    public static final PropertyDescriptor VISCOSITY =
        new PropertyDescriptor("viscosity", 0.0, 0.0, Double.POSITIVE_INFINITY, true,
            "Viscosidad del material [0=fluido ideal]");
}
