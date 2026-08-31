package Game.Items.Types.Bullets.Capability;

import Game.Engine.AbstractEntity;
import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Engine.Spatial.LinearSpatialQuery;
import Game.Engine.Spatial.SpatialQuery;
import Game.World.Core.WorldManager;
import java.util.List;

/**
 * Provides spatial query capability backed by WorldManager's global dynamic registry.
 * 
 * Uses LinearSpatialQuery from the Engine for O(n) spatial search.
 */
public class WorldSpatialCapabilityProvider implements CapabilityProvider<SpatialQueryCapability> {
    
    private final WorldManager worldManager;
    private final SpatialQuery spatialQuery;
    
    public WorldSpatialCapabilityProvider(WorldManager worldManager) {
        if (worldManager == null) {
            throw new IllegalArgumentException("WorldSpatialCapabilityProvider requires non-null WorldManager");
        }
        
        this.worldManager = worldManager;
        
        // Build SpatialQuery backed by global dynamic registry
        this.spatialQuery = new LinearSpatialQuery(
            () -> worldManager.getGlobalDynamicRegistry().getAll()
        );
    }
    
    @Override
    public Class<SpatialQueryCapability> getCapabilityType() {
        return SpatialQueryCapability.class;
    }
    
    @Override
    public SpatialQueryCapability createCapability() {
        return new SpatialQueryCapability() {
            @Override
            public List<? extends AbstractEntity> findEntitiesInRadius(Vector2D center, double radius) {
                // Delegate to spatial query, then filter alive entities
                return spatialQuery.findInRadius(center, radius, AbstractEntity.class)
                    .stream()
                    .filter(entity -> !entity.isDead())
                    .toList();
            }
        };
    }
}
