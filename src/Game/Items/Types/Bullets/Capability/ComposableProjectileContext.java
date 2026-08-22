package Game.Items.Types.Bullets.Capability;

import Game.Engine.AbstractEntity;
import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Items.Types.Bullets.Definition.ProjectileContext;
import Game.Items.Types.Bullets.ProjectileBlueprint;
import java.util.List;
import java.util.Map;

/**
 * Implementación composable de ProjectileContext.
 * 
 * ── ARQUITECTURA ──────────────────────────────────────────────────────────
 * 
 * ComposableProjectileContext almacena un Map de capacidades por tipo.
 * No conoce capacidades específicas — simplemente hace lookup type-safe.
 * 
 * Inmutable una vez construido — thread-safe y cacheable si es necesario.
 * 
 * ── CONSTRUCCIÓN ──────────────────────────────────────────────────────────
 * 
 * No se construye directamente. Se crea via ProjectileContextResolver:
 * 
 *   ProjectileContextResolver resolver = ...;
 *   Set<Class<?>> requirements = behavior.getRequiredCapabilities();
 *   ProjectileContext context = resolver.resolve(requirements);
 * 
 * El resolver consulta los providers registrados y compone el Map de
 * capacidades según los requirements.
 * 
 * ── EXTENSIBILIDAD ────────────────────────────────────────────────────────
 * 
 * Agregar nuevas capacidades no requiere modificar esta clase.
 * El Map es genérico — acepta cualquier par (Class<?>, Object).
 * 
 * ── MÉTODOS DE CONVENIENCIA ───────────────────────────────────────────────
 * 
 * Los métodos spawnProjectile() y findEntitiesInRadius() son shortcuts
 * que delegan a getCapability() internamente. Proporcionan ergonomía sin
 * sacrificar la arquitectura composable.
 * 
 * Si una capacidad no está disponible, los métodos de conveniencia son no-ops
 * seguros (no lanzan excepciones).
 */
public final class ComposableProjectileContext implements ProjectileContext {
    
    private final Map<Class<?>, Object> capabilities;
    
    /**
     * Constructor package-private — solo accesible desde ProjectileContextResolver.
     * 
     * @param capabilities mapa inmutable de capacidades por tipo
     */
    ComposableProjectileContext(Map<Class<?>, Object> capabilities) {
        this.capabilities = Map.copyOf(capabilities); // Defensive copy
    }
    
    // ── ProjectileContext implementation ──────────────────────────────────
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getCapability(Class<T> capabilityType) {
        return (T) capabilities.get(capabilityType);
    }
    
    @Override
    public boolean hasCapability(Class<?> capabilityType) {
        return capabilities.containsKey(capabilityType);
    }
    
    // ── Convenience methods ───────────────────────────────────────────────
    
    @Override
    public void spawnProjectile(ProjectileBlueprint blueprint,
                                Vector2D position,
                                Vector2D direction) {
        ProjectileSpawningCapability spawning = 
            getCapability(ProjectileSpawningCapability.class);
        if (spawning != null) {
            spawning.spawnProjectile(blueprint, position, direction);
        }
    }
    
    @Override
    public void spawnProjectileToward(ProjectileBlueprint blueprint,
                                      Vector2D origin,
                                      Vector2D target) {
        ProjectileSpawningCapability spawning = 
            getCapability(ProjectileSpawningCapability.class);
        if (spawning != null) {
            spawning.spawnProjectileToward(blueprint, origin, target);
        }
    }
    
    @Override
    public List<? extends AbstractEntity> findEntitiesInRadius(Vector2D center, double radius) {
        SpatialQueryCapability spatial = 
            getCapability(SpatialQueryCapability.class);
        return (spatial != null) 
            ? spatial.findEntitiesInRadius(center, radius)
            : List.of();
    }
    
    @Override
    public String toString() {
        return "ComposableProjectileContext[capabilities=" + capabilities.keySet() + "]";
    }
}
