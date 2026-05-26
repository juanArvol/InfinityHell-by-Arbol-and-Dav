package Game.Items;

/**
 * Tipo de ítem.
 *
 * Define la categoría general de un ítem para facilitar lógica de inventario,
 * filtros de equipamiento y comportamiento en uso.
 *
 * Extensible con un simple enum — no requiere jerarquía de clases por tipo.
 */
public enum ItemType {
    /** Armas de fuego: pistolas, rifles, escopetas. */
    FIREARM,
    /** Armas cuerpo a cuerpo: bates, cuchillos, hachas. */
    MELEE,
    /** Munición para armas de fuego. */
    AMMO,
    /** Consumibles: comida, vendajes, medicamentos. */
    CONSUMABLE,
    /** Armadura y ropa. */
    ARMOR,
    /** Herramientas: linterna, palanca, llave. */
    TOOL,
    /** Recursos genéricos: bolsas, materiales de craft. */
    RESOURCE
}
