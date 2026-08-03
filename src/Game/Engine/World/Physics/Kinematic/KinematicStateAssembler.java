package Game.Engine.World.Physics.Kinematic;

import Game.Engine.World.Physics.Contact.ContactState;
import Game.Engine.World.Physics.Core.PhysicalState;
import Game.Engine.World.Physics.Core.SimulationContext;
import Game.Engine.World.Physics.Environment.EnvironmentState;
import Game.Engine.World.Physics.Material.MaterialState;

/**
 * Helper de ensamblado para entidades con integración cinemática.
 *
 * ── HRFC-030 — Integración entre Kinematic Physics y World Physics ────────
 * ── HRFC-031 — Descomposición de PhysicalState en SimulationContext ───────
 * ── HRFC-032 — Evolución del SimulationContext hacia un registro extensible ─
 *
 * ── EVOLUCIÓN ARQUITECTÓNICA ─────────────────────────────────────────────
 *
 * HRFC-030 (diseño original):
 *   KinematicStateAssembler registraba PropertyDescriptors cinemáticos
 *   en PhysicalState. Los evaluadores los leían como propiedades del estado.
 *
 * HRFC-031:
 *   Los datos cinemáticos viven en StateSnapshot<KinematicState> dentro del
 *   SimulationContext. Este assembler construye el contexto compuesto.
 *
 * HRFC-032:
 *   SimulationContext incorpora un DomainStateRegistry genérico.
 *   Los nuevos dominios (Chemical, Optical, Acoustic, Nuclear, Biomechanical)
 *   se registran automáticamente con sus constantes neutras en el constructor
 *   de SimulationContext, por lo que los métodos de este assembler no
 *   necesitan declararlos explícitamente.
 *
 *   Para entidades con dominios específicos activos (p.ej. un personaje vivo
 *   con BiomechanicalState personalizado), usar el Builder directamente:
 *
 *     SimulationContext ctx = SimulationContext.builder(physical)
 *         .material(material)
 *         .environment(EnvironmentState.STANDARD)
 *         .register(BiomechanicalState.builder()
 *             .flexibility(0.9)
 *             .stiffness(0.2)
 *             .build())
 *         .register(ChemicalState.builder()
 *             .reactivity(0.3)
 *             .build())
 *         .build();
 *
 * ── RESPONSABILIDAD ACTUAL ────────────────────────────────────────────────
 * KinematicStateAssembler es el punto de entrada canónico para Assemblers de
 * entidades que necesitan integración cinemática completa:
 *
 *   SimulationContext ctx = KinematicStateAssembler.buildContext(physical, material);
 *   addComponent(new SimulationContextComponent(ctx));
 *   addComponent(new KinematicBridge());   // activa la integración automáticamente
 *
 * El KinematicBridge inicializa el StateSnapshot cinemático en el primer frame.
 * No es necesario pre-registrar ningún PropertyDescriptor cinemático.
 * Todos los nuevos dominios (HRFC-032) se registran en sus estados neutros
 * por defecto. Solo sobreescribir cuando la entidad tenga propiedades activas.
 *
 * ── PATRÓN DE USO EN ASSEMBLER ───────────────────────────────────────────
 *
 *   // Entidad con física completa + integración cinemática:
 *   PhysicalState physical = PhysicalState.builder()
 *       .register(ThermalProperties.TEMPERATURE, 20.0)
 *       .register(MechanicalProperties.PRESSURE)
 *       .build();
 *
 *   MaterialState material = MaterialState.builder()
 *       .thermalConductivity(0.8)
 *       .heatCapacity(500.0)
 *       .frictionCoefficient(0.4)
 *       .density(800.0)
 *       .build();
 *
 *   // Opción 1: helper completo (todos los dominios en valores por defecto)
 *   SimulationContext ctx = KinematicStateAssembler.buildContext(physical, material);
 *
 *   // Opción 2: control total con Builder (dominios especializados activos)
 *   SimulationContext ctx = SimulationContext.builder(physical)
 *       .material(material)
 *       .environment(EnvironmentState.STANDARD)
 *       .register(BiomechanicalState.builder().flexibility(0.9).build())
 *       .build();
 *
 *   addComponent(new SimulationContextComponent(ctx));
 *   addComponent(new KinematicBridge());
 *
 * ── DOMINIOS DISPONIBLES TRAS buildContext() ─────────────────────────────
 * Todo SimulationContext construido por este helper tendrá disponibles
 * los siguientes dominios (accesibles via context.state(X.class)):
 *
 *   PhysicalState        — el estado físico pasado como argumento
 *   KinematicState       — inicializado en el primer frame por KinematicBridge
 *   MaterialState        — el material pasado como argumento (o DEFAULT)
 *   ContactState         — ContactState.NONE (actualizado por CollisionsSystem)
 *   EnvironmentState     — EnvironmentState.STANDARD (o el entorno dado)
 *   ChemicalState        — ChemicalState.INERT       (neutro por defecto)
 *   OpticalState         — OpticalState.OPAQUE        (neutro por defecto)
 *   AcousticState        — AcousticState.SILENT       (neutro por defecto)
 *   NuclearState         — NuclearState.STABLE        (neutro por defecto)
 *   BiomechanicalState   — BiomechanicalState.RESTED  (neutro por defecto)
 *
 * ── ENTORNOS ESPECIALES ───────────────────────────────────────────────────
 * Para entidades en entornos con gravedad, temperatura o fluido diferente
 * al estándar, usar buildContext(physical, material, environment) o el Builder.
 *
 * ── INVARIANTE ────────────────────────────────────────────────────────────
 *   ✗ No registra PropertyDescriptors cinemáticos en PhysicalState.
 *   ✗ No referencia KinematicStateProperties.
 *   ✓ Construye SimulationContext correctamente configurado.
 *   ✓ El historial cinemático lo gestiona KinematicBridge automáticamente.
 *   ✓ Todos los dominios de HRFC-032 están presentes en sus valores neutros.
 */
public final class KinematicStateAssembler {

    private KinematicStateAssembler() {}

    // ── Builders de SimulationContext ─────────────────────────────────────

    /**
     * Construye un SimulationContext con integración cinemática completa
     * y condiciones de entorno estándar.
     *
     * Todos los dominios de HRFC-032 se registran automáticamente en sus
     * constantes neutras (INERT, OPAQUE, SILENT, STABLE, RESTED).
     * Para sobrescribir alguno, usar el Builder de SimulationContext directamente.
     *
     * @param physical el estado físico del objeto. No puede ser null.
     * @param material las propiedades del material. Null usa DEFAULT.
     * @return SimulationContext configurado con material y entorno estándar.
     */
    public static SimulationContext buildContext(PhysicalState physical,
                                                 MaterialState  material) {
        return SimulationContext.builder(physical)
            .material(material != null ? material : MaterialState.DEFAULT)
            .contact(ContactState.NONE)
            .environment(EnvironmentState.STANDARD)
            .build();
    }

    /**
     * Construye un SimulationContext con integración cinemática completa
     * y un entorno personalizado.
     *
     * Usar cuando la entidad nace en un entorno con gravedad, temperatura
     * o fluido diferente al estándar (bajo el agua, en el espacio, etc.).
     *
     * @param physical    el estado físico del objeto. No puede ser null.
     * @param material    las propiedades del material. Null usa DEFAULT.
     * @param environment las condiciones del entorno. Null usa STANDARD.
     * @return SimulationContext configurado con material y entorno dados.
     */
    public static SimulationContext buildContext(PhysicalState    physical,
                                                 MaterialState    material,
                                                 EnvironmentState environment) {
        return SimulationContext.builder(physical)
            .material(material    != null ? material    : MaterialState.DEFAULT)
            .contact(ContactState.NONE)
            .environment(environment != null ? environment : EnvironmentState.STANDARD)
            .build();
    }

    /**
     * Construye un SimulationContext mínimo con solo estado físico.
     * Sin material personalizado, sin entorno especial.
     *
     * Todos los dominios de HRFC-032 estarán presentes en sus valores neutros.
     *
     * @param physical el estado físico del objeto. No puede ser null.
     * @return SimulationContext con todos los dominios en valores por defecto.
     */
    public static SimulationContext buildMinimalContext(PhysicalState physical) {
        return SimulationContext.ofPhysical(physical);
    }
}
