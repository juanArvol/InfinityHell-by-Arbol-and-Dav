package Game.Engine.Physics.Environment;

/**
 * Representa un fenómeno o fuente que contribuye a las condiciones ambientales.
 *
 * ── HRFC-FASE2.5 — Composed Environmental Influence ──────────────────────
 *
 * ── RESPONSABILIDAD ──────────────────────────────────────────────────────
 * Un EnvironmentalContributor representa cualquier fenómeno, fuente o 
 * influencia que produce o modifica las condiciones del entorno.
 *
 * Ejemplos conceptuales:
 *   - LavaSource       → contribuye temperatura
 *   - WindGenerator    → contribuye windX/windY
 *   - RadiationSource  → contribuye ambientRadiation
 *   - GravityWell      → contribuye gravityX/gravityY (influencia externa)
 *   - ElectricField    → contribuye electricFieldX/electricFieldY
 *
 * ── SEPARACIÓN DE CONCEPTOS ──────────────────────────────────────────────
 *
 *   CONTRIBUTORS (este archivo)
 *     → fenómenos que producen condiciones
 *
 *   CONDITIONS (EnvironmentState)
 *     → estado resultante de las contribuciones
 *
 *   RELATIONS
 *     → interpretan las condiciones y producen efectos
 *
 * ── INMUTABILIDAD ─────────────────────────────────────────────────────────
 * Los contributors pueden ser mutables (estado dinámico) o inmutables
 * (contribución constante). El método contribute() es consultado cada vez
 * que se necesita recalcular las condiciones ambientales.
 *
 * ── USO TÍPICO ────────────────────────────────────────────────────────────
 *
 *   // Crear un ambiente compuesto
 *   Environment env = ComposedEnvironment.builder()
 *       .base(baseConditions)  // condiciones base (atmosfera, etc)
 *       .add(new ThermalSource(500.0))
 *       .add(new WindSource(10.0, 0.0))
 *       .build();
 *
 *   // El ambiente combina las contribuciones
 *   EnvironmentState state = env.current();
 *   // state.getAmbientTemperature() = base + thermal contribution
 *   // state.getWindX() = base + wind contribution
 *
 * ── NO DUPLICAR ABSTRACCIONES ────────────────────────────────────────────
 * Esta es la abstracción MÍNIMA necesaria para expresar composición.
 * No crear jerarquías innecesarias de ThermalContributor, FluidContributor,
 * etc. a menos que el dominio lo requiera explícitamente.
 *
 * La mayoría de los contributors serán implementaciones directas de esta
 * interfaz, no subclases especializadas.
 */
@FunctionalInterface
public interface EnvironmentalContributor {

    /**
     * Contribuye a la construcción del estado ambiental.
     *
     * Este método es invocado por ComposedEnvironment durante la
     * construcción del EnvironmentState actual. El contributor modifica
     * el builder proporcionado añadiendo o modificando propiedades.
     *
     * Ejemplo de implementación:
     *
     *   public void contribute(EnvironmentState.Builder builder) {
     *       // Añadir temperatura térmica
     *       double current = builder.getAmbientTemperature();
     *       builder.ambientTemperature(current + thermalContribution);
     *   }
     *
     * IMPORTANTE:
     *   - El builder ya contiene las condiciones base y contribuciones previas.
     *   - El contributor debe AÑADIR su efecto, no reemplazar el valor completo.
     *   - Para valores escalares (temp, pressure): sumar o combinar.
     *   - Para vectores (wind, gravity): componer vectorialmente.
     *
     * @param builder el builder de EnvironmentState en construcción.
     *                Nunca es null.
     */
    void contribute(EnvironmentState.Builder builder);
}
