package Game.Items.Types.Ammulets;

import Game.Engine.AbstractEntity;
import Game.Items.Core.ObjectTypeFactory;
import Game.Items.Creation.ItemRarity;
import Game.Items.Creation.ItemRegistry;
import Game.Items.Types.Bullets.BulletComport.BulletBehavior;
import Game.Items.Types.Weapons.WeaponType.WeaponStats;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Registry especializado de amuletos.
 *
 * ── ARQUITECTURA FINAL — Items Module ────────────────────────────────────
 *
 * AmuletRegistry especializa ItemRegistry para AmuletDefinition.
 *
 * ItemRegistry proporciona la infraestructura común de:
 *
 *     register()
 *     get()
 *     find()
 *     has()
 *     getAll()
 *
 * AmuletRegistry solamente contiene comportamiento específico de la familia
 * Amulet que no pertenece a ItemRegistry ni ObjectTypeFactory.
 *
 * RESPONSABILIDADES:
 *
 *     - Overrides de rareza.
 *     - Entity provider del World activo (lazy resolution).
 *     - Coordinación de aplicación de efectos.
 *
 * NO RESPONSABILIDADES:
 *
 *     - Registrar AmuletType.
 *     - Almacenar AmuletType.
 *     - Crear AmuletEffect.
 *     - Crear BulletBehavior directamente.
 *     - Registrar definiciones manualmente.
 *     - Resolver IDs String.
 *
 * AmuletType y ObjectTypeFactory son responsables del sistema de tipos.
 *
 * ── INICIALIZACIÓN AUTOMÁTICA ────────────────────────────────────────────
 *
 * AmuletRegistry ahora usa singleton eager (static final) igual que
 * WeaponType y BulletType, eliminando la necesidad de init() manual.
 *
 * ── ENTITY PROVIDER PATTERN ──────────────────────────────────────────────
 *
 * Algunos efectos de amuleto (como BounceAmuletWrapper) necesitan acceso
 * a las entidades del World activo. En lugar de inyectar el entityProvider
 * en el static block (cuando NO HAY World activo), usamos lazy resolution:
 *
 *   1. AmuletType.ECHO_STONE se define con entityProvider = null
 *   2. GameWorldBootstrap.setEntityProvider() lo configura cuando el World existe
 *   3. BounceAmuletWrapper lo obtiene vía AmuletRegistry.getEntityProvider()
 *
 * Esto resuelve el problema de inicialización circular: AmuletType puede
 * definirse sin World, y el proveedor se inyecta después.
 */
public final class AmuletRegistry
        extends ItemRegistry<AmuletDefinition> {

    private static final AmuletRegistry INSTANCE = new AmuletRegistry();

    /**
     * Overrides de rareza específicos de amuletos.
     *
     * La rareza base continúa perteneciendo a AmuletDefinition.
     */
    private final Map<AmuletID, ItemRarity> rarityOverrides =
            new HashMap<>();

    /**
     * Proveedor de entidades del World activo.
     *
     * Los efectos que necesiten consultar entidades vivas pueden obtener
     * el proveedor desde este registry sin acoplarse a WorldManager.
     *
     * Se inicializa con List::of (proveedor vacío seguro) y se actualiza
     * desde GameWorldBootstrap cuando el World está activo.
     */
    private Supplier<List<? extends AbstractEntity>> entityProvider =
            List::of;

    // ── Constructor ───────────────────────────────────────────────────────

    private AmuletRegistry() {
    }

    // ── Singleton ─────────────────────────────────────────────────────────

    public static AmuletRegistry getInstance() {
        return INSTANCE;
    }

    // ── Entity Provider ───────────────────────────────────────────────────

    /**
     * Inyecta el proveedor de entidades del World activo.
     *
     * null restaura un proveedor vacío seguro.
     */
    public static void setEntityProvider(
            Supplier<List<? extends AbstractEntity>> provider
    ) {

        getInstance().entityProvider =
                provider != null
                        ? provider
                        : List::of;
    }

    /**
     * Obtiene el proveedor de entidades del World activo.
     */
    public static Supplier<List<? extends AbstractEntity>>
    getEntityProvider() {

        return getInstance().entityProvider;
    }

    // ── Rareza ─────────────────────────────────────────────────────────────

    /**
     * Sobrescribe la rareza efectiva de un amuleto.
     *
     * No modifica la AmuletDefinition original.
     */
    public static void overrideRarity(
            AmuletID amuletId,
            ItemRarity rarity
    ) {

        if (amuletId == null) {
            throw new IllegalArgumentException(
                    "amuletId no puede ser null"
            );
        }

        if (rarity == null) {
            throw new IllegalArgumentException(
                    "rarity no puede ser null"
            );
        }

        getInstance()
                .rarityOverrides
                .put(amuletId, rarity);
    }

    /**
     * Obtiene la rareza efectiva del amuleto.
     *
     * Primero consulta un posible override.
     * Si no existe, utiliza la rareza de AmuletDefinition.
     */
    public static ItemRarity getRarity(
            AmuletID amuletId
    ) {

        if (amuletId == null) {
            throw new IllegalArgumentException(
                    "amuletId no puede ser null"
            );
        }

        AmuletRegistry registry = getInstance();

        ItemRarity override =
                registry.rarityOverrides.get(amuletId);

        if (override != null) {
            return override;
        }

        return registry
                .get(amuletId)
                .getRarity();
    }

    // ── Effect Application ────────────────────────────────────────────────

    /**
     * Aplica todos los amuletos poseídos al WeaponStats y al
     * BulletBehavior.
     *
     * Los amuletos son acumulables, por lo que un mismo AmuletID puede
     * aparecer múltiples veces.
     *
     * Flujo:
     *
     *     AmuletID
     *        ↓
     *     ObjectTypeFactory
     *        ↓
     *     AmuletType
     *        ↓
     *     AmuletEffect
     *        ↓
     *     AmuletEffectApplicator
     */
    public static BulletBehavior applyAll(
            List<AmuletID> ownedAmulets,
            WeaponStats stats,
            BulletBehavior behavior
    ) {

        if (ownedAmulets == null) {
            throw new IllegalArgumentException(
                    "ownedAmulets no puede ser null"
            );
        }

        if (stats == null) {
            throw new IllegalArgumentException(
                    "stats no puede ser null"
            );
        }

        if (behavior == null) {
            throw new IllegalArgumentException(
                    "behavior no puede ser null"
            );
        }

        List<AmuletType> types =
                ownedAmulets.stream()
                        .map(id ->
                                ObjectTypeFactory.find(
                                        AmuletType.class,
                                        id
                                )
                        )
                        .filter(type -> type != null)
                        .toList();

        return AmuletEffectApplicator.applyAll(
                types,
                stats,
                behavior
        );
    }
}