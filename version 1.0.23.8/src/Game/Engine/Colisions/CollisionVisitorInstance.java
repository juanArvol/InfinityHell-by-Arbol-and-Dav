package Game.Engine.Colisions;

import Game.Enemys.Enemy;
import Game.Engine.GameObjects;
import Game.Player.Player;
import Game.World.WorldObjects.BlockWorld;
import Game.World.WorldObjects.Obstacle;
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
    public void visit(Enemy enemy) {
        target.onCollisionWith(enemy);
    }

    @Override
    public void visit(BlockWorld block) {
        target.onCollisionWith(block);
    }

    @Override
    public void visit(Bullet bullet) {
        target.onCollisionWith(bullet);
    }

    @Override
    public void visit(Obstacle obstacle) {
        target.onCollisionWith(obstacle);
    }

    @Override
    public void visit(GameObjects other) {
        // Colisión genérica si no es uno de los tipos específicos
        target.onCollisionWith(other);
    }
}