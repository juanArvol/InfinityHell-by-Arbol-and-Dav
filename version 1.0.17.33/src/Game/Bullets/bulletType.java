package Game.Bullets;

public class bulletType {
    private int tipo;
    private double speed;
    private boolean gravedad;
    private int damage;

    public bulletType(int tipo) {
        this.tipo = tipo;
        switch (tipo) {
            case 1 -> { speed = 10; gravedad = false; damage = 10; } // bala normal
            case 2 -> { speed = 6; gravedad = true; damage = 15; }   // bala saltarina
            case 3 -> { speed = 2; gravedad = true; damage = 30; }   // bala tierra
            case 4 -> { speed = 8; gravedad = true; damage = 50; }   // bala meteoro
            default -> { speed = 0; gravedad = false; damage = 0; }
        }
    }

    public int getTipo() { return tipo; }
    public double getSpeed() { return speed; }
    public boolean tieneGravedad() { return gravedad; }
    public int getDamage() { return damage; }
}