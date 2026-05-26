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
        // ── ORDEN DE UPDATE ───────────────────────────────────────────────
        //
        // CORRECTO (este orden):
        //   1. Sincronizar state desde physics: CollisionsSystem FASE 0
        //      ya seteó physics.onGround este frame; hay que leerlo ANTES
        //      de pasárselo a moveX() y applyGravity().
        //   2. controller.update() → moveX() recibe el onGround real.
        //      Si hay salto: jump() + setOnGround(false) actualiza la física
        //      directamente; applyGravity() lo verá en el paso 3.
        //   3. applyGravity() con el onGround ya correcto.
        //
        // BUG anterior (doble problema):
        //   A) Physics.moveX() sobreescribía this.onGround con el parámetro,
        //      pisando el valor correcto que puso la FASE 0. Al caminar fuera
        //      de un bloque en X, onGround quedaba true hasta que vy != 0
        //      volvía a ejecutar el eje Y (fix: moveX() ya no toca onGround).
        //   B) state se sincronizaba DESPUÉS de controller.update(), así que
        //      moveX() y applyGravity() usaban el valor del frame anterior.
        // ─────────────────────────────────────────────────────────────────

        // 1. Sincronizar estado PRIMERO desde la física.
        //    CollisionsSystem ya corrió su FASE 0 y seteó physics.onGround
        //    correctamente. Leer aquí garantiza que controller.update() →
        //    moveX() y applyGravity() usen el valor real del frame,
        //    no el del frame anterior.
        if (pc != null) {
            state.setEnElSuelo(pc.getPhysics().getOnGround());
        }

        // 2. Input con onGround ya correcto
        controller.update();
        combat.update();

        // 3. Gravedad con el onGround correcto
        if (pc != null) {
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

    @Override
    public void onCollisionWith(BlockWorld block) {
        state.setEnElSuelo(true);
    }

    @Override
    public void onCollisionWith(Obstacle obstacle) {
        state.setEnElSuelo(true);
    }

    @Override public void onCollisionWith(Enemy enemy)   {}
    @Override public void onCollisionWith(Bullet bullet) {}
    @Override public void onCollisionWith(Player player) {}
}
