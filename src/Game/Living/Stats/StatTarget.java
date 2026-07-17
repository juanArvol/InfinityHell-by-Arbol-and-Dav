package Game.Living.Stats;

/**
 * Identificador tipado de la estadística objetivo de un StatModifier.
 *
 * ── HRFC-007 — Generalización al Living Entity Core ──────────────────────
 * Movido desde Game.Enemys.Core.Stats a Game.Living.Stats.
 * No conoce ninguna entidad concreta. Es el "routing key" del sistema de
 * modificadores: RuntimeStats lo usa para despachar cada StatModifier
 * a la categoría de stats correcta.
 *
 * ── Extensión ─────────────────────────────────────────────────────────────
 * Añadir una nueva estadística = añadir una entrada aquí + el campo en
 * la CategoryStats correspondiente + la lógica de aplicación en RuntimeStats.
 * Ningún otro archivo del Core necesita cambiar.
 */
public enum StatTarget {

    // ── MovementStats ─────────────────────────────────────────────────────
    MOVEMENT_SPEED,
    MOVEMENT_ACCELERATION,
    MOVEMENT_FRICTION,
    MOVEMENT_JUMP_HEIGHT,
    MOVEMENT_DASH_DISTANCE,

    // ── CombatStats ───────────────────────────────────────────────────────
    COMBAT_DAMAGE,
    COMBAT_DEFENSE,
    COMBAT_ATTACK_RANGE,
    COMBAT_ATTACK_COOLDOWN,
    COMBAT_CRITICAL_CHANCE,
    COMBAT_TELEPORT_RANGE,

    // ── PerceptionStats ───────────────────────────────────────────────────
    PERCEPTION_VISION_RANGE,
    PERCEPTION_HEARING_RANGE,
    PERCEPTION_DETECTION_ANGLE,

    // ── ResistanceStats ───────────────────────────────────────────────────
    RESISTANCE_FIRE,
    RESISTANCE_ICE,
    RESISTANCE_ELECTRIC,
    RESISTANCE_POISON,
    RESISTANCE_CURSE
}
