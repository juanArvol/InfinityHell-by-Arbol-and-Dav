package Game.Items.Creation;

import java.awt.image.BufferedImage;

/**
 * Definición visual de un ítem — datos presentacionales para UI y mundo.
 *
 * ── ARQUITECTURA — Items Module ──────────────────────────────────────────
 *
 * SEPARACIÓN:
 *   DocumentDefinition → Identidad tipada + delegación a VisualDefinition
 *   VisualDefinition   → Presentación (nombre, descripción, rareza, icono)
 *
 * PROPÓSITO:
 *   Centralizar toda la información visual/presentacional de un ítem.
 *   Los DocumentDefinition delegan a estas instancias para obtener
 *   sus datos visuales.
 *
 * INMUTABILIDAD:
 *   Todos los campos son final. Una vez creada, no se modifica.
 *
 * Uso:
 *   VisualDefinition visual = new VisualDefinition(
 *       "Pistola 9mm",
 *       "Arma de fuego básica con buen equilibrio",
 *       ItemRarity.COMMON,
 *       pistolIcon
 *   );
 */
public final class VisualDefinition {

    /** Nombre visible al jugador en UI. */
    public final String displayName;

    /** Descripción del comportamiento/efecto para UI de selección. */
    public final String description;

    /** Rareza del ítem (afecta probabilidad de drop y color en UI). */
    public final ItemRarity rarity;

    /** Icono para UI y world items. Puede ser null. */
    public final BufferedImage icon;

    /**
     * Constructor completo.
     */
    public VisualDefinition(String displayName,
                           String description,
                           ItemRarity rarity,
                           BufferedImage icon) {
        if (displayName == null || displayName.isBlank())
            throw new IllegalArgumentException("displayName no puede estar vacío");
        if (rarity == null)
            throw new IllegalArgumentException("rarity no puede ser null");

        this.displayName = displayName;
        this.description = description != null ? description : "";
        this.rarity      = rarity;
        this.icon        = icon;
    }

    /**
     * Constructor sin icono.
     */
    public VisualDefinition(String displayName, String description, ItemRarity rarity) {
        this(displayName, description, rarity, null);
    }

    // ── Getters ───────────────────────────────────────────────────────────

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public ItemRarity getRarity()  { return rarity; }
    public BufferedImage getIcon() { return icon; }

    @Override
    public String toString() {
        return "VisualDefinition{name='" + displayName + "', rarity=" + rarity + "}";
    }
}
