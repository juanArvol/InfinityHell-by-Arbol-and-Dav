package Game.Weapons.WeaponType;

public class weaponType {
    private int numberType;
    private weaponComport comport;

    public weaponType(int numberType) {
        this.numberType = numberType;
        this.comport = weaponComportFactory.getComportamiento(numberType);
    }

    public weaponComport getWeaponComport() { return comport; }
    public int getNumberTipo() { return numberType; }
    
}
