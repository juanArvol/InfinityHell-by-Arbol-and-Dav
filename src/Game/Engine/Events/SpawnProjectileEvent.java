package Game.Engine.Events;

import Game.Engine.GameMath.SpaceLogic.Logic2D.Vector2D;

/**
 * Evento genérico de disparo de proyectil.
 *
 * ── HRFC-006 — Desacoplamiento del sistema de armas ─────────────────────
 * Reemplaza el uso de GameEvents.OnWeaponFireEvent en contextos de enemigos,
 * Bosses, torretas y trampas. OnWeaponFireEvent está diseñado para el
 * jugador y su sistema de armas — no para entidades del mundo.
 *
 * SpawnProjectileEvent puede ser emitido por:
 *   - Enemigos (BoneBarragePattern, proyectiles de Bosses)
 *   - Bosses    (ataques de área, invocaciones de proyectiles)
 *   - Trampas   (torretas, pinchos disparadores)
 *   - Torretas
 *   - El propio jugador si lo requiere en el futuro
 *
 * ── Campos ────────────────────────────────────────────────────────────────
 *   projectileTypeId — ID del tipo de proyectil a instanciar.
 *                      El ProjectileFactory (o sistema de spawning)
 *                      usa este ID para seleccionar la fábrica correcta.
 *                      Ejemplos: "sans.bone", "fireball", "arrow"
 *
 *   origin           — posición de origen del proyectil (típicamente el
 *                      centro del emisor). Null si el sistema infiere la
 *                      posición desde el contexto.
 *
 *   target           — posición destino o dirección del proyectil. Null si
 *                      el proyectil no tiene objetivo directo (p.ej. omnidireccional).
 *
 *   sourceEntity     — objeto emisor del proyectil (Enemy, Player, Turret, etc.)
 *                      Representado como Object para no crear dependencia
 *                      cruzada entre módulos. El listener hace cast si lo necesita.
 *
 *   speed            — velocidad inicial del proyectil. Si es 0, el sistema
 *                      usará el valor por defecto del tipo de proyectil.
 *
 * ── Uso ───────────────────────────────────────────────────────────────────
 *   // Enemigo emite:
 *   GameEventBus.GLOBAL.post(new SpawnProjectileEvent(
 *       "sans.bone",
 *       enemy.getCenter(),
 *       ctx.getCenter(),
 *       enemy,
 *       8.0
 *   ));
 *
 *   // Sistema de proyectiles escucha:
 *   GameEventBus.GLOBAL.subscribe(SpawnProjectileEvent.class, e -> {
 *       ProjectileFactory.spawn(e.projectileTypeId(), e.origin(), e.target(), e.speed());
 *   });
 */
public record SpawnProjectileEvent(
    String   projectileTypeId,
    Vector2D origin,
    Vector2D target,
    Object   sourceEntity,
    double   speed
) {
    /**
     * Constructor de conveniencia sin velocidad explícita.
     * El sistema usará el valor por defecto del tipo de proyectil.
     */
    public SpawnProjectileEvent(String projectileTypeId, Vector2D origin,
                                 Vector2D target, Object sourceEntity) {
        this(projectileTypeId, origin, target, sourceEntity, 0.0);
    }

    /**
     * Constructor de conveniencia mínimo — solo tipo y emisor.
     * Útil cuando origen y destino se infieren del contexto en el listener.
     */
    public SpawnProjectileEvent(String projectileTypeId, Object sourceEntity) {
        this(projectileTypeId, null, null, sourceEntity, 0.0);
    }
}
