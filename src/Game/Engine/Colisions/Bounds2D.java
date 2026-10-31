package Game.Engine.Colisions;

import java.awt.Rectangle;

/**
 * Bounding box de colisión con precisión double.
 *
 * ── Motivación ─────────────────────────────────────────────────────────────
 * El sistema de física del engine opera en doubles (velocidad, posición,
 * desplazamiento), pero ColliderComponent.getBounds() truncaba todo a
 * java.awt.Rectangle (ints), causando:
 *   - Microgaps: posición 100.9 → bounds x=100 (parece 0.9px más a la izquierda)
 *   - Penetraciones fantasma: posición exacta en borde → truncado reporta overlap
 *   - Oscilación onGround: frame con 100.9 (contacto) vs 101.1 (aire) por truncado
 *   - Separación artificial: epsilons de 0.5px para compensar pérdida de precisión
 *
 * Bounds2D mantiene la precisión double desde Transform hasta la detección de
 * colisiones, eliminando todos esos problemas.
 *
 * ── Cuándo usar Bounds2D vs Rectangle ──────────────────────────────────────
 * Bounds2D: física y colisiones (SweptAABB, CollisionDetector, ContactState)
 * Rectangle: rendering y queries externas (convertir con toRectangle())
 *
 * ── Inmutabilidad ──────────────────────────────────────────────────────────
 * Los campos son final — cada llamada a getBounds2D() crea una instancia nueva.
 * Esto garantiza que modificaciones al Transform no afecten bounds cacheados.
 */
public final class Bounds2D {
    public final double x, y, width, height;

    public Bounds2D(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    // ── Conversión a Rectangle (rendering) ─────────────────────────────────

    /**
     * Convierte a java.awt.Rectangle para rendering o APIs legacy.
     * Redondea al int más cercano en lugar de truncar.
     */
    public Rectangle toRectangle() {
        return new Rectangle(
                (int) Math.round(x),
                (int) Math.round(y),
                (int) Math.round(width),
                (int) Math.round(height)
        );
    }

    // ── Tests de intersección (precisión double) ───────────────────────────

    /**
     * Test AABB: true si este bounds intersecta con otro.
     * Usa comparaciones double directas sin pérdida de precisión.
     */
    public boolean intersects(Bounds2D other) {
        return x < other.x + other.width
                && x + width > other.x
                && y < other.y + other.height
                && y + height > other.y;
    }

    /**
     * Test punto-en-bounds.
     */
    public boolean contains(double px, double py) {
        return px >= x && px < x + width
                && py >= y && py < y + height;
    }

    // ── Queries geométricas ────────────────────────────────────────────────

    public double centerX() { return x + width * 0.5; }
    public double centerY() { return y + height * 0.5; }

    public double left()   { return x; }
    public double right()  { return x + width; }
    public double top()    { return y; }
    public double bottom() { return y + height; }

    // ── Utilidades ─────────────────────────────────────────────────────────

    /**
     * Calcula el área del overlap con otro bounds.
     * Retorna 0.0 si no hay intersección.
     */
    public double overlapArea(Bounds2D other) {
        if (!intersects(other)) return 0.0;

        double overlapWidth  = Math.min(right(),  other.right())
                - Math.max(left(),   other.left());
        double overlapHeight = Math.min(bottom(), other.bottom())
                - Math.max(top(),    other.top());

        return overlapWidth * overlapHeight;
    }

    /**
     * Calcula la distancia euclidiana entre los centros.
     */
    public double distanceToCenter(Bounds2D other) {
        double dx = centerX() - other.centerX();
        double dy = centerY() - other.centerY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    @Override
    public String toString() {
        return String.format("Bounds2D[x=%.2f, y=%.2f, w=%.2f, h=%.2f]",
                x, y, width, height);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Bounds2D)) return false;
        Bounds2D other = (Bounds2D) obj;
        return Double.compare(x, other.x) == 0
                && Double.compare(y, other.y) == 0
                && Double.compare(width, other.width) == 0
                && Double.compare(height, other.height) == 0;
    }

    @Override
    public int hashCode() {
        long bits = Double.doubleToLongBits(x);
        bits ^= Double.doubleToLongBits(y) * 31;
        bits ^= Double.doubleToLongBits(width) * 37;
        bits ^= Double.doubleToLongBits(height) * 41;
        return (int) (bits ^ (bits >>> 32));
    }
}
