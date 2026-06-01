package Game.Items.Creation;

/**
 * Tipo de ítem — categoría general para lógica de inventario y UI.
 *
 * ── CAMBIOS RESPECTO A LA VERSIÓN ANTERIOR ───────────────────────────────
 * - Eliminado AMMO: no hay munición consumible. Las armas no se agotan.
 * - Eliminado FIREARM: las armas se gestionan por WeaponRegistry (no ItemRegistry).
 * - Añadido AMULET: mejoras pasivas acumulables (stackean infinitamente).
 *
 * ── SEPARACIÓN DE RESPONSABILIDADES ──────────────────────────────────────
 * El ItemRegistry gestiona ítems genéricos de mundo (consumibles, recursos).
 * Las armas, tipos de bala y amuletos tienen sus propios registros:
 *
 *   ItemRegistry    → CONSUMABLE, RESOURCE, ARMOR, TOOL, MELEE
 *   WeaponRegistry  → armas (únicas por run)
 *   BulletType      → tipos de bala (únicos por run, enum auto-registrado)
 *   AmuletRegistry  → amuletos (acumulables, infinitos)
 */
public enum ItemType {
    /** Armas cuerpo a cuerpo: bates, cuchillos, hachas. */
    MELEE,
    /** Consumibles: comida, vendajes, medicamentos. */
    CONSUMABLE,
    /** Armadura y ropa. */
    ARMOR,
    /** Herramientas: linterna, palanca, llave. */
    TOOL,
    /** Armamento de fuego: rifles, pistolas, ametralladoras. */
    FIREARM,
    /** Amuletos mágicos: mejoras pasivas acumulables. */
    AMMO,
    /** Recursos genéricos: bolsas, materiales de craft. */
    RESOURCE
}
