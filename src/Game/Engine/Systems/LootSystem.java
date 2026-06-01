package Game.Engine.Systems;

import Game.Engine.Events.GameEventBus;
import Game.Engine.Events.OnEnemyDeathEvent;            // ← standalone, fuente de verdad
import Game.Engine.GameMath.SpaceLogic.Logic2D.Vector2D;
import Game.Items.Creation.ItemDefinition;
import Game.Items.Creation.ItemRarity;
import Game.Items.Creation.ItemRegistry;
import Game.Items.Savement.ItemStack;
import Game.World.WorldObjects.WorldObjectFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Sistema de loot — spawnea WorldItems cuando un enemigo muere.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * CORRECCIÓN DE CONTRATO DE EVENTO
 *
 * PROBLEMA ANTERIOR:
 *   LootSystem importaba:
 *     import Game.Engine.Events.GameEvents.OnEnemyDeathEvent;
 *
 *   Esto referenciaba el record INTERNO de GameEvents, que es una clase
 *   distinta de Game.Engine.Events.OnEnemyDeathEvent (standalone).
 *
 *   Si el emisor (e.g. EnemyManager) usaba el standalone y LootSystem
 *   suscribía el interno → el handler NUNCA se ejecutaba.
 *   Sin error de compilación. Sin excepción en runtime. Loot inexistente.
 *
 * CAUSA RAÍZ:
 *   Divergencia de contratos por refactor incompleto (dos clases para el
 *   mismo evento semántico). Ver GameEvents.java y OnEnemyDeathEvent.java.
 *
 * SOLUCIÓN:
 *   Importar siempre Game.Engine.Events.OnEnemyDeathEvent (standalone).
 *   GameEvents.OnEnemyDeathEvent fue eliminado del catálogo interno.
 *   Ahora solo existe UNA clase → cero ambigüedad.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * ARQUITECTURA (sin cambios)
 *
 * LootSystem recibe un itemSpawner inyectado → no depende de WorldManager.
 * El que construye LootSystem decide cómo spawnear ítems en el mundo.
 */
public class LootSystem {

    private final List<LootEntry> entries;
    private final Random          random;
    private final java.util.function.Consumer<Object> itemSpawner;

    /**
     * @param itemSpawner callback que recibe el WorldItem y lo añade al mundo.
     *                    Normalmente: item -> world.add(item)
     */
    public LootSystem(java.util.function.Consumer<Object> itemSpawner) {
        this(itemSpawner, new Random());
    }

    public LootSystem(java.util.function.Consumer<Object> itemSpawner, long seed) {
        this(itemSpawner, new Random(seed));
    }

    private LootSystem(java.util.function.Consumer<Object> itemSpawner, Random random) {
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

    /**
     * Registra el handler en el bus de eventos global.
     * A partir de este momento, cada OnEnemyDeathEvent generará loot.
     */
    public void register() {
        // Suscribir al STANDALONE OnEnemyDeathEvent — fuente de verdad única.
        GameEventBus.GLOBAL.subscribe(OnEnemyDeathEvent.class, this::onEnemyDeath);
    }

    /**
     * Registra el handler en una instancia específica del bus.
     * Útil para escenas aisladas o tests.
     */
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

            Object worldItem = WorldObjectFactory.worldItem(spawnPos, new ItemStack(def, amount));
            itemSpawner.accept(worldItem);
        }
    }

    // ── Registro de entrada ───────────────────────────────────────────────

    private record LootEntry(
        String     itemId,
        ItemRarity rarity,
        float      dropChance,
        int        minAmount,
        int        maxAmount
    ) {}
}
