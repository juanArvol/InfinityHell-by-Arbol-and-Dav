package Game.Events;

import Game.Items.ItemDefinition;
import Game.Player.Player;
import Game.Weapons.Modifiers.WeaponModifier;

/**
 * Eventos del sistema — cada record es un evento inmutable.
 *
 * Todos son records para que sean simples de crear y consumir.
 * Se emiten via GameEventBus.post(new OnXxxEvent(...)).
 */
public final class GameEvents {

    private GameEvents() {}

    // ── Pickup ────────────────────────────────────────────────────────────

    /**
     * Emitido cuando el jugador recoge un ítem del mundo.
     * @param player     quien lo recogió
     * @param definition qué ítem es
     * @param amount     cuántas unidades se recogieron
     */
    public record OnPickupEvent(Player player, ItemDefinition definition, int amount) {}

    /**
     * Emitido cuando el jugador suelta un ítem al mundo.
     */
    public record OnDropEvent(Player player, ItemDefinition definition, int amount) {}

    // ── Armas ─────────────────────────────────────────────────────────────

    /**
     * Emitido cada vez que el arma dispara.
     * @param sound nombre del sonido a reproducir (puede ser null)
     */
    public record OnWeaponFireEvent(Player player, String sound) {}

    /**
     * Emitido cuando comienza la recarga de un arma.
     */
    public record OnReloadStartEvent(Player player, int reloadTimeTicks) {}

    /**
     * Emitido cuando termina la recarga.
     */
    public record OnReloadCompleteEvent(Player player) {}

    /**
     * Emitido cuando se aplica un modificador a un arma.
     */
    public record OnModifierAppliedEvent(Player player, WeaponModifier modifier) {}

    /**
     * Emitido cuando se quita un modificador de un arma.
     */
    public record OnModifierRemovedEvent(Player player, String modifierId) {}

    // ── Física 3D / Salto ────────────────────────────────────────────────

    /**
     * Emitido cuando el objeto con Physics3DComponent inicia un salto.
     */
    public record OnJumpEvent(Object source, double impulse) {}

    /**
     * Emitido cuando el objeto aterriza (Z llega a 0).
     */
    public record OnLandEvent(Object source) {}
}
