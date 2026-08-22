package Game.Items.Types.Bullets.Definition;

import Game.Engine.AbstractEntity;
import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Items.Types.Bullets.ProjectileBlueprint;
import java.util.List;

/**
 * Contexto de interacción abstracto para proyectiles.
 * 
 * ── ARQUITECTURA COMPOSABLE ───────────────────────────────────────────────
 * 
 * ProjectileContext representa el conjunto de capacidades contextuales
 * disponibles para un proyectil. NO es una interfaz monolítica que acumula
 * métodos obligatorios — es un contenedor abstracto de capacidades componibles.
 * 
 * Principios arquitectónicos:
 *   1. Cada proyectil recibe UN ProjectileContext
 *   2. El contenido del contexto depende de las necesidades del BulletBehavior
 *   3. Las capacidades se acceden via getCapability(Class<T>)
 *   4. Agregar capacidades NO requiere modificar esta interfaz
 * 
 * ── SEPARACIÓN METADATA VS SERVICIOS ──────────────────────────────────────
 * 
 * ProjectileContext = servicios externos que el proyectil CONSUME
 * Owner/origin = metadata que el proyectil POSEE
 * 
 * Por lo tanto:
 *   bullet.getOwner()           ✓ metadata propia
 *   bullet.getSpawnOrigin()     ✓ metadata propia
 *   bullet.getProjectileContext() ✓ servicios externos
 * 
 * ── CAPACIDADES DISPONIBLES ───────────────────────────────────────────────
 * 
 * Capacidades actuales:
 *   - ProjectileSpawningCapability: spawn de proyectiles secundarios
 *   - SpatialQueryCapability: búsqueda espacial de entidades
 * 
 * Capacidades futuras (ejemplos):
 *   - FactionQueryCapability: consultas de alianza/enemigo
 *   - EnvironmentQueryCapability: gravedad, agua, etc.
 *   - TargetingCapability: buscar objetivos
 * 
 * ── EJEMPLO DE USO ────────────────────────────────────────────────────────
 * 
 * En MetheorBullet:
 * 
 *   @Override
 *   public Set<Class<?>> getRequiredCapabilities() {
 *       return Set.of(SpatialQueryCapability.class);
 *   }
 *   
 *   private void explode(Bullet bullet) {
 *       SpatialQueryCapability spatial = 
 *           bullet.getProjectileContext()
 *                 .getCapability(SpatialQueryCapability.class);
 *       
 *       if (spatial == null) {
 *           throw new IllegalStateException("MetheorBullet requires SpatialQuery");
 *       }
 *       
 *       List<? extends AbstractEntity> entities = 
 *           spatial.findEntitiesInRadius(center, radius);
 *       // ... aplicar daño ...
 *   }
 * 
 * ── NULL OBJECT ───────────────────────────────────────────────────────────
 * 
 * ProjectileContext.NULL existe para tests, construcción temporal o ausencia
 * explícita de infraestructura. NO debe usarse silenciosamente durante gameplay.
 * 
 * Si un behavior requiere una capacidad y no está disponible, eso es un error
 * de configuración que debe detectarse explícitamente.
 */
public interface ProjectileContext {
    
    // ── Core capability API ───────────────────────────────────────────────
    
    /**
     * Obtiene una capacidad específica si está disponible.
     * 
     * @param capabilityType tipo de la capacidad (interfaz)
     * @param <T> tipo de retorno (inferido del parámetro)
     * @return instancia de la capacidad, o null si no está disponible
     */
    <T> T getCapability(Class<T> capabilityType);
    
    /**
     * Verifica si una capacidad está disponible sin obtenerla.
     * 
     * @param capabilityType tipo de la capacidad
     * @return true si la capacidad está disponible
     */
    default boolean hasCapability(Class<?> capabilityType) {
        return getCapability(capabilityType) != null;
    }
    
    // ── Convenience methods ───────────────────────────────────────────────
    // Delegan a getCapability() internamente. Proporcionan ergonomía sin
    // sacrificar la arquitectura composable.
    
    /**
     * Spawnea un proyectil secundario.
     * Método de conveniencia que delega a ProjectileSpawningCapability.
     * 
     * No-op seguro si la capacidad no está disponible.
     * 
     * @param blueprint definición del proyectil
     * @param position posición de spawn
     * @param direction dirección normalizada
     */
    default void spawnProjectile(ProjectileBlueprint blueprint,
                                 Vector2D position,
                                 Vector2D direction) {
        // Default no-op — las implementaciones reales delegan a la capacidad
    }
    
    /**
     * Spawnea un proyectil apuntando hacia un objetivo.
     * Método de conveniencia que delega a ProjectileSpawningCapability.
     * 
     * @param blueprint definición del proyectil
     * @param origin posición de spawn
     * @param target posición objetivo (null = dirección default)
     */
    default void spawnProjectileToward(ProjectileBlueprint blueprint,
                                       Vector2D origin,
                                       Vector2D target) {
        // Default no-op — las implementaciones reales delegan a la capacidad
    }
    
    /**
     * Busca entidades en un radio.
     * Método de conveniencia que delega a SpatialQueryCapability.
     * 
     * Retorna lista vacía si la capacidad no está disponible.
     * 
     * @param center posición central
     * @param radius radio de búsqueda
     * @return lista inmutable de entidades (puede estar vacía, nunca null)
     */
    default List<? extends AbstractEntity> findEntitiesInRadius(
            Vector2D center, double radius) {
        return List.of(); // Default: lista vacía
    }
    
    // ── Null Object ───────────────────────────────────────────────────────
    
    /**
     * Null Object para ausencia de contexto.
     * 
     * Uso apropiado:
     *   - Tests unitarios sin WorldManager
     *   - Construcción temporal durante bootstrap
     *   - Escenarios explícitos sin infraestructura
     * 
     * Uso INAPROPIADO:
     *   - Gameplay normal donde existe WorldManager
     *   - Ocultar errores de wiring/configuración
     */
    ProjectileContext NULL = new ProjectileContext() {
        @Override
        public <T> T getCapability(Class<T> capabilityType) {
            return null; // No capabilities available
        }
        
        @Override
        public String toString() {
            return "ProjectileContext.NULL";
        }
    };
}
