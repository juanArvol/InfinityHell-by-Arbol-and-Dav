package Game.Items.Types.Bullets.Definition;

import Game.Items.Creation.ItemRarity;
import Game.Items.Creation.ItemRegistry;
import Game.Items.Types.Bullets.BulletID;
import Game.Items.Types.Bullets.Definition.BulletDefinition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Registro central de definiciones de balas.
 *
 * ── ARQUITECTURA — Items Module ──────────────────────────────────────────
 *
 * BulletRegistry especializa ItemRegistry para BulletDefinition.
 *
 * ItemRegistry proporciona la infraestructura genérica de registro:
 * - register()
 * - get()
 * - find()
 * - has()
 * - getAll()
 *
 * BulletRegistry solamente contiene lógica específica de la familia Bullet.
 *
 * ── RESPONSABILIDADES ─────────────────────────────────────────────────────
 *
 * - Registrar/consultar BulletDefinition mediante BulletID.
 * - Gestionar overrides de rareza específicos de bullets.
 * - Construir pools de oferta de bullets.
 *
 * NO crea BulletBehavior.
 * NO crea ProjectileBlueprint.
 * NO gestiona ProjectilePool.
 * NO escucha SpawnProjectileEvent.
 *
 * Esas responsabilidades pertenecen a las capas correspondientes.
 */
public final class BulletRegistry
        extends ItemRegistry<BulletDefinition> {

    private static BulletRegistry instance;

    /**
     * Overrides de rareza específicos de bullets.
     *
     * La rareza base continúa perteneciendo a BulletDefinition.
     */
    private final Map<BulletID, ItemRarity> rarityOverrides =
            new HashMap<>();

    // ── Constructor ──────────────────────────────────────────────────────

    private BulletRegistry() {
    }

    // ── Lifecycle ────────────────────────────────────────────────────────

    public static BulletRegistry getInstance() {

        if (instance == null) {
            throw new IllegalStateException(
                    "BulletRegistry no inicializado. " +
                    "Llamá BulletRegistry.init() primero."
            );
        }

        return instance;
    }

    // ── Rareza ───────────────────────────────────────────────────────────

    /**
     * Sobreescribe la rareza efectiva de una bala.
     *
     * No modifica la BulletDefinition original.
     */
    public static void overrideRarity(
            BulletID bulletId,
            ItemRarity rarity
    ) {

        if (bulletId == null) {
            throw new IllegalArgumentException(
                    "bulletId no puede ser null"
            );
        }

        if (rarity == null) {
            throw new IllegalArgumentException(
                    "rarity no puede ser null"
            );
        }

        getInstance()
                .rarityOverrides
                .put(bulletId, rarity);
    }

    /**
     * Obtiene la rareza efectiva de una bala.
     */
    public static ItemRarity getRarity(BulletID bulletId) {

        if (bulletId == null) {
            throw new IllegalArgumentException(
                    "bulletId no puede ser null"
            );
        }

        BulletRegistry registry = getInstance();

        ItemRarity override =
                registry.rarityOverrides.get(bulletId);

        if (override != null) {
            return override;
        }

        return registry.get(bulletId).getRarity();
    }

    // ── Offer Pool ───────────────────────────────────────────────────────

    /**
     * Construye un pool de oferta de bullets.
     *
     * Excluye las balas que el jugador ya posee y selecciona
     * las restantes mediante ponderación por rareza.
     */
    public static List<BulletDefinition> buildOfferPool(
            Set<BulletID> alreadyOwned,
            int maxCount,
            Random random
    ) {

        if (alreadyOwned == null) {
            throw new IllegalArgumentException(
                    "alreadyOwned no puede ser null"
            );
        }

        if (maxCount < 0) {
            throw new IllegalArgumentException(
                    "maxCount no puede ser negativo"
            );
        }

        if (random == null) {
            throw new IllegalArgumentException(
                    "random no puede ser null"
            );
        }

        BulletRegistry registry = getInstance();

        List<BulletDefinition> remaining =
                new ArrayList<>();

        for (BulletDefinition definition : registry.getAll()) {

            if (!alreadyOwned.contains(
                    definition.getBulletId()
            )) {
                remaining.add(definition);
            }
        }

        if (remaining.isEmpty() || maxCount == 0) {
            return List.of();
        }

        List<BulletDefinition> result =
                new ArrayList<>();

        while (
                result.size() < maxCount
                        && !remaining.isEmpty()
        ) {

            int totalWeight =
                    remaining.stream()
                            .mapToInt(definition ->
                                    getRarity(
                                            definition.getBulletId()
                                    ).weight
                            )
                            .sum();

            int roll =
                    random.nextInt(totalWeight);

            int accumulated = 0;

            for (int i = 0; i < remaining.size(); i++) {

                BulletDefinition definition =
                        remaining.get(i);

                accumulated +=
                        getRarity(
                                definition.getBulletId()
                        ).weight;

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
