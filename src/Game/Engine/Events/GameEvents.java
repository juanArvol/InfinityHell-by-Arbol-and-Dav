package Game.Engine.Events;

import Game.Enemys.Enemy;
import Game.Engine.GameMath.SpaceLogic.Logic2D.Vector2D;
import Game.Items.Creation.ItemDefinition;
import Game.Items.Creation.ItemRarity;
import Game.Items.Types.Weapons.Modifiers.WeaponModifier;
import Game.Player.Player;

/**
 * Catálogo centralizado de eventos del Game.
 *
 * Vive en Game.Events porque todos los eventos aquí definidos referencian
 * tipos concretos del Game (Enemy, Player, ItemDefinition, WeaponModifier).
 * El Engine no debe conocer esos tipos — por eso este catálogo no pertenece
 * a Game.Engine.Events.
 *
 * MIGRADO DESDE: Game.Engine.Events.GameEvents
 * RAZÓN: el catálogo importaba Enemy, Player, ItemDefinition, WeaponModifier
 * desde el Engine, creando dependencias Engine → Game.* en 4 paquetes.
 *
 * NOTA sobre OnEnemyDeathEvent y OnPickupEvent:
 *   Tienen sus propios archivos standalone en sus paquetes naturales:
 *     Game.Enemys.OnEnemyDeathEvent
 *     Game.Items.OnPickupEvent
 *   Usar siempre esos, no definir records duplicados aquí.
 *
 * USO:
 *   // Emitir
 *   GameEventBus.GLOBAL.post(new GameEvents.OnWeaponFireEvent(player, sound));
 *
 *   // Suscribir
 *   GameEventBus.GLOBAL.subscribe(GameEvents.OnPlayerDeathEvent.class, e -> { ... });
 */
public final class GameEvents {

    private GameEvents() {}

    // ── Pickup / Drop ─────────────────────────────────────────────────────────

    public record OnDropEvent(Player player, ItemDefinition definition, int amount) {}

    // ── Armas ─────────────────────────────────────────────────────────────────

    public record OnWeaponFireEvent(Player player, String sound) {}
    public record OnReloadStartEvent(Player player, int reloadTimeTicks) {}
    public record OnReloadCompleteEvent(Player player) {}
    public record OnModifierAppliedEvent(Player player, WeaponModifier modifier) {}
    public record OnModifierRemovedEvent(Player player, String modifierId) {}

    // ── Enemigos ──────────────────────────────────────────────────────────────

    /**
     * Emitido cuando un enemigo recibe daño.
     * Útil para números flotantes de daño en pantalla (DamageNumberSystem).
     */
    public record OnEnemyDamageEvent(Enemy enemy, int amount, Vector2D position) {}

    /**
     * Emitido cuando un enemigo aparece en el mundo.
     * Útil para WaveSpawner, minimapa, audio de alerta.
     */
    public record OnEnemySpawnedEvent(Enemy enemy, Vector2D position) {}

    // ── Mundo ─────────────────────────────────────────────────────────────────

    public record OnWorldLoadedEvent(Object world) {}
    public record OnWorldUnloadedEvent(int coordX, int coordY) {}

    // ── Player ────────────────────────────────────────────────────────────────

    public record OnPlayerDeathEvent(Player player) {}
    public record OnPlayerDamagedEvent(Player player, int amount, Object source) {}

    // ── Sistema ───────────────────────────────────────────────────────────────

    public record OnGamePausedEvent() {}
    public record OnGameResumedEvent() {}

    // ── Física 3D / Salto ─────────────────────────────────────────────────────
    // Nota: usan Object para no acoplar el evento a GameObjects del Engine.
    // El listener hace cast si necesita el objeto concreto.

    public record OnJumpEvent(Object source, double impulse) {}
    public record OnLandEvent(Object source) {}
}
