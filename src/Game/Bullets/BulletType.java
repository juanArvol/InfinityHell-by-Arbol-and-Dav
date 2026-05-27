package Game.Bullets;

import Game.Bullets.BulletComport.BulletBehavior;
import Game.Bullets.BulletComport.BulletClass.*;
import Game.Items.ItemRarity;

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
    VOID_SHARD   (BulletNormal::new, ItemRarity.COMMON,
                  "Esquirla del Vacío",
                  "Un fragmento de energía pura, sin forma ni afinidad."),

    /** La bala impacta al enemigo y genera un impulso de salto. */
    SPRINGBULLET   (BulletJump::new,   ItemRarity.UNCOMMON,
                  "Tiro Resorte",
                  "Al impactar, libera un estallido cinético que catapulta al objetivo."),

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
}
