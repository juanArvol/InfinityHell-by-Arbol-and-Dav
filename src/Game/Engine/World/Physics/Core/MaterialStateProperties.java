package Game.Engine.World.Physics.Core;

/**
 * Catálogo de propiedades de estado del material — cristalización, plasma y tensión superficial.
 *
 * ── HRFC-024 — Auditoría de Consistencia Arquitectónica ──────────────────
 *
 * ── RESPONSABILIDAD ──────────────────────────────────────────────────────
 * Este catálogo agrupa las propiedades que describen transiciones de fase
 * y estados especiales del material: cristalización, ionización de plasma
 * y cohesión de fluidos.
 *
 * Estas propiedades son consumidas por FickEvaluator (cristalización),
 * PlanckEvaluator (transición a plasma) y StokesEvaluator (tensión superficial).
 *
 * ── CATÁLOGO AUTORIZADO — HRFC-024 ───────────────────────────────────────
 * MaterialStateProperties reside en el paquete Game.Engine.World.Physics.
 * PropertyDescriptor tiene constructor package-private.
 * Solo los catálogos de este paquete pueden crear PropertyDescriptor.
 *
 * ── CONSUMIDORES CORRECTOS ────────────────────────────────────────────────
 *   view.get(MaterialStateProperties.CRYSTAL_CONCENTRATION);
 *   view.get(MaterialStateProperties.PLASMA_STATE);
 *   view.has(MaterialStateProperties.PLASMA_THRESHOLD);
 *   view.get(MaterialStateProperties.SURFACE_TENSION);
 */
public final class MaterialStateProperties {

    private MaterialStateProperties() {}

    // ── Cristalización ────────────────────────────────────────────────────

    /**
     * Concentración de cristales precipitados [0, 1].
     * Fracción de la masa que se ha solidificado en forma cristalina.
     * Incrementada por FickEvaluator cuando temperatura < 0 y humedad > umbral.
     * 0 = material completamente amorfo o líquido.
     * 1 = material completamente cristalizado.
     */
    public static final PropertyDescriptor CRYSTAL_CONCENTRATION =
        new PropertyDescriptor("crystal_concentration", 0.0, 0.0, 1.0, true,
            "Fracción de masa cristalizada");

    /**
     * Tasa de cristalización del material [0, 1].
     * Velocidad a la que el material precipita cristales cuando
     * las condiciones de temperatura y humedad son favorables.
     * 0 = material no cristaliza.
     * 1 = cristalización instantánea.
     */
    public static final PropertyDescriptor CRYSTALLIZATION_RATE =
        new PropertyDescriptor("crystallization_rate", 0.0, 0.0, 1.0, true,
            "Velocidad de precipitación de cristales del material");

    // ── Plasma ────────────────────────────────────────────────────────────

    /**
     * Estado de plasma: fracción de ionización [0, 1].
     * 0 = material en estado normal (sólido, líquido o gas no ionizado).
     * 1 = material completamente ionizado (plasma puro).
     * Incrementado por PlanckEvaluator cuando temperatura > PLASMA_THRESHOLD.
     */
    public static final PropertyDescriptor PLASMA_STATE =
        new PropertyDescriptor("plasma_state", 0.0, 0.0, 1.0, true,
            "Fracción de ionización de plasma [0=normal, 1=plasma puro]");

    /**
     * Temperatura de transición a plasma del material.
     * Temperatura en la que el material comienza a ionizarse.
     * Double.POSITIVE_INFINITY = material no puede alcanzar estado de plasma.
     */
    public static final PropertyDescriptor PLASMA_THRESHOLD =
        new PropertyDescriptor("plasma_threshold", Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, false,
            "Temperatura en la que el material alcanza estado de plasma");

    // ── Tensión superficial ───────────────────────────────────────────────

    /**
     * Tensión superficial del material líquido.
     * Fuerza de cohesión entre objetos líquidos adyacentes a distancias cortas.
     * Consumida por StokesEvaluator para modelar la atracción entre fluidos.
     * 0 = sin tensión superficial (material no cohesivo).
     */
    public static final PropertyDescriptor SURFACE_TENSION =
        new PropertyDescriptor("surface_tension", 0.0, 0.0, Double.POSITIVE_INFINITY, true,
            "Tensión superficial del material en estado líquido");
}
