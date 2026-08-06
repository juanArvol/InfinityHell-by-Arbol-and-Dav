package Game.Items.Types.Bullets;

import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Items.Types.Bullets.BulletComport.BulletBehavior;
import Sprites.Entity.Bullets.BulletAssets;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Pool de proyectiles — reutilización de instancias para reducir GC pressure.
 *
 * ── HRFC — Projectile System Refactor ────────────────────────────────────
 *
 * En escenarios de bullet-hell con 200+ proyectiles activos simultáneamente,
 * crear un new Bullet() por disparo implica 7 allocaciones por proyectil.
 * ProjectilePool reutiliza instancias reseteando su estado.
 *
 * ── CAMBIOS RESPECTO A LA VERSIÓN ANTERIOR ───────────────────────────────
 *
 *   resetForReuse() ahora es COMPLETO:
 *     - Llama bullet.resetState() — método package-private en Bullet que
 *       resetea posición, velocidad, BulletLife y daño en una sola llamada.
 *     - damage ya no es final en Bullet, así que el pool puede reutilizar
 *       instancias con cualquier valor de daño.
 *     - destroyEventFired se resetea dentro de resetState().
 *
 *   Comportamiento de acquire() mejorado:
 *     - Si el movement del behavior es stateful (isStateless() == false),
 *       no reutiliza la instancia para ese tipo — crea una nueva.
 *       Esto es correcto: SinusoidalMovement tiene frameCount que no puede
 *       resetearse desde fuera sin acceso al estado interno del movement.
 *
 * ── DISEÑO ────────────────────────────────────────────────────────────────
 *
 * Pool por tipo de proyectil. Para bullet-hell, crear un pool por tipo.
 * Bounded (maxSize): el excedente se deja al GC si el pool está lleno.
 * Single-thread (el game loop es single-thread).
 *
 * ── CICLO DE VIDA ─────────────────────────────────────────────────────────
 *
 *   1. acquire()   → reutiliza del pool si hay disponibles, sino crea nuevo
 *   2. Bullet vive en el mundo (update, collisions, effects)
 *   3. isPendingDestruction() = true → WorldObjectsContainer.flush() lo elimina
 *   4. PooledBullet.isPendingDestruction() auto-devuelve al pool antes del flush
 */
public final class ProjectilePool {

    private static final int DEFAULT_MAX_SIZE = 64;

    private final Deque<PooledBullet> available = new ArrayDeque<>();
    private final int maxSize;

    private int totalAcquires = 0;
    private int poolHits      = 0;
    private int poolMisses    = 0;

    public ProjectilePool() {
        this(DEFAULT_MAX_SIZE);
    }

    public ProjectilePool(int maxSize) {
        this.maxSize = maxSize;
    }

    // ── Acquire ───────────────────────────────────────────────────────────

    /**
     * Obtiene un proyectil del pool o crea uno nuevo si el pool está vacío.
     *
     * Si el behavior tiene movimiento stateful (isStateless() == false),
     * siempre crea una instancia nueva — el estado del movement no puede
     * resetearse sin acceso a su estado interno.
     *
     * @param x         posición X de spawn
     * @param y         posición Y de spawn
     * @param direction dirección normalizada de vuelo
     * @param behavior  behavior del proyectil
     * @param speed     velocidad total (unidades/frame)
     * @param damage    daño al impactar
     * @return Bullet listo para añadir al mundo
     */
    public Bullet acquire(double x, double y, Vector2D direction,
                          BulletBehavior behavior, double speed, double damage) {
        totalAcquires++;

        ProjectileData data    = behavior.getDefaultData();
        ProjectileMovement mov = behavior.getDefaultMovement();
        double xSpeed          = direction.getX() * speed;
        double ySpeed          = direction.getY() * speed;

        // Solo reutilizar si el movement es stateless.
        // Stateful movements (SinusoidalMovement, BoomerangMovement) necesitan
        // una instancia nueva porque su estado interno (frameCount, etc.)
        // no puede resetearse externamente.
        if (mov.isStateless()) {
            PooledBullet pooled = available.pollFirst();
            if (pooled != null) {
                poolHits++;
                pooled.resetState(x, y, xSpeed, ySpeed, data.lifeTime(), damage);
                return pooled;
            }
        }

        poolMisses++;
        return new PooledBullet(
                new Vector2D(x, y),
                BulletAssets.balaHandle.resolveDefault().getImage(),
                behavior,
                mov,
                xSpeed, ySpeed,
                data.lifeTime(),
                damage,
                data.width(),
                data.height(),
                this
        );
    }

    // ── Release ───────────────────────────────────────────────────────────

    /**
     * Devuelve un proyectil al pool.
     * Solo acepta PooledBullet. Si el pool está lleno, descarta silenciosamente.
     */
    public void release(Bullet bullet) {
        if (!(bullet instanceof PooledBullet pb)) return;
        if (available.size() >= maxSize) return;
        available.addFirst(pb);
    }

    // ── Estadísticas ──────────────────────────────────────────────────────

    public int    getPoolSize()      { return available.size(); }
    public int    getTotalAcquires() { return totalAcquires; }
    public int    getPoolHits()      { return poolHits; }
    public int    getPoolMisses()    { return poolMisses; }
    public double getHitRate() {
        return (totalAcquires == 0) ? 0.0 : (double) poolHits / totalAcquires;
    }

    public void clear() { available.clear(); }

    // ── PooledBullet ──────────────────────────────────────────────────────

    /**
     * Subclase de Bullet que se auto-devuelve al pool al destruirse.
     *
     * isPendingDestruction() es el punto de devolución — se llama exactamente
     * una vez antes de que WorldObjectsContainer elimine el objeto del mundo.
     */
    public static final class PooledBullet extends Bullet {

        private final ProjectilePool owner;
        private boolean released = false;

        PooledBullet(Vector2D position, java.awt.image.BufferedImage texture,
                     BulletBehavior behavior, ProjectileMovement movement,
                     double xSpeed, double ySpeed, int lifeTime, double damage,
                     int colWidth, int colHeight, ProjectilePool owner) {
            super(position, texture, behavior, movement,
                  xSpeed, ySpeed, lifeTime, damage, colWidth, colHeight);
            this.owner = owner;
        }

        @Override
        public boolean isPendingDestruction() {
            boolean pending = super.isPendingDestruction();
            if (pending && !released) {
                released = true;
                owner.release(this);
            }
            return pending;
        }

        /**
         * Prepara la instancia para reutilización.
         * Resetea todo el estado mutable via Bullet.resetState() (package-private).
         * También resetea el flag released para el nuevo ciclo de vida.
         */
        @Override
        void resetState(double x, double y, double xSpeed, double ySpeed,
                        int lifeTime, double damage) {
            super.resetState(x, y, xSpeed, ySpeed, lifeTime, damage);
            this.released = false;
        }
    }
}
