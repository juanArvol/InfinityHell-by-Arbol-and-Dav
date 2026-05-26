package Game.Items;

import java.awt.image.BufferedImage;

/**
 * Definición de datos de un ítem — su "plantilla" estática.
 *
 * DISEÑO: separamos DEFINICIÓN (datos estáticos) de INSTANCIA (objeto en mundo).
 * Esto sigue el patrón Flyweight: todos los "bate de béisbol" comparten la misma
 * ItemDefinition, pero cada uno en el inventario es un ItemStack separado.
 *
 * Equivalente conceptual al ítem JSON de Project Zomboid (item Baseball Bat { ... }).
 *
 * Uso básico:
 *   ItemDefinition bat = new ItemDefinition.Builder("bat_baseball", ItemType.MELEE)
 *       .displayName("Bate de béisbol")
 *       .maxStack(1)
 *       .weight(1.5)
 *       .icon(batIcon)
 *       .build();
 *
 *   ItemDefinition pistol9mm = new ItemDefinition.Builder("pistol_9mm", ItemType.FIREARM)
 *       .displayName("Pistola 9mm")
 *       .maxStack(1)
 *       .weight(0.9)
 *       .ammoType("ammo_9mm")
 *       .magazineSize(15)
 *       .build();
 *
 *   ItemDefinition ammo = new ItemDefinition.Builder("ammo_9mm", ItemType.AMMO)
 *       .displayName("Munición 9mm")
 *       .maxStack(50)
 *       .weight(0.02)
 *       .build();
 */
public final class ItemDefinition {

    // ── Identificación ────────────────────────────────────────────────────

    /** ID único del ítem (snake_case: "pistol_9mm", "bat_baseball"). */
    public final String id;

    /** Nombre visible al jugador. */
    public final String displayName;

    /** Categoría del ítem. */
    public final ItemType type;

    // ── Inventario ────────────────────────────────────────────────────────

    /** Máximo de unidades en un stack (1 = no apilable). */
    public final int maxStack;

    /** Peso por unidad en kg. Afecta encumbrance del jugador. */
    public final double weight;

    /** Ícono para UI de inventario. Puede ser null si no hay sprite todavía. */
    public final BufferedImage icon;

    // ── Armas de fuego ────────────────────────────────────────────────────

    /** ID de la munición compatible. Null si no es arma de fuego. */
    public final String ammoType;

    /** Tamaño del cargador. 0 si no es arma de fuego. */
    public final int magazineSize;

    /** Daño por disparo. 0 si no es arma ofensiva. */
    public final float damage;

    /** Alcance efectivo en unidades de mundo. 0 si no aplica. */
    public final float range;

    // ── Constructor (solo via Builder) ────────────────────────────────────

    private ItemDefinition(Builder b) {
        this.id           = b.id;
        this.displayName  = b.displayName;
        this.type         = b.type;
        this.maxStack     = b.maxStack;
        this.weight       = b.weight;
        this.icon         = b.icon;
        this.ammoType     = b.ammoType;
        this.magazineSize = b.magazineSize;
        this.damage       = b.damage;
        this.range        = b.range;
    }

    public boolean isFirearm() { return type == ItemType.FIREARM; }
    public boolean isMelee()   { return type == ItemType.MELEE; }
    public boolean isAmmo()    { return type == ItemType.AMMO; }
    public boolean isStackable() { return maxStack > 1; }

    @Override
    public String toString() {
        return "ItemDefinition{id='" + id + "', type=" + type + "}";
    }

    // ── Builder ───────────────────────────────────────────────────────────

    public static final class Builder {
        private final String   id;
        private final ItemType type;
        private String         displayName;
        private int            maxStack     = 1;
        private double         weight       = 0.5;
        private BufferedImage  icon         = null;
        private String         ammoType     = null;
        private int            magazineSize = 0;
        private float          damage       = 0f;
        private float          range        = 0f;

        public Builder(String id, ItemType type) {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("id no puede estar vacío");
            this.id          = id;
            this.type        = type;
            this.displayName = id; // default: usar el id como nombre
        }

        public Builder displayName(String name)    { this.displayName  = name;  return this; }
        public Builder maxStack(int n)             { this.maxStack     = n;     return this; }
        public Builder weight(double kg)           { this.weight       = kg;    return this; }
        public Builder icon(BufferedImage img)     { this.icon         = img;   return this; }
        public Builder ammoType(String type)       { this.ammoType     = type;  return this; }
        public Builder magazineSize(int size)      { this.magazineSize = size;  return this; }
        public Builder damage(float dmg)           { this.damage       = dmg;   return this; }
        public Builder range(float r)              { this.range        = r;     return this; }

        public ItemDefinition build() {
            return new ItemDefinition(this);
        }
    }
}
