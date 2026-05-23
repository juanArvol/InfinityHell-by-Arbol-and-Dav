package Game.Bullets;

public class BulletLife {

    private int lifeTime;

    public BulletLife(int lifeTime) {
        this.lifeTime = lifeTime;
    }

    /** Reduce 1 unidad de vida y devuelve true si ya expiró */
    public boolean tick() {
        if (lifeTime <= 0) return true;
        lifeTime--;
        return lifeTime <= 0;
    }

    /** Marca la bala como muerta inmediatamente */
    public void setDead() {
        lifeTime = 0;
    }

    /** Revisa si la bala sigue viva */
    public boolean isAlive() {
        return lifeTime > 0;
    }

    /** Obtiene la vida restante */
    public int getLifeTime() {
        return lifeTime;
    }

    /** Opcional: reinicia la vida */
    public void reset(int newLifeTime) {
        this.lifeTime = lifeTime + newLifeTime;
    }
} 