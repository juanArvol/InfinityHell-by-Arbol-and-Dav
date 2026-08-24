package Game.Items.Types.Bullets.Definition;

import Game.Items.Core.ItemTypeRegistry;
import Game.Items.Core.ObjectType;
import Game.Items.Core.OfferPoolBuilder;
import Game.Items.ItemRarity;
import Game.Items.Types.Bullets.BulletComport.BulletBehavior;
import Game.Items.Types.Bullets.BulletComport.BulletClass.*;
import java.util.*;
import java.util.function.Supplier;

/**
 * Tipos de bala — efectos únicos que se obtienen una sola vez por run.
 *
 * ── HRFC — Items Module Architectural Consolidation ──────────────────────
 *
 * MIGRACIÓN COMPLETADA:
 *   - BulletType ahora extiende ItemTypeBase<BulletBehavior>
 *   - Usa ItemTypeRegistry para almacenamiento centralizado
 *   - Usa OfferPoolBuilder para selección ponderada
 *   - Elimina duplicación de infraestructura
 *
 * CONTRATOS PRESERVADOS:
 *   ✅ BulletType.NORMALBULLET — sigue siendo static final accesible
 *   ✅ bulletType.create() — sigue funcionando exactamente igual
 *   ✅ bulletType.defaultRarity — sigue siendo public final
 *   ✅ bulletType.displayName — sigue siendo public final
 *   ✅ bulletType.description — sigue siendo public final
 *   ✅ BulletType.buildOfferPool() — sigue funcionando igual
 *   ✅ BulletType.values() — reemplaza enum.values()
 *   ✅ BulletType.get(id) — resolución por ID
 *
 * ── DISEÑO ───────────────────────────────────────────────────────────────
 * Cada BulletType representa un EFECTO FANTÁSTICO permanente, no munición
 * consumible. Una vez que el jugador lo obtiene, lo tiene para siempre en
 * esa run.
 *
 * La rareza controla con qué frecuencia aparece en el pool de oferta
 * (loot tables, tiendas, recompensas de run). Es configurable desde datos:
 * el campo 'rarity' aquí es el default; puede sobreescribirse desde configs
 * externas sin recompilar.
 *
 * ── REGLA DE UNICIDAD ────────────────────────────────────────────────────
 * BulletType es único por run: el jugador nunca puede tener dos veces el
 * mismo tipo de bala equipado. El sistema de oferta (loot/shop) debe filtrar
 * los ya obtenidos antes de mostrarlos.
 *
 * ── AÑADIR UN NUEVO TIPO ─────────────────────────────────────────────────
 * ANTES (enum cerrado):
 *   1. Modificar BulletType.java
 *   2. Añadir entrada en el enum
 *
 * AHORA (class extensible con infraestructura unificada):
 *   1. Crear la clase de behavior en tu módulo (ej: FrostBullet.java)
 *   2. Registrar en tu módulo:
 *      BulletType.register(new BulletType("frost_bolt", FrostBullet::new, ...))
 *   3. ¡Listo! — BulletType.java NO se modifica
 *
 * @see Game.Items.Core.ObjectType  clase base unificada
 * @see Game.Items.Core.ItemTypeRegistry  registro centralizado
 */
public final class BulletType extends ObjectType<BulletBehavior> {

    // ── Tipos predefinidos ────────────────────────────────────────────────

    /** Tipos predefinidos — constantes para compatibilidad con código existente. */
    public static final BulletType NORMALBULLET;
    public static final BulletType SPRINGBULLET;
    public static final BulletType METHEORBULLET;

    static {
        // Inicializar tipos base — mismo orden que el enum original
        NORMALBULLET = register(new BulletType(
            "normal_bullet",
            BulletNormal::new,
            ItemRarity.COMMON,
            "Bala",
            "Un fragmento de energía pura, sin forma ni afinidad."
        ));

        SPRINGBULLET = register(new BulletType(
            "spring_bullet",
            BulletJump::new,
            ItemRarity.UNCOMMON,
            "Bala saltarina",
            "Al impactar en el suelo u objetos esta bala continuara rebotando hasta impactar contra un enemy."
        ));

        METHEORBULLET = register(new BulletType(
            "meteor_bullet",
            MetheorBullet::new,
            ItemRarity.RARE,
            "Bala nuke",
            "Proyectil de alta masa que ignora la física normal."
        ));
    }

    // ── Constructor (público para extensibilidad) ─────────────────────────

    /**
     * Construye un nuevo BulletType.
     *
     * @param id             identificador único (snake_case)
     * @param factory        factory que crea BulletBehavior
     * @param defaultRarity  rareza por defecto
     * @param displayName    nombre visible
     * @param description    descripción del efecto
     */
    public BulletType(String id,
                      Supplier<BulletBehavior> factory,
                      ItemRarity defaultRarity,
                      String displayName,
                      String description) {
        super(id, factory, defaultRarity, displayName, description);
    }

    // ── API pública — CONTRATOS PRESERVADOS ───────────────────────────────

    /**
     * Crea una nueva instancia del BulletBehavior asociado.
     * Cada bala individual tiene su propia instancia (con estado propio).
     *
     * ✅ CONTRATO PRESERVADO — este método funciona exactamente igual que antes.
     */
    public BulletBehavior create() {
        return createInstance();
    }

    // ── Registro y consulta (delegación a infraestructura unificada) ──────

    /**
     * Registra un nuevo tipo de bala.
     *
     * EXTENSIBILIDAD:
     *   Módulos externos pueden registrar sus propios tipos sin modificar
     *   este archivo. Ejemplo:
     *
     *   BulletType FROSTBOLT = BulletType.register(new BulletType(
     *       "frost_bolt", FrostBullet::new, ItemRarity.UNCOMMON,
     *       "Rayo de Escarcha", "Congela enemigos..."
     *   ));
     *
     * @param type tipo a registrar
     * @return el mismo tipo (para asignación en constantes)
     * @throws IllegalStateException si el ID ya está registrado
     */
    public static BulletType register(BulletType type) {
        return ItemTypeRegistry.register(BulletType.class, type);
    }

    /**
     * Obtiene un tipo por su ID.
     *
     * @param id identificador del tipo
     * @return el tipo correspondiente
     * @throws IllegalArgumentException si no existe
     */
    public static BulletType get(String id) {
        return ItemTypeRegistry.get(BulletType.class, id);
    }

    /**
     * Busca un tipo por su ID sin lanzar excepción.
     *
     * @param id identificador del tipo
     * @return el tipo correspondiente, o null si no existe
     */
    public static BulletType find(String id) {
        return ItemTypeRegistry.find(BulletType.class, id);
    }

    /**
     * Verifica si existe un tipo con el ID dado.
     */
    public static boolean has(String id) {
        return ItemTypeRegistry.has(BulletType.class, id);
    }

    /**
     * Retorna todos los tipos registrados.
     *
     * ✅ CONTRATO PRESERVADO — reemplaza enum.values()
     *
     * @return colección inmutable de todos los tipos
     */
    public static Collection<BulletType> values() {
        return ItemTypeRegistry.values(BulletType.class);
    }

    // ── Pool de oferta (delegación a infraestructura unificada) ───────────

    /**
     * Construye un pool de BulletTypes disponibles (no obtenidos aún),
     * con selección ponderada por rareza.
     *
     * ── OWNERSHIP ─────────────────────────────────────────────────────────
     * Este método pertenece a BulletType porque:
     *   - BulletType conoce todos los tipos existentes (values())
     *   - BulletType posee la autoridad sobre defaultRarity
     *   - La lógica de oferta depende únicamente de información del dominio Bullets
     *
     * Centralizado aquí para que loot y tienda usen la misma lógica.
     * No duplicar este algoritmo.
     *
     * ── ALGORITMO ─────────────────────────────────────────────────────────
     * Usa selección ponderada por ruleta (weighted roulette selection):
     *   1. Filtra tipos ya obtenidos por el jugador
     *   2. Calcula peso total (suma de defaultRarity.weight de candidatos)
     *   3. Selecciona aleatoriamente según peso hasta llenar maxCount
     *   4. Evita duplicados en la misma oferta
     *
     * Implementación delegada a OfferPoolBuilder (infraestructura unificada).
     *
     * @param alreadyOwned tipos que el jugador ya posee en esta run
     * @param maxCount     máximo de opciones a ofrecer
     * @param random       fuente de aleatoriedad
     * @return lista inmutable de BulletTypes disponibles (ya filtrados y seleccionados)
     */
    public static List<BulletType> buildOfferPool(
            Set<BulletType> alreadyOwned, int maxCount, Random random) {

        return OfferPoolBuilder.build(
            values(),
            type -> !alreadyOwned.contains(type),
            type -> type.defaultRarity,
            maxCount,
            random
        );
    }
}
