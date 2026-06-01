package Game.Items.Types.Weapons.Modifiers;

import Game.Enemys.Enemy;
import Game.Items.Types.Bullets.Bullet;
import Game.Items.Types.Bullets.BulletComport.BulletBehavior;
import Game.Player.Player;
import Game.World.WorldObjects.BlockWorld;
import Game.World.WorldObjects.Obstacle;
import Game.World.WorldObjects.Visuals.BackGround;

/**
 * Decorator base para BulletBehavior.
 *
 * Permite apilar comportamientos de bala sin herencia múltiple.
 * Cada wrapper delega al inner behavior y añade su propio efecto.
 *
 * Uso típico:
 *   BulletBehavior base   = new BulletNormal();
 *   BulletBehavior poison = new PoisonBehaviorWrapper(base);
 *   BulletBehavior expl   = new ExplosiveBehaviorWrapper(poison);
 *   // expl aplica normal + poison + explosive en cadena
 *
 * Al crear un Bullet pasas el behavior compuesto igual que antes:
 *   new Bullet(pos, texture, expl, xSpeed, ySpeed, lifetime, damage)
 */
public abstract class BulletBehaviorWrapper extends BulletBehavior {

    protected final BulletBehavior inner;

    protected BulletBehaviorWrapper(BulletBehavior inner) {
        super(
            inner.getBulletBaseDamage(),
            inner.getSpeedFactor(),
            inner.hasGravity(),
            inner.getGravityValue(),
            inner.getLifeTime()
        );
        this.inner = inner;
    }

    // Delegar update al inner y luego aplicar lógica propia
    @Override
    public void update(Bullet bullet) {
        inner.update(bullet);
        onUpdate(bullet);
    }

    @Override
    public void onCollision(Bullet bullet, Enemy enemy) {
        inner.onCollision(bullet, enemy);
        onHitEnemy(bullet, enemy);
    }

    @Override
    public void onCollision(Bullet bullet, Player player) {
        inner.onCollision(bullet, player);
        onHitPlayer(bullet, player);
    }

    @Override
    public void onCollision(Bullet bullet, BlockWorld block) {
        inner.onCollision(bullet, block);
        onHitBlock(bullet, block);
    }

    @Override
    public void onCollision(Bullet bullet, Obstacle obstacle) {
        inner.onCollision(bullet, obstacle);
        onHitObstacle(bullet, obstacle);
    }

    @Override
    public void onCollision(Bullet bullet, BackGround ambiente) {
        inner.onCollision(bullet, ambiente);
    }

    @Override
    public void onCollision(Bullet bullet, Bullet other) {
        inner.onCollision(bullet, other);
    }

    // ── Hooks para subclases ──────────────────────────────────────────────
    // Override solo lo que necesitás

    protected void onUpdate(Bullet bullet) {}
    protected void onHitEnemy(Bullet bullet, Enemy enemy) {}
    protected void onHitPlayer(Bullet bullet, Player player) {}
    protected void onHitBlock(Bullet bullet, BlockWorld block) {}
    protected void onHitObstacle(Bullet bullet, Obstacle obstacle) {}
}
