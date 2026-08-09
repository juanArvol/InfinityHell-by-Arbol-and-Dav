package Game.Engine.Lifecycle;

/**
 * Conecta el EntityContext con la decisión de simular o destruir una entidad.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 *
 * SimulationLifecycle es adoptable por cualquier entidad que quiera delegar
 * su decisión de "¿debo seguir existiendo?" a uno o más EntityContext.
 *
 * La entidad implementa esta interfaz y retorna su contexto primario.
 * El LifecycleSystem (o el sistema que gestione el ciclo de vida) consulta
 * shouldSimulate() para decidir si continuar o destruir la entidad.
 *
 * ── SEPARACIÓN CON Destroyable ────────────────────────────────────────────
 *
 *   Destroyable              → "¿debo ser eliminado del mundo?"
 *                              Responde inmediatamente, sin contexto.
 *
 *   SimulationLifecycle      → "¿tengo un contexto que me mantenga relevante?"
 *                              Responde según la lógica semántica del contexto.
 *
 * Una entidad puede implementar ambas:
 *   - SimulationLifecycle determina si sigue relevante.
 *   - Destroyable es el mecanismo de eliminación del contenedor.
 *
 * ── EJEMPLO CON Bullet ────────────────────────────────────────────────────
 *
 *   Bullet implementa SimulationLifecycle con un EntityContext que envuelve
 *   BulletLife.isAlive(). El contexto de simulación de un proyectil ES su
 *   tiempo de vida: existe porque aún no ha expirado ni colisionado.
 *
 *   Nota: Bullet usa una lambda cacheada (no LifetimeContext) porque BulletLife
 *   tiene semántica específica de proyectiles (kill/revive/extend) que no
 *   debe duplicarse en un contexto paralelo.
 *
 * ── EJEMPLO CON Boss ──────────────────────────────────────────────────────
 *
 *   public class SansBoss extends AbstractEntity implements SimulationLifecycle {
 *
 *       private final EntityContext context = CompositeEntityContext.any(
 *           combatContext,
 *           regionContext
 *       );
 *
 *       {@literal @}Override
 *       public EntityContext getSimulationContext() { return context; }
 *
 *       {@literal @}Override
 *       public boolean shouldSimulate() { return context.isActive(); }
 *   }
 */
public interface SimulationLifecycle {

    /**
     * Retorna el contexto que determina si esta entidad debe seguir simulándose.
     *
     * @return el EntityContext primario de esta entidad
     */
    EntityContext getSimulationContext();

    /**
     * Retorna true si esta entidad debe seguir siendo simulada este frame.
     *
     * La implementación por defecto delega directamente al contexto.
     * Las entidades pueden sobreescribir para añadir condiciones adicionales.
     *
     * @return true si hay un contexto activo que justifica la simulación
     */
    default boolean shouldSimulate() {
        return getSimulationContext().isActive();
    }
}
