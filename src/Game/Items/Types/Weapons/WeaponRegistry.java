package Game.Items.Types.Weapons;

import java.util.*;

import Game.Items.Creation.ItemRarity;

/**
 * Registro central de todas las armas disponibles en el juego.
 *
 * ── LIFECYCLE: SINGLETON DE APLICACIÓN ───────────────────────────────────
 *
 * WeaponRegistry es un singleton de APLICACIÓN, no de partida ni de World.
 * Vive desde la primera llamada a init() hasta que termina el proceso.
 *
 * DECISIÓN ARQUITECTÓNICA:
 *   Las definiciones de armas (nombre, rareza, factory) son constantes del
 *   juego — no cambian entre partidas, entre scenes ni entre Worlds.
 *   Destruir y recrear el registry entre partidas sería un reset sin valor:
 *   los mismos datos serían registrados nuevamente en el siguiente init().
 *
 *   Por eso WeaponRegistry NO tiene reset(). Si en el futuro se necesita
 *   reconfiguración en caliente (ej: DLC, modding), implementar un método
 *   de recarga específico, no un reset general.
 *
 * OWNERSHIP DE INICIALIZACIÓN:
 *   GameState.init() llama WeaponRegistry.init() + registerDefaults() UNA VEZ.
 *   Llamadas adicionales a init() son no-op (if instance == null guard).
 *   Llamadas adicionales a registerDefaults() lanzarán IllegalStateException
 *   porque register() detecta IDs duplicados — correcto: falla rápido.
 *
 * SIN LIFECYCLE DE WORLD:
 *   WeaponRegistry no instala listeners en GameEventBus. No retiene referencias
 *   a WorldManager, Player ni entidades del mundo. No necesita limpiarse
 *   cuando un World se destruye.
 *
 * ── DISEÑO ───────────────────────────────────────────────────────────────
 * Equivalente al ItemRegistry pero para armas. Cada arma es única por run:
 * el jugador la obtiene y la tiene permanentemente hasta que termina la run.
 *
 * ── RAREZA CONFIGURABLE ──────────────────────────────────────────────────
 * Las rarezas por defecto están definidas en el registro, pero pueden
 * sobreescribirse con overrideRarity() desde un archivo de configuración
 * de balance sin recompilar. Así el diseñador puede ajustar droprates
 * editando un JSON/properties, no el código.
 *
 * ── FLUJO DE USO ─────────────────────────────────────────────────────────
 *  1. GameState.init() → WeaponRegistry.init() + WeaponRegistry.registerDefaults()
 *  2. Sistema de loot/tienda → WeaponRegistry.buildOfferPool(alreadyOwned) → lista filtrada
 *  3. Jugador elige → WeaponRegistry.instantiate(id) → Weapon lista para usar
 *
 * ── CÓMO AÑADIR UN ARMA ──────────────────────────────────────────────────
 *  1. Crear la clase WeaponComport en WeaponType/WeaponClass/.
 *  2. Añadir una entrada en registerDefaults() con su factory.
 *  3. Nada más.
 */
public final class WeaponRegistry {

    private static WeaponRegistry instance;

    private final Map<String, Game.Items.Creation.ItemDefinition> definitions = new LinkedHashMap<>();

    // Permite al diseñador sobreescribir rarezas desde configuración externa
    private final Map<String, ItemRarity> rarityOverrides = new HashMap<>();

    private WeaponRegistry() {}

    // ── Ciclo de vida ─────────────────────────────────────────────────────

    public static void init() {
        if (instance == null) {
            instance = new WeaponRegistry();
        }
    }

    public static WeaponRegistry getInstance() {
        if (instance == null) throw new IllegalStateException(
            "WeaponRegistry no inicializado. Llamá WeaponRegistry.init() primero.");
        return instance;
    }

    /**
     * Método deprecated — las armas ahora se registran en WeaponType.
     * 
     * @deprecated Las armas se declaran en WeaponType static block.
     *             Este método no hace nada y existe solo para compatibilidad.
     */
    @Deprecated
    public static void registerDefaults() {
        // No-op: las armas se declaran ahora en WeaponType static block
        // Mantenido solo para evitar romper código que llama a este método
    }

    // ── API ───────────────────────────────────────────────────────────────

    public static void register(Game.Items.Creation.ItemDefinition def) {
        WeaponRegistry reg = getInstance();
        if (reg.definitions.containsKey(def.getIdAsString())) {
            throw new IllegalStateException("WeaponDefinition duplicada: '" + def.getIdAsString() + "'");
        }
        reg.definitions.put(def.getIdAsString(), def);
    }

    public static Game.Items.Creation.ItemDefinition get(String id) {
        Game.Items.Creation.ItemDefinition def = getInstance().definitions.get(id);
        if (def == null) throw new IllegalArgumentException(
            "WeaponDefinition no encontrada: '" + id + "'");
        return def;
    }

    public static boolean has(String id) {
        return getInstance().definitions.containsKey(id);
    }

    public static Collection<Game.Items.Creation.ItemDefinition> all() {
        return Collections.unmodifiableCollection(getInstance().definitions.values());
    }

    /**
     * Sobreescribe la rareza de un arma desde configuración externa.
     * Llamar antes de buildOfferPool(). No modifica el código fuente.
     *
     * Ejemplo desde un archivo de balance:
     *   WeaponRegistry.overrideRarity("rift_cannon", ItemRarity.UNCOMMON);
     */
    public static void overrideRarity(String weaponId, ItemRarity rarity) {
        getInstance().rarityOverrides.put(weaponId, rarity);
    }

    /**
     * Rareza efectiva de un arma (override externo si existe, default si no).
     */
    public static ItemRarity getRarity(String weaponId) {
        WeaponRegistry reg = getInstance();
        ItemRarity override = reg.rarityOverrides.get(weaponId);
        return override != null ? override : get(weaponId).getRarity();
    }

    /**
     * Construye un pool de oferta filtrado por rareza y excluyendo
     * las armas que el jugador ya posee en esta run.
     *
     * El sistema de loot/tienda llama esto para generar las opciones
     * que se presentan al jugador.
     *
     * @param alreadyOwned IDs de armas que el jugador ya tiene
     * @param maxCount     máximo de opciones a ofrecer
     * @param random       fuente de aleatoriedad
     * @return lista de ItemDefinitions disponibles (ya filtradas y seleccionadas)
     */
    public static List<Game.Items.Creation.ItemDefinition> buildOfferPool(
            Set<String> alreadyOwned, int maxCount, Random random) {

        WeaponRegistry reg = getInstance();

        // Pool de candidatos: todas las armas que el jugador aún no tiene
        List<Game.Items.Creation.ItemDefinition> candidates = new ArrayList<>();
        for (Game.Items.Creation.ItemDefinition def : reg.definitions.values()) {
            if (!alreadyOwned.contains(def.getIdAsString())) {
                candidates.add(def);
            }
        }
        if (candidates.isEmpty()) return List.of();

        // Selección ponderada por rareza (ruleta)
        int totalWeight = candidates.stream()
            .mapToInt(d -> getRarity(d.getDescription()).weight)
            .sum();

        List<Game.Items.Creation.ItemDefinition> result = new ArrayList<>();
        Set<String> selected = new HashSet<>();

        int attempts = 0;
        while (result.size() < maxCount && result.size() < candidates.size() && attempts < 100) {
            attempts++;
            int roll = random.nextInt(totalWeight);
            int acc  = 0;
            for (Game.Items.Creation.ItemDefinition d : candidates) {
                acc += getRarity(d.getIdAsString()).weight;
                if (roll < acc && !selected.contains(d.getIdAsString())) {
                    result.add(d);
                    selected.add(d.getIdAsString());
                    break;
                }
            }
        }
        return Collections.unmodifiableList(result);
    }
}
