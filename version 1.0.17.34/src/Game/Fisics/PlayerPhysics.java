package Game.Fisics;

import Game.Player;

public class PlayerPhysics extends Physics {
    
    @Override
    public void moveX(double inputX, boolean onGround, boolean direction, boolean running) {
        setMaxSpeed(onGround);
        slide= onGround ? 0.9 : 0.76;
        speedMaxPiso= running ? 135 : 70;
        speedMaxAir= running ? 14.5 : 10;
        aGround=2.5;
        aAir=1.07;

        super.moveX(inputX, onGround, direction, running);
        
        if(!onGround){  // Aire: aplica freno natural constante
            if(inputX==0){
                vx *= slide/bonus; //usando algo entero en lugar de decimal 0.algo: todo negativo vortice central, todo positivo vortice hacia afuera xd
            }
            double vxs;
            if(Math.abs(vx) >speedMaxAir) {
                vxs=vx/speedMaxAir;
                vSetX(vxs);
                System.out.println("la velocidad setteada fue "+vxs);
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
