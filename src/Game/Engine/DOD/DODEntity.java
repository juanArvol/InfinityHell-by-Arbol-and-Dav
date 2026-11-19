package Game.Engine.DOD;

import Game.Engine.Simulation.ComponentMask;
import Game.Engine.Simulation.EntityId;
import Game.Engine.Simulation.SimulationHandle;
import Game.Engine.Simulation.Storage.EntityStore;
import Game.Engine.Simulation.Storage.PrimitiveStorage;

/**
 * Helper que encapsula la identidad y almacenamiento DOD de una entidad.
 * 
 * ── PROPÓSITO ─────────────────────────────────────────────────────────────
 * 
 * DODEntity NO es una clase base en la jerarquía de GameObjects.
 * Es un COMPONENTE que gestiona el aspecto DOD de una entidad mediante composición.
 * 
 * Las entidades que usan DOD (Bullet, Enemy) tienen un campo:
 *   private final DODEntity dod;
 * 
 * Las entidades que NO usan DOD (Player, WorldItem, etc.) simplemente no lo tienen.
 * 
 * ── RESPONSABILIDADES ─────────────────────────────────────────────────────
 * 
 * - Registrar la entidad en EntityStore y obtener EntityId
 * - Mantener el SimulationHandle para acceso por índice
 * - Proveer acceso directo al PrimitiveStorage (arrays primitivos)
 * - Destruir la entidad del EntityStore cuando se elimine
 * 
 * ── USO ───────────────────────────────────────────────────────────────────
 * 
 * En el constructor de la entidad:
 * 
 *   public Bullet(..., EntityStore entityStore) {
 *       super(); // GameObjects constructor normal
 *       this.dod = new DODEntity(entityStore, ComponentType.PROJECTILE_MASK);
 *       
 *       // Inicializar datos en storage usando dod.index() y dod.getStorage()
 *       int idx = dod.index();
 *       dod.getStorage().positionsX()[idx] = (float) posX;
 *       dod.getStorage().velocitiesX()[idx] = (float) xSpeed;
 *       // ...
 *   }
 * 
 * Acceso a datos:
 * 
 *   public double getPositionX() {
 *       return dod.getStorage().positionsX()[dod.index()];
 *   }
 * 
 * ── ARQUITECTURA ──────────────────────────────────────────────────────────
 * 
 * Bullet, Enemy, Player, etc. → GameObjects (todos en la misma jerarquía)
 *                                    ↓
 *                        Algunos tienen DODEntity (composición)
 *                                    ↓
 *                            EntityStore + PrimitiveStorage
 * 
 * DOD es sobre CÓMO se almacena, no sobre QUÉ es el objeto.
 * Todos siguen siendo GameObjects para colisiones, componentes, etc.
 * 
 * ── GARANTÍA: VERDADERO DOD ───────────────────────────────────────────────
 * 
 * Este helper garantiza que las entidades que lo usan REALMENTE usan DOD:
 * - Datos en arrays primitivos contiguos (PrimitiveStorage)
 * - Acceso por índice directo (SimulationHandle.index())
 * - Sin wrappers u objetos intermedios innecesarios
 * - Cache-friendly, SIMD-friendly
 */
public final class DODEntity {
    
    private final EntityStore entityStore;
    private final EntityId entityId;
    private final SimulationHandle simulationHandle;
    
    /**
     * Crea una nueva entidad DOD y la registra en el EntityStore.
     * 
     * @param entityStore store donde se registrará esta entidad
     * @param componentMask máscara de componentes que define qué datos usa
     * @throws IllegalArgumentException si entityStore o componentMask son null
     */
    public DODEntity(EntityStore entityStore, ComponentMask componentMask) {
        if (entityStore == null) {
            throw new IllegalArgumentException("EntityStore cannot be null for DOD entities");
        }
        if (componentMask == null) {
            throw new IllegalArgumentException("ComponentMask cannot be null for DOD entities");
        }
        
        this.entityStore = entityStore;
        this.entityId = entityStore.create(componentMask);
        this.simulationHandle = entityStore.getHandle(entityId);
    }
    
    /**
     * Retorna el índice directo en los arrays de PrimitiveStorage.
     * 
     * Este es el método principal para acceso a datos DOD:
     * 
     *   int idx = dod.index();
     *   storage.positionsX()[idx] = newX;
     *   float currentY = storage.positionsY()[idx];
     * 
     * @return índice válido mientras SimulationHandle.isValid()
     */
    public int index() {
        return simulationHandle.index();
    }
    
    /**
     * Retorna el PrimitiveStorage para acceso directo a arrays.
     * 
     * El PrimitiveStorage contiene los arrays primitivos donde viven
     * REALMENTE los datos de la entidad:
     * - float[] positionsX, positionsY
     * - float[] velocitiesX, velocitiesY
     * - float[] lifetimes, health, damage, etc.
     * 
     * @return storage compartido por todas las entidades del mismo tipo
     */
    public PrimitiveStorage getStorage() {
        return entityStore.getStorage();
    }
    
    /**
     * Retorna el EntityStore donde esta entidad está registrada.
     */
    public EntityStore getEntityStore() {
        return entityStore;
    }
    
    /**
     * Retorna el EntityId único de esta entidad.
     */
    public EntityId getEntityId() {
        return entityId;
    }
    
    /**
     * Retorna el SimulationHandle para validación y acceso al índice.
     */
    public SimulationHandle getSimulationHandle() {
        return simulationHandle;
    }
    
    /**
     * Verifica si el handle sigue siendo válido.
     * 
     * Un handle puede invalidarse si la entidad fue destruida o el
     * EntityStore hizo compact() y reordenó los índices.
     * 
     * @return true si el handle es válido y el índice es seguro de usar
     */
    public boolean isValid() {
        return simulationHandle != null && simulationHandle.isValid();
    }
    
    /**
     * Destruye esta entidad del EntityStore.
     * 
     * Marca el slot como libre para ser reutilizado. El compact()
     * del EntityStore limpiará el slot al final del frame.
     */
    public void destroy() {
        if (entityStore != null && entityId != null) {
            entityStore.destroy(entityId);
        }
    }
}
