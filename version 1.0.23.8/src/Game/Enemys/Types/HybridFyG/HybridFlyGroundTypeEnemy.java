
package Game.Enemys.Types.HybridFyG;

import Game.Enemys.Enemy;
import Game.Enemys.AI.EnemyComport;
import Game.Fisics.EnemyPhysics;
import Game.Player.Player;
import GameMath.Vector2D;

import java.awt.image.BufferedImage;

public abstract class HybridFlyGroundTypeEnemy extends Enemy {

    protected boolean flyingMode;


    public HybridFlyGroundTypeEnemy(
            Vector2D position,
            BufferedImage texture,
            int hp,
            EnemyComport comport,
            Player player,
            EnemyPhysics physics
    ){

        super(
            position,
            texture,
            hp,
            comport,
            player,
            physics
        );

    }


    @Override
    protected void updateTypePhysics(){

        if(!flyingMode){

            getPhysics().applyGravity(
                    getState().isEnElSuelo()
            );

        }

    }


    public void setFlyingMode(boolean f){
        flyingMode = f;
    }

}