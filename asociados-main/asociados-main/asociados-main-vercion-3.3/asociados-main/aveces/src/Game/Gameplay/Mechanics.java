package Game.Gameplay;
import Game.Player;
import imput.KeyBoard;
import math.Vector2D;

public class Mechanics {
    public static void updateMechanics(Player player){

        if(KeyBoard.c){
            double dirX = 0;
            double dirY = 0;
            double angle;

            if (KeyBoard.up && KeyBoard.left){
                dirY = 0;
                dirX = -1;
            }

            if (KeyBoard.up && KeyBoard.right){
            dirY = 0;
            dirX = 1;
            }

            if (dirX != 0 || dirY != 0) {
                double len = Math.sqrt(dirX * dirX + dirY * dirY);
                dirX /= len;
                dirY /= len;

                angle = Math.atan2(dirY, dirX);
                player.setBulletAngle(angle);
            }

            player.setCongelado(true);
                if(KeyBoard.up && (KeyBoard.left==false || KeyBoard.right==false)){
                    player.setBulletDirY(-45);
                    player.setBulletDirX(0);
                }else if(KeyBoard.up && (KeyBoard.left||KeyBoard.right)){
                    player.setBulletDirY(-15);
                    player.setBulletDirX(55);
                }
                if(KeyBoard.up==false && (KeyBoard.left || KeyBoard.right)){
                    player.setBulletDirY(0);
                    player.setBulletDirX(55);
                }
        }else if (KeyBoard.up){
            player.setBulletDirY(-45);
            player.setBulletDirX(0);
        }else{
            player.setCongelado(false);
            player.setBulletDirY(1);
            player.setBulletDirX(55);
        }
    }
}
