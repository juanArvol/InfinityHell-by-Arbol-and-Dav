package Game.Items.Creation;

/**
 * Rareza de un ítem — determina su probabilidad en loot tables
 * y puede afectar bonuses de stats.
 *
 * ── HRFC — Items Module Architectural Consolidation ──────────────────────
 *
 * ItemRarity es compartido por todas las familias de Items:
 *   - BulletType
 *   - WeaponType
 *   - AmuletType
 *   - Futuros tipos de Item
 *
 * Los weights son relativos entre sí. El sistema de loot normaliza los totales.
 *
 * Ejemplo de distribución con weights por defecto:
 *   COMMON:    50 / 100 = 50%
 *   UNCOMMON:  30 / 100 = 30%
 *   RARE:      15 / 100 = 15%
 *   EPIC:       4 / 100 =  4%
 *   LEGENDARY:  1 / 100 =  1%
 */
public enum ItemRarity {

    COMMON(50,    "Común",      0xAAAAAA),
    UNCOMMON(30,  "Poco común", 0x4CFF50),
    RARE(15,      "Raro",       0x4C88FF),
    EPIC(4,       "Épico",      0xAA44FF),
    LEGENDARY(1,  "Legendario", 0xFF9900);

    /** Peso relativo para loot tables. Mayor = más frecuente. */
    public final int weight;

    /** Nombre localizado para UI. */
    public final String displayName;

    /** Color en formato 0xRRGGBB para UI. */
    public final int color;

    ItemRarity(int weight, String displayName, int color) {

        if (weight < 0) {
            throw new IllegalArgumentException(
                    "weight no puede ser negativo"
            );
        }

        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException(
                    "displayName no puede estar vacío"
            );
        }
        
        this.weight      = weight;
        this.displayName = displayName;
        this.color       = color;
    }

    public int getWeight() {
        return weight;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getColor() {
        return color;
    }
}