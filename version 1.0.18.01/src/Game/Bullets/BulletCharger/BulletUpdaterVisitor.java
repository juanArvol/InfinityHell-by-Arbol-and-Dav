package Game.Bullets.BulletCharger;

import Game.Ambiente;
import Game.Bullets.Bullet;
import Game.Colisions.SystemColisions.CollisionVisitor;
import Game.EnimyNormal;
import Game.Player;

public class BulletUpdaterVisitor implements CollisionVisitor {

    private Bullet bullet;

    public BulletUpdaterVisitor(Bullet bullet) {
        this.bullet = bullet;
    }

    @Override
    public void visit(Player player) {
        bullet.getTipo().getBulletClass().onUpdate(bullet, player);
    }

    @Override
    public void visit(EnimyNormal enemy) {
        bullet.getTipo().getBulletClass().onUpdate(bullet, enemy);
    }

    @Override
    public void visit(Ambiente ambiente) {
        bullet.getTipo().getBulletClass().onUpdate(bullet, ambiente);
    }

    @Override
    public void visit(Bullet otherBullet) {
        bullet.getTipo().getBulletClass().onUpdate(bullet, otherBullet);
    }
}
