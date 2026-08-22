package Game.Items.Types.Bullets.Capability;

import Game.Items.Types.Bullets.Definition.ProjectileContext;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Neutral resolver that composes ProjectileContext from registered capability providers.
 * NO switch/enum - purely registry-based resolution.
 */
public class ProjectileContextResolver {
    
    private final Map<Class<?>, CapabilityProvider<?>> providers = new HashMap<>();
    
    /**
     * Register a capability provider for a specific capability type.
     */
    public <T> void registerProvider(Class<T> capabilityType, CapabilityProvider<T> provider) {
        providers.put(capabilityType, provider);
    }
    
    /**
     * Resolve a ProjectileContext that provides all required capabilities.
     * 
     * @param requiredCapabilities Set of capability types needed
     * @return ComposableProjectileContext with registered providers
     * @throws IllegalStateException if any required capability has no registered provider
     */
    public ProjectileContext resolve(Set<Class<?>> requiredCapabilities) {
        // Validate all required capabilities have providers
        for (Class<?> capabilityType : requiredCapabilities) {
            if (!providers.containsKey(capabilityType)) {
                throw new IllegalStateException(
                    "No provider registered for required capability: " + capabilityType.getName()
                );
            }
        }
        
        // Create capabilities map from providers
        Map<Class<?>, Object> capabilities = new HashMap<>();
        for (Class<?> capabilityType : requiredCapabilities) {
            CapabilityProvider<?> provider = providers.get(capabilityType);
            Object capability = provider.createCapability();
            capabilities.put(capabilityType, capability);
        }
        
        return new ComposableProjectileContext(capabilities);
    }
    
    /**
     * Check if a capability type has a registered provider.
     */
    public boolean hasProvider(Class<?> capabilityType) {
        return providers.containsKey(capabilityType);
    }
}
