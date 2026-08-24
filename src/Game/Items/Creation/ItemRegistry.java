package Game.Items.Creation;

import Game.Items.ItemDefinition;
import Game.Items.Savement.ItemStack;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Registro global de ItemDefinitions.
 *
 * ── ARQUITECTURA — Items Module ──────────────────────────────────────────
 *
 * PROPÓSITO: Catálogo de TODAS las definiciones de items del juego.
 * Trabaja con ItemDefinition ABSTRACTO como puente a las concretas:
 *   - WeaponDefinition
 *   - BulletDefinition
 *   - AmuletDefinition
 *   - Cualquier otra subclase futura
 *
 * PATRÓN: Singleton de aplicación, inicializado explícitamente con init().
 *
 * Uso:
 *   // Al arrancar el juego:
 *   ItemRegistry.init();
 *   ItemRegistry.register(weaponDef);
 *   ItemRegistry.register(bulletDef);
 *
 *   // En código de loot:
 *   ItemDefinition def = ItemRegistry.get("pistol_9mm");
 *   ItemStack stack = new ItemStack(def, 1);
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
        if (reg.definitions.containsKey(def.getId())) {
            throw new IllegalStateException("ItemDefinition duplicada: '" + def.getId() + "'");
        }
        reg.definitions.put(def.getId(), def);
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
