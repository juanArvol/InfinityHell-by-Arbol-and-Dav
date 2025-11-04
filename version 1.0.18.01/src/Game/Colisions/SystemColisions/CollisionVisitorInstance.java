package Game.Colisions.SystemColisions;

import Game.Ambiente;
import Game.EnimyNormal;
import Game.Player;
import Game.Bullets.Bullet;

public class CollisionVisitorInstance implements CollisionVisitor {
    private final Collidable target; // el que recibe la colisión

    public CollisionVisitorInstance(Collidable target) {
        this.target = target;
    }

    @Override
    public void visit(Player player) {
        target.onCollisionWith(player);
    }

    @Override
    public void visit(EnimyNormal enemy) {
        target.onCollisionWith(enemy);
    }

    @Override
    public void visit(Ambiente ambiente) {
        target.onCollisionWith(ambiente);
    }

    @Override
    public void visit(Bullet bullet) {
        target.onCollisionWith(bullet);
    }
}