package Game.Items.Creation;

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
 * Trabaja con ItemDefinition como base universal para todas las familias:
 *   - Weapons
 *   - Bullets
 *   - Ammulets
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

    private final Map<String, ItemDefinition> definitions = new HashMap<>();

    private ItemRegistry() {}

    /**
     * Holder pattern para inicialización lazy thread-safe.
     * Se inicializa solo cuando se accede por primera vez.
     */

    /**
     * Holder pattern para inicialización lazy thread-safe.
     * Se inicializa solo cuando se accede por primera vez.
     */
    private static class Holder {
        static final ItemRegistry INSTANCE = new ItemRegistry();
    }

    public static ItemRegistry getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * @deprecated Usar getInstance() directamente. La inicialización es automática y thread-safe.
     */
    @Deprecated
    public static void init() {
        // No-op: la inicialización ocurre automáticamente vía Holder
        // Mantenido por compatibilidad con código existente
        getInstance();
    }

    // ── API estática de conveniencia ──────────────────────────────────────

    /** Registra una definición. Lanza si el ID ya existe. */
    public static void register(ItemDefinition def) {
        ItemRegistry reg = getInstance();
        if (reg.definitions.containsKey(def.getItemId().asString())) {
            throw new IllegalStateException("ItemDefinition duplicada: '" + def.getItemId() + "'");
        }
        reg.definitions.put(def.getItemId().asString(), def);
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
