package Game.Fisics;

/**
 * Fisica del jugador.
 *
 * FIX BUG-11: inputX ahora llega con signo (-1 izquierda, +1 derecha, 0 quieto).
 * La direccion visual (isDer) sigue llegando por el parametro direction,
 * pero ya no se usa para calcular el signo de la velocidad aqui:
 * inputX ya contiene esa informacion directamente.
 *
 * Esto elimina la ambiguedad de "inputX=1 pero dir=-1" que habia antes.
 */
public class PlayerPhysics extends Physics {

    public PlayerPhysics(double gravity) {
        super(gravity);
    }

    @Override
    public void moveX(double inputX, boolean onGround, boolean running) {
        setMass(1);
        setMaxSpeed(onGround);

        // Constantes de movimiento
        slide        = onGround ? 0.9  : 0.74;
        speedMaxPiso = running  ? 135  : 70;
        speedMaxAir  = running  ? 14.5 : 10;
        aGround      = 2.5;
        aAir         = 1.07;

        // FIX BUG-11: inputX ya viene con signo desde PlayerController.
        // Physics.moveX() internamente hace: dir = direction ? 1 : -1
        // y luego vx += (inputX * dir) * accel.
        // Con inputX con signo y dir=+1 siempre, el resultado es correcto.
        // Pasamos direction=true (dir=1) y dejamos que inputX maneje el signo.
        super.moveX(inputX, onGround, running);

        if (!onGround) {
            // Aire: aplica freno natural constante
            if (inputX == 0) {
                vx *= slide / bonus;
            }
            if (Math.abs(vx) > speedMaxAir) {
                vSetX(vx / speedMaxAir);
            }
            vSetX(vx);
        }

        if (onGround && inputX == 0) {
            // Suelo: aplica freno natural si no hay input
            vx *= slide;
            if (Math.abs(vx) < 0.05) vx = 0;
            vSetX(vx);
        }

        showInfo(false);
    }
}
