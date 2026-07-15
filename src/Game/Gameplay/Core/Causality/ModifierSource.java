package Game.Gameplay.Core.Causality;

/**
 * Abstracción del origen de un modificador.
 *
 * ── RESPONSABILIDAD ÚNICA ─────────────────────────────────────────────────
 * ModifierSource responde a una sola pregunta:
 *
 *   "¿De dónde proviene este modificador?"
 *
 * No representa entidades del juego.
 * No contiene lógica de gameplay.
 * No modifica propiedades.
 * No describe efectos.
 *
 * Su única responsabilidad es declarar el origen conceptual de una
 * modificación para que el pipeline de resolución, los predicados y las
 * influencias puedan operar sobre esa información.
 *
 * ── POR QUÉ ES UNA INTERFAZ ───────────────────────────────────────────────
 * ModifierSource es una interfaz para seguir el mismo principio de
 * extensión que GameplayTag y GameplayCapability: los sistemas futuros
 * declaran sus propias fuentes en sus propios catálogos sin modificar
 * el núcleo.
 *
 *   // En un módulo de armas:
 *   public final class WeaponSources {
 *       public static final ModifierSource WEAPON = () -> "Weapon";
 *       public static final ModifierSource WEAPON_PASSIVE = () -> "Weapon.Passive";
 *   }
 *
 *   // En un módulo de hechizos:
 *   public final class SpellSources {
 *       public static final ModifierSource SPELL = () -> "Spell";
 *       public static final ModifierSource SPELL_AREA = () -> "Spell.Area";
 *   }
 *
 * ── CATÁLOGO DE EJEMPLOS (NO IMPLEMENTADOS) ───────────────────────────────
 * Los siguientes son ejemplos conceptuales de fuentes que existirán en el
 * juego. NINGUNO se implementa aquí — solo la infraestructura.
 *
 *   Weapon, Spell, Amulet, Creature, StatusEffect, Environment,
 *   Player, Enemy, Fusion, Ultimate, HyperCharge, Passive,
 *   Buff, Debuff
 *
 * ── IDENTIDAD ────────────────────────────────────────────────────────────
 * Dos ModifierSource son conceptualmente iguales si tienen el mismo id().
 * Usar constantes estáticas en catálogos para garantizar reutilización.
 *
 * ── NAMING ────────────────────────────────────────────────────────────────
 * Los IDs siguen la misma notación de punto que GameplayTag:
 *   "Weapon", "Weapon.Ranged", "Spell.Fire", "Environment.Trap"
 */
public interface ModifierSource {

    /**
     * Identificador único del origen en notación de punto.
     *
     * Ejemplos:
     *   "Weapon"
     *   "Weapon.Ranged"
     *   "Spell"
     *   "Spell.Fire"
     *   "Amulet"
     *   "Player.Passive"
     *   "Environment"
     *
     * @return identificador, nunca null ni vacío.
     */
    String id();

    /**
     * True si este origen es exactamente el candidato, o si el ID del
     * candidato es un prefijo de este ID en la jerarquía de punto.
     *
     * Ejemplos:
     *   "Weapon.Ranged".isOrDescendantOf("Weapon")     → true
     *   "Spell.Fire".isOrDescendantOf("Spell")         → true
     *   "Spell.Fire".isOrDescendantOf("Weapon")        → false
     *   "Weapon".isOrDescendantOf("Weapon")            → true
     *
     * La implementación por defecto resuelve la jerarquía mediante el
     * prefijo de notación de punto, sin necesidad de una estructura
     * de árbol centralizada.
     *
     * @param candidate fuente ancestro a buscar
     * @return true si este origen es o desciende del candidato
     */
    default boolean isOrDescendantOf(ModifierSource candidate) {
        if (candidate == null) return false;
        String thisId  = this.id();
        String candId  = candidate.id();
        if (thisId.equals(candId)) return true;
        // Comprueba el prefijo con separador de punto para evitar falsos positivos
        // ej: "WeaponPassive" no es descendiente de "Weapon"
        return thisId.startsWith(candId + ".");
    }
}
