package Game.Items.Types.Bullets;

import Game.Engine.GameMath.Logic2D.Vector2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Predictor de trayectoria de proyectil — calcula la trayectoria estimada
 * que seguirá un proyectil según su comportamiento físico.
 *
 * ── SEPARACIÓN DE RESPONSABILIDADES ──────────────────────────────────────
 *
 * La UI NO debe implementar la física del proyectil.
 * Si MetheorBullet tiene un comportamiento determinado, la UI no debería
 * tener un if (isMeteor) ...
 *
 * Debe existir una forma de preguntar:
 * "Dame una representación predictiva de cómo se comportaría este proyectil."
 *
 * ── ARQUITECTURA ──────────────────────────────────────────────────────────
 *
 *   BulletBehavior (MetheorBullet, BulletNormal, etc.)
 *          │
 *          └── define su comportamiento físico (gravedad, aceleración, homing)
 *
 *   ProjectileTrajectoryPredictor
 *          │
 *          └── simula frame-by-frame la física del proyectil
 *          └── retorna TrajectoryPrediction (lista de puntos)
 *
 *   AimVisualizationCapability (TrajectoryVisualizationCapability)
 *          │
 *          └── consulta ProjectileTrajectoryPredictor
 *          └── renderiza los puntos en la UI
 *
 * ── USO ───────────────────────────────────────────────────────────────────
 *
 *   TrajectoryPrediction prediction = ProjectileTrajectoryPredictor.predict(
 *       spawnPosition,
 *       aimDirection,
 *       preview.speed(),
 *       preview.lifeTime(),
 *       preview.hasGravity() ? 0.4 : 0.0,
 *       0.0  // drag (pendiente de implementar)
 *   );
 *
 *   for (Vector2D point : prediction.getPoints()) {
 *       g.fillOval((int)point.getX(), (int)point.getY(), 4, 4);
 *   }
 *
 * ── EXTENSIBILIDAD ────────────────────────────────────────────────────────
 *
 * Futuras extensiones sin cambiar la UI:
 *   - Homing: curvas que siguen al target más cercano
 *   - Rebote: reflexión en superficies
 *   - Aceleración: cambio de velocidad en el tiempo
 *   - Curvatura: trayectorias parabólicas no simétricas
 */
public final class ProjectileTrajectoryPredictor {

    /**
     * Predice la trayectoria de un proyectil con física simple (lineal + gravedad).
     *
     * @param spawnPosition posición inicial del proyectil (mundo)
     * @param direction     dirección normalizada del disparo
     * @param speed         velocidad del proyectil (unidades/frame)
     * @param lifeTime      frames de vida máximos
     * @param gravity       aceleración gravitacional por frame (ej: 0.4)
     * @param drag          coeficiente de arrastre (0.0 = sin arrastre)
     * @return predicción de trayectoria
     */
    public static TrajectoryPrediction predict(
            Vector2D spawnPosition,
            Vector2D direction,
            double speed,
            int lifeTime,
            double gravity,
            double drag) {

        List<Vector2D> points = new ArrayList<>(lifeTime);

        double velX = direction.getX() * speed;
        double velY = direction.getY() * speed;

        double px = spawnPosition.getX();
        double py = spawnPosition.getY();
        double vy = velY;

        for (int i = 0; i < lifeTime; i++) {
            points.add(new Vector2D(px, py));

            // Actualizar posición
            px += velX;
            py += vy;

            // Aplicar gravedad
            if (gravity != 0.0) {
                vy += gravity;
            }

            // Aplicar drag (simplificado)
            if (drag != 0.0) {
                velX *= (1.0 - drag);
                vy *= (1.0 - drag);
            }
        }

        return new TrajectoryPrediction(
                Collections.unmodifiableList(points),
                TrajectoryType.LINEAR_GRAVITY
        );
    }

    /**
     * Predice la trayectoria de un proyectil con homing hacia un target.
     * Pendiente de implementación completa.
     *
     * @param spawnPosition posición inicial del proyectil (mundo)
     * @param direction     dirección inicial del disparo
     * @param speed         velocidad del proyectil
     * @param lifeTime      frames de vida máximos
     * @param targetPosition posición del target (puede ser null si no hay target)
     * @param homingStrength fuerza de homing (0.0-1.0)
     * @return predicción de trayectoria con curvas de homing
     */
    public static TrajectoryPrediction predictHoming(
            Vector2D spawnPosition,
            Vector2D direction,
            double speed,
            int lifeTime,
            Vector2D targetPosition,
            double homingStrength) {

        // TODO: Implementar simulación de homing
        // Por ahora, fallback a trayectoria lineal
        return predict(spawnPosition, direction, speed, lifeTime, 0.0, 0.0);
    }

    /**
     * Predicción de trayectoria — resultado inmutable con lista de puntos.
     *
     * @param points lista inmutable de posiciones (coordenadas de mundo)
     * @param type   tipo de trayectoria (para renderizado diferenciado)
     */
    public record TrajectoryPrediction(
            List<Vector2D> points,
            TrajectoryType type
    ) {
        public TrajectoryPrediction {
            if (points == null) {
                throw new IllegalArgumentException("points no puede ser null");
            }
            if (type == null) {
                throw new IllegalArgumentException("type no puede ser null");
            }
        }

        /**
         * Total de puntos en la trayectoria.
         */
        public int size() {
            return points.size();
        }

        /**
         * True si la trayectoria está vacía.
         */
        public boolean isEmpty() {
            return points.isEmpty();
        }
    }

    /**
     * Tipo de trayectoria — determina el estilo de renderizado en la UI.
     */
    public enum TrajectoryType {
        /** Trayectoria lineal con gravedad (parábola) */
        LINEAR_GRAVITY,

        /** Trayectoria con homing (curvas suaves) */
        HOMING,

        /** Trayectoria con rebotes (líneas rectas con ángulos) */
        BOUNCING,

        /** Trayectoria acelerada (parábola asimétrica) */
        ACCELERATING
    }
}
