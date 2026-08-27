package Game.Items.Types.Weapons;

import Game.Items.Creation.ItemDefinition;
import Game.Items.Creation.ItemRarity;
import Game.Items.Creation.VisualDefinition;
import Game.Items.Types.Bullets.BulletID;

/**
 * Definición concreta de un arma.
 *
 * ── ARQUITECTURA — Items Module ──────────────────────────────────────────
 *
 * WeaponDefinition especializa ItemDefinition para la familia Weapon.
 *
 * RESPONSABILIDAD:
 * - Definir la identidad del arma mediante WeaponID.
 * - Definir sus datos visuales.
 *
 * NO contiene comportamiento runtime.
 * El comportamiento pertenece a WeaponComport.
 */
public final class WeaponDefinition extends ItemDefinition {

    // ── Definiciones estáticas ────────────────────────────────────────────

    public static final WeaponDefinition PISTOLA =
            new WeaponDefinition(
                    WeaponID.PISTOLA,
                    new VisualDefinition(
                            "Pistola",
                            "Arma básica con cadencia moderada",
                            ItemRarity.COMMON
                    )
            );

    public static final WeaponDefinition ESCOPETA =
            new WeaponDefinition(
                    WeaponID.ESCOPETA,
                    new VisualDefinition(
                            "Escopeta",
                            "Dispara múltiples proyectiles en abanico",
                            ItemRarity.UNCOMMON
                    )
            );

    // ── Constructor ──────────────────────────────────────────────────────

    public WeaponDefinition(
            WeaponID itemId,
            VisualDefinition visual
    ) {
        super(itemId, visual);
    }
    public WeaponID getWeaponId() {
        return (WeaponID) getItemId();
    }
}