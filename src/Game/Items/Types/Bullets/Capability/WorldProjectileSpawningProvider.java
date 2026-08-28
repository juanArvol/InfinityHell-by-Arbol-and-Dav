package Game.Items.Types.Bullets.Capability;

import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Items.Types.Bullets.Definition.Bullet;
import Game.Items.Types.Bullets.Definition.ProjectilePool;
import Game.Items.Types.Bullets.ProjectileBlueprint;
import Game.World.Core.WorldManager;

/**
 * Provides projectile spawning capability backed by WorldManager and ProjectilePool.
 * 
 * Ensures secondary projectiles:
 * - Go through the same pool as primary projectiles
 * - Preserve owner from parent bullet when spawned
 * - Get added to WorldManager's dynamic registry
 */
public class WorldProjectileSpawningProvider implements CapabilityProvider<ProjectileSpawningCapability> {
    
    private final WorldManager worldManager;
    private final ProjectilePool projectilePool;
    
    public WorldProjectileSpawningProvider(WorldManager worldManager, ProjectilePool projectilePool) {
        if (worldManager == null) {
            throw new IllegalArgumentException("WorldProjectileSpawningProvider requires non-null WorldManager");
        }
        if (projectilePool == null) {
            throw new IllegalArgumentException("WorldProjectileSpawningProvider requires non-null ProjectilePool");
        }
        
        this.worldManager = worldManager;
        this.projectilePool = projectilePool;
    }
    
    @Override
    public Class<ProjectileSpawningCapability> getCapabilityType() {
        return ProjectileSpawningCapability.class;
    }
    
    @Override
    public ProjectileSpawningCapability createCapability() {
        return new ProjectileSpawningCapability() {
            @Override
            public void spawnProjectile(ProjectileBlueprint blueprint, Vector2D position, Vector2D direction) {
                // Acquire bullet from pool (will resolve context and inject owner/spawnOrigin from blueprint)
                // Owner MUST be set in blueprint by the behavior before calling this
                Bullet bullet = projectilePool.acquire(blueprint, position.getX(), position.getY(), direction.getX(), direction.getY());
                
                // Add to world
                worldManager.addDynamic(bullet);
            }
        };
    }
}
