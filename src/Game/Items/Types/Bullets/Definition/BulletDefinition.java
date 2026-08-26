package Game.Items.Types.Bullets.Definition;

import Game.Items.Creation.ItemDefinition;
import Game.Items.Creation.ItemRarity;
import Game.Items.Creation.VisualDefinition;
import Game.Items.Types.Bullets.BulletID;

/**
 * Definiciones estáticas de tipos de bala.
 *
 * ── ARQUITECTURA FINAL — Items Module ────────────────────────────────────
 *
 * PROPÓSITO:
 *   Centralizar todas las definiciones (ID + visual) de balas en un solo lugar.
 *   BulletType solo referencia estas definiciones + añade el factory.
 *
 * PATRÓN:
 *   BulletDefinition → ItemDefinition estática (ID + visual)
 *   BulletType       → ObjectType (BulletDefinition + factory)
 */
public final class BulletDefinition {

    // ── Definiciones estáticas ────────────────────────────────────────────

    public static final ItemDefinition NORMAL_BULLET = new ItemDefinition(
        BulletID.NORMAL_BULLET,
        new VisualDefinition(
            "Bala Normal",
            "Bala estándar sin efectos especiales",
            ItemRarity.COMMON
        )
    );

    public static final ItemDefinition SPRING_BULLET = new ItemDefinition(
        BulletID.SPRING_BULLET,
        new VisualDefinition(
            "Bala Saltarina",
            "Bala que rebota al impactar",
            ItemRarity.UNCOMMON
        )
    );

    public static final ItemDefinition METEOR_BULLET = new ItemDefinition(
        BulletID.METEOR_BULLET,
        new VisualDefinition(
            "Bala Meteoro",
            "Bala ardiente con efecto de área",
            ItemRarity.RARE
        )
    );

    // ── Constructor privado ───────────────────────────────────────────────

    private BulletDefinition() {
        throw new AssertionError("No se puede instanciar BulletDefinition");
    }
}
