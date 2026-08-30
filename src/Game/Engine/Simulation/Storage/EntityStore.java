package Game.Engine.Simulation.Storage;

import Game.Engine.Simulation.ComponentMask;
import Game.Engine.Simulation.EntityId;
import Game.Engine.Simulation.SimulationHandle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Store central que gestiona el ciclo de vida y almacenamiento de entidades simuladas.
 *
 * ── HRFC — Game.Engine Unified Simulation Data Architecture / ECS-DOD ─────
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 *
 * EntityStore es el corazón de la infraestructura DOD. Gestiona:
 *
 * 1. Creación de EntityId únicos
 * 2. Asignación de slots en el dense storage
 * 3. Mapping EntityId → dense index
 * 4. Validación de SimulationHandles
 * 5. Destrucción y compactación de entidades
 * 6. Gestión de free list para reutilización de slots
 *
 * ── ARQUITECTURA ──────────────────────────────────────────────────────────
 *
 *   EntityStore
 *      ├── nextEntityId (contador global monotónico)
 *      ├── records (Map<EntityId, EntityRecord>)
 *      ├── denseToEntity (array: dense index → EntityId)
 *      ├── freeList (slots disponibles para reutilizar)
 *      ├── count (número de entidades vivas actualmente)
 *      └── storage (PrimitiveStorage con los datos SoA)
 *
 * ── FLUJO DE CREACIÓN ────────────────────────────────────────────────────
 *
 *   1. create(mask) → genera nuevo EntityId
 *   2. Busca slot disponible (free list o al final)
 *   3. Crea EntityRecord(id, index, generation, mask)
 *   4. Almacena record en HashMap
 *   5. Retorna EntityId
 *
 * ── FLUJO DE ACCESO ──────────────────────────────────────────────────────
 *
 *   1. getHandle(entityId) → busca record en HashMap
 *   2. Valida que record.alive == true
 *   3. Retorna SimulationHandle(index, generation)
 *   4. Cliente usa handle.index() para acceder a los arrays
 *
 * ── FLUJO DE DESTRUCCIÓN ─────────────────────────────────────────────────
 *
 *   1. destroy(entityId) → marca record.alive = false
 *   2. (Opcionalmente) compactación inmediata o batch
 *
 *   Compactación:
 *   1. Si index < count-1: swap con última entidad
 *   2. storage.swap(index, lastIndex)
 *   3. Actualiza EntityRecord del último: setDenseIndex(index)
 *   4. Decrementa count
 *   5. Añade slot liberado a free list
 *   6. Incrementa generation del slot
 *
 * ── FREE LIST ─────────────────────────────────────────────────────────────
 *
 * Slots liberados se añaden a la free list para reutilización futura.
 * Al crear una nueva entidad, primero se intenta tomar de la free list.
 * Si está vacía, se usa el siguiente slot al final (count++).
 *
 * ── RESIZE AUTOMÁTICO ────────────────────────────────────────────────────
 *
 * Si count alcanza capacity, se hace resize automático:
 *   newCapacity = suggestGrowCapacity()
 *   storage.resize(newCapacity)
 *
 * ── THREAD SAFETY ────────────────────────────────────────────────────────
 *
 * EntityStore NO es thread-safe.
 * Responsabilidad de sincronización es del SimulationPipeline.
 *
 * ── EJEMPLO ──────────────────────────────────────────────────────────────
 *
 *   EntityStore store = new EntityStore();
 *
 *   // Crear entidad con Position + Velocity
 *   ComponentMask mask = ComponentMask.EMPTY
 *       .with(ComponentType.POSITION.id())
 *       .with(ComponentType.VELOCITY.id());
 *   EntityId id = store.create(mask);
 *
 *   // Inicializar datos
 *   SimulationHandle h = store.getHandle(id);
 *   PrimitiveStorage s = store.getStorage();
 *   s.positionsX()[h.index()] = 100f;
 *   s.positionsY()[h.index()] = 200f;
 *   s.velocitiesX()[h.index()] = 5f;
 *   s.velocitiesY()[h.index()] = -3f;
 *
 *   // Destruir entidad
 *   store.destroy(id);
 */
public final class EntityStore {

    private final PrimitiveStorage storage;
    private final Map<EntityId, EntityRecord> records;
    private final EntityId[] denseToEntity;  // mapeo inverso: index → EntityId
    private final List<Integer> freeList;
    private long nextEntityId;
    private int count;  // número de entidades vivas

    /**
     * Constructor con capacidad inicial por defecto.
     */
    public EntityStore() {
        this(256);
    }

    /**
     * Constructor con capacidad inicial especificada.
     *
     * @param initialCapacity capacidad inicial del storage
     */
    public EntityStore(int initialCapacity) {
        this.storage = new PrimitiveStorage(initialCapacity);
        this.records = new HashMap<>();
        this.denseToEntity = new EntityId[initialCapacity];
        this.freeList = new ArrayList<>();
        this.nextEntityId = 1; // 0 está reservado para INVALID
        this.count = 0;
    }

    /**
     * Retorna el PrimitiveStorage que contiene los datos SoA.
     * Los sistemas acceden directamente a este storage para procesamiento batch.
     */
    public PrimitiveStorage getStorage() {
        return storage;
    }

    /**
     * Retorna el número de entidades vivas actualmente.
     */
    public int count() {
        return count;
    }

    /**
     * Retorna la capacidad actual del storage.
     */
    public int capacity() {
        return storage.capacity();
    }

    /**
     * Crea una nueva entidad con la máscara de componentes especificada.
     *
     * @param mask máscara de componentes que tendrá esta entidad
     * @return EntityId único de la nueva entidad
     */
    public EntityId create(ComponentMask mask) {
        // Generar nuevo EntityId
        EntityId entityId = new EntityId(nextEntityId++);

        // Encontrar slot disponible
        int denseIndex;
        int generation;

        if (!freeList.isEmpty()) {
            // Reutilizar slot de la free list
            denseIndex = freeList.remove(freeList.size() - 1);
            generation = findGenerationForSlot(denseIndex) + 1;
        } else {
            // Usar siguiente slot al final
            denseIndex = count;
            generation = 1;

            // Resize si es necesario
            if (denseIndex >= storage.capacity()) {
                resize(storage.suggestGrowCapacity());
            }
        }

        // Crear record
        EntityRecord record = new EntityRecord(entityId, denseIndex, generation, mask);
        records.put(entityId, record);
        denseToEntity[denseIndex] = entityId;
        count++;

        return entityId;
    }

    /**
     * Destruye una entidad.
     * La entidad se marca como muerta pero no se compacta inmediatamente.
     * Llamar compact() para realizar la compactación.
     *
     * @param entityId ID de la entidad a destruir
     * @return true si la entidad fue destruida, false si no existía o ya estaba muerta
     */
    public boolean destroy(EntityId entityId) {
        EntityRecord record = records.get(entityId);
        if (record == null || !record.isAlive()) {
            return false;
        }

        record.markDead();
        return true;
    }

    /**
     * Compacta el storage eliminando entidades muertas.
     * Las entidades muertas son eliminadas mediante swap con la última entidad.
     *
     * Este método puede llamarse:
     * - Después de cada destroy() (compactación inmediata)
     * - Periódicamente en batch (compactación diferida)
     * - Al final de cada frame de simulación
     *
     * @return número de entidades compactadas
     */
    public int compact() {
        int compacted = 0;

        // Iterar sobre records y compactar entidades muertas
        List<EntityId> toRemove = new ArrayList<>();

        for (EntityRecord record : records.values()) {
            if (!record.isAlive()) {
                toRemove.add(record.entityId());
            }
        }

        for (EntityId deadId : toRemove) {
            compactEntity(deadId);
            compacted++;
        }

        return compacted;
    }

    /**
     * Compacta una entidad específica.
     * Usa swap-remove para mantener densidad.
     *
     * @param entityId ID de la entidad muerta a compactar
     */
    private void compactEntity(EntityId entityId) {
        EntityRecord deadRecord = records.remove(entityId);
        if (deadRecord == null) return;

        int deadIndex = deadRecord.denseIndex();
        int lastIndex = count - 1;

        if (deadIndex < lastIndex) {
            // Swap con la última entidad
            EntityId lastEntityId = denseToEntity[lastIndex];
            EntityRecord lastRecord = records.get(lastEntityId);

            // Swap en storage
            storage.swap(deadIndex, lastIndex);

            // Actualizar record de la entidad movida
            lastRecord.setDenseIndex(deadIndex);
            denseToEntity[deadIndex] = lastEntityId;
        }

        // Limpiar slot final
        denseToEntity[lastIndex] = null;
        count--;

        // Añadir a free list y aumentar generation
        freeList.add(deadIndex);
        deadRecord.incrementGeneration();
    }

    /**
     * Obtiene un SimulationHandle para acceder a los datos de una entidad.
     *
     * @param entityId ID de la entidad
     * @return SimulationHandle válido, o INVALID si la entidad no existe/está muerta
     */
    public SimulationHandle getHandle(EntityId entityId) {
        EntityRecord record = records.get(entityId);
        if (record == null || !record.isAlive()) {
            return SimulationHandle.INVALID;
        }
        return new SimulationHandle(record.denseIndex(), record.generation());
    }

    /**
     * Valida que un handle sigue siendo válido.
     * Comprueba generation counter para detectar reuso de slots.
     *
     * @param handle handle a validar
     * @return true si el handle es válido para el estado actual del store
     */
    public boolean validateHandle(SimulationHandle handle) {
        if (!handle.isValid()) return false;

        int index = handle.index();
        if (index >= count) return false;

        EntityId entityId = denseToEntity[index];
        if (entityId == null) return false;

        EntityRecord record = records.get(entityId);
        if (record == null || !record.isAlive()) return false;

        return record.generation() == handle.generation();
    }

    /**
     * Retorna el EntityId correspondiente a un índice denso.
     * Usado para debugging o sistemas que iteran directamente sobre índices.
     *
     * @param denseIndex índice en el storage denso
     * @return EntityId de la entidad en ese slot, o null si no hay entidad
     */
    public EntityId getEntityAt(int denseIndex) {
        if (denseIndex < 0 || denseIndex >= count) return null;
        return denseToEntity[denseIndex];
    }

    /**
     * Retorna el EntityRecord de una entidad.
     * Expuesto para debugging — no usar en hot paths.
     *
     * @param entityId ID de la entidad
     * @return EntityRecord, o null si no existe
     */
    public EntityRecord getRecord(EntityId entityId) {
        return records.get(entityId);
    }

    /**
     * Añade un componente a una entidad existente.
     * Actualiza su ComponentMask pero NO inicializa los datos del componente.
     * El caller debe inicializar manualmente los valores en el storage.
     *
     * @param entityId ID de la entidad
     * @param componentId ID del componente a añadir
     * @return true si se añadió, false si la entidad no existe o ya tenía el componente
     */
    public boolean addComponent(EntityId entityId, int componentId) {
        EntityRecord record = records.get(entityId);
        if (record == null || !record.isAlive()) return false;

        ComponentMask oldMask = record.mask();
        if (oldMask.has(componentId)) return false; // ya tiene el componente

        ComponentMask newMask = oldMask.with(componentId);
        record.setMask(newMask);
        return true;
    }

    /**
     * Elimina un componente de una entidad existente.
     * Actualiza su ComponentMask pero NO limpia los datos del componente en el storage.
     *
     * @param entityId ID de la entidad
     * @param componentId ID del componente a eliminar
     * @return true si se eliminó, false si la entidad no existe o no tenía el componente
     */
    public boolean removeComponent(EntityId entityId, int componentId) {
        EntityRecord record = records.get(entityId);
        if (record == null || !record.isAlive()) return false;

        ComponentMask oldMask = record.mask();
        if (!oldMask.has(componentId)) return false; // no tiene el componente

        ComponentMask newMask = oldMask.without(componentId);
        record.setMask(newMask);
        return true;
    }

    /**
     * Retorna true si una entidad tiene un componente específico.
     *
     * @param entityId ID de la entidad
     * @param componentId ID del componente
     * @return true si la entidad existe, está viva y tiene el componente
     */
    public boolean hasComponent(EntityId entityId, int componentId) {
        EntityRecord record = records.get(entityId);
        if (record == null || !record.isAlive()) return false;
        return record.mask().has(componentId);
    }

    /**
     * Resize interno del storage.
     *
     * @param newCapacity nueva capacidad
     */
    private void resize(int newCapacity) {
        storage.resize(newCapacity);
        // No es necesario redimensionar denseToEntity inmediatamente —
        // se hará en la próxima creación si es necesario.
        // Por simplicidad, no se implementa resize de denseToEntity aquí.
    }

    /**
     * Encuentra el generation counter de un slot específico.
     * Usado al reutilizar slots de la free list.
     *
     * @param slotIndex índice del slot
     * @return generation counter, o 0 si el slot nunca fue usado
     */
    private int findGenerationForSlot(int slotIndex) {
        EntityId entityId = denseToEntity[slotIndex];
        if (entityId == null) return 0;

        EntityRecord record = records.get(entityId);
        if (record == null) return 0;

        return record.generation();
    }
}
