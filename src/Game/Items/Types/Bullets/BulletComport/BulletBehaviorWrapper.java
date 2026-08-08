package Game.Items.Types.Bullets.BulletComport;

import Game.Engine.AbstractEntity;
import Game.Engine.GameObjects;
import Game.Items.Types.Bullets.Definition.Bullet;
import Game.Items.Types.Bullets.Definition.ProjectileData;
import Game.Items.Types.Bullets.ProjectileMovement;

/**
 * Decorator base para BulletBehavior.
 *
 * Permite apilar comportamientos de proyectil sin herencia múltiple.
 * Cada wrapper delega al inner y añade su propio efecto.
 *
 * ── HRFC — Weapon & Projectile System ────────────────────────────────────
 *
 * Se añade delegación de getDefaultData() y getDefaultMovement() al inner,
 * de modo que la cadena de wrappers propaga correctamente los datos del
 * behavior base sin necesidad de que cada wrapper los redeclare.
 *
 * ── DOS HOOKS SEMÁNTICOS ──────────────────────────────────────────────────
 *
 *   onHitEntity(Bullet, AbstractEntity) — impacto con cualquier entidad viva.
 *   onHitWorld(Bullet, GameObjects)     — impacto con cualquier objeto del mundo.
 *
 *   Los wrappers concretos sobreescriben solo el hook que necesitan.
 *   Para distinguir tipos concretos, usar instanceof dentro del hook:
 *
 *     protected void onHitEntity(Bullet b, AbstractEntity e) {
 *         if (e instanceof Enemy enemy) { enemy.addEffect(new PoisonEffect(...)); }
 *     }
 *
 * ── CADENA TÍPICA ────────────────────────────────────────────────────────
 *
 *   BulletBehavior base   = new BulletNormal();
 *   BulletBehavior poison = new PoisonBulletWrapper(base);
 *   BulletBehavior pierce = new PiercingAmuletWrapper(poison, 3);
 *   // pierce aplica normal → poison → piercing en cadena
 */
public abstract class BulletBehaviorWrapper extends BulletBehavior {

    protected final BulletBehavior inner;

    protected BulletBehaviorWrapper(BulletBehavior inner) {
        this.inner = inner;
    }

    // ── Delegación de datos al inner ──────────────────────────────────────

    /**
     * Delega los datos de configuración al inner.
     * La cadena de wrappers no modifica los datos del behavior base.
     * ModifiedWeapon calcula los datos finales externamente.
     */
    @Override
    public ProjectileData getDefaultData() {
        return inner.getDefaultData();
    }

    /**
     * Delega la estrategia de movimiento al inner.
     * Los wrappers no suelen cambiar el movimiento del proyectil.
     * Para movimiento especial, sobreescribir aquí.
     */
    @Override
    public ProjectileMovement getDefaultMovement() {
        return inner.getDefaultMovement();
    }

    // ── Delegación de ciclo de vida al inner ──────────────────────────────

    /**
     * Delega onAttached al inner y luego invoca el hook del wrapper.
     * Los wrappers que necesiten inicialización al adjuntarse sobreescriben
     * {@link #onWrapperAttached(Bullet)}.
     */
    @Override
    public void onAttached(Bullet bullet) {
        inner.onAttached(bullet);
        onWrapperAttached(bullet);
    }

    /**
     * Delega onDetached al inner y luego invoca el hook del wrapper.
     * Los wrappers que necesiten limpieza al separarse sobreescriben
     * {@link #onWrapperDetached(Bullet)}.
     */
    @Override
    public void onDetached(Bullet bullet) {
        inner.onDetached(bullet);
        onWrapperDetached(bullet);
    }

    /**
     * Propaga onRelease al inner y luego invoca el hook del wrapper.
     *
     * Los wrappers que necesiten cleanup garantizado al final del lifecycle
     * sobreescriben {@link #onWrapperRelease(Bullet)}.
     *
     * Llamado exactamente una vez por ciclo de vida, independientemente
     * de la causa de muerte. Es el punto correcto para:
     *   - Limpiar Sets de entidades impactadas (hitEntities).
     *   - Cancelar Subscriptions registradas en onAttached.
     *   - Liberar referencias externas.
     */
    @Override
    public void onRelease(Bullet bullet) {
        inner.onRelease(bullet);
        onWrapperRelease(bullet);
    }

    // ── Delegación de comportamiento ──────────────────────────────────────

    @Override
    public void onUpdate(Bullet bullet) {
        inner.onUpdate(bullet);
        onWrapperUpdate(bullet);
    }

    /**
     * Delega al inner y luego invoca el hook semántico correcto.
     *
     * ── HRFC — Projectile System Refactor ────────────────────────────────
     *
     * PROBLEMA ANTERIOR:
     *   Se invocaban siempre los hooks del wrapper (onHitEntity/onHitWorld)
     *   incluso cuando el inner ya había matado el proyectil con kill().
     *   Esto era frágil: los wrappers que necesitaban "no ejecutarse si ya murió"
     *   tenían que hacer ese check manualmente (PiercingAmuletWrapper lo hacía
     *   con revive(), BounceAmuletWrapper también). Era un contrato implícito
     *   que no estaba documentado y podía romperse fácilmente.
     *
     * SOLUCIÓN:
     *   Check explícito de isAlive() antes de invocar los hooks del wrapper.
     *   Si el inner mató el proyectil, los hooks del wrapper NO se ejecutan.
     *
     *   EXCEPCIÓN: los wrappers que NECESITAN ejecutarse aunque el proyectil
     *   muera (ej: un wrapper de explosión que explota al morir) deben
     *   sobreescribir onCollision() completo en lugar de usar los hooks.
     */
    @Override
    public void onCollision(Bullet bullet, GameObjects other) {
        inner.onCollision(bullet, other);

        // Si el inner mató el proyectil, no ejecutar los hooks del wrapper.
        // Esto evita que comportamientos adicionales se apliquen sobre un
        // proyectil ya destruido.
        if (!bullet.getBulletLife().isAlive()) return;

        if (other instanceof AbstractEntity entity) {
            onHitEntity(bullet, entity);
        } else {
            onHitWorld(bullet, other);
        }
    }

    // ── Delegación de onExpire ────────────────────────────────────────────

    /**
     * Delega el hook de expiración al inner y luego invoca el hook del wrapper.
     * Los wrappers pueden reaccionar a la expiración sobreescribiendo onWrapperExpire().
     */
    @Override
    public void onExpire(Bullet bullet,
                         Game.Items.Types.Bullets.Definition.ProjectileContext ctx) {
        inner.onExpire(bullet, ctx);
        onWrapperExpire(bullet, ctx);
    }

    // ── Contrato de estado (pool safety) ─────────────────────────────────

    /**
     * Un wrapper es stateless solo si TANTO el wrapper propio como el inner son stateless.
     * Si cualquiera de los dos tiene estado, el pool no reutilizará la instancia.
     * Las subclases que tengan estado propio deben sobreescribir retornando false.
     */
    @Override
    public boolean isBehaviorStateless() {
        return inner.isBehaviorStateless();
    }

    /**
     * Resetea el estado del inner y el estado propio del wrapper.
     * Las subclases con estado deben llamar super.resetBehaviorState()
     * y luego resetear sus propios campos.
     */
    @Override
    public void resetBehaviorState() {
        inner.resetBehaviorState();
    }

    // ── Hooks para subclases ──────────────────────────────────────────────

    /**
     * Llamado cada frame después de que el inner actualiza.
     * Override para lógica continua del wrapper (ej: rastro de partículas).
     */
    protected void onWrapperUpdate(Bullet bullet) {}

    /**
     * Llamado al impactar con cualquier entidad viva (Player, Enemy, NPC…).
     * Usar instanceof dentro si se necesita distinguir tipos concretos.
     */
    protected void onHitEntity(Bullet bullet, AbstractEntity entity) {}

    /**
     * Llamado al impactar con cualquier objeto del mundo (bloque, obstáculo…).
     * Usar instanceof dentro si se necesita distinguir tipos concretos.
     */
    protected void onHitWorld(Bullet bullet, GameObjects other) {}

    /**
     * Llamado cuando el proyectil expira (después de que el inner procesa la expiración).
     * Override para reaccionar a la expiración: explosiones, fragmentación, etc.
     */
    protected void onWrapperExpire(Bullet bullet,
                                   Game.Items.Types.Bullets.Definition.ProjectileContext ctx) {}

    /**
     * Llamado después de que el inner recibe onAttached.
     * Override en wrappers que necesiten inicialización al adjuntarse a un proyectil.
     */
    protected void onWrapperAttached(Bullet bullet) {}

    /**
     * Llamado después de que el inner recibe onDetached.
     * Override en wrappers que necesiten limpieza al separarse de un proyectil
     * VIVO por reemplazo de behavior. Para cleanup en muerte normal usar
     * {@link #onWrapperRelease(Bullet)}.
     */
    protected void onWrapperDetached(Bullet bullet) {}

    /**
     * Llamado justo antes de que el proyectil sea reciclado al pool o destruido.
     *
     * Se invoca siempre, independientemente de la causa de muerte.
     * Es el punto correcto para liberar recursos externos adquiridos en
     * onWrapperAttached: cancelar listeners, limpiar colecciones, liberar refs.
     *
     * Override en wrappers que mantengan estado externo (Sets de entidades,
     * Subscriptions, referencias a objetos del mundo).
     *
     * Default: sin efecto.
     */
    protected void onWrapperRelease(Bullet bullet) {}
}
