package Game.Items.Types.Bullets.BulletComport;

public class BulletStats {

    private final double speed;
    private final double damage;
    private final int lifeTime;
    private final boolean hasGravity;

    public BulletStats(
            double speed,
            double damage,
            int lifeTime,
            boolean hasGravity
    ){
        this.speed = speed;
        this.damage = damage;
        this.lifeTime = lifeTime;
        this.hasGravity = hasGravity;
    }

    public double getSpeed(){
        return speed;
    }

    public double getDamage(){
        return damage;
    }

    public int getLifeTime(){
        return lifeTime;
    }

    public boolean hasGravity(){
        return hasGravity;
    }
} 