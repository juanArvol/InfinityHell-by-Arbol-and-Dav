package Game.Items.Types.Bullets;

import Game.Engine.GameEventBus;
import Game.Items.Savement.Types.SelectableInventory;
import Game.Items.Types.Bullets.Definition.BulletType;

/**
 * Inventario de tipos de bala — autoridad del dominio Bullets.
 *
 * ── ARQUITECTURA — Items Module ──────────────────────────────────────────
 *
 * HERENCIA:
 *
 *   BulletInventory
 *       └── SelectableInventory<BulletType>
 *
 * RESPONSABILIDADES:
 *
 *   • Almacenar tipos de bala poseídos
 *   • Gestionar adquisición única por tipo
 *   • Exponer la selección actual de bala
 *   • Proporcionar una API específica del dominio Bullets
 *
 * La lógica genérica de selección pertenece a SelectableInventory.
 *
 * ── SELECCIÓN ─────────────────────────────────────────────────────────────
 *
 * BulletInventory mantiene su propia selección independiente de cualquier
 * otro inventario seleccionable.
 *
 *   WeaponInventory.currentIndex
 *          ≠
 *   BulletInventory.currentIndex
 *
 * Cambiar la bala seleccionada no modifica el arma seleccionada.
 *
 * ── EVENTOS ───────────────────────────────────────────────────────────────
 *
 * Los cambios de selección se comunican mediante:
 *
 *   InventoryEvents.OnSelectionChange<BulletType>
 *
 * BulletInventory no define eventos propios para selección.
 *
 * ── UNICIDAD ──────────────────────────────────────────────────────────────
 *
 * Cada BulletType solo puede aparecer una vez en el inventario.
 *
 * ── MODELO DE MUNICIÓN ───────────────────────────────────────────────────
 *
 * Las balas son infinitas.
 *
 * BulletType representa un tipo/equipamiento disponible, no una cantidad
 * consumible de munición.
 */
public final class BulletInventory
        extends SelectableInventory<BulletType> {

    /**
     * Constructor sin límite de slots ni bus de eventos.
     */
    public BulletInventory() {
        super();
    }

    /**
     * Constructor con bus de eventos.
     *
     * Permite emitir InventoryEvents.OnSelectionChange<BulletType>.
     */
    public BulletInventory(GameEventBus eventBus) {
        super(eventBus);
    }

    // ── Gestión de balas ──────────────────────────────────────────────────

    /**
     * Añade un tipo de bala al inventario.
     *
     * La unicidad es responsabilidad de Inventory.addItem().
     *
     * @param bulletType tipo de bala a añadir
     * @return true si fue añadida, false si ya estaba presente
     */
    public boolean addBullet(BulletType bulletType) {
        return addItem(bulletType);
    }

    /**
     * Elimina un tipo de bala del inventario.
     *
     * La eliminación se delega a SelectableInventory para que:
     *
     *   • mantenga currentIndex válido
     *   • preserve la selección cuando corresponda
     *   • emita OnSelectionChange si la selección cambia
     *
     * @param bulletType tipo de bala a eliminar
     * @return true si fue eliminada
     */
    public boolean removeBullet(BulletType bulletType) {
        return removeItem(bulletType);
    }

    /**
     * Elimina un tipo de bala por índice.
     *
     * @param index índice del tipo de bala
     * @return tipo de bala eliminado
     */
    public BulletType removeBulletAt(int index) {
        return removeItemAt(index);
    }

    // ── Consultas ─────────────────────────────────────────────────────────

    /**
     * Determina si el inventario contiene el tipo de bala indicado.
     *
     * @param bulletType tipo de bala a comprobar
     * @return true si se posee
     */
    public boolean hasBullet(BulletType bulletType) {
        return containsItem(bulletType);
    }

    /**
     * Obtiene un tipo de bala por índice.
     *
     * @param index índice del tipo de bala
     * @return tipo de bala
     */
    public BulletType getBulletBy(int index) {
        return getItem(index);
    }

    /**
     * Obtiene el tipo de bala actualmente seleccionado.
     *
     * @return bala seleccionada, o null si el inventario está vacío
     */
    public BulletType getCurrentBullet() {
        return getCurrent();
    }

    // ── Selección ─────────────────────────────────────────────────────────

    /**
     * Avanza a la siguiente bala.
     */
    public void nextBullet() {
        next();
    }

    /**
     * Retrocede a la bala anterior.
     */
    public void previousBullet() {
        previous();
    }

    /**
     * Selecciona una bala directamente por índice.
     *
     * @param index índice de la bala
     */
    public void selectBulletAt(int index) {
        selectAt(index);
    }

    /**
     * Obtiene el índice de la bala actualmente seleccionada.
     *
     * @return índice actual
     */
    public int getCurrentBulletIndex() {
        return getCurrentIndex();
    }

    // ── Inventario ────────────────────────────────────────────────────────

    /**
     * Vacía completamente el inventario de balas.
     */
    public void clearBulletsInventory() {
        clear();
    }
}