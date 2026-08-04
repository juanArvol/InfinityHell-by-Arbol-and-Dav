package Game.Engine.Physics.Core;

import Game.Engine.Component;

/**
 * Componente que expone el PhysicalState de un objeto al PhysicsSolver.
 *
 * ── HRFC-019 — Eliminación Definitiva del Modelo Orientado a Tipos de Ley ─
 *
 * ── RESPONSABILIDAD ──────────────────────────────────────────────────────
 * PhysicalStateComponent es el único punto de acceso del PhysicsSolver al
 * estado físico de un objeto. El Solver lo obtiene mediante:
 *
 *   obj.getComponent(PhysicalStateComponent.class)
 *
 * y a partir de ahí opera exclusivamente sobre el PhysicalState que contiene,
 * sin conocer qué propiedades concretas tiene el objeto.
 *
 * ── MODELO HRFC-019 ───────────────────────────────────────────────────────
 * El estado físico de un objeto es un PhysicalState: un mapa plano de
 * identificador de propiedad → valor numérico.
 *
 * No hay ThermalComponent, ElectricalComponent, FluidComponent ni
 * PressureComponent separados. Todo el estado físico de un objeto vive
 * en un único PhysicalState. Las propiedades de estado (temperatura, carga,
 * humedad, presión) y las propiedades de material (conductividad, capacidad
 * calorífica, compresibilidad...) son todas entradas en ese mismo mapa.
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
     *
     * El PhysicsSolver lee y escribe a través de este estado.
     * Gameplay lo usa para observar el resultado de la simulación.
     *
     * @return el PhysicalState del objeto. Nunca null.
     */
    public PhysicalState getState() { return state; }
}
