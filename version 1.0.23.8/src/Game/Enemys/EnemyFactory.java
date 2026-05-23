package Game.Enemys;

import Game.Enemys.Types.EnemyType;
import Game.Enemys.Types.Flying.Class.EnemyFlying;
import Game.Enemys.Types.Ground.Class.EnemyNormal;

import Game.Fisics.EnemyPhysics;

import Game.Player.Player;
import GameMath.Vector2D;
import Graficos.Enemys.EnemyAssets;

import java.awt.image.BufferedImage;

public class EnemyFactory {

    public static Enemy createEnemy(EnemyType type, Vector2D position, Player player){
        EnemyPhysics physics =
                createPhysics(type);
        BufferedImage texture =
                getRandomTexture(type);
                
        switch(type){
            case NORMAL_GROUND:
                return new EnemyNormal(position, texture, player, physics);
            case FLYING:
                return new EnemyFlying(position, texture, player, physics);
        }
        throw new RuntimeException(
                "EnemyType no soportado: "+type
        );
    }
    private static EnemyPhysics createPhysics(
            EnemyType type
    ){

        EnemyPhysicsConfig config;

        switch(type){
            case NORMAL_GROUND:
                config =
                    new EnemyPhysicsConfig(
                        0.4, //gravity
                        1,   //mass
                        0.3, //aAir
                        0.5, //aGround
                        2,   //speedAir
                        3,   //speedGround
                        0.8  //slide
                    );
                break;
            case FLYING:
                config =
                    new EnemyPhysicsConfig(
                        0,   //gravity
                        1,
                        0.2,
                        0.2,
                        2,
                        2,
                        0.9
                    );
                break;
            default:
                throw new RuntimeException(
                        "Physics no definida"
                );
        }
        return new EnemyPhysics(config);
    }
    
    private static BufferedImage getRandomTexture(EnemyType type){
        BufferedImage[] frames;
        switch(type){
            case NORMAL_GROUND:
                frames = EnemyAssets.Enormal.getFrames();
                break;
            case FLYING:
                frames = EnemyAssets.Eflying.getFrames();
                break;
            default:
                throw new RuntimeException("Textura no definida");
        }
        return frames[(int)(Math.random()*frames.length)];
    }
}