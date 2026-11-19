package Game.Engine.Colisions;

/**
 * Constantes centralizadas para el sistema de colisiones.
 * 
 * ── HRFC — Collision System Refactor ──────────────────────────────────────
 * 
 * PROPÓSITO:
 *   Eliminar magic numbers dispersos en el código y centralizar configuración
 *   de precisión numérica para garantizar consistencia.
 * 
 * MEJORA:
 *   Antes: 1e-6, 1e-8, -1, 0, 1 dispersos sin significado claro
 *   Ahora: Constantes con nombres descriptivos y documentación
 */
public final class CollisionConstants {
    
    private CollisionConstants() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    // ── Precisión numérica ─────────────────────────────────────────────────
    
    /**
     * Epsilon para comparaciones de desplazamiento/velocidad.
     * Usado para detectar si hubo cambio en la velocidad por colisión.
     * 
     * Valor: 1e-6 (0.000001 units) — suficiente para detectar colisiones
     * sin ser tan estricto que cause false negatives por floating point error.
     */
    public static final double VELOCITY_EPSILON = 1e-6;
    
    /**
     * Epsilon para tie-breaking en esquinas (penetración igual en X e Y).
     * Debe ser más estricto que VELOCITY_EPSILON para detectar casos edge.
     * 
     * Valor: 1e-8 (0.00000001 units) — detecta esquinas reales vs colisiones
     * que parecen esquinas por error numérico.
     */
    public static final double CORNER_TIE_EPSILON = 1e-8;
    
    // ── Normales de contacto ───────────────────────────────────────────────
    
    /** Normal apuntando hacia arriba (contacto con piso). */
    public static final int NORMAL_UP = -1;
    
    /** Sin normal (no hubo colisión en este eje). */
    public static final int NORMAL_NONE = 0;
    
    /** Normal apuntando hacia abajo (contacto con techo). */
    public static final int NORMAL_DOWN = 1;
    
    /** Normal apuntando hacia la izquierda (contacto con pared derecha). */
    public static final int NORMAL_LEFT = -1;
    
    /** Normal apuntando hacia la derecha (contacto con pared izquierda). */
    public static final int NORMAL_RIGHT = 1;
    
    // ── Configuración de spatial hash ──────────────────────────────────────
    
    /**
     * Tamaño mínimo de celda para spatial hash (bullets pequeños).
     * No tiene sentido usar celdas más pequeñas que el objeto más pequeño.
     */
    public static final int MIN_CELL_SIZE = 64;
    
    /**
     * Tamaño máximo de celda para spatial hash (bosses grandes).
     * Celdas muy grandes pierden eficiencia del spatial partitioning.
     */
    public static final int MAX_CELL_SIZE = 256;
    
    /**
     * Tamaño default de celda cuando no se puede calcular adaptativo.
     * Valor balanceado para mayoría de objetos (players, enemies, items).
     */
    public static final int DEFAULT_CELL_SIZE = 128;
    
    // ── Capacidades iniciales de colecciones ───────────────────────────────
    
    /**
     * Capacidad inicial para mapa de contactos por frame.
     * Estimado conservador: ~50 objetos físicos activos simultáneos.
     */
    public static final int INITIAL_CONTACT_MAP_CAPACITY = 64;
    
    /**
     * Capacidad inicial para lista de contactos de un objeto.
     * Típicamente un objeto toca 1-4 cosas (piso, 1-2 paredes, techo).
     */
    public static final int INITIAL_CONTACT_LIST_CAPACITY = 4;
    
    /**
     * Capacidad inicial para lista de CollisionHandler listeners.
     * Mayoría de objetos tiene 0-2 listeners (ej: sensor + behavior).
     */
    public static final int INITIAL_LISTENER_LIST_CAPACITY = 2;
}
