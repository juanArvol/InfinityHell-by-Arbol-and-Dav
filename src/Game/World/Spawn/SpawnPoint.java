package Game.World.Spawn;

import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.World.Core.WorldCoordinator;

/**
 * Punto o zona de spawn en el mundo.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * Un SpawnPoint define DÓNDE puede spawnear algo. Puede ser:
 *   - Un punto exacto (radio = 0).
 *   - Un área circular (radio > 0): la posición final se muestrea
 *     aleatoriamente dentro del círculo.
 *   - Un rectángulo (ancho/alto > 0): posición aleatoria dentro del rect.
 *
 * Un SpawnPoint NO decide QUÉ spawna ni CUÁNDO. Solo proporciona posición.
 *
 * ── EXTENSIBILIDAD ────────────────────────────────────────────────────────
 * Para spawn en path, polígono arbitrario o waypoints, implementar
 * SpawnPoint.samplePosition() en una subclase o añadir nuevas factories.
 *
 * ── COORDENADAS ───────────────────────────────────────────────────────────
 * Siempre en coordenadas de MUNDO. El SpawnPoint no conoce sectores ni
 * pantallas. El WorldCoordinator identifica en qué sector vive este punto.
 * Si es null, se usa el sector activo en el momento del spawn.
 */
public final class SpawnPoint {

    // ── Tipos de forma ────────────────────────────────────────────────────

    public enum Shape { POINT, CIRCLE, RECT }

    // ── Estado inmutable ──────────────────────────────────────────────────

    private final Vector2D         center;
    private final Shape            shape;
    private final double           radius;    // para CIRCLE
    private final double           halfW;     // para RECT
    private final double           halfH;     // para RECT
    private final WorldCoordinator sector;    // null = sector activo

    // ── Factories ─────────────────────────────────────────────────────────

    /**
     * Punto exacto en las coordenadas dadas.
     * El sector puede ser null (usa el activo en el momento del spawn).
     */
    public static SpawnPoint at(double x, double y) {
        return at(x, y, null);
    }

    public static SpawnPoint at(double x, double y, WorldCoordinator sector) {
        return new SpawnPoint(new Vector2D(x, y), Shape.POINT, 0, 0, 0, sector);
    }

    public static SpawnPoint at(Vector2D pos) {
        return at(pos.getX(), pos.getY(), null);
    }

    public static SpawnPoint at(Vector2D pos, WorldCoordinator sector) {
        return at(pos.getX(), pos.getY(), sector);
    }

    /**
     * Área circular: posición aleatoria dentro del radio.
     */
    public static SpawnPoint circle(double cx, double cy, double radius) {
        return circle(cx, cy, radius, null);
    }

    public static SpawnPoint circle(double cx, double cy, double radius,
                                    WorldCoordinator sector) {
        return new SpawnPoint(new Vector2D(cx, cy), Shape.CIRCLE, radius, 0, 0, sector);
    }

    /**
     * Área rectangular centrada en (cx, cy) con las dimensiones dadas.
     */
    public static SpawnPoint rect(double cx, double cy, double width, double height) {
        return rect(cx, cy, width, height, null);
    }

    public static SpawnPoint rect(double cx, double cy, double width, double height,
                                  WorldCoordinator sector) {
        return new SpawnPoint(new Vector2D(cx, cy), Shape.RECT, 0,
                              width / 2.0, height / 2.0, sector);
    }

    /**
     * Spawn en un área aleatoria del mundo dado.
     * El margen evita spawnear pegado a los bordes.
     */
    public static SpawnPoint worldBounds(int worldWidth, int worldHeight, int margin) {
        double cx = worldWidth  / 2.0;
        double cy = worldHeight / 2.0;
        double hw = worldWidth  / 2.0 - margin;
        double hh = worldHeight / 2.0 - margin;
        return new SpawnPoint(new Vector2D(cx, cy), Shape.RECT, 0,
                              Math.max(0, hw), Math.max(0, hh), null);
    }

    // ── Constructor privado ───────────────────────────────────────────────

    private SpawnPoint(Vector2D center, Shape shape, double radius,
                       double halfW, double halfH, WorldCoordinator sector) {
        this.center = center;
        this.shape  = shape;
        this.radius = radius;
        this.halfW  = halfW;
        this.halfH  = halfH;
        this.sector = sector;
    }

    // ── Muestreo de posición ──────────────────────────────────────────────

    /**
     * Retorna una posición de spawn muestreada dentro de esta zona.
     *
     * Para POINT: retorna siempre el centro.
     * Para CIRCLE: posición aleatoria uniforme dentro del disco.
     * Para RECT: posición aleatoria uniforme dentro del rectángulo.
     *
     * @return nueva instancia de Vector2D con las coordenadas de spawn.
     */
    public Vector2D samplePosition() {
        return switch (shape) {
            case POINT  -> new Vector2D(center.getX(), center.getY());
            case CIRCLE -> sampleCircle();
            case RECT   -> sampleRect();
        };
    }

    private Vector2D sampleCircle() {
        // Muestreo uniforme en disco: raíz cuadrada para distribución uniforme.
        double r     = radius * Math.sqrt(Math.random());
        double angle = Math.random() * Math.PI * 2;
        return new Vector2D(
            center.getX() + r * Math.cos(angle),
            center.getY() + r * Math.sin(angle)
        );
    }

    private Vector2D sampleRect() {
        double x = center.getX() + (Math.random() * 2 - 1) * halfW;
        double y = center.getY() + (Math.random() * 2 - 1) * halfH;
        return new Vector2D(x, y);
    }

    // ── Acceso ────────────────────────────────────────────────────────────

    public Vector2D         getCenter() { return new Vector2D(center.getX(), center.getY()); }
    public Shape            getShape()  { return shape;  }
    public double           getRadius() { return radius; }
    public WorldCoordinator getSector() { return sector; }

    @Override
    public String toString() {
        return "SpawnPoint[" + shape + " center=" + center + "]";
    }
}
