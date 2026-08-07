package Game.World.Entity;

import Game.Engine.Destroyable;
import Game.Engine.GameObjects;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Registro de entidades dinámicas del mundo global.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * DynamicEntityRegistry almacena todas las entidades que tienen comportamiento
 * dinámico y no están fijas al contenido estático de un chunk:
 *
 *   Player, Enemy, Bullet, Projectile, WorldItem (drops), NPC, etc.
 *
 * ── INDEPENDENCIA DE CHUNKS ───────────────────────────────────────────────
 * Una entidad dinámica puede cruzar libremente los límites de cualquier chunk
 * sin ser transferida, pausada, reiniciada ni destruida.
 *
 * La posición de una entidad dinámica es SIEMPRE global. Su "afiliación" a
 * un chunk es solo metadata de bookkeeping (ChunkAffiliationSystem) usada
 * para serialización/save-load, no para simulación.
 *
 * ── INVARIANTE CRÍTICO ────────────────────────────────────────────────────
 * Un chunk puede descargarse (ChunkStorage.evict()) sin afectar a ninguna
 * entidad dinámica viva. Las entidades dinámicas viven aquí, no en chunks.
 *
 * ── PENDIENTE DE ADICIÓN / REMOCIÓN ──────────────────────────────────────
 * Igual que WorldObjectsContainer, las modificaciones se acumulan en
 * pendingAdd / pendingRemove y se aplican en flush() al inicio del tick.
 * Esto evita ConcurrentModificationException si una entidad añade/elimina
 * otras entidades durante su update().
 *
 * ── CLEANUP AUTOMÁTICO DE DESTROYABLE ────────────────────────────────────
 * En cada flush(), las entidades que implementan {@link Destroyable} y
 * retornan isPendingDestruction() = true son automáticamente eliminadas.
 *
 * ── THREAD SAFETY ─────────────────────────────────────────────────────────
 * No es thread-safe. Toda escritura debe ocurrir desde el game loop thread.
 * WorldPrewarmService solo escribe en ChunkStorage (thread separado).
 */
public final class DynamicEntityRegistry {

    // ── Estado ────────────────────────────────────────────────────────────

    /** Lista viva — entidades activas en el world. */
    private final List<GameObjects> entities     = new ArrayList<>();

    /** Pendientes de añadir en el próximo flush. */
    private final List<GameObjects> pendingAdd    = new ArrayList<>();

    /** Pendientes de eliminar en el próximo flush. */
    private final List<GameObjects> pendingRemove = new ArrayList<>();

    // ── API pública ───────────────────────────────────────────────────────

    /**
     * Registra una entidad dinámica para añadirse en el próximo flush().
     * Thread-safe solo desde el game loop thread.
     *
     * @param entity la entidad a añadir (Player, Enemy, Bullet, etc.)
     */
    public void add(GameObjects entity) {
        if (entity != null) pendingAdd.add(entity);
    }

    /**
     * Marca una entidad para eliminarse en el próximo flush().
     *
     * @param entity la entidad a eliminar
     */
    public void remove(GameObjects entity) {
        if (entity != null) pendingRemove.add(entity);
    }

    /**
     * Aplica las adiciones y remociones pendientes. También elimina
     * automáticamente las entidades marcadas como Destroyable.
     *
     * Debe llamarse al inicio de cada tick, antes de que los sistemas
     * lean la lista de entidades.
     */
    public void flush() {
        // Aplicar adiciones
        if (!pendingAdd.isEmpty()) {
            entities.addAll(pendingAdd);
            pendingAdd.clear();
        }

        // Aplicar remociones explícitas
        if (!pendingRemove.isEmpty()) {
            entities.removeAll(pendingRemove);
            pendingRemove.clear();
        }

        // Cleanup automático de Destroyable
        // Separado en dos pasos para evitar modificar entities durante la iteración
        List<GameObjects> toDestroy = null;
        for (GameObjects e : entities) {
            if (e instanceof Destroyable d && d.isPendingDestruction()) {
                if (toDestroy == null) toDestroy = new ArrayList<>();
                toDestroy.add(e);
            }
        }
        if (toDestroy != null) {
            entities.removeAll(toDestroy);
        }
    }

    /**
     * Aplica un flush inmediato y forzado. Útil después de transferencias
     * o al cambiar de estado del juego.
     */
    public void flushImmediate() {
        flush();
    }

    // ── Consulta ──────────────────────────────────────────────────────────

    /**
     * Vista de solo lectura de todas las entidades dinámicas activas.
     *
     * Los sistemas de simulación (CollisionsSystem, AISystem) consumen
     * esta lista junto con los objetos estáticos de SimulationRegion.
     *
     * @return lista inmutable de entidades dinámicas vivas
     */
    public List<GameObjects> getAll() {
        return Collections.unmodifiableList(entities);
    }

    /**
     * Filtro por tipo. Retorna solo las entidades que son instancia del
     * tipo dado. Crea una nueva lista — no modifica el estado interno.
     *
     * @param type clase o interfaz a filtrar
     * @param <T>  tipo genérico
     * @return lista de entidades del tipo indicado
     */
    public <T> List<T> getByType(Class<T> type) {
        List<T> result = new ArrayList<>();
        for (GameObjects e : entities) {
            if (type.isInstance(e)) result.add(type.cast(e));
        }
        return result;
    }

    /**
     * Número de entidades dinámicas activas.
     *
     * @return cantidad de entidades
     */
    public int size() { return entities.size(); }

    /**
     * True si no hay entidades dinámicas activas.
     *
     * @return true si la lista está vacía
     */
    public boolean isEmpty() { return entities.isEmpty(); }

    /**
     * True si la entidad está registrada como dinámica.
     *
     * @param entity la entidad a comprobar
     * @return true si está en la lista activa o pendiente de adición
     */
    public boolean contains(GameObjects entity) {
        return entities.contains(entity) || pendingAdd.contains(entity);
    }

    // ── Limpieza total ────────────────────────────────────────────────────

    /**
     * Elimina todas las entidades dinámicas. Llamar al reiniciar el mundo
     * o cambiar de nivel. No invoca callbacks — destrucción completa.
     */
    public void clear() {
        entities.clear();
        pendingAdd.clear();
        pendingRemove.clear();
    }
}
