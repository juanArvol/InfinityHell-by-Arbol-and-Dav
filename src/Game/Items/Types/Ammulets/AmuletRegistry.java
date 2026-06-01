package Game.Items.Types.Ammulets;

import Game.Items.Creation.ItemRarity;
import Game.Items.Types.Bullets.BulletComport.BulletBehavior;
import Game.Items.Types.Weapons.WeaponType.WeaponStats;

import java.util.*;

/**
 * Registro central de amuletos y sistema de aplicación acumulativa.
 *
 * ── AMULETOS vs ARMAS/BALAS ───────────────────────────────────────────────
 * | Categoría      | Únicos por run | Apilables | Infinitos |
 * |----------------|---------------|-----------|-----------|
 * | Armas          | Sí            | No        | No        |
 * | Tipos de bala  | Sí            | No        | No        |
 * | Amuletos       | No            | Sí        | Sí*       |
 *
 * *"Infinitos" = pueden ofrecerse indefinidamente; el pool nunca se agota.
 *
 * ── FLUJO DE USO ─────────────────────────────────────────────────────────
 *  1. GameState.init() → AmuletRegistry.init() + registerDefaults()
 *  2. Loot/tienda → AmuletRegistry.buildOfferPool(count, random)
 *     (no recibe "ya obtenidos" — todos siempre son elegibles)
 *  3. Jugador recoge → PlayerAmulets.add(id)
 *  4. Al disparar → AmuletRegistry.applyAll(playerAmulets, stats, behavior)
 *
 * ── RAREZA CONFIGURABLE ──────────────────────────────────────────────────
 * Igual que WeaponRegistry: overrideRarity() permite al diseñador ajustar
 * frecuencias desde configuración externa sin recompilar.
 */
public final class AmuletRegistry {

    private static AmuletRegistry instance;

    private final Map<String, AmuletDefinition> definitions = new LinkedHashMap<>();
    private final Map<String, ItemRarity> rarityOverrides   = new HashMap<>();

    private AmuletRegistry() {}

    // ── Ciclo de vida ─────────────────────────────────────────────────────

    public static void init() {
        if (instance == null) instance = new AmuletRegistry();
    }

    public static AmuletRegistry getInstance() {
        if (instance == null) throw new IllegalStateException(
            "AmuletRegistry no inicializado. Llamá AmuletRegistry.init() primero.");
        return instance;
    }

    /**
     * Registra todos los amuletos del juego.
     * Llamar UNA VEZ tras init(), desde GameState.init().
     *
     * ── AQUÍ VA EL BALANCE DE AMULETOS ───────────────────────────────────
     * Los números concretos (cuánto daño, cuántas perforaciones) son los
     * que tú defines. Están centralizados aquí, no repartidos en modificadores.
     */
    public static void registerDefaults() {

        // ── Daño ──────────────────────────────────────────────────────────

        register(new AmuletDefinition(
            "bone_tip",
            "Punta Ósea",
            "+8 de daño por proyectil. Acumulable.",
            ItemRarity.COMMON,
            new AmuletEffect() {
                @Override
                public void applyToStats(WeaponStats stats) {
                    stats.setDamageBonusByWeapon(stats.getDamageBonusByWeapon() + 8.0);
                }
            }
        ));

        // ── Velocidad de proyectil / alcance ──────────────────────────────

        register(new AmuletDefinition(
            "swift_quill",
            "Pluma Veloz",
            "+15% velocidad de proyectil (aumenta alcance efectivo). Acumulable.",
            ItemRarity.COMMON,
            new AmuletEffect() {
                @Override
                public void applyToStats(WeaponStats stats) {
                    stats.setBulletSpeedBase(stats.getBulletSpeedBase() * 1.15);
                }
            }
        ));

        // ── Cadencia ──────────────────────────────────────────────────────

        register(new AmuletDefinition(
            "tempo_ring",
            "Anillo de Tempo",
            "-10% cooldown de disparo. Acumulable.",
            ItemRarity.UNCOMMON,
            new AmuletEffect() {
                @Override
                public void applyToStats(WeaponStats stats) {
                    stats.setCooldown((int)(stats.getCooldown() * 0.90));
                }
            }
        ));

        // ── Dispersión ────────────────────────────────────────────────────

        register(new AmuletDefinition(
            "steady_grip",
            "Empuñadura Firme",
            "-20% dispersión de proyectiles. Acumulable.",
            ItemRarity.UNCOMMON,
            new AmuletEffect() {
                @Override
                public void applyToStats(WeaponStats stats) {
                    stats.setSpread(stats.getSpread() * 0.80);
                }
            }
        ));

        // ── Perforación ───────────────────────────────────────────────────

        register(new AmuletDefinition(
            "phase_shard",
            "Esquirla de Fase",
            "Los proyectiles perforan +1 enemigo adicional. Acumulable.",
            ItemRarity.RARE,
            new AmuletEffect() {
                @Override
                public BulletBehavior wrapBehavior(BulletBehavior base) {
                    // Reutiliza el wrapper de PiercingModifier refactorizado
                    return new Game.Items.Types.Ammulets.Effects.PiercingAmuletWrapper(base, 1);
                }
            }
        ));

        // ── Rebote entre enemigos ─────────────────────────────────────────

        register(new AmuletDefinition(
            "echo_stone",
            "Piedra del Eco",
            "Al impactar, el proyectil salta a un enemigo cercano (+1 salto). Acumulable.",
            ItemRarity.RARE,
            new AmuletEffect() {
                @Override
                public BulletBehavior wrapBehavior(BulletBehavior base) {
                    return new Game.Items.Types.Ammulets.Effects.BounceAmuletWrapper(base, 1);
                }
            }
        ));

        // ── Multi-proyectil ───────────────────────────────────────────────

        register(new AmuletDefinition(
            "split_crystal",
            "Cristal Partido",
            "+1 proyectil por disparo. Acumulable.",
            ItemRarity.EPIC,
            new AmuletEffect() {
                @Override
                public void applyToStats(WeaponStats stats) {
                    stats.setBulletsPerShot(stats.getBulletsPerShot() + 1);
                }
            }
        ));
    }

    // ── API ───────────────────────────────────────────────────────────────

    public static void register(AmuletDefinition def) {
        AmuletRegistry reg = getInstance();
        if (reg.definitions.containsKey(def.id)) {
            throw new IllegalStateException("AmuletDefinition duplicada: '" + def.id + "'");
        }
        reg.definitions.put(def.id, def);
    }

    public static AmuletDefinition get(String id) {
        AmuletDefinition def = getInstance().definitions.get(id);
        if (def == null) throw new IllegalArgumentException(
            "AmuletDefinition no encontrada: '" + id + "'");
        return def;
    }

    public static Collection<AmuletDefinition> all() {
        return Collections.unmodifiableCollection(getInstance().definitions.values());
    }

    public static void overrideRarity(String amuletId, ItemRarity rarity) {
        getInstance().rarityOverrides.put(amuletId, rarity);
    }

    public static ItemRarity getRarity(String amuletId) {
        AmuletRegistry reg = getInstance();
        ItemRarity override = reg.rarityOverrides.get(amuletId);
        return override != null ? override : get(amuletId).defaultRarity;
    }

    /**
     * Construye un pool de amuletos ofrecidos al jugador.
     *
     * A diferencia de armas y balas, los amuletos SIEMPRE están disponibles:
     * no se filtra por "ya obtenidos". El pool puede devolver el mismo amuleto
     * que ya tiene el jugador — eso es intencional (apilamiento).
     *
     * @param maxCount máximo de opciones a ofrecer
     * @param random   fuente de aleatoriedad
     */
    public static List<AmuletDefinition> buildOfferPool(int maxCount, Random random) {
        AmuletRegistry reg = getInstance();
        List<AmuletDefinition> candidates = new ArrayList<>(reg.definitions.values());
        if (candidates.isEmpty()) return List.of();

        int totalWeight = candidates.stream()
            .mapToInt(d -> getRarity(d.id).weight)
            .sum();

        List<AmuletDefinition> result = new ArrayList<>();
        Set<String> selected = new HashSet<>();

        int attempts = 0;
        while (result.size() < maxCount && attempts < 200) {
            attempts++;
            int roll = random.nextInt(totalWeight);
            int acc  = 0;
            for (AmuletDefinition d : candidates) {
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
     * Aplica todos los amuletos del jugador (con apilamiento) a un WeaponStats
     * y envuelve el BulletBehavior.
     *
     * Llamar desde ModifiedWeapon.tryShoot() en lugar de iterar WeaponModifiers.
     *
     * @param ownedAmulets lista de IDs de amuletos del jugador (puede repetirse)
     * @param stats        copia mutable de WeaponStats a modificar
     * @param behavior     behavior base a envolver
     * @return behavior con todos los efectos de amuleto aplicados
     */
    public static BulletBehavior applyAll(
            List<String> ownedAmulets,
            WeaponStats stats,
            BulletBehavior behavior) {

        for (String id : ownedAmulets) {
            AmuletDefinition def = getInstance().definitions.get(id);
            if (def == null) continue; // ID desconocido — skip silencioso

            def.effect.applyToStats(stats);
            behavior = def.effect.wrapBehavior(behavior);
        }
        return behavior;
    }
}
