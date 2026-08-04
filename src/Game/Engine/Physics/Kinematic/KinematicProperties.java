package Game.Engine.Physics.Kinematic;

import Game.Engine.Physics.Core.PropertyDescriptor;

/**
 * Catálogo de propiedades cinemáticas — velocidad y movimiento.
 *
 * ── HRFC-024 — Auditoría de Consistencia Arquitectónica ──────────────────
 *
 * ── RESPONSABILIDAD ──────────────────────────────────────────────────────
 * Este catálogo agrupa las propiedades que describen el estado de movimiento
 * de un objeto: sus componentes de velocidad.
 *
 * Son las propiedades afectadas por todos los fenómenos que aplican fuerzas:
 * gravedad (NewtonEvaluator), campo gravitacional (SchwarzschildEvaluator),
 * tensión superficial (StokesEvaluator), magnetismo (OhmEvaluator).
 *
 * ── CONSUMIDORES CORRECTOS ────────────────────────────────────────────────
 *   view.get(KinematicProperties.VELOCITY_X);
 *   view.get(KinematicProperties.VELOCITY_Y);
 *   view.add(KinematicProperties.VELOCITY_Y, delta);
 */
public final class KinematicProperties {

    private KinematicProperties() {}

    // ── Velocidad ─────────────────────────────────────────────────────────

    /**
     * Componente Y de velocidad.
     * Afectada por gravedad (NewtonEvaluator) y campos gravitacionales.
     * 0 = en reposo verticalmente.
     */
    public static final PropertyDescriptor VELOCITY_Y =
        new PropertyDescriptor("velocity_y", 0.0,
            Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, false,
            "Velocidad vertical en unidades/s");

    /**
     * Componente X de velocidad.
     * Afectada por campos magnéticos, gravitacionales y tensión superficial.
     * 0 = en reposo horizontalmente.
     */
    public static final PropertyDescriptor VELOCITY_X =
        new PropertyDescriptor("velocity_x", 0.0,
            Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, false,
            "Velocidad horizontal en unidades/s");
}
