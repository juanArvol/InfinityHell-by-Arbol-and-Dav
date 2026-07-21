package Game.Engine.World.Physics;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Descriptor declarativo e inmutable de una ley física.
 *
 * ── HRFC-019 — Eliminación Definitiva del Modelo Orientado a Tipos de Ley ─
 *
 * ── FILOSOFÍA ─────────────────────────────────────────────────────────────
 * PhysicsLaw es la única abstracción de ley en el Engine.
 * No hay subtipos. No hay Intra, Transfer, Constraint ni ningún equivalente.
 * No hay aridad. No hay clasificación de ningún tipo.
 *
 * Una ley física es exactamente esto:
 *
 *   Una función que recibe el WorldContext y aplica efectos sobre las
 *   entidades que considera relevantes.
 *
 * El Solver entrega el mismo WorldContext a todas las leyes sin distinción.
 * Cada ley decide internamente qué entidades consulta, cuántas necesita,
 * con qué radio opera, y qué cambios acumula.
 *
 * ── ESTRUCTURA ────────────────────────────────────────────────────────────
 *
 *   solve      → el comportamiento de la ley. Recibe el WorldContext y
 *                opera sobre las entidades que necesita. Es el único campo
 *                obligatorio.
 *
 *   inputs     → identificadores de propiedades que la ley lee.
 *                Información declarativa. El Solver no la usa para preparar
 *                contextos — la ley puede leer lo que quiera de todas formas.
 *                Útil para herramientas de análisis, depuración y editores.
 *
 *   outputs    → identificadores de propiedades que la ley puede modificar.
 *                Mismo uso declarativo.
 *
 *   priority   → orden relativo de evaluación dentro de un frame.
 *                Menor valor = se evalúa antes. Por defecto: 100.
 *
 *   iterations → número de veces que el Solver invoca solve() por frame.
 *                Útil para leyes que convergen iterativamente. Por defecto: 1.
 *
 * ── QUÉ NO CONTIENE ──────────────────────────────────────────────────────
 *   ✗ Ningún campo arity.
 *   ✗ Ningún campo contactRadius (eso lo decide la ley internamente).
 *   ✗ Ninguna jerarquía de subtipos.
 *   ✗ Ninguna clasificación por fenómeno.
 *   ✗ Ninguna referencia a CoreDomains, PhysicalDomain o equivalentes.
 *
 * ── EXTENSIBILIDAD ────────────────────────────────────────────────────────
 *
 *   Gravedad (un objeto):
 *
 *     PhysicsLaw.builder()
 *         .inputs("velocity_y")
 *         .outputs("velocity_y")
 *         .solve(ctx -> {
 *             for (EntityView e : ctx.entities())
 *                 if (e.has("velocity_y"))
 *                     e.add("velocity_y", 9.8 * ctx.deltaTime());
 *         })
 *         .build()
 *
 *   Transferencia térmica (pares):
 *
 *     PhysicsLaw.builder()
 *         .inputs("temperature")
 *         .outputs("temperature")
 *         .solve(ctx -> {
 *             List<EntityView> all = ctx.entities();
 *             for (int i = 0; i < all.size() - 1; i++)
 *                 for (int j = i + 1; j < all.size(); j++) {
 *                     if (ctx.distance(all.get(i), all.get(j)) > 32.0) continue;
 *                     // transferir
 *                 }
 *         })
 *         .build()
 *
 *   Radio de Schwarzschild (N cuerpos, campo gravitacional):
 *
 *     PhysicsLaw.builder()
 *         .inputs("mass", "velocity_x", "velocity_y")
 *         .outputs("velocity_x", "velocity_y")
 *         .solve(ctx -> {
 *             List<EntityView> all = ctx.entities();
 *             for (EntityView a : all)
 *                 for (EntityView b : all) {
 *                     if (a == b) continue;
 *                     // fuerza gravitacional masiva
 *                 }
 *         })
 *         .build()
 *
 * Ninguno de estos ejemplos modifica PhysicsSolver, LawRegistry ni ningún
 * otro componente del Core. Solo registra una nueva instancia de PhysicsLaw.
 */
public final class PhysicsLaw {

    /**
     * El comportamiento de la ley.
     * Recibe el WorldContext y opera libremente sobre las entidades.
     * Es el único campo obligatorio de una PhysicsLaw.
     */
    private final Solve solve;

    /**
     * Identificadores de propiedades que la ley lee.
     * Información declarativa para herramientas y depuración.
     * El Solver no la usa para tomar decisiones.
     */
    private final Set<String> inputs;

    /**
     * Identificadores de propiedades que la ley puede modificar.
     * Información declarativa para herramientas y depuración.
     */
    private final Set<String> outputs;

    /** Prioridad de evaluación. Menor = se evalúa antes. */
    private final int priority;

    /**
     * Número de veces que el Solver invoca solve() por frame.
     * Siempre al menos 1.
     */
    private final int iterations;

    // ── Constructor privado — usar Builder ────────────────────────────────

    private PhysicsLaw(Builder b) {
        if (b.solve == null)
            throw new IllegalArgumentException("solve no puede ser null");
        this.solve      = b.solve;
        this.inputs     = Collections.unmodifiableSet(new LinkedHashSet<>(b.inputs));
        this.outputs    = Collections.unmodifiableSet(new LinkedHashSet<>(b.outputs));
        this.priority   = b.priority;
        this.iterations = Math.max(1, b.iterations);
    }

    // ── Interfaz funcional ────────────────────────────────────────────────

    /**
     * Interfaz funcional del comportamiento de una ley.
     *
     * Una implementación recibe el WorldContext y puede:
     *   - Iterar ctx.entities() para acceder a todas las entidades.
     *   - Filtrar entidades por sus propiedades con has() y get().
     *   - Emparejar entidades usando ctx.distance().
     *   - Acumular cambios con add().
     *   - No hacer nada si las condiciones de la ley no se cumplen.
     *
     * La ley es responsable de toda la lógica de selección de participantes.
     * El Solver nunca toma esa decisión.
     */
    @FunctionalInterface
    public interface Solve {
        /**
         * Aplica la ley sobre el contexto del mundo dado.
         *
         * @param ctx el estado del mundo este frame. Nunca null.
         */
        void solve(WorldContext ctx);
    }

    // ── Resolución ────────────────────────────────────────────────────────

    /**
     * Resuelve esta ley sobre el contexto del mundo dado.
     * Llamado por PhysicsSolver una vez por frame (o iterations veces).
     *
     * @param ctx el estado del mundo este frame. Nunca null.
     */
    public void solve(WorldContext ctx) {
        solve.solve(ctx);
    }

    // ── Accesores ─────────────────────────────────────────────────────────

    /**
     * Identificadores de propiedades que la ley lee.
     * Declarativo. No afecta al comportamiento del Solver.
     *
     * @return conjunto inmutable de ids de entrada.
     */
    public Set<String> getInputs() { return inputs; }

    /**
     * Identificadores de propiedades que la ley puede modificar.
     * Declarativo. No afecta al comportamiento del Solver.
     *
     * @return conjunto inmutable de ids de salida.
     */
    public Set<String> getOutputs() { return outputs; }

    /**
     * Prioridad de evaluación. Menor = antes.
     *
     * @return prioridad.
     */
    public int getPriority() { return priority; }

    /**
     * Número de veces que el Solver invoca solve() por frame.
     *
     * @return iteraciones. Siempre >= 1.
     */
    public int getIterations() { return iterations; }

    // ── Object ────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "PhysicsLaw[priority=" + priority
            + " inputs=" + inputs
            + " outputs=" + outputs + "]";
    }

    // ── Builder ───────────────────────────────────────────────────────────

    /** Punto de entrada del Builder. */
    public static Builder builder() { return new Builder(); }

    /**
     * Builder de PhysicsLaw.
     */
    public static final class Builder {

        private Solve          solve      = null;
        private Set<String>    inputs     = new LinkedHashSet<>();
        private Set<String>    outputs    = new LinkedHashSet<>();
        private int            priority   = 100;
        private int            iterations = 1;

        private Builder() {}

        /**
         * El comportamiento de la ley. Obligatorio.
         * La implementación recibe un WorldContext y opera libremente.
         *
         * @param s la función solve. No puede ser null al construir.
         */
        public Builder solve(Solve s) {
            this.solve = s;
            return this;
        }

        /**
         * Declara los identificadores de propiedades que la ley lee.
         * No afecta al comportamiento del Solver.
         *
         * @param propertyIds ids de propiedades de entrada.
         */
        public Builder inputs(String... propertyIds) {
            if (propertyIds != null)
                for (String id : propertyIds)
                    if (id != null) inputs.add(id);
            return this;
        }

        /**
         * Declara los identificadores de propiedades que la ley puede modificar.
         * No afecta al comportamiento del Solver.
         *
         * @param propertyIds ids de propiedades de salida.
         */
        public Builder outputs(String... propertyIds) {
            if (propertyIds != null)
                for (String id : propertyIds)
                    if (id != null) outputs.add(id);
            return this;
        }

        /**
         * Prioridad de evaluación. Menor = antes.
         * Por defecto: 100.
         */
        public Builder priority(int p) {
            this.priority = p;
            return this;
        }

        /**
         * Número de veces que el Solver invoca solve() por frame.
         * Por defecto: 1.
         */
        public Builder iterations(int i) {
            this.iterations = i;
            return this;
        }

        /** Construye la PhysicsLaw. Lanza IllegalArgumentException si solve es null. */
        public PhysicsLaw build() {
            return new PhysicsLaw(this);
        }
    }
}
