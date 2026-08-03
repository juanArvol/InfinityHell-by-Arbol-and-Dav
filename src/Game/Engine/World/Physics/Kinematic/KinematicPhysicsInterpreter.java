package Game.Engine.World.Physics.Kinematic;

import Game.Engine.World.Physics.Core.SimulationContext;

/**
 * Intérprete del estado cinemático hacia el SimulationContext.
 *
 * ── HRFC-030 — Integración entre Kinematic Physics y World Physics ────────
 * ── HRFC-031 — Descomposición de PhysicalState en SimulationContext ───────
 *
 * ── EVOLUCIÓN ARQUITECTÓNICA ─────────────────────────────────────────────
 *
 * HRFC-030 (diseño original):
 *   KinematicState → KinematicPhysicsInterpreter → PhysicalState
 *   El intérprete copiaba cada magnitud cinemática al PhysicalState del objeto
 *   usando set(), registrando propiedades cinemáticas dentro del mismo
 *   contenedor que las propiedades del simulador físico.
 *
 * HRFC-031 (diseño actual):
 *   KinematicState → SimulationContext.updateKinematic()
 *   El estado cinemático vive en su propio dominio dentro del SimulationContext.
 *   No hay copia hacia PhysicalState. No hay PropertyDescriptors cinemáticos.
 *   KinematicBridge llama directamente a SimulationContext.updateKinematic().
 *
 * ── RESPONSABILIDAD ACTUAL ────────────────────────────────────────────────
 * KinematicPhysicsInterpreter es ahora un thin wrapper de conveniencia.
 * Su único método delega directamente en SimulationContext.updateKinematic().
 *
 * Puede ser utilizado por sistemas legacy o por Assemblers que mantengan
 * referencias a este intérprete. En código nuevo, usar directamente:
 *
 *   context.updateKinematic(kinematicState);
 *
 * ── POR QUÉ SE CONSERVA ───────────────────────────────────────────────────
 * KinematicPhysicsInterpreter se conserva para:
 *   1. Compatibilidad con código que tenga referencia a INSTANCE.
 *   2. Documentar la evolución arquitectónica del pipeline cinemático.
 *   3. Punto de extensión futuro si se necesita lógica de traducción adicional.
 *
 * Si en el futuro no existen referencias externas a esta clase, puede
 * eliminarse sin impacto funcional.
 *
 * ── PIPELINE ACTUAL ──────────────────────────────────────────────────────
 *
 *   CollisionsSystem  (Kinematic Physics — 5 fases)
 *       ↓
 *   KinematicBridge.update()
 *       ↓
 *   KinematicState.from(vx, vy, mass, onGround, surface, dt)
 *       ↓
 *   SimulationContext.updateKinematic(kinematicState)
 *       ↓  avanza StateSnapshot<KinematicState>
 *       ↓
 *   WorldSimulation / PhysicsCoordinator
 *       ↓
 *   RelationEvaluators leen desde SimulationContext via EvaluationView
 *
 * ── INVARIANTE ────────────────────────────────────────────────────────────
 *   ✗ No copia propiedades cinemáticas hacia PhysicalState.
 *   ✗ No usa PropertyDescriptors cinemáticos.
 *   ✓ Delega exclusivamente en SimulationContext.updateKinematic().
 *
 * ── THREAD SAFETY ─────────────────────────────────────────────────────────
 * Sin estado mutable → thread-safe por diseño.
 */
public final class KinematicPhysicsInterpreter {

    /** Instancia compartida. Sin estado → seguro para reutilizar. */
    public static final KinematicPhysicsInterpreter INSTANCE =
        new KinematicPhysicsInterpreter();

    private KinematicPhysicsInterpreter() {}

    /**
     * Registra el estado cinemático en el SimulationContext del objeto.
     *
     * A partir de HRFC-031, este método ya no copia propiedades al
     * PhysicalState. Simplemente delega en SimulationContext.updateKinematic(),
     * que gestiona el avance del StateSnapshot<KinematicState>.
     *
     * En código nuevo, llamar directamente a context.updateKinematic().
     *
     * @param kinematicState el estado cinemático producido este frame.
     *                       No hace nada si es null.
     * @param context        el SimulationContext del objeto a actualizar.
     *                       No hace nada si es null.
     */
    public void interpret(KinematicState kinematicState,
                          SimulationContext context) {
        if (kinematicState == null || context == null) return;
        context.updateKinematic(kinematicState);
    }
}
