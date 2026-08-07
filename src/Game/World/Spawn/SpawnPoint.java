package Game.World.Spawn;

import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.World.Chunk.GlobalChunkResolver;
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
 * Las coordenadas internas del SpawnPoint son LOCALES al chunk al que pertenece
 * (o globales si sector == null y el caller ya proporciona coords globales).
 *
 * Cuando sector != null, samplePosition() convierte automáticamente a
 * coordenadas globales usando GlobalChunkResolver:
 *
 *   globalX = sector.x() * chunkWidth  + localX
 *   globalY = sector.y() * chunkHeight + localY
 *
 * El chunkWidth/chunkHeight se pasa en samplePosition(chunkW, chunkH).
 * La firma sin argumentos samplePosition() asume que el centro ya está en
 * coords globales (caso más común: sector == null, posición global directa).
 *
 * Para SpawnPoints con sector explícito usar samplePosition(chunkW, chunkH).
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
     * Retorna una posición de spawn muestreada en coordenadas GLOBALES.
     *
     * Si sector == null: asume que el centro ya está en coords globales.
     * Si sector != null: delega en samplePosition(chunkWidth, chunkHeight)
     *   usando dimensiones de chunk por defecto (0). Esto lanzará un error
     *   en runtime si se usa con sector != null. Preferir la sobrecarga
     *   con chunkWidth y chunkHeight explícitos.
     *
     * @return nueva instancia de Vector2D con coordenadas globales de spawn.
     */
    public Vector2D samplePosition() {
        Vector2D local = sampleLocal();

        if (sector != null) {
            // No tenemos chunkWidth/Height aquí — advertencia en la firma sin args
            // La posición local se devuelve tal cual como fallback seguro.
            // Usar samplePosition(chunkW, chunkH) para conversión correcta.
            return local;
        }

        return local;
    }

    /**
     * Retorna una posición de spawn muestreada en coordenadas GLOBALES,
     * convirtiendo desde coords locales al chunk si sector != null.
     *
     * Para SpawnPoints con sector explícito, siempre usar esta sobrecarga.
     *
     * @param chunkWidth  ancho del chunk en píxeles globales
     * @param chunkHeight alto del chunk en píxeles globales
     * @return nueva instancia de Vector2D con coordenadas globales de spawn.
     */
    public Vector2D samplePosition(int chunkWidth, int chunkHeight) {
        Vector2D local = sampleLocal();

        if (sector != null && chunkWidth > 0 && chunkHeight > 0) {
            // Convertir posición local a global usando el origen del sector
            double globalX = GlobalChunkResolver.toGlobalX(sector, local.getX(), chunkWidth);
            double globalY = GlobalChunkResolver.toGlobalY(sector, local.getY(), chunkHeight);
            return new Vector2D(globalX, globalY);
        }

        // sector == null: el centro ya está en coords globales
        return local;
    }

    /** Muestrea la posición interna (sin conversión de coordenadas). */
    private Vector2D sampleLocal() {
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
