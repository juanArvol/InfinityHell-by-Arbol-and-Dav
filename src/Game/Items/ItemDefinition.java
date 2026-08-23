package Game.Items;

import Game.Items.Creation.ItemRarity;

/**
 * Clase base abstracta para todas las definiciones de Items del juego.
 *
 * ── JERARQUÍA UNIFICADA ───────────────────────────────────────────────────
 *
 *   ItemDefinition (abstracta)
 *     ├── WeaponDefinition
 *     ├── BulletDefinition
 *     └── AmuletDefinition
 *
 * ── DISEÑO ───────────────────────────────────────────────────────────────
 * Todas las definiciones de Items comparten la misma estructura de metadata:
 *   - id: identificador único (snake_case)
 *   - displayName: nombre visible al jugador
 *   - description: descripción del comportamiento/efecto
 *   - defaultRarity: rareza por defecto (configurable externamente)
 *
 * Esta clase base centraliza esos campos comunes y asegura coherencia
 * entre todos los tipos de Items.
 *
 * ── RESPONSABILIDAD ──────────────────────────────────────────────────────
 * ItemDefinition representa los DATOS del Item — su plantilla estática.
 * NO representa el comportamiento en runtime (eso es responsabilidad de
 * BulletBehavior, WeaponComport, AmuletEffect).
 *
 * ── SEPARACIÓN DE CONCEPTOS ──────────────────────────────────────────────
 *
 *   ObjectType (BulletType, WeaponType):
 *     → Identidad del tipo + capacidad de construcción del comportamiento
 *     → bulletType.create(), weaponType.createComport()
 *     → Usados directamente en código de gameplay
 *
 *   ItemDefinition (WeaponDefinition, BulletDefinition, AmuletDefinition):
 *     → Metadata + configuración del Item
 *     → Usados en sistema de loot, tiendas, UI de selección
 *     → Pueden tener rareza/nombre/descripción sobreescritos externamente
 *
 *   Registry (WeaponRegistry, BulletRegistry, AmuletRegistry):
 *     → Almacenamiento y consulta de Definitions
 *     → Filtrado por rareza, pool de oferta, etc.
 *
 * ── EXTENSIBILIDAD ───────────────────────────────────────────────────────
 * Para añadir un nuevo tipo de Item al juego:
 *   1. Crear una subclase concreta de ItemDefinition
 *   2. Implementar su Registry correspondiente
 *   3. Listo — el sistema de loot lo reconocerá automáticamente
 *
 * ── EJEMPLOS DE USO ──────────────────────────────────────────────────────
 *
 *   // Weapon
 *   ItemDefinition def = new WeaponDefinition(...);
 *   String name = def.getDisplayName();
 *   ItemRarity rarity = def.getDefaultRarity();
 *
 *   // Bullet
 *   ItemDefinition def = new BulletDefinition(BulletType.SPRINGBULLET);
 *   BulletBehavior behavior = ((BulletDefinition) def).getType().create();
 *
 *   // Amulet
 *   ItemDefinition def = new AmuletDefinition(...);
 *   AmuletEffect effect = ((AmuletDefinition) def).effect;
 *
 * @see Game.Items.Types.Weapons.WeaponDefinition
 * @see Game.Items.Types.Bullets.BulletDefinition
 * @see Game.Items.Types.Ammulets.AmuletDefinition
 */
public abstract class ItemDefinition {

    /** ID único del Item (snake_case). Inmutable. */
    public final String id;

    /** Nombre visible al jugador en UI. Inmutable. */
    public final String displayName;

    /** Descripción del comportamiento/efecto para UI de selección. Inmutable. */
    public final String description;

    /** Rareza por defecto (puede sobreescribirse desde configuración externa). Inmutable. */
    public final ItemRarity defaultRarity;

    /**
     * Constructor protegido — solo subclases concretas pueden instanciar.
     *
     * @param id             identificador único (snake_case)
     * @param displayName    nombre visible al jugador
     * @param description    descripción del comportamiento/efecto
     * @param defaultRarity  rareza por defecto
     * @throws IllegalArgumentException si id o defaultRarity son null
     */
    protected ItemDefinition(String id,
                            String displayName,
                            String description,
                            ItemRarity defaultRarity) {
        if (id == null || id.isBlank())
            throw new IllegalArgumentException("id no puede estar vacío");
        if (defaultRarity == null)
            throw new IllegalArgumentException("defaultRarity no puede ser null");
        
        this.id            = id;
        this.displayName   = displayName != null ? displayName : id;
        this.description   = description != null ? description : "";
        this.defaultRarity = defaultRarity;
    }

    // ── Getters ───────────────────────────────────────────────────────────

    /** Retorna el ID único del Item. */
    public String getId() {
        return id;
    }

    /** Retorna el nombre visible al jugador. */
    public String getDisplayName() {
        return displayName;
    }

    /** Retorna la descripción del comportamiento/efecto. */
    public String getDescription() {
        return description;
    }

    /** Retorna la rareza por defecto. */
    public ItemRarity getDefaultRarity() {
        return defaultRarity;
    }

    // ── Object identity ───────────────────────────────────────────────────

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id='" + id + "', rarity=" + defaultRarity + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ItemDefinition that = (ItemDefinition) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
