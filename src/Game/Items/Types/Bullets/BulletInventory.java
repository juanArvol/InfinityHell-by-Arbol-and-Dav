package Game.Items.Types.Bullets;

import Game.Items.Types.Bullets.Definition.BulletType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Inventario de tipos de bala — autoridad del dominio Bullets.
 *
 * ── HRFC — Player Inventory & Domain Ownership Consolidation ──────────────
 *
 * ── OWNERSHIP ─────────────────────────────────────────────────────────────
 *
 * BulletInventory pertenece al dominio Bullets y responde la pregunta:
 *   "¿Qué tipos de bala posee el portador?"
 *
 * PlayerRuntime responde: "¿Qué tipo de bala está equipado actualmente?"
 * BulletFactory responde: "¿Cómo se construyen instancias Bullet?"
 *
 * ── RESPONSABILIDADES ─────────────────────────────────────────────────────
 *
 *   • Almacenar tipos de bala poseídos (BulletType enum)
 *   • Gestionar adquisición de tipos de bala
 *   • Prevenir duplicados por diseño (un tipo solo se puede poseer una vez)
 *   • Exponer la API de consulta de tipos de bala
 *
 * ── SEPARACIÓN DE RESPONSABILIDADES ──────────────────────────────────────
 *
 *   BulletInventory  → almacenamiento de tipos disponibles
 *   PlayerRuntime    → selección activa del tipo equipado
 *   BulletFactory    → construcción de instancias Bullet
 *   ProjectilePool   → gestión del ciclo de vida de instancias
 *
 * ── UNICIDAD ──────────────────────────────────────────────────────────────
 *
 * Los tipos de bala se obtienen una sola vez por partida. BulletInventory
 * previene duplicados por diseño (un tipo de bala solo se puede poseer una vez).
 *
 * Esto es diferente de armas porque BulletType es un enum/singleton conceptual,
 * mientras que ModifiedWeapon es una instancia con estado.
 *
 * ── MODELO DE MUNICIÓN ───────────────────────────────────────────────────
 *
 * Las balas son infinitas — representan tipos/equipamientos disponibles,
 * no munición consumible. El sistema no rastrea "cuántas balas quedan"
 * porque conceptualmente el portador tiene acceso ilimitado a cualquier
 * tipo de bala que haya desbloqueado.
 */
public final class BulletInventory {

    /** Tipos de bala poseídos por el portador. */
    private final List<BulletType> bullets = new ArrayList<>();

    // ── Gestión de balas ──────────────────────────────────────────────────

    /**
     * Añade un tipo de bala al inventario.
     * Si ya se posee, no se duplica (idempotente).
     *
     * @param bulletType tipo de bala a añadir. No puede ser null.
     * @throws IllegalArgumentException si bulletType es null
     */
    public void addBullet(BulletType bulletType) {
        if (bulletType == null) {
            throw new IllegalArgumentException("bulletType no puede ser null");
        }
        
        if (!bullets.contains(bulletType)) {
            bullets.add(bulletType);
        }
    }

    /**
     * Elimina un tipo de bala del inventario.
     *
     * @param bulletType tipo de bala a eliminar
     * @return true si se eliminó, false si no se encontró
     */
    public boolean removeBullet(BulletType bulletType) {
        return bullets.remove(bulletType);
    }

    /**
     * Elimina el tipo de bala en el índice especificado.
     *
     * @param index índice del tipo de bala a eliminar (0-based)
     * @return el tipo de bala eliminado
     * @throws IndexOutOfBoundsException si el índice es inválido
     */
    public BulletType removeBulletAt(int index) {
        return bullets.remove(index);
    }

    // ── Consultas ─────────────────────────────────────────────────────────

    /**
     * True si el portador posee el tipo de bala indicado.
     *
     * @param bulletType tipo de bala a verificar
     * @return true si se posee
     */
    public boolean hasBullet(BulletType bulletType) {
        return bullets.contains(bulletType);
    }

    /**
     * Obtiene un tipo de bala por su índice en el inventario.
     *
     * @param index índice del tipo de bala (0-based)
     * @return el tipo de bala en el índice especificado
     * @throws IndexOutOfBoundsException si el índice es inválido
     */
    public BulletType getBullet(int index) {
        return bullets.get(index);
    }

    /**
     * Lista inmutable de tipos de bala poseídos (en orden de adquisición).
     *
     * @return lista de tipos de bala. Nunca null, puede estar vacía.
     */
    public List<BulletType> getAll() {
        return Collections.unmodifiableList(bullets);
    }

    /** True si no se poseen tipos de bala. */
    public boolean isEmpty() {
        return bullets.isEmpty();
    }

    /** Número total de tipos de bala poseídos. */
    public int size() {
        return bullets.size();
    }

    /**
     * Índice del tipo de bala especificado en el inventario.
     *
     * @param bulletType tipo de bala a buscar
     * @return índice (0-based), o -1 si no se posee
     */
    public int indexOf(BulletType bulletType) {
        return bullets.indexOf(bulletType);
    }

    // ── Limpieza ──────────────────────────────────────────────────────────

    /**
     * Limpia el inventario — útil para testing o reinicios de run.
     */
    public void clear() {
        bullets.clear();
    }
}