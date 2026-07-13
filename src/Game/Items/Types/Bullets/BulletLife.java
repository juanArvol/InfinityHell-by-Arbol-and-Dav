package Game.Items.Types.Bullets;

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

    /**
     * Avanza un tick de vida. Debe llamarse exactamente una vez por frame,
     * desde Bullet.update(). No llamar desde isPendingDestruction().
     * @return true si la bala sigue viva tras el tick.
     */
    public boolean tick() {
        if (dead) return false;
        lifeTime--;
        if (lifeTime <= 0) {
            dead = true;
        }
        return !dead;
    }

    /**
     * Consulta sin efecto secundario: ¿sigue viva?
     * Seguro llamar múltiples veces por frame.
     */
    public boolean isAlive() {
        return !dead && lifeTime > 0;
    }

    public void setDead() {
        dead = true;
    }

    public void reset(int count) {
        lifeTime = lifeTime + count;
    }

    public void revive() {
        if (lifeTime > 0) {
            dead = false;
        }
    }

    public boolean isDead()          { return dead; }
    public int     getRemainingLife() { return lifeTime; }
}
