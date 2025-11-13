package Game.Bullets;

import Game.Bullets.BulletCharger.*;

public class bulletType {
    private int numberType;
    private BulletComport comport;

    public bulletType(int tipo) {
        this.numberType = tipo;
        this.comport = BulletComportFactory.getComportamiento(tipo);
    }
    
    public BulletComport getBulletClass() { return comport; }
    public int getNumberTipo() { return numberType; }
    public double getSpeedMaxAir() { return comport.getMaxSpeedAir(); }
    public double getSpeedMaxPiso() { return comport.getMaxSpeedPiso(); }
    public double getSpeed() { return comport.getBspeed(); }
    public double getAcceleration(boolean selection){ return comport.getAcceleration(selection); }
    public boolean tieneGravedad() { return comport.hasGravity(); }
    public int getDamage() { return comport.getDamage(); }
}
