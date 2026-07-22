package Game.Engine.World.Physics.Runtime;

import Game.Engine.Component;
import Game.Engine.World.Physics.Core.PhysicalState;

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
 * ── USO EN ASSEMBLER ──────────────────────────────────────────────────────
 *
 *   // Objeto con física térmica, eléctrica y fluídica:
 *   PhysicalState state = PhysicalState.builder()
 *       // propiedades de estado
 *       .register(CoreProperties.TEMPERATURE, 20.0)
 *       .register(CoreProperties.CHARGE)
 *       .register(CoreProperties.HUMIDITY)
 *       .register(CoreProperties.PRESSURE)
 *       // propiedades de material (las leyes las leen con get() igual que el estado)
 *       .register(CoreProperties.THERMAL_CONDUCTIVITY, 0.8)
 *       .register(CoreProperties.HEAT_CAPACITY, 500.0)
 *       .register(CoreProperties.THERMAL_DIFFUSIVITY, 0.6)
 *       .register(CoreProperties.ELECTRICAL_CONDUCTIVITY, 0.9)
 *       .register(CoreProperties.HUMIDITY_ABSORPTION, 0.05)
 *       .register(CoreProperties.COMPRESSIBILITY, 0.05)
 *       .build();
 *   addComponent(new PhysicalStateComponent(state));
 *
 *   // Objeto solo con física térmica:
 *   PhysicalState state = PhysicalState.builder()
 *       .register(CoreProperties.TEMPERATURE, 0.0)
 *       .register(CoreProperties.THERMAL_CONDUCTIVITY, 0.5)
 *       .register(CoreProperties.HEAT_CAPACITY, 1000.0)
 *       .register(CoreProperties.THERMAL_DIFFUSIVITY, 0.1)
 *       .build();
 *   addComponent(new PhysicalStateComponent(state));
 *
 *   // Objeto con propiedad electromagnética (campo magnético):
 *   PhysicalState state = PhysicalState.builder()
 *       .register(ElectromagneticProperties.MAGNETIC_FIELD, 5.0)
 *       .build();
 *   addComponent(new PhysicalStateComponent(state));
 *
 * ── LECTURA DESDE GAMEPLAY ────────────────────────────────────────────────
 *
 *   PhysicalStateComponent psc = obj.getComponent(PhysicalStateComponent.class);
 *   if (psc != null) {
 *       double temp     = psc.getState().get(CoreProperties.TEMPERATURE);
 *       double charge   = psc.getState().get(CoreProperties.CHARGE);
 *       double humidity = psc.getState().get(CoreProperties.HUMIDITY);
 *   }
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
