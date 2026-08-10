package Game.Engine.Entity.Tags;

import Game.Engine.Component;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Componente que otorga una identidad semántica a una entidad mediante tags.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * TagComponent responde a una sola pregunta: "¿qué ES esta entidad?"
 * No ejecuta lógica, no modifica estado, no comunica con otros sistemas.
 * Es exclusivamente un descriptor de identidad.
 *
 * ── IDENTIDAD DE TAG ─────────────────────────────────────────────────────
 * Las comparaciones internas usan == (identidad de referencia de objeto).
 * Solo se considera que una entidad "tiene" un tag si su colección contiene
 * exactamente esa instancia de GameplayTag.
 *
 * Esto garantiza que:
 *   - Solo las constantes estáticas de los catálogos pueden producir matches.
 *   - Es imposible consultar por string mágico.
 *   - Dos tags con el mismo displayName() pero distinta instancia son distintos.
 *
 * ── USO ──────────────────────────────────────────────────────────────────
 *
 *   // En el constructor de un Enemy concreto:
 *   TagComponent tags = new TagComponent();
 *   tags.add(GameplayTags.CREATURE);
 *   tags.add(GameplayTags.ENEMY_FACTION);
 *   tags.add(GameplayTags.FIRE);
 *   addComponent(tags);
 *
 *   // En cualquier sistema:
 *   TagComponent tags = entity.getComponent(TagComponent.class);
 *   if (tags != null && tags.hasTag(GameplayTags.ORGANIC)) {
 *       // aplicar veneno
 *   }
 *
 *   // Consulta jerárquica (CREATURE es ORGANIC y ENTITY):
 *   tags.hasTagOrAncestor(GameplayTags.ENTITY)   → true para toda entidad
 *   tags.hasTagOrAncestor(GameplayTags.ORGANIC)  → true para criaturas
 *   tags.hasTag(GameplayTags.CREATURE)            → true solo para criaturas
 *
 * ── CONSULTA JERÁRQUICA vs EXACTA ────────────────────────────────────────
 * - hasTag(tag)            → coincidencia exacta (== con cada tag en el set).
 * - hasTagOrAncestor(tag)  → el candidato está en la cadena de ancestros
 *                            de algún tag de este componente.
 *
 * ── THREAD SAFETY ────────────────────────────────────────────────────────
 * TagComponent no es thread-safe. Modificar tags fuera del game loop thread
 * requiere sincronización externa.
 */
public final class TagComponent extends Component {

    /**
     * Set de tags de esta entidad.
     * LinkedHashSet preserva orden de inserción y usa equals/hashCode del objeto.
     * Como GameplayTagNode no sobreescribe equals/hashCode, el set usa
     * identidad de referencia por defecto — idéntico a IdentityHashMap.
     */
    private final Set<GameplayTag> tags = new LinkedHashSet<>();

    // ── Constructor(es) ───────────────────────────────────────────────────

    /** Crea un TagComponent sin tags iniciales. */
    public TagComponent() {}

    /** Crea un TagComponent con un conjunto inicial de tags. */
    public TagComponent(GameplayTag... initialTags) {
        for (GameplayTag t : initialTags) {
            add(t);
        }
    }

    // ── Mutación ──────────────────────────────────────────────────────────

    /**
     * Añade un tag a esta entidad.
     * Si el tag ya está presente (misma instancia), la operación no tiene efecto.
     *
     * @param tag tag a añadir, no puede ser null.
     */
    public void add(GameplayTag tag) {
        if (tag == null) throw new IllegalArgumentException("El tag no puede ser null.");
        tags.add(tag);
    }

    /**
     * Elimina un tag de esta entidad.
     * Si el tag no está presente, la operación no tiene efecto.
     *
     * @param tag tag a eliminar (misma instancia que fue añadida).
     */
    public void remove(GameplayTag tag) {
        tags.remove(tag);
    }

    // ── Consultas ─────────────────────────────────────────────────────────

    /**
     * True si esta entidad tiene exactamente el tag indicado.
     *
     * La comparación usa == (identidad de referencia de objeto).
     * No considera jerarquía — usa {@link #hasTagOrAncestor} para eso.
     *
     * @param tag tag a buscar (debe ser la misma instancia que fue añadida).
     */
    public boolean hasTag(GameplayTag tag) {
        if (tag == null) return false;
        for (GameplayTag t : tags) {
            if (t == tag) return true;
        }
        return false;
    }

    /**
     * True si esta entidad tiene el tag indicado, o si algún tag de esta
     * entidad es descendiente del candidato.
     *
     * La jerarquía se recorre por referencias de objeto (== en la cadena de padres),
     * no por comparación de strings.
     *
     * Ejemplo: si la entidad tiene CREATURE, entonces:
     *   hasTagOrAncestor(ORGANIC)  → true  (CREATURE es descendiente de ORGANIC)
     *   hasTagOrAncestor(ENTITY)   → true  (CREATURE → ORGANIC → ENTITY)
     *   hasTagOrAncestor(BOSS)     → false (CREATURE no desciende de BOSS)
     *
     * @param candidate tag que se busca como ancestro o igual.
     */
    public boolean hasTagOrAncestor(GameplayTag candidate) {
        if (candidate == null) return false;
        for (GameplayTag t : tags) {
            if (t.isOrDescendantOf(candidate)) return true;
        }
        return false;
    }

    /**
     * True si esta entidad tiene TODOS los tags del conjunto dado.
     */
    public boolean hasAllTags(GameplayTag... required) {
        for (GameplayTag req : required) {
            if (!hasTag(req)) return false;
        }
        return true;
    }

    /**
     * True si esta entidad tiene AL MENOS UNO de los tags del conjunto dado.
     */
    public boolean hasAnyTag(GameplayTag... candidates) {
        for (GameplayTag c : candidates) {
            if (hasTag(c)) return true;
        }
        return false;
    }

    /**
     * Vista no modificable de todos los tags de esta entidad.
     */
    public Collection<GameplayTag> getTags() {
        return Collections.unmodifiableSet(tags);
    }

    /**
     * Número de tags en este componente.
     */
    public int size() {
        return tags.size();
    }
}
