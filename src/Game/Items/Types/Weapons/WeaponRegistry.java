package Game.Items.Types.Weapons;

import java.util.*;

import Game.Items.ItemRarity;

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

    private final Map<String, WeaponDefinition> definitions = new LinkedHashMap<>();

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
     * Registra todas las armas del juego.
     * Llamar UNA VEZ tras init(), desde GameState.init().
     *
     * ── AQUÍ VA EL BALANCE ───────────────────────────────────────────────
     * Añade nuevas armas aquí. La rareza es el DEFAULT; puede cambiarse
     * externamente con overrideRarity().
     */
    public static void registerDefaults() {
        WeaponRegistry reg = getInstance();

        // Importar las clases de WeaponComport concretas aquí:
        // import Game.Weapons.WeaponType.WeaponClass.*;

        reg.register(new WeaponDefinition(
            "ethereal_revolver",
            "Revólver Etéreo",
            "Un revólver forjado con materia del vacío. Disparo único, preciso.",
            ItemRarity.COMMON,
            // () -> new WeaponRevolver()   ← reemplaza con tu clase real
            () -> { throw new UnsupportedOperationException("Implementar WeaponRevolver"); }
        ));

        reg.register(new WeaponDefinition(
            "soul_caster",
            "Lanzador de Almas",
            "Dispara ráfagas de energía espectral. Alta cadencia, bajo daño por proyectil.",
            ItemRarity.UNCOMMON,
            () -> { throw new UnsupportedOperationException("Implementar WeaponSoulCaster"); }
        ));

        reg.register(new WeaponDefinition(
            "void_scatter",
            "Dispersor del Vacío",
            "Dispara múltiples esquirlas en cono. Devastador a corta distancia.",
            ItemRarity.UNCOMMON,
            // () -> new WeaponEscopeta()   ← tu clase existente
            () -> { throw new UnsupportedOperationException("Implementar WeaponVoidScatter"); }
        ));

        reg.register(new WeaponDefinition(
            "rift_cannon",
            "Cañón de Fisura",
            "Proyectil lento pero con área de impacto. Modo carga disponible.",
            ItemRarity.RARE,
            () -> { throw new UnsupportedOperationException("Implementar WeaponRiftCannon"); }
        ));

        reg.register(new WeaponDefinition(
            "phase_lance",
            "Lanza de Fase",
            "Disparo único que atraviesa todo. Cooldown muy alto.",
            ItemRarity.EPIC,
            () -> { throw new UnsupportedOperationException("Implementar WeaponPhaseLance"); }
        ));
    }

    // ── API ───────────────────────────────────────────────────────────────

    public static void register(WeaponDefinition def) {
        WeaponRegistry reg = getInstance();
        if (reg.definitions.containsKey(def.id)) {
            throw new IllegalStateException("WeaponDefinition duplicada: '" + def.id + "'");
        }
        reg.definitions.put(def.id, def);
    }

    public static WeaponDefinition get(String id) {
        WeaponDefinition def = getInstance().definitions.get(id);
        if (def == null) throw new IllegalArgumentException(
            "WeaponDefinition no encontrada: '" + id + "'");
        return def;
    }

    public static boolean has(String id) {
        return getInstance().definitions.containsKey(id);
    }

    public static Collection<WeaponDefinition> all() {
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
        return override != null ? override : get(weaponId).defaultRarity;
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
     * @return lista de WeaponDefinitions disponibles (ya filtradas y seleccionadas)
     */
    public static List<WeaponDefinition> buildOfferPool(
            Set<String> alreadyOwned, int maxCount, Random random) {

        WeaponRegistry reg = getInstance();

        // Pool de candidatos: todas las armas que el jugador aún no tiene
        List<WeaponDefinition> candidates = new ArrayList<>();
        for (WeaponDefinition def : reg.definitions.values()) {
            if (!alreadyOwned.contains(def.id)) {
                candidates.add(def);
            }
        }
        if (candidates.isEmpty()) return List.of();

        // Selección ponderada por rareza (ruleta)
        int totalWeight = candidates.stream()
            .mapToInt(d -> getRarity(d.id).weight)
            .sum();

        List<WeaponDefinition> result = new ArrayList<>();
        Set<String> selected = new HashSet<>();

        int attempts = 0;
        while (result.size() < maxCount && result.size() < candidates.size() && attempts < 100) {
            attempts++;
            int roll = random.nextInt(totalWeight);
            int acc  = 0;
            for (WeaponDefinition d : candidates) {
                acc += getRarity(d.id).weight;
                if (roll < acc && !selected.contains(d.id)) {
                    result.add(d);
                    selected.add(d.id);
                    break;
                }
            }
        }
        return Collections.unmodifiableList(result);
    }
}
