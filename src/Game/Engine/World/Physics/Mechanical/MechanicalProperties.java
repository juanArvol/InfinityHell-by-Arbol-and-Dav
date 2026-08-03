package Game.Engine.World.Physics.Mechanical;

import Game.Engine.World.Physics.Core.PropertyDescriptor;

/**
 * Catálogo de descriptores de propiedades del dominio mecánico.
 *
 * ── DOMINIO FÍSICO ────────────────────────────────────────────────────────
 * Este catálogo modela exclusivamente el comportamiento mecánico de los
 * materiales: su estado de presión interna, su respuesta a deformaciones
 * y sus propiedades estructurales como rigidez, elasticidad y densidad.
 *
 * Una propiedad pertenece a este catálogo si y solo si responde a la pregunta:
 *   ¿Describe el estado o las propiedades mecánicas de un objeto?
 *
 * No se agrupan elementos aquí por cantidad ni por conveniencia histórica.
 * La cohesión semántica del dominio mecánico prevalece sobre el tamaño del archivo.
 *
 * ── PROPIEDADES INCLUIDAS ─────────────────────────────────────────────────
 *
 *   PRESSURE        → presión local (propiedad de estado mecánico)
 *   COMPRESSIBILITY → facilidad de cambio de volumen bajo presión
 *   ELASTICITY      → fracción de energía conservada en deformaciones
 *   HARDNESS        → resistencia a deformación o penetración
 *   DENSITY         → masa por unidad de volumen
 *
 * ── NOTA ARQUITECTÓNICA: PRESSURE ────────────────────────────────────────
 * PRESSURE es el estado de presión local del objeto (sobrepresión / subpresión).
 * Aunque su disparador puede ser térmico (expansión volumétrica), la magnitud
 * en sí es una propiedad mecánica: fuerza por unidad de área acumulada en el
 * objeto. Es generada, disipada y corregida por evaluadores mecánicos
 * (PascalEvaluator, HookeEvaluator). Pertenece al dominio mecánico.
 *
 * ── CATÁLOGOS DEL SISTEMA ─────────────────────────────────────────────────
 * Cada dominio físico tiene su propio catálogo:
 *
 *   ThermalProperties      → energía térmica y transferencia de calor
 *   ElectricalProperties   → carga eléctrica y conductividad
 *   FluidProperties        → flujo de masa, humedad y viscosidad
 *   MechanicalProperties   → presión, elasticidad y propiedades del sólido  ← este
 *   KinematicProperties    → velocidad y movimiento
 *   GravityProperties      → masa y gravedad
 *   ElectromagneticProperties → campo magnético y superconductividad
 *   RadiationProperties    → radiación y absorción
 *   MaterialStateProperties → transiciones de fase y estados especiales
 *   QuantumProperties      → espín cuántico y función de onda
 */
public final class MechanicalProperties {

    private MechanicalProperties() {}

    // ── Propiedad de estado ───────────────────────────────────────────────

    /**
     * Presión local relativa al equilibrio del mundo.
     * 0 = presión ambiente. Positiva = sobrepresión. Negativa = subpresión.
     *
     * Es la propiedad de estado central del dominio mecánico.
     * Generada por PascalEvaluator (expansión volumétrica térmica).
     * Corregida por HookeEvaluator (disipación del exceso por compresibilidad).
     */
    public static final PropertyDescriptor PRESSURE =
        new PropertyDescriptor("pressure", 0.0,
            Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, false,
            "Presión local relativa al equilibrio ambiental");

    // ── Propiedades de material mecánico ─────────────────────────────────

    /**
     * Compresibilidad.
     * Facilidad de cambio de volumen bajo presión aplicada.
     * Rango [0, 1]: 0 = incompresible, 1 = muy compresible.
     *
     * Usada por PascalEvaluator: determina cuánta presión genera la temperatura.
     * Usada por HookeEvaluator: determina la velocidad de disipación del exceso.
     */
    public static final PropertyDescriptor COMPRESSIBILITY =
        new PropertyDescriptor("compressibility", 0.1, 0.0, 1.0, true,
            "Compresibilidad del material [0=incompresible, 1=muy compresible]");

    /**
     * Elasticidad.
     * Fracción de energía cinética conservada en colisiones o deformaciones.
     * Rango [0, 1]: 0 = completamente inelástico, 1 = elástico perfecto.
     */
    public static final PropertyDescriptor ELASTICITY =
        new PropertyDescriptor("elasticity", 0.3, 0.0, 1.0, true,
            "Elasticidad del material [0=inelástico, 1=elástico perfecto]");

    /**
     * Dureza.
     * Resistencia a deformación superficial o penetración mecánica.
     * Rango [0, 1]: 0 = muy blando, 1 = muy duro.
     */
    public static final PropertyDescriptor HARDNESS =
        new PropertyDescriptor("hardness", 0.5, 0.0, 1.0, true,
            "Dureza del material [0=blando, 1=duro]");

    /**
     * Densidad relativa del material.
     * Masa por unidad de volumen en unidades del juego.
     * Determina la inercia del objeto frente a fuerzas aplicadas.
     */
    public static final PropertyDescriptor DENSITY =
        new PropertyDescriptor("density", 1000.0, 0.0, Double.POSITIVE_INFINITY, true,
            "Densidad del material en unidades relativas del juego");
}
