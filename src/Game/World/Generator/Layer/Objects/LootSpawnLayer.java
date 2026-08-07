package Game.World.Generator.Layer.Objects;

import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Items.Creation.ItemDefinition;
import Game.Items.Creation.ItemRarity;
import Game.Items.Creation.ItemRegistry;
import Game.Items.Savement.ItemStack;
import Game.World.Chunk.Chunk;
import Game.World.Generator.Layer.WorldLayer;
import Game.World.WorldObjects.WorldItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Capa de loot — genera WorldItems en posiciones aleatorias del mundo.
 *
 * ── MIGRACIÓN A COORDENADAS GLOBALES (ETAPA 2) ────────────────────────────
 *
 * ANTES: posiciones x = margin + random.nextInt(maxX - margin) en coords locales.
 *
 * AHORA: posición global = chunk.getOriginX() + margen + random.nextInt(maxLocalX)
 *
 * VERIFICACIÓN para chunk(0,0): originX=0 → idéntico al anterior.
 *
 * ── DISEÑO ───────────────────────────────────────────────────────────────
 * Usa loot tables simples: LootEntry con item ID, rareza y rango de cantidad.
 * Los pesos se calculan sumando ItemRarity.weight de cada entry.
 *
 * ── USO ──────────────────────────────────────────────────────────────────
 *   WorldGeneratorConfig cfg = WorldGeneratorConfig.defaults()
 *       .addLayer(new LootSpawnLayer.Builder()
 *           .count(5, 10)
 *           .addEntry("pistol_9mm", ItemRarity.UNCOMMON, 1, 1)
 *           .addEntry("ammo_9mm",   ItemRarity.COMMON,   10, 30)
 *           .build());
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
    public void generate(Chunk chunk, Random random) {
        if (entries.isEmpty()) return;

        int range = maxCount - minCount;
        int count = (range > 0) ? minCount + random.nextInt(range) : minCount;

        // Área disponible en coordenadas locales
        int maxLocalX = chunk.getWidth()  - margin;
        int maxLocalY = chunk.getHeight() - margin;
        if (maxLocalX <= margin || maxLocalY <= margin) return;

        int totalWeight = entries.stream().mapToInt(e -> e.rarity.weight).sum();

        int originX = chunk.getOriginX();
        int originY = chunk.getOriginY();

        for (int i = 0; i < count; i++) {
            LootEntry entry = rollEntry(random, totalWeight);
            if (entry == null) continue;

            ItemDefinition def = ItemRegistry.find(entry.itemId);
            if (def == null) continue;

            int amount = entry.minAmount + (entry.maxAmount > entry.minAmount
                ? random.nextInt(entry.maxAmount - entry.minAmount + 1)
                : 0);

            // Posición local dentro del área disponible
            int localX = margin + random.nextInt(maxLocalX - margin);
            int localY = margin + random.nextInt(maxLocalY - margin);

            // Convertir a coordenadas globales
            int globalX = originX + localX;
            int globalY = originY + localY;

            WorldItem worldItem = new WorldItem(
                new Vector2D(globalX, globalY),
                new ItemStack(def, amount),
                def.icon
            );

            chunk.add(worldItem);
        }
    }

    private LootEntry rollEntry(Random random, int totalWeight) {
        int roll = random.nextInt(totalWeight);
        int accumulated = 0;
        for (LootEntry entry : entries) {
            accumulated += entry.rarity.weight;
            if (roll < accumulated) return entry;
        }
        return entries.get(entries.size() - 1);
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
