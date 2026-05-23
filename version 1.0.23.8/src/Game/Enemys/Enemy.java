package Game.Enemys;

import Game.Engine.MovingObjects;
import Game.Engine.Components.PhysicsComponent;
import Game.Engine.Components.Collisions.ColliderComponent;
import Game.Engine.Filter.CollisionProfile;
import Game.Player.Player;
import GameMath.Vector2D;
import Game.Enemys.AI.EnemyAI;
import Game.Enemys.AI.EnemyComport;
import Game.Enemys.Components.EnemyState;
import Game.Enemys.Components.HealthComponent;
import Game.Fisics.EnemyPhysics;

import java.awt.image.BufferedImage;

public abstract class Enemy extends MovingObjects {

    private final EnemyAI ai;
    private final Player player;

    private final HealthComponent health;
    private final EnemyState state;

    public Enemy(
            Vector2D position,
            BufferedImage texture,
            int maxHealth,
            EnemyComport comport,
            Player player,
            EnemyPhysics physics
    ) {

        super(position, texture, physics);

        this.player = player;

        this.health = new HealthComponent(maxHealth);
        this.state = new EnemyState();

        this.ai = new EnemyAI(comport);

        // ================= COLLIDER =================

        ColliderComponent collider =
                getComponent(ColliderComponent.class);

        if (collider != null) {
            collider.setProfile(CollisionProfile.ENEMY);
            collider.setSize(24, 30);
        }
    }

    @Override
    public void update() {

        if (health.isDead()) {
            onDeath();
            return;
        }

        state.setMoving(false);
        state.setAttacking(false);

        ai.update(this, player);

        updateTypePhysics();

        moveByPhysics();

        super.update();
    }

    protected abstract void updateTypePhysics();

    public PhysicsComponent getPhysicsComponent() {
        return getComponent(PhysicsComponent.class);
    }

    public EnemyState getState() {
        return state;
    }

    public HealthComponent getHealthComponent() {
        return health;
    }

    public void damage(int amount) {
        health.damage(amount);
    }

    protected void onDeath() {
        System.out.println(
                "[Enemy] Murió un enemigo en "
                        + getTransform().getPosition()
        );
    }
}