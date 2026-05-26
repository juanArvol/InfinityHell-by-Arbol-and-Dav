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
import Game.Items.EquippedItems;
import Game.Items.Inventory;
import Game.World.Core.World;
import Game.World.WorldObjects.BlockWorld;
import Game.World.WorldObjects.Obstacle;
import GameMath.Vector2D;

import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * Player — integrado con Inventory y EquippedItems.
 *
 * CAMBIOS RESPECTO AL ORIGINAL:
 *   - Añade Inventory (20 slots por defecto, configurable).
 *   - Añade EquippedItems para slots de equipamiento.
 *   - Expone getInventory() y getEquippedItems() para PickupSystem y UI.
 *
 * TODO LO DEMÁS ES IDÉNTICO AL ORIGINAL — no se tocó lógica de física,
 * colisiones, controller ni combat.
 *
 * Para activar Physics3D (salto Z), añadir:
 *   addComponent(new Physics3DComponent());
 * en el constructor (comentado abajo).
 */
public class Player extends MovingObjects {

    private final PlayerController controller;
    private final PlayerCombat combat;
    private final PlayerStats stats;
    private final PlayerState state;
    private final PhysicsComponent pc;

    // ── NUEVO: inventario y equipamiento ──────────────────────────────────
    private final Inventory inventory;
    private final EquippedItems equippedItems;

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

        // ── Inventario ────────────────────────────────────────────────────
        inventory     = new Inventory(20);
        equippedItems = new EquippedItems();

        // ── Physics3D opcional (descomentar para activar saltos Z) ─────────
        // addComponent(new Game.Physics3D.Physics3DComponent());
    }

    @Override
    public void update() {
        // Mismo orden que el original — NO modificado
        if (pc != null) {
            state.setEnElSuelo(pc.getPhysics().getOnGround());
        }

        controller.update();
        combat.update();

        if (pc != null) {
            pc.getPhysics().applyGravity(state.isEnElSuelo());
        }

        super.update();
        stats.update();
    }

    // ── Getters originales ────────────────────────────────────────────────

    public Vector2D getPosition()           { return getTransform().getPosition(); }
    public PlayerState getState()           { return state; }
    public PlayerController getController() { return controller; }
    public PlayerCombat getCombat()         { return combat; }
    public PlayerStats getStats()           { return stats; }

    // ── NUEVOS getters ────────────────────────────────────────────────────

    public Inventory getInventory()           { return inventory; }
    public EquippedItems getEquippedItems()   { return equippedItems; }

    // ── Colisiones — idénticas al original ────────────────────────────────

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
