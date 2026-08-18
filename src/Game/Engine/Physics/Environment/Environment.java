package Game.Engine.Physics.Environment;

/**
 * Abstracción de un entorno que produce y posee sus condiciones ambientales.
 *
 * ── HRFC-FASE2 — Declarative Environment Ownership ───────────────────────
 *
 * ── RESPONSABILIDAD ──────────────────────────────────────────────────────
 * Environment es el propietario de las condiciones ambientales donde existe
 * una entidad simulada. Produce el EnvironmentState que describe esas
 * condiciones en un momento dado.
 *
 * ── PRINCIPIO FUNDAMENTAL ─────────────────────────────────────────────────
 * La infraestructura NO decide cómo es un ambiente.
 * Cada Environment declara sus propias condiciones.
 * 
 *   ANTES (infraestructura impone):
 *     EnvironmentState.STANDARD → temperatura=0, gravedad=9.8 (hardcoded)
 *     Builder defaults → presión=1.0, densidad=1.2 (universales)
 *
 *   AHORA (ambiente declara):
 *     StandardAtmosphere → produce su temperatura, gravedad, presión
 *     VacuumEnvironment  → produce sus condiciones
 *     CustomEnvironment  → produce las que necesite
 *
 * ── SEPARACIÓN DE CONCEPTOS ──────────────────────────────────────────────
 *   Environment       → posee y produce las condiciones (este archivo)
 *   EnvironmentState  → describe el estado actual de esas condiciones
 *   Relations         → interpretan el estado y producen efectos
 *
 * ── MUTABILIDAD Y CICLO DE VIDA ───────────────────────────────────────────
 * Environment puede ser:
 *   - Estático: produce siempre el mismo EnvironmentState (atmósfera estable)
 *   - Dinámico: produce diferentes estados según el tiempo, posición, eventos
 *   - Compuesto: combina múltiples contribuyentes ambientales
 *
 * El método current() retorna el EnvironmentState actual del ambiente.
 * La implementación decide si crea un nuevo estado cada vez o cachea uno inmutable.
 *
 * ── USO TÍPICO ────────────────────────────────────────────────────────────
 *
 *   // Crear un ambiente concreto
 *   Environment env = StandardAtmosphere.create();
 *
 *   // En SimulationContext.Builder
 *   SimulationContext ctx = SimulationContext.builder(physical)
 *       .material(material)
 *       .environment(env)  // NO .environment(EnvironmentState.STANDARD)
 *       .build();
 *
 *   // El contexto consulta el estado actual del ambiente
 *   EnvironmentState state = ctx.environment();  // → env.current()
 *
 *   // Las relaciones leen del estado
 *   double gravityInfluence = state.getGravityInfluenceY();
 *   double temp = state.getAmbientTemperature();
 *
 * ── EXTENSIBILIDAD ────────────────────────────────────────────────────────
 * Para crear un nuevo tipo de ambiente:
 *   1. Implementar Environment
 *   2. Declarar las condiciones de referencia (campos, constantes)
 *   3. Producir el EnvironmentState en current()
 *   4. Opcionalmente: modificar el estado dinámico según interacciones
 *
 * NO es necesario modificar:
 *   - EnvironmentState (sigue siendo descriptivo e inmutable)
 *   - SimulationContext (acepta cualquier Environment)
 *   - Evaluadores físicos (leen del EnvironmentState sin conocer el tipo)
 *
 * ── DIFERENCIA CON WorldField ─────────────────────────────────────────────
 * WorldField aplica efectos localizados espacialmente (campo térmico, gravedad).
 * Environment representa las condiciones globales/base donde ocurre la simulación.
 *
 * Conceptualmente:
 *   Environment base + WorldFields activos → condiciones efectivas
 *
 * La composición de ambos puede implementarse cuando sea necesaria.
 * FASE 2 establece únicamente el concepto de Environment como propietario.
 *
 * ── THREAD SAFETY ─────────────────────────────────────────────────────────
 * Las implementaciones deben ser thread-safe si current() puede ser llamado
 * desde múltiples threads. Para implementaciones inmutables esto es trivial.
 */
public interface Environment {

    /**
     * Retorna el EnvironmentState actual de este ambiente.
     * 
     * Este método es consultado por SimulationContext para proporcionar
     * las condiciones ambientales a los evaluadores físicos.
     *
     * Implementaciones típicas:
     *   - Ambientes estáticos: retornan siempre la misma instancia cacheada
     *   - Ambientes dinámicos: construyen un nuevo estado según el tiempo/posición
     *   - Ambientes compuestos: combinan estados de múltiples contribuyentes
     *
     * @return el estado ambiental actual. No debe ser null.
     */
    EnvironmentState current();

    /**
     * Nombre legible del ambiente para depuración y logging.
     * 
     * @return nombre descriptivo. Nunca null.
     */
    default String getName() {
        return getClass().getSimpleName();
    }
}
