package Game.Bullets;

/**
 * Ciclo de vida de una bala.
 *
 * EXTENSIÓN vs. original: añade revive() para que PiercingModifier
 * pueda cancelar una muerte prematura del inner behavior.
 *
 * RETRO-COMPATIBLE: setDead() e isAlive() son idénticos al original.
 */
public class BulletLife {

    private int lifeTime;
    private boolean dead;

    public BulletLife(int lifeTime) {
        this.lifeTime = lifeTime;
        this.dead = false;
    }

    public boolean isAlive() {
        if (dead) return false;
        lifeTime--;
        return lifeTime > 0;
    }

    public void setDead() {
        dead = true;
    }

    public void reset(int count){
        lifeTime = lifeTime + count;
    }
    
    public void revive() {
        if (lifeTime > 0) {
            dead = false;
        }
    }

    public boolean isDead() { return dead; }
    public int getRemainingLife() { return lifeTime; }
}
