package Game.Events;

import Game.Enemys.Enemy;
import Game.Items.ItemDefinition;
import Game.Player.Player;
import Game.Weapons.Modifiers.WeaponModifier;
import GameMath.Vector2D;

/**
 * Eventos del sistema — records inmutables emitidos via GameEventBus.
 *
 * AÑADIDO vs. versión anterior:
 *   - OnEnemyDeathEvent  → LootSystem, FXSystem, WaveSpawner
 *   - OnEnemyDamageEvent → UI de barra de vida, efectos de hit
 */
public final class GameEvents {

    private GameEvents() {}

    // ── Pickup / Drop ─────────────────────────────────────────────────────
    public record OnPickupEvent(Player player, ItemDefinition definition, int amount) {}
    public record OnDropEvent(Player player, ItemDefinition definition, int amount) {}

    // ── Armas ─────────────────────────────────────────────────────────────
    public record OnWeaponFireEvent(Player player, String sound) {}
    public record OnReloadStartEvent(Player player, int reloadTimeTicks) {}
    public record OnReloadCompleteEvent(Player player) {}
    public record OnModifierAppliedEvent(Player player, WeaponModifier modifier) {}
    public record OnModifierRemovedEvent(Player player, String modifierId) {}

    // ── Enemigos ──────────────────────────────────────────────────────────

    /**
     * Emitido cuando un enemigo llega a 0 HP.
     * Listeners: LootSystem, FXSystem, AudioSystem, WaveSpawner, ScoreSystem.
     */
    public record OnEnemyDeathEvent(Enemy enemy, Vector2D position) {}

    /**
     * Emitido cuando un enemigo recibe daño.
     * Útil para números flotantes de daño en pantalla.
     */
    public record OnEnemyDamageEvent(Enemy enemy, int amount, Vector2D position) {}

    // ── Física 3D / Salto ─────────────────────────────────────────────────
    public record OnJumpEvent(Object source, double impulse) {}
    public record OnLandEvent(Object source) {}
}
