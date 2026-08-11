package Game.Player;

import Game.Items.Types.Bullets.Definition.BulletType;
import Game.Items.Types.Weapons.ModifiedWeapon;
import Game.Items.Types.Weapons.WeaponType.WeaponType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Inventario específico del Player — almacena armas y balas independientemente.
 *
 * ── HRFC — Player Reengineering v2 ────────────────────────────────────────
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 *
 * PlayerInventory separa el almacenamiento de posesiones de la ejecución
 * de combate. Permite selección independiente de armas y balas:
 *
 *   PlayerInventory                PlayerCombat
 *        │                             │
 *        ├── Weapon collection         │
 *        │      ├── Weapon A           │
 *        │      ├── Weapon B           │
 *        │      └── Weapon C           │
 *        │                             │
 *        └── Bullet collection         │
 *               ├── BulletType A       │
 *               ├── BulletType B       │ consulta
 *               └── BulletType C ◄─────┘
 *
 * ── SEPARACIÓN DE RESPONSABILIDADES ──────────────────────────────────────
 *
 *   PlayerInventory → almacenamiento (qué posee el Player)
 *   PlayerRuntime   → selección activa (qué está equipado actualmente)
 *   PlayerCombat    → ejecución (cómo se utiliza lo equipado)
 *
 * ── SELECCIÓN INDEPENDIENTE ───────────────────────────────────────────────
 *
 * Ejemplo de uso correcto:
 *
 *   Weapons: [Pistola, Escopeta]
 *   Bullets: [NormalBullet, BulletJump]
 *
 *   Current: Escopeta + BulletJump
 *
 *   runtime.selectWeapon(Pistola)  → Pistola + BulletJump
 *   runtime.selectBullet(Normal)   → Pistola + NormalBullet
 *
 * Cambiar de arma NO cambia automáticamente la bala.
 * Adquirir una nueva bala NO elimina las anteriores.
 *
 * ── DIFERENCIA CON WeaponInventory ────────────────────────────────────────
 *
 * WeaponInventory (existente):
 *   - Gestiona solo armas
 *   - Vive dentro de PlayerCombat
 *   - Mezcla almacenamiento con ejecución
 *
 * PlayerInventory (nuevo):
 *   - Gestiona armas Y balas por separado
 *   - Vive independiente de PlayerCombat
 *   - Solo almacenamiento, sin lógica de combate
 */
public class PlayerInventory {

    /** Armas poseídas por el Player. */
    private final List<ModifiedWeapon> weapons = new ArrayList<>();

    /** Tipos de bala poseídos por el Player. */
    private final List<BulletType> bullets = new ArrayList<>();

    // ── Gestión de armas ──────────────────────────────────────────────────

    /**
     * Añade un arma al inventario.
     * Si ya se posee un arma del mismo tipo, no se duplica.
     *
     * @param weapon arma a añadir
     */
    public void addWeapon(ModifiedWeapon weapon) {
        if (weapon == null) {
            throw new IllegalArgumentException("weapon no puede ser null");
        }
        
        // TODO: Implementar comparación por tipo cuando ModifiedWeapon exponga WeaponType
        // Por ahora añadir directamente sin verificar duplicados
        weapons.add(weapon);
    }

    /**
     * Elimina un arma del inventario.
     *
     * @param weapon arma a eliminar
     * @return true si se eliminó, false si no se encontró
     */
    public boolean removeWeapon(ModifiedWeapon weapon) {
        return weapons.remove(weapon);
    }

    /**
     * True si el Player posee un arma del tipo indicado.
     *
     * @param weaponType tipo de arma a verificar
     * @return true si se posee
     */
    public boolean hasWeapon(WeaponType weaponType) {
        // TODO: Implementar cuando ModifiedWeapon exponga WeaponType
        // Por ahora retornar false ya que no podemos comparar
        return !weapons.isEmpty(); // Placeholder
    }

    /**
     * Lista inmutable de armas poseídas.
     *
     * @return lista de armas. Nunca null.
     */
    public List<ModifiedWeapon> getWeapons() {
        return Collections.unmodifiableList(weapons);
    }

    // ── Gestión de balas ──────────────────────────────────────────────────

    /**
     * Añade un tipo de bala al inventario.
     * Si ya se posee, no se duplica.
     *
     * @param bulletType tipo de bala a añadir
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
     * True si el Player posee el tipo de bala indicado.
     *
     * @param bulletType tipo de bala a verificar
     * @return true si se posee
     */
    public boolean hasBullet(BulletType bulletType) {
        return bullets.contains(bulletType);
    }

    /**
     * Lista inmutable de tipos de bala poseídos.
     *
     * @return lista de tipos de bala. Nunca null.
     */
    public List<BulletType> getBullets() {
        return Collections.unmodifiableList(bullets);
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

    // ── Inicialización desde loadout ──────────────────────────────────────

    /**
     * Limpia el inventario — útil para testing o reinicios.
     */
    public void clear() {
        weapons.clear();
        bullets.clear();
    }

    /**
     * Inicializa el inventario con el contenido de un loadout.
     * Usado por PlayerAssembler para materializar la configuración inicial.
     *
     * @param loadoutWeapons armas del loadout
     * @param loadoutBullet bala inicial del loadout
     */
    public void initializeFromLoadout(List<ModifiedWeapon> loadoutWeapons, BulletType loadoutBullet) {
        clear();
        
        for (ModifiedWeapon weapon : loadoutWeapons) {
            addWeapon(weapon);
        }
        
        if (loadoutBullet != null) {
            addBullet(loadoutBullet);
        }
    }
}