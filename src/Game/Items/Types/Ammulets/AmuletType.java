package Game.Items.Types.Ammulets;

import Game.Items.Core.ItemTypeRegistry;
import Game.Items.Core.ObjectType;
import Game.Items.Core.OfferPoolBuilder;
import Game.Items.ItemRarity;
import Game.Items.Types.Ammulets.Effects.*;
import Game.Items.Types.Bullets.BulletComport.BulletBehavior;
import Game.Items.Types.Weapons.WeaponType.WeaponStats;
import java.util.*;
import java.util.function.Supplier;

/**
 * Tipos de amuleto — mejoras pasivas acumulables al estilo Hollow Knight.
 *
 * ── HRFC — Items Module Architectural Consolidation ──────────────────────
 *
 * MIGRACIÓN COMPLETADA:
 *   - Creación de AmuletType siguiendo el patrón de BulletType/WeaponType
 *   - Extiende ItemTypeBase<AmuletEffect>
 *   - Usa ItemTypeRegistry para almacenamiento centralizado
 *   - Usa OfferPoolBuilder para selección ponderada
 *   - Reemplaza AmuletRegistry singleton por patrón declarativo
 *
 * DIFERENCIA CLAVE respecto a Weapons y Bullets:
 *   • Armas y BulletTypes → únicos por run (no repiten)
 *   • Amuletos → pueden aparecer MÚLTIPLES VECES de forma aleatoria
 *     Cada copia suma su efecto (igual que en Hollow Knight o Binding of Isaac)
 *
 * ── DISEÑO ───────────────────────────────────────────────────────────────
 * Cada AmuletType representa una mejora pasiva que se aplica a las estadísticas
 * de las balas del jugador. Los amuletos son ADITIVOS por defecto; múltiples
 * copias suman.
 *
 * Ejemplo:
 *   "Punta Ósea" x1: +8 daño
 *   "Punta Ósea" x3: +24 daño
 *
 * ── RAREZA ────────────────────────────────────────────────────────────────
 * La rareza controla la frecuencia en el pool de oferta pero NO limita
 * cuántas veces puede aparecer. Un amuleto EPIC puede caer 5 veces en una run.
 *
 * ── AÑADIR UN NUEVO AMULETO ───────────────────────────────────────────────
 * ANTES (AmuletRegistry.registerDefaults()):
 *   1. Modificar AmuletRegistry.java
 *   2. Añadir entrada en registerDefaults()
 *
 * AHORA (class extensible con infraestructura unificada):
 *   1. Crear el AmuletEffect en tu módulo
 *   2. Registrar en tu módulo:
 *      AmuletType.register(new AmuletType("bone_tip", () -> new DamageEffect(8), ...))
 *   3. ¡Listo! — AmuletType.java NO se modifica
 *
 * @see Game.Items.Creation.ObjectType  clase base unificada
 * @see Game.Items.Creation.ItemTypeRegistry  registro centralizado
 * @see AmuletEffect  interfaz del efecto aplicado
 */
public final class AmuletType extends ObjectType<AmuletEffect> {

    // ── Tipos predefinidos ────────────────────────────────────────────────

    public static final AmuletType BONE_TIP;
    public static final AmuletType MARKSMAN_SIGHT;
    public static final AmuletType SWIFT_QUILL;
    public static final AmuletType TEMPO_RING;
    public static final AmuletType STEADY_GRIP;
    public static final AmuletType PHASE_SHARD;
    public static final AmuletType ECHO_STONE;
    public static final AmuletType SPLIT_CRYSTAL;

    static {
        // ── Daño ──────────────────────────────────────────────────────────

        BONE_TIP = register(new AmuletType(
            "bone_tip",
            () -> new AmuletEffect() {
                @Override
                public void applyToStats(WeaponStats stats) {
                    stats.setDamageBonusByWeapon(stats.getDamageBonusByWeapon() + 8.0);
                }
            },
            ItemRarity.COMMON,
            "Punta Ósea",
            "+8 de daño por proyectil. Acumulable."
        ));

        // ── UI / Visualización ────────────────────────────────────────────

        MARKSMAN_SIGHT = register(new AmuletType(
            "marksman_sight",
            () -> new UICapabilityEffect(
                () -> new Game.Gameplay.UI.Aim.TrajectoryVisualizationCapability(
                    Game.Gameplay.UI.Aim.TrajectoryVisualizationCapability.TrajectoryStyle.FADE,
                    java.awt.Color.CYAN
                )
            ),
            ItemRarity.EPIC,
            "Ojo del Tirador",
            "Revela la trayectoria completa de tus disparos, incluyendo información del arma y predicción de impactos. El primer amuleto visual del juego."
        ));

        // ── Velocidad de proyectil / alcance ──────────────────────────────

        SWIFT_QUILL = register(new AmuletType(
            "swift_quill",
            () -> new AmuletEffect() {
                @Override
                public void applyToStats(WeaponStats stats) {
                    stats.setBulletSpeedBase(stats.getBulletSpeedBase() * 1.15);
                }
            },
            ItemRarity.COMMON,
            "Pluma Veloz",
            "+15% velocidad de proyectil (aumenta alcance efectivo). Acumulable."
        ));

        // ── Cadencia ──────────────────────────────────────────────────────

        TEMPO_RING = register(new AmuletType(
            "tempo_ring",
            () -> new AmuletEffect() {
                @Override
                public void applyToStats(WeaponStats stats) {
                    stats.setCooldown(stats.getCooldown() * 0.90);
                }
            },
            ItemRarity.UNCOMMON,
            "Anillo de Tempo",
            "-10% cooldown de disparo. Acumulable."
        ));

        // ── Dispersión ────────────────────────────────────────────────────

        STEADY_GRIP = register(new AmuletType(
            "steady_grip",
            () -> new AmuletEffect() {
                @Override
                public void applyToStats(WeaponStats stats) {
                    stats.setSpread(stats.getSpread() * 0.80);
                }
            },
            ItemRarity.UNCOMMON,
            "Empuñadura Firme",
            "-20% dispersión de proyectiles. Acumulable."
        ));

        // ── Perforación ───────────────────────────────────────────────────

        PHASE_SHARD = register(new AmuletType(
            "phase_shard",
            () -> new AmuletEffect() {
                @Override
                public BulletBehavior wrapBehavior(BulletBehavior base) {
                    return new PiercingAmuletWrapper(base, 1);
                }
            },
            ItemRarity.RARE,
            "Esquirla de Fase",
            "Los proyectiles perforan +1 enemigo adicional. Acumulable."
        ));

        // ── Rebote entre enemigos ─────────────────────────────────────────

        ECHO_STONE = register(new AmuletType(
            "echo_stone",
            () -> new AmuletEffect() {
                @Override
                public BulletBehavior wrapBehavior(BulletBehavior base) {
                    return new BounceAmuletWrapper(
                        base, 1, AmuletRegistry.getEntityProvider()
                    );
                }
            },
            ItemRarity.RARE,
            "Piedra del Eco",
            "Al impactar, el proyectil salta a un enemigo cercano (+1 salto). Acumulable."
        ));

        // ── Multi-proyectil ───────────────────────────────────────────────

        SPLIT_CRYSTAL = register(new AmuletType(
            "split_crystal",
            () -> new AmuletEffect() {
                @Override
                public void applyToStats(WeaponStats stats) {
                    stats.setBulletsPerShot(stats.getBulletsPerShot() + 1);
                }
            },
            ItemRarity.EPIC,
            "Cristal Partido",
            "+1 proyectil por disparo. Acumulable."
        ));
    }

    // ── Constructor (público para extensibilidad) ─────────────────────────

    /**
     * Construye un nuevo AmuletType.
     *
     * @param id             identificador único (snake_case)
     * @param factory        factory que crea AmuletEffect
     * @param defaultRarity  rareza por defecto
     * @param displayName    nombre visible
     * @param description    descripción del efecto
     */
    public AmuletType(String id,
                      Supplier<AmuletEffect> factory,
                      ItemRarity defaultRarity,
                      String displayName,
                      String description) {
        super(id, factory, defaultRarity, displayName, description);
    }

    // ── API pública ───────────────────────────────────────────────────────

    /**
     * Crea una nueva instancia del AmuletEffect asociado.
     *
     * @return nueva instancia del efecto. Nunca null.
     */
    public AmuletEffect createEffect() {
        return createInstance();
    }

    // ── Registro y consulta (delegación a infraestructura unificada) ──────

    /**
     * Registra un nuevo tipo de amuleto.
     *
     * @param type tipo a registrar
     * @return el mismo tipo (para asignación en constantes)
     * @throws IllegalStateException si el ID ya está registrado
     */
    public static AmuletType register(AmuletType type) {
        return ItemTypeRegistry.register(AmuletType.class, type);
    }

    /**
     * Obtiene un tipo por su ID.
     *
     * @param id identificador del tipo
     * @return el tipo correspondiente
     * @throws IllegalArgumentException si no existe
     */
    public static AmuletType get(String id) {
        return ItemTypeRegistry.get(AmuletType.class, id);
    }

    /**
     * Busca un tipo por su ID sin lanzar excepción.
     *
     * @param id identificador del tipo
     * @return el tipo correspondiente, o null si no existe
     */
    public static AmuletType find(String id) {
        return ItemTypeRegistry.find(AmuletType.class, id);
    }

    /**
     * Verifica si existe un tipo con el ID dado.
     */
    public static boolean has(String id) {
        return ItemTypeRegistry.has(AmuletType.class, id);
    }

    /**
     * Retorna todos los tipos registrados.
     *
     * @return colección inmutable de todos los tipos
     */
    public static Collection<AmuletType> values() {
        return ItemTypeRegistry.values(AmuletType.class);
    }

    // ── Pool de oferta (delegación a infraestructura unificada) ───────────

    /**
     * Construye un pool de amuletos ofrecidos al jugador.
     *
     * A diferencia de armas y balas, los amuletos SIEMPRE están disponibles:
     * no se filtra por "ya obtenidos". El pool puede devolver el mismo amuleto
     * que ya tiene el jugador — eso es intencional (apilamiento).
     *
     * @param maxCount máximo de opciones a ofrecer
     * @param random   fuente de aleatoriedad
     * @return lista inmutable de AmuletTypes seleccionados
     */
    public static List<AmuletType> buildOfferPool(int maxCount, Random random) {
        return OfferPoolBuilder.build(
            values(),
            type -> true,  // sin filtro — todos siempre disponibles
            type -> AmuletRegistry.getRarity(type.id),  // permite override de rareza
            maxCount,
            random
        );
    }

    /**
     * Aplica todos los amuletos poseídos sobre las estadísticas y comportamiento.
     *
     * @param ownedAmulets tipos de amuletos que posee el jugador (puede repetirse)
     * @param stats        estadísticas mutables a modificar
     * @param behavior     comportamiento base a envolver
     * @return behavior con todos los efectos de amuleto aplicados
     */
    public static BulletBehavior applyAll(
            List<AmuletType> ownedAmulets,
            WeaponStats stats,
            BulletBehavior behavior) {

        for (AmuletType type : ownedAmulets) {
            AmuletEffect effect = type.createEffect();
            effect.applyToStats(stats);
            behavior = effect.wrapBehavior(behavior);
        }
        return behavior;
    }
}
