package Game.Fisics;

import Game.Player;

public class PlayerPhysics extends Physics {
    
    @Override
    public void moveX(double inputX, boolean onGround, boolean direction, boolean running) {
        setMaxSpeed(onGround);
        slide= onGround ? 0.9 : 0.76;
        speedMaxPiso= running ? 135 : 70;
        speedMaxAir= running ? 12.5 : 10;
        aGround=2.5;
        aAir=1.07;

        super.moveX(inputX, onGround, direction, running);
        
        if(!onGround){  // Aire: aplica freno natural constante
            if(inputX==0){
                vx *= slide/bonus; //usando algo entero en lugar de decimal 0.algo: todo negativo vortice central, todo positivo vortice hacia afuera xd
            }
            if(Math.abs(vx) >speedMaxAir) {
                vSetX(vx/speedMaxAir);
                System.out.println("la velocidad setteada fue "+vx);
            }
            vSetX(vx);
        }
        if(onGround && inputX==0){      // Suelo: aplica freno natural si no hay input
            vx *= slide;
            if (Math.abs(vx) < 0.05) vx = 0;
            vSetX(vx);
        }
    }   
}
