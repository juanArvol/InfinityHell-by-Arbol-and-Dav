package Game.Items;

import Game.Items.Creation.ItemID;
import Game.Items.Creation.ItemRarity;

/**
 * Definición documental de un ítem — identidad tipada + delegación a visual.
 *
 * ── ARQUITECTURA — Items Module ──────────────────────────────────────────
 *
 * SEPARACIÓN:
 *   DocumentDefinition → Identidad tipada (ItemID) + referencia a Visual
 *   VisualDefinition   → Presentación (nombre, descripción, rareza, icono)
 *
 * PROPÓSITO:
 *   Separar la identidad del ítem (fuertemente tipada) de su presentación.
 *   El DocumentDefinition es el esqueleto que conecta un ID tipado con
 *   sus datos visuales.
 *
 * DELEGACIÓN:
 *   Todos los getters de presentación delegan a VisualDefinition.
 *   Esto mantiene una única fuente de verdad para los datos visuales.
 *
 * Uso:
 *   DocumentDefinition doc = new DocumentDefinition(
 *       BulletID.NORMAL_BULLET,
 *       visualDef
 *   );
 *
 *   String name = doc.getDisplayName();  // delega a visualDef
 *   ItemRarity rarity = doc.getRarity(); // delega a visualDef
 */
public final class DocumentDefinition {

    /** ID tipado del ítem (tipo fuerte, no String). */
    public final ItemID id;

    /** Definición visual a la que delegar presentación. */
    public final VisualDefinition visual;

    /**
     * Constructor.
     *
     * @param id     identificador tipado (enum que implementa ItemID)
     * @param visual definición visual para presentación
     * @throws IllegalArgumentException si id o visual son null
     */
    public DocumentDefinition(ItemID id, VisualDefinition visual) {
        if (id == null)
            throw new IllegalArgumentException("id no puede ser null");
        if (visual == null)
            throw new IllegalArgumentException("visual no puede ser null");

        this.id     = id;
        this.visual = visual;
    }

    // ── Delegación a VisualDefinition ─────────────────────────────────────

    public String getDisplayName()       { return visual.getDisplayName(); }
    public String getDescription()       { return visual.getDescription(); }
    public ItemRarity getRarity()        { return visual.getRarity(); }
    public java.awt.image.BufferedImage getIcon() { return visual.getIcon(); }

    // ── Acceso directo a ID tipado ────────────────────────────────────────

    /**
     * Retorna el ID como string (para serialización, logs, etc.).
     */
    public String getIdAsString() {
        return id.asString();
    }

    /**
     * Retorna el ID tipado (para comparaciones type-safe).
     */
    public ItemID getId() {
        return id;
    }

    // ── Object identity ───────────────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DocumentDefinition that = (DocumentDefinition) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "DocumentDefinition{id=" + id.asString() + ", name='" + getDisplayName() + "'}";
    }
}
