package Game.Gameplay.Core.Causality;

/**
 * Abstracción del alcance de un modificador.
 *
 * ── RESPONSABILIDAD ÚNICA ─────────────────────────────────────────────────
 * ModifierScope responde a una sola pregunta:
 *
 *   "¿Sobre qué tipos de elementos puede actuar este modificador?"
 *
 * No contiene lógica.
 * No modifica nada.
 * No ejecuta comportamiento.
 * Solo expresa el alcance semántico de la modificación.
 *
 * ── POR QUÉ ES UNA INTERFAZ ───────────────────────────────────────────────
 * Al igual que ModifierSource, ModifierScope es una interfaz para permitir
 * que los sistemas futuros declaren sus propios alcances sin tocar el núcleo.
 *
 *   // En un módulo de proyectiles:
 *   public final class ProjectileScopes {
 *       public static final ModifierScope PROJECTILE = () -> "Projectile";
 *       public static final ModifierScope PROJECTILE_HOMING = () -> "Projectile.Homing";
 *   }
 *
 * ── CATÁLOGO DE EJEMPLOS CONCEPTUALES (NO IMPLEMENTADOS) ─────────────────
 * Los siguientes son ejemplos de alcances que existirán en el juego.
 * NINGUNO se implementa aquí — solo la infraestructura.
 *
 *   Self        → afecta a la propia entidad que posee el modificador
 *   Target      → afecta a la entidad objetivo de la acción
 *   Projectile  → afecta a proyectiles
 *   Weapon      → afecta a armas y sus estadísticas
 *   Spell       → afecta a hechizos
 *   Environment → afecta al entorno
 *   Creature    → afecta a criaturas
 *   Summon      → afecta a invocaciones
 *   Item        → afecta a objetos del inventario
 *   Modifier    → afecta a otros modificadores (ver ModifierInfluence)
 *   World       → alcance global, afecta al estado del mundo
 *
 * ── ALCANCE vs FUENTE ────────────────────────────────────────────────────
 * ModifierSource describe DE DÓNDE viene el modificador.
 * ModifierScope  describe QUÉ PUEDE SER el objetivo del modificador.
 *
 * Un modificador de "Spell" (source) puede tener alcance "Target" (scope),
 * lo que significa que el hechizo modifica la entidad objetivo.
 * O puede tener alcance "Projectile", modificando el proyectil que lanza.
 *
 * ── JERARQUÍA ────────────────────────────────────────────────────────────
 * ModifierScope soporta la misma jerarquía de prefijo de punto que
 * ModifierSource, permitiendo consultas amplias ("¿afecta a algún Projectile?")
 * sin enumerar todos los subtipos.
 *
 * ── NAMING ────────────────────────────────────────────────────────────────
 * IDs en PascalCase con notación de punto para subtipos:
 *   "Self", "Target", "Projectile", "Projectile.Homing"
 */
public interface ModifierScope {

    /**
     * Identificador único del alcance.
     *
     * Ejemplos:
     *   "Self"
     *   "Target"
     *   "Projectile"
     *   "Projectile.Homing"
     *   "Creature"
     *   "Modifier"
     *
     * @return identificador, nunca null ni vacío.
     */
    String id();

    /**
     * True si este alcance es igual o descendiente del candidato.
     *
     * Ejemplo:
     *   "Projectile.Homing".isOrDescendantOf("Projectile") → true
     *   "Target".isOrDescendantOf("Self")                   → false
     *
     * @param candidate alcance ancestro a buscar
     * @return true si este alcance es o desciende del candidato
     */
    default boolean isOrDescendantOf(ModifierScope candidate) {
        if (candidate == null) return false;
        String thisId = this.id();
        String candId = candidate.id();
        if (thisId.equals(candId)) return true;
        return thisId.startsWith(candId + ".");
    }
}
