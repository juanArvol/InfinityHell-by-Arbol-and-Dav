package Game.World.Transition;

import Game.Engine.GameObjects;
import Game.World.Core.WorldCoordinator;

/**
 * Eventos del ciclo de vida de una transición.
 *
 * Publicados en GameEventBus.GLOBAL para que cualquier sistema
 * pueda reaccionar sin acoplarse a TransitionSystem.
 *
 * ── USOS TÍPICOS ──────────────────────────────────────────────────────────
 *   OnTransitionStarted   → activar efecto de fade/sonido, pausar IA
 *   OnTransitionCompleted → activar nuevo spawn, actualizar minimapa, música
 *   OnTransitionRejected  → mostrar feedback visual ("no puedes pasar"), daño
 */
public final class TransitionEvent {

    private TransitionEvent() {}

    /**
     * Emitido cuando comienza el proceso de transición.
     * La entidad aún está en el sector origen.
     */
    public record OnTransitionStarted(
        GameObjects      subject,
        WorldCoordinator fromSector,
        WorldCoordinator toSector,
        TransitionStyle  style
    ) {}

    /**
     * Emitido cuando la entidad fue transferida al nuevo sector.
     * El style puede seguir ejecutándose (ej: fade-in todavía activo).
     */
    public record OnTransitionCompleted(
        GameObjects      subject,
        WorldCoordinator fromSector,
        WorldCoordinator toSector
    ) {}

    /**
     * Emitido cuando una transición fue rechazada por el resolver.
     * La entidad no se movió al sector destino.
     */
    public record OnTransitionRejected(
        GameObjects      subject,
        WorldCoordinator fromSector,
        WorldCoordinator toSector,
        String           reason
    ) {}

    /**
     * Emitido cuando el world controller cambia de sector.
     * WorldManager escucha este evento para actualizar el sector activo.
     */
    public record OnWorldControllerSectorChanged(
        GameObjects      controller,
        WorldCoordinator previousSector,
        WorldCoordinator newSector
    ) {}
}
