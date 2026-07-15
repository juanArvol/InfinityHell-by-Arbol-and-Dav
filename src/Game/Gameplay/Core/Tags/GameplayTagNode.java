package Game.Gameplay.Core.Tags;

/**
 * Nodo jerárquico del árbol de tags.
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
 * de ENTITY, sin importar la profundidad. La consulta sube por la cadena
 * de padres hasta encontrar al candidato o llegar a la raíz.
 *
 *   CREATURE.isOrDescendantOf(ENTITY) → true  (CREATURE → ORGANIC → ENTITY)
 *   CREATURE.isOrDescendantOf(MECHANICAL) → false
 *   ENTITY.isOrDescendantOf(CREATURE) → false  (no sube, solo baja)
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
 * Esto hace el árbol de tags seguro para compartir entre threads y predecible
 * durante el ciclo de vida del juego.
 */
public final class GameplayTagNode implements GameplayTag {

    private final String      id;
    private final GameplayTag parent;   // null = tag raíz

    private GameplayTagNode(String id, GameplayTag parent) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El ID de un tag no puede ser nulo o vacío.");
        }
        this.id     = id;
        this.parent = parent;
    }

    // ── Factory methods ───────────────────────────────────────────────────

    /**
     * Crea un tag raíz (sin padre).
     *
     * @param id identificador único, por ejemplo "Entity"
     */
    public static GameplayTagNode root(String id) {
        return new GameplayTagNode(id, null);
    }

    /**
     * Crea un tag hijo de otro tag.
     *
     * @param id     identificador único, por ejemplo "Entity.Organic"
     * @param parent tag padre, por ejemplo GameplayTags.ENTITY
     */
    public static GameplayTagNode of(String id, GameplayTag parent) {
        if (parent == null) throw new IllegalArgumentException("Usa root() para crear un tag sin padre.");
        return new GameplayTagNode(id, parent);
    }

    // ── GameplayTag ───────────────────────────────────────────────────────

    @Override
    public String id() {
        return id;
    }

    /**
     * Comprueba si este nodo ES el candidato, o si algún ancestro lo es.
     *
     * La búsqueda sube por la cadena de padres. El coste es O(profundidad),
     * que en la práctica es 2–5 niveles — despreciable.
     */
    @Override
    public boolean isOrDescendantOf(GameplayTag candidate) {
        GameplayTag current = this;
        while (current != null) {
            if (current.id().equals(candidate.id())) return true;
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

    // ── Object ────────────────────────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GameplayTag t)) return false;
        return id.equals(t.id());
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Tag[" + id + "]";
    }
}
