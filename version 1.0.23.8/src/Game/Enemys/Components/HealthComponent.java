package Game.Enemys.Components;

public class HealthComponent {

    private int current;
    private int max;

    public HealthComponent(int max) {
        this.max = max;
        this.current = max;
    }

    public void damage(int amount) {
        current -= amount;
        if (current < 0) current = 0;
    }

    public boolean isDead() {
        return current <= 0;
    }

    public int getCurrent() { return current; }
    public int getMax() { return max; }
}