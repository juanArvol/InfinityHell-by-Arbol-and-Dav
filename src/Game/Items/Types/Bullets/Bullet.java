package Game.Items.Types.Bullets;

import Game.Engine.Colisions.Filter.CollisionProfile;
import Game.Engine.Entity.Components.Collisions.ColliderComponent;
import Game.Engine.Entity.Components.Physics2DComponent;
import Game.Engine.Entity.Components.Visuals.HitBoxComponent;
import Game.Engine.Entity.Components.Visuals.SpriteRendererComponent;
import Game.Engine.Events.GameEventBus;
import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Engine.Physics.KineticPhysics.PhysicsStepper;
import Game.Engine.GameObjects;
import Game.Items.Types.Bullets.BulletComport.BulletBehavior;
import Game.Items.Types.Bullets.Movement.LinearMovement;
import Game.World.WorldObjects.WorldObjectsContainer;
import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * Proyectil del juego — entidad en el mundo con ciclo de vida finito.
 *
 * ── HRFC — Projectile System Refactor ────────────────────────────────────
 *
 * ── SEPARACIÓN DE RESPONSABILIDADES ──────────────────────────────────────
 *
 *   Bullet             → entidad en el mundo (posición, colisiones, ciclo de vida)
 *   BulletBehavior     → QUÉ HACE al impactar y en cada frame
 *   ProjectileMovement → CÓMO SE MUEVE cada frame
 *   ProjectileData     → QUÉ VALORES tiene al spawn (datos inmutables)
 *   BulletPhysics      → velocidad y movimiento por física
 *
 * ── CAMBIOS EN ESTA VERSIÓN ───────────────────────────────────────────────
 *
 *   damage ya NO es final:
 *     El pool necesita poder resetear el daño al reutilizar una instancia.
 *     El setter resetDamage() es package-private — solo accesible desde
 *     el paquete Game.Items.Types.Bullets (pool y factory).
 *
 *   destroyEventFired ya NO es un campo fijo:
 *     Se resetea en resetState() para que el pool pueda reutilizar la
 *     instancia con eventos correctos en el segundo y siguientes usos.
 *
 *   BulletLife.advance() en lugar de tick():
 *     advance() es la nomenclatura correcta — separa avance de consulta.
 *
 *   CollisionProfile.PLAYER_BULLET por defecto:
 *     Elimina el friendly fire. Los proyectiles enemigos deben llamar
 *     setCollisionProfile(CollisionProfile.ENEMY_BULLET) tras la construcción.
 *
 * ── CICLO DE VIDA ─────────────────────────────────────────────────────────
 *
 *   Spawn    → BulletFactory.create*() construye la instancia
 *   Alive    → update() se llama cada frame:
 *                1. BulletLife.advance()      → ¿sigue vivo?
 *                2. movement.tick(this)        → actualizar velocidad
 *                3. behavior.onUpdate(this)    → lógica del behavior
 *                4. moveByPhysics()            → mover la posición
 *                5. super.update()             → actualizar Components
 *   Impact   → onCollisionWith(other) → behavior.onCollision(this, other)
 *   Expire   → BulletLife.advance() retorna false → isPendingDestruction() = true
 *   Destroy  → WorldObjectsContainer.flush() lo elimina del mundo
 *   Pool     → (si PooledBullet) resetState() y devuelto al pool
 */
public class Bullet extends GameObjects implements WorldObjectsContainer.Destroyable {

    private final BulletBehavior      behavior;
    private final ProjectileMovement  movement;
    private final BulletLife          bulletLife;
    private final Physics2DComponent  physicsComponent;

    /**
     * Daño del proyectil. No es final para permitir el reset del pool.
     * Modificar solo desde resetDamage() (package-private).
     */
    private double damage;

    /**
     * Flag para emitir OnProjectileDestroy exactamente una vez por ciclo de vida.
     * Se resetea en resetState() para el pool.
     */
    private boolean destroyEventFired = false;

    /**
     * Constructor completo.
     *
     * @param position   posición inicial de spawn
     * @param texture    sprite del proyectil (null = invisible, para raycast)
     * @param behavior   comportamiento de impacto y update
     * @param movement   estrategia de movimiento por frame
     * @param xSpeed     velocidad inicial en X (unidades/frame)
     * @param ySpeed     velocidad inicial en Y (unidades/frame)
     * @param lifeTime   ticks de vida máximos
     * @param damage     daño que aplica al impactar
     * @param colWidth   ancho del collider en píxeles
     * @param colHeight  alto del collider en píxeles
     */
    public Bullet(
            Vector2D           position,
            BufferedImage      texture,
            BulletBehavior     behavior,
            ProjectileMovement movement,
            double             xSpeed,
            double             ySpeed,
            int                lifeTime,
            double             damage,
            int                colWidth,
            int                colHeight
    ) {
        getTransform().setPosition(position);

        this.behavior   = behavior;
        this.movement   = (movement != null) ? movement : LinearMovement.INSTANCE;
        this.damage     = damage;
        this.bulletLife = new BulletLife(lifeTime);

        // ── Render ────────────────────────────────────────────────────────
        if (texture != null) {
            addComponent(new SpriteRendererComponent(texture));
        }

        // ── Collider (TRIGGER) — PLAYER_BULLET por defecto ────────────────
        // Los proyectiles enemigos llaman setCollisionProfile(ENEMY_BULLET)
        // después de la construcción desde ProjectileRegistry/BulletFactory.
        ColliderComponent collider = new ColliderComponent(
                colWidth, colHeight, CollisionProfile.PLAYER_BULLET);
        collider.setType(ColliderComponent.Type.TRIGGER);
        addComponent(collider);

        addComponent(new HitBoxComponent(Color.YELLOW));

        // ── Physics ───────────────────────────────────────────────────────
        BulletPhysics physics = new BulletPhysics(xSpeed, ySpeed);
        physicsComponent = new Physics2DComponent(physics);
        addComponent(physicsComponent);
    }

    /**
     * Constructor de compatibilidad — collider 8×8, LinearMovement.
     */
    public Bullet(
            Vector2D       position,
            BufferedImage  texture,
            BulletBehavior behavior,
            double         xSpeed,
            double         ySpeed,
            int            lifeTime,
            double         damage
    ) {
        this(position, texture, behavior, LinearMovement.INSTANCE,
             xSpeed, ySpeed, lifeTime, damage, 8, 8);
    }

    // ── Update ────────────────────────────────────────────────────────────

    @Override
    public void update() {
        // advance() decrementa y retorna si sigue vivo
        if (!bulletLife.advance()) {
            emitExpireAndDestroy();
            return;
        }

        movement.tick(this);
        behavior.onUpdate(this);
        moveByPhysics();
        super.update();
    }

    // ── Colisión ──────────────────────────────────────────────────────────

    @Override
    public void onCollisionWith(GameObjects other) {
        if (GameEventBus.GLOBAL.hasListeners(ProjectileEvents.OnProjectileHit.class)) {
            GameEventBus.GLOBAL.post(new ProjectileEvents.OnProjectileHit(this, other));
        }

        behavior.onCollision(this, other);

        if (!bulletLife.isAlive()) {
            emitDestroy();
        }
    }

    // ── Movimiento ────────────────────────────────────────────────────────

    public void moveByPhysics() {
        var vel = getPhysics().getVelocity();
        PhysicsStepper.moveWith(this, vel.getX(), vel.getY());
    }

    // ── Destroyable ────────────────────────────────────────────────────────

    @Override
    public boolean isPendingDestruction() {
        return !bulletLife.isAlive();
    }

    // ── API pública ────────────────────────────────────────────────────────

    public BulletLife         getBulletLife() { return bulletLife; }
    public double             getDamage()     { return damage; }
    public BulletBehavior     getBehavior()   { return behavior; }
    public ProjectileMovement getMovement()   { return movement; }

    public BulletPhysics getPhysics() {
        return (BulletPhysics) physicsComponent.getPhysics();
    }

    /**
     * Cambia el perfil de colisión de este proyectil.
     *
     * Llamar inmediatamente después de crear el proyectil si no es del jugador:
     *   bullet.setCollisionProfile(CollisionProfile.ENEMY_BULLET);
     *
     * @param profile el nuevo CollisionProfile
     */
    public void setCollisionProfile(CollisionProfile profile) {
        ColliderComponent col = getComponent(ColliderComponent.class);
        if (col != null) col.setProfile(profile);
    }

    // ── Reset para pool (package-private) ────────────────────────────────

    /**
     * Resetea el estado mutable del proyectil para reutilización por el pool.
     *
     * package-private: solo accesible desde ProjectilePool (mismo paquete).
     * No usar desde código de gameplay.
     *
     * @param x        nueva posición X
     * @param y        nueva posición Y
     * @param xSpeed   nueva velocidad X
     * @param ySpeed   nueva velocidad Y
     * @param lifeTime nueva duración de vida en ticks
     * @param damage   nuevo valor de daño
     */
    void resetState(double x, double y,
                    double xSpeed, double ySpeed,
                    int lifeTime, double damage) {
        getTransform().setPosition(new Vector2D(x, y));
        getPhysics().setXspeed(xSpeed);
        getPhysics().setYspeed(ySpeed);
        bulletLife.resetTo(lifeTime);
        this.damage = damage;
        this.destroyEventFired = false;
    }

    /**
     * Modifica el daño del proyectil. Package-private para el pool.
     */
    void resetDamage(double newDamage) {
        this.damage = newDamage;
    }

    // ── Eventos de ciclo de vida ──────────────────────────────────────────

    private void emitExpireAndDestroy() {
        if (GameEventBus.GLOBAL.hasListeners(ProjectileEvents.OnProjectileExpire.class)) {
            GameEventBus.GLOBAL.post(new ProjectileEvents.OnProjectileExpire(this));
        }
        emitDestroy();
    }

    private void emitDestroy() {
        if (!destroyEventFired) {
            destroyEventFired = true;
            if (GameEventBus.GLOBAL.hasListeners(ProjectileEvents.OnProjectileDestroy.class)) {
                GameEventBus.GLOBAL.post(new ProjectileEvents.OnProjectileDestroy(this));
            }
        }
    }
}
