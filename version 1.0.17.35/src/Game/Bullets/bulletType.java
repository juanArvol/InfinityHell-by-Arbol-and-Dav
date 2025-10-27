package Game.Bullets;

import Game.Player;

public class bulletType {
    private int tipo;
    private double speed;
    private boolean tieneG;
    private int damage;

    public bulletType(int tipo) {
        this.tipo = tipo;
        switch (tipo) {
            case 1 -> { speed = 10; tieneG = true; damage = 10; } // bala normal
            case 2 -> { speed = 1; tieneG = true; damage = 15; }   // bala saltarina
            case 3 -> { speed = 1; tieneG = true; damage = 30; }   // bala tierra
            case 4 -> { speed = 0.1; tieneG = true; damage = 50; }   // bala meteoro
            case 5 -> { speed = 0.1; tieneG = true; damage = 50; }   // bala summon
            default -> { speed = 0; tieneG = false; damage = 0; }
        }
    }

    public int getTipo() { return tipo; }
    public double getSpeed() { return speed; }
    public boolean tieneGravedad() { return tieneG; }
    public int getDamage() { return damage; }
}