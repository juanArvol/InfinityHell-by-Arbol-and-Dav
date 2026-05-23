package Game.Player;

public class PlayerStats {

    private int life = 100;
    private int lifeMax = 200;

    private int invulnerabilityFrames = 0;
    private static final int MAX_INV_FRAMES = 20;

    public void update() {
        if (invulnerabilityFrames > 0) {
            invulnerabilityFrames--;
        }
    }

    public int getLife() { return life; }
    public int getLifeMax() { return lifeMax; }

    public void receiveDamage(int value) {

        if (invulnerabilityFrames > 0) return;

        life -= value;

        if (life < 0) life = 0;

        invulnerabilityFrames = MAX_INV_FRAMES;
    }

    public boolean isDead() {
        return life <= 0;
    }
}