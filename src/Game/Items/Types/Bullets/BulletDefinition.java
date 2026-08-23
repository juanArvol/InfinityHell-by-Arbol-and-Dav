package Game.Items.Types.Bullets;

import Game.Items.Creation.ItemRarity;
import Game.Items.ItemDefinition;
import Game.Items.Types.Bullets.Definition.BulletType;

/**
 * Definición de datos de un tipo de bala — su "plantilla" estática por run.
 *
 * ── JERARQUÍA ────────────────────────────────────────────────────────────
 * Extiende ItemDefinition para heredar la estructura común de metadata
 * (id, displayName, description, defaultRarity).
 *
 * ── DISEÑO ───────────────────────────────────────────────────────────────
 * Separa los DATOS del tipo de bala (nombre, descripción, rareza) de su
 * COMPORTAMIENTO (BulletBehavior via BulletType). Esto permite que el sistema
 * de loot y la UI accedan a la metadata sin necesitar instanciar el behavior.
 *
 * ── EQUIVALENCIA ARQUITECTÓNICA ──────────────────────────────────────────
 *
 *   WeaponDefinition : WeaponType :: BulletDefinition : BulletType
 *   AmuletDefinition : effect      :: BulletDefinition : BulletType
 *
 * Todas las Definitions comparten la misma estructura heredada de ItemDefinition:
 *   - id (String)
 *   - displayName (String)
 *   - description (String)
 *   - defaultRarity (ItemRarity)
 *
 * ── UNICIDAD POR RUN ─────────────────────────────────────────────────────
 * Igual que WeaponType, un tipo de bala solo puede obtenerse una vez por run.
 * El sistema de loot filtra los ya obtenidos antes de ofrecerlos.
 *
 * ── RAREZA CONFIGURABLE ──────────────────────────────────────────────────
 * La rareza por defecto está en BulletType. Puede sobreescribirse
 * externamente (desde un archivo de configuración de balance) sin tocar
 * el código fuente.
 *
 * ── RELACIÓN CON BulletType ──────────────────────────────────────────────
 *
 * BulletType es la identidad del tipo + capacidad de crear BulletBehavior.
 * BulletDefinition es la metadata para loot/UI + referencia al tipo.
 *
 *   BulletType       → identidad + construcción (bulletType.create())
 *   BulletDefinition → metadata + referencia al tipo
 *
 * Uso:
 *   BulletDefinition def = new BulletDefinition(BulletType.NORMALBULLET);
 *   BulletBehavior behavior = def.getType().create();
 *
 * @see Game.Items.ItemDefinition                    clase base con metadata común
 * @see Game.Items.Types.Bullets.Definition.BulletType tipo que construye el behavior
 */
public final class BulletDefinition extends ItemDefinition {

    /** Tipo de bala (identidad + factory de behavior). */
    private final BulletType type;

    /**
     * Construye una definición desde un BulletType.
     * Todos los datos se derivan del tipo.
     *
     * @param type tipo de bala. No puede ser null.
     */
    public BulletDefinition(BulletType type) {
        super(type.id, type.displayName, type.description, type.defaultRarity);
        this.type = type;
    }

    /**
     * Constructor con override de metadata.
     * Útil para configuración externa que quiere cambiar nombre/descripción
     * sin modificar el BulletType.
     *
     * @param type          tipo de bala
     * @param displayName   nombre custom (null = usar del tipo)
     * @param description   descripción custom (null = usar del tipo)
     * @param defaultRarity rareza custom (null = usar del tipo)
     */
    public BulletDefinition(BulletType type,
                           String displayName,
                           String description,
                           ItemRarity defaultRarity) {
        super(
            type.id,
            displayName != null ? displayName : type.displayName,
            description != null ? description : type.description,
            defaultRarity != null ? defaultRarity : type.defaultRarity
        );
        this.type = type;
    }

    /**
     * Retorna el BulletType asociado.
     * Desde ahí se puede crear el BulletBehavior con type.create().
     *
     * @return el tipo de bala. Nunca null.
     */
    public BulletType getType() {
        return type;
    }
}
