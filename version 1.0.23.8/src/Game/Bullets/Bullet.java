package Game.Bullets;

import Game.Bullets.BulletComport.BulletBehavior;
import Game.Engine.GameObjects;
import Game.Engine.Colisions.CollisionVisitor;
import Game.Engine.Components.PhysicsComponent;
import Game.Engine.Components.Collisions.ColliderComponent;
import Game.Engine.Components.Visuals.HitBoxComponent;
import Game.Engine.Components.Visuals.SizeSyncMode;
import Game.Engine.Components.Visuals.SpriteRenderer;
import Game.Fisics.BulletPhysics;
import Game.Fisics.PhysicsStepper;
import GameMath.Vector2D;

import java.awt.image.BufferedImage;

public class Bullet extends GameObjects {

    private final BulletBehavior comport;
    private final double damage;

    private final BulletLife bulletLife;
    private final PhysicsComponent physicsComponent;

    public Bullet(Vector2D position, BufferedImage texture, BulletBehavior comport, double xSpeed, double ySpeed, int lifeTime, double damage) {
        getTransform().setPosition(position);

        this.comport = comport;
        this.damage = damage;
        this.bulletLife = new BulletLife(lifeTime);

        SpriteRenderer renderer = new SpriteRenderer(texture);
        renderer.setSyncMode(SizeSyncMode.NONE);
        addComponent(renderer);

        if (texture != null) {
            HitBoxComponent hitBox = new HitBoxComponent(17,9,7,11);
            addComponent(hitBox);
        }

        addComponent(new ColliderComponent());

        BulletPhysics physics = new BulletPhysics(
                xSpeed,
                ySpeed,
                comport.hasGravity(),
                comport.getGravityValue()
        );

        physicsComponent = new PhysicsComponent(physics);
        addComponent(physicsComponent);
    }

    @Override
    public void update() {

        if (!bulletLife.isAlive())
            return;

        comport.update(this);

        if (comport.hasGravity()) {
            getPhysics().applyGravity(false);
        }

        moveByPhysics();
    }

    public BulletLife getBulletLife() {
        return bulletLife;
    }

    public double getDamage() {
        return damage;
    }

    public BulletPhysics getPhysics() {
        return (BulletPhysics) physicsComponent.getPhysics();
    }

    public void moveByPhysics() {
        var vel = getPhysics().getVelocity();
        PhysicsStepper.moveWith(this, vel.getX(), vel.getY());
    }

    @Override
    public void acceptVisitor(CollisionVisitor visitor) {
        visitor.visit(this);
    }
}