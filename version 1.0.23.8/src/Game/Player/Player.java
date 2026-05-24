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

    // FIX BUG-15: cacheado en constructor para evitar O(n) lookup cada frame.
    private final PhysicsComponent pc;

    public Player(
            Vector2D spawn,
            BufferedImage texture,
            World world
    ) {
        // SizeSyncMode.NONE: el sprite del jugador se dibuja a su tamaño natural.
        // Si querés que el sprite se escale a la hitbox (15x24), cambiá a NONE
        // y llamá syncRendererToCollider() después de setSize(). Ver comentario abajo.
        super(spawn, texture, new PlayerPhysics(0.78), SizeSyncMode.NONE);

        state  = new PlayerState();
        stats  = new PlayerStats();
        controller = new PlayerController(this, state);
        combat     = new PlayerCombat(this, state);

        // ================= COLLIDER =================

        ColliderComponent collider = getComponent(ColliderComponent.class);
        if (collider != null) {
            collider.setProfile(CollisionProfile.PLAYER);
            collider.setSize(15, 24);
            collider.setOffset(4, 0);
        }

        // ── Opción de sincronización ──────────────────────────────────────
        // Descomentá UNA de las líneas según lo que quieras:
        //
        // A) Sprite se escala para coincidir con la hitbox (15×24, offset 4,0):
        //    syncRendererToCollider();
        //
        // B) Control manual: ajustá el tamaño y offset del sprite a mano:
        //    getComponent(SpriteRenderer.class).setRenderSize(20, 32);
        //    getComponent(SpriteRenderer.class).setOffset(-2, -4);
        //
        // C) Sprite a tamaño natural (default actual con SizeSyncMode.NONE):
        //    No hacer nada. El sprite se ve a su resolución original.
        // ─────────────────────────────────────────────────────────────────

        // ================= DEBUG =================

        addComponent(new HitBoxComponent(Color.RED));

        // ================= RENDER =================

        addComponent(new PlayerRenderer(state));

        // FIX BUG-15: cachear aquí
        pc = physicsComponent;
    }

    @Override
    public void update() {

        // Sincronizar enElSuelo desde la física.
        if (pc != null) {
            state.setEnElSuelo(pc.getPhysics().getOnGround());
            // Aplicar gravedad: modifica vy para que CollisionsSystem (SweptAABB) la use.
            pc.getPhysics().applyGravity(state.isEnElSuelo());
        }

        controller.update();
        combat.update();

        // NO llamar moveByPhysics() aquí: el Player es SOLID.
        // CollisionsSystem (Fase 1, SweptAABB) es responsable de moverlo.

        super.update();
        stats.update();
    }

    public Vector2D getPosition()           { return getTransform().getPosition(); }
    public PlayerState getState()           { return state; }
    public PlayerController getController() { return controller; }
    public PlayerCombat getCombat()         { return combat; }
    public PlayerStats getStats()           { return stats; }

    // ================= COLLISIONS =================

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
