package Game.Player;

import Game.Items.Types.Ammulets.AmuletInventory;
import Game.Items.Types.Ammulets.AmuletRegistry;
import Game.Items.Types.Bullets.BulletInventory;
import Game.Items.Types.Bullets.Definition.BulletType;
import Game.Items.Types.Weapons.ModifiedWeapon;
import Game.Items.Types.Weapons.WeaponInventory;
import Game.Items.Types.Weapons.WeaponType.WeaponType;
import java.util.List;

/**
 * Fachada del inventario del Player — coordina inventarios especializados de dominio.
 *
 * ── HRFC — Player Inventory & Domain Ownership Consolidation ──────────────
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 *
 * PlayerInventory es la fachada/intermediario del inventario del Player.
 * NO es una God Class — delega las operaciones específicas a los inventarios
 * especializados correspondientes que viven en sus dominios.
 *
 * Su responsabilidad es:
 *   • Exponer el acceso coherente al inventario
 *   • Delegar operaciones a inventarios especializados de dominio
 *   • Servir como punto de entrada para sistemas externos que necesiten
 *     interactuar con el inventario
 *
 * ── ARQUITECTURA ──────────────────────────────────────────────────────────
 *
 *   PlayerInventory (fachada)
 *        │
 *        ├── Items.Types.Weapons.WeaponInventory  (autoridad de armas)
 *        ├── Items.Types.Bullets.BulletInventory  (autoridad de balas)
 *        └── Items.Types.Ammulets.AmuletInventory (autoridad de amuletos)
 *
 * PlayerInventory coordina/accede; los inventarios especializados implementan.
 * Cada inventario vive en su dominio correspondiente, respetando el ownership.
 *
 * ── SEPARACIÓN DE RESPONSABILIDADES ──────────────────────────────────────
 *
 *   PlayerInventory  → fachada/coordinador (este archivo)
 *   WeaponInventory  → almacenamiento de armas (Items.Types.Weapons)
 *   BulletInventory  → almacenamiento de balas (Items.Types.Bullets)
 *   AmuletInventory  → almacenamiento de amuletos (Items.Types.Ammulets)
 *   PlayerRuntime    → estado de equipamiento
 *   PlayerCombat     → ejecución de combate
 *
 * ── LO QUE NO CONTIENE ────────────────────────────────────────────────────
 *
 * PlayerInventory NO debe contener:
 *   ✗ Lógica específica de armas
 *   ✗ Lógica específica de balas
 *   ✗ Lógica específica de amuletos
 *   ✗ Lógica de combate
 *   ✗ Lógica de equipamiento
 *   ✗ Lógica de selección
 *   ✗ Operaciones shoot(), reload(), attack()
 *
 * ── SINGLE SOURCE OF TRUTH ────────────────────────────────────────────────
 *
 * Los inventarios pertenecen al dominio que administran, no al Player:
 *
 *   WeaponInventory  → Items.Types.Weapons   (NO Player.Inventory)
 *   BulletInventory  → Items.Types.Bullets   (NO Player.Inventory)
 *   AmuletInventory  → Items.Types.Ammulets  (NO Player.Inventory)
 *
 * PlayerInventory compone y expone, pero no duplica ni implementa.
 */
public final class PlayerInventory {

    private final WeaponInventory weapons;
    private final BulletInventory bullets;
    private final AmuletInventory amulets;

    /**
     * Construye el inventario del Player con inventarios especializados vacíos.
     * Los inventarios se instancian desde sus dominios correspondientes.
     */
    public PlayerInventory() {
        this.weapons = new WeaponInventory();    // Items.Types.Weapons
        this.bullets = new BulletInventory();    // Items.Types.Bullets  
        this.amulets = new AmuletInventory();    // Items.Types.Ammulets
    }

    // ── Acceso a inventarios especializados ───────────────────────────────

    /**
     * Inventario especializado de armas.
     * @return WeaponInventory — nunca null
     */
    public WeaponInventory weapons() {
        return weapons;
    }

    /**
     * Inventario especializado de balas.
     * @return BulletInventory — nunca null
     */
    public BulletInventory bullets() {
        return bullets;
    }

    /**
     * Inventario especializado de amuletos.
     * @return AmuletInventory — nunca null
     */
    public AmuletInventory amulets() {
        return amulets;
    }

    // ── APIs de conveniencia para armas ───────────────────────────────────

    /**
     * Añade un arma al inventario.
     * Delega a WeaponInventory.
     *
     * @param weapon arma a añadir
     * @return true si se añadió (nueva adquisición), false si ya se poseía
     */
    public boolean addWeapon(ModifiedWeapon weapon) {
        return weapons.addWeapon(weapon);
    }

    /**
     * Elimina un arma del inventario.
     * Delega a WeaponInventory.
     *
     * @param weapon arma a eliminar
     * @return true si se eliminó, false si no se encontró
     */
    public boolean removeWeapon(ModifiedWeapon weapon) {
        return weapons.removeWeapon(weapon);
    }

    /**
     * True si el Player posee un arma del tipo indicado.
     * Delega a WeaponInventory.
     *
     * @param weaponType tipo de arma a verificar
     * @return true si se posee
     */
    public boolean hasWeapon(WeaponType weaponType) {
        return weapons.hasWeapon(weaponType);
    }

    /**
     * Lista inmutable de armas poseídas.
     * Delega a WeaponInventory.
     *
     * @return lista de armas. Nunca null.
     */
    public List<ModifiedWeapon> getWeapons() {
        return weapons.getAll();
    }

    // ── APIs de conveniencia para balas ───────────────────────────────────

    /**
     * Añade un tipo de bala al inventario.
     * Delega a BulletInventory.
     *
     * @param bulletType tipo de bala a añadir
     * @return true si se añadió (nueva adquisición), false si ya se poseía
     */
    public boolean addBullet(BulletType bulletType) {
        return bullets.addBullet(bulletType);
    }

    /**
     * Elimina un tipo de bala del inventario.
     * Delega a BulletInventory.
     *
     * @param bulletType tipo de bala a eliminar
     * @return true si se eliminó, false si no se encontró
     */
    public boolean removeBullet(BulletType bulletType) {
        return bullets.removeBullet(bulletType);
    }

    /**
     * True si el Player posee el tipo de bala indicado.
     * Delega a BulletInventory.
     *
     * @param bulletType tipo de bala a verificar
     * @return true si se posee
     */
    public boolean hasBullet(BulletType bulletType) {
        return bullets.hasBullet(bulletType);
    }

    /**
     * Lista inmutable de tipos de bala poseídos.
     * Delega a BulletInventory.
     *
     * @return lista de tipos de bala. Nunca null.
     */
    public List<BulletType> getBullets() {
        return bullets.getAll();
    }

    // ── APIs de conveniencia para amuletos ────────────────────────────────

    /**
     * Añade un amuleto al inventario.
     * Resuelve el ID a AmuletDefinition antes de delegar a AmuletInventory.
     *
     * @param amuletId ID del amuleto a añadir
     * @return true si se añadió (nueva adquisición), false si ya se poseía
     * @throws IllegalArgumentException si amuletId es null o no existe
     */
    public boolean addAmulet(String amuletId) {
        var definition = AmuletRegistry.get(amuletId);
        return amulets.add(definition);
    }

    // ── Consultas de estado ───────────────────────────────────────────────

    /**
     * True si no se poseen armas.
     */
    public boolean hasNoWeapons() {
        return weapons.isEmpty();
    }

    /**
     * True si no se poseen balas.
     */
    public boolean hasNoBullets() {
        return bullets.isEmpty();
    }

    /**
     * Número total de armas poseídas.
     */
    public int getWeaponCount() {
        return weapons.size();
    }

    /**
     * Número total de tipos de bala poseídos.
     */
    public int getBulletCount() {
        return bullets.size();
    }

    /**
     * Número total de amuletos únicos poseídos.
     */
    public int getAmuletCount() {
        return amulets.size();
    }

    // ── Limpieza ──────────────────────────────────────────────────────────

    /**
     * Limpia todos los inventarios — útil para testing o reinicios de run.
     */
    public void clear() {
        weapons.clear();
        bullets.clear();
        amulets.clear();
    }
}