package Game.Engine.Physics.Environment;

import java.util.ArrayList;
import java.util.List;

/**
 * Ambiente que combina condiciones base con múltiples contributors.
 *
 * ── HRFC-FASE2.5 — Composed Environmental Influence ──────────────────────
 *
 * ── ARQUITECTURA ──────────────────────────────────────────────────────────
 *
 *                    COMPOSED ENVIRONMENT
 *                           │
 *              ┌────────────┴────────────┐
 *              │                         │
 *        BASE CONDITIONS           CONTRIBUTORS
 *              │                         │
 *              │               ┌─────────┼─────────┐
 *              │               │         │         │
 *              │           Thermal    Wind    Radiation
 *              │               │         │         │
 *              └───────────────┴─────────┴─────────┘
 *                              │
 *                              ▼
 *                    ENVIRONMENT STATE
 *                       (effective conditions)
 *
 * ── RESPONSABILIDAD ──────────────────────────────────────────────────────
 * ComposedEnvironment representa un ambiente cuyas condiciones resultan
 * de la composición de:
 *
 *   1. Condiciones base (EnvironmentState base)
 *   2. Contribuciones de fenómenos (List<EnvironmentalContributor>)
 *
 * Cada vez que se invoca current(), el ambiente:
 *   1. Copia las condiciones base a un Builder
 *   2. Aplica cada contributor en orden
 *   3. Construye el EnvironmentState resultante
 *
 * ── INMUTABILIDAD VS DINAMISMO ───────────────────────────────────────────
 * ComposedEnvironment es inmutable en su estructura (base + contributors fijos).
 * Sin embargo, los contributors individuales pueden ser mutables y producir
 * diferentes contribuciones en cada invocación de current().
 *
 * Ejemplos:
 *   - Contributor estático: ThermalSource(500) → siempre +500 temperatura
 *   - Contributor dinámico: TimeBasedWind → varía según tiempo transcurrido
 *   - Contributor posicional: LocalGravityWell → varía según posición (futuro)
 *
 * ── SEMÁNTICA DE COMPOSICIÓN ─────────────────────────────────────────────
 * Los contributors se aplican secuencialmente. Cada contributor ve el
 * resultado de los anteriores y puede:
 *
 *   - SUMAR: temperature += contribution
 *   - MULTIPLICAR: pressure *= factor
 *   - COMPONER VECTORIALMENTE: windX += contribution
 *   - REEMPLAZAR (raramente): temperature = newValue
 *
 * El orden de los contributors puede ser relevante para efectos multiplicativos.
 *
 * ── EJEMPLO DE USO ────────────────────────────────────────────────────────
 *
 *   // Crear condiciones base (atmósfera terrestre)
 *   EnvironmentState base = EnvironmentState.builder()
 *       .ambientTemperature(20.0)
 *       .atmosphericPressure(1.0)
 *       .gravityY(9.8)
 *       .build();
 *
 *   // Añadir fenómenos
 *   Environment hellEnv = ComposedEnvironment.builder()
 *       .base(base)
 *       .add(new ThermalSource(500.0))      // lava → +500°C
 *       .add(new WindSource(15.0, 0.0))     // viento horizontal
 *       .add(new RadiationSource(0.8))      // radiación térmica
 *       .build();
 *
 *   // Resultado:
 *   EnvironmentState state = hellEnv.current();
 *   // temperature = 20 + 500 = 520
 *   // windX = 0 + 15 = 15
 *   // radiation = 0 + 0.8 = 0.8
 *
 * ── DIFERENCIA CON StandardAtmosphere ────────────────────────────────────
 * StandardAtmosphere: ambiente estático simple, una colección de constantes.
 * ComposedEnvironment: ambiente composicional, combina base + fenómenos.
 *
 * StandardAtmosphere es apropiado cuando las condiciones son realmente estáticas.
 * ComposedEnvironment es apropiado cuando las condiciones resultan de fenómenos.
 *
 * ── THREAD SAFETY ─────────────────────────────────────────────────────────
 * ComposedEnvironment es thread-safe si:
 *   1. El base EnvironmentState es inmutable (siempre lo es)
 *   2. Los contributors son thread-safe
 *
 * Para contributors mutables, sincronizar su estado interno si se accede
 * desde múltiples threads.
 */
public final class ComposedEnvironment implements Environment {

    private final String name;
    private final EnvironmentState base;
    private final List<EnvironmentalContributor> contributors;

    // ── Constructor privado — usar Builder ────────────────────────────────

    private ComposedEnvironment(String name,
                                EnvironmentState base,
                                List<EnvironmentalContributor> contributors) {
        this.name         = name;
        this.base         = base;
        this.contributors = new ArrayList<>(contributors); // copia defensiva
    }

    // ── Factories ─────────────────────────────────────────────────────────

    /** Punto de entrada del Builder. */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Factory para ambiente compuesto con nombre específico.
     *
     * @param name nombre del ambiente.
     * @return builder configurado con el nombre.
     */
    public static Builder builder(String name) {
        return new Builder().name(name);
    }

    // ── Environment interface ─────────────────────────────────────────────

    @Override
    public EnvironmentState current() {
        // 1. Crear builder con condiciones base
        EnvironmentState.Builder builder = copyToBuilder(base);

        // 2. Aplicar cada contributor en orden
        for (EnvironmentalContributor contributor : contributors) {
            contributor.contribute(builder);
        }

        // 3. Construir estado resultante
        return builder.build();
    }

    @Override
    public String getName() {
        return name;
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    /**
     * Copia un EnvironmentState existente a un nuevo Builder.
     * Necesario porque EnvironmentState es inmutable.
     */
    private static EnvironmentState.Builder copyToBuilder(EnvironmentState state) {
        return EnvironmentState.builder()
            .ambientTemperature(state.getAmbientTemperature())
            .atmosphericPressure(state.getAtmosphericPressure())
            .ambientHumidity(state.getAmbientHumidity())
            .windX(state.getWindX())
            .windY(state.getWindY())
            .fluidDensity(state.getFluidDensity())
            .fluidViscosity(state.getFluidViscosity())
            .gravityX(state.getGravityX())
            .gravityY(state.getGravityY())
            .electricFieldX(state.getElectricFieldX())
            .electricFieldY(state.getElectricFieldY())
            .magneticFieldZ(state.getMagneticFieldZ())
            .ambientRadiation(state.getAmbientRadiation())
            .illuminance(state.getIlluminance());
    }

    @Override
    public String toString() {
        return String.format("ComposedEnvironment[%s, contributors=%d]",
                             name, contributors.size());
    }

    // ═════════════════════════════════════════════════════════════════════
    // Builder
    // ═════════════════════════════════════════════════════════════════════

    public static final class Builder {

        private String name = "ComposedEnvironment";
        private EnvironmentState base = null;
        private final List<EnvironmentalContributor> contributors = new ArrayList<>();

        private Builder() {}

        /**
         * Establece el nombre del ambiente.
         *
         * @param name nombre descriptivo. Si es null, usa "ComposedEnvironment".
         * @return this.
         */
        public Builder name(String name) {
            this.name = (name != null && !name.isEmpty())
                ? name
                : "ComposedEnvironment";
            return this;
        }

        /**
         * Establece las condiciones base del ambiente.
         *
         * Las condiciones base representan el estado ambiental antes de
         * aplicar cualquier contributor. Típicamente:
         *   - Atmósfera base (StandardAtmosphere.INSTANCE.current())
         *   - Vacío base (VacuumEnvironment.INSTANCE.current())
         *   - Condiciones personalizadas
         *
         * Si no se establece, se usa un estado neutral (todos los valores a 0).
         *
         * @param base condiciones base. Si es null, se ignora.
         * @return this.
         */
        public Builder base(EnvironmentState base) {
            this.base = base;
            return this;
        }

        /**
         * Establece las condiciones base desde otro Environment.
         *
         * Equivalente a: base(environment.current())
         *
         * @param environment ambiente fuente. Si es null, se ignora.
         * @return this.
         */
        public Builder base(Environment environment) {
            if (environment != null) {
                this.base = environment.current();
            }
            return this;
        }

        /**
         * Añade un contributor al ambiente.
         *
         * Los contributors se aplican en el orden en que se añaden.
         *
         * @param contributor fenómeno que contribuye. Si es null, se ignora.
         * @return this.
         */
        public Builder add(EnvironmentalContributor contributor) {
            if (contributor != null) {
                this.contributors.add(contributor);
            }
            return this;
        }

        /**
         * Añade múltiples contributors al ambiente.
         *
         * @param contributors fenómenos que contribuyen. Elementos null se ignoran.
         * @return this.
         */
        public Builder addAll(Iterable<EnvironmentalContributor> contributors) {
            if (contributors != null) {
                for (EnvironmentalContributor c : contributors) {
                    add(c);
                }
            }
            return this;
        }

        /**
         * Construye el ComposedEnvironment.
         *
         * Si no se estableció base, se usa un estado neutral (todos valores a 0).
         *
         * @return el ambiente compuesto.
         */
        public ComposedEnvironment build() {
            EnvironmentState effectiveBase = (base != null)
                ? base
                : EnvironmentState.builder().build(); // estado neutral

            return new ComposedEnvironment(name, effectiveBase, contributors);
        }
    }
}
