package Game.Items.Types.Weapons.WeaponType;

import Game.Items.Creation.ItemRarity;
import Game.Items.Types.Weapons.WeaponType.WeaponClass.WeaponPistola;
import Game.Items.Types.Weapons.WeaponType.WeaponClass.WeaponEscopeta;
import java.util.function.Supplier;

/**
 * Tipos de arma — el paradigma declarativo equivalente a BulletType.
 *
 * ── HRFC — Player Reengineering ───────────────────────────────────────────
 *
 * MOTIVACIÓN:
 *   Antes, PlayerCombat recibía directamente un {@code new WeaponPistola()},
 *   acoplando el código de loadout a la clase concreta. Eso viola el mismo
 *   principio que ya se resolvió en proyectiles con BulletType + ProjectileRegistry.
 *
 *   WeaponType introduce el paradigma equivalente para armas:
 *
 *       BulletType.NORMALBULLET  →  WeaponType.PISTOLA
 *       ProjectileRegistry       →  WeaponRegistry
 *       BulletBehavior.create()  →  WeaponType.createComport()
 *
 * ── DISEÑO ────────────────────────────────────────────────────────────────
 *
 * Cada WeaponType representa un arma que el jugador puede obtener en la run.
 * La rareza controla la frecuencia de aparición en loot/tiendas.
 *
 * La construcción concreta (WeaponPistola, WeaponEscopeta) queda centralizada
 * aquí — el resto del código declara solo el tipo:
 *
 *   PlayerLoadout.with(WeaponType.PISTOLA)
 *
 * sin conocer WeaponPistola.
 *
 * ── CÓMO AÑADIR UN ARMA ───────────────────────────────────────────────────
 *
 *   1. Crear la clase WeaponComport en WeaponType/WeaponClass/.
 *   2. Añadir la entrada aquí con su factory, rareza y metadata.
 *   3. Nada más — PlayerLoadout, PlayerAssembler y WeaponRegistry lo recogen.
 *
 * ── RELACIÓN CON WeaponRegistry ───────────────────────────────────────────
 *
 * WeaponRegistry trabaja con IDs de string ("ethereal_revolver") para el
 * sistema de loot/tiendas. WeaponType trabaja con constantes tipadas para
 * el código de loadout y combate. Ambos son válidos y complementarios:
 *
 *   WeaponType  → código de gameplay (loadout, combate, tests)
 *   WeaponRegistry → sistema de loot, tiendas, balance externo
 *
 * WeaponType.createComport() produce el mismo WeaponComport que
 * WeaponRegistry.get(id).createComport() para los armas equivalentes.
 *
 * @see Game.Items.Types.Bullets.Definition.BulletType  el patrón que este enum replica
 * @see Game.Items.Types.Weapons.WeaponRegistry         registro de loot complementario
 */
public enum WeaponType {

    // ── Armas del juego ───────────────────────────────────────────────────

    /**
     * Pistola automática — arma inicial del jugador.
     * Cadencia media, 1 bala por disparo, bajo spread.
     */
    PISTOLA(
        WeaponPistola::new,
        ItemRarity.COMMON,
        "Pistola del Vacío",
        "Un arma forjada con residuos del vacío. Disparo automático, confiable."
    ),

    /**
     * Escopeta — arma de área a corta distancia.
     * Cadencia baja, múltiples balas por disparo, spread alto.
     */
    ESCOPETA(
        WeaponEscopeta::new,
        ItemRarity.UNCOMMON,
        "Dispersor de Esquirlas",
        "Expulsa un cono de fragmentos de energía. Devastadora a corta distancia."
    );

    // ── Datos del tipo ────────────────────────────────────────────────────

    private final Supplier<WeaponComport> factory;

    /** Rareza por defecto. Puede sobreescribirse desde configuración externa. */
    public final ItemRarity defaultRarity;

    /** Nombre visible al jugador en UI de recompensa/tienda. */
    public final String displayName;

    /** Descripción del comportamiento para la UI de selección. */
    public final String description;

    WeaponType(Supplier<WeaponComport> factory,
               ItemRarity defaultRarity,
               String displayName,
               String description) {
        this.factory       = factory;
        this.defaultRarity = defaultRarity;
        this.displayName   = displayName;
        this.description   = description;
    }

    /**
     * Crea una nueva instancia del WeaponComport asociado a este tipo.
     *
     * <p>Cada arma equipada por el jugador tiene su propia instancia —
     * con su cooldown, ammo y estado de recarga independientes.
     *
     * @return nueva instancia del WeaponComport. Nunca null.
     */
    public WeaponComport createComport() {
        return factory.get();
    }
}
