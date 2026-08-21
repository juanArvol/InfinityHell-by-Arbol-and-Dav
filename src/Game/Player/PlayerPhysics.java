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
 * ── HRFC Phase 3 — Temporal Migration ─────────────────────────────────────
 * ── Mini-HRFC — Final Temporal & Parameter Semantics Normalization ─────────
 *
 * MIGRACIÓN CRÍTICA DE UNIDADES:
 *   Todos los valores de movimiento ahora están expresados en unidades/segundo
 *   en lugar de unidades/frame.
 *
 *   CONVERSIÓN REALIZADA (CORREGIDA):
 *     El sistema legacy operaba a 30 FPS, no 60 FPS.
 *     velocidades:    valores @ 30 FPS × 30 = units/s
 *     aceleraciones:  valores @ 30 FPS × 30 = units/s²
 *
 *   ANTES (frame-based @ 30 FPS):
 *     WALK_SPEED_GROUND = 70    (px/frame)
 *     ACCEL_GROUND      = 2.5   (px/frame²)
 *
 *   AHORA (time-based):
 *     WALK_SPEED_GROUND = 2100  (px/s) = 70 × 30
 *     ACCEL_GROUND      = 75    (px/s²) = 2.5 × 30
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

    /**
     * Fuerza muscular base del jugador (parámetro de gameplay).
     * 
     * Representa la capacidad física del jugador para generar aceleración.
     * Se combina con la masa para determinar el modificador de entidad:
     *   entityModifier = PLAYER_STRENGTH / mass
     * 
     * Con mass=40: entityModifier = 20.0 / 40.0 = 0.5
     * 
     * Este valor fue calibrado empíricamente en el sistema legacy y define
     * qué tan responsive se siente el movimiento del jugador.
     * 
     * EFECTO EN ACELERACIÓN EFECTIVA:
     *   ACCEL_GROUND = 75 units/s²
     *   Efectiva = 75 × 0.5 = 37.5 units/s²
     *   Legacy equivalente = 37.5 / 30 = 1.25 px/frame² @ 30 FPS
     */
    private static final double PLAYER_STRENGTH = 20.0;

    // ── Constantes de movimiento ──────────────────────────────────────────
    // ── HRFC Phase 3 — Temporal Migration ─────────────────────────────────
    // ── Mini-HRFC — Final Temporal Normalization ──────────────────────────
    //
    // MIGRACIÓN DE UNIDADES: frame-based @ 30 FPS → time-based
    //
    // CORRECCIÓN CRÍTICA: El sistema legacy operaba a 30 FPS, no 60 FPS.
    // Todos los valores se convierten de unidades/frame @ 30 FPS a 
    // unidades/segundo multiplicando por 30 (no 60).
    //
    // DERIVACIÓN DE CADA PARÁMETRO:
    //   Legacy: valor en px/frame a 30 FPS
    //   Conversión: valor × 30 = units/s
    //   Verificación: @ 30 FPS, dt=1/30s, v×dt debe reproducir comportamiento legacy
    //
    // Centralizar estos valores evita que el tuning se disperse en múltiples
    // expresiones literales dentro de moveX(). Cambiar el comportamiento del
    // jugador requiere editar solo esta sección.

    /** 
     * Velocidad máxima horizontal caminando en units/s.
     * DERIVACIÓN: 70 px/frame @ 30 FPS → 70 × 30 = 2100 units/s
     */
    private static final double WALK_SPEED_GROUND = 2100.0;
    
    /** 
     * Velocidad máxima horizontal corriendo en units/s.
     * DERIVACIÓN: 135 px/frame @ 30 FPS → 135 × 30 = 4050 units/s
     */
    private static final double RUN_SPEED_GROUND  = 4050.0;

    /** 
     * Velocidad máxima horizontal en el aire caminando en units/s.
     * DERIVACIÓN: 10 px/frame @ 30 FPS → 10 × 30 = 300 units/s
     */
    private static final double WALK_SPEED_AIR    = 300.0;
    
    /** 
     * Velocidad máxima horizontal en el aire corriendo en units/s.
     * DERIVACIÓN: 18.5 px/frame @ 30 FPS → 18.5 × 30 = 555 units/s
     */
    private static final double RUN_SPEED_AIR     = 555.0;

    /** 
     * Factor de deslizamiento en el suelo (adimensional, frame-based @ 30 FPS).
     * 
     * SEMÁNTICA: velocidad *= 0.9 cada frame @ 30 FPS
     * 
     * CONVERSIÓN TEMPORAL (Physics2D.moveX()):
     *   k = -30 × ln(0.9) ≈ 3.154 (tasa de decay)
     *   damping_factor = e^(-k × deltaTime)
     *   v_new = v_old × damping_factor
     * 
     * A 30 FPS (dt=1/30): e^(-3.154/30) ≈ 0.900 (reproduce legacy) ✓
     * A 60 FPS (dt=1/60): e^(-3.154/60) ≈ 0.949 (más suave, correcto) ✓
     * A 120 FPS (dt=1/120): e^(-3.154/120) ≈ 0.974 (aún más suave) ✓
     * 
     * El comportamiento temporal ahora es independiente del framerate.
     */
    private static final double SLIDE_GROUND      = 0.9;
    
    /** 
     * Factor de deslizamiento en el aire (adimensional, frame-based @ 30 FPS).
     * 
     * SEMÁNTICA: velocidad *= 0.74 cada frame @ 30 FPS
     * 
     * CONVERSIÓN TEMPORAL (Physics2D.moveX()):
     *   k = -30 × ln(0.74) ≈ 9.060 (tasa de decay)
     *   damping_factor = e^(-k × deltaTime)
     *   v_new = v_old × damping_factor
     * 
     * A 30 FPS (dt=1/30): e^(-9.060/30) ≈ 0.740 (reproduce legacy) ✓
     * A 60 FPS (dt=1/60): e^(-9.060/60) ≈ 0.860 (más suave, correcto) ✓
     * 
     * El factor más bajo que SLIDE_GROUND proporciona más control aéreo
     * (deceleración más rápida sin input).
     */
    private static final double SLIDE_AIR         = 0.74;

    /** 
     * Aceleración base en el suelo en units/s².
     * DERIVACIÓN: 2.5 px/frame² @ 30 FPS → 2.5 × 30 = 75 units/s²
     */
    private static final double ACCEL_GROUND      = 75.0;
    
    /** 
     * Aceleración base en el aire en units/s².
     * DERIVACIÓN: 1.07 px/frame² @ 30 FPS → 1.07 × 30 = 32.1 units/s²
     */
    private static final double ACCEL_AIR         = 32.1;

    // ── Propiedades aerodinámicas (HRFC — Consolidación) ─────────────────
    // ── Mini-HRFC — Temporal Normalization ────────────────────────────────
    //
    // CORRECCIÓN DE DOCUMENTACIÓN:
    // Los coeficientes están calibrados para el sistema a 30 FPS.
    // La velocidad terminal emerge del balance F_gravity ≈ F_drag.
    //
    // Con gravity=23.4 units/s² (0.78 units/frame² @ 30 FPS),
    // mass=40.0, effectiveArea=0, dragCoefficient=9.0004:
    //   - Velocidad terminal emerge naturalmente del sistema
    //   - Comportamiento de caída natural y controlado
    //   - Sin límites artificiales
    //
    // NOTA: dragCoefficient y effectiveArea fueron calibrados empíricamente
    // para el sistema legacy a 30 FPS. Mantener estos valores preserva el
    // comportamiento de caída original.

    /** Área efectiva del jugador expuesta al flujo de aire. */
    private static final double PLAYER_EFFECTIVE_AREA = 0;

    /** 
     * Coeficiente de drag del jugador (calibrado empíricamente @ 30 FPS).
     * 
     * UNIDAD: Valor escalado para el sistema de juego (no Cd físico).
     * El cálculo usa: F_drag = dragCoefficient × effectiveArea × v²
     * (sin densidad del medio - ya absorbida en el coeficiente).
     */
    private static final double PLAYER_DRAG_COEFFICIENT = 9.0004;

    // ── Capacidades Físicas (HRFC — Motion Intent) ───────────────────────
    // HRFC: Kinetic Physics — separación de masa (propiedad física) y
    // fuerza/capacidad (habilidad muscular).

    /**
     * Altura base de salto del jugador en píxeles virtuales.
     * 
     * NOTA SOBRE GRAVEDAD Y SALTO:
     * El valor de gravedad en PlayerAssembler debe expresarse en units/s²,
     * no en units/frame². 
     * 
     * VALOR ACTUAL: gravity = 23.4 units/s²
     *   - Legacy: 0.78 units/frame² @ 30 FPS
     *   - Correcto: 0.78 × 30 = 23.4 units/s²
     * 
     * Con gravity=23.4 units/s² y baseJumpHeight=15:
     *   v₀ = sqrt(2 × g × h) = sqrt(2 × 23.4 × 15) = sqrt(702) ≈ 26.5 units/s
     * 
     * Verificación @ 30 FPS (dt=1/30s):
     *   Altura alcanzada: h = v₀²/(2g) = 26.5²/(2×23.4) = 702/46.8 ≈ 15 units ✓
     * 
     * El valor legacy physics.jump(10) era un impulso mal calibrado:
     *   v₀ = 10/mass = 10/40 = 0.25 units/frame @ 30 FPS
     *   Convertido: 0.25 × 30 = 7.5 units/s
     *   Altura: 7.5²/(2×23.4) ≈ 1.2 units (salto muy bajo)
     * 
     * Por tanto, baseJumpHeight=15 produce un salto mucho más alto que el
     * legacy jump(10), lo cual probablemente era intencional para corregir
     * un salto demasiado bajo.
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
     * 
     * ANÁLISIS DEL FACTOR 20.0:
     * 
     * El valor 20.0 parece ser un parámetro de "fuerza muscular" o capacidad
     * física del jugador que se combina con la masa para determinar cuánta
     * aceleración puede generar el jugador a partir de la aceleración base.
     * 
     * Con mass=40 y base=20.0:
     *   entityModifier = 20.0 / 40.0 = 0.5
     * 
     * Aplicado a ACCEL_GROUND=75 units/s²:
     *   mAccel = 75 × 0.5 = 37.5 units/s²
     * 
     * Conversión a legacy @ 30 FPS:
     *   37.5 units/s² / 30 = 1.25 px/frame²
     * 
     * Esto significa que aunque ACCEL_GROUND sea 2.5 px/frame² en el diseño,
     * la aceleración EFECTIVA del jugador era 1.25 px/frame² debido al
     * modificador de entidad.
     * 
     * INTERPRETACIÓN:
     * El factor 20.0 representa la "fuerza base" o "capacidad física" del jugador.
     * Es un parámetro de gameplay que define qué tan responsive es el movimiento.
     * 
     * Relación conceptual:
     *   F = strength × baseAccel
     *   a = F / mass = (strength × baseAccel) / mass
     *   
     * Donde strength=20.0 es la capacidad muscular del jugador.
     * 
     * ALTERNATIVA MÁS CLARA (no implementada):
     *   Podría expresarse como:
     *   - PLAYER_STRENGTH = 20.0
     *   - return PLAYER_STRENGTH / mass × (running ? RUN_BOOST : 1.0)
     * 
     * ESTADO: Documentado pero no modificado. El valor 20.0 es un parámetro
     * de gameplay calibrado. Cambiar requeriría retuning completo del movimiento.
     */
    @Override
    protected double computeEntityModifier(MovementContext ctx) {
        double base = PLAYER_STRENGTH / mass;
        return ctx.running() ? base * RUN_BOOST : base;
    }

    @Override
    public void moveX(double inputX, boolean onGround, boolean running, double deltaTime) {
        speedMaxPiso = running ? RUN_SPEED_GROUND : WALK_SPEED_GROUND;
        speedMaxAir  = running ? RUN_SPEED_AIR    : WALK_SPEED_AIR;

        // IMPORTANTE: asignar slide ANTES de super.moveX()
        slide = onGround ? SLIDE_GROUND : SLIDE_AIR;

        super.moveX(inputX, onGround, running, deltaTime);
    }
}
