package Game.World.Transition;

import Game.Engine.GameObjects;
import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.World.Core.WorldCoordinator;

/**
 * Solicitud inmutable de transición de una entidad entre sectores.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * TransitionRequest encapsula todos los datos de una transición:
 *   - quién transita (subject)
 *   - desde qué sector (fromSector)
 *   - hacia qué sector (toSector) — null si no se conoce a priori
 *   - posición destino en el nuevo sector (targetPosition)
 *   - qué estilo visual usar (style)
 *   - si este objeto es el "world controller" (controla cuál es el sector activo)
 *
 * ── QUIÉN LO CREA ─────────────────────────────────────────────────────────
 * Los TransitionRequest son creados por:
 *   - TransitionDetector (cruce de borde automático)
 *   - TransitionGate (portal, puerta, teleporte explícito)
 *   - Código de gameplay (habilidad, script, IA)
 *   - TransitionSystem.request() directamente
 *
 * ── INMUTABILIDAD ─────────────────────────────────────────────────────────
 * Un TransitionRequest creado no se modifica. Si la validación encuentra
 * una posición alternativa, TransitionResolver retorna un nuevo request
 * con la posición corregida.
 */
public final class TransitionRequest {

    private final GameObjects      subject;
    private final WorldCoordinator fromSector;
    private final WorldCoordinator toSector;
    private final Vector2D         targetPosition;
    private final TransitionStyle  style;
    private final boolean          isWorldController;
    private final String           sourceId;  // ID del origen para debug/eventos

    // ── Constructor ───────────────────────────────────────────────────────

    private TransitionRequest(Builder b) {
        if (b.subject        == null) throw new IllegalArgumentException("subject is required");
        if (b.fromSector     == null) throw new IllegalArgumentException("fromSector is required");
        if (b.targetPosition == null) throw new IllegalArgumentException("targetPosition is required");
        this.subject           = b.subject;
        this.fromSector        = b.fromSector;
        this.toSector          = b.toSector;
        this.targetPosition    = b.targetPosition;
        this.style             = b.style != null ? b.style : TransitionStyle.INSTANT;
        this.isWorldController = b.isWorldController;
        this.sourceId          = b.sourceId != null ? b.sourceId : "unknown";
    }

    // ── Builders ──────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    /**
     * Factory de conveniencia para cruce de borde (el caso más común).
     * El sector destino se calcula del delta dx/dy.
     *
     * @param subject     entidad que transita
     * @param fromSector  sector de origen
     * @param dx          delta X de sector (-1, 0, +1)
     * @param dy          delta Y de sector (-1, 0, +1)
     * @param targetPos   posición ya ajustada en el nuevo sector
     * @param isController true si este objeto controla cuál sector es el activo
     */
    public static TransitionRequest borderCross(GameObjects subject,
                                                WorldCoordinator fromSector,
                                                int dx, int dy,
                                                Vector2D targetPos,
                                                boolean isController) {
        return builder()
            .subject(subject)
            .fromSector(fromSector)
            .toSector(new WorldCoordinator(fromSector.x() + dx, fromSector.y() + dy))
            .targetPosition(targetPos)
            .style(TransitionStyle.INSTANT)
            .isWorldController(isController)
            .sourceId("border_cross")
            .build();
    }

    /**
     * Factory para teleporte explícito hacia un sector y posición arbitrarios.
     *
     * @param subject    entidad que teleporta
     * @param fromSector sector de origen
     * @param toSector   sector destino
     * @param targetPos  posición en el sector destino
     * @param style      estilo visual de la transición
     */
    public static TransitionRequest teleport(GameObjects subject,
                                             WorldCoordinator fromSector,
                                             WorldCoordinator toSector,
                                             Vector2D targetPos,
                                             TransitionStyle style,
                                             boolean isController) {
        return builder()
            .subject(subject)
            .fromSector(fromSector)
            .toSector(toSector)
            .targetPosition(targetPos)
            .style(style)
            .isWorldController(isController)
            .sourceId("teleport")
            .build();
    }

    /**
     * Crea un nuevo request con la posición destino modificada.
     * Usado por TransitionResolver para devolver la posición corregida
     * sin mutar el request original.
     */
    public TransitionRequest withTargetPosition(Vector2D newTarget) {
        return builder()
            .subject(subject)
            .fromSector(fromSector)
            .toSector(toSector)
            .targetPosition(newTarget)
            .style(style)
            .isWorldController(isWorldController)
            .sourceId(sourceId)
            .build();
    }

    // ── Getters ───────────────────────────────────────────────────────────

    public GameObjects      getSubject()          { return subject;           }
    public WorldCoordinator getFromSector()       { return fromSector;        }
    public WorldCoordinator getToSector()         { return toSector;          }
    public Vector2D         getTargetPosition()   { return targetPosition;    }
    public TransitionStyle  getStyle()            { return style;             }
    public boolean          isWorldController()   { return isWorldController; }
    public String           getSourceId()         { return sourceId;          }

    // ── Builder ───────────────────────────────────────────────────────────

    public static final class Builder {
        private GameObjects      subject;
        private WorldCoordinator fromSector;
        private WorldCoordinator toSector;
        private Vector2D         targetPosition;
        private TransitionStyle  style         = TransitionStyle.INSTANT;
        private boolean          isWorldController = false;
        private String           sourceId;

        private Builder() {}

        public Builder subject(GameObjects s)           { this.subject = s; return this; }
        public Builder fromSector(WorldCoordinator c)   { this.fromSector = c; return this; }
        public Builder toSector(WorldCoordinator c)     { this.toSector = c; return this; }
        public Builder targetPosition(Vector2D p)       { this.targetPosition = p; return this; }
        public Builder style(TransitionStyle s)         { this.style = s; return this; }
        public Builder isWorldController(boolean b)     { this.isWorldController = b; return this; }
        public Builder sourceId(String id)              { this.sourceId = id; return this; }

        public TransitionRequest build() { return new TransitionRequest(this); }
    }
}
