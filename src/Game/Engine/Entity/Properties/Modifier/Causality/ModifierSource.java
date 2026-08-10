package Game.Engine.Entity.Properties.Modifier.Causality;

/**
 * Abstracción del origen de un modificador de propiedad.
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
 * ── IDENTIDAD ────────────────────────────────────────────────────────────
 * La identidad de un ModifierSource ES EL OBJETO MISMO, no su nombre textual.
 * Implementaciones concretas deben declararse como constantes estáticas
 * y compararse por referencia (==).
 *
 * displayName() existe únicamente para logging, debug y serialización.
 * No participa en equals ni en ninguna colección interna.
 *
 * ── JERARQUÍA ────────────────────────────────────────────────────────────
 * isOrDescendantOf() permite consultas jerárquicas basadas en la cadena
 * de padres explícita, no en comparación de prefijos de strings.
 *
 * ── POR QUÉ ES UNA INTERFAZ ───────────────────────────────────────────────
 * Los sistemas futuros declaran sus propias fuentes en sus propios catálogos
 * sin modificar el núcleo:
 *
 *   // En un módulo de armas:
 *   public final class WeaponSources {
 *       public static final ModifierSource WEAPON =
 *           ModifierSource.of("Weapon");
 *       public static final ModifierSource WEAPON_PASSIVE =
 *           ModifierSource.of("Weapon.Passive");
 *   }
 */
public interface ModifierSource {

    /**
     * Nombre legible de este origen, para logging, debug y serialización.
     * NO es la identidad del origen. No usar como clave en colecciones.
     *
     * @return nombre, nunca null ni vacío.
     */
    String displayName();

    /**
     * True si este origen es igual o descendiente del candidato.
     *
     * La implementación base comprueba igualdad por referencia (misma instancia).
     * Implementaciones jerárquicas sobreescriben esto para subir por la cadena
     * de padres.
     *
     * @param candidate origen a comparar
     * @return true si son el mismo origen o este es descendiente del candidato
     */
    default boolean isOrDescendantOf(ModifierSource candidate) {
        return this == candidate;
    }

    /**
     * Crea una implementación simple sin jerarquía.
     * Útil para fuentes inline o de un solo nivel.
     *
     * @param name nombre legible para display/debug
     */
    static ModifierSource of(String name) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("ModifierSource.of: name no puede ser null o vacío.");
        return new ModifierSource() {
            @Override public String displayName() { return name; }
            @Override public String toString()    { return "ModifierSource[" + name + "]"; }
        };
    }
}
