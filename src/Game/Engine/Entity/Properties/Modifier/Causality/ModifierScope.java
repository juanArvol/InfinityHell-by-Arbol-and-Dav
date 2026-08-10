package Game.Engine.Entity.Properties.Modifier.Causality;

/**
 * Abstracción del alcance de un modificador de propiedad.
 *
 * ── RESPONSABILIDAD ÚNICA ─────────────────────────────────────────────────
 * ModifierScope responde a una sola pregunta:
 *
 *   "¿Sobre qué tipos de elementos puede actuar este modificador?"
 *
 * No contiene lógica. No modifica nada. Solo expresa el alcance semántico.
 *
 * ── IDENTIDAD ────────────────────────────────────────────────────────────
 * La identidad de un ModifierScope ES EL OBJETO MISMO, no su nombre textual.
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
 * ── ALCANCE vs FUENTE ────────────────────────────────────────────────────
 * ModifierSource describe DE DÓNDE viene el modificador.
 * ModifierScope  describe QUÉ PUEDE SER el objetivo del modificador.
 */
public interface ModifierScope {

    /**
     * Nombre legible de este alcance, para logging, debug y serialización.
     * NO es la identidad del alcance. No usar como clave en colecciones.
     *
     * @return nombre, nunca null ni vacío.
     */
    String displayName();

    /**
     * True si este alcance es igual o descendiente del candidato.
     *
     * La implementación base comprueba igualdad por referencia (misma instancia).
     * Implementaciones jerárquicas sobreescriben esto para subir por la cadena
     * de padres.
     *
     * @param candidate alcance a comparar
     * @return true si son el mismo alcance o este es descendiente del candidato
     */
    default boolean isOrDescendantOf(ModifierScope candidate) {
        return this == candidate;
    }
}
