package Game.Items.Creation;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import Game.Items.Savement.ItemStack;

/**
 * Registro global de ItemDefinitions.
 *
 * PROPÓSITO: punto único donde se registran todos los tipos de ítem del juego.
 * Equivalente al sistema de data de Project Zomboid (items/*.txt parseados).
 *
 * PATRÓN: Singleton de aplicación, inicializado explícitamente con init()
 * (mismo patrón que WorldManager).
 *
 * Los ítems concretos (armas, munición, consumibles) se definen en clases
 * de datos separadas (ej. WeaponItems, AmmoItems) que llaman a register()
 * durante la inicialización.
 *
 * Uso:
 *   // Al arrancar el juego:
 *   ItemRegistry.init();
 *   ItemRegistry.register(new ItemDefinition.Builder("pistol_9mm", ItemType.FIREARM)
 *       .displayName("Pistola 9mm").magazineSize(15).build());
 *
 *   // En cualquier parte del código:
 *   ItemDefinition def = ItemRegistry.get("pistol_9mm");
 *   ItemStack pistol = new ItemStack(def);
 */
public final class ItemRegistry {

    private static ItemRegistry instance;

    private final Map<String, ItemDefinition> definitions = new HashMap<>();

    private ItemRegistry() {}

    public static void init() {
        if (instance == null) {
            instance = new ItemRegistry();
        }
    }

    public static ItemRegistry getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ItemRegistry no fue inicializado. Llamá ItemRegistry.init() primero.");
        }
        return instance;
    }

    // ── API estática de conveniencia ──────────────────────────────────────

    /** Registra una definición. Lanza si el ID ya existe. */
    public static void register(ItemDefinition def) {
        ItemRegistry reg = getInstance();
        if (reg.definitions.containsKey(def.id)) {
            throw new IllegalStateException("ItemDefinition duplicada: '" + def.id + "'");
        }
        reg.definitions.put(def.id, def);
    }

    /**
     * Obtiene una definición por ID.
     * @throws IllegalArgumentException si el ID no está registrado.
     */
    public static ItemDefinition get(String id) {
        ItemDefinition def = getInstance().definitions.get(id);
        if (def == null) {
            throw new IllegalArgumentException("ItemDefinition no encontrada: '" + id + "'");
        }
        return def;
    }

    /** Devuelve null si no existe (no lanza). Útil para checks opcionales. */
    public static ItemDefinition find(String id) {
        return getInstance().definitions.get(id);
    }

    public static boolean has(String id) {
        return getInstance().definitions.containsKey(id);
    }

    public static Collection<ItemDefinition> all() {
        return Collections.unmodifiableCollection(getInstance().definitions.values());
    }

    /**
     * Crea un ItemStack directamente a partir del ID.
     * Shorthand para: new ItemStack(ItemRegistry.get(id), count)
     */
    public static ItemStack createStack(String id, int count) {
        return new ItemStack(get(id), count);
    }

    public static ItemStack createStack(String id) {
        return new ItemStack(get(id));
    }
}
