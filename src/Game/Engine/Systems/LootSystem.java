package Game.Engine.Systems;

import Game.Events.GameEventBus;
import Game.Events.OnEnemyDeathEvent;
import Game.Items.ItemDefinition;
import Game.Items.ItemRarity;
import Game.Items.ItemRegistry;
import Game.Items.ItemStack;
import Game.World.Core.World;
import Game.World.Core.WorldManager;
import Game.World.WorldObjects.WorldObjectFactory;
import GameMath.Vector2D;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Sistema de loot — spawnea WorldItems cuando un enemigo muere.
 *
 * ── DISEÑO ───────────────────────────────────────────────────────────────
 * Escucha OnEnemyDeathEvent del bus. No conoce a Enemy directamente.
 * La loot table se configura por tipo de enemigo o globalmente.
 *
 * Registro:
 *   LootSystem.register();  // en GameBootstrap
 *
 * Uso con tabla por defecto (todos los enemigos):
 *   LootSystem system = new LootSystem();
 *   system.addEntry("bandage",   ItemRarity.COMMON,   0.4f, 1, 2);
 *   system.addEntry("ammo_9mm",  ItemRarity.COMMON,   0.6f, 5, 15);
 *   system.addEntry("pistol_9mm",ItemRarity.RARE,     0.08f,1, 1);
 *   system.register();
 */
public class LootSystem {

    private final List<LootEntry> entries = new ArrayList<>();
    private final Random random;

    public LootSystem() {
        this(new Random());
    }

    public LootSystem(long seed) {
        this(new Random(seed));
    }

    private LootSystem(Random random) {
        this.random = random;
    }

    // ── Configuración ─────────────────────────────────────────────────────

    /**
     * @param itemId    ID en ItemRegistry
     * @param rarity    rareza (afecta color en UI, aquí no afecta drop chance)
     * @param dropChance probabilidad de caer [0.0, 1.0]
     * @param minAmount  mínimo a dropear
     * @param maxAmount  máximo a dropear
     */
    public LootSystem addEntry(String itemId, ItemRarity rarity,
                               float dropChance, int minAmount, int maxAmount) {
        entries.add(new LootEntry(itemId, rarity, dropChance, minAmount, maxAmount));
        return this;
    }

    // ── Activación ────────────────────────────────────────────────────────

    /** Registra este sistema en el GameEventBus. Llamar una vez en bootstrap. */
    public void register() {
        GameEventBus.subscribe(OnEnemyDeathEvent.class, this::onEnemyDeath);
    }

    // ── Handler ───────────────────────────────────────────────────────────

    private void onEnemyDeath(OnEnemyDeathEvent event) {
        World world = WorldManager.getInstance().getCurrentWorld();
        if (world == null) return;

        Vector2D pos = event.position();

        for (LootEntry entry : entries) {
            if (random.nextFloat() > entry.dropChance) continue;

            ItemDefinition def = ItemRegistry.find(entry.itemId);
            if (def == null) continue;

            int amount = entry.minAmount;
            if (entry.maxAmount > entry.minAmount) {
                amount += random.nextInt(entry.maxAmount - entry.minAmount + 1);
            }

            // Pequeño offset aleatorio para que los ítems no se apilen exactamente
            double ox = (random.nextDouble() - 0.5) * 24;
            double oy = (random.nextDouble() - 0.5) * 24;
            Vector2D spawnPos = new Vector2D(pos.getX() + ox, pos.getY() + oy);

            world.add(WorldObjectFactory.worldItem(spawnPos, new ItemStack(def, amount)));
        }
    }

    // ── Entrada de tabla ─────────────────────────────────────────────────

    private record LootEntry(
        String itemId,
        ItemRarity rarity,
        float dropChance,
        int minAmount,
        int maxAmount
    ) {}
}
