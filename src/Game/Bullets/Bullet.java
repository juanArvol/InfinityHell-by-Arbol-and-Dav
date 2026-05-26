package Game.Bullets;

import Game.Bullets.BulletComport.BulletBehavior;
import Game.Engine.GameObjects;
import Game.Engine.Components.PhysicsComponent;
import Game.Engine.Components.Collisions.ColliderComponent;
import Game.Engine.Components.Visuals.HitBoxComponent;
import Game.Engine.Components.Visuals.SpriteRenderer;
import Game.Engine.Filter.CollisionProfile;
import Game.Fisics.BulletPhysics;
import Game.Fisics.PhysicsStepper;
import Game.World.WorldObjects.BlockWorld;
import Game.Enemys.Enemy;
import GameMath.Vector2D;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class Bullet extends GameObjects {

    private final BulletBehavior behavior;
    private final double damage;

    private final BulletLife bulletLife;
    private final PhysicsComponent physicsComponent;

    public Bullet(
            Vector2D position,
            BufferedImage texture,
            BulletBehavior behavior,
            double xSpeed,
            double ySpeed,
            int lifeTime,
            double damage
    ) {

        getTransform().setPosition(position);

        this.behavior = behavior;
        this.damage = damage;
        this.bulletLife = new BulletLife(lifeTime);

        // ================= RENDER =================

        if (texture != null) {
            addComponent(new SpriteRenderer(texture));
        }

        // ================= COLLIDER =================

        ColliderComponent collider =
                new ColliderComponent(
                        8,
                        8,
                        CollisionProfile.BULLET
                );

        collider.setType(ColliderComponent.Type.TRIGGER);

        addComponent(collider);

        // Debug visual opcional
        addComponent(new HitBoxComponent(Color.YELLOW));

        // ================= PHYSICS =================

        BulletPhysics physics = new BulletPhysics(
                xSpeed,
                ySpeed,
                behavior.hasGravity(),
                behavior.getGravityValue()
        );

        physicsComponent = new PhysicsComponent(physics);

        addComponent(physicsComponent);
    }

    @Override
    public void update() {

        if (!bulletLife.isAlive())
            return;

        // FIX BUG-06: behavior.update() delega en BulletPhysics.update() que ya
        // aplica gravedad internamente (si hasGravity=true). NO aplicar gravedad
        // de nuevo aqui — causaria que las balas caigan al doble de velocidad.
        behavior.update(this);

        moveByPhysics();

        super.update();
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

    // ================= COLLISIONS =================

    @Override
    public void onCollisionWith(Enemy enemy) {
        enemy.damage((int) damage);
        bulletLife.setDead();
    }

    @Override
    public void onCollisionWith(BlockWorld block) {
    }
}
