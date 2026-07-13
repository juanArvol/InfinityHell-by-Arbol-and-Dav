package Game.Engine.GameMath.SpaceLogic.Logic2D;

/**
 * Vector 2D mutable con API consistente.
 *
 * ── CONTRATO DE MUTABILIDAD ──────────────────────────────────────────────
 *
 * Métodos SIN sufijo  → retornan una NUEVA instancia; this no cambia.
 *   add, subtract, scale, normalize, limit, rotate, applySpread
 *
 * Métodos con sufijo "Local" → mutan THIS y retornan this (para encadenado).
 *   addLocal, scaleLocal, normalizeLocal, limitLocal, rotateLocal
 *
 * ── POR QUÉ ESTE CAMBIO ──────────────────────────────────────────────────
 *
 * PROBLEMA ORIGINAL:
 *   normalize(), limit() y rotate() mutaban this y retornaban this,
 *   igual que addLocal()/scaleLocal(). Pero add()/subtract()/scale()
 *   retornaban nuevas instancias. La misma clase tenía dos contratos
 *   contradictorios sin distinción visible en el nombre del método.
 *
 *   Un llamador que encadenara v.add(a).normalize() esperaba razonablemente
 *   que normalize() no tocara el vector original (por analogía con add()),
 *   pero lo mutaba. Esto causó bugs sutiles en código de steering y disparo.
 *
 * SOLUCIÓN:
 *   Todos los métodos sin sufijo son inmutantes. Los que necesitan mutar
 *   tienen el sufijo "Local", consistente con addLocal()/scaleLocal() ya
 *   existentes. El contrato es visible en el nombre.
 *
 * ── MIGRACIÓN DE LLAMADORES ──────────────────────────────────────────────
 *
 *   Antes: desired.normalize()         → ahora: desired.normalizeLocal()
 *   Antes: velocity.limit(max)         → ahora: velocity.limitLocal(max)
 *   Antes: dir.rotate(angle)           → ahora: dir.rotateLocal(angle)
 *
 *   Si el resultado era asignado: dir = dir.normalize() → sin cambio
 *   (retorna nueva instancia, la asignación es correcta en ambos casos).
 */
public class Vector2D {

    private double x, y;
    private static final double EPS = 1e-9;

    public Vector2D() { this(0, 0); }
    public Vector2D(double x, double y) { this.x = x; this.y = y; }

    // ── Getters / Setters ─────────────────────────────────────────────────

    public double getX() { return x; }
    public double getY() { return y; }
    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }

    // ── Operaciones inmutantes (retornan nueva instancia) ─────────────────

    /** Suma: retorna nuevo vector, this no cambia. */
    public Vector2D add(Vector2D v) { return new Vector2D(x + v.x, y + v.y); }

    /** Resta: retorna nuevo vector, this no cambia. */
    public Vector2D subtract(Vector2D v) { return new Vector2D(x - v.x, y - v.y); }

    /** Escala: retorna nuevo vector, this no cambia. */
    public Vector2D scale(double s) { return new Vector2D(x * s, y * s); }

    /**
     * Normalización: retorna nuevo vector unitario, this no cambia.
     * Retorna (0,0) si la longitud es prácticamente cero.
     */
    public Vector2D normalize() {
        double len = length();
        if (len > EPS) return new Vector2D(x / len, y / len);
        return new Vector2D(0, 0);
    }

    /**
     * Limitar magnitud: retorna nuevo vector con magnitud ≤ max, this no cambia.
     */
    public Vector2D limit(double max) {
        double lsq = lengthSquared();
        if (lsq > max * max) {
            double len = Math.sqrt(lsq);
            return new Vector2D(x / len * max, y / len * max);
        }
        return new Vector2D(x, y);
    }

    /**
     * Rotación: retorna nuevo vector rotado, this no cambia.
     */
    public Vector2D rotate(double radians) {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new Vector2D(x * cos - y * sin, x * sin + y * cos);
    }

    /**
     * Aplica dispersión aleatoria al baseDir en un arco de ±spreadDegrees/2.
     * Retorna siempre una nueva instancia. this no se usa ni muta.
     *
     * Nota: this se ignora; el método es una factory estática expresada como
     * método de instancia por conveniencia de encadenado. Si se prefiere
     * semántica clara: Vector2D.withSpread(baseDir, spread).
     */
    public Vector2D applySpread(Vector2D baseDir, double spreadDegrees) {
        double randomAngle = (Math.random() - 0.5) * spreadDegrees;
        double radians     = Math.toRadians(randomAngle);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new Vector2D(
            baseDir.x * cos - baseDir.y * sin,
            baseDir.x * sin + baseDir.y * cos
        );
    }

    // ── Operaciones mutantes (sufijo "Local", mutan this, retornan this) ───

    /** Suma in-place. Muta this y retorna this para encadenado. */
    public Vector2D addLocal(Vector2D v) { x += v.x; y += v.y; return this; }

    /** Escala in-place. Muta this y retorna this para encadenado. */
    public Vector2D scaleLocal(double s) { x *= s; y *= s; return this; }

    /**
     * Normalización in-place. Muta this y retorna this.
     * Usar cuando se quiere modificar el vector existente sin crear uno nuevo.
     * Ejemplo: desired.normalizeLocal().scaleLocal(maxSpeed)
     */
    public Vector2D normalizeLocal() {
        double len = length();
        if (len > EPS) { x /= len; y /= len; }
        else            { x = 0;   y = 0;    }
        return this;
    }

    /**
     * Limitar magnitud in-place. Muta this y retorna this.
     * Ejemplo: velocity.limitLocal(maxSpeed)
     */
    public Vector2D limitLocal(double max) {
        double lsq = lengthSquared();
        if (lsq > max * max) {
            double len = Math.sqrt(lsq);
            x = x / len * max;
            y = y / len * max;
        }
        return this;
    }

    /**
     * Rotación in-place. Muta this y retorna this.
     * Ejemplo: direction.rotateLocal(Math.toRadians(45))
     */
    public Vector2D rotateLocal(double radians) {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        double nx  = x * cos - y * sin;
        double ny  = x * sin + y * cos;
        x = nx;
        y = ny;
        return this;
    }

    // ── Magnitud y distancia ──────────────────────────────────────────────

    public double length()        { return Math.hypot(x, y); }
    public double lengthSquared() { return x * x + y * y; }
    public double distance(Vector2D v) { return Math.hypot(x - v.x, y - v.y); }

    // ── Producto escalar ──────────────────────────────────────────────────

    public double puntoInter(Vector2D v) { return x * v.x + y * v.y; }

    // ── toString ──────────────────────────────────────────────────────────

    @Override
    public String toString() { return "Vector2D(" + x + ", " + y + ")"; }
}
