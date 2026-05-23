package Game.Player;

import Game.Bullets.Bullet;
import Game.Enemys.Enemy;
import Game.Engine.GameObjects;
import Game.Engine.MovingObjects;
import Game.Engine.Components.PhysicsComponent;
import Game.Engine.Components.Collisions.ColliderComponent;
import Game.Engine.Components.Visuals.HitBoxComponent;
import Game.Engine.Components.Visuals.SpriteRenderer;
import Game.Engine.Filter.CollisionProfile;
import Game.Fisics.PlayerPhysics;
import Game.World.Core.World;
import Game.World.WorldObjects.BlockWorld;
import Game.World.WorldObjects.Obstacle;
import GameMath.Vector2D;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class Player extends MovingObjects {

    private final PlayerController controller;
    private final PlayerCombat combat;
    private final PlayerStats stats;
    private final PlayerState state;

    public Player(
            Vector2D spawn,
            BufferedImage texture,
            World world
    ) {

        super(
                spawn,
                texture,
                new PlayerPhysics(0.78)
        );

        state = new PlayerState();

        stats = new PlayerStats();

        controller = new PlayerController(this, state);

        combat = new PlayerCombat(this, state);

        // ================= COLLIDER =================

        ColliderComponent collider =
                getComponent(ColliderComponent.class);

        if (collider != null) {
            collider.setProfile(CollisionProfile.PLAYER);
            collider.setSize(15, 24);
            collider.setOffset(4, 0);
        }

        // ================= DEBUG =================

        addComponent(new HitBoxComponent(Color.RED));

        // ================= RENDER =================

        addComponent(new PlayerRenderer(state));
    }

    @Override
    public void update() {

        PhysicsComponent pc =
                getComponent(PhysicsComponent.class);

        if (pc != null) {
            state.setEnElSuelo(
                    pc.getPhysics().getOnGround()
            );
        }

        controller.update();

        combat.update();

        moveByPhysics();

        super.update();

        stats.update();
    }

    public Vector2D getPosition() {
        return getTransform().getPosition();
    }

    public PlayerState getState() {
        return state;
    }

    public PlayerController getController() {
        return controller;
    }

    public PlayerCombat getCombat() {
        return combat;
    }

    public PlayerStats getStats() {
        return stats;
    }

    // ================= COLLISIONS =================

    @Override
    public void onCollisionWith(BlockWorld block) {
        state.setEnElSuelo(true);
    }

    @Override
    public void onCollisionWith(Obstacle obstacle) {
        state.setEnElSuelo(true);
    }

    @Override
    public void onCollisionWith(Enemy enemy) {
        // TODO
    }

    @Override
    public void onCollisionWith(Bullet bullet) {
        // TODO
    }

    @Override
    public void onCollisionWith(Player player) {}

}