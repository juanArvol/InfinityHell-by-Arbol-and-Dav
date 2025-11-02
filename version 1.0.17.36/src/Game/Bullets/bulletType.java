package Game.Bullets;

import Game.Bullets.BulletType.*;

public class bulletType {
    private int numberType;
    private BulletComport comport;

    public bulletType(int tipo) {
        this.numberType = tipo;
        this.comport = BulletComportFactory.getComportamiento(tipo);
    }

    public int getNumberTipo() { return numberType; }
    public double getSpeed() { return comport.getSpeed(); }
    public boolean tieneGravedad() { return comport.hasGravity(); }
    public int getDamage() { return comport.getDamage(); }
    public BulletComport getBulletClass() { return comport; }
}
