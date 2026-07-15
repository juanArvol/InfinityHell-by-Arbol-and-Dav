package Game.Gameplay.Core.Tags;

import Game.Engine.Component;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Componente que otorga una identidad de gameplay a una entidad mediante tags.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * TagComponent responde a una sola pregunta: "¿qué ES esta entidad?"
 * No ejecuta lógica, no modifica estado, no comunica con otros sistemas.
 * Es exclusivamente un descriptor de identidad.
 *
 * ── USO ──────────────────────────────────────────────────────────────────
 *
 *   // En el constructor de un Enemy concreto:
 *   TagComponent tags = new TagComponent();
 *   tags.add(GameplayTags.CREATURE);
 *   tags.add(GameplayTags.ENEMY_FACTION);
 *   tags.add(GameplayTags.FIRE);      // si es un enemigo de fuego
 *   addComponent(tags);
 *
 *   // En cualquier sistema, sin saber el tipo concreto:
 *   TagComponent tags = entity.getComponent(TagComponent.class);
 *   if (tags != null && tags.hasTag(GameplayTags.ORGANIC)) {
 *       // aplicar veneno — solo a criaturas orgánicas
 *   }
 *
 *   // Consulta jerárquica (CREATURE es ORGANIC y ENTITY):
 *   tags.hasTagOrAncestor(GameplayTags.ENTITY)   → true para toda entidad
 *   tags.hasTagOrAncestor(GameplayTags.ORGANIC)  → true para criaturas
 *   tags.hasTag(GameplayTags.CREATURE)            → true solo para criaturas
 *
 * ── CONSULTA JERÁRQUICA vs EXACTA ────────────────────────────────────────
 * - hasTag(tag)            → coincidencia exacta de ID.
 * - hasTagOrAncestor(tag)  → el candidato está en la cadena de ancestros
 *                            de algún tag de este componente.
 *
 * La consulta jerárquica permite filtros amplios ("¿es orgánico?") sin
 * enumerar cada subtipo posible.
 *
 * ── THREAD SAFETY ────────────────────────────────────────────────────────
 * TagComponent no es thread-safe. Modificar tags fuera del game loop thread
 * requiere sincronización externa.
 */
public final class TagComponent extends Component {

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
     * Si el tag ya está presente, la operación no tiene efecto (set semántico).
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
     * @param tag tag a eliminar.
     */
    public void remove(GameplayTag tag) {
        tags.remove(tag);
    }

    // ── Consultas ─────────────────────────────────────────────────────────

    /**
     * True si esta entidad tiene exactamente el tag indicado.
     * No considera jerarquía — usa {@link #hasTagOrAncestor} para eso.
     *
     * @param tag tag a buscar.
     */
    public boolean hasTag(GameplayTag tag) {
        for (GameplayTag t : tags) {
            if (t.id().equals(tag.id())) return true;
        }
        return false;
    }

    /**
     * True si esta entidad tiene el tag indicado, o si algún tag de esta
     * entidad es descendiente del candidato.
     *
     * Ejemplo: si la entidad tiene CREATURE, entonces:
     *   hasTagOrAncestor(ORGANIC)  → true  (CREATURE es descendiente de ORGANIC)
     *   hasTagOrAncestor(ENTITY)   → true  (CREATURE → ORGANIC → ENTITY)
     *   hasTagOrAncestor(BOSS)     → false (CREATURE no es descendiente de BOSS)
     *
     * @param candidate tag que se busca como ancestro o igual.
     */
    public boolean hasTagOrAncestor(GameplayTag candidate) {
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
