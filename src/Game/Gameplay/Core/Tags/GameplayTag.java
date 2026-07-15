package Game.Gameplay.Core.Tags;

/**
 * Contrato de un tag de gameplay.
 *
 * ── QUÉ ES UN TAG ────────────────────────────────────────────────────────
 * Un tag es un identificador semántico que se adhiere a una entidad para
 * describir qué ES o a qué CATEGORÍA pertenece.
 *
 * Su responsabilidad es ÚNICA: clasificar. No transporta lógica, no ejecuta
 * comportamiento, no almacena estado mutable.
 *
 * ── POR QUÉ NO ES UN ENUM ────────────────────────────────────────────────
 * Un enum de tags requeriría modificar la clase base cada vez que un nuevo
 * sistema (un mod, una expansión, un DLC) añade una categoría.
 * Esto viola el principio de que el núcleo no debe modificarse para añadir
 * contenido.
 *
 * GameplayTag es una interfaz: cualquier clase o constante puede declarar un
 * tag simplemente implementándola. La infraestructura nunca necesita conocer
 * los tags concretos — solo opera con el contrato.
 *
 * ── JERARQUÍA ────────────────────────────────────────────────────────────
 * Los tags pueden ser jerárquicos. GameplayTagNode implementa esta interfaz
 * y añade la noción de padre/hijo, permitiendo consultas del tipo:
 *
 *   "¿tiene esta entidad el tag Entity.Organic o algún subtag suyo?"
 *
 * Las implementaciones simples (sin jerarquía) pueden implementar la interfaz
 * directamente como singletons o constantes estáticas.
 *
 * ── USO ──────────────────────────────────────────────────────────────────
 *
 *   // Definición de un tag simple
 *   public static final GameplayTag BOSS = () -> "Entity.Enemy.Boss";
 *
 *   // Definición de un nodo jerárquico
 *   public static final GameplayTagNode ORGANIC =
 *       GameplayTagNode.of("Entity.Organic", GameplayTags.ENTITY);
 *
 *   // Consulta en un TagComponent
 *   if (tagComp.hasTag(GameplayTags.ORGANIC)) { ... }
 *   if (tagComp.hasTagOrDescendant(GameplayTags.ENTITY)) { ... }
 *
 * ── REGLA DE NAMING ──────────────────────────────────────────────────────
 * Los IDs siguen notación de punto: "Categoría.Subcategoría.Específico"
 * Esto hace los IDs legibles, uniquificables y fáciles de filtrar por prefijo.
 */
public interface GameplayTag {

    /**
     * Identificador único del tag en notación de punto.
     *
     * Ejemplos:
     *   "Entity"
     *   "Entity.Organic"
     *   "Entity.Organic.Creature"
     *   "Entity.Projectile"
     *   "Element.Fire"
     *   "Status.Frozen"
     *
     * @return identificador, nunca null ni vacío.
     */
    String id();

    /**
     * Comprueba si este tag es igual o descendiente del tag candidato.
     *
     * La implementación base comprueba igualdad exacta de ID.
     * GameplayTagNode sobreescribe esto para subir por la jerarquía de padres.
     *
     * @param candidate tag a comparar
     * @return true si son el mismo tag, false si no
     */
    default boolean isOrDescendantOf(GameplayTag candidate) {
        return this.id().equals(candidate.id());
    }
}
