package Game.Engine.World.Physics.Core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Descripción declarativa de una relación física entre propiedades del mundo.
 *
 * ── HRFC-022 — Eliminación del Paradigma de Ley Ejecutable ───────────────
 * ── HRFC-027 — Auditoría de Consistencia Arquitectónica ──────────────────
 *
 * ── QUÉ ES PhysicalRelation ───────────────────────────────────────────────
 * PhysicalRelation describe un fenómeno físico de forma declarativa:
 *   - qué tipo de fenómeno es (RelationType)
 *   - qué propiedades participan en él
 *   - bajo qué condiciones es activo (RelationConstraint)
 *   - en qué orden debe evaluarse (priority)
 *
 * PhysicalRelation NO ejecuta nada. NO contiene matemática.
 * El procedimiento matemático vive exclusivamente en el RelationEvaluator
 * correspondiente, registrado en EvaluatorRegistry por RelationType.
 *
 * ── SEPARACIÓN FUNDAMENTAL ───────────────────────────────────────────────
 *
 *   PhysicalRelation   → describe el fenómeno (QUÉ ocurre)
 *   RelationEvaluator  → implementa la matemática (CÓMO se calcula)
 *   PhysicsSolver      → coordina la resolución completa del frame
 *
 * ── PRIORIDAD ─────────────────────────────────────────────────────────────
 * El PhysicsSolver evalúa las relaciones ordenadas por prioridad ascendente.
 * Una relación con prioridad 1 se evalúa antes que una con prioridad 100.
 * Las prioridades garantizan el orden correcto en cadenas de fenómenos:
 *   GRAVITY (1) → BLACK_HOLE_GRAVITY (2) → BLACK_HOLE_HORIZON (3)
 *   VOLUMETRIC_EXPANSION (5) → THERMAL_CONDUCTION (100) → JOULE_HEATING (105)
 *
 * ── PROPIEDADES PARTICIPANTES ────────────────────────────────────────────
 * El conjunto de descriptores que el evaluador puede leer y escribir.
 * El orden de inserción se preserva — algunos evaluadores (AmbientDissipation)
 * interpretan el primero como "propiedad que se disipa" y el segundo como
 * "coeficiente de disipación".
 *
 * ── CONSTRUCCIÓN ─────────────────────────────────────────────────────────
 *
 *   PhysicalRelation THERMAL_CONDUCTION = PhysicalRelation.builder()
 *       .name("thermal_conduction")
 *       .relationType(RelationType.FOURIER)
 *       .participating(ThermalProperties.TEMPERATURE,
 *                      ThermalProperties.THERMAL_CONDUCTIVITY,
 *                      ThermalProperties.HEAT_CAPACITY)
 *       .constraint(RelationConstraint.maxDistance(32.0))
 *       .constraint(RelationConstraint.minDelta(1e-6))
 *       .priority(100)
 *       .build();
 *
 * ── INMUTABILIDAD ─────────────────────────────────────────────────────────
 * PhysicalRelation es inmutable una vez construida. Sus colecciones internas
 * son vistas inmutables.
 *
 * ── QUÉ NO CONTIENE ──────────────────────────────────────────────────────
 *   ✗ Ningún algoritmo físico.
 *   ✗ Ninguna referencia al Solver ni a evaluadores concretos.
 *   ✗ Ninguna referencia a entidades.
 *   ✗ Ningún estado mutable.
 */
public final class PhysicalRelation {

    /** Nombre legible de la relación. Solo para depuración y logging. */
    private final String                    name;

    /** Tipo del fenómeno — clave de despacho hacia el evaluador. */
    private final RelationType              relationType;

    /**
     * Propiedades que participan en este fenómeno.
     * Orden de inserción preservado (LinkedHashSet).
     * Vista inmutable expuesta por getParticipatingProperties().
     */
    private final Set<PropertyDescriptor>   participatingProperties;

    /**
     * Restricciones que condicionan la activación de esta relación.
     * Indexadas por tipo para acceso O(1) desde los evaluadores.
     */
    private final List<RelationConstraint>  constraints;

    /**
     * Prioridad de evaluación. Menor valor = evaluado antes.
     * El PhysicsSolver ordena las relaciones por prioridad ascendente.
     */
    private final int                       priority;

    // ── Constructor privado — usar Builder ────────────────────────────────

    private PhysicalRelation(Builder b) {
        this.name                   = b.name;
        this.relationType           = b.relationType;
        this.participatingProperties = Collections.unmodifiableSet(
            new LinkedHashSet<>(b.participatingProperties));
        this.constraints            = Collections.unmodifiableList(
            new ArrayList<>(b.constraints));
        this.priority               = b.priority;
    }

    // ── Factory ───────────────────────────────────────────────────────────

    /** Punto de entrada del Builder. */
    public static Builder builder() { return new Builder(); }

    // ── Accesores ─────────────────────────────────────────────────────────

    /**
     * Nombre legible de la relación.
     * Usado exclusivamente para depuración y logging.
     *
     * @return nombre. Puede ser null si no fue declarado.
     */
    public String getName() { return name; }

    /**
     * Tipo del fenómeno físico — clave de despacho hacia el evaluador.
     *
     * @return RelationType. Nunca null.
     */
    public RelationType getRelationType() { return relationType; }

    /**
     * Conjunto inmutable de propiedades participantes en este fenómeno,
     * en orden de inserción declarado en el Builder.
     *
     * Los evaluadores itegan sobre este conjunto para leer y escribir
     * los valores de las propiedades participantes en cada entidad.
     *
     * @return conjunto inmutable de PropertyDescriptor. Nunca null.
     */
    public Set<PropertyDescriptor> getParticipatingProperties() {
        return participatingProperties;
    }

    /**
     * Prioridad de evaluación. Menor valor = evaluado antes en el frame.
     *
     * @return prioridad. Valor por defecto: 100.
     */
    public int getPriority() { return priority; }

    /**
     * Retorna la restricción del tipo dado, o null si no hay ninguna.
     *
     * Los evaluadores usan este método para conocer los parámetros
     * de activación declarados en la relación:
     *   relation.getConstraint(RelationConstraint.Type.MAX_DISTANCE)
     *   relation.getConstraint(RelationConstraint.Type.THRESHOLD_ABOVE)
     *
     * Si hay múltiples restricciones del mismo tipo, retorna la primera.
     *
     * @param type tipo de restricción buscado.
     * @return la restricción, o null si no existe.
     */
    public RelationConstraint getConstraint(RelationConstraint.Type type) {
        if (type == null) return null;
        for (RelationConstraint c : constraints) {
            if (c.getType() == type) return c;
        }
        return null;
    }

    /**
     * Todas las restricciones declaradas en esta relación.
     *
     * @return lista inmutable de restricciones. Nunca null.
     */
    public List<RelationConstraint> getConstraints() { return constraints; }

    // ── Object ────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "PhysicalRelation["
            + (name != null ? name : relationType)
            + " prio=" + priority + "]";
    }

    // ═════════════════════════════════════════════════════════════════════
    // Builder
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Builder de PhysicalRelation.
     *
     * Uso:
     *   PhysicalRelation.builder()
     *       .name("thermal_conduction")
     *       .relationType(RelationType.FOURIER)
     *       .participating(ThermalProperties.TEMPERATURE, ...)
     *       .constraint(RelationConstraint.maxDistance(32.0))
     *       .priority(100)
     *       .build();
     */
    public static final class Builder {

        private String                   name                  = null;
        private RelationType             relationType          = null;
        private final Set<PropertyDescriptor> participatingProperties =
            new LinkedHashSet<>();
        private final List<RelationConstraint> constraints     = new ArrayList<>();
        private int                      priority              = 100;

        private Builder() {}

        /**
         * Nombre legible de la relación (para depuración).
         *
         * @param name nombre. Ignorado si null.
         * @return this.
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Tipo del fenómeno — clave de despacho hacia el evaluador.
         *
         * @param relationType el tipo. No puede ser null.
         * @return this.
         */
        public Builder relationType(RelationType relationType) {
            if (relationType == null)
                throw new IllegalArgumentException("relationType no puede ser null");
            this.relationType = relationType;
            return this;
        }

        /**
         * Añade propiedades participantes en el fenómeno.
         *
         * @param descriptors descriptores de las propiedades. Ignorados si null.
         * @return this.
         */
        public Builder participating(PropertyDescriptor... descriptors) {
            if (descriptors == null) return this;
            for (PropertyDescriptor d : descriptors) {
                if (d != null) participatingProperties.add(d);
            }
            return this;
        }

        /**
         * Añade una restricción que condiciona la activación de la relación.
         *
         * @param constraint la restricción. Ignorada si null.
         * @return this.
         */
        public Builder constraint(RelationConstraint constraint) {
            if (constraint != null) constraints.add(constraint);
            return this;
        }

        /**
         * Prioridad de evaluación (menor = antes). Por defecto: 100.
         *
         * @param priority prioridad.
         * @return this.
         */
        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        /**
         * Construye la PhysicalRelation.
         *
         * @throws IllegalStateException si relationType no fue declarado.
         */
        public PhysicalRelation build() {
            if (relationType == null)
                throw new IllegalStateException("relationType es obligatorio");
            return new PhysicalRelation(this);
        }
    }
}
