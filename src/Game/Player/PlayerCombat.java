package Game.Player;

import java.util.List;

import Entradas.MouseInput;
import Entradas.Listeners.MouseActionListener;
import Game.Bullets.Bullet;
import Game.Weapons.WeaponInventory;
import Game.Weapons.WeaponSelected;
import Game.World.Core.World;
import Game.World.Core.WorldManager;
import GameMath.Vector2D;

/**
 * Combate del jugador.
 *
 * ─── REFACTOR (Entradas v2) ───────────────────────────────────────────────────
 *
 *  · Eliminados MouseInput.leftPressed y MouseInput.isLeftClicked() (campos/
 *    métodos estáticos de la API anterior). El estado continuo ahora se
 *    consulta con MouseInput.getButtonState("leftPressed") y los clicks
 *    de edge se reciben via MouseActionListener.
 *
 *  · PlayerCombat implementa MouseActionListener para reaccionar al edge
 *    "leftClick" (disparo puntual) y gestiona isHoldingFire internamente
 *    para el disparo continuo.
 *
 *  · La lógica de recarga se mantiene igual: PlayerState.isReloading() se
 *    activa desde fuera (p.ej. KeyActionListener con edge "reload").
 */
public class PlayerCombat implements MouseActionListener {

    private final Player player;
    private final PlayerState state;
    private final WeaponInventory inventory;

    /** Indica si se recibió un click de edge este frame (disparo puntual). */
    private boolean clickFired = false;

    public PlayerCombat(Player player, PlayerState state) {
        this.player    = player;
        this.state     = state;
        this.inventory = new WeaponInventory();

        inventory.addWeapon(
            new WeaponSelected(
                new Game.Weapons.WeaponType.WeaponClass.WeaponEscopeta(),
                Game.Bullets.BulletType.SPRINGBULLET
            )
        );
    }

    public WeaponInventory getInventory() {
        return inventory;
    }

    // ─── MouseActionListener ──────────────────────────────────────────────────

    @Override
    public void onMouseAction(String action, float virtualX, float virtualY) {
        if ("leftClick".equals(action)) {
            clickFired = true;
        }
    }

    // ─── Update (GameLoop thread) ─────────────────────────────────────────────

    public void update() {
        WeaponSelected currentWeapon = inventory.getCurrentWeapon();
        if (currentWeapon == null) return;

        if (state.isReloading()) {
            currentWeapon.reload();
        }

        // Estado continuo: botón izquierdo mantenido
        boolean holding = MouseInput.getButtonState("leftPressed");

        Vector2D aim = state.getAimDirection();

        List<Bullet> newBullets = currentWeapon.handleInput(
            holding,
            clickFired,
            player.getPosition().getX(),
            player.getPosition().getY(),
            state.isDer(),
            aim
        );

        // Consumir el edge click después de pasarlo al arma
        clickFired = false;

        World world = WorldManager.getInstance().getCurrentWorld();
        for (Bullet b : newBullets) {
            world.add(b);
        }

        currentWeapon.update();
    }

    public void nextWeapon()     { inventory.nextWeapon();     }
    public void previousWeapon() { inventory.previousWeapon(); }
}
