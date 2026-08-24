package Game.Items.Types.Weapons;

import Game.Items.VisualDefinition;
import Game.Items.Creation.ItemDefinition;
import Game.Items.Creation.ItemRarity;

/**
 * Definiciones estáticas de tipos de arma.
 *
 * ── ARQUITECTURA FINAL — Items Module ────────────────────────────────────
 *
 * PROPÓSITO:
 *   Centralizar todas las definiciones (ID + visual) de armas en un solo lugar.
 *   WeaponType solo referencia estas definiciones + añade el factory.
 *
 * PATRÓN:
 *   WeaponDefinitions → ItemDefinition estática (ID + visual)
 *   WeaponType        → ObjectType (ItemDefinition + factory)
 */
public final class WeaponDefinitions {

    // ── Definiciones estáticas ────────────────────────────────────────────

    public static final ItemDefinition PISTOLA = new ItemDefinition(
        WeaponID.PISTOLA,
        new VisualDefinition(
            "Pistola",
            "Arma básica con cadencia moderada",
            ItemRarity.COMMON
        )
    );

    public static final ItemDefinition ESCOPETA = new ItemDefinition(
        WeaponID.ESCOPETA,
        new VisualDefinition(
            "Escopeta",
            "Dispara múltiples proyectiles en abanico",
            ItemRarity.UNCOMMON
        )
    );

    // ── Constructor privado ───────────────────────────────────────────────

    private WeaponDefinitions() {
        throw new AssertionError("No se puede instanciar WeaponDefinitions");
    }
}
