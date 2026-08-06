package Game.Engine.Camera.Target;

import Game.Engine.GameMath.Logic2D.Vector2D;

/**
 * Target de cámara que apunta a una posición fija en el mundo.
 *
 * ── USOS ──────────────────────────────────────────────────────────────────
 *   - Cinemáticas: la cámara mira una posición específica
 *   - Eventos: "zoom a la puerta que se abre"
 *   - Tutoriales: destacar un elemento del mapa
 *   - Combate: fijar la vista en un punto de interés
 *
 * ── DURACIÓN OPCIONAL ─────────────────────────────────────────────────────
 * Si se crea con durationTicks > 0, el target expira automáticamente
 * tras ese número de ticks y la cámara vuelve al target anterior.
 */
public final class StaticLocationTarget implements CameraTarget {

    private final Vector2D position;
    private final int      durationTicks;  // 0 = infinito
    private final int      priority;
    private int            ticksElapsed = 0;

    /**
     * Target estático permanente en la posición dada.
     */
    public StaticLocationTarget(double x, double y) {
        this(x, y, 0, 50);
    }

    /**
     * Target estático permanente con prioridad configurable.
     */
    public StaticLocationTarget(double x, double y, int priority) {
        this(x, y, 0, priority);
    }

    /**
     * Target estático con duración finita.
     *
     * @param x             coordenada X en el mundo
     * @param y             coordenada Y en el mundo
     * @param durationTicks ticks antes de expirar (0 = permanente)
     * @param priority      prioridad respecto a otros targets
     */
    public StaticLocationTarget(double x, double y, int durationTicks, int priority) {
        this.position      = new Vector2D(x, y);
        this.durationTicks = durationTicks;
        this.priority      = priority;
    }

    public static StaticLocationTarget at(double x, double y) {
        return new StaticLocationTarget(x, y);
    }

    public static StaticLocationTarget at(Vector2D pos) {
        return new StaticLocationTarget(pos.getX(), pos.getY());
    }

    public static StaticLocationTarget at(double x, double y, int durationTicks) {
        return new StaticLocationTarget(x, y, durationTicks, 50);
    }

    @Override
    public Vector2D getPosition() {
        return new Vector2D(position.getX(), position.getY());
    }

    @Override
    public void update() {
        if (durationTicks > 0) ticksElapsed++;
    }

    @Override
    public boolean isExpired() {
        return durationTicks > 0 && ticksElapsed >= durationTicks;
    }

    @Override
    public int getPriority() { return priority; }
}
