package Game.Items.Types.Bullets.Capability;

import Game.Items.Types.Bullets.Definition.ProjectileContext;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Neutral resolver that composes ProjectileContext from registered capability providers.
 * NO switch/enum - purely registry-based resolution.
 * 
 * FASE 5 — Optimization: context caching para reducir allocations.
 * Los ProjectileContext son inmutables y compartibles — se cachean por
 * conjunto de requiredCapabilities para reutilización.
 */
public class ProjectileContextResolver {
    
    private final Map<Class<?>, CapabilityProvider<?>> providers = new HashMap<>();
    
    /**
     * FASE 5 — Cache de contextos resueltos por signature.
     * Key: Set inmutable de capabilities requeridas
     * Value: ComposableProjectileContext correspondiente (inmutable y compartible)
     */
    private final Map<Set<Class<?>>, ProjectileContext> contextCache = new HashMap<>();
    
    /**
     * Register a capability provider for a specific capability type.
     */
    public <T> void registerProvider(Class<T> capabilityType, CapabilityProvider<T> provider) {
        providers.put(capabilityType, provider);
        // FASE 5 — Invalidar cache cuando cambian los providers
        contextCache.clear();
    }
    
    /**
     * Resolve a ProjectileContext that provides all required capabilities.
     * 
     * FASE 5 — Optimization: los contextos son cacheados por signature.
     * Si el mismo conjunto de capabilities fue resuelto previamente, se
     * reutiliza el contexto existente (inmutable y thread-safe).
     * 
     * @param requiredCapabilities Set de capability types requeridos
     * @return ComposableProjectileContext con los providers registrados
     * @throws IllegalStateException si alguna capability requerida no tiene provider
     */
    public ProjectileContext resolve(Set<Class<?>> requiredCapabilities) {
        // FASE 5 — Cache lookup: mismo set → mismo contexto (immutable)
        ProjectileContext cached = contextCache.get(requiredCapabilities);
        if (cached != null) {
            return cached;
        }
        
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
        
        // Create context and cache it
        ProjectileContext context = new ComposableProjectileContext(capabilities);
        contextCache.put(requiredCapabilities, context);
        
        return context;
    }
    
    /**
     * Check if a capability type has a registered provider.
     */
    public boolean hasProvider(Class<?> capabilityType) {
        return providers.containsKey(capabilityType);
    }
}
