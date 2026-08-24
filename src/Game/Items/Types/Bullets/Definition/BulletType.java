package Game.Items.Types.Bullets.Definition;

import Game.Items.Core.ObjectType;
import Game.Items.Core.ObjectTypeFactory;
import Game.Items.Creation.ItemDefinition;
import Game.Items.Types.Bullets.BulletComport.BulletBehavior;
import Game.Items.Types.Bullets.BulletComport.BulletClass.*;
import java.util.function.Supplier;

/**
 * Tipos de bala — efectos únicos que se obtienen una sola vez por run.
 *
 * ── ARQUITECTURA FINAL — Items Module ────────────────────────────────────
 *
 * BulletType SOLO contiene las instancias estáticas de tipos de bala.
 * Toda la lógica de registro y consulta está en ObjectTypeFactory.
 *
 * PATRÓN:
 *   BulletDefinition → ItemDefinition estática (ID + visual)
 *   BulletType       → ObjectType (BulletDefinition + factory)
 *
 * @see ObjectType contenedor base
 * @see ObjectTypeFactory lógica de registro
 */
public final class BulletType extends ObjectType<BulletBehavior> {

    // ── Tipos predefinidos ────────────────────────────────────────────────

    public static final BulletType NORMAL_BULLET;
    public static final BulletType SPRING_BULLET;
    public static final BulletType METEOR_BULLET;

    static {
        NORMAL_BULLET = register(new BulletType(
            BulletDefinition.NORMAL_BULLET,
            BulletNormal::new
        ));

        SPRING_BULLET = register(new BulletType(
            BulletDefinition.SPRING_BULLET,
            BulletJump::new
        ));

        METEOR_BULLET = register(new BulletType(
            BulletDefinition.METEOR_BULLET,
            MetheorBullet::new
        ));
    }

    // ── Constructor privado ───────────────────────────────────────────────

    private BulletType(ItemDefinition definition, Supplier<BulletBehavior> factory) {
        super(definition, factory);
    }

    // ── API específica de dominio ─────────────────────────────────────────

    /**
     * Crea una nueva instancia del BulletBehavior asociado.
     * Alias de createInstance() para mayor claridad en el dominio.
     */
    public BulletBehavior create() {
        return createInstance();
    }

    // ── Registro privado ──────────────────────────────────────────────────

    private static BulletType register(BulletType type) {
        return ObjectTypeFactory.register(BulletType.class, type);
    }
}
