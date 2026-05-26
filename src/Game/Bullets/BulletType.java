package Game.Bullets;

import java.util.function.Supplier;

import Game.Bullets.BulletComport.BulletBehavior;
import Game.Bullets.BulletComport.BulletClass.*;

public enum BulletType {

    NORMAL(BulletNormal::new),
    JUMP(BulletJump::new);
    /*METEOR(MeteorBullet::new),
    THUNDER(ThunderBullet::new); */

    private final Supplier<BulletBehavior> constructor;

    BulletType(Supplier<BulletBehavior> constructor) {
        this.constructor = constructor;
    }

    public BulletBehavior create() {
        return constructor.get();
    }
} 