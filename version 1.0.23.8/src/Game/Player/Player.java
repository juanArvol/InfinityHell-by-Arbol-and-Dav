package Game.Player;

import Game.Bullets.Bullet;
import Game.Enemys.Enemy;
import Game.Engine.GameObjects;
import Game.Engine.MovingObjects;
import Game.Engine.Components.PhysicsComponent;
import Game.Engine.Components.Collisions.ColliderComponent;
import Game.Engine.Components.Visuals.HitBoxComponent;
import Game.Engine.Components.Visuals.SizeSyncMode;
import Game.Engine.Components.Visuals.SpriteRenderer;
import Game.Engine.Filter.Masks.CollisionProfiles;
import Game.Fisics.PlayerPhysics;
import Game.World.Core.World;
import Game.World.WorldObjects.BlockWorld;
import Game.World.WorldObjects.Obstacle;
import GameMath.Vector2D;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class Player extends MovingObjects {

    private PlayerController controller;
    private PlayerCombat combat;
    private PlayerStats stats;
    private PlayerRenderer renderer;
    private PlayerState state;

    private HitBoxComponent hitBox;
    private ColliderComponent collider;
    private SpriteRenderer baseRenderer;
    private PhysicsComponent pc;

    public Player(Vector2D spawn, BufferedImage texture, World world) {

        super(
            spawn,
            texture,
            32,
            38,
            new PlayerPhysics(0.78)
        );

        state = new PlayerState();
        stats = new PlayerStats();

        controller = new PlayerController(this, state);
        combat = new PlayerCombat(this, state);

        // Renderer independiente
        baseRenderer = new SpriteRenderer(texture);
        baseRenderer.setSyncMode(SizeSyncMode.NONE);
        addComponent(baseRenderer);

        // Hitbox más pequeña para tolerancia
        hitBox = new HitBoxComponent(15, 24, 4, 0);
        hitBox.setDebugColor(Color.RED);
        hitBox.setVisible(true);
        addComponent(hitBox);
        
        // ========= COLLIDER =========
        collider = new ColliderComponent();
        collider.applyProfile(CollisionProfiles.PLAYER);
        addComponent(collider);
        
    }

    @Override
    public void update() {
        pc = getComponent(PhysicsComponent.class);
        if (pc != null) {
            state.setEnElSuelo(pc.getPhysics().getOnGround());
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

    public PlayerState getState() { return state; }
    public PlayerController getController() { return controller; }
    public PlayerCombat getCombat() { return combat; }
    public PlayerStats getStats() { return stats; }
    public PlayerRenderer getRenderer() { return renderer; }
    
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
        state.setEnElSuelo(true);
    }
    
    @Override
    public void onCollisionWith(Obstacle obstacle) {
        state.setEnElSuelo(true);
    }
    
    @Override
    public void onCollisionWith(GameObjects other) {

    }
}