package Game.Engine.World.Solver;

import Game.Engine.Component;
import Game.Engine.World.Physics.PhysicalState;

/**
 * Componente que expone el PhysicalState de un objeto al sistema de componentes.
 *
 * ── HRFC-017 — Consolidación Definitiva del Modelo Declarativo ────────────
 *
 * ── RESPONSABILIDAD ──────────────────────────────────────────────────────
 * PhysicalStateComponent es el punto de entrada declarativo del sistema
 * físico para un objeto. El PhysicsSolver lo obtiene mediante
 * obj.getComponent(PhysicalStateComponent.class) para acceder al estado
 * físico del objeto sin conocer qué propiedades concretas tiene.
 *
 * ── RELACIÓN CON LOS COMPONENTES EXISTENTES ──────────────────────────────
 * Este componente no reemplaza ThermalComponent, ElectricalComponent, etc.
 * en la fase de transición. Coexiste con ellos.
 *
 * Para migrar un objeto al nuevo modelo:
 *
 *   // Opción A: estado completamente nuevo (sin componentes legacy)
 *   PhysicalState state = PhysicalState.builder()
 *       .register(PhysicalProperties.TEMPERATURE, 0.0)
 *       .register(PhysicalProperties.CHARGE, 0.0)
 *       .material(mat)
 *       .build();
 *   addComponent(new PhysicalStateComponent(state));
 *
 *   // Opción B: puente con componentes existentes (mismas instancias)
 *   ThermalComponent thermal = new ThermalComponent(20.0);
 *   PhysicalState state = PhysicalState.builder()
 *       .registerExisting(PhysicalProperties.TEMPERATURE, thermal.getQuantity())
 *       .material(mat)
 *       .build();
 *   addComponent(thermal);                         // para el sistema legacy
 *   addComponent(new PhysicalStateComponent(state)); // para el PhysicsSolver
 *
 * En la Opción B, el Solver y el sistema legacy operan sobre la misma
 * PhysicalQuantity — las modificaciones son inmediatamente visibles en ambos.
 *
 * ── DISEÑO ────────────────────────────────────────────────────────────────
 * PhysicalStateComponent es un envoltorio mínimo: solo almacena la referencia
 * al PhysicalState. Toda la lógica de acceso y modificación vive en
 * PhysicalState y PhysicsSolver.
 */
public final class PhysicalStateComponent extends Component {

    private final PhysicalState state;

    /**
     * Crea un componente que expone el estado físico dado.
     *
     * @param state estado físico del objeto. No puede ser null.
     */
    public PhysicalStateComponent(PhysicalState state) {
        if (state == null) throw new IllegalArgumentException("state no puede ser null");
        this.state = state;
    }

    /**
     * El estado físico del objeto.
     * El PhysicsSolver lee y escribe a través de este estado.
     *
     * @return el PhysicalState del objeto. Nunca null.
     */
    public PhysicalState getState() { return state; }
}
