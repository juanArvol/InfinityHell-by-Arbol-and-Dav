package Game.Items.Core;

import Game.Items.ItemRarity;
import java.util.*;
import java.util.function.Predicate;

/**
 * Constructor unificado de pools de oferta con selección ponderada por rareza.
 *
 * ── HRFC — Items Module Architectural Consolidation ──────────────────────
 *
 * RESPONSABILIDAD:
 *   Proporciona el algoritmo común de selección ponderada (weighted roulette)
 *   que antes estaba duplicado en BulletType.buildOfferPool() y cada familia
 *   de Items.
 *
 * ALGORITMO:
 *   1. Filtra candidatos según criterio (ej: no obtenidos por jugador)
 *   2. Calcula peso total (suma de rarities)
 *   3. Selecciona aleatoriamente según peso hasta llenar maxCount
 *   4. Evita duplicados en la misma oferta
 *
 * CONFIGURABILIDAD:
 *   - Admite filtro custom vía Predicate
 *   - Admite override de rareza vía función de lookup
 *   - Admite límite de intentos para evitar loops infinitos
 *
 * EJEMPLO DE USO:
 *
 *   // Balas (únicas por run — filtrar obtenidas)
 *   List<BulletType> offer = OfferPoolBuilder.build(
 *       BulletType.values(),
 *       type -> !alreadyOwned.contains(type),
 *       type -> type.defaultRarity,
 *       3,
 *       random
 *   );
 *
 *   // Amuletos (apilables — todos siempre disponibles)
 *   List<AmuletDefinition> offer = OfferPoolBuilder.build(
 *       AmuletRegistry.all(),
 *       def -> true,  // sin filtro
 *       def -> AmuletRegistry.getRarity(def.id),  // con override posible
 *       3,
 *       random
 *   );
 *
 * PRINCIPIO:
 *   Consolidar infraestructura común sin forzar a todas las familias a usar
 *   exactamente la misma API pública. Cada familia puede exponer su propio
 *   buildOfferPool() adaptado a sus necesidades, delegando en esta clase.
 */
public final class OfferPoolBuilder {

    // Constructor privado — clase de utilidad estática
    private OfferPoolBuilder() {}

    /**
     * Construye un pool de oferta con selección ponderada por rareza.
     *
     * @param <T>           tipo de elemento del pool
     * @param candidates    colección de candidatos potenciales
     * @param filter        filtro para determinar elegibilidad (ej: no obtenidos)
     * @param rarityMapper  función que obtiene la rareza de cada candidato
     * @param maxCount      máximo de elementos a incluir en el pool
     * @param random        fuente de aleatoriedad
     * @return lista inmutable de elementos seleccionados
     */
    public static <T> List<T> build(
            Collection<T> candidates,
            Predicate<T> filter,
            java.util.function.Function<T, ItemRarity> rarityMapper,
            int maxCount,
            Random random) {

        return build(candidates, filter, rarityMapper, maxCount, random, 200);
    }

    /**
     * Construye un pool de oferta con selección ponderada por rareza.
     *
     * @param <T>           tipo de elemento del pool
     * @param candidates    colección de candidatos potenciales
     * @param filter        filtro para determinar elegibilidad
     * @param rarityMapper  función que obtiene la rareza de cada candidato
     * @param maxCount      máximo de elementos a incluir en el pool
     * @param random        fuente de aleatoriedad
     * @param maxAttempts   máximo de intentos para evitar loops infinitos
     * @return lista inmutable de elementos seleccionados
     */
    public static <T> List<T> build(
            Collection<T> candidates,
            Predicate<T> filter,
            java.util.function.Function<T, ItemRarity> rarityMapper,
            int maxCount,
            Random random,
            int maxAttempts) {

        if (candidates == null || candidates.isEmpty())
            return Collections.emptyList();
        if (maxCount <= 0)
            return Collections.emptyList();

        // Fase 1: filtrar candidatos elegibles
        List<T> eligible = new ArrayList<>();
        for (T candidate : candidates) {
            if (filter.test(candidate)) {
                eligible.add(candidate);
            }
        }

        if (eligible.isEmpty())
            return Collections.emptyList();

        // Fase 2: calcular peso total
        int totalWeight = 0;
        for (T candidate : eligible) {
            ItemRarity rarity = rarityMapper.apply(candidate);
            if (rarity != null) {
                totalWeight += rarity.weight;
            }
        }

        if (totalWeight <= 0)
            return Collections.emptyList();

        // Fase 3: selección ponderada (ruleta)
        List<T> result = new ArrayList<>();
        Set<T> selected = new HashSet<>();

        int attempts = 0;
        while (result.size() < maxCount &&
               result.size() < eligible.size() &&
               attempts < maxAttempts) {

            attempts++;

            // Tirar dado ponderado
            int roll = random.nextInt(totalWeight);
            int acc = 0;

            for (T candidate : eligible) {
                ItemRarity rarity = rarityMapper.apply(candidate);
                if (rarity == null) continue;

                acc += rarity.weight;
                if (roll < acc && !selected.contains(candidate)) {
                    result.add(candidate);
                    selected.add(candidate);
                    break;
                }
            }
        }

        return Collections.unmodifiableList(result);
    }

    /**
     * Variante simplificada cuando la rareza está en el mismo objeto.
     *
     * Útil para tipos que implementan una interfaz común de rareza.
     *
     * @param <T>        tipo de elemento que tiene rareza
     * @param candidates colección de candidatos
     * @param filter     filtro de elegibilidad
     * @param maxCount   máximo de elementos
     * @param random     fuente de aleatoriedad
     * @return lista inmutable de elementos seleccionados
     */
    public static <T extends HasRarity> List<T> buildSimple(
            Collection<T> candidates,
            Predicate<T> filter,
            int maxCount,
            Random random) {

        return build(candidates, filter, HasRarity::getRarity, maxCount, random);
    }

    /**
     * Interfaz marcador para tipos que exponen su rareza directamente.
     * Permite usar buildSimple() sin lambda.
     */
    public interface HasRarity {
        ItemRarity getRarity();
    }
}
