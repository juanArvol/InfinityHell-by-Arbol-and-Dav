package Game.Engine.World.Physics.Core;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Descripción declarativa e inmutable de una relación física entre propiedades.
 *
 * ── HRFC-022 — Eliminación del Paradigma de Ley Ejecutable ───────────────
 *
 * ── PRINCIPIO FUNDAMENTAL ─────────────────────────────────────────────────
 * PhysicalRelation describe conocimiento físico.
 * PhysicalRelation nunca ejecuta nada.
 * PhysicalRelation nunca selecciona entidades.
 * PhysicalRelation nunca modifica propiedades.
 * PhysicalRelation nunca decide el flujo de la simulación.
 * PhysicalRelation nunca contiene algoritmos.
 * PhysicalRelation nunca contiene callbacks.
 * PhysicalRelation nunca contiene lambdas.
 * PhysicalRelation nunca contiene referencias al mundo.
 *
 * Toda la responsabilidad de ejecución pertenece exclusivamente al sistema
 * de resolución, mediante evaluadores especializados por RelationType.
 *
 * ── MODELO CONCEPTUAL ─────────────────────────────────────────────────────
 *
 *   PhysicalRelation
 *       ↓
 *   ParticipatingProperties   →  qué propiedades participan en la relación
 *       ↓
 *   RelationType              →  qué fenómeno físico identifica la relación
 *       ↓
 *   Constraints               →  restricciones físicas asociadas
 *
 * ── CONTENIDO ─────────────────────────────────────────────────────────────
 *
 *   name                    → nombre legible de la relación. Solo para depuración.
 *
 *   participatingProperties → conjunto de propiedades físicas que participan
 *                             en esta relación. El evaluador del sistema de
 *                             resolución las utiliza para leer y producir cambios.
 *
 *   relationType            → identifica qué fenómeno físico describe esta
 *                             relación. El sistema de resolución lo usa para
 *                             seleccionar el evaluador especializado correcto.
 *                             No es una fórmula. No es una función. Solo identifica.
 *
 *   constraints             → conjunto de restricciones físicas declarativas.
 *                             Describen condiciones bajo las cuales la relación
 *                             es activa o válida. El evaluador las consulta para
 *                             decidir si aplica la relación en un contexto dado.
 *
 *   priority                → orden relativo de evaluación dentro de un frame.
 *                             Menor valor = se evalúa antes.
 *
 * ── QUÉ NO CONTIENE ──────────────────────────────────────────────────────
 *   ✗ Ningún método solve().
 *   ✗ Ninguna referencia a WorldContext.
 *   ✗ Ninguna interfaz funcional.
 *   ✗ Ningún callback ni lambda.
 *   ✗ Ningún algoritmo de simulación.
 *   ✗ Ninguna referencia a PhysicsLaw.
 *   ✗ Ninguna iteración sobre entidades.
 *   ✗ Ninguna modificación de propiedades.
 *
 * ── RESPONSABILIDADES SEPARADAS ───────────────────────────────────────────
 *   PhysicalRelation       → describe conocimiento físico.
 *   RelationType           → identifica el fenómeno físico.
 *   RelationEvaluator      → implementa la matemática correspondiente.
 *   PhysicsSolver          → coordina el proceso completo de simulación.
 *
 * ── ROL EN EL DEPENDENCY GRAPH ───────────────────────────────────────────
 * PhysicalRelation es la carga útil (payload) de las aristas del
 * PropertyDependencyGraph. Cada arista declara:
 *
 *   "La propiedad A influye sobre la propiedad B
 *    mediante la relación física R de tipo T."
 *
 * El grafo deja de contener referencias a objetos ejecutables.
 * El grafo representa únicamente relaciones físicas entre propiedades.
 *
 * ── EXTENSIBILIDAD ────────────────────────────────────────────────────────
 * Añadir una nueva relación física:
 *   1. Añadir la constante en RelationType.
 *   2. Crear el evaluador en Game.Engine.World.Solver.
 *   3. Registrar el evaluador en EvaluatorRegistry.
 *   4. Declarar la PhysicalRelation en el catálogo correspondiente.
 *
 * No se modifica PhysicalRelation. No se modifica PhysicsSolver. No se
 * modifica ningún otro componente del Core.
 *
 * ── USO EN UN CATÁLOGO ───────────────────────────────────────────────────
 *
 *   // Conducción térmica entre dos objetos (ley de Fourier):
 *   PhysicalRelation THERMAL_CONDUCTION = PhysicalRelation.builder()
 *       .name("thermal_conduction")
 *       .relationType(RelationType.FOURIER)
 *       .participating(ThermalProperties.TEMPERATURE,
 *                      ThermalProperties.THERMAL_CONDUCTIVITY,
 *                      ThermalProperties.HEAT_CAPACITY)
 *       .priority(100)
 *       .build();
 *
 *   // Transferencia de carga eléctrica (ley de Ohm):
 *   PhysicalRelation ELECTRICAL_TRANSFER = PhysicalRelation.builder()
 *       .name("electrical_transfer")
 *       .relationType(RelationType.OHM)
 *       .participating(ElectricalProperties.CHARGE,
 *                      ElectricalProperties.ELECTRICAL_CONDUCTIVITY)
 *       .priority(100)
 *       .build();
 */
public final class PhysicalRelation {

    /** Nombre legible. Solo para depuración y logging. */
    private final String name;

    /**
     * Propiedades físicas que participan en esta relación.
     * El evaluador correspondiente las usa para leer el estado actual
     * y producir los deltas que el sistema de resolución aplicará.
     */
    private final Set<PropertyDescriptor> participatingProperties;

    /**
     * Identifica qué fenómeno físico describe esta relación.
     * El sistema de resolución lo usa para seleccionar el evaluador correcto.
     * No es una fórmula. No es una función. Solo identifica.
     */
    private final RelationType relationType;

    /**
     * Restricciones físicas asociadas a esta relación.
     * El evaluador las consulta para decidir si la relación aplica
     * en un contexto concreto.
     */
    private final Set<RelationConstraint> constraints;

    /**
     * Prioridad de evaluación. Menor valor = se evalúa antes.
     * Por defecto: 100.
     */
    private final int priority;

    // ── Constructor privado — usar Builder ────────────────────────────────

    private PhysicalRelation(Builder b) {
        if (b.relationType == null)
            throw new IllegalArgumentException("relationType no puede ser null");
        this.name                   = b.name;
        this.relationType           = b.relationType;
        this.participatingProperties =
            Collections.unmodifiableSet(new LinkedHashSet<>(b.participatingProperties));
        this.constraints            =
            Collections.unmodifiableSet(new LinkedHashSet<>(b.constraints));
        this.priority               = b.priority;
    }

    // ── Accesores ─────────────────────────────────────────────────────────

    /**
     * Nombre legible de la relación. Puede ser null.
     * Solo para depuración. No afecta al comportamiento del sistema.
     *
     * @return nombre, o null si no fue definido.
     */
    public String getName() {
        return name;
    }

    /**
     * Identifica qué fenómeno físico describe esta relación.
     * El sistema de resolución lo usa para seleccionar el evaluador correcto.
     *
     * @return el tipo de relación. Nunca null.
     */
    public RelationType getRelationType() {
        return relationType;
    }

    /**
     * Propiedades físicas que participan en esta relación.
     * El evaluador las usa para leer el estado y producir cambios.
     *
     * @return conjunto inmutable de descriptores de propiedades. Nunca null.
     */
    public Set<PropertyDescriptor> getParticipatingProperties() {
        return participatingProperties;
    }

    /**
     * Restricciones físicas asociadas a esta relación.
     * El evaluador las consulta para decidir si la relación aplica.
     *
     * @return conjunto inmutable de restricciones. Nunca null.
     */
    public Set<RelationConstraint> getConstraints() {
        return constraints;
    }

    /**
     * True si la relación tiene la restricción del tipo indicado.
     *
     * @param type tipo de restricción a buscar.
     * @return true si existe una restricción de ese tipo.
     */
    public boolean hasConstraint(RelationConstraint.Type type) {
        for (RelationConstraint c : constraints)
            if (c.getType() == type) return true;
        return false;
    }

    /**
     * Retorna la primera restricción del tipo indicado, o null si no existe.
     *
     * @param type tipo de restricción a buscar.
     * @return la restricción, o null.
     */
    public RelationConstraint getConstraint(RelationConstraint.Type type) {
        for (RelationConstraint c : constraints)
            if (c.getType() == type) return c;
        return null;
    }

    /**
     * Prioridad de evaluación. Menor = se evalúa antes.
     *
     * @return prioridad. Por defecto 100.
     */
    public int getPriority() {
        return priority;
    }

    // ── Object ────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        String n = name != null ? name : relationType.name();
        return "PhysicalRelation[" + n + " type=" + relationType
            + " props=" + participatingProperties.size()
            + " priority=" + priority + "]";
    }

    // ── Builder ───────────────────────────────────────────────────────────

    /** Punto de entrada del Builder. */
    public static Builder builder() { return new Builder(); }

    /**
     * Builder de PhysicalRelation.
     *
     * Solo relationType y al menos una propiedad participante son obligatorios.
     */
    public static final class Builder {

        private String                     name                   = null;
        private RelationType               relationType           = null;
        private final Set<PropertyDescriptor>  participatingProperties =
            new LinkedHashSet<>();
        private final Set<RelationConstraint>  constraints            =
            new LinkedHashSet<>();
        private int                        priority               = 100;

        private Builder() {}

        /**
         * Nombre legible de la relación. Opcional.
         * Solo para depuración.
         *
         * @param n nombre de la relación.
         */
        public Builder name(String n) {
            this.name = n;
            return this;
        }

        /**
         * Tipo de relación física. Obligatorio.
         * Identifica el fenómeno y el evaluador a usar.
         *
         * @param type el tipo de relación. No puede ser null al construir.
         */
        public Builder relationType(RelationType type) {
            this.relationType = type;
            return this;
        }

        /**
         * Declara las propiedades físicas que participan en esta relación.
         * El evaluador las usará para leer y producir cambios.
         *
         * @param descriptors descriptores de propiedades participantes.
         */
        public Builder participating(PropertyDescriptor... descriptors) {
            if (descriptors != null)
                for (PropertyDescriptor d : descriptors)
                    if (d != null) participatingProperties.add(d);
            return this;
        }

        /**
         * Añade una restricción física a la relación.
         *
         * @param constraint la restricción. Ignorada si null.
         */
        public Builder constraint(RelationConstraint constraint) {
            if (constraint != null) constraints.add(constraint);
            return this;
        }

        /**
         * Prioridad de evaluación. Menor = antes.
         * Por defecto: 100.
         *
         * @param p prioridad.
         */
        public Builder priority(int p) {
            this.priority = p;
            return this;
        }

        /** Construye la PhysicalRelation con la configuración acumulada. */
        public PhysicalRelation build() {
            return new PhysicalRelation(this);
        }
    }
}
