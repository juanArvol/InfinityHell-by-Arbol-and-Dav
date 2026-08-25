package Game.Items.Types.Bullets;

import Game.Items.Savement.Inventory;
import Game.Items.Types.Bullets.Definition.BulletType;

/**
 * Inventario de tipos de bala — autoridad del dominio Bullets.
 *
 * ── ARQUITECTURA — Items Module ──────────────────────────────────────────
 *
 * HERENCIA:
 *   BulletInventory extends Inventory<BulletType>
 *   Implementa unicidad: cada tipo de bala solo puede poseerse una vez.
 *
 * RESPONSABILIDADES:
 *   • Almacenar tipos de bala poseídos (BulletType instances)
 *   • Gestionar adquisición única por tipo mediante verificación en add()
 *   • Prevenir duplicados por verificación de identidad
 *   • Exponer la lista para que el sistema de combate la consulte
 *
 * ── SEPARACIÓN DE RESPONSABILIDADES ──────────────────────────────────────
 *
 *   BulletInventory  → almacenamiento de tipos disponibles (únicos)
 *   PlayerRuntime    → selección activa del tipo equipado
 *   BulletFactory    → construcción de instancias Bullet
 *   ProjectilePool   → gestión del ciclo de vida de instancias
 *
 * ── UNICIDAD ──────────────────────────────────────────────────────────────
 *
 * Los tipos de bala se obtienen una sola vez por partida. add() verifica
 * duplicados antes de añadir.
 *
 * Esto es diferente de armas porque BulletType es un tipo singleton,
 * mientras que ModifiedWeapon es una instancia con estado mutable.
 *
 * ── MODELO DE MUNICIÓN ───────────────────────────────────────────────────
 *
 * Las balas son infinitas — representan tipos/equipamientos disponibles,
 * no munición consumible. El sistema no rastrea "cuántas balas quedan"
 * porque conceptualmente el portador tiene acceso ilimitado a cualquier
 * tipo de bala que haya desbloqueado.
 */
public final class BulletInventory extends Inventory<BulletType> {

    /**
     * Constructor sin límite de slots.
     */
    public BulletInventory() {
        super();
    }

    // ── Override add() con lógica de unicidad ─────────────────────────────

    /**
     * Añade un tipo de bala al inventario.
     * Implementa UNICIDAD: si ya se posee (mismo tipo), la operación es no-op.
     *
     * @param bulletType tipo de bala a añadir. No puede ser null.
     * @return true si se añadió (nueva adquisición), false si ya se poseía
     * @throws IllegalArgumentException si bulletType es null
     */
    public boolean addBullet(BulletType bulletType) {
        return addItem(bulletType);
    }

    // ── Métodos de conveniencia específicos del dominio ───────────────────

    /**
     * True si el portador posee el tipo de bala indicado.
     * Alias de contains() para mayor claridad en el dominio.
     *
     * @param bulletType tipo de bala a verificar
     * @return true si se posee
     */
    public boolean hasBullet(BulletType bulletType) {
        return contains(bulletType);
    }
    public boolean removeBullet(){return true;}

    /**
     * Obtiene un tipo de bala por su índice en el inventario.
     * Alias de get() para mayor claridad en el dominio.
     *
     * @param index índice del tipo de bala (0-based)
     * @return el tipo de bala en el índice especificado
     * @throws IndexOutOfBoundsException si el índice es inválido
     */
    public BulletType getBulletType(int index) {
        return getItem(index);
    }
}