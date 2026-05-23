package Game.Enemys;

import Game.Engine.MovingObjects;
import Game.Engine.Components.PhysicsComponent;
import Game.Engine.Components.Collisions.ColliderComponent;
import Game.Engine.Colisions.CollisionVisitor;
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
        super(position,texture, 10, 30, physics);
        getTransform().setPosition(position);

        this.player = player;
        this.health = new HealthComponent(maxHealth);
        this.state = new EnemyState();
        this.ai = new EnemyAI(comport);

        addComponent(new PhysicsComponent(physics));
        addComponent(new ColliderComponent());
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

    }

    protected abstract void updateTypePhysics();

    public PhysicsComponent getPhysicsComponent() {
        return getComponent(PhysicsComponent.class);
    }

    public EnemyState getState() {
        return state;
    }

    public void damage(int amount) {
        health.damage(amount);
    }

    protected void onDeath() {
        System.out.println("enemigo morido");
    }

    @Override
    public void acceptVisitor(CollisionVisitor visitor) {
        visitor.visit(this);
    }
}