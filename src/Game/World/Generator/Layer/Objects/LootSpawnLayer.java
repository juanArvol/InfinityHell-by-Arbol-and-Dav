package Game.World.Generator.Layer.Objects;

import Game.Items.ItemDefinition;
import Game.Items.ItemRarity;
import Game.Items.ItemRegistry;
import Game.Items.ItemStack;
import Game.World.Core.World;
import Game.World.Generator.Layer.WorldLayer;
import Game.World.WorldObjects.WorldItem;
import GameMath.Vector2D;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Capa de loot — genera WorldItems en posiciones aleatorias del mundo.
 *
 * ── DISEÑO ───────────────────────────────────────────────────────────────
 * Usa loot tables simples: una lista de LootEntry donde cada entrada tiene
 * un item ID, una rareza (para peso) y un rango de cantidad.
 *
 * Los pesos se calculan sumando ItemRarity.weight de cada entry,
 * sin hardcodear distribuciones específicas.
 *
 * ── USO ──────────────────────────────────────────────────────────────────
 *   // En WorldGeneratorConfig:
 *   WorldGeneratorConfig cfg = WorldGeneratorConfig.defaults()
 *       .addLayer(new LootSpawnLayer.Builder()
 *           .count(5, 10)
 *           .addEntry("pistol_9mm",  ItemRarity.UNCOMMON, 1, 1)
 *           .addEntry("ammo_9mm",    ItemRarity.COMMON,   10, 30)
 *           .addEntry("bandage",     ItemRarity.COMMON,   1, 3)
 *           .build());
 *
 * ── RETRO-COMPATIBLE ─────────────────────────────────────────────────────
 * LootSpawnLayer no modifica WorldGenerator. Se añade a la config como layer.
 */
public class LootSpawnLayer implements WorldLayer {

    private final int minCount;
    private final int maxCount;
    private final List<LootEntry> entries;
    private final int margin;

    private LootSpawnLayer(Builder b) {
        this.minCount = b.minCount;
        this.maxCount = b.maxCount;
        this.entries  = List.copyOf(b.entries);
        this.margin   = b.margin;
    }

    @Override
    public void generate(World world, Random random) {
        if (entries.isEmpty()) return;

        int range = maxCount - minCount;
        int count = (range > 0) ? minCount + random.nextInt(range) : minCount;

        int maxX = world.getWidth()  - margin;
        int maxY = world.getHeight() - margin;
        if (maxX <= margin || maxY <= margin) return;

        // Total de pesos para la ruleta
        int totalWeight = entries.stream().mapToInt(e -> e.rarity.weight).sum();

        for (int i = 0; i < count; i++) {
            LootEntry entry = rollEntry(random, totalWeight);
            if (entry == null) continue;

            // Intentar obtener la definición del registro
            ItemDefinition def = ItemRegistry.find(entry.itemId);
            if (def == null) continue; // ítem no registrado — skip silencioso

            int amount = entry.minAmount + (entry.maxAmount > entry.minAmount
                ? random.nextInt(entry.maxAmount - entry.minAmount + 1)
                : 0);

            int x = margin + random.nextInt(maxX - margin);
            int y = margin + random.nextInt(maxY - margin);

            WorldItem worldItem = new WorldItem(
                new Vector2D(x, y),
                new ItemStack(def, amount),
                def.icon
            );

            world.add(worldItem);
        }
    }

    private LootEntry rollEntry(Random random, int totalWeight) {
        int roll = random.nextInt(totalWeight);
        int accumulated = 0;
        for (LootEntry entry : entries) {
            accumulated += entry.rarity.weight;
            if (roll < accumulated) return entry;
        }
        return entries.get(entries.size() - 1); // fallback
    }

    // ── Entrada de loot table ─────────────────────────────────────────────

    public record LootEntry(
        String itemId,
        ItemRarity rarity,
        int minAmount,
        int maxAmount
    ) {}

    // ── Builder ───────────────────────────────────────────────────────────

    public static class Builder {
        private int minCount = 3;
        private int maxCount = 8;
        private int margin   = 40;
        private final List<LootEntry> entries = new ArrayList<>();

        public Builder count(int min, int max) {
            this.minCount = min;
            this.maxCount = max;
            return this;
        }

        public Builder margin(int px) {
            this.margin = px;
            return this;
        }

        public Builder addEntry(String itemId, ItemRarity rarity, int minAmount, int maxAmount) {
            entries.add(new LootEntry(itemId, rarity, minAmount, maxAmount));
            return this;
        }

        public Builder addEntry(String itemId, ItemRarity rarity) {
            return addEntry(itemId, rarity, 1, 1);
        }

        public LootSpawnLayer build() {
            return new LootSpawnLayer(this);
        }
    }
}
