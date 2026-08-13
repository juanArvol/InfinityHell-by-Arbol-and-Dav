package Game.Player;

import Game.Items.Types.Bullets.Definition.BulletType;
import Game.Items.Types.Weapons.ModifiedWeapon;
import Game.Items.Types.Weapons.WeaponType.WeaponType;

/**
 * Estado mutable de la run del Player — coordina inventario y equipamiento.
 *
 * ── HRFC — Player Architecture Consolidation ──────────────────────────────
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 *
 * PlayerRuntime representa el estado mutable que se adquiere durante la run.
 * Es el propietario conceptual de:
 *
 *   • Progreso adquirido durante la run
 *   • Equipamiento adquirido
 *   • Estado runtime de gameplay
 *   • Coordinación entre inventario y equipamiento
 *
 * Separa claramente:
 *
 *   PlayerLoadout    → configuración inicial (inmutable)
 *   PlayerRuntime    → estado actual de la run (mutable)
 *   PlayerInventory  → almacenamiento de posesiones (fachada)
 *   WeaponInventory  → almacenamiento de armas (especializado)
 *   BulletInventory  → almacenamiento de balas (especializado)
 *   AmuletInventory  → almacenamiento de amuletos (especializado)
 *   PlayerCombat     → ejecución de combate
 *
 * ── ARQUITECTURA ──────────────────────────────────────────────────────────
 *
 *   PlayerRuntime
 *        │
 *        ├── PlayerInventory (fachada)
 *        │      ├── WeaponInventory (almacenamiento de armas)
 *        │      ├── BulletInventory (almacenamiento de balas)
 *        │      └── AmuletInventory (almacenamiento de amuletos)
 *        │
 *        └── Equipment state
 *               ├── currentWeaponIndex (índice en WeaponInventory)
 *               └── currentBulletIndex (índice en BulletInventory)
 *
 * ── SELECCIÓN INDEPENDIENTE ───────────────────────────────────────────────
 *
 * Las armas y balas se seleccionan independientemente:
 *
 *   runtime.selectWeapon(WeaponType.ESCOPETA)  // no cambia bala
 *   runtime.selectBullet(BulletType.BULLETJUMP) // no cambia arma
 *
 * ── OWNERSHIP VS EQUIPMENT ───────────────────────────────────────────────
 *
 * PlayerInventory responde: "¿Qué posee el Player?"
 * PlayerRuntime responde: "¿Qué está equipado actualmente?"
 *
 * La relación correcta es:
 *
 *   PlayerInventory.weapons()
 *        │
 *        └── owns Weapon
 *                  ▲
 *                  │ equipped
 *                  │
 *            PlayerRuntime
 *
 * ── INVARIANTES ───────────────────────────────────────────────────────────
 *
 *   • Una bala seleccionada permanece activa hasta que se cambie explícitamente
 *   • Adquirir una nueva bala no elimina automáticamente otras
 *   • Las balas son infinitas (representan tipos/equipamientos disponibles)
 *   • El arma y bala activas son independientes entre sí
 *   • Solo se puede equipar lo que se posee
 */
public final class PlayerRuntime {

    /** Inventario del Player — almacena armas, balas y amuletos. */
    private final PlayerInventory inventory;

    /** Arma actualmente seleccionada (index en inventory.weapons()). */
    private int currentWeaponIndex = 0;

    /** Bala actualmente seleccionada (index en inventory.bullets()). */
    private int currentBulletIndex = 0;

    /**
     * @param inventory inventario del Player para gestionar armas, balas y amuletos
     */
    public PlayerRuntime(PlayerInventory inventory) {
        if (inventory == null) {
            throw new IllegalArgumentException("inventory es requerido");
        }
        this.inventory = inventory;
    }

    // ── Acceso al inventario ──────────────────────────────────────────────

    /**
     * Inventario del Player — punto de acceso a armas, balas y amuletos.
     */
    public PlayerInventory getInventory() {
        return inventory;
    }

    // ── Gestión de armas ──────────────────────────────────────────────────

    /**
     * Arma actualmente seleccionada.
     * @return arma activa, o null si no hay armas en el inventario
     */
    public ModifiedWeapon getCurrentWeapon() {
        if (inventory.hasNoWeapons() || currentWeaponIndex >= inventory.getWeaponCount()) {
            return null;
        }
        return inventory.weapons().getWeapon(currentWeaponIndex);
    }

    /**
     * Selecciona un arma por tipo.
     * No afecta la bala actualmente seleccionada.
     *
     * ── HRFC — Weapon Type Runtime Identity ──────────────────────────────
     *
     * Implementado usando la identidad tipada de ModifiedWeapon.getWeaponType().
     * Busca el arma que coincide con el WeaponType solicitado y la selecciona.
     *
     * @param weaponType tipo de arma a seleccionar
     * @return true si se encontró y seleccionó el arma, false si no se posee
     */
    public boolean selectWeapon(WeaponType weaponType) {
        if (weaponType == null) return false;

        for (int i = 0; i < inventory.getWeaponCount(); i++) {
            ModifiedWeapon weapon = inventory.weapons().getWeapon(i);
            if (weapon != null && weapon.getWeaponType() == weaponType) {
                currentWeaponIndex = i;
                return true;
            }
        }
        return false;
    }

    /**
     * Avanza al siguiente arma en el inventario (ciclo circular).
     * No afecta la bala seleccionada.
     */
    public void nextWeapon() {
        int weaponCount = inventory.getWeaponCount();
        if (weaponCount <= 1) return;
        currentWeaponIndex = (currentWeaponIndex + 1) % weaponCount;
    }

    /**
     * Retrocede al arma anterior en el inventario (ciclo circular).
     * No afecta la bala seleccionada.
     */
    public void previousWeapon() {
        int weaponCount = inventory.getWeaponCount();
        if (weaponCount <= 1) return;
        currentWeaponIndex = (currentWeaponIndex - 1 + weaponCount) % weaponCount;
    }

    // ── Gestión de balas ──────────────────────────────────────────────────

    /**
     * Bala actualmente seleccionada.
     * @return tipo de bala activa, o null si no hay balas en el inventario
     */
    public BulletType getCurrentBullet() {
        if (inventory.hasNoBullets() || currentBulletIndex >= inventory.getBulletCount()) {
            return null;
        }
        return inventory.bullets().getBullet(currentBulletIndex);
    }

    /**
     * Selecciona una bala por tipo.
     * No afecta el arma actualmente seleccionada.
     *
     * @param bulletType tipo de bala a seleccionar
     * @return true si se encontró y seleccionó la bala, false si no se posee
     */
    public boolean selectBullet(BulletType bulletType) {
        int index = inventory.bullets().indexOf(bulletType);
        if (index >= 0) {
            currentBulletIndex = index;
            return true;
        }
        return false;
    }

    /**
     * Avanza a la siguiente bala en el inventario (ciclo circular).
     * No afecta el arma seleccionada.
     */
    public void nextBullet() {
        int bulletCount = inventory.getBulletCount();
        if (bulletCount <= 1) return;
        currentBulletIndex = (currentBulletIndex + 1) % bulletCount;
    }

    /**
     * Retrocede a la bala anterior en el inventario (ciclo circular).
     * No afecta el arma seleccionada.
     */
    public void previousBullet() {
        int bulletCount = inventory.getBulletCount();
        if (bulletCount <= 1) return;
        currentBulletIndex = (currentBulletIndex - 1 + bulletCount) % bulletCount;
    }

    // ── Adquisiciones runtime ─────────────────────────────────────────────

    /**
     * Añade un arma al inventario durante la run.
     * El arma se añade al final del inventario pero no se selecciona automáticamente.
     *
     * ── FLUJO DE ADQUISICIÓN CORRECTO ────────────────────────────────────
     *
     * Gameplay / Reward / Pickup
     *          │
     *          ▼
     *   player.getRuntime().acquireWeapon(weapon)
     *          │
     *          ▼
     *   PlayerRuntime → PlayerInventory → WeaponInventory
     *          │
     *          ▼
     *   Weapon ownership updated
     *
     * ── SELECCIÓN POSTERIOR ──────────────────────────────────────────────
     *
     * El jugador puede seleccionar el arma después con:
     *   - player.getRuntime().selectWeapon(weaponType)
     *   - player.getRuntime().nextWeapon() / previousWeapon()
     *   - Input handling en PlayerCombat
     *
     * @param weapon arma a añadir
     * @return true si se añadió (nueva adquisición), false si ya se poseía
     */
    public boolean acquireWeapon(ModifiedWeapon weapon) {
        return inventory.addWeapon(weapon);
    }

    /**
     * Añade una bala al inventario durante la run.
     * La bala se añade al final del inventario pero no se selecciona automáticamente.
     *
     * ── FLUJO DE ADQUISICIÓN CORRECTO ────────────────────────────────────
     *
     * Gameplay / Reward / Pickup
     *          │
     *          ▼
     *   player.getRuntime().acquireBullet(bulletType)
     *          │
     *          ▼
     *   PlayerRuntime → PlayerInventory → BulletInventory
     *          │
     *          ▼
     *   Bullet ownership updated
     *
     * ── SELECCIÓN POSTERIOR ──────────────────────────────────────────────
     *
     * El jugador puede seleccionar la bala después con:
     *   - player.getRuntime().selectBullet(bulletType)
     *   - player.getRuntime().nextBullet() / previousBullet()
     *   - Input handling en PlayerCombat
     *
     * @param bulletType tipo de bala a añadir
     * @return true si se añadió (nueva adquisición), false si ya se poseía
     */
    public boolean acquireBullet(BulletType bulletType) {
        return inventory.addBullet(bulletType);
    }

    /**
     * Añade un amuleto al inventario durante la run.
     *
     * ── FLUJO DE ADQUISICIÓN CORRECTO ────────────────────────────────────
     *
     * Gameplay / Reward / Pickup
     *          │
     *          ▼
     *   player.getRuntime().acquireAmulet(amuletId)
     *          │
     *          ▼
     *   PlayerRuntime → PlayerInventory → AmuletInventory
     *          │
     *          ▼
     *   Amulet ownership updated
     *
     * Los amuletos se aplican automáticamente durante el disparo via
     * AmuletRegistry.applyAll() llamado desde ModifiedWeapon.
     *
     * @param amuletId ID del amuleto a añadir
     * @return true si se añadió (nueva adquisición), false si ya se poseía
     */
    public boolean acquireAmulet(String amuletId) {
        return inventory.addAmulet(amuletId);
    }

    // ── Consultas de estado ───────────────────────────────────────────────

    /**
     * True si el Player posee el tipo de arma indicado.
     */
    public boolean hasWeapon(WeaponType weaponType) {
        return inventory.hasWeapon(weaponType);
    }

    /**
     * True si el Player posee el tipo de bala indicado.
     */
    public boolean hasBullet(BulletType bulletType) {
        return inventory.hasBullet(bulletType);
    }

    /**
     * Número total de armas en el inventario.
     */
    public int getWeaponCount() {
        return inventory.getWeaponCount();
    }

    /**
     * Número total de balas en el inventario.
     */
    public int getBulletCount() {
        return inventory.getBulletCount();
    }

    // ── Update ────────────────────────────────────────────────────────────

    /**
     * Actualiza el estado runtime.
     * Llamar una vez por frame desde Player.update().
     */
    public void update() {
        // Validar índices tras posibles cambios en el inventario
        validateWeaponIndex();
        validateBulletIndex();
    }

    /**
     * Valida que currentWeaponIndex esté dentro del rango del inventario.
     * Si el arma actual fue removida, selecciona la primera disponible.
     */
    private void validateWeaponIndex() {
        int weaponCount = inventory.getWeaponCount();
        if (weaponCount == 0) {
            currentWeaponIndex = 0;
        } else if (currentWeaponIndex >= weaponCount) {
            currentWeaponIndex = weaponCount - 1;
        }
    }

    /**
     * Valida que currentBulletIndex esté dentro del rango del inventario.
     * Si la bala actual fue removida, selecciona la primera disponible.
     */
    private void validateBulletIndex() {
        int bulletCount = inventory.getBulletCount();
        if (bulletCount == 0) {
            currentBulletIndex = 0;
        } else if (currentBulletIndex >= bulletCount) {
            currentBulletIndex = bulletCount - 1;
        }
    }
}