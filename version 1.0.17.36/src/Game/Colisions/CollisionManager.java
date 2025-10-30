package Game.Colisions;

import Game.GameObjects;
import java.util.List;

public class CollisionManager {
    
    public static void checkColicion(List<GameObjects> objects){
        for (int i = 0; i < objects.size(); i++) {
            for (int j = i + 1; j < objects.size(); j++) {
                    GameObjects a = objects.get(i);
                    GameObjects b = objects.get(j);
                if (a.getBounds().intersects(b.getBounds())) {
                    a.acceptCollision(b);
                    b.acceptCollision(a);
                }
            }
        }
    }
    public static GameObjects findCollision(GameObjects obj, List<GameObjects> all) {
        for (GameObjects other : all) {
            if (obj == other) continue;
            if (obj.getBounds().intersects(other.getBounds())) {
                return other;
            }
        }
        return null;
    }
}
