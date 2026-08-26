package Game.Items.Core;

import Game.Items.Creation.ItemRarity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Constructor genérico de pools de oferta.
 *
 * ── ARCHITECTURE — Items Module ──────────────────────────────────────────
 *
 * Infraestructura transversal para selección ponderada.
 *
 * NO conoce:
 *
 *   - Bullets
 *   - Weapons
 *   - Amulets
 *
 * Trabaja únicamente con:
 *
 *   Collection<T>
 *   Predicate<T>
 *   Function<T, ItemRarity>
 *
 * Por ello puede utilizarse con cualquier familia.
 */
public final class OfferPoolBuilder {

    private OfferPoolBuilder() {
    }

    /**
     * Construye una oferta ponderada.
     */
    public static <T> List<T> build(
            Collection<T> candidates,
            Predicate<T> filter,
            Function<T, ItemRarity> rarityMapper,
            int maxCount,
            Random random
    ) {

        return build(
                candidates,
                filter,
                rarityMapper,
                maxCount,
                random,
                200
        );
    }

    /**
     * Construye una oferta ponderada con límite de intentos.
     */
    public static <T> List<T> build(
            Collection<T> candidates,
            Predicate<T> filter,
            Function<T, ItemRarity> rarityMapper,
            int maxCount,
            Random random,
            int maxAttempts
    ) {

        if (candidates == null ||
                candidates.isEmpty()) {

            return Collections.emptyList();
        }

        if (filter == null) {
            throw new IllegalArgumentException(
                    "filter no puede ser null"
            );
        }

        if (rarityMapper == null) {
            throw new IllegalArgumentException(
                    "rarityMapper no puede ser null"
            );
        }

        if (random == null) {
            throw new IllegalArgumentException(
                    "random no puede ser null"
            );
        }

        if (maxCount <= 0 ||
                maxAttempts <= 0) {

            return Collections.emptyList();
        }

        // ── Filtrado ───────────────────────────────────────────────────────

        List<T> eligible =
                new ArrayList<>();

        for (T candidate : candidates) {

            if (candidate != null &&
                    filter.test(candidate)) {

                ItemRarity rarity =
                        rarityMapper.apply(candidate);

                if (rarity != null &&
                        rarity.getWeight() > 0) {

                    eligible.add(candidate);
                }
            }
        }

        if (eligible.isEmpty()) {
            return Collections.emptyList();
        }

        // ── Peso total ─────────────────────────────────────────────────────

        long totalWeight = 0;

        for (T candidate : eligible) {

            ItemRarity rarity =
                    rarityMapper.apply(candidate);

            totalWeight +=
                    rarity.getWeight();
        }

        if (totalWeight <= 0) {
            return Collections.emptyList();
        }

        // ── Selección ──────────────────────────────────────────────────────

        List<T> result =
                new ArrayList<>();

        Set<T> selected =
                new HashSet<>();

        int attempts = 0;

        while (
                result.size() < maxCount &&
                result.size() < eligible.size() &&
                attempts < maxAttempts
        ) {

            attempts++;

            /*
             * Random.nextInt(int) limita el peso máximo a Integer.MAX_VALUE.
             *
             * Para el sistema actual de ItemRarity no representa un problema,
             * pero mantenemos el total como long para evitar overflow.
             */
            int roll;

            if (totalWeight <= Integer.MAX_VALUE) {

                roll = random.nextInt(
                        (int) totalWeight
                );

            } else {

                roll = (int) Math.floorMod(
                        random.nextLong(),
                        totalWeight
                );
            }

            long accumulated = 0;

            for (T candidate : eligible) {

                ItemRarity rarity =
                        rarityMapper.apply(candidate);

                accumulated +=
                        rarity.getWeight();

                if (roll < accumulated &&
                        !selected.contains(candidate)) {

                    result.add(candidate);
                    selected.add(candidate);

                    break;
                }
            }
        }

        return Collections.unmodifiableList(
                result
        );
    }

    /**
     * Variante simplificada para elementos que poseen rareza.
     */
    public static <T extends HasRarity> List<T> buildSimple(
            Collection<T> candidates,
            Predicate<T> filter,
            int maxCount,
            Random random
    ) {

        return build(
                candidates,
                filter,
                HasRarity::getRarity,
                maxCount,
                random
        );
    }

    /**
     * Contrato mínimo para objetos con rareza.
     */
    public interface HasRarity {

        ItemRarity getRarity();
    }
}