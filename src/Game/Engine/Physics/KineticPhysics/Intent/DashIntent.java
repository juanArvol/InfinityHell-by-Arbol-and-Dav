package Game.Engine.Physics.KineticPhysics.Intent;

import Game.Engine.Physics.KineticPhysics.PhysicalCapabilities;
import Game.Engine.Physics.KineticPhysics.Types.Physics2D;

/**
 * Intención de dash — movimiento rápido horizontal instantáneo.
 *
 * ── HRFC — Kinetic Physics: Forces, Impulses & Motion Intent ─────────────
 *
 * ── CLASIFICACIÓN: MOTION INTENT ──────────────────────────────────────────
 *
 * El dash se clasifica como MOTION INTENT (no Impulse simple) porque:
 *
 * 1. Expresa una INTENCIÓN DE GAMEPLAY: "quiero moverme rápidamente X distancia"
 * 2. El impulso necesario depende de CAPACIDADES FÍSICAS (fuerza, masa)
 * 3. Puede ser modificado por buffs/amulets/training en runtime
 * 4. La distancia objetivo es expresada por gameplay, no como velocidad arbitraria
 *
 * Si dash fuera solo un impulso fijo (J = constant), sería clasificado como
 * Impulse simple. Pero como depende de capacidades físicas y puede ser
 * modificado, es Motion Intent.
 *
 * ── FÍSICA DEL DASH ───────────────────────────────────────────────────────
 *
 * El dash es un movimiento horizontal rápido e instantáneo. A diferencia del
 * salto (que tiene trayectoria parabólica bajo gravedad), el dash aplica un
 * impulso horizontal que produce velocidad inmediata en el eje X.
 *
 * Hay dos modelos posibles:
 *
 * MODELO A — Impulso instantáneo (este modelo):
 *   - Aplica impulso horizontal inmediato
 *   - El jugador mantiene esa velocidad hasta que el slide la disipe
 *   - Simple, responsive, no requiere tracking de estado
 *   - Fórmula: v_dash = dashDistance / dashDuration
 *              J = m × v_dash
 *
 * MODELO B — Velocidad mantenida durante duración:
 *   - Aplica velocidad objetivo durante N frames
 *   - Requiere tracking de estado (dashTimer, dashActive)
 *   - Más complejo pero permite control más fino
 *   - Requiere cancelación explícita del estado
 *
 * Esta implementación usa MODELO A (impulso instantáneo) por simplicidad.
 * Si se necesita MODELO B, extender esta clase y sobreescribir resolve().
 *
 * ── DIRECCIÓN DEL DASH ────────────────────────────────────────────────────
 *
 * El dash puede ser:
 *   - Horizontal puro (direccionX ∈ {-1, +1})
 *   - Diagonal (direccionX, direccionY normalizados)
 *
 * Esta implementación base soporta ambos mediante un vector de dirección.
 *
 * ── CAPACIDADES FÍSICAS ───────────────────────────────────────────────────
 *
 * La distancia de dash se escala por:
 *   effectiveDashDistance = baseDashDistance × strengthMultiplier × forceOutputMultiplier
 *
 * NO se usa jumpCapacityMultiplier (ese es específico de salto).
 * El dash usa forceOutputMultiplier directamente.
 *
 * ── USO ───────────────────────────────────────────────────────────────────
 *
 * Desde PlayerController o sistema de habilidades:
 *
 *   if (KeyBoard.getState("dashKey") && dashCooldown.isReady()) {
 *       double directionX = state.isDer() ? 1.0 : -1.0;
 *       DashIntent intent = new DashIntent(
 *           capabilities,
 *           dashDistance,
 *           directionX,
 *           0.0  // sin componente vertical
 *       );
 *       intent.resolve(physics);
 *       dashCooldown.start();
 *   }
 *
 * ── EXTENSIBILIDAD ────────────────────────────────────────────────────────
 *
 * Para dash direccional avanzado (air dash, diagonal dash):
 *
 *   class DiagonalDashIntent extends DashIntent {
 *       public DiagonalDashIntent(capabilities, distance, dirX, dirY) {
 *           super(capabilities, distance, dirX, dirY);
 *       }
 *   }
 */
public class DashIntent implements MotionIntent {

    private final PhysicalCapabilities capabilities;
    private final double baseDashDistance;
    private final double directionX;
    private final double directionY;

    /**
     * Duración asumida del dash en frames para calcular velocidad necesaria.
     * Un dash "instantáneo" aún requiere algunos frames para ejecutarse.
     * Valor típico: 1-3 frames.
     */
    private static final double DASH_DURATION_FRAMES = 1.0;

    /**
     * Crea una intención de dash horizontal puro.
     *
     * @param capabilities capacidades físicas que escalan la distancia
     * @param baseDashDistance distancia base del dash (sin modificadores)
     * @param directionX dirección horizontal (-1 = izquierda, +1 = derecha)
     */
    public DashIntent(PhysicalCapabilities capabilities,
                      double baseDashDistance,
                      double directionX) {
        this(capabilities, baseDashDistance, directionX, 0.0);
    }

    /**
     * Crea una intención de dash con componente vertical (diagonal/air dash).
     *
     * @param capabilities capacidades físicas que escalan la distancia
     * @param baseDashDistance distancia base del dash (sin modificadores)
     * @param directionX dirección horizontal (-1 a +1)
     * @param directionY dirección vertical (-1 a +1)
     */
    public DashIntent(PhysicalCapabilities capabilities,
                      double baseDashDistance,
                      double directionX,
                      double directionY) {
        if (capabilities == null) {
            throw new IllegalArgumentException("capabilities no puede ser null");
        }
        if (baseDashDistance < 0) {
            throw new IllegalArgumentException("baseDashDistance debe ser >= 0");
        }

        this.capabilities = capabilities;
        this.baseDashDistance = baseDashDistance;

        // Normalizar dirección si es necesario
        double magnitude = Math.sqrt(directionX * directionX + directionY * directionY);
        if (magnitude > 0) {
            this.directionX = directionX / magnitude;
            this.directionY = directionY / magnitude;
        } else {
            // Sin dirección válida, usar dirección derecha por defecto
            this.directionX = 1.0;
            this.directionY = 0.0;
        }
    }

    /**
     * Resuelve el dash aplicando el impulso horizontal necesario para alcanzar
     * la distancia efectiva definida en capabilities × baseDashDistance.
     *
     * Fórmula:
     *   effectiveDistance = baseDashDistance × strengthMultiplier × forceOutputMultiplier
     *   v_dash = effectiveDistance / DASH_DURATION_FRAMES
     *   J = m × v_dash × direction
     *   physics.addForce(Jx, Jy)
     *
     * @param physics contexto físico sobre el que aplicar el impulso
     */
    @Override
    public void resolve(Physics2D physics) {
        // Calcular distancia efectiva con modificadores
        double effectiveDistance = baseDashDistance 
                                 * capabilities.getEffectiveForceMultiplier();

        // Calcular velocidad necesaria para cubrir la distancia en DASH_DURATION_FRAMES
        double dashVelocity = effectiveDistance / DASH_DURATION_FRAMES;

        // Aplicar dirección
        double vx = dashVelocity * directionX;
        double vy = dashVelocity * directionY;

        // Calcular impulso necesario: J = m × v
        double mass = physics.getMass();
        double impulseX = mass * vx;
        double impulseY = mass * vy;

        // Aplicar impulso
        physics.addForce(impulseX, impulseY);
    }

    /**
     * Calcula la velocidad de dash efectiva sin aplicar el impulso.
     * Útil para preview y debug.
     *
     * @return velocidad de dash (magnitud, sin dirección)
     */
    public double calculateDashVelocity() {
        double effectiveDistance = baseDashDistance 
                                 * capabilities.getEffectiveForceMultiplier();
        return effectiveDistance / DASH_DURATION_FRAMES;
    }

    /**
     * Calcula el impulso requerido para el dash sin aplicarlo.
     * Útil para preview y debug.
     *
     * @param physics contexto físico (para consultar masa)
     * @return magnitud del impulso necesario
     */
    public double calculateRequiredImpulse(Physics2D physics) {
        double velocity = calculateDashVelocity();
        return physics.getMass() * velocity;
    }

    /**
     * @return distancia base de dash (sin modificadores)
     */
    public double getBaseDashDistance() {
        return baseDashDistance;
    }

    /**
     * @return dirección horizontal del dash (-1 a +1)
     */
    public double getDirectionX() {
        return directionX;
    }

    /**
     * @return dirección vertical del dash (-1 a +1)
     */
    public double getDirectionY() {
        return directionY;
    }

    /**
     * @return capacidades físicas asociadas a este intent
     */
    public PhysicalCapabilities getCapabilities() {
        return capabilities;
    }

    @Override
    public String toString() {
        return String.format("DashIntent{baseDist=%.2f, effectiveDist=%.2f, dir=(%.2f,%.2f)}",
                baseDashDistance,
                baseDashDistance * capabilities.getEffectiveForceMultiplier(),
                directionX,
                directionY);
    }
}
