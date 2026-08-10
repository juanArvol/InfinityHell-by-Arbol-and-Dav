package Game.Engine.Entity.Tags;

/**
 * Nodo jerárquico del árbol de tags.
 *
 * ── IDENTIDAD ────────────────────────────────────────────────────────────
 * La identidad de un GameplayTagNode ES EL OBJETO MISMO.
 *
 * Dos nodos son idénticos si y solo si son la misma instancia Java.
 * equals y hashCode NO se sobreescriben — la JVM usa identidad de referencia.
 *
 * La comparación correcta es:
 *   myTag == GameplayTags.ORGANIC      // true solo si ES la misma instancia
 *   myTag.isOrDescendantOf(GameplayTags.ENTITY)  // recorre jerarquía por ==
 *
 * ── JERARQUÍA EXPLÍCITA ───────────────────────────────────────────────────
 * Un GameplayTagNode tiene un padre opcional. Esto permite consultas
 * jerárquicas sin ninguna estructura de árbol centralizada:
 *
 *   ENTITY  (raíz)
 *     ├── ORGANIC
 *     │     ├── CREATURE
 *     │     └── COMPANION
 *     ├── MECHANICAL
 *     ├── PROJECTILE
 *     └── ENVIRONMENTAL
 *
 * ── CONSULTA JERÁRQUICA ───────────────────────────────────────────────────
 * isOrDescendantOf(ENTITY) retorna true para cualquier nodo en el árbol
 * de ENTITY. La consulta sube por la cadena de padres usando == para comparar
 * referencias de objeto, sin comparar strings.
 *
 *   CREATURE.isOrDescendantOf(ENTITY)    → true  (CREATURE → ORGANIC → ENTITY)
 *   CREATURE.isOrDescendantOf(MECHANICAL)→ false
 *   ENTITY.isOrDescendantOf(CREATURE)    → false  (no sube, solo baja)
 *
 * ── REPRESENTACIÓN TEXTUAL ───────────────────────────────────────────────
 * displayName() existe únicamente para logging, debug y serialización.
 * No participa en equals ni en ninguna colección interna.
 *
 * ── EXTENSIBILIDAD ────────────────────────────────────────────────────────
 * Nuevos sistemas añaden sus propios nodos sin tocar este archivo:
 *
 *   // En un módulo de magia:
 *   public static final GameplayTagNode SPELL =
 *       GameplayTagNode.of("Entity.Spell", GameplayTags.ENTITY);
 *   public static final GameplayTagNode SPELL_FIRE =
 *       GameplayTagNode.of("Entity.Spell.Fire", SPELL);
 *
 * ── DISEÑO: INMUTABILIDAD ─────────────────────────────────────────────────
 * Los nodos son inmutables por diseño. El padre se fija en construcción.
 */
public final class GameplayTagNode implements GameplayTag {

    private final String      name;    // metadata de display únicamente
    private final GameplayTag parent;  // null = tag raíz

    private GameplayTagNode(String name, GameplayTag parent) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre de un GameplayTagNode no puede ser nulo o vacío.");
        }
        this.name   = name;
        this.parent = parent;
    }

    // ── Factory methods ───────────────────────────────────────────────────

    /**
     * Crea un tag raíz (sin padre).
     *
     * @param name nombre legible para display/debug, por ejemplo "Entity"
     */
    public static GameplayTagNode root(String name) {
        return new GameplayTagNode(name, null);
    }

    /**
     * Crea un tag hijo de otro tag.
     *
     * @param name   nombre legible para display/debug, por ejemplo "Entity.Organic"
     * @param parent tag padre, por ejemplo GameplayTags.ENTITY
     */
    public static GameplayTagNode of(String name, GameplayTag parent) {
        if (parent == null) throw new IllegalArgumentException("Usa root() para crear un tag sin padre.");
        return new GameplayTagNode(name, parent);
    }

    // ── GameplayTag ───────────────────────────────────────────────────────

    /**
     * Nombre legible de este tag, para logging, debug y serialización.
     * NO es la identidad del tag.
     */
    @Override
    public String displayName() {
        return name;
    }

    /**
     * Comprueba si este nodo ES el candidato (==), o si algún ancestro lo es.
     *
     * La búsqueda sube por la cadena de padres usando == (referencia de objeto).
     * El coste es O(profundidad), que en la práctica es 2–5 niveles.
     */
    @Override
    public boolean isOrDescendantOf(GameplayTag candidate) {
        if (candidate == null) return false;
        GameplayTag current = this;
        while (current != null) {
            if (current == candidate) return true;
            current = (current instanceof GameplayTagNode node) ? node.parent : null;
        }
        return false;
    }

    // ── Consultas ─────────────────────────────────────────────────────────

    /**
     * Retorna el tag padre de este nodo, o null si es raíz.
     */
    public GameplayTag getParent() {
        return parent;
    }

    /**
     * True si este nodo es raíz (no tiene padre).
     */
    public boolean isRoot() {
        return parent == null;
    }

    // ── Identidad: basada en instancia, no en String ──────────────────────
    //
    // equals y hashCode NO se sobreescriben.
    // La JVM usa identidad de objeto (referencia) por defecto.
    // Dos GameplayTagNode son iguales si y solo si son la misma instancia.

    @Override
    public String toString() {
        return "Tag[" + name + "]";
    }
}
