package Game.Items.Types.Bullets.Definition;

import Game.Items.Creation.ItemDefinition;
import Game.Items.Creation.ItemRarity;
import Game.Items.Creation.VisualDefinition;
import Game.Items.Types.Bullets.BulletID;

/**
 * Definición concreta de una bala.
 *
 * ── ARQUITECTURA — Items Module ──────────────────────────────────────────
 *
 * BulletDefinition especializa ItemDefinition para la familia Bullet.
 *
 * RESPONSABILIDAD:
 * - Definir la identidad de la bala mediante BulletID.
 * - Definir sus datos visuales.
 *
 * NO contiene comportamiento runtime.
 * El comportamiento pertenece a BulletBehavior.
 */
public final class BulletDefinition extends ItemDefinition {

    // ── Definiciones estáticas ────────────────────────────────────────────

    public static final BulletDefinition NORMAL_BULLET =
            new BulletDefinition(
                    BulletID.NORMAL_BULLET,
                    new VisualDefinition(
                            "Bala Normal",
                            "Bala estándar sin efectos especiales",
                            ItemRarity.COMMON
                    )
            );

    public static final BulletDefinition SPRING_BULLET =
            new BulletDefinition(
                    BulletID.SPRING_BULLET,
                    new VisualDefinition(
                            "Bala Saltarina",
                            "Bala que rebota al impactar",
                            ItemRarity.UNCOMMON
                    )
            );

    public static final BulletDefinition METEOR_BULLET =
            new BulletDefinition(
                    BulletID.METEOR_BULLET,
                    new VisualDefinition(
                            "Bala Meteoro",
                            "Bala ardiente con efecto de área",
                            ItemRarity.RARE
                    )
            );

    // ── Constructor ──────────────────────────────────────────────────────

    public BulletDefinition(
            BulletID itemId,
            VisualDefinition visual
    ) {
        super(itemId, visual);
    }

    // ── Identidad específica ─────────────────────────────────────────────

    /**
     * Retorna el identificador específico de esta bala.
     */
    public BulletID getBulletId() {
        return (BulletID) getItemId();
    }
}