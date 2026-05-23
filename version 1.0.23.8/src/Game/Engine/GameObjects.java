package Game.Engine;

import Game.Bullets.Bullet;
import Game.Enemys.Enemy;
import Game.Engine.Colisions.Collidable;
import Game.Engine.Colisions.CollisionVisitorInstance;
import Game.Engine.Events.CollisionListener;
import Game.Player.Player;
import Game.World.WorldObjects.BlockWorld;
import Game.World.WorldObjects.Obstacle;

import java.util.*;

public class GameObjects implements Collidable {

    private final Transform transform = new Transform();
    private final List<Component> components = new ArrayList<>();

    private final Set<GameObjects> currentCollisions = new HashSet<>();
    private final Set<GameObjects> currentTriggers   = new HashSet<>();

    public Transform getTransform() {
        return transform;
    }

    public void addComponent(Component c) {
        c.setGameObject(this);
        components.add(c);
        c.start();
    }

    public void update() {
        for (Component c : components) {
            c.update();
        }
    }

    public <T> T getComponent(Class<T> type) {
        for (Component c : components) {
            if (type.isInstance(c))
                return type.cast(c);
        }
        return null;
    }

    public List<Component> getComponents() {
        return components;
    }

    // =============================
    // COLLISION STATE HANDLING
    // =============================

    public void handleCollision(GameObjects other, boolean isTrigger) {

        Set<GameObjects> set = isTrigger ? currentTriggers : currentCollisions;

        if (!set.contains(other)) {

            set.add(other);
            this.acceptCollision(other);

            for (Component c : components) {
                if (c instanceof CollisionListener listener) {
                    if (isTrigger)
                        listener.onTriggerEnter(other);
                    else
                        listener.onCollisionEnter(other);
                }
            }

        } else {

            for (Component c : components) {
                if (c instanceof CollisionListener listener) {
                    if (isTrigger)
                        listener.onTriggerStay(other);
                    else
                        listener.onCollisionStay(other);
                }
            }
        }
    }

    public void resolveExits(Set<GameObjects> detected, boolean isTrigger) {

        Set<GameObjects> set = isTrigger ? currentTriggers : currentCollisions;

        Iterator<GameObjects> it = set.iterator();

        while (it.hasNext()) {

            GameObjects obj = it.next();

            if (!detected.contains(obj)) {

                for (Component c : components) {
                    if (c instanceof CollisionListener listener) {
                        if (isTrigger)
                            listener.onTriggerExit(obj);
                        else
                            listener.onCollisionExit(obj);
                    }
                }

                it.remove();
            }
        }
    }

    public void scale(double scaleX, double scaleY) {
        transform.setX(transform.getX() * scaleX);
        transform.setY(transform.getY() * scaleY);
        // Si tienes componentes que dependen de tamaño, redimensiónalos aquí
        for (Component c : components) {
            c.scale(scaleX, scaleY);
        }
    }

    @Override
    public void acceptCollision(GameObjects other) {
        other.acceptVisitor(new CollisionVisitorInstance(this));
    }

    @Override
    public void onCollisionWith(Player player) {
    }

    @Override
    public void onCollisionWith(Enemy enemy) {
    }

    @Override
    public void onCollisionWith(Bullet bullet) {
    }

    @Override
    public void onCollisionWith(BlockWorld block) {
    }

    @Override
    public void onCollisionWith(Obstacle obstacle) {
    }
}