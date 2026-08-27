package Game.Items.Types.Weapons;

import Game.Items.Creation.ItemRarity;
import Game.Items.Creation.ItemRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Registro central de definiciones de armas.
 *
 * ── ARQUITECTURA — Items Module ──────────────────────────────────────────
 *
 * WeaponRegistry especializa ItemRegistry para WeaponDefinition.
 *
 * ItemRegistry proporciona toda la infraestructura genérica:
 * - register()
 * - get()
 * - find()
 * - has()
 * - getAll()
 *
 * WeaponRegistry solamente contiene responsabilidades específicas
 * del dominio Weapon.
 *
 * ── LIFECYCLE ────────────────────────────────────────────────────────────
 *
 * Singleton de aplicación.
 *
 * Las definiciones de armas son datos estáticos del juego y no pertenecen
 * al lifecycle de World ni de una partida.
 *
 * ── RESPONSABILIDADES ESPECÍFICAS ────────────────────────────────────────
 *
 * - Overrides de rareza para balance.
 * - Construcción de pools de oferta de armas.
 *
 * La creación del comportamiento runtime NO pertenece aquí.
 * WeaponType + ObjectTypeFactory se encargan de ello.
 */
public final class WeaponRegistry
        extends ItemRegistry<WeaponDefinition> {

    private static WeaponRegistry instance;

    /**
     * Rarezas modificadas externamente para balance.
     *
     * La rareza base continúa perteneciendo a WeaponDefinition.
     */
    private final Map<WeaponID, ItemRarity> rarityOverrides =
            new HashMap<>();

    // ── Constructor ──────────────────────────────────────────────────────

    private WeaponRegistry() {
    }

    // ── Lifecycle ────────────────────────────────────────────────────────

    public static void init() {

        if (instance == null) {
            instance = new WeaponRegistry();
        }
    }

    public static WeaponRegistry getInstance() {

        if (instance == null) {
            throw new IllegalStateException("WeaponRegistry no inicializado. " + "Llamá WeaponRegistry.init() primero.");
        }

        return instance;
    }

    // ── Rareza ───────────────────────────────────────────────────────────

    /**
     * Sobreescribe la rareza efectiva de un arma.
     *
     * No modifica la WeaponDefinition original.
     */
    public static void overrideRarity(WeaponID weaponId, ItemRarity rarity) {
        if (weaponId == null) {
            throw new IllegalArgumentException("weaponId no puede ser null");
        }

        if (rarity == null) {
            throw new IllegalArgumentException("rarity no puede ser null");
        }

        getInstance()
                .rarityOverrides
                .put(weaponId, rarity);
    }

    /**
     * Obtiene la rareza efectiva de un arma.
     *
     * Utiliza el override si existe; de lo contrario,
     * utiliza la rareza definida en WeaponDefinition.
     */
    public static ItemRarity getRarity(WeaponID weaponId) {

        if (weaponId == null) {
            throw new IllegalArgumentException("weaponId no puede ser null");
        }

        WeaponRegistry registry = getInstance();

        ItemRarity override = registry.rarityOverrides.get(weaponId);

        if (override != null) {
            return override;
        }

        return registry.get(weaponId).getRarity();
    }

    // ── Offer Pool ───────────────────────────────────────────────────────

    /**
     * Construye un pool de oferta de armas.
     *
     * Excluye las armas que ya posee el jugador y selecciona
     * las restantes mediante ponderación por rareza.
     *
     * @param alreadyOwned IDs de armas que el jugador ya posee
     * @param maxCount máximo de ofertas
     * @param random fuente de aleatoriedad
     * @return lista inmutable de definiciones disponibles
     */
    public static List<WeaponDefinition> buildOfferPool(
            Set<WeaponID> alreadyOwned,
            int maxCount,
            Random random
    ) {

        if (alreadyOwned == null) {
            throw new IllegalArgumentException("alreadyOwned no puede ser null");
        }

        if (maxCount < 0) {
            throw new IllegalArgumentException("maxCount no puede ser negativo");
        }

        if (random == null) {
            throw new IllegalArgumentException("random no puede ser null");
        }

        WeaponRegistry registry = getInstance();

        List<WeaponDefinition> candidates = new ArrayList<>();

        for (WeaponDefinition definition : registry.getAll()) {

            WeaponID weaponId =
                    (WeaponID) definition.getItemId();

            if (!alreadyOwned.contains(weaponId)) {
                candidates.add(definition);
            }
        }

        if (candidates.isEmpty() || maxCount == 0) {
            return List.of();
        }

        List<WeaponDefinition> result = new ArrayList<>();

        List<WeaponDefinition> remaining = new ArrayList<>(candidates);

        while (result.size() < maxCount && !remaining.isEmpty()) {
            int totalWeight = remaining.stream().mapToInt(definition -> getRarity((WeaponID)definition.getItemId()).weight).sum();

            int roll = random.nextInt(totalWeight);

            int accumulated = 0;

            for (int i = 0; i < remaining.size(); i++) {

                WeaponDefinition definition =
                        remaining.get(i);

                accumulated += getRarity((WeaponID)definition.getItemId()).weight;

                if (roll < accumulated) {
                    result.add(definition);
                    remaining.remove(i);
                    break;
                }
            }
        }

        return Collections.unmodifiableList(result);
    }
}