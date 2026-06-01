package Game.Items.Types.Weapons;

import Game.Items.Creation.ItemRarity;
import Game.Items.Types.Weapons.WeaponType.WeaponComport;

import java.util.*;

/**
 * Registro central de todas las armas disponibles en el juego.
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

    /**
     * Construye un pool de BulletTypes disponibles (no obtenidos aún),
     * con selección ponderada por rareza.
     *
     * Centralizado aquí para que loot y tienda usen la misma lógica.
     */
    public static List<Game.Items.Types.Bullets.BulletType> buildBulletOfferPool(
            Set<Game.Items.Types.Bullets.BulletType> alreadyOwned, int maxCount, Random random) {

        List<Game.Items.Types.Bullets.BulletType> candidates = new ArrayList<>();
        for (Game.Items.Types.Bullets.BulletType bt : Game.Items.Types.Bullets.BulletType.values()) {
            if (!alreadyOwned.contains(bt)) {
                candidates.add(bt);
            }
        }
        if (candidates.isEmpty()) return List.of();

        int totalWeight = candidates.stream()
            .mapToInt(bt -> bt.defaultRarity.weight)
            .sum();

        List<Game.Items.Types.Bullets.BulletType> result = new ArrayList<>();
        Set<Game.Items.Types.Bullets.BulletType> selected = new HashSet<>();

        int attempts = 0;
        while (result.size() < maxCount && result.size() < candidates.size() && attempts < 100) {
            attempts++;
            int roll = random.nextInt(totalWeight);
            int acc  = 0;
            for (Game.Items.Types.Bullets.BulletType bt : candidates) {
                acc += bt.defaultRarity.weight;
                if (roll < acc && !selected.contains(bt)) {
                    result.add(bt);
                    selected.add(bt);
                    break;
                }
            }
        }
        return Collections.unmodifiableList(result);
    }
}
