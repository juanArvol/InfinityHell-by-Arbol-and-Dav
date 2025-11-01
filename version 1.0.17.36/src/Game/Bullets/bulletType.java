package Game.Bullets;

import Game.Bullets.BulletType.*;

public class bulletType {
    private int tipo;
    private BulletComport comport;

    public bulletType(int tipo) {
        this.tipo = tipo;
        this.comport = BulletComportFactory.getComportamiento(tipo);
    }

    public int getTipo() { return tipo; }
    public double getSpeed() { return comport.getSpeed(); }
    public boolean tieneGravedad() { return comport.hasGravity(); }
    public int getDamage() { return comport.getDamage(); }
    public BulletComport getBulletClass() { return comport; }
}
