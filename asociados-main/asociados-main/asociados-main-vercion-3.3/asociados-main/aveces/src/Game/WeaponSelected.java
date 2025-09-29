package Game;
import java.util.ArrayList;

public class WeaponSelected {
    private int selectedWeapon;
    private Weapon weapon;
    
    public WeaponSelected(int selectedWeapon, int typeBullet){
        this.selectedWeapon = selectedWeapon;
        this.weapon = new Weapon(selectedWeapon, typeBullet);
    }

    public void setBulletA(double bulletA){
        weapon.setBulletA(bulletA);
    }
    public void setBulletDirY(double bulletY){
        weapon.setBulletDirY(bulletY);
    }

    public void setBulletDirX(double bulletX){
        weapon.setBulletDirX(bulletX);
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

    public ArrayList<Bullet> getBullets(){
        return weapon.getBullets();
    }
}
