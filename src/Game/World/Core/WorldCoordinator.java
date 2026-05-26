package Game.World.Core;

import java.util.Objects;

public class WorldCoordinator {

    private final int x;
    private final int y;

    public WorldCoordinator(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int x() { return x; }
    public int y() { return y; }

    public WorldCoordinator right() {
        return new WorldCoordinator(x + 1, y);
    }

    public WorldCoordinator left() {
        return new WorldCoordinator(x - 1, y);
    }

    public WorldCoordinator up() {
        return new WorldCoordinator(x, y - 1);
    }

    public WorldCoordinator down() {
        return new WorldCoordinator(x, y + 1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WorldCoordinator that)) return false;
        return x == that.x && y == that.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
}