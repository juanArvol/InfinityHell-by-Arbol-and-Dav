package Game.Items.Types.Bullets.Definition;

import Game.Items.Creation.ItemRarity;
import Game.Items.Types.Bullets.BulletComport.BulletBehavior;
import Game.Items.Types.Bullets.BulletComport.BulletClass.*;
import java.util.*;
import java.util.function.Supplier;

/**
 * Tipos de bala — efectos únicos que se obtienen una sola vez por run.
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
 * 1. Crear la clase de behavior en BulletComport/BulletClass/.
 * 2. Añadir la entrada aquí con su rareza por defecto.
 * 3. Nada más — el resto del sistema lo recoge automáticamente.
 */
public enum BulletType {

    // ── Tipos base ────────────────────────────────────────────────────────

    /** Proyectil estándar — base para el arma de inicio. */
    NORMALBULLET   (BulletNormal::new, ItemRarity.COMMON,
                  "Esquirla del Vacío",
                  "Un fragmento de energía pura, sin forma ni afinidad."),

    /** La bala impacta al enemigo y genera un impulso de salto. */
    SPRINGBULLET   (BulletJump::new,   ItemRarity.UNCOMMON,
                  "Bala saltarina",
                  "Al impactar en el suelo u objetos esta bala continuara rebotando hasta impactar contra un enemy."),

    // ── Efectos elementales (pendientes de implementar sus behaviors) ─────
    // Descomenta cada uno cuando crees la clase de behavior correspondiente.

    /*
    FROSTBOLT    (FrostBullet::new,   ItemRarity.UNCOMMON,
                  "Rayo de Escarcha",
                  "Congela brevemente al enemigo, reduciéndole la velocidad."),

    EMBERSHARD   (EmberBullet::new,   ItemRarity.UNCOMMON,
                  "Fragmento Ascua",
                  "Prende fuego al objetivo; el daño continúa después del impacto."),

    ARCLANCE     (ThunderBullet::new, ItemRarity.RARE,
                  "Lanza de Arco",
                  "Un rayo que salta entre enemigos cercanos al impactar."),

    VOIDMETEOR   (MetheorBullet::new, ItemRarity.RARE,
                  "Meteoro del Vacío",
                  "Proyectil de alta masa que ignora la física normal."),

    PHANTOM_WISP (SummonBullet::new,  ItemRarity.EPIC,
                  "Fuego Fantasma",
                  "Invoca un espectro que persigue enemigos durante unos segundos."),
    */
    ;

    // ── Datos del tipo ────────────────────────────────────────────────────

    private final Supplier<BulletBehavior> factory;

    /** Rareza por defecto. Puede sobreescribirse desde configuración externa. */
    public final ItemRarity defaultRarity;

    /** Nombre visible al jugador en UI de recompensa/tienda. */
    public final String displayName;

    /** Descripción del efecto para la UI de selección. */
    public final String description;

    BulletType(Supplier<BulletBehavior> factory,
               ItemRarity defaultRarity,
               String displayName,
               String description) {
        this.factory        = factory;
        this.defaultRarity  = defaultRarity;
        this.displayName    = displayName;
        this.description    = description;
    }

    /**
     * Crea una nueva instancia del BulletBehavior asociado.
     * Cada bala individual tiene su propia instancia (con estado propio).
     */
    public BulletBehavior create() {
        return factory.get();
    }

    // ── Pool de oferta ────────────────────────────────────────────────────

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
     * @param alreadyOwned tipos que el jugador ya posee en esta run
     * @param maxCount     máximo de opciones a ofrecer
     * @param random       fuente de aleatoriedad
     * @return lista inmutable de BulletTypes disponibles (ya filtrados y seleccionados)
     */
    public static List<BulletType> buildOfferPool(
            Set<BulletType> alreadyOwned, int maxCount, Random random) {

        // Pool de candidatos: todos los tipos que el jugador aún no tiene
        List<BulletType> candidates = new ArrayList<>();
        for (BulletType bt : BulletType.values()) {
            if (!alreadyOwned.contains(bt)) {
                candidates.add(bt);
            }
        }
        if (candidates.isEmpty()) return List.of();

        // Selección ponderada por rareza (ruleta)
        int totalWeight = candidates.stream()
            .mapToInt(bt -> bt.defaultRarity.weight)
            .sum();

        List<BulletType> result = new ArrayList<>();
        Set<BulletType> selected = new HashSet<>();

        int attempts = 0;
        while (result.size() < maxCount && result.size() < candidates.size() && attempts < 100) {
            attempts++;
            int roll = random.nextInt(totalWeight);
            int acc  = 0;
            for (BulletType bt : candidates) {
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
