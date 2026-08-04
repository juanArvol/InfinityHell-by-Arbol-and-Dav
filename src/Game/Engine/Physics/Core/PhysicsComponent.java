package Game.Engine.Physics.Core;

import Game.Engine.Component;

/**
 * Componente de participación en la simulación física.
 *
 * ── HRFC-021 — Property-Driven Physics Architecture ───────────────────────
 *
 * ── RESPONSABILIDAD ──────────────────────────────────────────────────────
 * PhysicsComponent es el único puente entre el ECS y el Physics Core.
 *
 * Su responsabilidad es exactamente doble:
 *   1. Declarar que esta entidad participa en la simulación física.
 *   2. Almacenar el PhysicalState de la entidad.
 *
 * Nada más.
 *
 * ── PRINCIPIO FUNDAMENTAL ─────────────────────────────────────────────────
 * PhysicsComponent NO implementa física.
 * PhysicsComponent NO contiene algoritmos.
 * PhysicsComponent NO contiene simulación.
 * PhysicsComponent NO conoce leyes.
 * PhysicsComponent NO conoce electricidad.
 * PhysicsComponent NO conoce temperatura.
 * PhysicsComponent NO conoce gravedad.
 *
 * La entidad que tiene un PhysicsComponent únicamente declara:
 *   "Tengo estado físico. El Physics Core puede simularlo."
 *
 * Todo el comportamiento físico emerge de las leyes del universo evaluadas
 * sobre ese estado. Nunca está en el componente.
 *
 * ── RELACIÓN CON EL PHYSICS CORE ─────────────────────────────────────────
 * PhysicsCoordinator descubre las entidades físicas mediante:
 *
 *   obj.getComponent(PhysicsComponent.class)
 *
 * y opera exclusivamente sobre el PhysicalState que contiene. El componente
 * es transparente al Core — solo provee acceso al estado.
 *
 * ── MIGRACIÓN DESDE PhysicalStateComponent ────────────────────────────────
 * PhysicsComponent reemplaza a PhysicalStateComponent (HRFC-019).
 * La API es idéntica. El cambio es semántico: el nuevo nombre refleja
 * correctamente la responsabilidad según HRFC-021.
 *
 * PhysicalStateComponent sigue existiendo por compatibilidad con código
 * existente del proyecto. Todo código nuevo debe usar PhysicsComponent.
 */
public final class PhysicsComponent extends Component {

    /** Estado físico de esta entidad. La única fuente de verdad. */
    private final PhysicalState state;

    // ── Constructor ───────────────────────────────────────────────────────

    /**
     * Crea un componente de física con el estado dado.
     *
     * @param state el estado físico de la entidad. No puede ser null.
     */
    public PhysicsComponent(PhysicalState state) {
        if (state == null)
            throw new IllegalArgumentException("state no puede ser null");
        this.state = state;
    }

    // ── Acceso al estado ──────────────────────────────────────────────────

    /**
     * El estado físico de la entidad.
     *
     * El PhysicsCoordinator lee y escribe exclusivamente a través de este
     * estado. Gameplay lo usa para observar el resultado de la simulación.
     *
     * El estado nunca contiene lógica. Solo contiene valores numéricos de
     * propiedades físicas.
     *
     * @return el PhysicalState de la entidad. Nunca null.
     */
    public PhysicalState getState() {
        return state;
    }

    // ── Ciclo de vida — Component ─────────────────────────────────────────

    /**
     * No realiza ninguna inicialización.
     * PhysicsComponent no contiene lógica de ciclo de vida.
     */
    @Override
    public void start() {}

    /**
     * No ejecuta ninguna lógica de frame.
     * Todo el comportamiento físico viene del PhysicsCoordinator.
     */
    @Override
    public void update() {}
}
