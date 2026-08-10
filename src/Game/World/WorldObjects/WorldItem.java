package Game.World.WorldObjects;

import Game.Engine.Colisions.Filter.CollisionProfile;
import Game.Engine.Entity.Components.Collisions.ColliderComponent;
import Game.Engine.Entity.Components.Visuals.SpriteRendererComponent;
import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Engine.GameEventBus;
import Game.Engine.GameObjects;
import Game.Items.Savement.ItemStack;
import Game.Player.Player;
import Game.World.Systems.PickupSystem;
import java.awt.image.BufferedImage;

/**
 * Ítem físico en el mundo — puede ser recogido por el jugador.
 *
 * ── DISEÑO ───────────────────────────────────────────────────────────────
 * WorldItem es un GameObject real: tiene Transform, Collider y SpriteRenderer.
 * Participa en el depth sort y en el sistema de colisiones igual que cualquier
 * otro objeto del mundo.
 *
 * La lógica de pickup NO vive en Player ni en WorldItem — vive en PickupSystem.
 * WorldItem solo sabe que tiene un ItemStack y que puede "ser recogido".
 *
 * ── PICKUP ───────────────────────────────────────────────────────────────
 * La colisión con Player dispara intentPickup(player). PickupSystem decide
 * si el inventario tiene espacio y merge automáticamente si aplica.
 *
 * Si el ItemStack queda vacío después del pickup (pickup parcial), WorldItem
 * se destruye. Si no, queda con el sobrante.
 *
 * ── USO ──────────────────────────────────────────────────────────────────
 *   WorldItem item = new WorldItem(
 *       new Vector2D(200, 300),
 *       new ItemStack(ItemRegistry.get("pistol_9mm")),
 *       pistolIcon
 *   );
 *   world.add(item);
 */
public class WorldItem extends GameObjects implements Game.Engine.Destroyable {

    private ItemStack itemStack;
    private boolean pendingRemoval = false;

    /**
     * Bus de eventos para publicar OnPickupEvent.
     * Inyectado desde el sistema que crea el WorldItem (LootSystem, PickupBootstrap, etc.).
     * Si es null, el pickup ocurre pero no se emite el evento.
     */
    private GameEventBus eventBus;

    public WorldItem(Vector2D position, ItemStack itemStack, BufferedImage icon) {
        this.itemStack = itemStack;

        getTransform().setPosition(position);

        // Render: usa el ícono del ítem como sprite
        if (icon != null) {
            addComponent(new SpriteRendererComponent(icon));
        }

        // Collider trigger para detectar al jugador
        ColliderComponent collider = new ColliderComponent(
            16, 16,
            CollisionProfile.WORLD_ITEM   // nuevo profile — ver nota abajo
        );
        collider.setType(ColliderComponent.Type.TRIGGER);
        addComponent(collider);
    }

    /** Intenta que `player` recoja todo o parte del item. */
    public void attemptPickup(Player player) {
        if (itemStack == null || itemStack.isEmpty()) {
            pendingRemoval = true;
            return;
        }

        // Delegar a PickupSystem para desacoplar la lógica de inventario
        PickupSystem.handlePickup(eventBus, player, this);
    }

    @Override
    public void update() {
        super.update();
        // La remoción real la hace el World en su ciclo de limpieza
    }

    @Override
    public void onCollisionWith(GameObjects other) {
        if (other instanceof Player p) {
            attemptPickup(p);
        }
        // Ignora colisiones con Enemy, Bullet, etc.
    }

    // ── API ───────────────────────────────────────────────────────────────

    public ItemStack getItemStack()             { return itemStack; }
    public void setItemStack(ItemStack stack)   { this.itemStack = stack; }
    public boolean isPendingRemoval()           { return pendingRemoval; }
    public void markForRemoval()                { pendingRemoval = true; }

    /**
     * Inyecta el bus de eventos para publicar OnPickupEvent al recoger el ítem.
     * Llamar desde el sistema que crea y añade el WorldItem al mundo.
     *
     * @param bus bus de eventos activo. Null = el evento no se emite.
     */
    public void setEventBus(GameEventBus bus)   { this.eventBus = bus; }

    /** Implementa Destroyable — WorldObjectsContainer elimina el ítem cuando se recoge. */
    @Override
    public boolean isPendingDestruction()       { return pendingRemoval; }
}
