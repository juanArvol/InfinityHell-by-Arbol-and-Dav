package Game.Engine.World.Physics.Radiation;

import Game.Engine.World.Physics.Core.PropertyDescriptor;

/**
 * Catálogo de propiedades radiantes — nivel de radiación y absorción.
 *
 * ── HRFC-024 — Auditoría de Consistencia Arquitectónica ──────────────────
 *
 * ── RESPONSABILIDAD ──────────────────────────────────────────────────────
 * Este catálogo agrupa las propiedades que describen el comportamiento radiante
 * de un objeto: cuánta radiación ionizante ha acumulado y qué fracción
 * de la radiación incidente puede absorber.
 *
 * RADIATION_LEVEL es leída y escrita por PlanckEvaluator durante la
 * transferencia de radiación entre pares de objetos.
 *
 * RADIATION_ABSORPTION es leída por PlanckEvaluator para calcular la
 * fracción de radiación que el receptor absorbe por frame.
 *
 * ── CONSUMIDORES CORRECTOS ────────────────────────────────────────────────
 *   view.get(RadiationProperties.RADIATION_LEVEL);
 *   view.get(RadiationProperties.RADIATION_ABSORPTION);
 *   view.add(RadiationProperties.RADIATION_LEVEL, -transferred);
 */
public final class RadiationProperties {

    private RadiationProperties() {}

    // ── Radiación acumulada ───────────────────────────────────────────────

    /**
     * Nivel de radiación ionizante acumulada.
     * Intercambiado entre pares por PlanckEvaluator según la diferencia
     * de nivel y el coeficiente de absorción de cada participante.
     * 0 = sin radiación acumulada.
     */
    public static final PropertyDescriptor RADIATION_LEVEL =
        new PropertyDescriptor("radiation_level", 0.0, 0.0, Double.POSITIVE_INFINITY, true,
            "Nivel de radiación ionizante acumulada");

    // ── Capacidad de absorción ────────────────────────────────────────────

    /**
     * Coeficiente de absorción de radiación del material [0, 1].
     * Fracción de la radiación incidente que el material absorbe por frame.
     * 0 = material completamente transparente a la radiación.
     * 1 = material opaco que absorbe toda la radiación incidente.
     */
    public static final PropertyDescriptor RADIATION_ABSORPTION =
        new PropertyDescriptor("radiation_absorption", 0.1, 0.0, 1.0, true,
            "Fracción de radiación que el material absorbe por frame");
}
