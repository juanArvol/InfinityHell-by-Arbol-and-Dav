package Game.Items.Savement;

/**
 * Slot de equipamiento — define dónde puede equiparse un ítem.
 *
 * Diseño intencionalmente simple. Si el juego agrega más slots
 * (mochila, cinturón, manos izq/der), se añaden aquí sin romper
 * nada existente.
 */
public enum EquipmentSlot {
    PRIMARY_WEAPON,    // Arma principal
    SECONDARY_WEAPON,  // Arma secundaria
    MELEE_WEAPON,      // Arma cuerpo a cuerpo
    ARMOR,             // Chaleco / armadura
    TOOL               // Herramienta activa (linterna, vendaje rápido)
}
