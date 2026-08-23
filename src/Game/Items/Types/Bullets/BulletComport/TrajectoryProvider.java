package Game.Items.Types.Bullets.BulletComport;

import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Items.Types.Bullets.ProjectileTrajectoryPredictor;

/**
 * Provider de trayectoria de proyectil — permite que cada BulletBehavior
 * exponga su propia lógica de predicción de trayectoria.
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
 *          └── implementa getTrajectoryProvider()
 *          └── retorna TrajectoryProvider que conoce su física
 *
 *   UI (TrajectoryVisualizationCapability)
 *          │
 *          └── llama behavior.getTrajectoryProvider().predict(...)
 *          └── renderiza los puntos sin conocer la física interna
 *
 * ── POLIMORFISMO ──────────────────────────────────────────────────────────
 *
 * Cada BulletBehavior puede:
 *   - Retornar DEFAULT_LINEAR_GRAVITY para física simple
 *   - Retornar un TrajectoryProvider custom con física compleja
 *   - Sobrescribir predict() para simular homing, rebotes, aceleración, etc.
 *
 * ── USO ───────────────────────────────────────────────────────────────────
 *
 *   // En BulletNormal:
 *   @Override
 *   public TrajectoryProvider getTrajectoryProvider() {
 *       return TrajectoryProvider.DEFAULT_LINEAR_GRAVITY;
 *   }
 *
 *   // En MetheorBullet (con gravedad custom):
 *   @Override
 *   public TrajectoryProvider getTrajectoryProvider() {
 *       return new TrajectoryProvider() {
 *           @Override
 *           public TrajectoryPrediction predict(...) {
 *               return ProjectileTrajectoryPredictor.predict(
 *                   spawnPosition, direction, speed, lifeTime,
 *                   1200.0,  // GRAVITY_STRENGTH custom
 *                   0.0021   // DRAG_COEFFICIENT custom
 *               );
 *           }
 *       };
 *   }
 */
@FunctionalInterface
public interface TrajectoryProvider {

    /**
     * Predice la trayectoria que seguirá el proyectil.
     *
     * @param spawnPosition posición inicial del proyectil (mundo)
     * @param direction     dirección normalizada del disparo
     * @param speed         velocidad del proyectil (unidades/frame)
     * @param lifeTime      frames de vida máximos
     * @return predicción de trayectoria
     */
    ProjectileTrajectoryPredictor.TrajectoryPrediction predict(
            Vector2D spawnPosition,
            Vector2D direction,
            double speed,
            int lifeTime
    );

    // ── Providers predefinidos ────────────────────────────────────────────

    /**
     * Provider por defecto para proyectiles lineales sin gravedad.
     * Usado por balas rápidas, láseres, proyectiles mágicos, etc.
     */
    TrajectoryProvider DEFAULT_LINEAR = (spawnPosition, direction, speed, lifeTime) ->
            ProjectileTrajectoryPredictor.predict(
                    spawnPosition, direction, speed, lifeTime,
                    0.0,  // sin gravedad
                    0.0   // sin drag
            );

    /**
     * Provider por defecto para proyectiles con gravedad estándar.
     * Usado por balas físicas normales, flechas, granadas, etc.
     */
    TrajectoryProvider DEFAULT_LINEAR_GRAVITY = (spawnPosition, direction, speed, lifeTime) ->
            ProjectileTrajectoryPredictor.predict(
                    spawnPosition, direction, speed, lifeTime,
                    0.4,  // gravedad estándar
                    0.0   // sin drag
            );

    /**
     * Provider para proyectiles pesados con alta gravedad.
     * Usado por meteoros, bolas de cañón, proyectiles masivos, etc.
     */
    TrajectoryProvider HEAVY_GRAVITY = (spawnPosition, direction, speed, lifeTime) ->
            ProjectileTrajectoryPredictor.predict(
                    spawnPosition, direction, speed, lifeTime,
                    1.2,  // gravedad alta (3x estándar)
                    0.0   // sin drag
            );
}
