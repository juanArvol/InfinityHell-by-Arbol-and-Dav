package Game.Player;

import java.util.List;

import Entradas.MouseInput;
import Game.Bullets.Bullet;
import Game.Weapons.WeaponInventory;
import Game.Weapons.WeaponSelected;
import Game.World.Core.World;
import Game.World.Core.WorldManager;
import GameMath.Vector2D;

public class PlayerCombat {

    private final Player player;
    private final PlayerState state;
    private final WeaponInventory inventory;

    public PlayerCombat(Player player, PlayerState state) {
        this.player = player;
        this.state = state;

        this.inventory = new WeaponInventory();

        inventory.addWeapon(
            new WeaponSelected(
                new Game.Weapons.WeaponType.WeaponClass.WeaponEscopeta(),
                Game.Bullets.BulletType.SPRINGBULLET
            )
        );
    }

    public WeaponInventory getInventory(){
        return inventory;
    }

    public void update() {

        WeaponSelected currentWeapon = inventory.getCurrentWeapon();
        if (currentWeapon == null) return;

        if (state.isReloading()){
            currentWeapon.reload();
        }

        Vector2D aim = state.getAimDirection();

        List<Bullet> newBullets =
            currentWeapon.handleInput(
                MouseInput.leftPressed,
                MouseInput.isLeftClicked(),
                player.getPosition().getX(),
                player.getPosition().getY(),
                state.isDer(),
                aim
            );

        World world = WorldManager
                .getInstance()
                .getCurrentWorld();

        for (Bullet b : newBullets) {
            world.add(b);
        }

        currentWeapon.update();
    }

    public void nextWeapon() {
        inventory.nextWeapon();
    }

    public void previousWeapon() {
        inventory.previousWeapon();
    }
}