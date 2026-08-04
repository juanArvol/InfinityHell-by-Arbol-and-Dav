package Game.Engine.Physics.Core;

/**
 * Catálogo de magnitudes físicas transitorias del frame.
 *
 * ── HRFC-022 — Identidad fuerte en FrameState ────────────────────────────
 *
 * ── RESPONSABILIDAD ──────────────────────────────────────────────────────
 * FrameMagnitudes es el catálogo central de todas las magnitudes transitorias
 * utilizadas por el sistema de resolución durante un frame de simulación.
 *
 * Es al FrameState lo que CoreProperties es al PhysicalState.
 *
 * ── PRINCIPIO FUNDAMENTAL ─────────────────────────────────────────────────
 * Ningún evaluador debe usar String para identificar una magnitud transitoria.
 * Toda referencia a FrameState ocurre a través de constantes de este catálogo.
 *
 * ── MAGNITUDES INCLUIDAS ──────────────────────────────────────────────────
 *
 *   CURRENT              → corriente eléctrica calculada por OhmEvaluator
 *                          consumida por JouleEvaluator
 *
 *   ABSORBED_RADIATION   → energía radiante absorbida calculada por PlanckEvaluator
 *                          consumida por RadiationThermalEvaluator
 *
 *   HEAT_FLUX            → flujo de calor transitorio (disponible para extensiones)
 *
 *   PRESSURE_GRADIENT    → gradiente de presión local (disponible para extensiones)
 *
 *   DRAG_FORCE           → fuerza de arrastre viscoso (disponible para extensiones)
 *
 *   BUOYANT_FORCE        → fuerza de empuje de Arquímedes (disponible para extensiones)
 *
 * ── EXTENSIBILIDAD ────────────────────────────────────────────────────────
 * Añadir una nueva magnitud transitoria:
 *
 *   public static final FrameMagnitude MY_MAGNITUDE =
 *       new FrameMagnitude("my_magnitude", "Descripción");
 *
 * Solo es posible desde este paquete. El constructor de FrameMagnitude es
 * package-private — ningún código externo puede crear nuevos descriptores.
 * No modifica FrameState. No modifica ningún evaluador existente.
 * No modifica CoreProperties ni PhysicalState.
 */
public final class FrameMagnitudes {

    private FrameMagnitudes() {}

    // ── Magnitudes eléctricas ─────────────────────────────────────────────

    /**
     * Corriente eléctrica (A) calculada durante la transferencia de carga.
     *
     * Producida por: OhmEvaluator  (RelationType.OHM)
     * Consumida por: JouleEvaluator (RelationType.JOULE)
     *
     * Representa la intensidad de corriente que fluyó entre dos objetos
     * durante este frame, acumulada por entidad. No es una propiedad del mundo.
     */
    public static final FrameMagnitude CURRENT =
        new FrameMagnitude("current",
            "Corriente eléctrica transferida este frame (A)");

    // ── Magnitudes radiantes ──────────────────────────────────────────────

    /**
     * Energía radiante absorbida por el objeto durante este frame.
     *
     * Producida por: PlanckEvaluator       (RelationType.PLANCK)
     * Consumida por: RadiationThermalEvaluator (RelationType.RADIATION_THERMAL)
     *
     * Representa la energía que el receptor absorbió de su entorno radiante
     * durante este frame. No es una propiedad del mundo.
     */
    public static final FrameMagnitude ABSORBED_RADIATION =
        new FrameMagnitude("absorbed_radiation",
            "Energía radiante absorbida por el objeto este frame");

    // ── Magnitudes térmicas ───────────────────────────────────────────────

    /**
     * Flujo de calor transitorio calculado durante la conducción térmica.
     * Disponible para extensiones del sistema de resolución.
     *
     * Producida por: FourierEvaluator (si se extiende para producirla)
     */
    public static final FrameMagnitude HEAT_FLUX =
        new FrameMagnitude("heat_flux",
            "Flujo de calor calculado durante la conducción térmica este frame");

    // ── Magnitudes mecánicas ──────────────────────────────────────────────

    /**
     * Gradiente de presión local calculado durante la expansión volumétrica.
     * Disponible para extensiones del sistema de resolución.
     */
    public static final FrameMagnitude PRESSURE_GRADIENT =
        new FrameMagnitude("pressure_gradient",
            "Gradiente de presión local calculado este frame");

    /**
     * Fuerza de arrastre viscoso calculada por StokesEvaluator.
     * Disponible para extensiones del sistema de resolución.
     */
    public static final FrameMagnitude DRAG_FORCE =
        new FrameMagnitude("drag_force",
            "Fuerza de arrastre viscoso calculada este frame");

    /**
     * Fuerza de empuje de Arquímedes calculada por ArchimedesEvaluator.
     * Disponible para extensiones del sistema de resolución.
     */
    public static final FrameMagnitude BUOYANT_FORCE =
        new FrameMagnitude("buoyant_force",
            "Fuerza de empuje de Arquímedes calculada este frame");
}
