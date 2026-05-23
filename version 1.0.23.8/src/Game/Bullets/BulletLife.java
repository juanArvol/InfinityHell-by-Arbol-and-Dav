package Game.Bullets;

/**
 * Gestiona la vida útil de una bala.
 * FIX BUG-006: reset() ahora reemplaza el tiempo de vida en lugar de sumarlo.
 */
public class BulletLife {

    private int lifeTime;

    public BulletLife(int lifeTime) {
        this.lifeTime = lifeTime;
    }

    /** Reduce 1 unidad de vida. Retorna true si ya expiró. */
    public boolean tick() {
        if (lifeTime <= 0) return true;
        lifeTime--;
        return lifeTime <= 0;
    }

    /** Marca la bala como muerta inmediatamente. */
    public void setDead() {
        lifeTime = 0;
    }

    /** Revisa si la bala sigue viva. */
    public boolean isAlive() {
        return lifeTime > 0;
    }

    /** Tiempo de vida restante. */
    public int getLifeTime() {
        return lifeTime;
    }

    /**
     * Reinicia la vida de la bala con un nuevo valor.
     * FIX BUG-006: era "lifeTime + newLifeTime" (sumaba), ahora reemplaza.
     */
    public void reset(int newLifeTime) {
        this.lifeTime = newLifeTime;
    }
}
