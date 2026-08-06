package Game.Engine.Events;

import Game.Enemys.Core.Enemy;
import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Items.Creation.ItemDefinition;
import Game.Player.Player;

/**
 * Catálogo centralizado de eventos del Game.
 *
 * Vive en Game.Events porque todos los eventos aquí definidos referencian
 * tipos concretos del Game (Enemy, Player, ItemDefinition).
 * El Engine no debe conocer esos tipos — por eso este catálogo no pertenece
 * a Game.Engine.Events.
 *
 * ── HRFC — Projectile System Refactor ────────────────────────────────────
 * Eliminados: OnModifierAppliedEvent, OnModifierRemovedEvent.
 * El sistema de modificadores legacy (WeaponModifier) fue eliminado.
 * El sistema de amuletos (AmuletRegistry) reemplaza esa funcionalidad.
 * Los eventos de amuletos viven en WeaponEvents (OnWeaponFired, etc.).
 */
public final class GameEvents {

    private GameEvents() {}

    // ── Pickup / Drop ─────────────────────────────────────────────────────────

    public record OnDropEvent(Player player, ItemDefinition definition, int amount) {}

    // ── Armas ─────────────────────────────────────────────────────────────────

    public record OnWeaponFireEvent(Player player, String sound) {}
    public record OnReloadStartEvent(Player player, int reloadTimeTicks) {}
    public record OnReloadCompleteEvent(Player player) {}

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

    public record OnJumpEvent(Object source, double impulse) {}
    public record OnLandEvent(Object source) {}
}
