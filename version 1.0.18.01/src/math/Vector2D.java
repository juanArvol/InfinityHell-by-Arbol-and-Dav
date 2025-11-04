package math;

public class Vector2D {
    private double x, y;
    private static final double EPS = 1e-9;

    public Vector2D() { this(0, 0); }
    public Vector2D(double x, double y) { this.x = x; this.y = y; }

    // Getters y Setters
    public double getX() { return x; }
    public double getY() { return y; }
    public void setX(double x) { this.x= x; }
    public void setY(double y) { this.y= y; }

    // Operaciones básicas
    public Vector2D add(Vector2D v) { return new Vector2D(x + v.x, y + v.y); }
    public Vector2D subtract(Vector2D v) { return new Vector2D(x - v.x, y - v.y); }
    public Vector2D scale(double s) { return new Vector2D(x * s, y * s); }

    // Métodos locales (mutables, si los necesitas)
    public Vector2D addLocal(Vector2D v) { x += v.x; y += v.y; return this; }
    public Vector2D scaleLocal(double s) { x *= s; y *= s; return this; }

    // Magnitud
    public double length() { return Math.hypot(x, y); }
    public double lengthSquared() { return x * x + y * y; }

    // Normalización
    public Vector2D normalize() {
        double len = length();
        if (len > EPS) { x /= len; y /= len; }
        else { x = 0; y = 0; }
        return this;
    }

    // Limitar magnitud
    public Vector2D limit(double max) {
        double lsq = lengthSquared();
        if (lsq > max * max) {
            double len = Math.sqrt(lsq);
            x = x / len * max;
            y = y / len * max;
        }
        return this;
    }

    // Operaciones geométricas
    public double puntoInter(Vector2D v) { return (x * v.x) + (y * v.y); }
    public double distance(Vector2D v) { return Math.hypot(x - v.x, y - v.y); }
    public Vector2D rotate(double radians) {
        double cos = Math.cos(radians), sin = Math.sin(radians);
        double nx = x * cos - y * sin;
        double ny = x * sin + y * cos;
        x = nx; y = ny;
        return this;
    }

    @Override
    public String toString() { return "Vector2D(" + x + ", " + y + ")"; }
}
