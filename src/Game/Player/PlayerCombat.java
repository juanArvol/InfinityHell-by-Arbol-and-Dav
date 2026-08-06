package Game.Player;

import Game.Engine.Events.GameEventBus;
import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Items.Types.Bullets.Bullet;
import Game.Items.Types.Weapons.ModifiedWeapon;
import Game.Items.Types.Weapons.WeaponEvents;
import Game.Items.Types.Weapons.WeaponInventory;
import Inputs.Listeners.MouseActionListener;
import Inputs.MouseInput;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Combate del jugador — responsable de conectar input, arma y mundo.
 *
 * ── HRFC — Weapon & Projectile System ────────────────────────────────────
 *
 * PlayerCombat ahora usa {@link ModifiedWeapon} directamente como tipo
 * de arma, eliminando la capa de indirección de WeaponSelected + Weapon
 * que no añadía valor. El pipeline de disparo completo (FireMode →
 * amuletos → BulletBehavior compuesto → proyectiles) vive en ModifiedWeapon.
 *
 * ── RESPONSABILIDADES ─────────────────────────────────────────────────────
 *
 *   1. Leer input de mouse (click puntual, botón mantenido).
 *   2. Gestionar la recarga manual.
 *   3. Delegar el disparo a la ModifiedWeapon activa.
 *   4. Pasar los proyectiles resultantes al bulletSpawner inyectado.
 *   5. Emitir eventos de ciclo de vida del arma (cargador vacío, recarga).
 *
 * ── LO QUE NO HACE ────────────────────────────────────────────────────────
 *
 *   - No conoce World, WorldManager ni cómo añadir objetos al mundo.
 *   - No conoce Player directamente (recibe un Supplier<Vector2D> para posición).
 *   - No construye armas (eso lo hace Player o un sistema de loadout).
 *
 * ── DEPENDENCY INJECTION ─────────────────────────────────────────────────
 *
 *   positionSupplier → proveedor de posición del portador. Lazy.
 *   bulletSpawner    → callback que añade proyectiles al mundo.
 *
 *   Esto hace PlayerCombat testeable sin ningún sistema de mundo real.
 */
public class PlayerCombat implements MouseActionListener {

    private final PlayerState        state;
    private final WeaponInventory    inventory;
    private final Supplier<Vector2D> positionSupplier;
    private final Consumer<Bullet>   bulletSpawner;

    /** Edge-click registrado este frame (disparo puntual en SemiAuto/Burst). */
    private boolean clickFired = false;

    /**
     * @param state            estado del jugador (congelado, apuntado, recargando)
     * @param positionSupplier proveedor de posición actual del portador
     * @param bulletSpawner    callback para añadir proyectiles al mundo
     */
    public PlayerCombat(
            PlayerState state,
            Supplier<Vector2D> positionSupplier,
            Consumer<Bullet> bulletSpawner
    ) {
        this.state            = state;
        this.positionSupplier = positionSupplier;
        this.bulletSpawner    = bulletSpawner;
        this.inventory        = new WeaponInventory();
    }

    // ── Loadout ────────────────────────────────────────────────────────────

    /**
     * Añade un arma al inventario del jugador.
     *
     * Llamar desde Player o desde un sistema de loadout externo.
     * Si es la primera arma, pasa a ser el arma activa automáticamente.
     *
     * @param weapon arma ya construida con su WeaponComport, BulletType y amulets
     */
    public void addWeapon(ModifiedWeapon weapon) {
        inventory.addWeapon(weapon);
    }

    public WeaponInventory getInventory() {
        return inventory;
    }

    // ── MouseActionListener ────────────────────────────────────────────────

    @Override
    public void onMouseAction(String action, float virtualX, float virtualY) {
        if ("leftClick".equals(action)) {
            clickFired = true;
        }
    }

    // ── Update ─────────────────────────────────────────────────────────────

    public void update() {
        if (state.isCongelado()) return;

        ModifiedWeapon currentWeapon = inventory.getCurrentWeapon();
        if (currentWeapon == null) return;

        // ── Recarga manual ────────────────────────────────────────────────
        boolean reloadKeyPressed = Inputs.KeyBoard.getState("reload");
        if (reloadKeyPressed && !state.isReloading() && !currentWeapon.isFullyLoaded()) {
            state.setReloading(true);
            currentWeapon.reload();

            // Evento de inicio de recarga (suscriptores opcionales: UI, audio)
            if (GameEventBus.GLOBAL.hasListeners(WeaponEvents.OnReloadStart.class)) {
                GameEventBus.GLOBAL.post(new WeaponEvents.OnReloadStart(
                        currentWeapon, currentWeapon.getComport().getStats().getCooldown()));
            }
        }

        // ── Sincronizar estado de recarga ─────────────────────────────────
        if (state.isReloading()) {
            boolean wasReloading = currentWeapon.isReloading();
            if (!wasReloading) {
                // La recarga se completó este frame
                state.setReloading(false);
                if (GameEventBus.GLOBAL.hasListeners(WeaponEvents.OnReloadComplete.class)) {
                    GameEventBus.GLOBAL.post(new WeaponEvents.OnReloadComplete(currentWeapon));
                }
            }
        }

        // ── Cargador vacío ────────────────────────────────────────────────
        if (clickFired && currentWeapon.getCurrentAmmo() <= 0 && !currentWeapon.isReloading()) {
            if (GameEventBus.GLOBAL.hasListeners(WeaponEvents.OnEmptyMagazine.class)) {
                GameEventBus.GLOBAL.post(new WeaponEvents.OnEmptyMagazine(currentWeapon));
            }
        }

        // ── Disparo ───────────────────────────────────────────────────────
        boolean holding  = MouseInput.getButtonState("leftPressed");
        Vector2D pos     = positionSupplier.get();
        Vector2D aim     = state.getAimDirection();

        List<Bullet> newBullets = currentWeapon.handleInput(
                holding,
                clickFired,
                pos.getX(),
                pos.getY(),
                state.isDer(),
                aim
        );

        clickFired = false;

        // Pasar proyectiles al mundo via el spawner inyectado
        for (Bullet b : newBullets) {
            bulletSpawner.accept(b);
        }

        // Actualizar timers del arma (cooldown, recarga interna)
        currentWeapon.update();
    }

    // ── Cambio de arma ────────────────────────────────────────────────────

    public void nextWeapon()     { inventory.nextWeapon();     }
    public void previousWeapon() { inventory.previousWeapon(); }
}
