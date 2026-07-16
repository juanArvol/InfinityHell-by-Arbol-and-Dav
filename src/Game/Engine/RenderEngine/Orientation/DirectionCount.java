package Game.Engine.RenderEngine.Orientation;

/**
 * DirectionCount — cuántas direcciones discretas soporta una entidad.
 *
 * ── PROPÓSITO ─────────────────────────────────────────────────────────────
 * Configura la granularidad del sistema de orientación para cada entidad.
 * El Gameplay configura DirectionCount en el SpriteComponent; el
 * OrientationResolver lo usa para cuantizar el ángulo continuo.
 *
 *   FOUR   → S, N, E, W         (juego simple, sprites mínimos)
 *   EIGHT  → + SE, SW, NE, NW   (calidad media, juegos de rol top-down)
 *   SIXTEEN → no implementado todavía; preparado por arquitectura
 *
 * ── FUTURA EXTENSIÓN ──────────────────────────────────────────────────────
 * CONTINUOUS → orientación continua (rotación real del sprite, no discreta).
 * Se añadirá cuando el sistema de rotación de sprites esté completo.
 */
public enum DirectionCount {

    /**
     * 4 direcciones: NORTH, SOUTH, EAST, WEST.
     * Cada sector cubre 90°.
     */
    FOUR(4),

    /**
     * 8 direcciones: las 4 cardinales + 4 diagonales.
     * Cada sector cubre 45°.
     */
    EIGHT(8),

    /**
     * 16 direcciones.
     * Cada sector cubre 22.5°.
     * Preparado; OrientationResolver aproxima a 8 hasta que los assets estén.
     */
    SIXTEEN(16);

    private final int count;

    DirectionCount(int count) { this.count = count; }

    /** Número de direcciones. */
    public int getCount() { return count; }
}
