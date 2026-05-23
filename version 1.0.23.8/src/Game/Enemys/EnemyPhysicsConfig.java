package Game.Enemys;

public class EnemyPhysicsConfig {

    public double gravity;

    public double mass;

    public double aAir;
    public double aGround;

    public double speedMaxAir;
    public double speedMaxGround;

    public double slide;


    public EnemyPhysicsConfig(
            double gravity,
            double mass,
            double aAir,
            double aGround,
            double speedMaxAir,
            double speedMaxGround,
            double slide
    ) {
        this.gravity = gravity;
        this.mass = mass;
        this.aAir = aAir;
        this.aGround = aGround;
        this.speedMaxAir = speedMaxAir;
        this.speedMaxGround = speedMaxGround;
        this.slide = slide;
    }
}