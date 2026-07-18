package Game.Engine.Entity.Stats;

/**
 * Identificador tipado de la estadística objetivo de un StatModifier.
 *
 * ── HRFC-007 — Generalización al Living Entity Core ──────────────────────
 * Movido desde Game.Enemys.Core.Stats a Game.Living.Stats.
 * No conoce ninguna entidad concreta. Es el "routing key" del sistema de
 * modificadores: RuntimeStats lo usa para despachar cada StatModifier
 * a la categoría de stats correcta.
 *
 * ── HRFC-013 — Consolidación Definitiva del Dominio Entity ───────────────
 * Añadidas entradas HEALTH_* para los campos modificables de HealthStats:
 *   HEALTH_MAX_HP                 — vida máxima (buffs de HP)
 *   HEALTH_HEALTH_REGEN           — regeneración de vida por frame
 *   HEALTH_HEALING_MULTIPLIER     — multiplicador de curación recibida
 *   HEALTH_INCOMING_DAMAGE_MULT   — multiplicador de daño entrante
 *
 * Nota: currentHp, shield, barrier NO tienen StatTarget porque son estado
 * de runtime directo, no "estadísticas" que los modificadores amplíen.
 * Los modificadores de HP máximo sí tienen sentido (ítem +50 maxHP).
 *
 * ── Extensión ─────────────────────────────────────────────────────────────
 * Añadir una nueva estadística = añadir una entrada aquí + el campo en
 * la CategoryStats correspondiente + la lógica de aplicación en RuntimeStats.
 * Ningún otro archivo del Core necesita cambiar.
 */
public enum StatTarget {

    // ── HealthStats ───────────────────────────────────────────────────────
    HEALTH_MAX_HP,
    HEALTH_HEALTH_REGEN,
    HEALTH_HEALING_MULTIPLIER,
    HEALTH_INCOMING_DAMAGE_MULT,

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
