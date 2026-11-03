package Game.Engine.GameMath.Logic2D;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;

/**
 * Bounding box de colisión con precisión double.
 * Reemplazo completo de java.awt.Rectangle con soporte para doubles.
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
 * RectangleH mantiene la precisión double desde Transform hasta la detección de
 * colisiones, eliminando todos esos problemas.
 *
 * ── API Compatible con Rectangle ───────────────────────────────────────────
 * Esta clase implementa toda la funcionalidad esencial de java.awt.Rectangle:
 *   - Constructores: vacío, desde valores, desde otros bounds, desde Rectangle
 *   - Getters/Setters: get/set X/Y/Width/Height, setRect, setLocation, setSize
 *   - Métodos legacy: setBounds, reshape, move, resize, inside (deprecated)
 *   - Tests: intersects, contains, isEmpty, intersectsLine
 *   - Operaciones: union, intersection, add (punto/bounds/rect)
 *   - Transformaciones: translate, grow
 *   - Compatibilidad Rectangle2D: createIntersection, createUnion, outcode
 *
 * ── Utilidades Adicionales ─────────────────────────────────────────────────
 * Además de Rectangle, incluye +70 métodos útiles para game dev:
 *   - Física: penetrationX/Y, minimumTranslationVector, overlapArea
 *   - Distancia: distanceToPoint, distanceTo, closestPoint
 *   - Transformación: scale, expand/shrink, centerOn, clampInside
 *   - Queries: corners, edges, area, perimeter, aspectRatio
 *   - Construcción: fromCenter, fromPoints, fromCircle
 *   - Comparación: isLargerThan, isSquare, isLandscape
 *   - Animación: lerp, diagonal, inradius, circumradius
 *
 * ── Cuándo usar RectangleH vs Rectangle ───────────────────────────────────
 * RectangleH: física y colisiones (SweptAABB, CollisionDetector, ContactState)
 * Rectangle: rendering y queries externas (convertir con toRectangle())
 *
 * ── Mutabilidad ────────────────────────────────────────────────────────────
 * Los campos x, y, width, height son públicos y mutables (como Rectangle).
 * Para operaciones inmutables, usar métodos que retornan nuevas instancias.
 */
public class RectangleH implements Serializable, Cloneable {
    private static final long serialVersionUID = 1L;

    /** Coordenada X de la esquina superior izquierda */
    public double x;
    
    /** Coordenada Y de la esquina superior izquierda */
    public double y;
    
    /** Ancho del bounds */
    public double width;
    
    /** Alto del bounds */
    public double height;

    // ═══════════════════════════════════════════════════════════════════════
    // ══ CONSTRUCTORES
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Constructor vacío: crea un bounds en (0, 0) con tamaño (0, 0).
     */
    public RectangleH() {
        this(0.0, 0.0, 0.0, 0.0);
    }

    /**
     * Constructor principal: especifica posición y tamaño.
     */
    public RectangleH(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /**
     * Constructor desde otro RectangleH (copia).
     */
    public RectangleH(RectangleH bounds) {
        this(bounds.x, bounds.y, bounds.width, bounds.height);
    }

    /**
     * Constructor desde Rectangle (conversión int → double).
     */
    public RectangleH(Rectangle rect) {
        this(rect.x, rect.y, rect.width, rect.height);
    }

    /**
     * Constructor desde Rectangle2D.
     */
    public RectangleH(Rectangle2D rect) {
        this(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ══ GETTERS / SETTERS (Compatible con Rectangle2D)
    // ═══════════════════════════════════════════════════════════════════════

    public double getX() { return x; }
    public double getY() { return y; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }

    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }
    public void setWidth(double width) { this.width = width; }
    public void setHeight(double height) { this.height = height; }

    /**
     * Establece posición y tamaño en un solo método.
     */
    public void setRect(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /**
     * Establece desde otro RectangleH.
     */
    public void setRect(RectangleH bounds) {
        setRect(bounds.x, bounds.y, bounds.width, bounds.height);
    }

    /**
     * Establece desde Rectangle2D.
     */
    public void setRect(Rectangle2D rect) {
        setRect(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
    }

    /**
     * Alias de setRect (compatibilidad con Rectangle2D.setFrame).
     */
    public void setFrame(double x, double y, double width, double height) {
        setRect(x, y, width, height);
    }

    /**
     * Alias de setRect (compatibilidad con Rectangle.setBounds).
     */
    public void setBounds(double x, double y, double width, double height) {
        setRect(x, y, width, height);
    }

    /**
     * Alias de setRect (compatibilidad con Rectangle.setBounds).
     */
    public void setBounds(RectangleH bounds) {
        setRect(bounds);
    }

    /**
     * Alias de setRect (compatibilidad con Rectangle.reshape - deprecated en AWT pero incluido por completitud).
     * @deprecated Usar setRect en su lugar.
     */
    @Deprecated
    public void reshape(double x, double y, double width, double height) {
        setRect(x, y, width, height);
    }

    /**
     * Retorna una copia de este bounds (equivalente a getBounds2D).
     */
    public RectangleH getBounds() {
        return new RectangleH(this);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ══ QUERIES GEOMÉTRICAS
    // ═══════════════════════════════════════════════════════════════════════

    public double getMinX() { return x; }
    public double getMinY() { return y; }
    public double getMaxX() { return x + width; }
    public double getMaxY() { return y + height; }

    public double getCenterX() { return x + width * 0.5; }
    public double getCenterY() { return y + height * 0.5; }

    // Aliases para compatibilidad con código existente
    public double centerX() { return getCenterX(); }
    public double centerY() { return getCenterY(); }
    public double left()    { return getMinX(); }
    public double right()   { return getMaxX(); }
    public double top()     { return getMinY(); }
    public double bottom()  { return getMaxY(); }

    /**
     * Retorna true si este bounds no representa un área o punto válido.
     * 
     * CONVENCIÓN PARA RECTANGLEH:
     * - width < 0 o height < 0: VACÍO (inválido)
     * - width == 0 o height == 0: VÁLIDO (punto o línea degenerada)
     * - width > 0 y height > 0: VÁLIDO (área rectangular)
     * 
     * DIFERENCIA CON AWT Rectangle:
     * AWT considera vacío cualquier rectángulo con width <= 0 o height <= 0.
     * RectangleH permite puntos (width=0, height=0) y líneas degeneradas como válidos.
     * 
     * RAZÓN: En física y colisiones de precisión double, los puntos y líneas
     * son casos geométricos válidos que deben procesarse (ej: posición exacta,
     * swept bounds de velocidad cero en un eje).
     * 
     * @return true si tiene dimensiones negativas (inválido/vacío)
     */
    public boolean isEmpty() {
        return width < 0.0 || height < 0.0;
    }

    /**
     * Retorna una copia de este bounds (equivalente a getBounds() en Rectangle).
     */
    public RectangleH getBounds2D() {
        return new RectangleH(this);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ══ MODIFICACIÓN DE POSICIÓN Y TAMAÑO
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Mueve el bounds a una nueva posición (sin cambiar tamaño).
     */
    public void setLocation(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Mueve el bounds a la posición de un punto.
     */
    public void setLocation(Point2D point) {
        setLocation(point.getX(), point.getY());
    }

    /**
     * Mueve el bounds a una nueva posición (alias para compatibilidad con Rectangle.move).
     * @deprecated Usar setLocation en su lugar.
     */
    @Deprecated
    public void move(double x, double y) {
        setLocation(x, y);
    }

    /**
     * Cambia el tamaño del bounds (sin cambiar posición).
     */
    public void setSize(double width, double height) {
        this.width = width;
        this.height = height;
    }

    /**
     * Cambia el tamaño del bounds (alias para compatibilidad con Rectangle.resize).
     * @deprecated Usar setSize en su lugar.
     */
    @Deprecated
    public void resize(double width, double height) {
        setSize(width, height);
    }

    /**
     * Traslada el bounds por un delta (suma a la posición actual).
     */
    public void translate(double dx, double dy) {
        this.x += dx;
        this.y += dy;
    }

    /**
     * Expande o contrae el bounds desde su centro.
     * Un valor positivo agranda, negativo achica.
     * Si el resultado produce dimensiones negativas, se ajusta a cero.
     */
    public void grow(double h, double v) {
        this.x -= h;
        this.y -= v;
        this.width += h * 2.0;
        this.height += v * 2.0;
        
        // Prevenir dimensiones negativas
        if (this.width < 0.0) {
            this.x += this.width * 0.5;
            this.width = 0.0;
        }
        if (this.height < 0.0) {
            this.y += this.height * 0.5;
            this.height = 0.0;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ══ TESTS DE CONTENCIÓN
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Test punto-en-bounds (alias de contains para compatibilidad con Rectangle.inside).
     * @deprecated Usar contains en su lugar.
     */
    @Deprecated
    public boolean inside(double px, double py) {
        return contains(px, py);
    }

    /**
     * Test punto-en-bounds usando límites semiabiertos [x, x+width) × [y, y+height).
     * 
     * CONVENCIÓN DE LÍMITES:
     * - Borde izquierdo e inferior: INCLUIDO (px >= x, py >= y)
     * - Borde derecho y superior: EXCLUIDO (px < x+width, py < y+height)
     * 
     * Ejemplo con bounds (0, 0, 10, 10):
     * - (0, 0): DENTRO ✓
     * - (9.999, 9.999): DENTRO ✓
     * - (10, 0): FUERA ✗ (borde derecho)
     * - (0, 10): FUERA ✗ (borde superior)
     * 
     * DIFERENCIA CON intersectsLine():
     * contains() usa límites semiabiertos mientras que intersectsLine()
     * considera válidos los contactos tangentes en los bordes. Esto es
     * intencional: un punto ESTÁ o NO ESTÁ dentro, pero una línea puede
     * TOCAR un borde sin cruzarlo.
     * 
     * COHERENCIA CON outcode():
     * outcode(px, py) == 0  ⟺  contains(px, py) == true
     */
    public boolean contains(double px, double py) {
        return px >= x && px < x + width
                && py >= y && py < y + height;
    }

    /**
     * Test punto-en-bounds (Point2D).
     */
    public boolean contains(Point2D point) {
        return contains(point.getX(), point.getY());
    }

    /**
     * Test si este bounds contiene completamente otro bounds.
     */
    public boolean contains(RectangleH bounds) {
        return contains(bounds.x, bounds.y, bounds.width, bounds.height);
    }

    /**
     * Test si este bounds contiene completamente un rectángulo dado.
     */
    public boolean contains(double rx, double ry, double rw, double rh) {
        if (isEmpty() || rw <= 0.0 || rh <= 0.0) {
            return false;
        }
        return rx >= x && ry >= y
                && (rx + rw) <= (x + width)
                && (ry + rh) <= (y + height);
    }

    /**
     * Test si este bounds contiene completamente un Rectangle2D.
     */
    public boolean contains(Rectangle2D rect) {
        return contains(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ══ TESTS DE INTERSECCIÓN
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Test AABB: true si este bounds intersecta con otro.
     * Usa comparaciones double directas sin pérdida de precisión.
     * 
     * COMPORTAMIENTO CON DIMENSIONES CERO:
     * - Bounds vacíos (negativos) nunca intersectan
     * - Puntos (0,0) pueden intersectar si están dentro del otro bounds
     * - Líneas degeneradas se tratan como casos válidos
     * 
     * CONTACTO DE BORDE:
     * Los bounds que solo se tocan por un borde NO se consideran intersectando.
     * Ejemplo: (0,0,10,10) y (10,0,10,10) no intersectan (tocan en x=10).
     */
    public boolean intersects(RectangleH other) {
        // Bounds vacíos nunca intersectan
        if (isEmpty() || other.isEmpty()) {
            return false;
        }
        // Intersección estricta: los bordes deben solaparse, no solo tocarse
        return x < other.x + other.width
                && x + width > other.x
                && y < other.y + other.height
                && y + height > other.y;
    }

    /**
     * Test AABB inclusivo: true si este bounds intersecta o TOCA otro bounds.
     * 
     * A diferencia de intersects(), este método considera contacto de borde
     * como intersección válida.
     * 
     * Ejemplo:
     *   A = (0,0,10,10), B = (10,0,10,10)
     *   A.intersects(B) = false (solo se tocan)
     *   A.intersectsInclusive(B) = true (contacto válido)
     * 
     * Útil para:
     * - Detección de contactos físicos (objetos que se tocan)
     * - Grid/tile adjacency checks
     * - Pathfinding y navegación
     */
    public boolean intersectsInclusive(RectangleH other) {
        if (isEmpty() || other.isEmpty()) {
            return false;
        }
        // Intersección inclusiva: <= en lugar de <
        return x <= other.x + other.width
                && x + width >= other.x
                && y <= other.y + other.height
                && y + height >= other.y;
    }

    /**
     * Test AABB con tolerancia geométrica explícita.
     * 
     * Considera que hay intersección si los bounds están a una distancia
     * menor o igual que la tolerancia especificada.
     * 
     * @param other bounds a verificar
     * @param tolerance distancia máxima para considerar intersección (en unidades de mundo)
     * @return true si intersectan o están dentro de la tolerancia
     * 
     * Ejemplo con tolerance=1.0:
     *   A = (0,0,10,10), B = (11,0,10,10)
     *   A.intersects(B) = false (separados por 1px)
     *   A.intersectsWithTolerance(B, 1.0) = true (dentro de tolerancia)
     * 
     * Útil para:
     * - Detección "casi-colisión" para physics warmup
     * - Proximity queries con margen de error
     * - Compensación de imprecisión numérica acumulada
     */
    public boolean intersectsWithTolerance(RectangleH other, double tolerance) {
        if (isEmpty() || other.isEmpty()) {
            return false;
        }
        // Expandir conceptualmente ambos bounds por tolerance/2
        return x < other.x + other.width + tolerance
                && x + width + tolerance > other.x
                && y < other.y + other.height + tolerance
                && y + height + tolerance > other.y;
    }

    /**
     * Test si este bounds intersecta un rectángulo dado.
     */
    public boolean intersects(double rx, double ry, double rw, double rh) {
        if (isEmpty() || rw <= 0.0 || rh <= 0.0) {
            return false;
        }
        return x < rx + rw
                && x + width > rx
                && y < ry + rh
                && y + height > ry;
    }

    /**
     * Test si este bounds intersecta un Rectangle2D.
     */
    public boolean intersects(Rectangle2D rect) {
        return intersects(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
    }

    /**
     * Test si una línea intersecta este bounds.
     * 
     * Implementación basada en algoritmo Cohen-Sutherland que maneja correctamente:
     * - Segmentos horizontales (y1 == y2)
     * - Segmentos verticales (x1 == x2)
     * - Segmentos diagonales
     * - Segmentos tangentes a los bordes
     * - Segmentos completamente dentro
     * - Segmentos completamente fuera
     * 
     * @param x1 coordenada X del primer punto
     * @param y1 coordenada Y del primer punto
     * @param x2 coordenada X del segundo punto
     * @param y2 coordenada Y del segundo punto
     * @return true si el segmento intersecta o toca el bounds
     */
    public boolean intersectsLine(double x1, double y1, double x2, double y2) {
        // Caso especial: segmento degenerado (punto)
        if (x1 == x2 && y1 == y2) {
            return contains(x1, y1);
        }
        
        // Implementación basada en algoritmo Cohen-Sutherland
        int outcode0 = outcode(x1, y1);
        int outcode1 = outcode(x2, y2);

        while (true) {
            if ((outcode0 | outcode1) == 0) {
                // Ambos puntos dentro
                return true;
            }
            if ((outcode0 & outcode1) != 0) {
                // Ambos puntos fuera en el mismo lado
                return false;
            }

            // Al menos un punto fuera: calculamos intersección
            int outcodeOut = (outcode0 != 0) ? outcode0 : outcode1;
            double ix, iy;

            if ((outcodeOut & OUT_TOP) != 0) { // TOP
                // Evitar división por cero en líneas horizontales
                if (y2 - y1 == 0) return false;
                ix = x1 + (x2 - x1) * (y - y1) / (y2 - y1);
                iy = y;
            } else if ((outcodeOut & OUT_BOTTOM) != 0) { // BOTTOM
                // Evitar división por cero en líneas horizontales
                if (y2 - y1 == 0) return false;
                ix = x1 + (x2 - x1) * (y + height - y1) / (y2 - y1);
                iy = y + height;
            } else if ((outcodeOut & OUT_RIGHT) != 0) { // RIGHT
                // Evitar división por cero en líneas verticales
                if (x2 - x1 == 0) return false;
                iy = y1 + (y2 - y1) * (x + width - x1) / (x2 - x1);
                ix = x + width;
            } else { // LEFT
                // Evitar división por cero en líneas verticales
                if (x2 - x1 == 0) return false;
                iy = y1 + (y2 - y1) * (x - x1) / (x2 - x1);
                ix = x;
            }

            if (outcodeOut == outcode0) {
                x1 = ix;
                y1 = iy;
                outcode0 = outcode(x1, y1);
            } else {
                x2 = ix;
                y2 = iy;
                outcode1 = outcode(x2, y2);
            }
        }
    }

    /**
     * Outcode para algoritmo Cohen-Sutherland (compatible con Rectangle2D).
     * Retorna un código de bits indicando la posición relativa del punto respecto a este bounds:
     * - OUT_LEFT (1): el punto está a la izquierda
     * - OUT_RIGHT (2): el punto está a la derecha
     * - OUT_TOP (8): el punto está arriba
     * - OUT_BOTTOM (4): el punto está abajo
     * - 0: el punto está dentro
     * 
     * COHERENCIA CON contains():
     * - outcode(px, py) == 0  ⟺  contains(px, py) == true
     * - outcode(px, py) != 0  ⟺  contains(px, py) == false
     * 
     * NOTA: Los puntos exactamente en los bordes (x == bounds.x o y == bounds.y)
     * se consideran DENTRO para coherencia con contains() que usa >= en los límites inferiores.
     */
    public int outcode(double px, double py) {
        int code = 0;
        if (py >= y + height) code |= OUT_BOTTOM;  // 4 - punto abajo del bounds
        else if (py < y) code |= OUT_TOP;          // 8 - punto arriba del bounds
        if (px >= x + width) code |= OUT_RIGHT;    // 2 - punto a la derecha
        else if (px < x) code |= OUT_LEFT;         // 1 - punto a la izquierda
        return code;
    }

    // Constantes de outcode (compatibles con Rectangle2D)
    public static final int OUT_LEFT = 1;
    public static final int OUT_TOP = 8;
    public static final int OUT_RIGHT = 2;
    public static final int OUT_BOTTOM = 4;

    // ═══════════════════════════════════════════════════════════════════════
    // ══ OPERACIONES GEOMÉTRICAS (Retornan nuevos bounds)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Calcula la intersección entre este bounds y otro.
     * Retorna un nuevo RectangleH con el área común.
     * Si no hay intersección (o solo se tocan por un borde), retorna un bounds vacío.
     */
    public RectangleH intersection(RectangleH other) {
        double tx1 = Math.max(this.x, other.x);
        double ty1 = Math.max(this.y, other.y);
        double tx2 = Math.min(this.x + this.width, other.x + other.width);
        double ty2 = Math.min(this.y + this.height, other.y + other.height);

        // Si no hay overlap real (solo contacto de borde), retornar vacío
        if (tx2 <= tx1 || ty2 <= ty1) {
            return new RectangleH(0, 0, -1, -1); // Bounds vacío con dimensiones negativas
        }
        return new RectangleH(tx1, ty1, tx2 - tx1, ty2 - ty1);
    }

    /**
     * Alias de intersection para compatibilidad con Rectangle2D.
     */
    public RectangleH createIntersection(RectangleH other) {
        return intersection(other);
    }

    /**
     * Alias de intersection para compatibilidad con Rectangle2D.
     */
    public Rectangle2D createIntersection(Rectangle2D other) {
        RectangleH b = new RectangleH(other);
        return intersection(b).toRectangle2D();
    }

    /**
     * Calcula la unión entre este bounds y otro.
     * Retorna un nuevo RectangleH que contiene ambos.
     * Si uno está vacío, retorna una copia del otro.
     * Si ambos están vacíos, retorna un bounds vacío.
     */
    public RectangleH union(RectangleH other) {
        if (isEmpty()) {
            return new RectangleH(other);
        }
        if (other.isEmpty()) {
            return new RectangleH(this);
        }

        double tx1 = Math.min(this.x, other.x);
        double ty1 = Math.min(this.y, other.y);
        double tx2 = Math.max(this.x + this.width, other.x + other.width);
        double ty2 = Math.max(this.y + this.height, other.y + other.height);

        return new RectangleH(tx1, ty1, tx2 - tx1, ty2 - ty1);
    }

    /**
     * Alias de union para compatibilidad con Rectangle2D.
     */
    public RectangleH createUnion(RectangleH other) {
        return union(other);
    }

    /**
     * Alias de union para compatibilidad con Rectangle2D.
     */
    public Rectangle2D createUnion(Rectangle2D other) {
        RectangleH b = new RectangleH(other);
        return union(b).toRectangle2D();
    }

    /**
     * Retorna un nuevo bounds trasladado.
     */
    public RectangleH translated(double dx, double dy) {
        return new RectangleH(x + dx, y + dy, width, height);
    }

    /**
     * Retorna un nuevo bounds expandido/contraído.
     */
    public RectangleH grown(double h, double v) {
        return new RectangleH(x - h, y - v, width + h * 2.0, height + v * 2.0);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ══ OPERACIONES IN-PLACE (Modifican este bounds)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Expande este bounds para incluir un punto.
     * Si este bounds está vacío (dimensiones negativas), se convierte en un punto.
     * Maneja correctamente la acumulación de puntos, incluso cuando el bounds tiene dimensiones cero.
     */
    public void add(double px, double py) {
        if (isEmpty()) {
            // Bounds vacío: crear punto en esa posición
            setRect(px, py, 0.0, 0.0);
            return;
        }
        
        // Calcular los extremos actuales (incluye bounds de dimensión cero)
        double x1 = x;
        double y1 = y;
        double x2 = x + width;
        double y2 = y + height;
        
        // Expandir para incluir el nuevo punto
        x1 = Math.min(x1, px);
        y1 = Math.min(y1, py);
        x2 = Math.max(x2, px);
        y2 = Math.max(y2, py);
        
        setRect(x1, y1, x2 - x1, y2 - y1);
    }

    /**
     * Expande este bounds para incluir un punto.
     */
    public void add(Point2D point) {
        add(point.getX(), point.getY());
    }

    /**
     * Expande este bounds para incluir otro bounds.
     * Si este bounds está vacío, se convierte en una copia del otro bounds.
     * Si el otro bounds está vacío (dimensiones negativas), no hace nada.
     * Los bounds con dimensiones cero (puntos) SÍ se incluyen.
     */
    public void add(RectangleH bounds) {
        if (bounds.isEmpty()) {
            return; // Ignorar bounds vacíos
        }
        if (isEmpty()) {
            setRect(bounds);
            return;
        }
        double x1 = Math.min(x, bounds.x);
        double y1 = Math.min(y, bounds.y);
        double x2 = Math.max(x + width, bounds.x + bounds.width);
        double y2 = Math.max(y + height, bounds.y + bounds.height);
        setRect(x1, y1, x2 - x1, y2 - y1);
    }

    /**
     * Expande este bounds para incluir un Rectangle.
     */
    public void add(Rectangle rect) {
        add(rect.x, rect.y, rect.width, rect.height);
    }

    /**
     * Expande este bounds para incluir un Rectangle2D.
     */
    public void add(Rectangle2D rect) {
        add(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
    }

    /**
     * Expande este bounds para incluir un rectángulo.
     * Si este bounds está vacío, se convierte en el rectángulo dado.
     * Si el rectángulo tiene dimensiones negativas, no hace nada.
     * Los rectángulos con dimensiones cero SÍ se incluyen.
     */
    public void add(double rx, double ry, double rw, double rh) {
        if (rw < 0.0 || rh < 0.0) {
            return; // Ignorar rectángulos vacíos
        }
        if (isEmpty()) {
            setRect(rx, ry, rw, rh);
            return;
        }
        double x1 = Math.min(x, rx);
        double y1 = Math.min(y, ry);
        double x2 = Math.max(x + width, rx + rw);
        double y2 = Math.max(y + height, ry + rh);
        setRect(x1, y1, x2 - x1, y2 - y1);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ══ CONVERSIÓN A OTROS TIPOS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Convierte a java.awt.Rectangle para rendering o APIs legacy.
     * Usa floor/ceil para garantizar que el Rectangle cubra completamente este bounds.
     * 
     * OVERFLOW DE INT:
     * Si las coordenadas exceden el rango de int (±2^31), se ajustan a los límites:
     * - Coordenadas < Integer.MIN_VALUE → Integer.MIN_VALUE
     * - Coordenadas > Integer.MAX_VALUE → Integer.MAX_VALUE
     * 
     * IMPORTANTE: Este método agranda ligeramente para evitar pérdida de área.
     * Para colisiones, usar siempre RectangleH directamente.
     * 
     * Ejemplo:
     *   RectangleH(10.3, 20.7, 5.4, 8.2) → Rectangle(10, 20, 6, 9)
     *   El Rectangle cubre [10..16] × [20..29], garantizando que incluye [10.3..15.7] × [20.7..28.9]
     */
    public Rectangle toRectangle() {
        // Clamp a rango de int para prevenir overflow
        int left = clampToInt(Math.floor(x));
        int top = clampToInt(Math.floor(y));
        int right = clampToInt(Math.ceil(x + width));
        int bottom = clampToInt(Math.ceil(y + height));
        
        // Calcular width y height con protección contra overflow
        int w = (right > left) ? Math.min(right - left, Integer.MAX_VALUE) : 0;
        int h = (bottom > top) ? Math.min(bottom - top, Integer.MAX_VALUE) : 0;
        
        return new Rectangle(left, top, w, h);
    }

    /**
     * Convierte a Rectangle redondeando al int más cercano.
     * Más preciso visualmente pero puede no cubrir completamente el bounds original.
     * Útil para rendering donde la precisión visual es más importante que la cobertura exacta.
     * 
     * OVERFLOW DE INT:
     * Las coordenadas se ajustan al rango válido de int si es necesario.
     */
    public Rectangle toRectangleRounded() {
        return new Rectangle(
                clampToInt(Math.round(x)),
                clampToInt(Math.round(y)),
                clampToInt(Math.round(width)),
                clampToInt(Math.round(height))
        );
    }
    
    /**
     * Ajusta un double al rango válido de int.
     */
    private static int clampToInt(double value) {
        if (value <= Integer.MIN_VALUE) return Integer.MIN_VALUE;
        if (value >= Integer.MAX_VALUE) return Integer.MAX_VALUE;
        return (int) value;
    }

    /**
     * Convierte a java.awt.geom.Rectangle2D.Double.
     */
    public Rectangle2D.Double toRectangle2D() {
        return new Rectangle2D.Double(x, y, width, height);
    }

    /**
     * Crea un RectangleH desde un Rectangle (int → double).
     */
    public static RectangleH fromRectangle(Rectangle rect) {
        return new RectangleH(rect);
    }

    /**
     * Crea un RectangleH desde un Rectangle2D.
     */
    public static RectangleH fromRectangle2D(Rectangle2D rect) {
        return new RectangleH(rect);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ══ UTILIDADES ADICIONALES (No están en Rectangle, pero son útiles)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Calcula el área del bounds.
     */
    public double area() {
        return width * height;
    }

    /**
     * Calcula el perímetro del bounds.
     */
    public double perimeter() {
        return 2.0 * (width + height);
    }

    /**
     * Calcula el área del overlap con otro bounds.
     * Retorna 0.0 si no hay intersección.
     */
    public double overlapArea(RectangleH other) {
        if (!intersects(other)) return 0.0;

        double overlapWidth = Math.min(right(), other.right())
                - Math.max(left(), other.left());
        double overlapHeight = Math.min(bottom(), other.bottom())
                - Math.max(top(), other.top());

        return overlapWidth * overlapHeight;
    }

    /**
     * Calcula la distancia euclidiana entre los centros.
     */
    public double distanceToCenter(RectangleH other) {
        double dx = centerX() - other.centerX();
        double dy = centerY() - other.centerY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Calcula la distancia Manhattan entre los centros.
     */
    public double manhattanDistanceToCenter(RectangleH other) {
        return Math.abs(centerX() - other.centerX())
                + Math.abs(centerY() - other.centerY());
    }

    /**
     * Calcula el aspect ratio (ancho / alto).
     */
    public double aspectRatio() {
        return height != 0.0 ? width / height : Double.POSITIVE_INFINITY;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ══ UTILIDADES DE DISTANCIA Y PROXIMIDAD
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Calcula la distancia más corta desde un punto a este bounds.
     * Retorna 0.0 si el punto está dentro O EN EL BORDE.
     * 
     * COMPORTAMIENTO EN BORDES:
     * Un punto en el borde derecho (x == bounds.x + bounds.width) o
     * superior (y == bounds.y + bounds.height) retorna distancia 0.0,
     * aunque contains() lo considere fuera por usar límites semiabiertos.
     * 
     * RAZÓN: Geométricamente, un punto EN el borde tiene distancia cero
     * al rectángulo. La diferencia con contains() es intencional:
     * - contains(): membresía lógica (¿está DENTRO del área?)
     * - distanceToPoint(): distancia geométrica (¿qué tan lejos está?)
     * 
     * Estos son conceptos diferentes y ambos comportamientos son correctos
     * en sus respectivos contextos.
     */
    public double distanceToPoint(double px, double py) {
        double dx = Math.max(0.0, Math.max(x - px, px - (x + width)));
        double dy = Math.max(0.0, Math.max(y - py, py - (y + height)));
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Calcula la distancia más corta desde un punto a este bounds.
     */
    public double distanceToPoint(Point2D point) {
        return distanceToPoint(point.getX(), point.getY());
    }

    /**
     * Calcula la distancia más corta entre dos bounds.
     * Retorna 0.0 si se están intersectando.
     */
    public double distanceTo(RectangleH other) {
        if (intersects(other)) return 0.0;

        double dx = Math.max(0.0, Math.max(x - (other.x + other.width), other.x - (x + width)));
        double dy = Math.max(0.0, Math.max(y - (other.y + other.height), other.y - (y + height)));
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Verifica si otro bounds está dentro de un radio desde este bounds.
     */
    public boolean isNearby(RectangleH other, double radius) {
        return distanceTo(other) <= radius;
    }

    /**
     * Calcula el punto más cercano en este bounds a un punto dado.
     * Si el punto está dentro, retorna el punto mismo.
     */
    public Point2D.Double closestPoint(double px, double py) {
        double cx = Math.max(x, Math.min(px, x + width));
        double cy = Math.max(y, Math.min(py, y + height));
        return new Point2D.Double(cx, cy);
    }

    /**
     * Calcula el punto más cercano en este bounds a un punto dado.
     */
    public Point2D.Double closestPoint(Point2D point) {
        return closestPoint(point.getX(), point.getY());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ══ UTILIDADES DE PENETRACIÓN Y OVERLAP
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Calcula la penetración en el eje X con otro bounds.
     * Retorna el desplazamiento necesario para separar este bounds del otro.
     * Positivo = mover hacia la derecha, negativo = mover hacia la izquierda.
     * Retorna 0.0 si no hay overlap.
     * 
     * CASO ESPECIAL - CENTROS COINCIDENTES EN X:
     * Cuando los centros tienen exactamente la misma coordenada X (caso raro pero posible),
     * el desempate se hace por el orden de las coordenadas absolutas:
     * - Si this.x < other.x: mover hacia la izquierda (negativo)
     * - Si this.x >= other.x: mover hacia la derecha (positivo)
     * Esto garantiza un comportamiento determinista independiente del orden de llamada.
     * 
     * Ejemplo:
     *   A está en (0,0,10,10), B está en (8,0,10,10)
     *   A.penetrationX(B) retorna -2.0 (mover A hacia la izquierda)
     *   B.penetrationX(A) retorna +2.0 (mover B hacia la derecha)
     */
    public double penetrationX(RectangleH other) {
        if (!intersects(other)) return 0.0;

        double leftPenetration = (x + width) - other.x;      // Cuánto penetra este bounds por la izquierda de other
        double rightPenetration = (other.x + other.width) - x; // Cuánto penetra este bounds por la derecha de other

        // Elegir la penetración más pequeña
        if (Math.abs(leftPenetration - rightPenetration) < 1e-10) {
            // Centros casi coincidentes: desempate por posición absoluta
            return (x < other.x) ? -leftPenetration : rightPenetration;
        }
        
        return (leftPenetration < rightPenetration) ? -leftPenetration : rightPenetration;
    }

    /**
     * Calcula la penetración en el eje Y con otro bounds.
     * Retorna el desplazamiento necesario para separar este bounds del otro.
     * Positivo = mover hacia abajo, negativo = mover hacia arriba.
     * Retorna 0.0 si no hay overlap.
     * 
     * CASO ESPECIAL - CENTROS COINCIDENTES EN Y:
     * Cuando los centros tienen exactamente la misma coordenada Y,
     * el desempate se hace por el orden de las coordenadas absolutas.
     * Ver documentación de penetrationX() para detalles.
     * 
     * Ejemplo:
     *   A está en (0,0,10,10), B está en (0,8,10,10)
     *   A.penetrationY(B) retorna -2.0 (mover A hacia arriba)
     *   B.penetrationY(A) retorna +2.0 (mover B hacia abajo)
     */
    public double penetrationY(RectangleH other) {
        if (!intersects(other)) return 0.0;

        double topPenetration = (y + height) - other.y;      // Cuánto penetra este bounds por arriba de other
        double bottomPenetration = (other.y + other.height) - y; // Cuánto penetra este bounds por abajo de other

        // Elegir la penetración más pequeña
        if (Math.abs(topPenetration - bottomPenetration) < 1e-10) {
            // Centros casi coincidentes: desempate por posición absoluta
            return (y < other.y) ? -topPenetration : bottomPenetration;
        }
        
        return (topPenetration < bottomPenetration) ? -topPenetration : bottomPenetration;
    }

    /**
     * Calcula el vector de penetración mínimo (MTV - Minimum Translation Vector).
     * Retorna el vector que mueve este bounds fuera del otro con mínimo desplazamiento.
     * Retorna (0, 0) si no hay intersección.
     */
    public Point2D.Double minimumTranslationVector(RectangleH other) {
        if (!intersects(other)) {
            return new Point2D.Double(0.0, 0.0);
        }

        double penX = penetrationX(other);
        double penY = penetrationY(other);

        if (Math.abs(penX) < Math.abs(penY)) {
            return new Point2D.Double(penX, 0.0);
        } else {
            return new Point2D.Double(0.0, penY);
        }
    }

    /**
     * Calcula el porcentaje de overlap con otro bounds (0.0 a 1.0).
     * Basado en el área de este bounds.
     */
    public double overlapPercentage(RectangleH other) {
        double overlap = overlapArea(other);
        double thisArea = area();
        return thisArea > 0.0 ? overlap / thisArea : 0.0;
    }

    /**
     * Calcula la profundidad de solapamiento en el eje X.
     * Retorna un valor positivo indicando cuánto se solapan en X,
     * o 0.0 si no hay solapamiento.
     * 
     * A diferencia de penetrationX(), este método NO calcula la dirección
     * de separación, solo la magnitud del overlap.
     * 
     * Útil para:
     * - Queries de proximidad simples
     * - Scoring de colisiones por profundidad
     * - Optimizaciones que solo necesitan magnitud
     */
    public double overlapX(RectangleH other) {
        if (!intersects(other)) return 0.0;
        
        double leftEdge = Math.max(x, other.x);
        double rightEdge = Math.min(x + width, other.x + other.width);
        return rightEdge - leftEdge;
    }

    /**
     * Calcula la profundidad de solapamiento en el eje Y.
     * Retorna un valor positivo indicando cuánto se solapan en Y,
     * o 0.0 si no hay solapamiento.
     * 
     * Ver overlapX() para detalles.
     */
    public double overlapY(RectangleH other) {
        if (!intersects(other)) return 0.0;
        
        double topEdge = Math.max(y, other.y);
        double bottomEdge = Math.min(y + height, other.y + other.height);
        return bottomEdge - topEdge;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ══ UTILIDADES DE ESCALA Y TRANSFORMACIÓN
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Escala este bounds desde su centro.
     * factor > 1.0 agranda, factor < 1.0 achica.
     * Factores negativos se convierten a su valor absoluto.
     * Si el resultado produce dimensiones negativas, se ajusta a cero.
     */
    public void scale(double factor) {
        // Prevenir factores negativos
        factor = Math.abs(factor);
        
        double cx = getCenterX();
        double cy = getCenterY();
        double newWidth = width * factor;
        double newHeight = height * factor;
        x = cx - newWidth * 0.5;
        y = cy - newHeight * 0.5;
        width = newWidth;
        height = newHeight;
    }

    /**
     * Escala este bounds desde su centro con factores independientes.
     * Factores negativos se convierten a su valor absoluto.
     */
    public void scale(double factorX, double factorY) {
        // Prevenir factores negativos
        factorX = Math.abs(factorX);
        factorY = Math.abs(factorY);
        
        double cx = getCenterX();
        double cy = getCenterY();
        double newWidth = width * factorX;
        double newHeight = height * factorY;
        x = cx - newWidth * 0.5;
        y = cy - newHeight * 0.5;
        width = newWidth;
        height = newHeight;
    }

    /**
     * Retorna un nuevo bounds escalado desde su centro.
     */
    public RectangleH scaled(double factor) {
        RectangleH result = new RectangleH(this);
        result.scale(factor);
        return result;
    }

    /**
     * Retorna un nuevo bounds escalado con factores independientes.
     */
    public RectangleH scaled(double factorX, double factorY) {
        RectangleH result = new RectangleH(this);
        result.scale(factorX, factorY);
        return result;
    }

    /**
     * Expande este bounds por un margen uniforme en todos los lados.
     * Márgenes negativos contraen el bounds.
     * Si el resultado produce dimensiones negativas, se ajusta a cero.
     */
    public void expand(double margin) {
        x -= margin;
        y -= margin;
        width += margin * 2.0;
        height += margin * 2.0;
        
        // Prevenir dimensiones negativas
        if (width < 0.0) {
            x += width * 0.5;
            width = 0.0;
        }
        if (height < 0.0) {
            y += height * 0.5;
            height = 0.0;
        }
    }

    /**
     * Expande este bounds con márgenes independientes.
     * Márgenes negativos contraen el bounds.
     * Si el resultado produce dimensiones negativas, se ajusta a cero.
     */
    public void expand(double horizontal, double vertical) {
        x -= horizontal;
        y -= vertical;
        width += horizontal * 2.0;
        height += vertical * 2.0;
        
        // Prevenir dimensiones negativas
        if (width < 0.0) {
            x += width * 0.5;
            width = 0.0;
        }
        if (height < 0.0) {
            y += height * 0.5;
            height = 0.0;
        }
    }

    /**
     * Retorna un nuevo bounds expandido uniformemente.
     */
    public RectangleH expanded(double margin) {
        return new RectangleH(
                x - margin,
                y - margin,
                width + margin * 2.0,
                height + margin * 2.0
        );
    }

    /**
     * Retorna un nuevo bounds expandido con márgenes independientes.
     */
    public RectangleH expanded(double horizontal, double vertical) {
        return new RectangleH(
                x - horizontal,
                y - vertical,
                width + horizontal * 2.0,
                height + vertical * 2.0
        );
    }

    /**
     * Contrae este bounds (expand negativo).
     */
    public void shrink(double margin) {
        expand(-margin);
    }

    /**
     * Retorna un nuevo bounds contraído.
     */
    public RectangleH shrunken(double margin) {
        return expanded(-margin);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ══ UTILIDADES DE ALINEACIÓN Y POSICIONAMIENTO
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Centra este bounds en un punto dado.
     */
    public void centerOn(double cx, double cy) {
        x = cx - width * 0.5;
        y = cy - height * 0.5;
    }

    /**
     * Centra este bounds en un punto dado.
     */
    public void centerOn(Point2D point) {
        centerOn(point.getX(), point.getY());
    }

    /**
     * Centra este bounds dentro de otro bounds.
     */
    public void centerIn(RectangleH container) {
        centerOn(container.getCenterX(), container.getCenterY());
    }

    /**
     * Retorna un nuevo bounds centrado en un punto.
     */
    public RectangleH centeredOn(double cx, double cy) {
        return new RectangleH(
                cx - width * 0.5,
                cy - height * 0.5,
                width,
                height
        );
    }

    /**
     * Retorna un nuevo bounds centrado en otro bounds.
     */
    public RectangleH centeredIn(RectangleH container) {
        return centeredOn(container.getCenterX(), container.getCenterY());
    }

    /**
     * Ajusta este bounds para quedar completamente dentro de otro bounds.
     * Si es más grande, lo centra.
     */
    public void clampInside(RectangleH container) {
        if (width > container.width || height > container.height) {
            centerIn(container);
            return;
        }

        if (x < container.x) {
            x = container.x;
        } else if (x + width > container.x + container.width) {
            x = container.x + container.width - width;
        }

        if (y < container.y) {
            y = container.y;
        } else if (y + height > container.y + container.height) {
            y = container.y + container.height - height;
        }
    }

    /**
     * Retorna un nuevo bounds ajustado dentro de un contenedor.
     */
    public RectangleH clampedInside(RectangleH container) {
        RectangleH result = new RectangleH(this);
        result.clampInside(container);
        return result;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ══ UTILIDADES DE CORNERS Y EDGES
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Retorna la esquina superior izquierda.
     */
    public Point2D.Double getTopLeft() {
        return new Point2D.Double(x, y);
    }

    /**
     * Retorna la esquina superior derecha.
     */
    public Point2D.Double getTopRight() {
        return new Point2D.Double(x + width, y);
    }

    /**
     * Retorna la esquina inferior izquierda.
     */
    public Point2D.Double getBottomLeft() {
        return new Point2D.Double(x, y + height);
    }

    /**
     * Retorna la esquina inferior derecha.
     */
    public Point2D.Double getBottomRight() {
        return new Point2D.Double(x + width, y + height);
    }

    /**
     * Retorna el punto central.
     */
    public Point2D.Double getCenter() {
        return new Point2D.Double(getCenterX(), getCenterY());
    }

    /**
     * Retorna el punto central del borde superior.
     */
    public Point2D.Double getTopCenter() {
        return new Point2D.Double(getCenterX(), y);
    }

    /**
     * Retorna el punto central del borde inferior.
     */
    public Point2D.Double getBottomCenter() {
        return new Point2D.Double(getCenterX(), y + height);
    }

    /**
     * Retorna el punto central del borde izquierdo.
     */
    public Point2D.Double getLeftCenter() {
        return new Point2D.Double(x, getCenterY());
    }

    /**
     * Retorna el punto central del borde derecho.
     */
    public Point2D.Double getRightCenter() {
        return new Point2D.Double(x + width, getCenterY());
    }

    /**
     * Retorna todas las 4 esquinas en orden: TL, TR, BR, BL.
     */
    public Point2D.Double[] getCorners() {
        return new Point2D.Double[] {
                getTopLeft(),
                getTopRight(),
                getBottomRight(),
                getBottomLeft()
        };
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ══ UTILIDADES DE COMPARACIÓN Y CLASIFICACIÓN
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Verifica si este bounds es más grande que otro (por área).
     */
    public boolean isLargerThan(RectangleH other) {
        return area() > other.area();
    }

    /**
     * Verifica si este bounds es más pequeño que otro (por área).
     */
    public boolean isSmallerThan(RectangleH other) {
        return area() < other.area();
    }

    /**
     * Verifica si este bounds contiene completamente otro bounds.
     */
    public boolean fullyContains(RectangleH other) {
        return contains(other);
    }

    /**
     * Verifica si este bounds está completamente contenido en otro.
     */
    public boolean isContainedIn(RectangleH container) {
        return container.contains(this);
    }

    /**
     * Verifica si este bounds es un cuadrado (width == height con tolerancia).
     */
    public boolean isSquare() {
        return isSquare(0.001);
    }

    /**
     * Verifica si este bounds es un cuadrado con una tolerancia dada.
     */
    public boolean isSquare(double tolerance) {
        return Math.abs(width - height) <= tolerance;
    }

    /**
     * Verifica si este bounds tiene orientación landscape (ancho > alto).
     */
    public boolean isLandscape() {
        return width > height;
    }

    /**
     * Verifica si este bounds tiene orientación portrait (alto > ancho).
     */
    public boolean isPortrait() {
        return height > width;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ══ UTILIDADES DE CONSTRUCCIÓN Y FACTORIES
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Crea un bounds desde un punto central y dimensiones.
     */
    public static RectangleH fromCenter(double cx, double cy, double width, double height) {
        return new RectangleH(
                cx - width * 0.5,
                cy - height * 0.5,
                width,
                height
        );
    }

    /**
     * Crea un bounds desde un punto central y dimensiones.
     */
    public static RectangleH fromCenter(Point2D center, double width, double height) {
        return fromCenter(center.getX(), center.getY(), width, height);
    }

    /**
     * Crea un bounds cuadrado desde un punto central y tamaño.
     */
    public static RectangleH squareFromCenter(double cx, double cy, double size) {
        return fromCenter(cx, cy, size, size);
    }

    /**
     * Crea un bounds desde dos puntos opuestos (cualquier esquina).
     */
    public static RectangleH fromPoints(double x1, double y1, double x2, double y2) {
        double minX = Math.min(x1, x2);
        double minY = Math.min(y1, y2);
        double maxX = Math.max(x1, x2);
        double maxY = Math.max(y1, y2);
        return new RectangleH(minX, minY, maxX - minX, maxY - minY);
    }

    /**
     * Crea un bounds desde dos puntos opuestos.
     */
    public static RectangleH fromPoints(Point2D p1, Point2D p2) {
        return fromPoints(p1.getX(), p1.getY(), p2.getX(), p2.getY());
    }

    /**
     * Crea un bounds que engloba todos los puntos dados.
     * Los elementos null se ignoran.
     * Si no hay puntos válidos, retorna un bounds vacío.
     */
    public static RectangleH fromPoints(Point2D... points) {
        if (points == null || points.length == 0) {
            return new RectangleH(0, 0, -1, -1);
        }

        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;

        int validCount = 0;
        for (Point2D p : points) {
            if (p == null) continue; // Ignorar nulls
            
            minX = Math.min(minX, p.getX());
            minY = Math.min(minY, p.getY());
            maxX = Math.max(maxX, p.getX());
            maxY = Math.max(maxY, p.getY());
            validCount++;
        }

        // Si no hay puntos válidos, retornar vacío
        if (validCount == 0) {
            return new RectangleH(0, 0, -1, -1);
        }

        return new RectangleH(minX, minY, maxX - minX, maxY - minY);
    }

    /**
     * Crea un bounds que engloba todos los bounds dados.
     * Ignora los bounds con dimensiones negativas (vacíos).
     * Los bounds con dimensiones cero (puntos) SÍ se incluyen.
     * Si todos están vacíos, retorna un bounds vacío.
     */
    public static RectangleH union(RectangleH... bounds) {
        if (bounds == null || bounds.length == 0) {
            return new RectangleH(0, 0, -1, -1);
        }

        // Buscar el primer bounds válido (no vacío con dimensiones negativas)
        RectangleH result = null;
        for (RectangleH b : bounds) {
            if (!b.isEmpty()) {  // isEmpty() ahora solo detecta dimensiones negativas
                result = new RectangleH(b);
                break;
            }
        }

        // Si todos estaban vacíos, retornar vacío
        if (result == null) {
            return new RectangleH(0, 0, -1, -1);
        }

        // Agregar el resto de bounds válidos (incluye puntos con dimensión cero)
        for (RectangleH b : bounds) {
            if (!b.isEmpty()) {
                result.add(b);
            }
        }

        return result;
    }

    /**
     * Crea un bounds circular (cuadrado) desde centro y radio.
     */
    public static RectangleH fromCircle(double cx, double cy, double radius) {
        double diameter = radius * 2.0;
        return fromCenter(cx, cy, diameter, diameter);
    }

    /**
     * Crea un bounds circular desde centro y radio.
     */
    public static RectangleH fromCircle(Point2D center, double radius) {
        return fromCircle(center.getX(), center.getY(), radius);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ══ UTILIDADES DE INTERPOLACIÓN Y ANIMACIÓN
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Interpola linealmente entre este bounds y otro.
     * t = 0.0 retorna este bounds, t = 1.0 retorna el otro, valores intermedios interpolan.
     */
    public RectangleH lerp(RectangleH target, double t) {
        return new RectangleH(
                x + (target.x - x) * t,
                y + (target.y - y) * t,
                width + (target.width - width) * t,
                height + (target.height - height) * t
        );
    }

    /**
     * Calcula la diagonal del bounds.
     */
    public double diagonal() {
        return Math.sqrt(width * width + height * height);
    }

    /**
     * Retorna el radio del círculo inscrito (la mitad del lado más pequeño).
     */
    public double inradius() {
        return Math.min(width, height) * 0.5;
    }

    /**
     * Retorna el radio del círculo circunscrito (la mitad de la diagonal).
     */
    public double circumradius() {
        return diagonal() * 0.5;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ══ SWEPT Y SUPPORT (Para física avanzada y CCD)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Construye el bounds barrido (swept) por un desplazamiento dado.
     * 
     * El swept bounds es el AABB que contiene todas las posiciones que
     * este bounds ocupa durante el movimiento desde su posición actual
     * hasta posición + (dx, dy).
     * 
     * @param dx desplazamiento en X
     * @param dy desplazamiento en Y
     * @return nuevo RectangleH que envuelve el path completo
     * 
     * Ejemplo:
     *   bounds = (10, 10, 5, 5), desplazamiento = (3, -2)
     *   sweep(3, -2) = (10, 8, 8, 7)
     *   
     * Útil para:
     * - Broad phase en CCD (Continuous Collision Detection)
     * - Path queries (¿qué objetos puede tocar durante el movimiento?)
     * - Motion blur bounds para rendering
     */
    public RectangleH sweep(double dx, double dy) {
        double minX = Math.min(x, x + dx);
        double minY = Math.min(y, y + dy);
        double maxX = Math.max(x + width, x + width + dx);
        double maxY = Math.max(y + height, y + height + dy);
        return new RectangleH(minX, minY, maxX - minX, maxY - minY);
    }

    /**
     * Calcula la unión entre este bounds y su versión desplazada.
     * Equivalente a: this.union(this.translated(dx, dy))
     * 
     * Más eficiente que crear el bounds trasladado y hacer union.
     * 
     * @param dx desplazamiento en X
     * @param dy desplazamiento en Y
     * @return bounds que contiene posición inicial y final
     */
    public RectangleH sweptUnion(double dx, double dy) {
        return sweep(dx, dy);  // Implementación idéntica, nombre semántico diferente
    }

    /**
     * Obtiene el punto extremo de soporte en una dirección dada.
     * 
     * El punto de soporte es el vértice del bounds que está más lejos
     * en la dirección especificada. Útil para algoritmos de separación
     * de ejes (SAT) y detección de colisiones avanzada.
     * 
     * @param dirX componente X de la dirección (no necesita estar normalizada)
     * @param dirY componente Y de la dirección
     * @return punto del bounds más alejado en esa dirección
     * 
     * Ejemplo:
     *   bounds = (0, 0, 10, 10)
     *   getSupportPoint(1, 0) = (10, 0) o (10, 10) (cualquier esquina derecha)
     *   getSupportPoint(1, 1) = (10, 10) (esquina superior-derecha)
     *   getSupportPoint(-1, 0) = (0, 0) o (0, 10) (cualquier esquina izquierda)
     * 
     * Útil para:
     * - Separating Axis Theorem (SAT)
     * - GJK algorithm para detección de colisiones
     * - Cálculo de distancias dirigidas
     */
    public Point2D.Double getSupportPoint(double dirX, double dirY) {
        // El punto de soporte de un AABB es siempre una de sus esquinas
        // Elegimos la esquina cuyo producto punto con la dirección es máximo
        double supportX = (dirX >= 0) ? (x + width) : x;
        double supportY = (dirY >= 0) ? (y + height) : y;
        return new Point2D.Double(supportX, supportY);
    }

    /**
     * Sobrecarga de getSupportPoint que acepta un Point2D como dirección.
     */
    public Point2D.Double getSupportPoint(Point2D direction) {
        return getSupportPoint(direction.getX(), direction.getY());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ══ VALIDACIÓN Y UTILIDADES DE ESTADO
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Verifica que todas las coordenadas y dimensiones son finitas.
     * 
     * Retorna false si algún campo contiene:
     * - NaN (Not a Number)
     * - POSITIVE_INFINITY
     * - NEGATIVE_INFINITY
     * 
     * Útil para:
     * - Validación de entrada después de cálculos complejos
     * - Debug de errores numéricos
     * - Assertions en código crítico
     * 
     * Ejemplo:
     *   RectangleH valid = new RectangleH(10, 20, 5, 8);
     *   valid.isFinite() = true
     *   
     *   RectangleH invalid = new RectangleH(10, Double.NaN, 5, 8);
     *   invalid.isFinite() = false
     */
    public boolean isFinite() {
        return Double.isFinite(x) 
            && Double.isFinite(y) 
            && Double.isFinite(width) 
            && Double.isFinite(height);
    }

    /**
     * Copia los valores de otro bounds a este bounds.
     * 
     * Actualiza este bounds existente sin crear una nueva instancia.
     * Más eficiente que crear nuevos objetos cuando se reutilizan buffers.
     * 
     * @param source bounds desde el cual copiar los valores
     * 
     * Útil para:
     * - Reutilización de objetos en hot paths (evita GC pressure)
     * - Actualización de bounds cacheados
     * - Patrones de object pooling
     * 
     * Ejemplo:
     *   RectangleH buffer = new RectangleH();
     *   // En hot loop:
     *   buffer.copyFrom(collider.getBounds());  // Sin allocación
     *   processCollision(buffer);
     */
    public void copyFrom(RectangleH source) {
        this.x = source.x;
        this.y = source.y;
        this.width = source.width;
        this.height = source.height;
    }

    /**
     * Copia los valores desde un Rectangle a este bounds.
     */
    public void copyFrom(Rectangle source) {
        this.x = source.x;
        this.y = source.y;
        this.width = source.width;
        this.height = source.height;
    }

    /**
     * Copia los valores desde un Rectangle2D a este bounds.
     */
    public void copyFrom(Rectangle2D source) {
        this.x = source.getX();
        this.y = source.getY();
        this.width = source.getWidth();
        this.height = source.getHeight();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ══ OBJECT OVERRIDES
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public RectangleH clone() {
        try {
            return (RectangleH) super.clone();
        } catch (CloneNotSupportedException e) {
            // No debería ocurrir ya que implementamos Cloneable
            throw new InternalError(e);
        }
    }

    @Override
    public String toString() {
        return String.format("RectangleH[x=%.2f, y=%.2f, w=%.2f, h=%.2f]",
                x, y, width, height);
    }

    /**
     * Compara este bounds con otro objeto para igualdad EXACTA.
     * 
     * IGUALDAD EXACTA vs APROXIMADA:
     * Este método verifica igualdad bit-a-bit de los valores double.
     * Dos bounds son iguales si y solo si x, y, width, height son
     * EXACTAMENTE iguales (mismo patrón de bits IEEE 754).
     * 
     * ADVERTENCIA:
     * Esta NO es igualdad geométrica aproximada. Bounds que difieren
     * en cantidades microscópicas (< epsilon) se consideran diferentes.
     * 
     * Para comparación con tolerancia, usar:
     *   Math.abs(a.x - b.x) < epsilon &&
     *   Math.abs(a.y - b.y) < epsilon &&
     *   Math.abs(a.width - b.width) < epsilon &&
     *   Math.abs(a.height - b.height) < epsilon
     * 
     * USO EN COLECCIONES:
     * Este método es consistente con hashCode() y seguro para
     * usar en HashMap, HashSet, etc.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof RectangleH)) return false;
        RectangleH other = (RectangleH) obj;
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
