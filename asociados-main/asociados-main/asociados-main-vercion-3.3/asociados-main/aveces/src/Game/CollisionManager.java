package Game;


import java.awt.Rectangle;
import java.util.List;

import Game.GameObjects;
public class CollisionManager {
    
    public static void checkColicion(List<GameObjects> objects){
        for (int i = 0; i < objects.size(); i++) {
            for (int j = i; j <objects.size(); j++) {
                GameObjects a = objects.get(i);
                GameObjects b = objects.get(j);
                if (a.getBounds().intersects(b.getBounds()) && a != b) {
                    a.onCollision(b);
                    b.onCollision(a);
                    
                }
                
            }
            
        }

    }
}
