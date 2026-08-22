package Game.Items.Types.Bullets.Capability;

/**
 * Contrato de un provider de capacidad.
 * 
 * ── ARQUITECTURA COMPOSABLE ───────────────────────────────────────────────
 * 
 * Un CapabilityProvider es responsable de proporcionar UNA capacidad específica.
 * 
 * Responsabilidades:
 *   - Declarar qué tipo de capacidad proporciona (getCapabilityType)
 *   - Crear instancias de esa capacidad (createCapability)
 * 
 * NO debe:
 *   - Conocer otras capacidades
 *   - Conocer behaviors específicos
 *   - Implementar lógica de resolución
 * 
 * ── EXTENSIBILIDAD ────────────────────────────────────────────────────────
 * 
 * Agregar una nueva capacidad al sistema requiere:
 *   1. Crear la interfaz de capacidad (ej: FactionQueryCapability)
 *   2. Crear el provider (ej: WorldFactionProvider implements CapabilityProvider)
 *   3. Registrar el provider durante bootstrap
 * 
 * NO requiere:
 *   - Modificar ProjectileContext
 *   - Modificar ProjectileContextResolver
 *   - Modificar enums centrales
 *   - Agregar switch/case
 * 
 * ── EJEMPLO ───────────────────────────────────────────────────────────────
 * 
 * Provider de búsqueda espacial:
 * 
 *   public class WorldSpatialProvider 
 *           implements CapabilityProvider<SpatialQueryCapability> {
 *       
 *       private final SpatialQuery spatialQuery;
 *       
 *       public WorldSpatialProvider(WorldManager worldManager) {
 *           this.spatialQuery = new LinearSpatialQuery(
 *               () -> worldManager.getGlobalDynamicRegistry().getAll()
 *           );
 *       }
 *       
 *       @Override
 *       public Class<SpatialQueryCapability> getCapabilityType() {
 *           return SpatialQueryCapability.class;
 *       }
 *       
 *       @Override
 *       public SpatialQueryCapability createCapability() {
 *           return center -> spatialQuery.findInRadius(...);
 *       }
 *   }
 * 
 * @param <T> tipo de capacidad que este provider proporciona
 */
public interface CapabilityProvider<T> {
    
    /**
     * Retorna el tipo de capacidad que este provider proporciona.
     * Usado por el resolver para registrar y lookup providers.
     * 
     * @return Class de la interfaz de capacidad
     */
    Class<T> getCapabilityType();
    
    /**
     * Crea una instancia de la capacidad.
     * 
     * Puede retornar:
     *   - Una instancia nueva por cada llamada
     *   - Una instancia singleton compartida
     *   - Una lambda/proxy ligero
     * 
     * La estrategia depende de si la capacidad tiene estado mutable o no.
     * 
     * @return instancia de la capacidad (nunca null)
     */
    T createCapability();
}
