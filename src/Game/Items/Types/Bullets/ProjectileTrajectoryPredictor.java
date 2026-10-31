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
     *
     * Simula el comportamiento de HomingMovement: en cada frame, el proyectil
     * rota su vector de velocidad hacia el target, limitado por turnSpeed.
     * Esto produce una curva suave que converge gradualmente hacia el objetivo.
     *
     * IMPORTANTE: Esta simulación asume que el target está estático.
     * Un target en movimiento produciría una trayectoria diferente en tiempo real.
     *
     * @param spawnPosition posición inicial del proyectil (mundo)
     * @param direction     dirección inicial del disparo (normalizada)
     * @param speed         velocidad del proyectil (unidades/segundo)
     * @param lifeTime      tiempo de vida máximo en segundos
     * @param targetPosition posición del target (puede ser null si no hay target)
     * @param turnSpeedDeg   velocidad de giro en grados/frame @ 30 FPS
     * @return predicción de trayectoria con curvas de homing
     */
    public static TrajectoryPrediction predictHoming(
            Vector2D spawnPosition,
            Vector2D direction,
            double speed,
            double lifeTime,
            Vector2D targetPosition,
            double turnSpeedDeg) {

        // Si no hay target, fallback a trayectoria lineal
        if (targetPosition == null) {
            int frames = (int)(lifeTime * 30.0); // convertir a frames @ 30 FPS
            return predict(spawnPosition, direction, speed, frames, 0.0, 0.0);
        }

        List<Vector2D> points = new ArrayList<>();
        
        // Convertir turnSpeed de grados a radianes
        double turnSpeedRad = Math.toRadians(turnSpeedDeg);
        
        // Estado inicial del proyectil
        double px = spawnPosition.getX();
        double py = spawnPosition.getY();
        double currentAngle = Math.atan2(direction.getY(), direction.getX());
        
        // Calcular número de frames @ 30 FPS legacy
        int frames = (int)(lifeTime * 30.0);
        double deltaTime = 1.0 / 30.0; // frame time @ 30 FPS
        
        for (int i = 0; i < frames; i++) {
            points.add(new Vector2D(px, py));
            
            // Calcular dirección al target
            double dx = targetPosition.getX() - px;
            double dy = targetPosition.getY() - py;
            double dist = Math.hypot(dx, dy);
            
            // Si está muy cerca del target, continuar en línea recta
            if (dist < 1e-6) {
                px += Math.cos(currentAngle) * speed * deltaTime;
                py += Math.sin(currentAngle) * speed * deltaTime;
                continue;
            }
            
            // Ángulo objetivo
            double targetAngle = Math.atan2(dy, dx);
            
            // Diferencia angular más corta (entre -π y π)
            double diff = targetAngle - currentAngle;
            while (diff >  Math.PI) diff -= 2 * Math.PI;
            while (diff < -Math.PI) diff += 2 * Math.PI;
            
            // Limitar el giro por frame (simulando HomingMovement.tick)
            double rotation = Math.copySign(
                Math.min(Math.abs(diff), turnSpeedRad), 
                diff
            );
            currentAngle += rotation;
            
            // Actualizar posición con nueva dirección
            px += Math.cos(currentAngle) * speed * deltaTime;
            py += Math.sin(currentAngle) * speed * deltaTime;
        }

        return new TrajectoryPrediction(
                Collections.unmodifiableList(points),
                TrajectoryType.HOMING
        );
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
