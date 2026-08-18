package Game.Enemys.Bosses.Sans.Movement;

import Game.Enemys.AI.EnemyContext;
import Game.Enemys.Core.Contracts.MovementStrategy;
import Game.Enemys.Core.Enemy;

/**
 * Estrategia de movimiento exclusiva de Sans.
 *
 * ── HRFC-007 — Living Entity Core ────────────────────────────────────────
 * SansMovement no importa ningún tipo del paquete Enemy.Stats — lee
 * únicamente a través de enemy.getStats() (EntityStats) y
 * enemy.getFlags() (EntityFlags), ambos ya resueltos en Enemy.java.
 *
 * ── Modos de movimiento ───────────────────────────────────────────────────
 *
 *   IDLE        — Sans está quieto cuando la distancia al jugador es correcta.
 *   ORBIT       — rodea al jugador describiendo un arco lento.
 *   MAINTAIN    — se aproxima para quedar en el rango de cómfort.
 *   RETREAT     — retrocede cuando el jugador invade su espacio personal.
 *
 * ── Separación de responsabilidades ─────────────────────────────────────
 *   SansMovement      → desplazamiento continuo frame-a-frame (física suave).
 *   SansTeleportAction → salto instantáneo sin física (acción de IA).
 *   SansDodgeBehavior  → decisión de cuándo teleportarse (comportamiento IA).
 *
 * ── Flags ────────────────────────────────────────────────────────────────
 * onActivate() marca isFlying=true en EntityFlags para que el motor de
 * animación sepa que Sans está flotando.
 * onDeactivate() lo limpia.
 */
public final class SansMovement implements MovementStrategy {

    // ── Parámetros de movimiento ───────────────────────────────────────────
    /** Distancia ideal de "cómfort" — Sans orbita a esta distancia. */
    private static final double COMFORT_DISTANCE  = 220.0;
    /** Margen de tolerancia: dentro de este rango, Sans permanece en IDLE. */
    private static final double COMFORT_MARGIN    = 40.0;
    /** Velocidad de orbitación angular (radianes por frame). */
    private static final double ORBIT_ANGULAR_SPEED = 0.012;
    /** Velocidad de movimiento de aproximación/retirada (px/frame). */
    private static final double MOVE_SPEED        = 1.8;

    private double orbitAngle = 0.0;

    // ── Ciclo de vida ─────────────────────────────────────────────────────

    @Override
    public void onActivate(Enemy enemy) {
        enemy.getFlags().setFlying(true);
        enemy.getState().setFlying(true);
    }

    @Override
    public void onDeactivate(Enemy enemy) {
        enemy.getFlags().setFlying(false);
        enemy.getState().setFlying(false);
    }

    // ── Movimiento por frame ──────────────────────────────────────────────

    @Override
    public void move(Enemy enemy, EnemyContext ctx, double deltaTime) {
        if (ctx == null) return;

        double sansX   = enemy.getTransform().getPosition().getX();
        double sansY   = enemy.getTransform().getPosition().getY();
        double targetX = ctx.getPosition().getX();
        double targetY = ctx.getPosition().getY();

        double dx   = targetX - sansX;
        double dy   = targetY - sansY;
        double dist = Math.sqrt(dx * dx + dy * dy);

        if (dist < 0.01) return; // evitar división por cero

        double speed = enemy.getStats().getSpeed() > 0
            ? enemy.getStats().getSpeed()
            : MOVE_SPEED;

        if (dist > COMFORT_DISTANCE + COMFORT_MARGIN) {
            // ── MAINTAIN: acercarse suavemente hasta el rango de cómfort ──
            double nx = dx / dist;
            double ny = dy / dist;
            applyVelocity(enemy, nx * speed, ny * speed);
            enemy.getState().setMoving(true);

        } else if (dist < COMFORT_DISTANCE - COMFORT_MARGIN) {
            // ── RETREAT: alejarse del jugador ─────────────────────────────
            double nx = -dx / dist;
            double ny = -dy / dist;
            applyVelocity(enemy, nx * speed, ny * speed);
            enemy.getState().setMoving(true);

        } else {
            // ── ORBIT: rodear al jugador en arco ──────────────────────────
            orbitAngle += ORBIT_ANGULAR_SPEED;
            double orbitX = targetX + Math.cos(orbitAngle) * COMFORT_DISTANCE;
            double orbitY = targetY + Math.sin(orbitAngle) * COMFORT_DISTANCE;

            double odx = orbitX - sansX;
            double ody = orbitY - sansY;
            double od  = Math.sqrt(odx * odx + ody * ody);

            if (od > 1.0) {
                double nx = odx / od;
                double ny = ody / od;
                applyVelocity(enemy, nx * speed * 0.6, ny * speed * 0.6);
                enemy.getState().setMoving(true);
            } else {
                // ── IDLE: exactamente en posición ─────────────────────────
                applyVelocity(enemy, 0, 0);
            }
        }

        // Actualizar dirección de sprite
        enemy.getState().setMirandoDerecha(dx >= 0);
    }

    // ── Auxiliar ──────────────────────────────────────────────────────────

    private void applyVelocity(Enemy enemy, double vx, double vy) {
        if (enemy.getPhysics() != null) {
            enemy.getPhysics().getVelocity().setX(vx);
            enemy.getPhysics().getVelocity().setY(vy);
        }
    }
}
