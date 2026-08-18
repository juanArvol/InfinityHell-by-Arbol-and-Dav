package Game.Engine.Physics.Core;

import Game.Engine.Component;

/**
 * Componente de participación en la simulación física con contexto compuesto.
 *
 * ── HRFC-031 — Descomposición de PhysicalState en SimulationContext ───────
 * ── HRFC-FASE2 — Declarative Environment Ownership ───────────────────────
 *
 * ── RESPONSABILIDAD ──────────────────────────────────────────────────────
 * SimulationContextComponent es el puente entre el ECS y el SimulationContext.
 *
 * Su responsabilidad es exactamente doble:
 *   1. Declarar que esta entidad participa en la simulación con contexto compuesto.
 *   2. Almacenar el SimulationContext de la entidad.
 *
 * Nada más.
 *
 * ── RELACIÓN CON PhysicsComponent ─────────────────────────────────────────
 * SimulationContextComponent es el sucesor arquitectónico de PhysicsComponent
 * para entidades que necesitan la integración completa HRFC-031:
 *
 *   PhysicsComponent            → solo PhysicalState (física pura)
 *   SimulationContextComponent  → SimulationContext compuesto (física + cinemática
 *                                 + material + contacto + entorno)
 *
 * PhysicsComponent sigue siendo válido para entidades que solo necesiten
 * propiedades físicas sin integración cinemática. No es reemplazado.
 *
 * ── PRECEDENCIA EN PhysicsSolver ──────────────────────────────────────────
 * PhysicsSolver.FrameContext.resolveState() resuelve en este orden:
 *   1. SimulationContextComponent — extrae physical() del contexto compuesto
 *   2. PhysicsComponent           — extrae state directamente
 *
 * Esto garantiza compatibilidad total: entidades con PhysicsComponent
 * siguen funcionando sin cambios.
 *
 * ── PRINCIPIO FUNDAMENTAL ─────────────────────────────────────────────────
 * SimulationContextComponent NO implementa física.
 * SimulationContextComponent NO contiene algoritmos.
 * SimulationContextComponent NO conoce leyes ni fenómenos.
 *
 * La entidad que tiene un SimulationContextComponent únicamente declara:
 *   "Tengo contexto de simulación compuesto. El Engine puede simularlo."
 *
 * ── CÓMO USAR ────────────────────────────────────────────────────────────
 *
 *   // En el Assembler de la entidad:
 *   PhysicalState physical = PhysicalState.builder()
 *       .register(ThermalProperties.TEMPERATURE, 20.0)
 *       .register(MechanicalProperties.PRESSURE)
 *       .build();
 *
 *   MaterialState material = MaterialState.builder()
 *       .thermalConductivity(0.8)
 *       .heatCapacity(500.0)
 *       .frictionCoefficient(0.4)
 *       .build();
 *
 *   SimulationContext context = SimulationContext.builder(physical)
 *       .material(material)
 *       .environment(StandardAtmosphere.INSTANCE)  // HRFC-FASE2
 *       .build();
 *
 *   addComponent(new SimulationContextComponent(context));
 *   addComponent(new KinematicBridge());   // activa integración cinemática
 *
 * ── THREAD SAFETY ─────────────────────────────────────────────────────────
 * No es thread-safe. Usar exclusivamente desde el game loop thread.
 */
public final class SimulationContextComponent extends Component {

    /** Contexto de simulación compuesto de esta entidad. Nunca null. */
    private final SimulationContext context;

    // ── Constructor ───────────────────────────────────────────────────────

    /**
     * Crea el componente con el contexto de simulación dado.
     *
     * @param context el contexto de simulación de la entidad. No puede ser null.
     */
    public SimulationContextComponent(SimulationContext context) {
        if (context == null)
            throw new IllegalArgumentException("context no puede ser null");
        this.context = context;
    }

    // ── Acceso al contexto ────────────────────────────────────────────────

    /**
     * El contexto de simulación compuesto de la entidad.
     *
     * Contiene PhysicalState, StateSnapshot cinemático, MaterialState,
     * ContactState y EnvironmentState. Todos los sistemas del Engine
     * acceden a los dominios a través de este contexto.
     *
     * @return el SimulationContext de la entidad. Nunca null.
     */
    public SimulationContext getContext() {
        return context;
    }

    /**
     * Shortcut: el PhysicalState del contexto.
     *
     * Equivalente a getContext().physical().
     * Usado por PhysicsSolver para extraer el estado físico de manera
     * uniforme independientemente del tipo de componente.
     *
     * @return el PhysicalState. Nunca null.
     */
    public PhysicalState getPhysicalState() {
        return context.physical();
    }

    // ── Ciclo de vida — Component ─────────────────────────────────────────

    /**
     * No realiza ninguna inicialización.
     * SimulationContextComponent no contiene lógica de ciclo de vida.
     */
    @Override
    public void start() {}

    /**
     * No ejecuta ninguna lógica de frame.
     * Los sistemas del Engine actualizan el contexto directamente:
     *   - KinematicBridge actualiza kinematicSnapshot.
     *   - CollisionsSystem actualiza contactState.
     *   - WorldFieldSystem puede actualizar environmentState.
     */
    @Override
    public void update(double dt) {}
}
