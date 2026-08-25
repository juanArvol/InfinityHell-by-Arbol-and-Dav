package Game.Items.Creation;

import Game.Items.VisualDefinition;

/**
 * Clase base para todas las definiciones de Items del juego.
 *
 * ── ARQUITECTURA FINAL — Items Module ────────────────────────────────────
 *
 * CENTRALIZACIÓN:
 *   ItemDefinition CENTRALIZA ItemID y VisualDefinition.
 *   Contiene AMBOS conceptos directamente, sin delegación complicada.
 *
 *   ItemDefinition
 *     ├─ ItemID (tipado fuerte)
 *     └─ VisualDefinition
 *         ├─ displayName
 *         ├─ description
 *         ├─ rarity
 *         └─ icon
 *
 * JERARQUÍA:
 *   ItemDefinition (concreta, puede instanciarse directamente)
 *     ├─ Se usa directamente en BulletDefinition, WeaponDefinitions, AmuletDefinitions
 *     └─ Las familias de Types usan ItemDefinition directamente sin herencia
 *
 * RESPONSABILIDAD:
 *   Representa los DATOS del Item — su plantilla estática.
 *   NO representa el comportamiento en runtime (eso es responsabilidad de
 *   BulletBehavior, WeaponComport, AmuletEffect).
 *
 * @see Game.Items.VisualDefinition
 */
public class ItemDefinition {

    /** ID tipado fuerte del Item. */
    private final ItemID itemId;

    /** Definición visual completa. */
    private final VisualDefinition visual;

    /**
     * Constructor público — permite instanciar directamente o extender.
     *
     * @param itemId identificador tipado fuerte
     * @param visual definición visual completa
     * @throws IllegalArgumentException si itemId o visual son null
     */
    public ItemDefinition(ItemID itemId, VisualDefinition visual) {
        if (itemId == null)
            throw new IllegalArgumentException("itemId no puede ser null");
        if (visual == null)
            throw new IllegalArgumentException("visual no puede ser null");
        
        this.itemId = itemId;
        this.visual = visual;
    }

    // ── Getters ───────────────────────────────────────────────────────────

    /** Retorna el ItemID tipado. */
    public ItemID getItemId() {
        return itemId;
    }

    /** Retorna el ID como string. */
    public String getIdAsString() {
        return itemId.asString();
    }

    

    /** Retorna el nombre visible al jugador. */
    public String getDisplayName() {
        return visual.displayName;
    }

    /** Retorna la descripción del comportamiento/efecto. */
    public String getDescription() {
        return visual.description;
    }

    /** Retorna la rareza del ítem. */
    public ItemRarity getRarity() {
        return visual.rarity;
    }

    /** Retorna el icono para UI (puede ser null). */
    public java.awt.image.BufferedImage getIcon() {
        return visual.icon;
    }

    /** Retorna la definición visual completa. */
    public VisualDefinition getVisual() {
        return visual;
    }

    // ── Object identity ───────────────────────────────────────────────────

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id='" + getIdAsString() + "', rarity=" + getRarity() + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ItemDefinition that = (ItemDefinition) o;
        return itemId.equals(that.itemId);
    }

    @Override
    public int hashCode() {
        return itemId.hashCode();
    }
}
