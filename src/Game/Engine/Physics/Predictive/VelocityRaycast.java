package Game.Engine.Physics.Predictive;

import Game.Engine.GameMath.Logic2D.RectangleH;
import Game.Engine.GameMath.Logic2D.Vector2D;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * VelocityRaycast — Raycast Predictivo que Limita Movimiento
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * PRINCIPIO CORRECTO:
 *   El raycast NO es solo para "predecir", es para **LIMITAR EL MOVIMIENTO**.
 *   
 *   Funciona como un "margen de frenado":
 *   - Velocidad baja → raycast corto → puede moverse sin restricción
 *   - Velocidad alta → raycast largo → detecta obstáculos antes y DETIENE
 *
 * ALGORITMO:
 *   1. Calcular raycast proporcional a velocidad
 *   2. Si el raycast toca un obstáculo a distancia D
 *   3. LIMITAR el movimiento a máximo D (no puede ir más allá)
 *   4. El objeto se DETIENE justo antes del obstáculo
 *
 * CONCEPTO VISUAL:
 *   [Bala]----raycast---->|Muro|
 *          ^
 *          |
 *   Movimiento máximo = distancia al muro
 *
 * USO:
 *   1. Calcular raycast
 *   2. Detectar obstáculo más cercano
 *   3. Si distancia < desplazamiento planeado → LIMITAR desplazamiento
 *   4. Mover solo hasta el obstáculo
 *
 * ── PARÁMETROS DE CALIBRACIÓN ─────────────────────────────────────────────
 */
public class VelocityRaycast {
    
    // ═════════════════════════════════════════════════════════════════════
    // DEBUG SETTINGS (CONFIGURABLE)
    // ═════════════════════════════════════════════════════════════════════
    
    /**
     * Flag global para visualizar raycasts en pantalla.
     * Modificar para debug visual de raycasts.
     */
    public static boolean SHOW_RAYCASTS = false;

    // ═════════════════════════════════════════════════════════════════════
    // SECCIÓN 1: CONSTANTES DE CALIBRACIÓN
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Velocidad mínima para activar raycast (px/frame).
     * Por debajo de este umbral, NO se hace raycast (optimización).
     */
    private static final double MIN_VELOCITY_FOR_RAYCAST = 3.0;

    /**
     * Factor de proporcionalidad: raycast_length = velocity * RAYCAST_FACTOR.
     * 
     * NUEVO SIGNIFICADO:
     *   Factor que determina cuán "anticipado" debe ser el frenado.
     *   1.0 = raycast igual al desplazamiento del frame (detección justa)
     *   2.0 = raycast al doble (margen extra de seguridad)
     *
     * VALOR RECOMENDADO: 2.0
     *   - Da suficiente "tiempo de reacción" al sistema
     *   - Evita penetraciones incluso en frames irregulares
     */
    private static final double RAYCAST_FACTOR = 2.0;

    /**
     * Longitud máxima del raycast (px).
     * Limita el raycast para evitar cálculos excesivos en velocidades extremas.
     * AUMENTADO de 100 a 200 para velocidades muy altas.
     */
    private static final double MAX_RAYCAST_LENGTH = 200.0;

    /**
     * Longitud mínima del raycast cuando se activa (px).
     * Garantiza un mínimo de detección incluso en velocidades bajas.
     */
    private static final double MIN_RAYCAST_LENGTH = 8.0;

    // ═════════════════════════════════════════════════════════════════════
    // SECCIÓN 2: RESULTADO DEL RAYCAST
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Resultado de un raycast predictivo.
     */
    public static class RaycastResult {
        /** ¿Se debe realizar raycast? (false si velocidad < umbral) */
        public final boolean shouldCast;
        
        /** Longitud calculada del raycast (0 si shouldCast = false) */
        public final double rayLength;
        
        /** Dirección normalizada del raycast */
        public final Vector2D direction;
        
        /** Velocidad magnitude usada para el cálculo */
        public final double velocityMagnitude;

        private RaycastResult(boolean shouldCast, double rayLength, 
                             Vector2D direction, double velocityMagnitude) {
            this.shouldCast = shouldCast;
            this.rayLength = rayLength;
            this.direction = direction;
            this.velocityMagnitude = velocityMagnitude;
        }

        /** Constructor para caso de NO-CAST */
        private static RaycastResult noCast() {
            return new RaycastResult(false, 0.0, new Vector2D(0, 0), 0.0);
        }

        /** Constructor para caso de CAST */
        private static RaycastResult cast(double rayLength, Vector2D direction, 
                                         double velocityMagnitude) {
            return new RaycastResult(true, rayLength, direction, velocityMagnitude);
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // SECCIÓN 3: CÁLCULO DE RAYCAST
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Calcula los parámetros del raycast predictivo basado en velocidad.
     *
     * ALGORITMO:
     *   1. Calcular magnitud de velocidad
     *   2. Si < MIN_VELOCITY → NO raycast (optimización)
     *   3. Si >= MIN_VELOCITY:
     *      a. Calcular longitud proporcional (velocityMag * RAYCAST_FACTOR)
     *      b. Clampear entre MIN y MAX
     *      c. Normalizar dirección
     *
     * NOTA: Los parámetros se leen de RaycastConfig para facilitar tuning.
     *
     * @param vx velocidad en X (px/frame)
     * @param vy velocidad en Y (px/frame)
     * @return RaycastResult con parámetros calculados
     */
    public static RaycastResult calculateRaycast(double vx, double vy) {
        // Calcular magnitud de velocidad
        double velocityMag = Math.sqrt(vx * vx + vy * vy);

        // Optimización: velocidad muy baja → no raycast
        if (velocityMag < RaycastConfig.MIN_VELOCITY_FOR_RAYCAST) {
            return RaycastResult.noCast();
        }

        // Calcular longitud proporcional a velocidad
        double rawLength = velocityMag * RaycastConfig.RAYCAST_FACTOR;
        
        // Clampear entre min y max
        double rayLength = Math.max(RaycastConfig.MIN_RAYCAST_LENGTH, 
                                   Math.min(RaycastConfig.MAX_RAYCAST_LENGTH, rawLength));

        // Normalizar dirección
        Vector2D direction = new Vector2D(vx / velocityMag, vy / velocityMag);

        return RaycastResult.cast(rayLength, direction, velocityMag);
    }

    /**
     * Calcula el punto final del raycast desde una posición inicial.
     *
     * @param startX posición inicial X
     * @param startY posición inicial Y
     * @param result resultado de calculateRaycast()
     * @return Vector2D con el punto final, o null si shouldCast = false
     */
    public static Vector2D calculateRayEnd(double startX, double startY, 
                                           RaycastResult result) {
        if (!result.shouldCast) {
            return null;
        }

        double endX = startX + result.direction.getX() * result.rayLength;
        double endY = startY + result.direction.getY() * result.rayLength;
        
        return new Vector2D(endX, endY);
    }

    /**
     * Crea un bounds extendido en la dirección del raycast para broadphase.
     * 
     * UTILIDAD:
     *   Permite usar este bounds para filtrar rápidamente qué objetos
     *   podrían estar en el path del raycast, antes de hacer cálculos precisos.
     *
     * @param bounds bounds actual del objeto
     * @param result resultado de calculateRaycast()
     * @return RectangleH que cubre desde la posición actual hasta el final del ray
     */
    public static RectangleH createRaycastBounds(RectangleH bounds, 
                                                 RaycastResult result) {
        if (!result.shouldCast) {
            return bounds; // Sin extensión
        }

        double extendX = result.direction.getX() * result.rayLength;
        double extendY = result.direction.getY() * result.rayLength;

        // Crear bounds que engloba posición actual + extensión del ray
        double minX = extendX < 0 ? bounds.x + extendX : bounds.x;
        double minY = extendY < 0 ? bounds.y + extendY : bounds.y;
        double maxX = extendX > 0 ? bounds.x + bounds.width + extendX 
                                  : bounds.x + bounds.width;
        double maxY = extendY > 0 ? bounds.y + bounds.height + extendY 
                                  : bounds.y + bounds.height;

        return new RectangleH(minX, minY, maxX - minX, maxY - minY);
    }

    // ═════════════════════════════════════════════════════════════════════
    // SECCIÓN 4: UTILITIES
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Calcula el número de sub-steps necesarios basado en el raycast.
     *
     * PRINCIPIO:
     *   Si el raycast detecta obstáculo cercano, aumentar sub-steps
     *   para garantizar detección precisa.
     *
     * @param result resultado del raycast
     * @param defaultSteps sub-steps por defecto del sistema
     * @return número ajustado de sub-steps
     */
    public static int calculateAdjustedSubsteps(RaycastResult result, 
                                                int defaultSteps, 
                                                double maxStepSize) {
        if (!result.shouldCast) {
            return defaultSteps;
        }

        // Si la velocidad es alta, calcular sub-steps necesarios
        // para que ningún paso individual exceda maxStepSize
        int requiredSteps = (int) Math.ceil(result.velocityMagnitude / maxStepSize);
        
        return Math.max(defaultSteps, requiredSteps);
    }

    // ═════════════════════════════════════════════════════════════════════
    // SECCIÓN 5: DEBUG INFO
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Obtiene información de debug sobre los parámetros del raycast.
     */
    public static String getDebugInfo() {
        return RaycastConfig.getConfigInfo();
    }
}
