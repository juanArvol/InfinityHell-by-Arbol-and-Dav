package Game.Items.Types.Bullets.Movement;

import Game.Items.Types.Bullets.Definition.Bullet;
import Game.Items.Types.Bullets.ProjectileMovement;

/**
 * Movimiento con aceleración — el proyectil gana o pierde velocidad exponencialmente.
 *
 * ── HRFC Phase 3 — Temporal Migration ─────────────────────────────────────
 * ── Mini-HRFC — Final Temporal Normalization ──────────────────────────────
 *
 * MIGRACIÓN TEMPORAL:
 *   AcceleratingMovement ahora usa aceleración temporal correcta.
 *
 *   ANTES (frame-based @ 30 FPS):
 *     v_new = v_old × factor  (cada frame, dependiente del FPS)
 *
 *   AHORA (time-based):
 *     v_new = v_old × factor^(dt × FPS_BASE)  (exponencial temporal)
 *
 *   CORRECCIÓN CRÍTICA: FPS_BASE = 30, no 60.
 *   El factor se interpreta como "multiplicador por frame @ 30 FPS".
 *   Para independencia temporal: factor_dt = factor^(dt × 30)
 *
 *   DERIVACIÓN:
 *     A 30 FPS: dt = 1/30, factor_dt = factor^(1/30 × 30) = factor^1 = factor ✓
 *     A 60 FPS: dt = 1/60, factor_dt = factor^(1/60 × 30) = factor^0.5 = sqrt(factor)
 *     A 120 FPS: dt = 1/120, factor_dt = factor^(1/120 × 30) = factor^0.25
 *
 * Casos de uso:
 *   - Cohetes que arrancan despacio y aceleran (factor > 1.0)
 *   - Proyectiles que se frenan antes de desaparecer (factor < 1.0)
 *   - Balas de jefe que emiten un "aviso" lento y luego disparan veloz
 *   - Proyectiles que rebotan y pierden energía gradualmente
 *
 * Uso:
 *   // Cohete: empieza lento, acelera hasta 1200 units/s
 *   ProjectileMovement m = new AcceleratingMovement(1.08, 1200.0);
 *
 *   // Fricción: el proyectil se frena hasta detenerse
 *   ProjectileMovement m = new AcceleratingMovement(0.95, 0.0);
 */
public final class AcceleratingMovement implements ProjectileMovement {

    private final double accelFactor;  // multiplicador @ 30 FPS (>1 acelera, <1 frena)
    private final double maxSpeed;     // velocidad máxima en units/s

    /**
     * ── HRFC Phase 3 — Temporal Migration ─────────────────────────────────
     * ── Mini-HRFC — Final Temporal Normalization ──────────────────────────
     *
     * CORRECCIÓN: FPS_BASE = 30, no 60.
     *
     * @param accelFactor multiplicador de velocidad @ 30 FPS (>1 acelera, <1 frena)
     * @param maxSpeed    velocidad máxima en units/s (0 = sin límite superior si se frena)
     */
    public AcceleratingMovement(double accelFactor, double maxSpeed) {
        this.accelFactor = accelFactor;
        this.maxSpeed    = maxSpeed;
    }

    @Override
    public void tick(Bullet bullet, double dt) {
        // ── HRFC Phase 3 + Mini-HRFC: Temporal integration corrected ─────
        // Convertir el factor de "por frame @ 30 FPS" a "por dt segundos"
        // factor_dt = factor^(dt × 30)
        // 
        // Ejemplos:
        //   factor=1.08 @ 30 FPS (dt=1/30): 1.08^1 = 1.080 ✓
        //   factor=1.08 @ 60 FPS (dt=1/60): 1.08^0.5 ≈ 1.039
        //   factor=1.08 @ 120 FPS (dt=1/120): 1.08^0.25 ≈ 1.019
        double temporalFactor = Math.pow(accelFactor, dt * 30.0);
        
        double vx = bullet.getPhysics().getXspeed() * temporalFactor;
        double vy = bullet.getPhysics().getYspeed() * temporalFactor;

        if (maxSpeed > 0) {
            double speed = Math.hypot(vx, vy);
            if (speed > maxSpeed) {
                double scale = maxSpeed / speed;
                vx *= scale;
                vy *= scale;
            }
        }

        bullet.getPhysics().setXspeed(vx);
        bullet.getPhysics().setYspeed(vy);
    }

    /**
     * AcceleratingMovement no tiene estado interno mutable.
     * La misma instancia puede compartirse entre proyectiles del mismo tipo.
     */
    @Override
    public boolean isStateless() {
        return true;
    }
}
