package Game.Items.Types.Bullets;

import Game.Engine.Colisions.Filter.CollisionProfile;
import Game.Engine.Entity.Components.Collisions.ColliderComponent;
import Game.Engine.Entity.Components.Physics2DComponent;
import Game.Engine.Entity.Components.Visuals.HitBoxComponent;
import Game.Engine.Entity.Components.Visuals.SpriteRendererComponent;
import Game.Engine.GameMath.KineticPhysics.PhysicsStepper;
import Game.Engine.GameMath.SpaceLogic.Logic2D.Vector2D;
import Game.Engine.GameObjects;
import Game.Items.Types.Bullets.BulletComport.BulletBehavior;
import Game.World.WorldObjects.WorldObjectsContainer;
import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * Proyectil del juego.
 *
 * ── HRFC-014 — GAP-2 / GAP-3: Colisiones generalizadas ──────────────────
 *
 * ANTES:
 *   Bullet.onCollisionWith() tenía lógica específica:
 *     if (other instanceof Enemy e) { e.damage((int)damage); setDead(); }
 *
 *   Esto significaba:
 *   (a) Bullet conocía Enemy directamente (dependencia cruzada).
 *   (b) El daño se aplicaba saltando el pipeline de HealthComponent
 *       (ignorando resistencias, escudos, barreras, invulnerabilidad).
 *   (c) Player y otras futuras entidades nunca podían recibir daño de balas
 *       propias sin añadir más ramas instanceof.
 *
 * SOLUCIÓN:
 *   Bullet delega toda la lógica de colisión a su BulletBehavior:
 *
 *     behavior.onCollision(this, other)
 *
 *   BulletBehavior.onCollision(Bullet, GameObjects) es el único punto de
 *   decisión. Cada behavior concreto hace instanceof donde necesita distinguir
 *   tipos y aplica el daño a través del pipeline correcto (AbstractEntity.damage()
 *   → HealthComponent → HealthStats, pasando por escudo, barrera, etc.).
 *
 *   Bullet no importa Enemy ni Player. La lógica de qué hace la bala al impactar
 *   pertenece al behavior, no al proyectil.
 */
public class Bullet extends GameObjects implements WorldObjectsContainer.Destroyable {

    private final BulletBehavior    behavior;
    private final double            damage;
    private final BulletLife        bulletLife;
    private final Physics2DComponent physicsComponent;

    public Bullet(
            Vector2D       position,
            BufferedImage  texture,
            BulletBehavior behavior,
            double         xSpeed,
            double         ySpeed,
            int            lifeTime,
            double         damage
    ) {
        getTransform().setPosition(position);

        this.behavior    = behavior;
        this.damage      = damage;
        this.bulletLife  = new BulletLife(lifeTime);

        // ── Render ────────────────────────────────────────────────────────
        if (texture != null) {
            addComponent(new SpriteRendererComponent(texture));
        }

        // ── Collider (TRIGGER) ────────────────────────────────────────────
        ColliderComponent collider = new ColliderComponent(8, 8, CollisionProfile.BULLET);
        collider.setType(ColliderComponent.Type.TRIGGER);
        addComponent(collider);

        addComponent(new HitBoxComponent(Color.YELLOW));

        // ── Physics ───────────────────────────────────────────────────────
        BulletPhysics physics = new BulletPhysics(
                xSpeed, ySpeed,
                behavior.hasGravity(), behavior.getGravityValue()
        );
        physicsComponent = new Physics2DComponent(physics);
        addComponent(physicsComponent);
    }

    // ── Update ────────────────────────────────────────────────────────────

    @Override
    public void update() {
        if (!bulletLife.tick()) return;

        if (behavior.hasGravity()) {
            getPhysics().applyGravity(false);
        }

        behavior.update(this);
        moveByPhysics();
        super.update();
    }

    // ── Colisión — delega completamente al behavior ───────────────────────

    /**
     * Delega la lógica de colisión al BulletBehavior.
     *
     * El behavior es el único responsable de decidir:
     *   - A quién daña y cuánto.
     *   - Si el proyectil muere al impactar.
     *   - Si aplica efectos secundarios (fuego, hielo, veneno…).
     *
     * Bullet no tiene conocimiento de Player, Enemy ni ningún tipo concreto.
     * La aplicación de daño ocurre dentro del behavior a través del pipeline
     * correcto: AbstractEntity.damage() → HealthComponent → HealthStats.
     */
    @Override
    public void onCollisionWith(GameObjects other) {
        behavior.onCollision(this, other);
    }

    // ── API pública ───────────────────────────────────────────────────────

    public BulletLife    getBulletLife() { return bulletLife; }
    public double        getDamage()     { return damage; }

    public BulletPhysics getPhysics() {
        return (BulletPhysics) physicsComponent.getPhysics();
    }

    public void moveByPhysics() {
        var vel = getPhysics().getVelocity();
        PhysicsStepper.moveWith(this, vel.getX(), vel.getY());
    }

    /** Destroyable — WorldObjectsContainer elimina la bala cuando muere. */
    @Override
    public boolean isPendingDestruction() {
        return !bulletLife.isAlive();
    }
}
