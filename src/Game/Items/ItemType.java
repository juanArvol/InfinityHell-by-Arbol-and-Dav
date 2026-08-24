package Game.Items;

/**
 * Clasificación general de Items — categoría para lógica de inventario y UI.
 *
 * ── ARQUITECTURA — Items Module ──────────────────────────────────────────
 *
 * IMPORTANTE: ItemType NO es equivalente a ItemTypeBase.
 *
 * ItemType:
 *   → Clasificación general de categorías de Items
 *   → Usado para lógica de inventario, filtrado, UI
 *   → Representa "¿a qué categoría pertenece este Item?"
 *
 * ItemTypeBase:
 *   → Infraestructura común para tipos concretos (BulletType, WeaponType)
 *   → Usado para registro, creación, resolución
 *   → Representa "¿cómo se declara y gestiona un tipo concreto?"
 *
 * AMBOS CONCEPTOS COEXISTEN:
 *
 *   ItemType.FIREARM             (categoría general)
 *     │
 *     └── WeaponType.ESCOPETA    (tipo concreto)
 *
 * ── SEPARACIÓN DE RESPONSABILIDADES ──────────────────────────────────────
 *
 * ItemType se usa para:
 *   - Filtrado de inventario ("mostrar solo armas")
 *   - UI de categorías
 *   - Lógica general de clasificación
 *   - Sistema de loot genérico
 *
 * Los Types concretos (BulletType, WeaponType, AmuletType) se usan para:
 *   - Declaración de tipos específicos disponibles
 *   - Creación de instancias concretas
 *   - Lógica especializada por dominio
 *
 * ── NOTAS ARQUITECTÓNICAS ────────────────────────────────────────────────
 *
 * WEAPON representa armas cuerpo a cuerpo tradicionales (bates, cuchillos).
 * FIREARM representa armas de fuego (pistolas, rifles).
 * RESOURCE representa materiales y recursos genéricos.
 * AMULET representa mejoras pasivas acumulables.
 *
 * @see Game.Items.Core.ObjectType  infraestructura para tipos concretos
 * @see Game.Items.Types.Weapons.WeaponType.WeaponType  tipo concreto de armas
 * @see Game.Items.Types.Bullets.Definition.BulletType  tipo concreto de balas
 */
public enum ItemType {
    /** Armas cuerpo a cuerpo: bates, cuchillos, hachas. */
    WEAPON,
    
    /** Consumibles: comida, vendajes, medicamentos. */
    CONSUMABLE,
    
    /** Armadura y ropa. */
    ARMOR,
    
    /** Herramientas: linterna, palanca, llave. */
    TOOL,
    
    /** Armamento de fuego: rifles, pistolas, ametralladoras. */
    FIREARM,
    
    /** Amuletos mágicos: mejoras pasivas acumulables. */
    AMULET,
    
    /** Recursos genéricos: bolsas, materiales de craft. */
    RESOURCE
}
