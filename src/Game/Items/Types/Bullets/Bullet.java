package Game.Items.Types.Bullets;

import Game.Enemys.Core.Enemy;
import Game.Engine.Colisions.Filter.CollisionProfile;
import Game.Engine.Components.Collisions.ColliderComponent;
import Game.Engine.Components.Physics2DComponent;
import Game.Engine.Components.Visuals.HitBoxComponent;
import Game.Engine.Components.Visuals.SpriteRenderer;
import Game.Engine.GameMath.Physics.PhysicsStepper;
import Game.Engine.GameMath.SpaceLogic.Logic2D.Vector2D;
import Game.Engine.GameObjects;
import Game.Items.Types.Bullets.BulletComport.BulletBehavior;
import Game.World.WorldObjects.WorldObjectsContainer;
import java.awt.Color;
import java.awt.image.BufferedImage;

public class Bullet extends GameObjects implements WorldObjectsContainer.Destroyable {

    private final BulletBehavior behavior;
    private final double damage;

    private final BulletLife bulletLife;
    private final Physics2DComponent physicsComponent;

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

        physicsComponent = new Physics2DComponent(physics);

        addComponent(physicsComponent);
    }

    @Override
    public void update() {

        if (!bulletLife.tick())
            return;

        // Aplicar gravedad al vector de velocidad antes de mover,
        // solo si este behavior tiene gravedad habilitada.
        // applyGravity() solo modifica velocity.y — no mueve la posición.
        if (behavior.hasGravity()) {
            getPhysics().applyGravity(false);
        }

        behavior.update(this);

        moveByPhysics();

        super.update();
    }

    public BulletLife getBulletLife() {
        return bulletLife;
    }

    /** Implementa Destroyable — WorldObjectsContainer elimina la bala cuando muere. */
    @Override
    public boolean isPendingDestruction() {
        return !bulletLife.isAlive();
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
    public void onCollisionWith(GameObjects other) {
        if (other instanceof Enemy e) {
            e.damage((int) damage);
            bulletLife.setDead();
        }
        // BlockWorld: la bala simplemente para (no hace daño al bloque)
        // El bulletLife no se marca como dead aquí — el movimiento
        // ya se detiene por CollisionsSystem al resolver el SweptAABB.
    }
}
