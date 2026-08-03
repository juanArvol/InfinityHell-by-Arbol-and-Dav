package Game.Engine.World.Physics.Kinematic;

import Game.Engine.Component;
import Game.Engine.Entity.Components.Physics2DComponent;
import Game.Engine.GameMath.KineticPhysics.Types.Physics2D;
import Game.Engine.World.Physics.Core.PhysicsComponent;
import Game.Engine.World.Physics.Core.PhysicalStateComponent;
import Game.Engine.World.Physics.Core.SimulationContext;
import Game.Engine.World.Physics.Core.SimulationContextComponent;

/**
 * Puente entre Kinematic Physics y World Physics.
 *
 * ── HRFC-030 — Integración entre Kinematic Physics y World Physics ────────
 * ── HRFC-031 — Descomposición de PhysicalState en SimulationContext ───────
 *
 * ── POSICIÓN EN EL PIPELINE ──────────────────────────────────────────────
 *
 *   CollisionsSystem  (Kinematic Physics — 5 fases)
 *       ↓  frame completado
 *   KinematicBridge.update()   ← ESTE COMPONENTE  (via GameObjects.update())
 *       ↓
 *   KinematicState  (DTO inmutable — estado instantáneo del frame actual)
 *       ↓
 *   SimulationContext.updateKinematic(kinematicState)
 *       ↓  avanza StateSnapshot<KinematicState> (current → previous, new → current)
 *       ↓
 *   WorldSimulation / PhysicsCoordinator  (evalúa relaciones del mundo)
 *       ↓
 *   Physical Phenomena  (calor por fricción, disipación, presión...)
 *
 * ── RESPONSABILIDAD ──────────────────────────────────────────────────────
 * KinematicBridge es el único componente que conecta Physics2D con el
 * SimulationContext del mismo objeto.
 *
 * En cada frame:
 *   1. Lee la velocidad y el estado de Physics2D.
 *   2. Construye un KinematicState con los datos cinemáticos instantáneos.
 *   3. Invoca SimulationContext.updateKinematic() para avanzar el snapshot.
 *      El propio SimulationContext gestiona el historial (current/previous).
 *
 * ── HRFC-031: ELIMINACIÓN DE previousSpeed ───────────────────────────────
 * Antes de HRFC-031, KinematicBridge mantenía un campo previousSpeed para
 * calcular la aceleración y el delta de energía cinética.
 *
 * Ese patrón introducía una deuda técnica: cada nueva magnitud derivada
 * requería un campo análogo (previousMomentum, previousAngularVelocity…).
 *
 * Con StateSnapshot<KinematicState> dentro de SimulationContext, el historial
 * es completamente genérico. KinematicBridge ya no almacena ningún valor del
 * frame anterior: toda la información histórica vive en el snapshot.
 *
 * Los evaluadores calculan deltas usando los helpers del KinematicState:
 *   snap.current().accelerationFrom(snap.previous())
 *   snap.current().deltaKineticEnergyFrom(snap.previous())
 *   snap.current().deltaMomentumFrom(snap.previous())
 *
 * ── ORDEN DE EJECUCIÓN ────────────────────────────────────────────────────
 * KinematicBridge.update() se ejecuta como parte de GameObjects.update(),
 * que ocurre en el paso (1) de WorldObjectsContainer:
 *
 *   (1) objectUpdater  ← GameObjects.update() → KinematicBridge.update()
 *   (2) StatusEffectSystem
 *   (3) WorldSimulation  ← lee el SimulationContext ya actualizado
 *   (4) CollisionsSystem
 *
 * Esto garantiza que cuando WorldSimulation evalúa las relaciones cinemáticas,
 * el SimulationContext ya contiene el estado cinemático del frame actual,
 * con el frame anterior accesible en snapshot.previous().
 *
 * ── CONDICIÓN DE ACTIVACIÓN ──────────────────────────────────────────────
 * KinematicBridge actúa si el objeto tiene:
 *   - Physics2DComponent          (Kinematic Physics activo)
 *   - SimulationContextComponent  (SimulationContext activo — HRFC-031)
 *
 * Compatibilidad legacy: si el objeto tiene PhysicsComponent o
 * PhysicalStateComponent sin SimulationContextComponent, KinematicBridge
 * no hace nada (la integración cinemática requiere el contexto compuesto).
 *
 * ── DELTATIME ─────────────────────────────────────────────────────────────
 * KinematicBridge usa el deltaTime fijo del motor (1/60 s por defecto).
 * Si el proyecto usa un deltaTime variable, inyectarlo vía
 * {@link #setDeltaTime(double)} antes de cada frame.
 *
 * ── CÓMO USAR ────────────────────────────────────────────────────────────
 * Añadir a cualquier entidad que tenga Physics2D y SimulationContextComponent:
 *
 *   // En el Assembler de la entidad:
 *   addComponent(new SimulationContextComponent(context));
 *   addComponent(new KinematicBridge());
 *
 * No se requiere ninguna configuración adicional.
 *
 * ── INVARIANTE ────────────────────────────────────────────────────────────
 *   ✗ No mueve la entidad.
 *   ✗ No modifica velocidades ni aceleración.
 *   ✗ No evalúa relaciones físicas.
 *   ✗ No almacena ningún estado del frame anterior (eliminado en HRFC-031).
 *   ✓ Solo lee Physics2D y delega la actualización al SimulationContext.
 *   ✓ El historial cinemático lo gestiona StateSnapshot dentro del contexto.
 *
 * ── THREAD SAFETY ─────────────────────────────────────────────────────────
 * No thread-safe. Usar exclusivamente desde el game loop thread.
 */
public final class KinematicBridge extends Component {

    /**
     * Duración del frame en segundos.
     * Default: 1/60 s (60 fps fijo). Inyectable via setDeltaTime().
     */
    private double deltaTime = 1.0 / 60.0;

    // ── Ciclo de vida — Component ─────────────────────────────────────────

    /**
     * Inicialización del componente.
     * No hay estado previo que resetear — el historial vive en SimulationContext.
     */
    @Override
    public void start() {
        // Sin estado interno que inicializar.
        // El snapshot cinemático en SimulationContext se inicializa al primer
        // frame de update() mediante StateSnapshot.initial().
    }

    /**
     * Ejecutado cada frame por GameObjects.update().
     *
     * Lee Physics2D → construye KinematicState instantáneo →
     * delega a SimulationContext.updateKinematic() para avanzar el snapshot.
     */
    @Override
    public void update() {
        if (gameObject == null) return;

        // ── Obtener Physics2D ─────────────────────────────────────────────
        Physics2DComponent physComp = gameObject.getComponent(Physics2DComponent.class);
        if (physComp == null) return;
        Physics2D physics = physComp.getPhysics();
        if (physics == null) return;

        // ── Obtener SimulationContext ─────────────────────────────────────
        SimulationContext context = resolveSimulationContext();
        if (context == null) return;

        // ── Construir KinematicState instantáneo ──────────────────────────
        // Solo el estado del frame actual. El snapshot gestiona el historial.
        KinematicState kinematicState = KinematicState.from(
            physics.getVelocity().getX(),
            physics.getVelocity().getY(),
            physics.getMass(),
            physics.getOnGround(),
            physics.getCurrentSurface(),
            deltaTime
        );

        // ── Actualizar el SimulationContext ───────────────────────────────
        // updateKinematic() inicializa el snapshot en el primer frame
        // y lo avanza (current → previous, new → current) en los siguientes.
        context.updateKinematic(kinematicState);
    }

    // ── Configuración ─────────────────────────────────────────────────────

    /**
     * Establece el deltaTime que usará el bridge al construir KinematicState.
     *
     * Llamar desde el sistema de tiempo del engine si se usa timestep variable:
     *   bridge.setDeltaTime(actualDeltaTime);
     *
     * @param deltaTime duración del frame en segundos. Debe ser > 0.
     */
    public void setDeltaTime(double deltaTime) {
        if (deltaTime > 0) this.deltaTime = deltaTime;
    }

    /**
     * Retorna el deltaTime actualmente configurado, en segundos.
     *
     * @return deltaTime en segundos.
     */
    public double getDeltaTime() {
        return deltaTime;
    }

    // ── Resolución del SimulationContext ──────────────────────────────────

    /**
     * Resuelve el SimulationContext del objeto.
     *
     * Solo SimulationContextComponent es el canal canónico para HRFC-031.
     * PhysicsComponent y PhysicalStateComponent no son suficientes: la
     * integración cinemática requiere el contexto compuesto.
     *
     * @return el SimulationContext, o null si el objeto no tiene el componente.
     */
    private SimulationContext resolveSimulationContext() {
        SimulationContextComponent ctxComp =
            gameObject.getComponent(SimulationContextComponent.class);
        return ctxComp != null ? ctxComp.getContext() : null;
    }
}
