package Game.Engine.World.Physics.Core;

import Game.Engine.World.Physics.Acoustic.AcousticState;
import Game.Engine.World.Physics.Biomechanical.BiomechanicalState;
import Game.Engine.World.Physics.Chemical.ChemicalState;
import Game.Engine.World.Physics.Contact.ContactState;
import Game.Engine.World.Physics.Environment.EnvironmentState;
import Game.Engine.World.Physics.Kinematic.KinematicState;
import Game.Engine.World.Physics.Material.MaterialState;
import Game.Engine.World.Physics.Nuclear.NuclearState;
import Game.Engine.World.Physics.Optical.OpticalState;

/**
 * Contexto de simulación compuesto — registro extensible de Domain States.
 *
 * ── HRFC-031 — Descomposición de PhysicalState en SimulationContext ───────
 * ── HRFC-032 — Evolución del SimulationContext hacia un registro extensible ─
 *
 * ── RESPONSABILIDAD ──────────────────────────────────────────────────────
 * SimulationContext es el punto de acceso único al conjunto de estados de
 * dominio que describen a una entidad dentro del simulador.
 *
 * HRFC-031 introdujo el concepto de "contexto compuesto" con getters
 * específicos para cada dominio conocido. HRFC-032 completa esa evolución
 * convirtiendo SimulationContext en un registro genérico:
 *
 *   ── Antes (HRFC-031) ──────────────────────────────────────────────────
 *   context.getPhysicalState()
 *   context.getMaterialState()
 *   context.getContactState()
 *   context.getEnvironmentState()
 *
 *   ── Ahora (HRFC-032) ─────────────────────────────────────────────────
 *   context.state(PhysicalState.class)
 *   context.state(MaterialState.class)
 *   context.state(ContactState.class)
 *   context.state(EnvironmentState.class)
 *   context.state(ChemicalState.class)      // nuevo dominio, sin tocar este archivo
 *   context.state(AcousticState.class)      // nuevo dominio, sin tocar este archivo
 *
 * Los getters específicos de HRFC-031 se mantienen como wrappers que
 * delegan en el acceso genérico, garantizando retrocompatibilidad total
 * con todos los evaluadores y sistemas existentes.
 *
 * ── ESTRUCTURA INTERNA ────────────────────────────────────────────────────
 *
 *   SimulationContext
 *       │
 *       ├── physical (campo dedicado)
 *       │       La única fuente de verdad mutable del frame: temperatura,
 *       │       presión, carga eléctrica… Es el único estado que el
 *       │       WorkingState/commit modifica directamente. Por ello vive
 *       │       como campo dedicado y NO en el registro.
 *       │
 *       ├── kinematicSnapshot (campo dedicado)
 *       │       StateSnapshot<KinematicState> — contiene current + previous.
 *       │       No es un DomainState en sí mismo; es un contenedor de historial.
 *       │       Los accesos a KinematicState se hacen vía currentKinematic()
 *       │       o kinematic(). También puede accederse la instancia del frame
 *       │       actual vía state(KinematicState.class) (ver nota abajo).
 *       │
 *       └── DomainStateRegistry
 *               Registro genérico indexado por tipo.
 *               Contiene: MaterialState, ContactState, EnvironmentState
 *               y cualquier dominio nuevo que se registre.
 *
 * ── NOTA SOBRE KinematicState EN EL REGISTRO ─────────────────────────────
 * state(KinematicState.class) retorna currentKinematic() — el estado del
 * frame actual — en lugar del snapshot. Esto hace que los evaluadores que
 * solo necesitan el estado actual (sin delta temporal) puedan consumir
 * KinematicState de forma uniforme con el resto de dominios.
 * Para deltas temporales, usar kinematic() / hasKinematic() como siempre.
 *
 * ── PRINCIPIO FUNDAMENTAL ─────────────────────────────────────────────────
 * SimulationContext conecta. No calcula. No transforma. No produce efectos.
 *
 *   State            → describe
 *   Relation         → interpreta
 *   System           → ejecuta
 *   SimulationContext → conecta y registra
 *
 * ── MUTABILIDAD CONTROLADA ────────────────────────────────────────────────
 * Solo los sistemas autorizados deben llamar a los métodos de actualización:
 *   - KinematicBridge         → updateKinematic()
 *   - CollisionsSystem        → updateContact()
 *   - WorldFieldSystem        → updateEnvironment()
 *   - Assemblers / runtime    → register(DomainState)
 *
 * ── EXTENSIBILIDAD ────────────────────────────────────────────────────────
 * Para incorporar un nuevo dominio de simulación:
 *   1. Crear la clase del nuevo estado implementando DomainState.
 *   2. Registrarla en SimulationContext.Builder (o en runtime via register()).
 *   3. Acceder con context.state(NuevoDominio.class).
 *
 * Ningún getter específico es necesario. El núcleo del Engine no cambia.
 *
 * ── THREAD SAFETY ─────────────────────────────────────────────────────────
 * No es thread-safe. Usar exclusivamente desde el game loop thread.
 */
public final class SimulationContext {

    // ── Dominio físico — campo dedicado (no en el registro) ───────────────

    /**
     * Estado físico del objeto.
     * Única fuente de verdad para propiedades físicas persistentes:
     * temperatura, presión, carga eléctrica, humedad, energía interna…
     *
     * Es el único estado que el ciclo WorkingState/commit modifica cada frame.
     * Por ello vive como campo dedicado fuera del registro: el registro no
     * tiene semántica de "estado mutable a lo largo del frame".
     *
     * Nunca null. Obligatorio para participar en la simulación.
     */
    private final PhysicalState physical;

    // ── Dominio cinemático — campo dedicado (historial via StateSnapshot) ──

    /**
     * Snapshot del estado cinemático: frame actual y frame anterior.
     * Null si la entidad no tiene integración cinemática activa.
     *
     * Vive como campo dedicado porque es un StateSnapshot<KinematicState>,
     * no un KinematicState directamente. El registro expone el estado actual
     * (currentKinematic()) via state(KinematicState.class) para acceso uniforme.
     *
     * Actualizado cada frame por KinematicBridge.
     */
    private StateSnapshot<KinematicState> kinematicSnapshot;

    // ── Registro genérico de dominios ─────────────────────────────────────

    /**
     * Registro indexado por tipo de todos los demás Domain States.
     *
     * Contiene por defecto: MaterialState, ContactState, EnvironmentState.
     * Puede contener además: ChemicalState, OpticalState, AcousticState,
     * NuclearState, BiomechanicalState, y cualquier dominio futuro.
     *
     * Acceso: context.state(MaterialState.class)
     */
    private final DomainStateRegistry registry;

    // ── Constructor privado — usar Builder ────────────────────────────────

    private SimulationContext(Builder b) {
        this.physical          = b.physical;
        this.kinematicSnapshot = b.kinematicSnapshot;
        this.registry          = b.registry;

        // ── Dominios base — siempre presentes ────────────────────────────
        if (!this.registry.has(MaterialState.class))
            this.registry.register(MaterialState.DEFAULT);
        if (!this.registry.has(ContactState.class))
            this.registry.register(ContactState.NONE);
        if (!this.registry.has(EnvironmentState.class))
            this.registry.register(EnvironmentState.STANDARD);

        // ── Dominios extendidos (HRFC-032) — neutrales si no se declaran ─
        // Los nuevos dominios se registran con sus constantes neutras para que
        // todo evaluador pueda acceder a ellos vía state(X.class) sin null-check
        // obligatorio en entidades que no los necesitan activamente.
        if (!this.registry.has(ChemicalState.class))
            this.registry.register(ChemicalState.INERT);
        if (!this.registry.has(OpticalState.class))
            this.registry.register(OpticalState.OPAQUE);
        if (!this.registry.has(AcousticState.class))
            this.registry.register(AcousticState.SILENT);
        if (!this.registry.has(NuclearState.class))
            this.registry.register(NuclearState.STABLE);
        if (!this.registry.has(BiomechanicalState.class))
            this.registry.register(BiomechanicalState.RESTED);
    }

    // ── Factory ───────────────────────────────────────────────────────────

    /** Punto de entrada del Builder. */
    public static Builder builder(PhysicalState physical) {
        return new Builder(physical);
    }

    /**
     * Contexto mínimo con solo estado físico.
     * Sin integración cinemática. Dominios no físicos en valores por defecto.
     *
     * @param physical el estado físico de la entidad. No puede ser null.
     * @return SimulationContext con valores por defecto para todos los dominios.
     */
    public static SimulationContext ofPhysical(PhysicalState physical) {
        return builder(physical).build();
    }

    // ── Acceso genérico al registro ───────────────────────────────────────

    /**
     * Retorna el estado del dominio identificado por el tipo dado.
     *
     * Es el mecanismo de acceso canónico de HRFC-032. Permite a los
     * evaluadores consumir cualquier dominio sin que SimulationContext
     * necesite getters específicos:
     *
     *   KinematicState  kin  = context.state(KinematicState.class);
     *   MaterialState   mat  = context.state(MaterialState.class);
     *   ContactState    con  = context.state(ContactState.class);
     *   ChemicalState   chem = context.state(ChemicalState.class);
     *
     * Para KinematicState retorna el estado del frame actual (currentKinematic()).
     * Para todos los demás dominios delega en el DomainStateRegistry.
     *
     * Retorna null si el dominio no está registrado. Los evaluadores deben
     * verificar null antes de consumir un dominio opcional.
     *
     * @param type tipo del estado de dominio a buscar. No puede ser null.
     * @param <T>  el tipo del estado de dominio.
     * @return la instancia registrada, o null si no hay ninguna para ese tipo.
     */
    @SuppressWarnings("unchecked")
    public <T extends DomainState> T state(Class<T> type) {
        if (type == null) return null;

        // KinematicState: acceso especial al frame actual del snapshot
        if (type == KinematicState.class) {
            return (T) currentKinematic();
        }

        // PhysicalState: campo dedicado
        if (type == PhysicalState.class) {
            return (T) physical;
        }

        // Todos los demás dominios: registro genérico
        return registry.get(type);
    }

    /**
     * True si hay un estado registrado para el tipo dado.
     *
     * Para KinematicState equivale a hasKinematic().
     * Para PhysicalState siempre retorna true.
     *
     * @param type tipo del estado a verificar.
     * @return true si el dominio está disponible.
     */
    public boolean hasState(Class<? extends DomainState> type) {
        if (type == null) return false;
        if (type == KinematicState.class) return hasKinematic();
        if (type == PhysicalState.class)  return true;
        return registry.has(type);
    }

    /**
     * Registra (o reemplaza) un estado de dominio en tiempo de ejecución.
     *
     * Usar para incorporar nuevos dominios que no se configuraron en el
     * Builder, o para actualizar estados de dominio en runtime.
     *
     * No puede usarse para reemplazar PhysicalState ni el kinematicSnapshot
     * (que tienen campos dedicados y métodos de actualización específicos).
     *
     * @param state el estado de dominio a registrar. Ignorado si null.
     */
    public void register(DomainState state) {
        registry.register(state);
    }

    /**
     * Vista de solo lectura del DomainStateRegistry interno.
     *
     * Usar para introspección: depuración, serialización, herramientas de
     * desarrollo. No modificar el registro a través de esta referencia;
     * usar register() o los métodos updateXxx() para actualizaciones.
     *
     * @return el DomainStateRegistry de este contexto. Nunca null.
     */
    public DomainStateRegistry registry() {
        return registry;
    }

    // ── Acceso a dominios — API de HRFC-031 (retrocompatibilidad) ─────────

    /**
     * Estado físico del objeto.
     * Propiedades físicas persistentes: temperatura, presión, carga…
     *
     * @return PhysicalState. Nunca null.
     */
    public PhysicalState physical() {
        return physical;
    }

    /**
     * Snapshot del estado cinemático (current + previous frame).
     * Null si la entidad no tiene integración cinemática activa.
     *
     * Siempre verificar {@link #hasKinematic()} antes de llamar a este método.
     *
     * @return StateSnapshot con el estado cinemático actual y anterior, o null.
     */
    public StateSnapshot<KinematicState> kinematic() {
        return kinematicSnapshot;
    }

    /**
     * Estado cinemático del frame actual.
     * Shortcut de kinematic().current() con protección null.
     *
     * @return KinematicState del frame actual, o null si no hay integración cinemática.
     */
    public KinematicState currentKinematic() {
        return kinematicSnapshot != null ? kinematicSnapshot.current() : null;
    }

    /**
     * Estado cinemático del frame anterior.
     * Shortcut de kinematic().previous() con protección null.
     *
     * @return KinematicState del frame anterior, o null si no hay integración cinemática.
     */
    public KinematicState previousKinematic() {
        return kinematicSnapshot != null ? kinematicSnapshot.previous() : null;
    }

    /**
     * True si la entidad tiene integración cinemática activa.
     *
     * @return true si kinematicSnapshot != null.
     */
    public boolean hasKinematic() {
        return kinematicSnapshot != null;
    }

    /**
     * Propiedades intrínsecas del material.
     * Conductividad, elasticidad, dureza, coeficiente de fricción…
     *
     * @return MaterialState. Nunca null (fallback a DEFAULT).
     */
    public MaterialState material() {
        MaterialState m = registry.get(MaterialState.class);
        return m != null ? m : MaterialState.DEFAULT;
    }

    /**
     * Estado de contacto instantáneo con otros cuerpos.
     * onGround, fricción de superficie, fuerzas de contacto…
     *
     * @return ContactState. Nunca null (fallback a NONE).
     */
    public ContactState contact() {
        ContactState c = registry.get(ContactState.class);
        return c != null ? c : ContactState.NONE;
    }

    /**
     * Condiciones del entorno donde existe el objeto.
     * Temperatura ambiente, gravedad local, viento, campos externos…
     *
     * @return EnvironmentState. Nunca null (fallback a STANDARD).
     */
    public EnvironmentState environment() {
        EnvironmentState e = registry.get(EnvironmentState.class);
        return e != null ? e : EnvironmentState.STANDARD;
    }

    // ── Actualización de dominios — sistemas autorizados ─────────────────

    /**
     * Actualiza el snapshot cinemático con el estado del frame actual.
     *
     * Invocado exclusivamente por KinematicBridge cada frame.
     *
     * @param newState el KinematicState calculado para este frame. No puede ser null.
     */
    public void updateKinematic(KinematicState newState) {
        if (newState == null) return;
        if (kinematicSnapshot == null) {
            kinematicSnapshot = StateSnapshot.initial(newState);
        } else {
            kinematicSnapshot = kinematicSnapshot.advance(newState);
        }
    }

    /**
     * Reemplaza el estado de material.
     *
     * @param newMaterial el nuevo estado de material. Si null, usa DEFAULT.
     */
    public void updateMaterial(MaterialState newMaterial) {
        registry.register(newMaterial != null ? newMaterial : MaterialState.DEFAULT);
    }

    /**
     * Reemplaza el estado de contacto con el producido por el frame actual.
     *
     * Invocado por el sistema de colisiones al finalizar su fase de detección.
     *
     * @param newContact el ContactState del frame actual. Puede ser null.
     */
    public void updateContact(ContactState newContact) {
        registry.register(newContact != null ? newContact : ContactState.NONE);
    }

    /**
     * Reemplaza el estado del entorno.
     *
     * @param newEnvironment el nuevo EnvironmentState. Si null, usa STANDARD.
     */
    public void updateEnvironment(EnvironmentState newEnvironment) {
        registry.register(newEnvironment != null ? newEnvironment : EnvironmentState.STANDARD);
    }

    // ── Object ────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "SimulationContext["
            + "physical=" + physical
            + ", kinematic=" + (kinematicSnapshot != null
                ? kinematicSnapshot.current() : "none")
            + ", registry=" + registry
            + "]";
    }

    // ═════════════════════════════════════════════════════════════════════
    // Builder
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Builder de SimulationContext.
     *
     * El PhysicalState es el único parámetro obligatorio.
     * El registro se construye progresivamente: cada llamada a material(),
     * contact(), environment() o register() añade el estado al registro
     * interno que se transferirá al SimulationContext al construirlo.
     *
     * Dominios con valores por defecto si no se declaran:
     *   material     = MaterialState.DEFAULT
     *   contact      = ContactState.NONE
     *   environment  = EnvironmentState.STANDARD
     *
     * ── Patrón de construcción típico ─────────────────────────────────────
     *
     *   SimulationContext ctx = SimulationContext.builder(physical)
     *       .material(material)
     *       .contact(ContactState.NONE)
     *       .environment(EnvironmentState.STANDARD)
     *       .register(ChemicalState.INERT)
     *       .build();
     *
     * ── Patrón con acceso genérico ────────────────────────────────────────
     *
     *   // Cualquier evaluador puede acceder al dominio químico:
     *   ChemicalState chem = context.state(ChemicalState.class);
     *   if (chem == null) continue; // dominio no disponible en esta entidad
     */
    public static final class Builder {

        private final PhysicalState           physical;
        private       StateSnapshot<KinematicState> kinematicSnapshot = null;
        private final DomainStateRegistry     registry              = new DomainStateRegistry();

        private Builder(PhysicalState physical) {
            if (physical == null)
                throw new IllegalArgumentException("physical no puede ser null");
            this.physical = physical;
        }

        /**
         * Establece el snapshot cinemático inicial.
         * En la mayoría de los casos KinematicBridge lo inicializa automáticamente.
         *
         * @param snapshot el snapshot inicial. Puede ser null.
         * @return this.
         */
        public Builder kinematic(StateSnapshot<KinematicState> snapshot) {
            this.kinematicSnapshot = snapshot;
            return this;
        }

        /**
         * Establece el MaterialState del objeto.
         * Si no se llama, se usa MaterialState.DEFAULT.
         *
         * @param material el estado de material. Puede ser null.
         * @return this.
         */
        public Builder material(MaterialState material) {
            registry.register(material != null ? material : MaterialState.DEFAULT);
            return this;
        }

        /**
         * Establece el ContactState inicial.
         * Si no se llama, se usa ContactState.NONE.
         *
         * @param contact el estado de contacto inicial. Puede ser null.
         * @return this.
         */
        public Builder contact(ContactState contact) {
            registry.register(contact != null ? contact : ContactState.NONE);
            return this;
        }

        /**
         * Establece el EnvironmentState.
         * Si no se llama, se usa EnvironmentState.STANDARD.
         *
         * @param environment el entorno del objeto. Puede ser null.
         * @return this.
         */
        public Builder environment(EnvironmentState environment) {
            registry.register(environment != null ? environment : EnvironmentState.STANDARD);
            return this;
        }

        /**
         * Registra cualquier DomainState adicional en el contexto.
         *
         * Usar para incorporar dominios nuevos (ChemicalState, OpticalState…)
         * o para sobrescribir dominios ya registrados con valores específicos.
         *
         * @param state el estado de dominio a registrar. Ignorado si null.
         * @return this.
         */
        public Builder register(DomainState state) {
            registry.register(state);
            return this;
        }

        /** Construye el SimulationContext con la configuración acumulada. */
        public SimulationContext build() {
            return new SimulationContext(this);
        }
    }
}
