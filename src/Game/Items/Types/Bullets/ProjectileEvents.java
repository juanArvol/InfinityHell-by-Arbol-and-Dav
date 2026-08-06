package Game.Items.Types.Bullets;

import Game.Engine.GameObjects;

/**
 * Catálogo de eventos del ciclo de vida de un proyectil.
 *
 * ── HRFC — Weapon & Projectile System ────────────────────────────────────
 *
 * Centraliza todos los eventos que un proyectil puede emitir durante su vida.
 * Suscriptores opcionales (VFX, audio, achievements, analytics, UI de combate)
 * se registran en GameEventBus.GLOBAL.
 *
 * Ningún evento es obligatorio para el funcionamiento del sistema.
 * Si no hay suscriptores, el bus no invoca nada (coste cero gracias a
 * GameEventBus.hasListeners()).
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
 *   OnProjectileDestroy — proyectil eliminado del mundo (cubrir ambos casos)
 *
 * ── USO ────────────────────────────────────────────────────────────────────
 *
 *   // Suscribir desde VFXSystem, AudioSystem, etc.:
 *   GameEventBus.GLOBAL.subscribe(ProjectileEvents.OnProjectileHit.class, e -> {
 *       VFXSystem.spawnImpact(e.projectile().getTransform().getPosition());
 *   });
 *
 *   GameEventBus.GLOBAL.subscribe(ProjectileEvents.OnProjectileExpire.class, e -> {
 *       AudioSystem.play("dissipate.wav");
 *   });
 *
 * ── NOTA DE RENDIMIENTO ────────────────────────────────────────────────────
 *
 * En escenarios de bullet hell (200+ proyectiles/frame), emitir un evento
 * por cada impacto puede ser costoso si hay muchos suscriptores. Para esos
 * casos, usar un sistema de batching o desactivar los eventos de alta
 * frecuencia (OnProjectileHit) cuando el VFX no sea crítico.
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
     * Se emite ANTES de que el behavior procese la colisión.
     *
     * @param projectile el proyectil que impactó
     * @param target     el objeto con el que colisionó
     */
    public record OnProjectileHit(Bullet projectile, GameObjects target) {}

    /**
     * Emitido cuando el proyectil perfora una entidad (sigue vivo tras el impacto).
     * Emitido por PiercingAmuletWrapper / PiercingModifier después de revive().
     *
     * @param projectile el proyectil que perforó
     * @param target     la entidad que fue perforada
     */
    public record OnProjectilePierce(Bullet projectile, GameObjects target) {}

    /**
     * Emitido cuando el proyectil rebota en un objeto del mundo.
     * Emitido por BounceAmuletWrapper o BulletJump después de cambiar dirección.
     *
     * @param projectile el proyectil que rebotó
     * @param surface    el objeto en el que rebotó
     */
    public record OnProjectileBounce(Bullet projectile, GameObjects surface) {}

    // ── Fin de vida ────────────────────────────────────────────────────────

    /**
     * Emitido cuando el lifeTime del proyectil llega a 0 sin impactar nada.
     * El proyectil se destruye en el próximo flush del WorldObjectsContainer.
     *
     * @param projectile el proyectil que expiró
     */
    public record OnProjectileExpire(Bullet projectile) {}

    /**
     * Emitido cuando el proyectil es destruido (por impacto o por expiración).
     * Es el evento "catch-all" para limpieza — cubre ambos casos de destrucción.
     *
     * @param projectile el proyectil destruido
     */
    public record OnProjectileDestroy(Bullet projectile) {}
}
