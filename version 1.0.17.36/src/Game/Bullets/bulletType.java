package Game.Bullets;

import Game.Bullets.BulletType.*;

public class bulletType {
    private int tipo;
    private BulletComport behavior;

    public bulletType(int tipo) {
        this.tipo = tipo;
        this.behavior = BulletComportFactory.getComportamiento(tipo);
    }

    public int getTipo() { return tipo; }
    public double getSpeed() { return behavior.getSpeed(); }
    public boolean tieneGravedad() { return behavior.hasGravity(); }
    public int getDamage() { return behavior.getDamage(); }
    public BulletComport getBehavior() { return behavior; }
}
