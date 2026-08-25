package Game.Items.Types.Ammulets;

import Game.Items.Core.ObjectTypeFactory;
import Game.Items.Creation.ItemRarity;
import Game.Items.Types.Bullets.BulletComport.BulletBehavior;
import Game.Items.Types.Weapons.WeaponType.WeaponStats;
import java.util.*;

/**
 * Registro central de amuletos y sistema de aplicación acumulativa.
 *
 * ── HRFC — Items Module Architectural Consolidation ──────────────────────
 *
 * MIGRACIÓN COMPLETADA:
 *   - Los tipos de amuleto ahora se declaran en AmuletType (patrón BulletType)
 *   - AmuletRegistry mantiene solo: rarityOverrides y entityProvider
 *   - Eliminada duplicación de registro/storage (ahora en ObjectTypeFactory)
 *   - Mantenida compatibilidad con código existente via métodos adaptadores
 *
 * ── LIFECYCLE: SINGLETON DE APLICACIÓN CON ESTADO DE SCOPE WORLD ─────────
 *
 * AmuletRegistry tiene dos responsabilidades con lifecycle diferente:
 *
 *   1. RAREZA OVERRIDE (scope de aplicación):
 *      rarityOverrides — permite ajustar frecuencias desde configuración externa.
 *      No cambia entre partidas normalmente.
 *
 *   2. ENTITY PROVIDER (scope de World):
 *      entityProvider — referencia al proveedor de entidades del mundo activo.
 *      Debe inyectarse cuando el World arranca (GameWorldBootstrap) y limpiarse
 *      cuando el World muere (GameWorldBootstrap.shutdown()).
 *
 * DECISIÓN ARQUITECTÓNICA PARA EL ENTITY PROVIDER:
 *   AmuletRegistry necesita acceso a entidades vivas para amuletos como
 *   BounceAmuletWrapper (busca enemigos cercanos al impactar). Este acceso
 *   se hace via Supplier para no acoplar el registry a WorldManager.
 *
 *   El Supplier tiene lifecycle de World: si el World se destruye y el Supplier
 *   no se limpia, AmuletRegistry retiene una referencia a un WorldManager
 *   destruido. Por eso GameWorldBootstrap.shutdown() llama:
 *     AmuletRegistry.setEntityProvider(null);
 *   Esto restaura el provider a un Supplier vacío seguro (List::of).
 *
 * SIN LISTENERS EN GAMEVENTBUS:
 *   AmuletRegistry no instala listeners en GameEventBus. No necesita
 *   Subscription ni cleanup del bus. Su única dependencia de scope World
 *   es el entityProvider, gestionado explícitamente via setter.
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
 *  1. GameState.init() → AmuletType static initializers ejecutan
 *  2. GameWorldBootstrap → AmuletRegistry.setEntityProvider(worldSupplier)
 *  3. Loot/tienda → AmuletType.buildOfferPool(count, random)
 *  4. Jugador recoge → PlayerAmulets.add(amuletType)
 *  5. Al disparar → AmuletEffectApplicator.applyAll(playerAmulets, stats, behavior)
 *  6. Al destruir el World → AmuletRegistry.setEntityProvider(null)
 *
 * ── RAREZA CONFIGURABLE ──────────────────────────────────────────────────
 * Igual que WeaponRegistry: overrideRarity() permite al diseñador ajustar
 * frecuencias desde configuración externa sin recompilar.
 *
 * @deprecated Los métodos de registro y consulta ahora se encuentran en AmuletType.
 *             Este registry mantiene solo rarityOverrides y entityProvider.
 */
@Deprecated
public final class AmuletRegistry {

    private static AmuletRegistry instance;

    private final Map<String, ItemRarity> rarityOverrides   = new HashMap<>();

    /**
     * Proveedor de entidades del mundo activo.
     * Inyectado desde GameWorldBootstrap cuando el mundo está disponible.
     * Usado por amuletos como BounceAmuletWrapper que necesitan buscar objetivos.
     * Null hasta que se llame setEntityProvider().
     */
    private java.util.function.Supplier<java.util.List<? extends Game.Engine.AbstractEntity>> entityProvider = java.util.List::of;

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
     * Inyecta el proveedor de entidades del mundo activo.
     *
     * Necesario para amuletos que buscan entidades cercanas (BounceAmuletWrapper).
     * Llamar desde GameWorldBootstrap después de crear el mundo y el player.
     *
     * @param provider proveedor que retorna la lista de AbstractEntity del mundo actual
     */
    public static void setEntityProvider(
            java.util.function.Supplier<java.util.List<? extends Game.Engine.AbstractEntity>> provider) {
        getInstance().entityProvider = (provider != null) ? provider : java.util.List::of;
    }

    /**
     * Proveedor de entidades activo.
     * Los amuletos que lo necesitan lo leen desde aquí.
     */
    public static java.util.function.Supplier<java.util.List<? extends Game.Engine.AbstractEntity>>
            getEntityProvider() {
        return getInstance().entityProvider;
    }

    /**
     * @deprecated Los tipos de amuleto ahora se registran en AmuletType via static initializers.
     *             Este método mantiene compatibilidad pero ya no es necesario.
     */
    @Deprecated
    public static void registerDefaults() {
        // No-op: los amuletos se declaran ahora en AmuletType static block
        // Mantenido para compatibilidad con código existente que llama a este método
    }

    // ── API de compatibilidad (delegación a AmuletType) ───────────────────

    /**
     * @deprecated Los tipos de amuleto ahora se registran en AmuletType via static initializers.
     *             Este método mantiene compatibilidad pero ya no es necesario.
     */
    @Deprecated
    public static void register(Game.Items.Creation.ItemDefinition def) {
        // No-op: definitions ya no se almacenan aquí
        // Las llamadas legacy se ignoran silenciosamente para evitar romper código existente
    }

    /**
     * Obtiene una definición por ID.
     * 
     * COMPATIBILIDAD: Retorna la ItemDefinition del AmuletType especificado.
     *
     * @param id identificador del amuleto
     * @return definición del amuleto
     * @throws IllegalArgumentException si no existe
     */
    public static Game.Items.Creation.ItemDefinition get(String id) {
        AmuletType type = ObjectTypeFactory.get(AmuletType.class, id);
        return type.getDefinition();
    }

    /**
     * Retorna todas las definiciones registradas.
     * 
     * COMPATIBILIDAD: Convierte AmuletType a AmuletDefinition para código legacy.
     */
    /* public static Collection<AmuletDefinition> all() {
        List<AmuletDefinition> result = new ArrayList<>();
        for (AmuletType type : ObjectTypeFactory.values(AmuletType.class)) {
            result.add(new AmuletDefinition(
                type.getId(),
                type.getDisplayName(),
                type.getDescription(),
                type.getRarity(),
                type.createEffect()
            ));
        }
        return Collections.unmodifiableList(result);
    } */

    public static void overrideRarity(String amuletId, ItemRarity rarity) {
        getInstance().rarityOverrides.put(amuletId, rarity);
    }

    public static ItemRarity getRarity(String amuletId) {
        AmuletRegistry reg = getInstance();
        ItemRarity override = reg.rarityOverrides.get(amuletId);
        if (override != null) return override;
        
        // Buscar en ObjectTypeFactory
        AmuletType type = ObjectTypeFactory.find(AmuletType.class, amuletId);
        return type != null ? type.getRarity() : ItemRarity.COMMON;
    }

    /**
     * Construye un pool de amuletos ofrecidos al jugador.
     * 
     * COMPATIBILIDAD: Delega a ObjectTypeFactory.buildOfferPool()
     *
     * @param maxCount máximo de opciones a ofrecer
     * @param random   fuente de aleatoriedad
     * @return lista inmutable de definiciones de amuletos
     */
    /* public static List<AmuletDefinition> buildOfferPool(int maxCount, Random random) {
        List<AmuletType> types = ObjectTypeFactory.buildOfferPool(
            AmuletType.class,
            type -> true,  // todos son candidatos
            maxCount,
            random
        );
        List<AmuletDefinition> result = new ArrayList<>();
        for (AmuletType type : types) {
            result.add(new AmuletDefinition(
                type.getId(),
                type.getDisplayName(),
                type.getDescription(),
                type.getRarity(),
                type.createEffect()
            ));
        }
        return Collections.unmodifiableList(result);
    } */

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
    /**
     * Aplica todos los amuletos del jugador (con apilamiento) a un WeaponStats
     * y envuelve el BulletBehavior.
     *
     * COMPATIBILIDAD: Convierte definiciones a tipos y delega a AmuletEffectApplicator
     *
     * @param ownedAmulets definiciones de amuletos que posee el jugador
     * @param stats        copia mutable de WeaponStats a modificar
     * @param behavior     behavior base a envolver
     * @return behavior con todos los efectos de amuleto aplicados
     */
    /* public static BulletBehavior applyAllFromDefinitions(
            List<AmuletDefinition> ownedAmulets,
            WeaponStats stats,
            BulletBehavior behavior) {

        // Convertir definiciones a tipos
        List<AmuletType> types = new ArrayList<>();
        for (AmuletDefinition def : ownedAmulets) {
            AmuletType type = ObjectTypeFactory.find(AmuletType.class, def.getId());
            if (type != null) {
                types.add(type);
            }
        }
        
        return AmuletEffectApplicator.applyAll(types, stats, behavior);
    } */

    /**
     * Aplica todos los amuletos del jugador (con apilamiento) a un WeaponStats
     * y envuelve el BulletBehavior.
     *
     * COMPATIBILIDAD: Convierte IDs a tipos y delega a AmuletEffectApplicator
     *
     * @param ownedAmulets IDs de amuletos que posee el jugador
     * @param stats        copia mutable de WeaponStats a modificar
     * @param behavior     behavior base a envolver
     * @return behavior con todos los efectos de amuleto aplicados
     */
    public static BulletBehavior applyAll(
            List<String> ownedAmulets,
            WeaponStats stats,
            BulletBehavior behavior) {

        // Convertir IDs a tipos
        List<AmuletType> types = new ArrayList<>();
        for (String id : ownedAmulets) {
            AmuletType type = ObjectTypeFactory.find(AmuletType.class, id);
            if (type != null) {
                types.add(type);
            }
        }
        
        return AmuletEffectApplicator.applyAll(types, stats, behavior);
    }
}
