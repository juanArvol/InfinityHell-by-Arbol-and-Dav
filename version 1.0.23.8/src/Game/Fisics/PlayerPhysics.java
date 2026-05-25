package Game.Fisics;

/**
 * Física del jugador.
 *
 * Restaura los valores originales de la versión estable:
 *   aGround=2.5, speedMaxPiso=70/135, slide=0.9/0.74
 *
 * La fricción de superficie (hielo, barro, etc.) se aplica ENCIMA de
 * estos valores base via SurfaceMaterial — no los reemplaza.
 * En superficie normal (friction=1.0, drag=1.0) el comportamiento
 * es idéntico a la versión original.
 */
public class PlayerPhysics extends Physics {

    public PlayerPhysics(double gravity) {
        super(gravity);
        mass    = 1.0;
        aGround = 2.5;
        aAir    = 1.07;
    }

    @Override
    public void moveX(double inputX, boolean onGround, boolean running) {
        setMass(1);
        setMaxSpeed(onGround);

        // Velocidades máximas según estado de correr
        speedMaxPiso = running ? 135 : 70;
        speedMaxAir  = running ? 14.5 : 10;

        // Slide base original: suelo frena normal, aire frena menos
        slide = onGround ? 0.9 : 0.74;

        // Delegar a Physics.moveX() que aplica friction/drag encima del slide
        super.moveX(inputX, onGround, running);
    }

    public void setRunning(boolean running) {
        // speedMax se actualiza en moveX() — este método solo existe
        // para que PlayerController pueda llamarlo antes de moveX si lo necesita
    }
}
