package Game.Player;

import Game.Engine.Physics.KineticPhysics.MovementContext;
import Game.Engine.Physics.KineticPhysics.PhysicalCapabilities;
import Game.Engine.Physics.KineticPhysics.Types.Physics2D;

/**
 * Física del jugador.
 *
 * Implementación específica del Game que extiende Physics2D del Engine.
 * Vive en Game.Player porque es una regla del juego (parámetros de
 * movimiento del jugador), no infraestructura reutilizable.
 *
 * ── HRFC — Consolidación Final de Kinetic Physics ────────────────────────
 *
 * MIGRADO DESDE: Game.Engine.GameMath.Physics.Implementation.PlayerPhysics
 * RAZÓN: PlayerPhysics contiene valores de gameplay específicos del jugador
 * (RUN_BOOST, speedMaxPiso, slide). No es infraestructura genérica del Engine.
 *
 * ── HRFC — Kinetic Physics: Forces, Impulses & Motion Intent ─────────────
 *
 * PlayerPhysics ahora integra PhysicalCapabilities para separar masa (propiedad
 * física) de fuerza/capacidad (habilidad muscular). Esto permite:
 *
 *   - Modificar capacidad de salto sin cambiar masa
 *   - Entrenar/progresar incrementando strengthMultiplier
 *   - Aplicar buffs/debuffs que afecten capacidades físicas
 *   - Usar Motion Intents (JumpIntent) en lugar de velocidades arbitrarias
 *
 * ── Propiedades Aerodinámicas ────────────────────────────────────────────
 *
 * El Player tiene propiedades aerodinámicas configuradas para producir
 * una velocidad terminal de caída razonable (~20-25 px/frame).
 *
 * Con gravity=0.4, mass=40.0, effectiveArea=1.2, dragCoefficient=0.0004:
 *   - Velocidad terminal ≈ 22 px/frame
 *   - Comportamiento de caída natural y controlado
 *   - Sin límites artificiales
 *
 * Estas propiedades pueden ajustarse durante el gameplay (power-ups,
 * estados alterados) sin modificar el core de física.
 *
 * ── Capas de modificador ─────────────────────────────────────────────────
 *
 *   mAccel = baseAccel
 *          × entityModifier      → (1/mass) × runBoost
 *          × surfaceModifier     → friction × accelScale  (en Physics2D)
 *          × statusModifier      → herido, stun, buff     (externo via statusStack)
 *          × environmentModifier → viento, agua           (externo via environmentStack)
 *          × airControlModifier  → airControl del surface (en Physics2D)
 */
public class PlayerPhysics extends Physics2D {

    /** Multiplicador de aceleración cuando el jugador corre. */
    private static final double RUN_BOOST = 1.8;

    // ── Constantes de movimiento ──────────────────────────────────────────
    // Centralizar estos valores evita que el tuning se disperse en múltiples
    // expresiones literales dentro de moveX(). Cambiar el comportamiento del
    // jugador requiere editar solo esta sección.

    /** Velocidad máxima horizontal caminando (píxeles virtuales/frame). */
    private static final double WALK_SPEED_GROUND = 70;
    /** Velocidad máxima horizontal corriendo (píxeles virtuales/frame). */
    private static final double RUN_SPEED_GROUND  = 135;

    /** Velocidad máxima horizontal en el aire caminando. */
    private static final double WALK_SPEED_AIR    = 10;
    /** Velocidad máxima horizontal en el aire corriendo. */
    private static final double RUN_SPEED_AIR     = 18.5;

    /** Factor de deslizamiento en el suelo (0=stop inmediato, 1=sin fricción). */
    private static final double SLIDE_GROUND      = 0.9;
    /** Factor de deslizamiento en el aire (menor que en el suelo → más control aéreo). */
    private static final double SLIDE_AIR         = 0.74;

    /** Aceleración base en el suelo. */
    private static final double ACCEL_GROUND      = 2.5;
    /** Aceleración base en el aire (menor → menos control aéreo brusco). */
    private static final double ACCEL_AIR         = 1.07;

    // ── Propiedades aerodinámicas (HRFC — Consolidación) ─────────────────
    // HRFC FASE 2: Coeficientes escalados para px/frame.
    // Configuradas para producir velocidad terminal ≈ 20-25 px/frame con gravity=0.4

    /** Área efectiva del jugador expuesta al flujo de aire. */
    private static final double PLAYER_EFFECTIVE_AREA = 1.2;

    /** Coeficiente de drag del jugador (escalado para px/frame). */
    private static final double PLAYER_DRAG_COEFFICIENT = 0.0004;

    // ── Capacidades Físicas (HRFC — Motion Intent) ───────────────────────
    // HRFC: Kinetic Physics — separación de masa (propiedad física) y
    // fuerza/capacidad (habilidad muscular).

    /**
     * Altura base de salto del jugador en píxeles virtuales.
     * 
     * Con gravity=0.4 (PlayerController típico), baseJumpHeight=15 produce
     * aproximadamente la misma altura de salto que el valor legacy jump(10).
     * 
     * Fórmula legacy: velocity.y = -10 / mass = -10/40 = -0.25
     * Con v₀=-0.25 y g=0.4: h = v₀²/(2g) = 0.0625/0.8 ≈ 0.078 (muy bajo)
     * 
     * Nota: El valor legacy physics.jump(10) producía un salto muy bajo
     * porque 10 era el impulso (no la altura). Con mass=40:
     *   v₀ = 10/40 = 0.25 px/frame
     *   altura alcanzada con g=0.4: h = 0.25²/(2×0.4) ≈ 0.078 px
     * 
     * Para un salto visible (~15-20 px de altura), se necesita ajustar este valor
     * según la gravedad configurada en PlayerController.
     */
    private static final double BASE_JUMP_HEIGHT = 15.0;

    /**
     * Capacidades físicas del jugador (fuerza, capacidad de salto, etc.).
     * Permite modificación runtime para buffs/debuffs/entrenamiento.
     */
    private final PhysicalCapabilities capabilities;

    public PlayerPhysics(double gravity) {
        super(gravity);
        mass            = 40.0;
        aGround         = ACCEL_GROUND;
        aAir            = ACCEL_AIR;

        // Configurar propiedades aerodinámicas del jugador (HRFC FASE 2)
        effectiveArea   = PLAYER_EFFECTIVE_AREA;
        dragCoefficient = PLAYER_DRAG_COEFFICIENT;
        // mediumDensity obsoleto — no se usa después de corrección de unidades

        // HRFC — Motion Intent: Inicializar capacidades físicas
        // Los modificadores comienzan en 1.0 (sin buffs/debuffs)
        this.capabilities = new PhysicalCapabilities(
            BASE_JUMP_HEIGHT,  // baseJumpHeight
            8.0,               // strengthMultiplier
            1.0,               // forceOutputMultiplier
            1.0                // jumpCapacityMultiplier
        );
    }

    // ── Capacidades Físicas (HRFC — Motion Intent) ───────────────────────

    /**
     * Obtiene las capacidades físicas del jugador.
     * 
     * Usar para consultar/modificar capacidades en runtime:
     * 
     *   // Aplicar buff de entrenamiento
     *   physics.getCapabilities().setStrengthMultiplier(1.3);
     * 
     *   // Aplicar Super Jump amulet
     *   physics.getCapabilities().setJumpCapacityMultiplier(2.0);
     * 
     *   // Aplicar Gym Rat buff
     *   physics.getCapabilities().setForceOutputMultiplier(8.0);
     * 
     * @return capacidades físicas mutables del jugador
     */
    public PhysicalCapabilities getCapabilities() {
        return capabilities;
    }

    /**
     * Capa de entidad del jugador: masa + boost de carrera.
     */
    @Override
    protected double computeEntityModifier(MovementContext ctx) {
        double base = 20.0 / mass;
        return ctx.running() ? base * RUN_BOOST : base;
    }

    @Override
    public void moveX(double inputX, boolean onGround, boolean running) {
        speedMaxPiso = running ? RUN_SPEED_GROUND : WALK_SPEED_GROUND;
        speedMaxAir  = running ? RUN_SPEED_AIR    : WALK_SPEED_AIR;

        // IMPORTANTE: asignar slide ANTES de super.moveX()
        slide = onGround ? SLIDE_GROUND : SLIDE_AIR;

        super.moveX(inputX, onGround, running);
    }
}
