package Game.Engine;

import java.awt.Rectangle;

import Game.Engine.Components.PhysicsComponent;
import Game.Engine.Components.Collisions.ColliderComponent;
import Game.Engine.Components.Visuals.SpriteRenderer;
import Game.Fisics.Physics;
import Game.Fisics.PhysicsStepper;
import GameMath.Vector2D;

import java.awt.image.BufferedImage;

public abstract class MovingObjects extends GameObjects {

    protected PhysicsComponent physicsComponent;

    public MovingObjects(Vector2D position,
                         BufferedImage texture,
                         int colWidth,
                         int colHeight,
                         Physics physics) {

        getTransform().setPosition(position);

        addComponent(new SpriteRenderer(texture));
        addComponent(new ColliderComponent());

        physicsComponent = new PhysicsComponent(physics);
        addComponent(physicsComponent);
    }

    public Physics getPhysics() {
        return physicsComponent.getPhysics();
    }

    public Vector2D getVelocity() {
        return getPhysics().getVelocity();
    }

    public void moveByPhysics() {
        Vector2D vel = getVelocity();
        PhysicsStepper.moveWith(this, vel.getX(), vel.getY());
    }

    public Vector2D getCenter() {
        var pos = getTransform().getPosition();
        ColliderComponent collider = getComponent(ColliderComponent.class);
        if (collider != null) {
            return pos.add(new Vector2D(collider.getWidth() / 2.0, collider.getHeight() / 2.0));
        }
        return pos;
    }
    
    public Rectangle getBounds() {
        ColliderComponent collider = getComponent(ColliderComponent.class);
        if (collider != null) return collider.getBounds();
        return new java.awt.Rectangle(
                (int)getTransform().getPosition().getX(),
                (int)getTransform().getPosition().getY(),
                0, 0
        );
    }
}