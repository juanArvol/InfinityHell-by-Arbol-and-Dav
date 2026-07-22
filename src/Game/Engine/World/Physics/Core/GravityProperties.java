package Game.Engine.World.Physics.Core;

/**
 * Catálogo de propiedades gravitacionales — masa y geometría de Schwarzschild.
 *
 * ── HRFC-024 — Auditoría de Consistencia Arquitectónica ──────────────────
 *
 * ── RESPONSABILIDAD ──────────────────────────────────────────────────────
 * Este catálogo agrupa las propiedades que describen la naturaleza gravitacional
 * de un objeto: su masa inercial y su radio de Schwarzschild.
 *
 * Son las propiedades consumidas por SchwarzschildEvaluator y EventHorizonEvaluator,
 * que modelan la atracción gravitacional y la absorción al cruzar el horizonte
 * de eventos respectivamente.
 *
 * ── CATÁLOGO AUTORIZADO — HRFC-024 ───────────────────────────────────────
 * GravityProperties reside en el paquete Game.Engine.World.Physics.
 * PropertyDescriptor tiene constructor package-private.
 * Solo los catálogos de este paquete pueden crear PropertyDescriptor.
 *
 * ── CONSUMIDORES CORRECTOS ────────────────────────────────────────────────
 *   view.get(GravityProperties.MASS);
 *   view.get(GravityProperties.SCHWARZSCHILD_RADIUS);
 *   view.has(GravityProperties.SCHWARZSCHILD_RADIUS);
 */
public final class GravityProperties {

    private GravityProperties() {}

    // ── Masa ──────────────────────────────────────────────────────────────

    /**
     * Masa del objeto en unidades del juego.
     * Usada como factor en la atracción gravitacional (F = G·m_a·m_b / d²)
     * y como divisor para calcular la aceleración resultante.
     * Valor mínimo efectivo en evaluadores: 0.01 (evita división por cero).
     */
    public static final PropertyDescriptor MASS =
        new PropertyDescriptor("mass", 1.0, 0.0, Double.POSITIVE_INFINITY, true,
            "Masa del objeto en kg relativos");

    // ── Geometría relativista ─────────────────────────────────────────────

    /**
     * Radio de Schwarzschild efectivo (en unidades del mundo).
     * Define el horizonte de eventos: si otro objeto cae dentro de este radio,
     * EventHorizonEvaluator cancela su velocidad.
     * 0 = sin horizonte de eventos (el objeto no actúa como agujero negro).
     */
    public static final PropertyDescriptor SCHWARZSCHILD_RADIUS =
        new PropertyDescriptor("schwarzschild_radius", 0.0, 0.0, Double.POSITIVE_INFINITY, true,
            "Radio de Schwarzschild derivado de la masa del objeto");
}
