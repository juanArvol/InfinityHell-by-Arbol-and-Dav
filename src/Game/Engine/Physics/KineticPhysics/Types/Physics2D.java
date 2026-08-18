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
 *
 * INVARIANTE:
 *   El framerate NO forma parte de ninguna magnitud física.
 *   Prohibido: factores como /60, *60, /FPS, *FPS, 0.016, 1/60.
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
    protected double effectiveArea = 1.0;

    /**
     * Coeficiente de drag aerodinámico escalado para unidades del juego (px/frame).
     * Representa la forma aerodinámica del objeto:
     *   ~0.0001: aerodinámica extrema (bala, proyectil)
     *   ~0.0003: objeto razonablemente aerodinámico
     *   ~0.0005: objeto no optimizado (caja, persona)
     *   ~0.0008: objeto muy poco aerodinámico
     * Default: 0.0003 (objeto genérico, escalado para px/frame).
     */
    protected double dragCoefficient = 0.0003;

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
    protected double mediumDensity = 1.0;

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
     * Las subclases NO necesitan sobreescribir este método para añadir
     * modificadores; usan setStatusModifier(), setEnvironmentModifier()
     * o sobreescriben computeEntityModifier().
     */
    public void moveX(double inputX, boolean onGround, boolean running) {
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
        vx = velocity.getX() + (inputX * mAccel);

        if (inputX != 0 && Math.abs(vx) >= speedMax) {
            vx = Math.copySign(speedMax, vx);
        }

        // ── Frenado pasivo ────────────────────────────────────────────
        if (inputX == 0) {
            double effectiveDrag = slide * currentSurface.getDrag();
            vx = velocity.getX() * effectiveDrag;
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
     * F_drag = Cd × A × v²  (opuesta a la velocidad)
     * a_drag = F_drag / m
     * Δv = a_drag × deltaTime
     *
     * La resistencia del aire produce naturalmente velocidad terminal cuando
     * se balancea con otras fuerzas (gravedad aplicada vía evaluators).
     *
     * @param onGround si true, no aplica drag en caída (objeto en contacto con suelo)
     * @param deltaTime tiempo del simulation step en segundos
     */
    public void applyAerodynamicDrag(boolean onGround, double deltaTime) {
        if (onGround) return;

        // ── Resistencia aerodinámica (escalada para px/frame) ─────────────
        // F_drag = Cd × A × v²
        // Cd ya está escalado (0.0001-0.001) para unidades del juego.
        // NO se usa mediumDensity — era factor de SI units incompatible.
        double vy = velocity.getY();
        double speed = Math.abs(vy);
        double dragForce = dragCoefficient * effectiveArea * speed * speed;

        // Dirección del drag: opuesta a la velocidad
        // Si vy > 0 (cayendo) → drag hacia arriba (negativo)
        // Si vy < 0 (subiendo) → drag hacia abajo (positivo)
        double dragDirection = (vy >= 0) ? -1.0 : 1.0;

        // ── Aceleración por drag (F / m) ──────────────────────────────────
        double a_drag = (dragForce / mass) * dragDirection;

        // ── Mini-HRFC: Integración temporal correcta ─────────────────────
        // Δv = a_drag × deltaTime
        // NO: newVy = vy + a_drag (depende del framerate)
        double newVy = vy + (a_drag * deltaTime);
        velocity.setY(newVy);
    }

    /**
     * Aplica gravedad y resistencia aerodinámica del medio.
     *
     * ── HRFC — Consolidación Final de Kinetic Physics ────────────────────
     * ── HRFC FASE 2 — Corrección de Unidades ─────────────────────────────
     * ── HRFC FASE 3.5 CORRECCIÓN — Eliminación de Duplicación ────────────
     * ── Mini-HRFC — Unified Physics Time Integration ─────────────────────
     *
     * ARQUITECTURA CORREGIDA:
     *
     * Este método ahora DELEGA la aplicación de gravedad al sistema de
     * acumulación de fuerzas, eliminando la duplicación con NewtonEvaluator.
     *
     * ANTES (duplicación):
     *   Physics2D.applyGravity() → calcula gravedad directamente
     *   NewtonEvaluator → calcula gravedad
     *
     * AHORA (delegación):
     *   Physics2D.applyGravity() → registra gravedad vía accumulate()
     *   flushAccumulatedForces(deltaTime) → aplica fuerzas acumuladas
     *   (alternativamente: PhysicsSolver → NewtonEvaluator)
     *
     * ── FUNCIONAMIENTO ───────────────────────────────────────────────────
     *
     * 1. Registra gravedad como fuerza continua vía accumulate()
     * 2. Aplica drag aerodinámico directamente (correcto en Physics2D)
     *
     * La gravedad registrada será integrada cuando se llame
     * flushAccumulatedForces(deltaTime), permitiendo que sistemas externos
     * (PhysicsSolver, zones, etc.) modifiquen la gravedad antes de aplicarla.
     *
     * MIGRACIÓN A SISTEMA DECLARATIVO:
     *
     * Para migrar completamente al sistema declarativo (PhysicsSolver):
     *
     *   1. Configurar PhysicalRelation NEWTON con:
     *      - THRESHOLD_ABOVE = entity.gravity × environment.gravityInfluenceY
     *      - Participating: VELOCITY_Y
     *
     *   2. Llamar physicsSolver.solve(entities, environment, deltaTime)
     *      antes de applyGravity()
     *
     *   3. applyGravity() omitirá registro de gravedad si
     *      isGravityManagedExternally() = true
     *
     * @param onGround si true, no aplica gravedad (objeto en contacto con suelo).
     * @param deltaTime tiempo del simulation step en segundos.
     */
    public void applyGravity(boolean onGround, double deltaTime) {
        if (onGround) return;

        // ── HRFC FASE 3.5 + Mini-HRFC: Delegación sin duplicación ─────────
        // En lugar de calcular gravedad directamente (duplicando NewtonEvaluator),
        // registramos la gravedad como fuerza continua.
        //
        // Esto permite que:
        // 1. Sistemas externos modifiquen la gravedad antes de aplicarla
        // 2. Se integre correctamente con otras fuerzas acumuladas
        // 3. Se elimine la duplicación con NewtonEvaluator
        //
        // La gravedad será aplicada cuando se llame flushAccumulatedForces(deltaTime).
        
        if (!isGravityManagedExternally()) {
            // Registrar gravedad como fuerza continua
            // F_gravity = m × g
            double gravityForce = mass * gravity;
            accumulate(0, gravityForce);
        }

        // ── Drag aerodinámico (correcto en Physics2D) ────────────────────
        applyAerodynamicDrag(false, deltaTime);  // Ya verificamos onGround arriba
    }

    // ── Salto ─────────────────────────────────────────────────────────────

    /**
     * Aplica un impulso de salto vertical.
     *
     * ── HRFC — Kinetic Physics: Forces, Impulses & Motion Intent ─────────
     *
     * Este método ahora utiliza addForce() en lugar de asignación directa
     * de velocidad, integrándose correctamente con el sistema de fuerzas/impulsos.
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
     * ── Mini-HRFC — Unified Physics Time Integration ─────────────────────
     *
     * CORRECCIÓN TEMPORAL:
     *
     * La posición debe integrarse usando velocity × deltaTime:
     *   Δx = v × deltaTime
     *
     * NO:
     *   Δx = v  (asume 1 unidad de tiempo = 1 frame, dependiente del framerate)
     *
     * Si la velocidad está expresada en unidades/segundo (u/s), el desplazamiento
     * por frame debe ser: posición += velocidad × deltaTime_en_segundos.
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
     * HRFC FASE 2: El coeficiente ahora está escalado para px/frame.
     * Rango típico: 0.0001 - 0.001 (no confundir con Cd físico 0.1-2.0).
     *
     * @param cd coeficiente de drag escalado [típicamente 0.0001 - 0.001]. Debe ser >= 0.
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
     * ── SEMÁNTICA ─────────────────────────────────────────────────────────
     * addForce() representa una aplicación INMEDIATA de un impulso discreto:
     *
     *   Δv = J / m
     *
     * donde J es el impulso aplicado durante este step.
     *
     * ── USO TÍPICO ────────────────────────────────────────────────────────
     *   - Knockback de explosiones (MetheorBullet)
     *   - Empuje radial (PushableComponent)
     *   - Impactos puntuales
     *   - Saltos
     *   - Rebotes
     *
     * ── DIFERENCIA CON accumulate() ───────────────────────────────────────
     *   addForce()   → impulso instantáneo aplicado inmediatamente
     *   accumulate() → fuerza continua acumulada hasta flushAccumulatedForces()
     *
     * Para fuerzas continuas (viento, campos vectoriales, gravedad de zona),
     * usar {@link #accumulate(double, double)} en su lugar.
     *
     * @param fx componente X del impulso (en unidades de fuerza).
     * @param fy componente Y del impulso (en unidades de fuerza).
     */
    public void addForce(double fx, double fy) {
        velocity.setX(velocity.getX() + (fx / mass));
        velocity.setY(velocity.getY() + (fy / mass));
    }

    /**
     * Acumula una fuerza continua para ser integrada en el siguiente
     * {@link #flushAccumulatedForces()}.
     *
     * ── SEMÁNTICA ─────────────────────────────────────────────────────────
     * accumulate() representa fuerzas CONTINUAS que actúan durante el frame:
     *
     *   F_net = ΣF    (suma de todas las fuerzas acumuladas)
     *   Δv = (F_net / m) × Δt
     *
     * Múltiples sistemas pueden llamar accumulate() en el mismo frame.
     * Todas las fuerzas se suman vectorialmente.
     *
     * ── USO TÍPICO ────────────────────────────────────────────────────────
     *   - Campos de viento (VectorFieldSystem)
     *   - Zonas de gravedad modificada
     *   - Campos magnéticos
     *   - Corrientes de agua
     *   - Fuerzas ambientales persistentes
     *
     * ── DIFERENCIA CON addForce() ─────────────────────────────────────────
     *   addForce()   → impulso instantáneo aplicado inmediatamente
     *   accumulate() → fuerza continua acumulada hasta flush
     *
     * ── CICLO DE VIDA ─────────────────────────────────────────────────────
     * 1. Sistemas externos llaman accumulate() durante su update
     * 2. CollisionsSystem FASE 0.5 llama flushAccumulatedForces()
     * 3. Fuerzas se integran como impulso: velocity += (ΣF / mass)
     * 4. Acumulador se resetea automáticamente para el siguiente frame
     *
     * Para impulsos instantáneos (knockback, explosiones), usar
     * {@link #addForce(double, double)} en su lugar.
     *
     * @param fx fuerza en X (en unidades de fuerza, no velocidad).
     * @param fy fuerza en Y (en unidades de fuerza, no velocidad).
     */
    public void accumulate(double fx, double fy) {
        accumulatedFx += fx;
        accumulatedFy += fy;
    }

    /**
     * Aplica todas las fuerzas acumuladas como un único impulso y
     * limpia el acumulador para el siguiente frame.
     *
     * ── Mini-HRFC — Unified Physics Time Integration ─────────────────────
     *
     * CORRECCIÓN TEMPORAL:
     *
     * Las fuerzas continuas deben integrarse usando deltaTime:
     *   Δv = (ΣF / m) × deltaTime
     *
     * NO:
     *   Δv = ΣF / m  (dependiente del framerate)
     *
     * ANTES: accumulate() registraba fuerzas y flush las aplicaba sin tiempo.
     * AHORA: flush recibe deltaTime explícito del simulation step.
     *
     * Llamar desde CollisionsSystem FASE 0.5, después de applyGravity()
     * y antes del SweptAABB, para que las fuerzas de zona se integren
     * correctamente en el mismo step que la gravedad.
     *
     * No hace nada si no hay fuerzas acumuladas.
     *
     * @param deltaTime tiempo del simulation step en segundos
     */
    public void flushAccumulatedForces(double deltaTime) {
        if (accumulatedFx == 0.0 && accumulatedFy == 0.0) return;
        
        // Mini-HRFC: Integración temporal correcta
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
