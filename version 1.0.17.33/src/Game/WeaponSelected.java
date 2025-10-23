package Game;

import java.util.ArrayList;

public class WeaponSelected {
    private int selectedWeapon;
    private Player owner;
    private Weapon weapon;
    private ArrayList<EnimyNormal> enemies;
    public WeaponSelected(Player owner, int selectedWeapon, int typeBullet){
        this.selectedWeapon = selectedWeapon;
        this.weapon = new Weapon(owner, selectedWeapon, typeBullet, owner.getEnemies());
    }
    public void tryShoot(double x, double y, boolean mirandoDerecha){
        weapon.tryShoot(x, y, mirandoDerecha);
    }

    public void resetBurst(){
        weapon.resetBurst();
    }

    public void update(){
        weapon.update();
    }
}
