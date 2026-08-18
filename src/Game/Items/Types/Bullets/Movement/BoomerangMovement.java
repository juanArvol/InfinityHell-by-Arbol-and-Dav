package Game.Items.Types.Bullets.Movement;

import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Items.Types.Bullets.Definition.Bullet;
import Game.Items.Types.Bullets.ResettableMovement;

/**
 * Movimiento boomerang — el proyectil avanza, frena y vuelve al origen de spawn.
 *
 * Fases:
 *   1. OUTWARD   — avanza en la dirección inicial durante travelTicks frames.
 *   2. RETURNING — gira hacia el punto de origen y regresa.
 *   3. ARRIVED   — cuando llega al origen, el proyectil muere.
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
 *   (cuando frameCount == 0 antes de incrementar). En ese frame, el bullet
 *   ya tiene la posición de spawn correcta.
 *
 *   reset() limpia tanto frameCount como origin (lo marca como no-capturado).
 *   Así el próximo ciclo de vida recaptura automáticamente la nueva posición.
 *
 *   GENERALIZACIÓN: cualquier movement que necesite la posición de spawn del
 *   bullet debe capturarla en el primer tick, no recibirla en el constructor.
 *   El constructor solo recibe CONFIGURACIÓN (travelTicks, returnSpeed) —
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
 *   ProjectileMovement m = new BoomerangMovement(45, 10.0);
 *   //   travelTicks=45: frames de vuelo hacia afuera antes de volver
 *   //   returnSpeed=10: velocidad de regreso en unidades/frame
 *
 * Pool:
 *   Implementa ResettableMovement. reset() marca origin como no-capturado.
 *   El próximo ciclo recaptura la posición del nuevo bullet en el primer tick.
 *   El pool puede reutilizar instancias de BoomerangMovement de forma segura.
 */
public final class BoomerangMovement implements ResettableMovement {

    private final int      travelTicks;
    private final double   returnSpeed;

    /** Frame counter. Reseteable. */
    private int frameCount = 0;

    /**
     * Posición de spawn capturada en el primer tick.
     * null = aún no capturado (estado inicial y estado post-reset).
     * Se captura del bullet en el primer frame del ciclo de vida.
     */
    private Vector2D capturedOrigin = null;

    /**
     * @param travelTicks  ticks que el proyectil avanza antes de volver
     * @param returnSpeed  velocidad de regreso (unidades/frame)
     */
    public BoomerangMovement(int travelTicks, double returnSpeed) {
        this.travelTicks = travelTicks;
        this.returnSpeed = returnSpeed;
    }

    /**
     * Constructor de compatibilidad que acepta origin explícito.
     *
     * @deprecated Pasar origin explícito está obsoleto. Usar {@link #BoomerangMovement(int, double)}
     *             — el origin se captura automáticamente del bullet en el primer tick.
     *             Este constructor sigue funcionando pero origin será ignorado al hacer reset().
     */
    @Deprecated
    public BoomerangMovement(Vector2D origin, int travelTicks, double returnSpeed) {
        this.travelTicks   = travelTicks;
        this.returnSpeed   = returnSpeed;
        // Captura inmediata del origin provisto — compatible con código existente.
        // En el primer tick el capturedOrigin ya está listo, no se sobreescribe.
        this.capturedOrigin = new Vector2D(origin.getX(), origin.getY());
    }

    @Override
    public void tick(Bullet bullet, double dt) {
        // Capturar origin en el primer tick si aún no fue capturado.
        // Siempre refleja la posición de spawn del bullet actual (no del anterior).
        if (capturedOrigin == null) {
            Vector2D pos = bullet.getTransform().getPosition();
            capturedOrigin = new Vector2D(pos.getX(), pos.getY());
        }

        frameCount++;

        if (frameCount <= travelTicks) {
            return; // fase de avance — la velocidad inicial ya está fija
        }

        Vector2D pos = bullet.getTransform().getPosition();
        double dx   = capturedOrigin.getX() - pos.getX();
        double dy   = capturedOrigin.getY() - pos.getY();
        double dist = Math.hypot(dx, dy);

        if (dist < returnSpeed) {
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
     * Limpia frameCount Y el capturedOrigin — en el primer tick del nuevo ciclo
     * se recapturará la posición del nuevo bullet, garantizando que el boomerang
     * regresa al spawn correcto del nuevo disparo.
     */
    @Override
    public void reset() {
        frameCount     = 0;
        capturedOrigin = null; // se recaptura en el primer tick del nuevo ciclo
    }
}
