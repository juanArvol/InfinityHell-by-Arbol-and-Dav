
package Game.Colisions.SystemColisions;

import Game.Ambiente;
import Game.Bullets.Bullet;
import Game.EnimyNormal;
import Game.Player;

public interface CollisionVisitor {
    void visit(Player player);
    void visit(EnimyNormal enemy);
    void visit(Ambiente ambiente);
    void visit(Bullet bullet);
}