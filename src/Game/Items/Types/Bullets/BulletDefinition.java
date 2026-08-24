package Game.Items.Types.Bullets;

import Game.Items.Creation.ItemDefinition;
import Game.Items.Types.Bullets.Definition.BulletType;

/**
 * Definición de datos de un tipo de bala — wrapper sobre BulletType.
 *
 * ── ARQUITECTURA — Items Module ──────────────────────────────────────────
 *
 * BulletDefinition simplemente envuelve un BulletType y expone sus datos
 * a través de ItemDefinition.
 *
 * @see Game.Items.Creation.ItemDefinition
 * @see Game.Items.Types.Bullets.Definition.BulletType
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
        super(type.getDefinition().getItemId(), type.getDefinition().getVisual());
        this.type = type;
    }

    /**
     * Retorna el BulletType asociado.
     */
    public BulletType getType() {
        return type;
    }
}
