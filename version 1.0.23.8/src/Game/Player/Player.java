package Game.Player;

import Game.Bullets.Bullet;
import Game.Enemys.Enemy;
import Game.Engine.MovingObjects;
import Game.Engine.Components.PhysicsComponent;
import Game.Engine.Components.Collisions.ColliderComponent;
import Game.Engine.Components.Visuals.HitBoxComponent;
import Game.Engine.Components.Visuals.SizeSyncMode;
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

    private final PhysicsComponent pc;

    public Player(Vector2D spawn, BufferedImage texture, World world) {
        super(spawn, texture, new PlayerPhysics(0.78), SizeSyncMode.NONE);

        state      = new PlayerState();
        stats      = new PlayerStats();
        controller = new PlayerController(this, state);
        combat     = new PlayerCombat(this, state);

        ColliderComponent collider = getComponent(ColliderComponent.class);
        if (collider != null) {
            collider.setProfile(CollisionProfile.PLAYER);
            collider.setSize(15, 24);
            collider.setOffset(4, 0);
        }

        addComponent(new HitBoxComponent(Color.RED));
        addComponent(new PlayerRenderer(state));

        pc = physicsComponent;
    }

    @Override
    public void update() {
        // ── FIX SALTO ────────────────────────────────────────────────────
        //
        // ORDEN CORRECTO:
        //   1. controller.update() → procesa input, puede llamar jump() y
        //      setOnGround(false) en la misma instrucción.
        //   2. Leer onGround DESPUÉS del input, no antes.
        //   3. applyGravity() con el onGround ya actualizado.
        //
        // ─────────────────────────────────────────────────────────────────

        // 1. Input primero
        controller.update();
        combat.update();

        // 2. Sincronizar estado desde física (ya actualizado por el controller)
        if (pc != null) {
            state.setEnElSuelo(pc.getPhysics().getOnGround());
            // 3. Gravedad con el onGround correcto
            pc.getPhysics().applyGravity(state.isEnElSuelo());
        }

        // CollisionsSystem (SweptAABB) mueve el objeto; no llamar moveByPhysics() aquí.
        super.update();
        stats.update();
    }

    public Vector2D getPosition()           { return getTransform().getPosition(); }
    public PlayerState getState()           { return state; }
    public PlayerController getController() { return controller; }
    public PlayerCombat getCombat()         { return combat; }
    public PlayerStats getStats()           { return stats; }

    // ── Colisiones ────────────────────────────────────────────────────────

    /**
     * FIX: onCollisionWith(BlockWorld/Obstacle) ahora sincroniza TANTO el
     * PlayerState como la física interna (setOnGround + setCurrentSurface).
     *
     * Antes solo se hacía state.setEnElSuelo(true), dejando la física
     * desacoplada: pc.getPhysics().getOnGround() seguía siendo false, lo que
     * hacía que applyGravity() acumulara vy aunque el jugador estuviera parado
     * sobre el suelo, y causaba comportamiento erróneo / crashes al siguiente frame.
     */
    @Override
    public void onCollisionWith(BlockWorld block) {
        state.setEnElSuelo(true);
        if (pc != null) {
            pc.getPhysics().setOnGround(true);
            pc.getPhysics().setCurrentSurface(block);
        }
    }

    @Override
    public void onCollisionWith(Obstacle obstacle) {
        state.setEnElSuelo(true);
        if (pc != null) {
            pc.getPhysics().setOnGround(true);
            pc.getPhysics().setCurrentSurface(obstacle);
        }
    }

    @Override public void onCollisionWith(Enemy enemy)   {}
    @Override public void onCollisionWith(Bullet bullet) {}
    @Override public void onCollisionWith(Player player) {}
}
