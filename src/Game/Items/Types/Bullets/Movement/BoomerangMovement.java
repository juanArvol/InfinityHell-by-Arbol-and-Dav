package Game.Items.Types.Bullets.Movement;

import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Items.Types.Bullets.Definition.Bullet;
import Game.Items.Types.Bullets.ResettableMovement;

/**
 * Movimiento boomerang — el proyectil avanza, frena y vuelve al origen de spawn.
 *
 * Fases:
 *   1. OUTWARD   — avanza en la dirección inicial durante travelDuration segundos.
 *   2. RETURNING — gira hacia el punto de origen y regresa.
 *   3. ARRIVED   — cuando llega al origen, el proyectil muere.
 *
 * ── HRFC Phase 3 — Temporal Migration ─────────────────────────────────────
 *
 * MIGRACIÓN CRÍTICA:
 *   BoomerangMovement ahora usa tiempo en segundos en lugar de frameCount.
 *
 *   ANTES (frame-based):
 *     frameCount++ cada tick
 *     if (frameCount <= travelTicks) → comportamiento dependiente del FPS
 *
 *   AHORA (time-based):
 *     elapsedTime += deltaTime
 *     if (elapsedTime <= travelDuration) → comportamiento independiente del FPS
 *
 *   CONVERSIÓN:
 *     travelTicks @ 30 FPS → travelDuration en segundos
 *     travelDuration = travelTicks / 30.0
 *
 * ── HRFC §12 — Pool semantic parity: corrección de origin ─────────────────
 *
 * PROBLEMA ANTERIOR:
 *   origin se pasaba como parámetro del constructor y se copiaba al construir.
 *   Al reutilizar el proyectil desde el pool, reset() solo reseteaba frameCount,
 *   pero origin seguía apuntando al spawn del proyectil ANTERIOR. El boomerang
 *   regresaba al punto de spawn viejo en lugar del nuevo.
 *
 * SOLUCIÓN ARQUITECTÓNICA GENERALIZABLE:
 *   El origin es POSICIÓN DE SPAWN DEL PROYECTIL, no configuración del movimiento.
 *   El movimiento captura el origin automáticamente del bullet en el PRIMER TICK
 *   (cuando elapsedTime == 0 antes de incrementar). En ese frame, el bullet
 *   ya tiene la posición de spawn correcta.
 *
 *   reset() limpia tanto elapsedTime como origin (lo marca como no-capturado).
 *   Así el próximo ciclo de vida recaptura automáticamente la nueva posición.
 *
 *   GENERALIZACIÓN: cualquier movement que necesite la posición de spawn del
 *   bullet debe capturarla en el primer tick, no recibirla en el constructor.
 *   El constructor solo recibe CONFIGURACIÓN (travelDuration, returnSpeed) —
 *   valores que no cambian entre disparos del mismo tipo.
 *
 * Casos de uso:
 *   - Boomerangs y hachas de retorno
 *   - Latigazos de energía
 *   - Proyectiles teledirigidos de retorno
 *   - Yo-yo de combate
 *
 * Uso:
 *   // Ya no se pasa origin — se captura automáticamente del bullet al spawn.
 *   ProjectileMovement m = new BoomerangMovement(0.75, 600.0);
 *   //   travelDuration=0.75: segundos de vuelo hacia afuera antes de volver
 *   //   returnSpeed=600: velocidad de regreso en unidades/s
 *
 * Pool:
 *   Implementa ResettableMovement. reset() marca origin como no-capturado.
 *   El próximo ciclo recaptura la posición del nuevo bullet en el primer tick.
 *   El pool puede reutilizar instancias de BoomerangMovement de forma segura.
 */
public final class BoomerangMovement implements ResettableMovement {

    private final double travelDuration;  // segundos de viaje antes de volver
    private final double returnSpeed;     // velocidad de regreso en units/s

    /** Elapsed time counter. Resettable. */
    private double elapsedTime = 0.0;

    /**
     * Posición de spawn capturada en el primer tick.
     * null = aún no capturado (estado inicial y estado post-reset).
     * Se captura del bullet en el primer frame del ciclo de vida.
     */
    private Vector2D capturedOrigin = null;

    /**
     * Constructor principal.
     *
     * @param travelDuration segundos que el proyectil avanza antes de volver
     * @param returnSpeed velocidad de regreso en units/s
     */
    public BoomerangMovement(double travelDuration, double returnSpeed) {
        this.travelDuration = travelDuration;
        this.returnSpeed = returnSpeed;
    }

    @Override
    public void tick(Bullet bullet, double dt) {
        // Capturar origin en el primer tick si aún no fue capturado.
        // Siempre refleja la posición de spawn del bullet actual (no del anterior).
        if (capturedOrigin == null) {
            // Sin allocation — construir Vector2D con primitivos
            capturedOrigin = new Vector2D(bullet.getPositionX(), bullet.getPositionY());
        }

        // ── HRFC Phase 3: Temporal integration ───────────────────────────
        elapsedTime += dt;

        if (elapsedTime <= travelDuration) {
            return; // fase de avance — la velocidad inicial ya está fija
        }

        // Sin allocation — usar primitivos directamente
        double posX = bullet.getPositionX();
        double posY = bullet.getPositionY();
        double dx   = capturedOrigin.getX() - posX;
        double dy   = capturedOrigin.getY() - posY;
        double dist = Math.hypot(dx, dy);

        if (dist < returnSpeed * dt) {
            bullet.getBulletLife().kill();
            return;
        }

        double scale = returnSpeed / dist;
        bullet.getPhysics().setXspeed(dx * scale);
        bullet.getPhysics().setYspeed(dy * scale);
    }

    /**
     * BoomerangMovement tiene estado interno (frameCount, capturedOrigin).
     * No puede compartirse como singleton.
     */
    @Override
    public boolean isStateless() {
        return false;
    }

    /**
     * Resetea el estado para el próximo ciclo de vida.
     *
     * Limpia elapsedTime Y el capturedOrigin — en el primer tick del nuevo ciclo
     * se recapturará la posición del nuevo bullet, garantizando que el boomerang
     * regresa al spawn correcto del nuevo disparo.
     */
    @Override
    public void reset() {
        elapsedTime    = 0.0;
        capturedOrigin = null; // se recaptura en el primer tick del nuevo ciclo
    }
}
