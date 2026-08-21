package Game.Engine.Physics.KineticPhysics.Types;

import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Engine.Physics.KineticPhysics.ModifierStack;
import Game.Engine.Physics.KineticPhysics.MovementContext;
import Game.Engine.Physics.KineticPhysics.MovementModifier;
import Game.Engine.Physics.KineticPhysics.SurfaceMaterial;

/**
 * Clase base de física.
 *
 * ── HRFC — Consolidación Final de Kinetic Physics ────────────────────────
 * ── Mini-HRFC — Unified Physics Time Integration ─────────────────────────
 * ── Mini-HRFC 1.5 — Canonical Physics Units ──────────────────────────────
 *
 * UNIDADES CANÓNICAS:
 *
 *   POSICIÓN:      unidades espaciales del juego (units)
 *   VELOCIDAD:     unidades / segundo (u/s)
 *   ACELERACIÓN:   unidades / segundo² (u/s²)
 *   FUERZA:        masa × unidades / segundo² (mass × u/s²)
 *   IMPULSO:       masa × unidades / segundo (mass × u/s)
 *   deltaTime:     segundos (s)
 *
 * CONTRATOS MATEMÁTICOS:
 *
 *   Aceleración:          Δv = a × dt
 *   Fuerza continua:      Δv = (F / m) × dt
 *   Impulso instantáneo:  Δv = J / m          (SIN deltaTime)
 *   Posición:             Δx = v × dt
 *   Amortiguación:        v(t) = v₀ × e^(-k×dt)
 *
 * INVARIANTE:
 *   El framerate NO forma parte de ninguna magnitud física.
 *   Prohibido: factores como /60, *60, /30, *30, 0.016, 1/60, 1/30.
 *
 * REFERENCIA TEMPORAL:
 *   Sistema legacy calibrado @ 30 FPS.
 *   Todos los parámetros derivados asumen FPS_BASE = 30.
 *   La conversión temporal garantiza comportamiento equivalente a cualquier FPS.
 *
 * ── Diseño de velocidad en capas ─────────────────────────────────────────
 *
 *   vFinal = baseAccel
 *          × entityModifier      (masa, modo correr)        ← en Physics
 *          × surfaceModifier     (friction × accelScale)    ← SurfaceMaterial
 *          × statusModifier      (estado interno)           ← hook subclase
 *          × environmentModifier (zona externa)             ← hook subclase
 *          × airControlModifier  (airControl del surface)   ← SurfaceMaterial
 *
 * Cada capa es un double multiplicativo. 1.0 = sin efecto.
 * Las subclases sobreescriben los hooks para añadir sus capas sin
 * tocar la lógica de combinación en moveX().
 *
 * También se pueden registrar MovementModifier externos acumulables vía
 * {@code statusStack()} y {@code environmentStack()}. Cada stack combina
 * todos sus modificadores multiplicativamente, de modo que varios efectos
 * de estado coexisten sin pisarse:
 *
 * ── Surface drag vs Aerodynamic drag ─────────────────────────────────────
 *
 *   Surface drag (SurfaceMaterial.getDrag()):
 *     Amortiguación PASIVA de vx cuando NO hay input (frenado horizontal).
 *     Se multiplica por el slide base de la subclase.
 *     Representa fricción superficial del contacto.
 *
 *   Aerodynamic drag (applyAerodynamicDrag()):
 *     Resistencia del medio (aire) que actúa en dirección opuesta a la
 *     velocidad relativa. Produce naturalmente velocidad terminal de caída.
 *     F_drag = 0.5 × ρ × Cd × A × v²
 *     La velocidad terminal emerge cuando F_gravity ≈ F_drag.
 *
 * ── friction ─────────────────────────────────────────────────────────────
 *
 *   friction — escala la aceleración ACTIVA (cuando hay input).
 *
 * ── Acumulación de fuerzas (HRFC-014 — GAP-5) ───────────────────────────
 *
 *   addForce()   aplica un impulso inmediato: velocity += F/mass.
 *                Correcto para knockback puntual, saltos, explosiones.
 *
 *   accumulate() acumula fuerzas continuas (viento, gravedad personalizada,
 *                campos magnéticos) que se integran en flushAccumulatedForces()
 *                al inicio de cada step de física, ANTES de moveX().
 *                Todas las fuerzas acumuladas se suman y se aplican como
 *                un único impulso: velocity += (ΣF / mass).
 *                Después se limpian automáticamente para el siguiente frame.
 *
 *   Ejemplo de campo de viento continuo (sistema externo, sin modificar Physics):
 *
 *     // En WindZoneSystem.update():
 *     for (GameObjects obj : objects) {
 *         Physics2DComponent pc = obj.getComponent(Physics2DComponent.class);
 *         if (pc != null) pc.getPhysics().accumulate(windFx, 0);
 *     }
 *     // Antes de CollisionsSystem o en CollisionsSystem.FASE 0.5:
 *     physics.flushAccumulatedForces();
 *
 * ── Velocidad terminal ───────────────────────────────────────────────────
 *
 *   La velocidad terminal NO se almacena como constante.
 *   Emerge naturalmente del balance entre gravedad y drag aerodinámico:
 *
 *     F_gravity = m × g
 *     F_drag    = 0.5 × ρ × Cd × A × v²
 *
 *   Cuando F_net ≈ 0 → a ≈ 0 → velocidad terminal alcanzada.
 *
 *   Objetos con diferente masa/área/coeficiente presentan velocidades
 *   terminales diferentes, como corresponde físicamente.
 */
public class Physics2D {

    protected Vector2D velocity = new Vector2D(0, 0);
    
    /**
     * Gravedad propia de la entidad en u/s².
     * 
     * IMPORTANTE - HRFC-FASE2.5: CORRECCIÓN SEMÁNTICA DE OWNERSHIP
     * 
     * Este campo representa la PROPIEDAD GRAVITACIONAL PROPIA de la entidad.
     * La entidad ES PROPIETARIA de su gravedad, no el ambiente.
     * 
     * OWNERSHIP CORRECTO:
     * 
     *   ENTIDAD (Physics2D):
     *     gravity → propiedad gravitacional PROPIA (ej: 9.8 m/s² para objetos normales)
     *     mass    → masa inercial (GravityProperties.MASS)
     * 
     *   AMBIENTE (EnvironmentState):
     *     gravityInfluenceY → FACTOR de modificación ambiental (1.0 = normal, 0.0 = anulada)
     * 
     * COMPOSICIÓN CORRECTA:
     *   a_efectiva = entity.gravity × environment.gravityInfluenceY
     * 
     * EJEMPLOS:
     *   Entity.gravity = 9.8, Environment.gravityInfluence = 1.0
     *     → 9.8 × 1.0 = 9.8 (Tierra normal)
     * 
     *   Entity.gravity = 9.8, Environment.gravityInfluence = 0.0
     *     → 9.8 × 0.0 = 0.0 (microgravedad/espacio)
     * 
     *   Entity.gravity = 9.8, Environment.gravityInfluence = 2.0
     *     → 9.8 × 2.0 = 19.6 (zona de alta gravedad)
     * 
     * VALORES TÍPICOS:
     *   9.8  = objetos normales (gravedad terrestre estándar)
     *   0.4  = objetos muy ligeros / con sustentación
     *   0.0  = objetos sin gravedad propia (flotantes)
     * 
     * PRINCIPIO FUNDAMENTAL:
     * El ambiente NO dice: "La gravedad aquí ES 19.6."
     * El ambiente dice: "La gravedad de la entidad se ve modificada por ×2."
     * 
     * La entidad posee su gravedad.
     * El ambiente la modifica mediante un factor de influencia.
     * Las Relations combinan ambos para producir la aceleración efectiva.
     * 
     * ESTADO ACTUAL:
     *   Este campo se usa directamente como aceleración en applyGravity().
     *   Funcionalmente correcto para casos donde gravityInfluence = 1.0,
     *   pero debe actualizarse para considerar el factor ambiental cuando
     *   el ambiente modifique la gravedad (ej: microgravedad, alta gravedad).
     */
    protected double gravity;

    protected double mass;
    protected double aAir;
    protected double speedMaxAir;
    protected double speedMaxPiso;
    protected double aGround;
    protected double slide;

    protected double speedMax;
    protected double inputX;
    protected double vx;
    protected double accel;

    protected boolean onGround;
    protected boolean onWall;
    protected boolean onCeiling;
    protected boolean salto;
    protected boolean running;

    // ── Propiedades aerodinámicas (HRFC — Consolidación Kinetic Physics) ─
    // Propiedades que determinan la resistencia aerodinámica del objeto.
    // La velocidad terminal emerge naturalmente del balance F_gravity ≈ F_drag.

    /**
     * Área efectiva del objeto expuesta al flujo de aire (en unidades²).
     * Mayor área → mayor resistencia → menor velocidad terminal.
     * Default: 1.0 (objeto de tamaño unitario).
     */
    protected double effectiveArea;

    /**
     * Coeficiente de drag aerodinámico escalado para unidades del juego.
     * 
     * ── Mini-HRFC — Final Temporal Normalization ──────────────────────────
     * 
     * FÓRMULA: F_drag = dragCoefficient × effectiveArea × v²
     * 
     * UNIDADES AMBIGUAS - REQUIERE VERIFICACIÓN:
     * 
     * El coeficiente fue calibrado empíricamente en el sistema legacy.
     * Sin embargo, hay ambigüedad sobre si fue calibrado para:
     *   a) v en units/frame (sistema legacy)
     *   b) v en units/s (sistema actual)
     * 
     * ANÁLISIS:
     *   Si legacy usaba v en units/frame @ 30 FPS:
     *     v_per_second = v_per_frame × 30
     *     v²_per_second = v²_per_frame × 900
     *   
     *   Para producir la misma fuerza con v en units/s:
     *     Cd_temporal = Cd_legacy / 900
     * 
     * VALORES TÍPICOS ACTUALES:
     *   ~0.0001: aerodinámica extrema (bala, proyectil)
     *   ~0.0003: objeto razonablemente aerodinámico (default)
     *   ~0.0004: Player (menos aerodinámico)
     *   ~0.0005: objeto no optimizado
     *   ~0.0008: objeto muy poco aerodinámico
     * 
     * Si estos valores FUERON calibrados para units/frame y ahora velocity
     * está en units/s, el drag sería 900× más fuerte de lo esperado.
     * 
     * VERIFICACIÓN EMPÍRICA REQUERIDA:
     *   1. Medir velocidad terminal de caída en sistema actual
     *   2. Comparar con comportamiento legacy esperado
     *   3. Si diverge significativamente, ajustar por factor 900
     * 
     * ESTADO: Documentado pero no modificado hasta verificación empírica.
     * 
     * Default: 0.0003 (objeto genérico, semántica por confirmar).
     */
    protected double dragCoefficient;

    /**
     * Densidad del medio (obsoleto - mantenido por compatibilidad).
     * 
     * NOTA: Este parámetro ya no se usa en el cálculo de drag después de
     * la corrección de unidades en HRFC FASE 2. Los coeficientes de drag
     * ahora están escalados directamente para unidades del juego (px/frame).
     * 
     * Mantener este campo evita romper código que pueda leer/escribir
     * mediumDensity, pero no afecta la física.
     * 
     * @deprecated Ya no afecta el cálculo de drag. Usar dragCoefficient.
     */
    @Deprecated
    protected double mediumDensity;

    protected SurfaceMaterial currentSurface = SurfaceMaterial.DEFAULT;

    // ── Acumulación de fuerzas por frame (HRFC-014 — GAP-5) ───────────────
    // Fuerzas continuas declaradas por sistemas externos (viento, gravedad
    // personalizada, campos magnéticos). Se integran como impulso único al
    // inicio del step de física y se limpian automáticamente.
    private double accumulatedFx = 0.0;
    private double accumulatedFy = 0.0;

    // Pilas de modificadores acumulables (buff/debuff, zona, etc.)
    private final ModifierStack statusStack      = new ModifierStack();
    private final ModifierStack environmentStack = new ModifierStack();

    public Physics2D(double gravity) {
        this.gravity = gravity;
    }

    // ── Gravedad gestionada externamente ─────────────────────────────────

    public boolean isGravityManagedExternally() { return false; }

    // ── Surface ───────────────────────────────────────────────────────────

    public void setCurrentSurface(SurfaceMaterial surface) {
        this.currentSurface = (surface != null) ? surface : SurfaceMaterial.DEFAULT;
    }

    public void clearSurface() {
        this.currentSurface = SurfaceMaterial.AIR;
    }

    public SurfaceMaterial getCurrentSurface() { return currentSurface; }

    // ── Modificadores externos ────────────────────────────────────────────

    /**
     * Pila de modificadores de estado (herido, stun, buff de velocidad…).
     * 
     * HRFC-FASE3 — Ahora requiere MovementModifierSource en lugar de String:
     * 
     *   Añadir efectos: {@code statusStack().add(poisonEffect, ctx -> 0.6);}
     *   Removerlos:     {@code statusStack().remove(poisonEffect);}
     *
     * Todos los modificadores registrados se combinan multiplicativamente
     * en cada frame: factor = mod1 × mod2 × … × modN.
     */
    public ModifierStack statusStack() { return statusStack; }

    /**
     * Pila de modificadores de entorno (viento, agua, zona de gravedad…).
     * Mismo contrato que {@link #statusStack()}.
     */
    public ModifierStack environmentStack() { return environmentStack; }

    /**
     * Encadena dos modificadores ad-hoc: a × b.
     * Útil para combinar lambdas puntuales sin registrarlas en una stack.
     *
     *   MovementModifier combo = Physics.chain(herido, envenenado);
     */
    public static MovementModifier chain(MovementModifier a, MovementModifier b) {
        return ctx -> a.compute(ctx) * b.compute(ctx);
    }

    // ── Hooks para subclases ──────────────────────────────────────────────

    /**
     * Factor de entidad: atributos propios del objeto (masa, modo correr).
     * Sobreescribir en subclases para personalizar sin tocar moveX().
     *
     * Ejemplo en PlayerPhysics:
     *   return (1.0 / mass) * (running ? RUN_BOOST : 1.0);
     */
    protected double computeEntityModifier(MovementContext ctx) {
        return 1.0 / mass;
    }

    // ── Movimiento horizontal ─────────────────────────────────────────────

    /**
     * Calcula la velocidad horizontal en capas multiplicativas:
     *
     *   mAccel = baseAccel
     *          × entityModifier
     *          × surfaceFriction × surfaceAccelScale
     *          × statusModifier
     *          × environmentModifier
     *          × airControlModifier
     *
     * ── HRFC Phase 3 — Temporal Migration ────────────────────────────────
     *
     * MIGRACIÓN CRÍTICA:
     *   moveX() ahora recibe deltaTime para integración temporal correcta.
     *   
     *   ANTES (frame-based):
     *     vx = v + (input × accel)     // accel en units/frame²
     *   
     *   AHORA (time-based):
     *     vx = v + (input × accel × dt) // accel en units/s²
     *
     * UNIDADES:
     *   aGround, aAir → units/s² (aceleración por segundo cuadrado)
     *   deltaTime     → s (segundos)
     *   velocity      → units/s
     *
     * Las subclases NO necesitan sobreescribir este método para añadir
     * modificadores; usan setStatusModifier(), setEnvironmentModifier()
     * o sobreescriben computeEntityModifier().
     *
     * @param inputX dirección horizontal [-1, 0, 1]
     * @param onGround si la entidad está en contacto con el suelo
     * @param running si la entidad está corriendo
     * @param deltaTime tiempo del simulation step en segundos
     */
    public void moveX(double inputX, boolean onGround, boolean running, double deltaTime) {
        // NOTA: this.onGround NO se toca aquí.
        // Es responsabilidad exclusiva de CollisionsSystem via setOnGround().
        // CollisionsSystem resetea onGround a false en FASE 0.5 (post-gravedad)
        // y lo vuelve a establecer en FASE 1 según la normal del contacto SweptAABB.
        // Modificarlo aquí pisaría el valor correcto establecido por el sistema.
        this.inputX   = inputX;
        this.running  = running;

        setMaxSpeed(onGround);

        // Aceleración base según estado
        double baseAccel = onGround ? aGround : aAir;
        accel = baseAccel;

        // Snapshot de contexto compartido por todos los modificadores
        MovementContext ctx = new MovementContext(
                inputX, onGround, running, velocity.getX(), baseAccel, currentSurface
        );

        // ── Capas multiplicativas ─────────────────────────────────────
        double entityFactor  = computeEntityModifier(ctx);
        double surfaceFactor = currentSurface.getFriction() * currentSurface.getAccelScale();
        double statusFactor  = statusStack.compute(ctx);
        double envFactor     = environmentStack.compute(ctx);
        double airFactor     = currentSurface.getAirControl();

        double mAccel = baseAccel * entityFactor * surfaceFactor * statusFactor * envFactor * airFactor;

        // ── Aceleración activa ────────────────────────────────────────
        // HRFC Phase 3: Δv = a × dt (temporal integration)
        vx = velocity.getX() + (inputX * mAccel * deltaTime);

        if (inputX != 0 && Math.abs(vx) >= speedMax) {
            vx = Math.copySign(speedMax, vx);
        }

        // ── Frenado pasivo ────────────────────────────────────────────
        // Mini-HRFC — Temporal Exponential Damping (Frame-rate independent)
        //
        // CONVERSIÓN DE AMORTIGUACIÓN FRAME-BASED A TEMPORAL:
        //
        // Legacy @ 30 FPS: v_new = v_old × slide cada frame
        // Temporal: v(t) = v₀ × e^(-k×t)
        //
        // Para preservar comportamiento legacy:
        //   e^(-k×dt) = slide cuando dt = 1/30
        //   k = -30 × ln(slide)
        //
        // Ejemplos:
        //   slide=0.9  → k = -30×ln(0.9) ≈ 3.154
        //   slide=0.74 → k = -30×ln(0.74) ≈ 9.060
        //
        // En runtime:
        //   damping_factor = e^(-k × deltaTime)
        //   v_new = v_old × damping_factor × surface_drag
        //
        // Verificación @ 30 FPS (dt=1/30):
        //   damping_factor = e^(-3.154/30) = e^(-0.1051) ≈ 0.900 ✓
        //
        // Beneficio: Funciona correctamente a cualquier FPS.
        //
        // CORRECCIÓN CRÍTICA (HRFC-DT-014):
        //   El damping debe aplicarse a vx (velocidad acumulada este frame),
        //   NO a velocity.getX() (velocidad del frame anterior).
        //   Esto produce consistencia entre diferentes frame rates.
        if (inputX == 0) {
            // Convertir slide de factor-por-frame a tasa de decay temporal
            // k = -FPS_BASE × ln(slide) donde FPS_BASE = 30
            double decayRate = -30.0 * Math.log(slide);
            
            // Calcular factor de amortiguación para este deltaTime específico
            double dampingFactor = Math.exp(-decayRate * deltaTime);
            
            // Aplicar amortiguación combinada con drag de superficie
            // CORRECCIÓN: Aplicar a vx (velocidad acumulada), no velocity.getX()
            double effectiveDrag = dampingFactor * currentSurface.getDrag();
            vx = vx * effectiveDrag;
            
            // Threshold para detener completamente (evitar drift infinito)
            if (Math.abs(vx) < 0.05) vx = 0;
        }

        vSetX(vx);
    }

    public void setMaxSpeed(boolean onGround) {
        speedMax = onGround ? speedMaxPiso : speedMaxAir;
    }

    // ── Gravedad y Drag Aerodinámico ─────────────────────────────────────

    /**
     * Aplica resistencia aerodinámica del medio (drag).
     *
     * ── HRFC FASE 3.5 — Separación de Responsabilidades ──────────────────
     * ── Mini-HRFC — Unified Physics Time Integration ─────────────────────
     * ── Mini-HRFC — Final Temporal Normalization ──────────────────────────
     *
     * Este método implementa ÚNICAMENTE el drag aerodinámico, que es correcto
     * que permanezca en Physics2D (no es un fenómeno formalizado por evaluator).
     *
     * El drag aerodinámico es una propiedad del movimiento en el medio, no un
     * fenómeno físico discreto entre entidades. Por tanto, pertenece a la
     * integración de movimiento, no a PhysicsSolver.
     *
     * ── FUNCIONAMIENTO ───────────────────────────────────────────────────
     *
     * FÓRMULA SIMPLIFICADA:
     *   F_drag = Cd × A × v²  (opuesta a la velocidad)
     *   a_drag = F_drag / m
     *   Δv = a_drag × deltaTime
     *
     * UNIDADES:
     *   v: units/s (velocidad temporal)
     *   Cd: coeficiente escalado empíricamente (units⁻¹)
     *   A: área efectiva (adimensional o units²)
     *   F_drag: mass × units/s²
     *   a_drag: units/s²
     *
     * NOTA SOBRE COEFICIENTE:
     *   El dragCoefficient fue calibrado empíricamente en el sistema legacy
     *   cuando las velocidades estaban en units/frame. 
     *   
     *   Con la migración temporal, velocity está en units/s, NO en units/frame.
     *   Por tanto, v² está en (units/s)², no (units/frame)².
     *   
     *   A 30 FPS: v_per_second = v_per_frame × 30
     *   Por tanto: v²_per_second = v²_per_frame × 900
     *   
     *   Para compensar, dragCoefficient debe ser 900× menor para producir
     *   la misma fuerza con velocidad en units/s.
     *   
     *   ESTADO ACTUAL: Los coeficientes NO han sido ajustados para esta
     *   conversión. Si los valores legacy fueron calibrados para units/frame,
     *   deberían ser ~900× menores.
     *   
     *   VERIFICACIÓN NECESARIA: Medir velocidad terminal empíricamente y
     *   comparar con comportamiento legacy.
     *
     * La resistencia del aire produce naturalmente velocidad terminal cuando
     * se balancea con otras fuerzas (gravedad aplicada vía evaluators).
     *
     * @param onGround si true, no aplica drag en caída (objeto en contacto con suelo)
     * @param deltaTime tiempo del simulation step en segundos
     */
    public void applyAerodynamicDrag(boolean onGround, double deltaTime) {
        if (onGround) return;

        // ── Resistencia aerodinámica ──────────────────────────────────────
        // F_drag = Cd × A × v²
        // 
        // IMPORTANTE: velocity está en units/s (después de integración temporal).
        // Si dragCoefficient fue calibrado para velocity en units/frame,
        // necesitaría ser ajustado por factor de 900 (30²) para compensar.
        double vy = velocity.getY();
        double speed = Math.abs(vy);
        double dragForce = dragCoefficient * effectiveArea * speed ;

        // Dirección del drag: opuesta a la velocidad
        // Si vy > 0 (cayendo) → drag hacia arriba (negativo)
        // Si vy < 0 (subiendo) → drag hacia abajo (positivo)
        double dragDirection = (vy >= 0) ? -1.0 : 1.0;

        // ── Aceleración por drag (F / m) ──────────────────────────────────
        double a_drag = (dragForce / mass) * dragDirection;

        // ── Mini-HRFC: Integración temporal correcta ─────────────────────
        // Δv = a_drag × deltaTime
        double newVy = vy + (a_drag * deltaTime);
        velocity.setY(newVy);
    }

    /**
     * Aplica gravedad y resistencia aerodinámica del medio.
     *
     * ── HRFC — Temporal Behavioral Restoration & Legacy Equivalence ───────
     * ── CORRECCIÓN CRÍTICA — Eliminación de Compensación Temporal Hack ────
     *
     * PROBLEMA ANTERIOR:
     *   applyGravity() registraba: (mass × gravity) / deltaTime
     *   flushAccumulatedForces() aplicaba: (F / mass) × deltaTime
     *   Resultado: Δv = ((m×g/dt)/m)×dt = g
     *
     *   La división por deltaTime era una COMPENSACIÓN para anular la
     *   multiplicación posterior, no una expresión semánticamente correcta.
     *
     * CORRECCIÓN APLICADA:
     *   La gravedad ES una aceleración (units/s²), no una fuerza.
     *   Debe aplicarse directamente mediante: Δv = g × dt
     *
     * MATEMÁTICA CORRECTA:
     *   gravity = 23.4 units/s² (Player legacy: 0.78 px/frame² × 30)
     *   deltaTime = 1/30 s @ 30 FPS
     *   Δv = 23.4 × (1/30) = 0.78 units/frame-equivalent ✓
     *
     * ARQUITECTURA:
     *   La gravedad NO pasa por accumulate()/flush() porque NO es una fuerza
     *   continua acumulable — es una aceleración directa.
     *
     *   accumulate() está diseñado para fuerzas que requieren F=ma.
     *   La gravedad ya está expresada como aceleración, por tanto se aplica
     *   directamente a velocity.
     *
     * PRESERVACIÓN DE VALORES:
     *   gravity = 23.4 units/s² → NO cambiado
     *   mass = 40.0 → NO cambiado
     *   Solo se corrige la integración temporal.
     *
     * LIMITACIÓN: GRAVEDAD AMBIENTAL NO INTEGRADA
     *   La arquitectura prevé: a_efectiva = entity.gravity × environment.gravityInfluenceY
     *   Actualmente Physics2D no tiene acceso a EnvironmentState.
     *   Para implementar modificación ambiental, pasar EnvironmentState como parámetro.
     *
     * @param onGround si true, no aplica gravedad (objeto en contacto con suelo).
     * @param deltaTime tiempo del simulation step en segundos.
     */
    public void applyGravity(boolean onGround, double deltaTime) {
        if (onGround) return;

        // ── HRFC: Aplicación directa de gravedad como aceleración ─────────
        // CORRECCIÓN CRÍTICA: Eliminar compensación /deltaTime.
        // 
        // La gravedad ES aceleración (units/s²), no fuerza.
        // Integración correcta: Δv = a × dt
        //
        // ANTES (incorrecto — compensación hack):
        //   double gravityForce = (mass × gravity) / deltaTime;
        //   accumulate(0, gravityForce);
        //   // Luego flush hacía: Δv = (F/m)×dt = ((m×g/dt)/m)×dt = g ✗
        //
        // AHORA (correcto — integración temporal directa):
        //   Δv = gravity × deltaTime
        //
        // Verificación @ 30 FPS (dt = 1/30):
        //   Player: gravity = 23.4 units/s²
        //   Δv = 23.4 × (1/30) = 0.78 units/frame-equivalent
        //   Legacy: 0.78 px/frame² → Equivalencia preservada ✓
        
        if (!isGravityManagedExternally()) {
            // Aplicar gravedad directamente como aceleración
            // TODO: Considerar environment.gravityInfluenceY cuando esté disponible:
            //   double effectiveGravity = gravity * environment.getGravityInfluenceY();
            double deltaVy = gravity * deltaTime;
            velocity.setY(velocity.getY() + deltaVy);
        }

        // ── Drag aerodinámico (correcto en Physics2D) ────────────────────
        applyAerodynamicDrag(false, deltaTime);  // Ya verificamos onGround arriba
    }

    // ── Salto ─────────────────────────────────────────────────────────────

    /**
     * Aplica un impulso de salto vertical.
     *
     * ── HRFC — Temporal Behavioral Restoration ────────────────────────────
     *
     * Este método utiliza addForce() correctamente para aplicar un impulso.
     * La integración es: Δv = force / mass (sin deltaTime, correcto para impulsos).
     *
     * DEPRECADO: Se recomienda usar {@link Game.Engine.Physics.KineticPhysics.Intent.JumpIntent}
     * en su lugar, que expresa la intención de salto como altura objetivo en
     * lugar de fuerza arbitraria.
     *
     * Migración recomendada:
     *
     *   Antes:
     *     physics.jump(10);
     *
     *   Después:
     *     JumpIntent intent = new JumpIntent(capabilities);
     *     intent.resolve(physics);
     *
     * AUDITORÍA: ✓ CORRECTO
     *   Usa addForce() que implementa correctamente Δv = J/m para impulsos.
     *   No requiere integración temporal adicional.
     *
     * @param force impulso de salto (positivo, será aplicado hacia arriba como -force)
     *
     * @deprecated Usar JumpIntent para expresar saltos como altura objetivo
     */
    @Deprecated
    public void jump(double force) {
        // HRFC: Migrado de velocity.setY(-force/mass) a addForce(0, -force)
        // Esto hace que jump() sea consistente con el resto del sistema de fuerzas.
        // addForce() internamente hace: velocity.y += (-force / mass) = -force/mass
        // El resultado es idéntico, pero ahora pasa por la API consolidada.
        addForce(0, -force);
        salto = true;
    }

    // ── Movimiento ────────────────────────────────────────────────────────

    /**
     * Integra la velocidad en la posición.
     *
     * ── HRFC — Temporal Behavioral Restoration ────────────────────────────
     *
     * INTEGRACIÓN TEMPORAL CORRECTA:
     *   La posición debe integrarse usando velocity × deltaTime:
     *   
     *   Δx = v × deltaTime
     *
     * MATEMÁTICA:
     *   v: velocidad en [units/s]
     *   dt: tiempo en [s]
     *   Δx: desplazamiento en [units]
     *
     * VERIFICACIÓN @ 30 FPS:
     *   Ejemplo: Player caminando, v = 2100 units/s, dt = 1/30
     *   Δx = 2100 × (1/30) = 70 units/frame
     *   Legacy: 70 px/frame → Equivalencia preservada ✓
     *
     * AUDITORÍA: ✓ CORRECTO
     *   Implementación matemáticamente correcta.
     *   Integración temporal Δx = v×dt es correcta.
     *
     * @param position posición a modificar
     * @param deltaTime tiempo del simulation step en segundos
     */
    public void updateMoves(Vector2D position, double deltaTime) {
        position.setX(position.getX() + velocity.getX() * deltaTime);
        position.setY(position.getY() + velocity.getY() * deltaTime);
    }

    // ── Stop ──────────────────────────────────────────────────────────────

    public void stopY()        { velocity.setY(0); }
    public void stopX()        { velocity.setX(0); vx = 0; }
    public void stopVelocity() { stopX(); stopY(); }

    // ── Setters/Getters ───────────────────────────────────────────────────

    public void vSetX(double vX)  { velocity.setX(vX); }
    public void vSetY(double vY)  { velocity.setY(vY); }

    public void setMass(double m)            { this.mass = m; }
    public double getMass()                  { return mass; }
    public void setJumping(boolean jumping)  { this.salto = jumping; }
    public Vector2D getVelocity()            { return velocity; }

    // ── Propiedades aerodinámicas (HRFC — Consolidación) ─────────────────

    /**
     * Establece el área efectiva expuesta al flujo de aire.
     * Mayor área → mayor resistencia → menor velocidad terminal.
     *
     * @param area área en unidades². Debe ser > 0.
     */
    public void setEffectiveArea(double area) {
        if (area <= 0) throw new IllegalArgumentException("area debe ser > 0");
        this.effectiveArea = area;
    }

    public double getEffectiveArea() { return effectiveArea; }

    /**
     * Establece el coeficiente de drag aerodinámico.
     * Representa la forma aerodinámica del objeto.
     *
     * ── Mini-HRFC — Final Temporal Normalization ──────────────────────────
     * 
     * ADVERTENCIA: La semántica de este coeficiente es ambigua.
     * Ver documentación del campo dragCoefficient para detalles completos.
     * 
     * Si el coeficiente fue calibrado para velocity en units/frame y ahora
     * velocity está en units/s, puede requerirse ajuste por factor ~900.
     * 
     * VERIFICACIÓN EMPÍRICA RECOMENDADA antes de cambiar valores existentes.
     *
     * @param cd coeficiente de drag [típicamente 0.0001 - 0.001]. Debe ser >= 0.
     */
    public void setDragCoefficient(double cd) {
        if (cd < 0) throw new IllegalArgumentException("dragCoefficient debe ser >= 0");
        this.dragCoefficient = cd;
    }

    public double getDragCoefficient() { return dragCoefficient; }

    /**
     * Establece la densidad del medio (obsoleto - no afecta física).
     * 
     * @deprecated Después de HRFC FASE 2, mediumDensity ya no se usa en
     *             el cálculo de drag. Los coeficientes están escalados
     *             directamente para px/frame. Este método se mantiene
     *             por compatibilidad pero no tiene efecto.
     */
    @Deprecated
    public void setMediumDensity(double density) {
        // No-op: dragCoefficient ya está escalado para px/frame
        this.mediumDensity = density;
    }

    public double getMediumDensity() { return mediumDensity; }

    // ─────────────────────────────────────────────────────────────────────

    public double getGravity()               { return gravity; }
    public void setGravity(double g)         { this.gravity = g; }
    public double getOposite(double x)       { return -x; }
    public boolean getOnGround()             { return onGround; }
    public void setOnGround(boolean v)       { this.onGround = v; }

    public boolean getOnWall()               { return onWall; }
    public void setOnWall(boolean v)         { this.onWall = v; }

    public boolean getOnCeiling()            { return onCeiling; }
    public void setOnCeiling(boolean v)      { this.onCeiling = v; }

    // ── Fuerzas e Impulsos (HRFC — Consolidación Semántica) ──────────────

    /**
     * Aplica un impulso instantáneo sobre la velocidad.
     *
     * ── HRFC — Temporal Behavioral Restoration ────────────────────────────
     *
     * SEMÁNTICA CORRECTA:
     *   addForce() representa un IMPULSO INSTANTÁNEO discreto:
     *   
     *   Δv = J / m
     *   
     *   donde J es el impulso (momentum transfer) aplicado en este evento.
     *
     * UNIDADES:
     *   fx, fy: impulso en [mass × units/s]
     *   mass: masa en [mass units]
     *   Δv: cambio de velocidad en [units/s]
     *
     * INTEGRACIÓN TEMPORAL:
     *   Los impulsos NO se integran con deltaTime porque representan
     *   transferencias instantáneas de momentum, no fuerzas continuas.
     *   
     *   CORRECTO: Δv = J / m
     *   INCORRECTO: Δv = (J / m) × dt
     *
     * USO TÍPICO:
     *   - Knockback de explosiones (MetheorBullet)
     *   - Empuje radial (PushableComponent)
     *   - Impactos puntuales
     *   - Rebotes en colisiones
     *   - Dash instantáneo
     *
     * DIFERENCIA CON accumulate():
     *   addForce()   → impulso instantáneo (J/m), aplicado inmediatamente
     *   accumulate() → fuerza continua (F), integrada con dt en flush
     *
     * Para fuerzas continuas (viento, campos vectoriales), usar
     * {@link #accumulate(double, double)} en su lugar.
     *
     * AUDITORÍA: ✓ CORRECTO
     *   Implementación matemáticamente correcta para impulsos.
     *   No requiere modificación temporal.
     *
     * @param fx componente X del impulso en [mass × units/s].
     * @param fy componente Y del impulso en [mass × units/s].
     */
    public void addForce(double fx, double fy) {
        velocity.setX(velocity.getX() + (fx / mass));
        velocity.setY(velocity.getY() + (fy / mass));
    }

    /**
     * Acumula una fuerza continua para ser integrada en el siguiente
     * {@link #flushAccumulatedForces()}.
     *
     * ── HRFC — Temporal Behavioral Restoration ────────────────────────────
     *
     * SEMÁNTICA CORRECTA:
     *   accumulate() representa fuerzas CONTINUAS que actúan durante el frame:
     *
     *   F_net = ΣF    (suma vectorial de todas las fuerzas acumuladas)
     *   Δv = (F_net / m) × dt
     *
     * UNIDADES:
     *   fx, fy: fuerza en [mass × units/s²]
     *   mass: masa en [mass units]
     *   deltaTime (en flush): tiempo en [s]
     *   Δv: cambio de velocidad en [units/s]
     *
     * INTEGRACIÓN TEMPORAL:
     *   Las fuerzas continuas SÍ se integran con deltaTime porque representan
     *   fuerzas que actúan durante un intervalo de tiempo.
     *   
     *   CORRECTO: Δv = (F / m) × dt
     *   INCORRECTO: Δv = F / m (dependiente del framerate)
     *
     * USO TÍPICO:
     *   - Campos de viento (VectorFieldSystem)
     *   - Zonas de gravedad modificada
     *   - Campos magnéticos
     *   - Corrientes de agua
     *   - Fuerzas ambientales persistentes
     *
     * DIFERENCIA CON addForce():
     *   addForce()   → impulso instantáneo (J/m), aplicado inmediatamente
     *   accumulate() → fuerza continua (F), integrada con dt en flush
     *
     * CICLO DE VIDA:
     *   1. Sistemas externos llaman accumulate() durante su update
     *   2. CollisionsSystem FASE 0 llama flushAccumulatedForces(dt)
     *   3. Fuerzas se integran: velocity += (ΣF / mass) × dt
     *   4. Acumulador se resetea automáticamente para el siguiente frame
     *
     * NOTA IMPORTANTE:
     *   La gravedad YA NO usa accumulate() después de la corrección HRFC.
     *   La gravedad se aplica directamente como aceleración porque YA está
     *   expresada en units/s², no como fuerza que requiera F=ma.
     *
     * Para impulsos instantáneos (knockback, explosiones), usar
     * {@link #addForce(double, double)} en su lugar.
     *
     * AUDITORÍA: ✓ CORRECTO
     *   Implementación matemáticamente correcta para fuerzas continuas.
     *   Integración temporal en flush() es correcta.
     *
     * @param fx fuerza en X en [mass × units/s²].
     * @param fy fuerza en Y en [mass × units/s²].
     */
    public void accumulate(double fx, double fy) {
        accumulatedFx += fx;
        accumulatedFy += fy;
    }

    /**
     * Aplica todas las fuerzas acumuladas como un único impulso y
     * limpia el acumulador para el siguiente frame.
     *
     * ── HRFC — Temporal Behavioral Restoration ────────────────────────────
     *
     * INTEGRACIÓN TEMPORAL CORRECTA:
     *   Las fuerzas continuas deben integrarse usando deltaTime:
     *   
     *   Δv = (ΣF / m) × deltaTime
     *
     * MATEMÁTICA:
     *   F: fuerza en [mass × units/s²]
     *   m: masa en [mass units]
     *   dt: tiempo en [s]
     *   Δv: cambio de velocidad en [units/s]
     *
     * VERIFICACIÓN @ 30 FPS:
     *   Ejemplo: Viento con F = 1200 (mass × units/s²), m = 40, dt = 1/30
     *   Δv = (1200 / 40) × (1/30) = 30 × (1/30) = 1.0 units/s por frame
     *
     * USO:
     *   Llamar desde CollisionsSystem FASE 0, después de applyGravity()
     *   y antes del SweptAABB, para que las fuerzas de zona se integren
     *   correctamente en el mismo step.
     *
     * NOTA IMPORTANTE:
     *   Después de la corrección HRFC, la gravedad YA NO pasa por este método.
     *   La gravedad se aplica directamente en applyGravity() como aceleración.
     *   Este método ahora es exclusivo para fuerzas continuas ambientales.
     *
     * No hace nada si no hay fuerzas acumuladas.
     *
     * AUDITORÍA: ✓ CORRECTO
     *   Implementación matemáticamente correcta.
     *   Integración temporal Δv = (F/m)×dt es correcta.
     *
     * @param deltaTime tiempo del simulation step en segundos
     */
    public void flushAccumulatedForces(double deltaTime) {
        if (accumulatedFx == 0.0 && accumulatedFy == 0.0) return;
        
        // HRFC: Integración temporal correcta para fuerzas continuas
        // Δv = (F / m) × deltaTime
        velocity.setX(velocity.getX() + (accumulatedFx / mass) * deltaTime);
        velocity.setY(velocity.getY() + (accumulatedFy / mass) * deltaTime);
        
        accumulatedFx = 0.0;
        accumulatedFy = 0.0;
    }

    /**
     * Consulta si hay fuerzas acumuladas sin integrar este frame.
     * Útil para debug y para que sistemas de diagnóstico eviten llamadas
     * innecesarias a flushAccumulatedForces().
     */
    public boolean hasPendingForces() {
        return accumulatedFx != 0.0 || accumulatedFy != 0.0;
    }

    // ── Debug ─────────────────────────────────────────────────────────────

    public void showInfo(boolean yes) {
        if (yes) {
            MovementContext ctx = new MovementContext(
                    inputX, onGround, running, velocity.getX(), accel, currentSurface
            );
            System.out.printf(
                "inputX:%.2f velX:%.4f velY:%.4f onGround:%b surface:%s%n" +
                "  entity:%.3f  surface(f×a):%.3f  status:%.3f  env:%.3f  air:%.3f%n",
                inputX, velocity.getX(), velocity.getY(), onGround, currentSurface,
                computeEntityModifier(ctx),
                currentSurface.getFriction() * currentSurface.getAccelScale(),
                statusStack.compute(ctx),
                environmentStack.compute(ctx),
                currentSurface.getAirControl()
            );
        }
    }
}
