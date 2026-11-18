package Game.Engine.GameMath.Logic2D;

/**
 * Axis-Aligned Bounding Box con precisión double.
 * Implementación tipo Minecraft (AxisAlignedBB) para detección y resolución
 * continua de colisiones mediante volumen de barrido con Axis Clipping.
 *
 * ── Arquitectura Minecraft-style ──────────────────────────────────────────
 *
 * PRINCIPIO FUNDAMENTAL:
 *   La física de Minecraft NO usa swept AABB tradicional con cálculo de time.
 *   Usa AXIS CLIPPING: cada eje recorta la velocidad independientemente para
 *   evitar penetración, sin calcular tiempos de contacto.
 *
 * ORDEN ESTRICTO DE RESOLUCIÓN:
 *   1. Aplicar gravedad y fuerzas → actualizar velocidad
 *   2. Crear broadphase box con expandTowards(vx, vy)
 *   3. Recolectar bloques sólidos que intersecten broadphase
 *   4. FASE Y: recortar vy con calculateYOffset, luego move(0, vy)
 *   5. FASE X: recortar vx con calculateXOffset, luego move(vx, 0)
 *   6. Detectar estado: onGround si originalVy != vy && originalVy < 0
 *
 * VENTAJAS SOBRE SWEPT AABB:
 *   - Más simple: no requiere calcular tiempos de contacto
 *   - Más robusto: no sufre problemas numéricos de división por velocidad
 *   - Más eficiente: no necesita iterar múltiples veces
 *   - Determinista: orden fijo produce siempre el mismo resultado
 *   - Permite deslizamiento natural: si X colisiona, Y sigue moviéndose
 *
 * DIFERENCIAS CON RectangleH:
 *   - AABB usa coordenadas min/max (como Minecraft)
 *   - RectangleH usa x/y/width/height (como AWT)
 *   - AABB está diseñado para axis clipping
 *   - RectangleH está diseñado para swept AABB y queries generales
 *
 * CUÁNDO USAR AABB vs RectangleH:
 *   - AABB: física de entidades tipo platformer (Player, Enemies)
 *   - RectangleH: proyectiles y queries geométricas generales
 */
public final class AABB {

    /** Coordenada mínima en X (borde izquierdo) */
    public double minX;
    
    /** Coordenada mínima en Y (borde superior) */
    public double minY;
    
    /** Coordenada máxima en X (borde derecho) */
    public double maxX;
    
    /** Coordenada máxima en Y (borde inferior) */
    public double maxY;

    // ══════════════════════════════════════════════════════════════════════
    // ══ CONSTRUCTORES
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Constructor principal: coordenadas min/max.
     * NO valida que min < max - permite AABBs invertidos para casos especiales.
     */
    public AABB(double minX, double minY, double maxX, double maxY) {
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
    }

    /**
     * Constructor desde posición + tamaño (equivalente a x/y/width/height).
     */
    public static AABB fromPositionAndSize(double x, double y, double width, double height) {
        return new AABB(x, y, x + width, y + height);
    }

    /**
     * Constructor desde centro + half-extents (para objetos simétricos).
     */
    public static AABB fromCenter(double centerX, double centerY, double halfWidth, double halfHeight) {
        return new AABB(
            centerX - halfWidth,
            centerY - halfHeight,
            centerX + halfWidth,
            centerY + halfHeight
        );
    }

    /**
     * Copia este AABB.
     */
    public AABB copy() {
        return new AABB(minX, minY, maxX, maxY);
    }

    // ══════════════════════════════════════════════════════════════════════
    // ══ QUERIES GEOMÉTRICAS
    // ══════════════════════════════════════════════════════════════════════

    public double getWidth() { return maxX - minX; }
    public double getHeight() { return maxY - minY; }
    public double getCenterX() { return (minX + maxX) * 0.5; }
    public double getCenterY() { return (minY + maxY) * 0.5; }

    /**
     * Test de intersección AABB estricto.
     * Retorna true si hay solapamiento real (no solo contacto de borde).
     */
    public boolean intersects(AABB other) {
        return this.minX < other.maxX
            && this.maxX > other.minX
            && this.minY < other.maxY
            && this.maxY > other.minY;
    }

    /**
     * Test de contención: retorna true si este AABB contiene completamente otro.
     */
    public boolean contains(AABB other) {
        return other.minX >= this.minX
            && other.maxX <= this.maxX
            && other.minY >= this.minY
            && other.maxY <= this.maxY;
    }

    /**
     * Test de contención de punto.
     */
    public boolean contains(double x, double y) {
        return x >= this.minX && x < this.maxX
            && y >= this.minY && y < this.maxY;
    }

    // ══════════════════════════════════════════════════════════════════════
    // ══ BROADPHASE: Construcción del volumen de barrido
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Crea un AABB de barrido (broadphase box) expandiendo este AABB según
     * el vector de velocidad deseado.
     *
     * ── Propósito ───────────────────────────────────────────────────────────
     * El broadphase box contiene TODAS las posiciones que el objeto puede
     * ocupar si se mueve con velocidad (vx, vy) en este frame. Se usa para
     * filtrar rápidamente qué bloques necesitan ser testeados:
     *
     *   - Si un bloque NO intersecta broadphase → imposible colisión
     *   - Si un bloque SÍ intersecta broadphase → testear con calculateOffset
     *
     * ── Comportamiento ─────────────────────────────────────────────────────
     * - Si vx > 0: expande hacia la derecha (maxX += vx)
     * - Si vx < 0: expande hacia la izquierda (minX += vx)
     * - Si vy > 0: expande hacia abajo (maxY += vy)
     * - Si vy < 0: expande hacia arriba (minY += vy)
     *
     * ── Mini-HRFC 1.5: Parámetros como displacement ────────────────────────
     *
     * @param vx desplazamiento horizontal deseado [units], NO velocidad
     * @param vy desplazamiento vertical deseado [units], NO velocidad
     * @return nuevo AABB que contiene todo el path del movimiento
     *
     * IMPORTANTE: vx y vy deben calcularse como velocity × deltaTime antes
     * de llamar a este método.
     */
    public AABB expandTowards(double vx, double vy) {
        double newMinX = this.minX;
        double newMinY = this.minY;
        double newMaxX = this.maxX;
        double newMaxY = this.maxY;

        // Expandir en X según dirección de movimiento
        if (vx < 0.0) {
            newMinX += vx;  // Expandir hacia la izquierda
        } else if (vx > 0.0) {
            newMaxX += vx;  // Expandir hacia la derecha
        }

        // Expandir en Y según dirección de movimiento
        if (vy < 0.0) {
            newMinY += vy;  // Expandir hacia arriba
        } else if (vy > 0.0) {
            newMaxY += vy;  // Expandir hacia abajo
        }

        return new AABB(newMinX, newMinY, newMaxX, newMaxY);
    }

    // ══════════════════════════════════════════════════════════════════════
    // ══ AXIS CLIPPING: Resolución por eje independiente
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Calcula el offset recortado en el eje Y para evitar penetración.
     *
     * ── Algoritmo Minecraft ─────────────────────────────────────────────────
     *
     * PRE-CONDICIÓN:
     *   Este AABB y 'other' DEBEN estar alineados en X (intersección horizontal).
     *   Si no hay overlap en X, este método retorna vy sin cambios.
     *
     * LÓGICA:
     *   1. Verificar alineación en X (overlap horizontal)
     *   2. Si vy > 0 (bajando): calcular distancia al borde superior de other
     *      → si estamos arriba de other, recortar vy para detener justo antes
     *   3. Si vy < 0 (subiendo): calcular distancia al borde inferior de other
     *      → si estamos abajo de other, recortar vy para detener justo antes
     *   4. Si vy = 0 o no aplica, retornar vy sin cambios
     *
     * CASOS DE USO:
     *   - Detectar piso: vy recortado < vy original → contacto con suelo
     *   - Detectar techo: vy recortado > vy original → contacto con techo
     *   - Sin colisión: vy recortado == vy original → movimiento libre
     *
     * ── Mini-HRFC 1.5: Parámetros como displacement ────────────────────────
     *
     * @param other el obstáculo estático (bloque/pared)
     * @param vy    desplazamiento vertical deseado [units], NO velocidad
     * @return vy recortado para evitar penetración (puede ser igual al original)
     */
    public double calculateYOffset(AABB other, double vy) {
        // Pre-condición: debe haber overlap en X para que haya colisión posible
        if (other.maxX <= this.minX || other.minX >= this.maxX) {
            return vy;  // Sin overlap horizontal → sin colisión en Y
        }

        // CASO 1: Movimiento hacia abajo (vy > 0) → detectar piso
        if (vy > 0.0 && this.maxY <= other.minY) {
            // Estamos arriba del bloque y bajando hacia él
            double distancia = other.minY - this.maxY;
            if (distancia < vy) {
                // Colisionaríamos si nos movemos vy completo
                return distancia;  // Recortar para detener justo en el borde
            }
        }

        // CASO 2: Movimiento hacia arriba (vy < 0) → detectar techo
        if (vy < 0.0 && this.minY >= other.maxY) {
            // Estamos abajo del bloque y subiendo hacia él
            double distancia = other.maxY - this.minY;
            if (distancia > vy) {
                // Colisionaríamos si nos movemos vy completo
                return distancia;  // Recortar para detener justo en el borde
            }
        }

        // Sin colisión o ya penetrados → retornar vy original
        return vy;
    }

    /**
     * Calcula el offset recortado en el eje X para evitar penetración.
     *
     * ── Algoritmo Minecraft ─────────────────────────────────────────────────
     *
     * PRE-CONDICIÓN:
     *   Este AABB y 'other' DEBEN estar alineados en Y (intersección vertical).
     *   Si no hay overlap en Y, este método retorna vx sin cambios.
     *
     * LÓGICA:
     *   1. Verificar alineación en Y (overlap vertical)
     *   2. Si vx > 0 (derecha): calcular distancia al borde izquierdo de other
     *      → si estamos a la izquierda de other, recortar vx para detener justo antes
     *   3. Si vx < 0 (izquierda): calcular distancia al borde derecho de other
     *      → si estamos a la derecha de other, recortar vx para detener justo antes
     *   4. Si vx = 0 o no aplica, retornar vx sin cambios
     *
     * CASOS DE USO:
     *   - Detectar pared: vx recortado < |vx original| → contacto con pared
     *   - Sin colisión: vx recortado == vx original → movimiento libre
     *
     * ── Mini-HRFC 1.5: Parámetros como displacement ────────────────────────
     *
     * @param other el obstáculo estático (bloque/pared)
     * @param vx    desplazamiento horizontal deseado [units], NO velocidad
     * @return vx recortado para evitar penetración (puede ser igual al original)
     */
    public double calculateXOffset(AABB other, double vx) {
        // Pre-condición: debe haber overlap en Y para que haya colisión posible
        if (other.maxY <= this.minY || other.minY >= this.maxY) {
            return vx;  // Sin overlap vertical → sin colisión en X
        }

        // CASO 1: Movimiento hacia la derecha (vx > 0) → detectar pared derecha
        if (vx > 0.0 && this.maxX <= other.minX) {
            // Estamos a la izquierda del bloque y moviendo hacia él
            double distancia = other.minX - this.maxX;
            if (distancia < vx) {
                // Colisionaríamos si nos movemos vx completo
                return distancia;  // Recortar para detener justo en el borde
            }
        }

        // CASO 2: Movimiento hacia la izquierda (vx < 0) → detectar pared izquierda
        if (vx < 0.0 && this.minX >= other.maxX) {
            // Estamos a la derecha del bloque y moviendo hacia él
            double distancia = other.maxX - this.minX;
            if (distancia > vx) {
                // Colisionaríamos si nos movemos vx completo
                return distancia;  // Recortar para detener justo en el borde
            }
        }

        // Sin colisión o ya penetrados → retornar vx original
        return vx;
    }

    // ══════════════════════════════════════════════════════════════════════
    // ══ MOVIMIENTO: Aplicar desplazamiento ya recortado
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Mueve este AABB por el desplazamiento especificado.
     * Modifica las coordenadas min/max in-place.
     *
     * IMPORTANTE: Este método debe llamarse DESPUÉS de recortar el desplazamiento
     * con calculateXOffset/calculateYOffset. NO aplica detección de colisiones.
     *
     * @param dx desplazamiento en X (ya recortado)
     * @param dy desplazamiento en Y (ya recortado)
     */
    public void move(double dx, double dy) {
        this.minX += dx;
        this.maxX += dx;
        this.minY += dy;
        this.maxY += dy;
    }

    /**
     * Retorna un nuevo AABB movido por el desplazamiento (versión inmutable).
     */
    public AABB offset(double dx, double dy) {
        return new AABB(minX + dx, minY + dy, maxX + dx, maxY + dy);
    }

    /**
     * Establece la posición de este AABB manteniendo su tamaño.
     * Útil para sincronizar con Transform.position.
     */
    public void setPosition(double x, double y) {
        double width = this.maxX - this.minX;
        double height = this.maxY - this.minY;
        this.minX = x;
        this.minY = y;
        this.maxX = x + width;
        this.maxY = y + height;
    }

    // ══════════════════════════════════════════════════════════════════════
    // ══ CONVERSIÓN Y UTILIDADES
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Convierte este AABB a RectangleH para compatibilidad con código existente.
     */
    public RectangleH toRectangleH() {
        return new RectangleH(minX, minY, maxX - minX, maxY - minY);
    }

    /**
     * Crea un AABB desde RectangleH.
     */
    public static AABB fromRectangleH(RectangleH rect) {
        return new AABB(rect.x, rect.y, rect.x + rect.width, rect.y + rect.height);
    }

    /**
     * Expande este AABB por un margen en todas direcciones (in-place).
     * Útil para crear broadphase con margen de seguridad.
     */
    public void expand(double margin) {
        this.minX -= margin;
        this.minY -= margin;
        this.maxX += margin;
        this.maxY += margin;
    }

    /**
     * Retorna un nuevo AABB expandido (versión inmutable).
     */
    public AABB expanded(double margin) {
        return new AABB(
            minX - margin,
            minY - margin,
            maxX + margin,
            maxY + margin
        );
    }

    @Override
    public String toString() {
        return String.format("AABB[%.2f, %.2f → %.2f, %.2f]", minX, minY, maxX, maxY);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof AABB)) return false;
        AABB other = (AABB) obj;
        return Double.compare(minX, other.minX) == 0
            && Double.compare(minY, other.minY) == 0
            && Double.compare(maxX, other.maxX) == 0
            && Double.compare(maxY, other.maxY) == 0;
    }

    @Override
    public int hashCode() {
        long bits = Double.doubleToLongBits(minX);
        bits ^= Double.doubleToLongBits(minY) * 31;
        bits ^= Double.doubleToLongBits(maxX) * 31;
        bits ^= Double.doubleToLongBits(maxY) * 31;
        return (int) (bits ^ (bits >>> 32));
    }
}
