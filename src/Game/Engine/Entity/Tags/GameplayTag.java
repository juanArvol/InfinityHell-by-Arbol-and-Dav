package Game.Engine.Entity.Tags;

/**
 * Contrato de un tag de entidad.
 *
 * ── QUÉ ES UN TAG ────────────────────────────────────────────────────────
 * Un tag es un identificador semántico que se adhiere a una entidad para
 * describir qué ES o a qué CATEGORÍA pertenece.
 *
 * Su responsabilidad es ÚNICA: clasificar. No transporta lógica, no ejecuta
 * comportamiento, no almacena estado mutable.
 *
 * ── IDENTIDAD ────────────────────────────────────────────────────────────
 * La identidad de un GameplayTag ES EL OBJETO MISMO, no su nombre textual.
 *
 * Dos tags son idénticos si y solo si son la misma instancia Java.
 * Esto se garantiza declarando todos los tags como constantes estáticas
 * en catálogos (GameplayTags, SpellTags, etc.) y nunca instanciando
 * nuevos tags en el punto de uso.
 *
 * La comparación correcta es:
 *
 *   tagComp.hasTag(GameplayTags.ORGANIC)          // por referencia
 *   tagComp.hasTagOrAncestor(GameplayTags.ENTITY) // por jerarquía de objetos
 *
 * NUNCA:
 *   tagComp.hasTag("organic")                     // string mágico — no existe
 *
 * ── REPRESENTACIÓN TEXTUAL ───────────────────────────────────────────────
 * displayName() existe únicamente para:
 *   - logging y debug
 *   - serialización externa
 *   - herramientas de inspección
 *
 * No participa en equals, hashCode ni en ninguna colección interna.
 *
 * ── JERARQUÍA ────────────────────────────────────────────────────────────
 * Los tags pueden ser jerárquicos. GameplayTagNode implementa esta interfaz
 * y añade la noción de padre/hijo, permitiendo consultas del tipo:
 *
 *   "¿tiene esta entidad el tag ORGANIC o algún subtag suyo?"
 *
 * La jerarquía se recorre por referencias de objeto (parent == candidate),
 * no por comparación de strings.
 *
 * ── POR QUÉ NO ES UN ENUM ────────────────────────────────────────────────
 * Un enum de tags requeriría modificar la clase base cada vez que un nuevo
 * sistema añade una categoría. GameplayTag es una interfaz: cualquier clase
 * puede declarar un tag simplemente implementándola. La infraestructura
 * nunca necesita conocer los tags concretos.
 *
 * ── USO ──────────────────────────────────────────────────────────────────
 *
 *   // Definición de un tag simple (sin jerarquía)
 *   public static final GameplayTag MY_TAG = GameplayTag.simple("MyTag");
 *
 *   // Definición de un nodo jerárquico
 *   public static final GameplayTagNode ORGANIC =
 *       GameplayTagNode.of("Entity.Organic", GameplayTags.ENTITY);
 *
 *   // Consulta en un TagComponent
 *   if (tagComp.hasTag(GameplayTags.ORGANIC)) { ... }
 *   if (tagComp.hasTagOrAncestor(GameplayTags.ENTITY)) { ... }
 */
public interface GameplayTag {

    /**
     * Nombre legible de este tag, para logging, debug y serialización.
     * NO es la identidad del tag. No usar como clave en colecciones.
     *
     * Convención: notación de punto, "Categoría.Subcategoría.Específico"
     *
     * @return nombre, nunca null ni vacío.
     */
    String displayName();

    /**
     * Comprueba si este tag es el mismo objeto que el candidato, o si es
     * descendiente del candidato en la jerarquía.
     *
     * La implementación base comprueba igualdad por referencia (==).
     * GameplayTagNode sobreescribe esto para subir por la cadena de padres.
     *
     * @param candidate tag a comparar
     * @return true si este tag ES el candidato o desciende de él
     */
    default boolean isOrDescendantOf(GameplayTag candidate) {
        return this == candidate;
    }

    /**
     * Crea un tag simple sin jerarquía.
     * Útil para tags inline o de sistemas que no necesitan árbol.
     *
     * @param name nombre legible para display/debug
     */
    static GameplayTag simple(String name) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("GameplayTag.simple: name no puede ser null o vacío.");
        return new GameplayTag() {
            @Override public String displayName()          { return name; }
            @Override public String toString()             { return "Tag[" + name + "]"; }
        };
    }
}
