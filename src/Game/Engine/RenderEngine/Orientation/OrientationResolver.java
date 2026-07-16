package Game.Engine.RenderEngine.Orientation;

/**
 * OrientationResolver — calcula la FacingDirection de una entidad.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * Dada la posición de la entidad y la posición de la cámara (o del objetivo
 * al que mira la entidad), determina qué dirección cardinal discreta
 * corresponde al ángulo entre ellos.
 *
 * El Gameplay solo dice "este sprite mira hacia este ángulo / posición".
 * El RenderEngine decide qué FacingDirection usar según el DirectionCount.
 *
 * ── DOS MODOS ─────────────────────────────────────────────────────────────
 * 1. RELATIVO A CÁMARA (pseudo-3D clásico):
 *    La dirección se calcula entre la posición de la entidad y la posición
 *    de la cámara. Así, una entidad "mirando al norte" muestra su frente
 *    cuando la cámara está al norte.
 *
 *    Uso: resolver(entityX, entityY, cameraX, cameraY, FOUR)
 *
 * 2. RELATIVO A OBJETIVO (mira al cursor, a un enemigo):
 *    La dirección se calcula entre la entidad y el objetivo al que apunta.
 *
 *    Uso: resolveToward(entityX, entityY, targetX, targetY, EIGHT)
 *
 * ── CONVENCIÓN DE ÁNGULOS ─────────────────────────────────────────────────
 * Usa Math.atan2(dy, dx) en el sistema de coordenadas de mundo (Y crece
 * hacia abajo en pantalla). Los sectores angulares se calculan con ese
 * sistema.
 *
 *   angle ≈  0°      → EAST
 *   angle ≈  90°     → SOUTH  (Y crece hacia abajo)
 *   angle ≈  180°    → WEST
 *   angle ≈  270°    → NORTH
 *
 * ── FLIP AUTOMÁTICO ───────────────────────────────────────────────────────
 * resolveWithFlip() devuelve un par (FacingDirection, flipH).
 * Si el asset de WEST no existe pero EAST sí, devuelve (EAST, flipH=true).
 * Esto reduce a la mitad los sprites necesarios para un juego 2D lateral.
 *
 * ── INSTANCIA ─────────────────────────────────────────────────────────────
 * Clase utilitaria estática. No instanciar.
 */
public final class OrientationResolver {

    // No instanciar.
    private OrientationResolver() {}

    // ── Resolución relativa a cámara ──────────────────────────────────────────

    /**
     * Calcula la FacingDirection de una entidad relativa a la posición de la cámara.
     *
     * Semántica: "¿qué cara de la entidad ve la cámara?"
     *
     * @param entityX     posición X de la entidad en el mundo
     * @param entityY     posición Y de la entidad en el mundo
     * @param cameraX     posición X de la cámara (centro del viewport)
     * @param cameraY     posición Y de la cámara (centro del viewport)
     * @param directions  granularidad de direcciones
     * @return dirección discreta de la entidad relativa a la cámara
     */
    public static FacingDirection resolve(double entityX,  double entityY,
                                          double cameraX,  double cameraY,
                                          DirectionCount directions) {
        // Vector de la entidad a la cámara
        double dx = cameraX - entityX;
        double dy = cameraY - entityY;
        return fromVector(dx, dy, directions);
    }

    // ── Resolución hacia objetivo ─────────────────────────────────────────────

    /**
     * Calcula la FacingDirection de una entidad apuntando hacia un objetivo.
     *
     * Semántica: "¿en qué dirección mira la entidad?"
     *
     * @param entityX  posición X de la entidad
     * @param entityY  posición Y de la entidad
     * @param targetX  posición X del objetivo
     * @param targetY  posición Y del objetivo
     * @param directions granularidad
     */
    public static FacingDirection resolveToward(double entityX, double entityY,
                                                double targetX, double targetY,
                                                DirectionCount directions) {
        double dx = targetX - entityX;
        double dy = targetY - entityY;
        return fromVector(dx, dy, directions);
    }

    // ── Resolución desde ángulo ───────────────────────────────────────────────

    /**
     * Calcula la FacingDirection desde un ángulo en radianes.
     *
     * @param angleRadians ángulo en radianes (convenio Math.atan2: 0=EAST, π/2=SOUTH)
     * @param directions   granularidad
     */
    public static FacingDirection fromAngle(double angleRadians, DirectionCount directions) {
        // Normalizar a [0, 2π)
        double angle = ((angleRadians % (2 * Math.PI)) + 2 * Math.PI) % (2 * Math.PI);

        return switch (directions) {
            case FOUR   -> fromAngleFour(angle);
            case EIGHT  -> fromAngleEight(angle);
            case SIXTEEN -> fromAngleEight(angle); // aproximar a 8 hasta tener 16 assets
        };
    }

    // ── Resultado con flip ────────────────────────────────────────────────────

    /**
     * Resultado de resolución que incluye si se debe aplicar flip horizontal.
     *
     * @param direction la FacingDirection resuelta (puede ser el espejo)
     * @param flipH     true si se debe aplicar flip horizontal para simular la dirección original
     */
    public record ResolutionResult(FacingDirection direction, boolean flipH) {}

    /**
     * Resuelve la dirección y, si la dirección resultante es una que
     * se puede simular con flip (WEST → EAST+flipH), lo indica.
     *
     * Útil para reducir la cantidad de sprites necesarios.
     *
     * @param angleRadians ángulo de orientación en radianes
     * @param directions   granularidad
     * @param hasWestSprite true si el asset tiene sprites para WEST/mirrored
     * @return resultado con dirección y flag de flip
     */
    public static ResolutionResult resolveWithFlip(double angleRadians,
                                                   DirectionCount directions,
                                                   boolean hasWestSprite) {
        FacingDirection dir = fromAngle(angleRadians, directions);

        // Si la dirección necesita flip y no hay sprite propio, usar el espejo
        if (!hasWestSprite && dir.isMirrorable()) {
            FacingDirection counterpart = dir.flippedCounterpart();
            if (counterpart != null) {
                return new ResolutionResult(counterpart, true);
            }
        }

        return new ResolutionResult(dir, false);
    }

    /**
     * Construye la clave de animación para una base + dirección.
     *
     * @param baseKey    clave base (ej: "walk", "idle")
     * @param direction  dirección resuelta
     * @return clave compuesta (ej: "walk_south")
     */
    public static String animationKey(String baseKey, FacingDirection direction) {
        return direction.toAnimationKey(baseKey);
    }

    // ── Implementación interna ────────────────────────────────────────────────

    private static FacingDirection fromVector(double dx, double dy, DirectionCount directions) {
        if (dx == 0 && dy == 0) return FacingDirection.SOUTH; // default si no hay diferencia
        double angle = Math.atan2(dy, dx);
        // Normalizar a [0, 2π)
        angle = ((angle % (2 * Math.PI)) + 2 * Math.PI) % (2 * Math.PI);

        return switch (directions) {
            case FOUR   -> fromAngleFour(angle);
            case EIGHT  -> fromAngleEight(angle);
            case SIXTEEN -> fromAngleEight(angle); // aproximar a 8 por ahora
        };
    }

    /**
     * Mapea un ángulo normalizado [0, 2π) a 4 direcciones.
     *
     * Sectores (Y crece hacia abajo):
     *   [315°, 45°)  → EAST
     *   [45°,  135°) → SOUTH
     *   [135°, 225°) → WEST
     *   [225°, 315°) → NORTH
     */
    private static FacingDirection fromAngleFour(double angle) {
        double deg = Math.toDegrees(angle);

        if (deg >= 315.0 || deg < 45.0)  return FacingDirection.EAST;
        if (deg >= 45.0  && deg < 135.0) return FacingDirection.SOUTH;
        if (deg >= 135.0 && deg < 225.0) return FacingDirection.WEST;
        return FacingDirection.NORTH;
    }

    /**
     * Mapea un ángulo normalizado [0, 2π) a 8 direcciones.
     *
     * Sectores de 45° empezando en EAST:
     *   [337.5°, 22.5°)  → EAST
     *   [22.5°,  67.5°)  → SOUTH_EAST
     *   [67.5°,  112.5°) → SOUTH
     *   [112.5°, 157.5°) → SOUTH_WEST
     *   [157.5°, 202.5°) → WEST
     *   [202.5°, 247.5°) → NORTH_WEST
     *   [247.5°, 292.5°) → NORTH
     *   [292.5°, 337.5°) → NORTH_EAST
     */
    private static FacingDirection fromAngleEight(double angle) {
        double deg = Math.toDegrees(angle);

        if (deg >= 337.5 || deg < 22.5)   return FacingDirection.EAST;
        if (deg >= 22.5  && deg < 67.5)   return FacingDirection.SOUTH_EAST;
        if (deg >= 67.5  && deg < 112.5)  return FacingDirection.SOUTH;
        if (deg >= 112.5 && deg < 157.5)  return FacingDirection.SOUTH_WEST;
        if (deg >= 157.5 && deg < 202.5)  return FacingDirection.WEST;
        if (deg >= 202.5 && deg < 247.5)  return FacingDirection.NORTH_WEST;
        if (deg >= 247.5 && deg < 292.5)  return FacingDirection.NORTH;
        return FacingDirection.NORTH_EAST;
    }
}
