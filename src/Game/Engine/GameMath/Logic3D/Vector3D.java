package Game.Engine.GameMath.Logic3D;

import Game.Engine.GameMath.Logic2D.Vector2D;

/**
 * Vector tridimensional para soporte de sistema 2.5D.
 *
 * NUEVO SISTEMA: base arquitectónica para la migración a 2.5D.
 * Retro-compatible: Z=0 por defecto mantiene comportamiento 2D idéntico.
 *
 * Uso típico:
 * - X, Y: posición en el plano del mundo (igual que antes)
 * - Z: altura sobre el suelo (0 = en el suelo, >0 = elevado)
 *
 * Para render 2.5D estilo Project Zomboid:
 *   screenY = worldY - (worldZ * PERSPECTIVE_FACTOR)
 * donde PERSPECTIVE_FACTOR ≈ 0.5 a 1.0 según el ángulo de cámara.
 */
public class Vector3D {

    private double x;
    private double y;
    private double z;

    private static final double EPS = 1e-9;

    public Vector3D() { this(0, 0, 0); }

    public Vector3D(double x, double y) { this(x, y, 0); }

    public Vector3D(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /** Crea desde un Vector2D existente (z=0). Migración sin romper nada. */
    public static Vector3D from(Vector2D v2) {
        return new Vector3D(v2.getX(), v2.getY(), 0);
    }

    // ==================== GETTERS / SETTERS ====================

    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }

    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }
    public void setZ(double z) { this.z = z; }

    // ==================== OPERACIONES ====================

    public Vector3D add(Vector3D v) {
        return new Vector3D(x + v.x, y + v.y, z + v.z);
    }

    public Vector3D subtract(Vector3D v) {
        return new Vector3D(x - v.x, y - v.y, z - v.z);
    }

    public Vector3D scale(double s) {
        return new Vector3D(x * s, y * s, z * s);
    }

    public Vector3D addLocal(Vector3D v) {
        x += v.x; y += v.y; z += v.z;
        return this;
    }

    public Vector3D scaleLocal(double s) {
        x *= s; y *= s; z *= s;
        return this;
    }

    // ==================== MAGNITUD ====================

    public double length() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public double lengthSquared() {
        return x * x + y * y + z * z;
    }

    /** Longitud en el plano XY (ignorando Z). Útil para pathfinding 2D. */
    public double lengthXY() {
        return Math.hypot(x, y);
    }

    // ==================== NORMALIZACIÓN ====================

    public Vector3D normalize() {
        double len = length();
        if (len > EPS) { x /= len; y /= len; z /= len; }
        else { x = 0; y = 0; z = 0; }
        return this;
    }

    public Vector3D limit(double max) {
        double lsq = lengthSquared();
        if (lsq > max * max) {
            double len = Math.sqrt(lsq);
            x = x / len * max;
            y = y / len * max;
            z = z / len * max;
        }
        return this;
    }

    // ==================== DISTANCIAS ====================

    /** Distancia 3D completa. */
    public double distance(Vector3D v) {
        double dx = x - v.x, dy = y - v.y, dz = z - v.z;
        return Math.sqrt(dx*dx + dy*dy + dz*dz);
    }

    /** Distancia solo en XY (ignorando altura). Para colisiones en plano. */
    public double distanceXY(Vector3D v) {
        return Math.hypot(x - v.x, y - v.y);
    }

    // ==================== RENDER 2.5D ====================

    /**
     * Calcula la Y de pantalla aplicando perspectiva de la altura Z.
     * En juegos top-down 2.5D, los objetos más "altos" aparecen
     * visualmente más arriba en pantalla.
     *
     * @param perspectiveFactor factor de perspectiva (0.5 = suave, 1.0 = fuerte)
     * @return Y ajustado para render
     */
    public double getScreenY(double perspectiveFactor) {
        return y - z * perspectiveFactor;
    }

    /**
     * Valor de profundidad para depth sorting (painter's algorithm).
     * Objetos con mayor valor se dibujan DESPUÉS (encima).
     * Combina Y y Z para orden correcto en 2.5D top-down.
     */
    public double getDepthSortValue() {
        return y + z * 0.5;
    }

    // ==================== CONVERSIÓN ====================

    /** Retorna Vector2D con solo X e Y (proyección al plano). */
    public Vector2D toVector2D() {
        return new Vector2D(x, y);
    }

    @Override
    public String toString() {
        return String.format("Vector3D(%.2f, %.2f, %.2f)", x, y, z);
    }
}
