package Game.World.Transition;

import Game.Engine.Entity.Capabilities.GameplayCapability;

/**
 * Capacidad de transición como marcador para entidades del Engine.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * TransitionCapability es un marcador de capacidad para el sistema de
 * Gameplay Core. Las entidades que pueden participar en transiciones
 * (ser teleportadas, atravesar portales, cruzar bordes) pueden declarar
 * esta capacidad en su CapabilityComponent.
 *
 * ── DIFERENCIA CON EL SISTEMA DE TRANSICIÓN ───────────────────────────────
 * El sistema de transición (TransitionSystem) puede operar sobre cualquier
 * GameObjects independientemente de si tiene esta capacidad declarada.
 * TransitionCapability es para el sistema de Gameplay Core (CFCC) que
 * necesita saber si una entidad puede participar en transiciones antes
 * de solicitarlas.
 *
 * ── USO ───────────────────────────────────────────────────────────────────
 *   // En el assembler de una entidad:
 *   CapabilityComponent caps = new CapabilityComponent();
 *   caps.add(TransitionCapability.CAN_TRANSITION);
 *   caps.add(TransitionCapability.CAN_TELEPORT);
 *   entity.addComponent(caps);
 *
 *   // En sistemas de gameplay:
 *   CapabilityComponent caps = entity.getComponent(CapabilityComponent.class);
 *   if (caps != null && caps.has(TransitionCapability.CAN_TRANSITION)) {
 *       transitionSystem.request(teleportRequest);
 *   }
 *
 * ── CAPACIDADES DEFINIDAS ─────────────────────────────────────────────────
 *   CAN_TRANSITION  → puede participar en cualquier tipo de transición
 *   CAN_TELEPORT    → puede ser teleportado instantáneamente
 *   CAN_ENTER_PORTAL → puede atravesar portales
 *   CAN_CROSS_BORDER → puede cruzar bordes de sector automáticamente
 *   BLOCKS_TRANSITION → bloquea transiciones de otras entidades que lo impacten
 */
public final class TransitionCapability
        implements GameplayCapability {

    // ── Capacidades de transición ─────────────────────────────────────────

    /** Puede participar en cualquier tipo de transición. */
    public static final TransitionCapability CAN_TRANSITION =
        new TransitionCapability("CAN_TRANSITION");

    /** Puede ser teleportado instantáneamente. */
    public static final TransitionCapability CAN_TELEPORT =
        new TransitionCapability("CAN_TELEPORT");

    /** Puede atravesar portales. */
    public static final TransitionCapability CAN_ENTER_PORTAL =
        new TransitionCapability("CAN_ENTER_PORTAL");

    /** Puede cruzar bordes de sector automáticamente. */
    public static final TransitionCapability CAN_CROSS_BORDER =
        new TransitionCapability("CAN_CROSS_BORDER");

    /** Bloquea la transición de otras entidades que lo impacten. */
    public static final TransitionCapability BLOCKS_TRANSITION =
        new TransitionCapability("BLOCKS_TRANSITION");

    // ── Estado ────────────────────────────────────────────────────────────

    private final String id;

    private TransitionCapability(String id) {
        this.id = id;
    }

    @Override
    public String toString() { return "TransitionCapability[" + id + "]"; }
}
