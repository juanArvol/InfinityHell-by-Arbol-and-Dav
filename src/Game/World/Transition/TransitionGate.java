package Game.World.Transition;

import Game.Engine.GameObjects;
import Game.World.Core.WorldCoordinator;
import Game.Engine.GameMath.Logic2D.Vector2D;

/**
 * Punto de activación de una transición en el mundo.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * Un TransitionGate define un PUNTO donde puede ocurrir una transición.
 * Es la contraparte explícita de la detección automática por borde.
 *
 * Un TransitionGate puede ser:
 *   - Un portal que teleporta a otro sector
 *   - Una puerta que lleva a una sala específica
 *   - Un ascensor que cambia de nivel
 *   - Una salida de mazmorra
 *   - Un trigger de zona que activa la transición
 *
 * ── DIFERENCIA CON DETECCIÓN POR BORDE ────────────────────────────────────
 * La detección por borde (TransitionDetector) ocurre automáticamente cuando
 * una entidad sale de los bounds del sector. Es para el movimiento continuo.
 *
 * TransitionGate es para transiciones EXPLÍCITAS: el jugador activa una puerta,
 * un script teleporta a una entidad, un portal captura proyectiles, etc.
 *
 * ── EVALUACIÓN ────────────────────────────────────────────────────────────
 * TransitionSystem evalúa todos los gates registrados cada tick.
 * Cuando un gate reporta que debe activarse para una entidad, crea el
 * TransitionRequest correspondiente.
 *
 * ── IMPLEMENTACIONES PREVISTAS ────────────────────────────────────────────
 *   BorderTransitionGate  → activado al cruzar el borde del sector
 *   PortalGate            → portal visual con área de activación
 *   DoorGate              → puerta con condición de apertura
 *   ElevatorGate          → ascensor con múltiples destinos
 *   TriggerZoneGate       → zona de activación rectangular/circular
 *   ScriptedGate          → activado explícitamente por código
 */
public interface TransitionGate {

    /**
     * Retorna el TransitionRequest si este gate debe activarse para la entidad dada,
     * o null si no aplica.
     *
     * @param subject    la entidad que podría activar la transición
     * @param fromSector el sector actual de la entidad
     * @return TransitionRequest si la transición debe ocurrir, null si no
     */
    TransitionRequest evaluate(GameObjects subject, WorldCoordinator fromSector);

    /**
     * True si este gate puede seguir activándose.
     * Retornar false elimina el gate del sistema automáticamente.
     */
    default boolean isActive() { return true; }

    // ── Factories ─────────────────────────────────────────────────────────

    /**
     * Gate que teleporta una entidad específica a un sector y posición destino.
     * Se activa exactamente una vez (oneShot).
     *
     * @param target    entidad que será teleportada
     * @param toSector  sector destino
     * @param targetPos posición en el sector destino
     * @param style     estilo visual de la transición
     * @param isController true si esta entidad controla cuál sector es el activo
     */
    static TransitionGate oneShotTeleport(GameObjects target,
                                           WorldCoordinator toSector,
                                           Vector2D targetPos,
                                           TransitionStyle style,
                                           boolean isController) {
        return new TransitionGate() {
            private boolean activated = false;

            @Override
            public TransitionRequest evaluate(GameObjects subject, WorldCoordinator fromSector) {
                if (activated || subject != target) return null;
                return TransitionRequest.teleport(subject, fromSector, toSector,
                                                  targetPos, style, isController);
            }

            @Override
            public boolean isActive() { return !activated; }

            @Override
            public void onTransitionExecuted(TransitionRequest request) {
                if (request.getSubject() == target) activated = true;
            }
        };
    }

    /**
     * Gate de portal bidireccional: cuando una entidad entra en el área,
     * la teleporta al destino. Permanece activo indefinidamente.
     *
     * @param area       área de activación (en coordenadas de mundo del sector origen)
     * @param toSector   sector destino
     * @param entryPoint posición de llegada en el sector destino
     * @param style      estilo visual
     * @param isController true si esta entidad controla el sector activo
     */
    static TransitionGate portal(java.awt.Rectangle area,
                                  WorldCoordinator toSector,
                                  Vector2D entryPoint,
                                  TransitionStyle style,
                                  boolean isController) {
        return new TransitionGate() {
            @Override
            public TransitionRequest evaluate(GameObjects subject, WorldCoordinator fromSector) {
                var pos = subject.getTransform().getPosition();
                if (!area.contains(pos.getX(), pos.getY())) return null;
                return TransitionRequest.teleport(subject, fromSector, toSector,
                                                  entryPoint, style, isController);
            }
        };
    }

    /**
     * Llamado por TransitionSystem después de ejecutar la transición.
     * Permite a los gates con estado (oneShotTeleport) actualizarse.
     */
    default void onTransitionExecuted(TransitionRequest request) {}
}
