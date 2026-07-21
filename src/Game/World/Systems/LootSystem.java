package Game.World.Systems;

import Game.Engine.Events.GameEventBus;
import Game.Engine.Events.OnEnemyDeathEvent;
import Game.Engine.GameMath.SpaceLogic.Logic2D.Vector2D;
import Game.Items.Creation.ItemDefinition;
import Game.Items.Creation.ItemRarity;
import Game.Items.Creation.ItemRegistry;
import Game.Items.Savement.ItemStack;
import Game.World.WorldObjects.WorldItem;
import Game.World.WorldObjects.WorldObjectFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

/**
 * Sistema de loot — spawnea WorldItems cuando un enemigo muere.
 *
 * Vive en Game.World.Systems porque orquesta conceptos del Game:
 * Enemy, Items, WorldObjects. No es infraestructura reutilizable del Engine.
 *
 * MIGRADO DESDE: Game.Engine.Systems.LootSystem
 * RAZÓN: LootSystem importa Enemy, ItemDefinition, ItemRegistry, WorldItem,
 * WorldObjectFactory — todos tipos del Game. Tenerlo en el Engine creaba
 * dependencias Engine → Game en 5 paquetes distintos.
 *
 * LootSystem recibe un itemSpawner inyectado → no depende de WorldManager.
 * El que construye LootSystem decide cómo spawnear ítems en el mundo.
 */
public class LootSystem {

    private final List<LootEntry> entries;
    private final Random          random;
    private final Consumer<WorldItem> itemSpawner;

    public LootSystem(Consumer<WorldItem> itemSpawner) {
        this(itemSpawner, new Random());
    }

    public LootSystem(Consumer<WorldItem> itemSpawner, long seed) {
        this(itemSpawner, new Random(seed));
    }

    private LootSystem(Consumer<WorldItem> itemSpawner, Random random) {
        this.itemSpawner = itemSpawner;
        this.random      = random;
        this.entries     = new ArrayList<>();
    }

    // ── Configuración ─────────────────────────────────────────────────────

    public LootSystem addEntry(String itemId, ItemRarity rarity,
                               float dropChance, int minAmount, int maxAmount) {
        entries.add(new LootEntry(itemId, rarity, dropChance, minAmount, maxAmount));
        return this;
    }

    // ── Activación ────────────────────────────────────────────────────────

    /** Registra el handler en el bus de eventos global. */
    public void register() {
        GameEventBus.GLOBAL.subscribe(OnEnemyDeathEvent.class, this::onEnemyDeath);
    }

    /** Registra el handler en una instancia específica del bus. */
    public void register(GameEventBus bus) {
        bus.subscribe(OnEnemyDeathEvent.class, this::onEnemyDeath);
    }

    // ── Handler ───────────────────────────────────────────────────────────

    private void onEnemyDeath(OnEnemyDeathEvent event) {
        Vector2D pos = event.position();

        for (LootEntry entry : entries) {
            if (random.nextFloat() > entry.dropChance) continue;

            ItemDefinition def = ItemRegistry.find(entry.itemId);
            if (def == null) continue;

            int amount = entry.minAmount;
            if (entry.maxAmount > entry.minAmount) {
                amount += random.nextInt(entry.maxAmount - entry.minAmount + 1);
            }

            double ox = (random.nextDouble() - 0.5) * 24;
            double oy = (random.nextDouble() - 0.5) * 24;
            Vector2D spawnPos = new Vector2D(pos.getX() + ox, pos.getY() + oy);

            WorldItem worldItem = WorldObjectFactory.worldItem(spawnPos, new ItemStack(def, amount));
            itemSpawner.accept(worldItem);
        }
    }

    // ── Entrada de loot ───────────────────────────────────────────────────

    private record LootEntry(
        String     itemId,
        ItemRarity rarity,
        float      dropChance,
        int        minAmount,
        int        maxAmount
    ) {}
}
