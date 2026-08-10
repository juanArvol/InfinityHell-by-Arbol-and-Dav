package Game.Gameplay.Events;

import Game.Engine.GameObjects;
import Game.Items.Types.Bullets.Definition.Bullet;

/**
 * Catálogo de eventos del ciclo de vida de un proyectil.
 *
 * Vive en Game.Gameplay.Events porque transporta tipos concretos del Gameplay
 * (Bullet, GameObjects) y representa hechos del dominio del juego.
 *
 * Migrado desde: Game.Items.Types.Bullets.Definition.ProjectileEvents
 * Razón: los eventos concretos pertenecen al catálogo centralizado de Gameplay,
 * no al paquete de implementación de los proyectiles.
 *
 * ── CICLO DE VIDA ─────────────────────────────────────────────────────────
 *
 *   Spawn → Hit → (Pierce | Bounce)* → (Expire | Destroy)
 *
 *   OnProjectileSpawn   — proyectil creado y añadido al mundo
 *   OnProjectileHit     — impacto con cualquier objeto (entidad o mundo)
 *   OnProjectilePierce  — impacto con entidad pero el proyectil continúa
 *   OnProjectileBounce  — rebote en un objeto del mundo
 *   OnProjectileExpire  — ciclo de vida agotado (lifeTime = 0)
 *   OnProjectileDestroy — proyectil eliminado del mundo (cubre ambos casos)
 *
 * ── NOTA DE RENDIMIENTO ────────────────────────────────────────────────────
 *
 * En escenarios de bullet hell (200+ proyectiles/frame), emitir un evento
 * por cada impacto puede ser costoso si hay muchos suscriptores. Usar
 * hasListeners() antes de construir el evento para coste cero cuando no
 * hay nadie escuchando.
 */
public final class ProjectileEvents {

    private ProjectileEvents() {}

    // ── Spawn ──────────────────────────────────────────────────────────────

    /**
     * Emitido cuando un proyectil es creado y añadido al mundo.
     *
     * @param projectile el proyectil recién spawneado
     * @param owner      el objeto que lo disparó (Player, Enemy, Turret…);
     *                   puede ser null si no hay un dueño conocido
     */
    public record OnProjectileSpawn(Bullet projectile, Object owner) {}

    // ── Impacto ────────────────────────────────────────────────────────────

    /**
     * Emitido en cualquier colisión del proyectil con un objeto.
     *
     * @param projectile el proyectil que impactó
     * @param target     el objeto con el que colisionó
     */
    public record OnProjectileHit(Bullet projectile, GameObjects target) {}

    /**
     * Emitido cuando el proyectil perfora una entidad (sigue vivo tras el impacto).
     *
     * @param projectile el proyectil que perforó
     * @param target     la entidad que fue perforada
     */
    public record OnProjectilePierce(Bullet projectile, GameObjects target) {}

    /**
     * Emitido cuando el proyectil rebota en un objeto del mundo.
     *
     * @param projectile el proyectil que rebotó
     * @param surface    el objeto en el que rebotó
     */
    public record OnProjectileBounce(Bullet projectile, GameObjects surface) {}

    // ── Fin de vida ────────────────────────────────────────────────────────

    /**
     * Emitido cuando el lifeTime del proyectil llega a 0 sin impactar nada.
     *
     * @param projectile el proyectil que expiró
     */
    public record OnProjectileExpire(Bullet projectile) {}

    /**
     * Emitido cuando el proyectil es destruido (por impacto o por expiración).
     * Evento "catch-all" para limpieza — cubre ambos casos de destrucción.
     *
     * @param projectile el proyectil destruido
     */
    public record OnProjectileDestroy(Bullet projectile) {}
}
