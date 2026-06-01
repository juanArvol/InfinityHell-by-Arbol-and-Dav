package Game.Engine.Events;

import Game.Enemys.Enemy;
import Game.Engine.GameMath.SpaceLogic.Logic2D.Vector2D;
import Game.Items.Creation.ItemDefinition;
import Game.Items.Creation.ItemRarity;
import Game.Items.Types.Weapons.Modifiers.WeaponModifier;
import Game.Player.Player;

/**
 * Catálogo centralizado de eventos del sistema.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * CORRECCIÓN DE CONTRATOS DIVERGENTES
 *
 * PROBLEMA:
 *   GameEvents definía internamente OnEnemyDeathEvent y OnPickupEvent como
 *   records internos. Simultáneamente existían los archivos standalone:
 *     Game.Engine.Events.OnPickupEvent
 *     Game.Engine.Events.OnEnemyDeathEvent
 *
 *   Son CLASES DISTINTAS para la JVM. Un sistema que emite con una clase
 *   y un listener que suscribe con la otra → evento nunca recibido.
 *   Bug silencioso: compila, no produce excepción, simplemente no funciona.
 *
 * CAUSA RAÍZ:
 *   Refactor incompleto. Se añadieron los records internos como catálogo
 *   pero los archivos standalone no fueron eliminados. Ambos existían.
 *
 * SOLUCIÓN:
 *   Los eventos con archivo standalone propio (OnPickupEvent, OnEnemyDeathEvent)
 *   son la fuente de verdad. Se ELIMINAN los records internos duplicados.
 *   GameEvents mantiene únicamente los eventos que NO tienen standalone propio.
 *
 * CONTRATOS DE IMPORT (regla única):
 *   Siempre importar desde Game.Engine.Events.OnPickupEvent (standalone).
 *   Nunca desde Game.Engine.Events.GameEvents.OnPickupEvent (ya no existe).
 *
 * ──────────────────────────────────────────────────────────────────────────
 * USO
 *
 *   // Emitir
 *   eventBus.post(new OnEnemyDeathEvent(enemy, pos));
 *   eventBus.post(new GameEvents.OnWeaponFireEvent(player, sound));
 *
 *   // Suscribir
 *   eventBus.subscribe(OnEnemyDeathEvent.class, e -> { ... });
 *   eventBus.subscribe(GameEvents.OnPlayerDeathEvent.class, e -> { ... });
 */
public final class GameEvents {

    private GameEvents() {}

    // ── Pickup / Drop ─────────────────────────────────────────────────────────
    //
    // NOTA: OnPickupEvent tiene su propia clase standalone.
    // → import Game.Engine.Events.OnPickupEvent;
    // → NO usar GameEvents.OnPickupEvent (eliminado para evitar ambigüedad).

    public record OnDropEvent(Player player, ItemDefinition definition, int amount) {}

    // ── Armas ─────────────────────────────────────────────────────────────────

    public record OnWeaponFireEvent(Player player, String sound) {}
    public record OnReloadStartEvent(Player player, int reloadTimeTicks) {}
    public record OnReloadCompleteEvent(Player player) {}
    public record OnModifierAppliedEvent(Player player, WeaponModifier modifier) {}
    public record OnModifierRemovedEvent(Player player, String modifierId) {}

    // ── Enemigos ──────────────────────────────────────────────────────────────
    //
    // NOTA: OnEnemyDeathEvent tiene su propia clase standalone.
    // → import Game.Engine.Events.OnEnemyDeathEvent;
    // → NO usar GameEvents.OnEnemyDeathEvent (eliminado para evitar ambigüedad).

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

    /**
     * Emitido cuando se carga un nuevo mundo (tras transición o inicio).
     * Object para evitar dependencia circular con el sistema de World.
     */
    public record OnWorldLoadedEvent(Object world) {}

    /**
     * Emitido cuando se descarga un mundo (antes de eliminarlo del cache).
     * Útil para SaveSystem (auto-save antes de salir).
     */
    public record OnWorldUnloadedEvent(int coordX, int coordY) {}

    // ── Player ────────────────────────────────────────────────────────────────

    /**
     * Emitido cuando el player muere.
     * Listeners: GameOverSystem, AudioSystem, StatsSystem.
     */
    public record OnPlayerDeathEvent(Player player) {}

    /**
     * Emitido cuando el player recibe daño.
     * Listeners: UISystem (flash de pantalla), AudioSystem.
     */
    public record OnPlayerDamagedEvent(Player player, int amount, Object source) {}

    // ── Sistema ───────────────────────────────────────────────────────────────

    public record OnGamePausedEvent() {}
    public record OnGameResumedEvent() {}

    // ── Física 3D / Salto ─────────────────────────────────────────────────────

    public record OnJumpEvent(Object source, double impulse) {}
    public record OnLandEvent(Object source) {}
}
