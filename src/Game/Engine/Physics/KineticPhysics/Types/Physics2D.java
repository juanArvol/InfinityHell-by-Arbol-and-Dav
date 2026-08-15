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
     * Añadir efectos: {@code statusStack().add("poison", ctx -> 0.6);}
     * Removerlos:     {@code statusStack().remove("poison");}
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
     * Aplica gravedad y resistencia aerodinámica del medio.
     *
     * ── HRFC — Consolidación Final de Kinetic Physics ────────────────────
     * ── HRFC FASE 2 — Corrección de Unidades ─────────────────────────────
     *
     * La velocidad terminal NO se impone artificialmente.
     * Emerge naturalmente del balance entre fuerzas:
     *
     *   F_gravity = m × g (hacia abajo)
     *   F_drag = Cd × A × v² (opuesta a la velocidad)
     *
     * Cuando F_net ≈ 0 → velocidad terminal alcanzada.
     *
     * Este método:
     *   1. Aplica gravedad: a_gravity = g (NO escalada por masa — corrección física)
     *   2. Calcula drag aerodinámico: F_drag = Cd × A × vy²
     *      (Cd y A ya están escalados para px/frame, no se usa mediumDensity)
     *   3. Aplica drag como deceleración: a_drag = F_drag / m
     *   4. Integra aceleración neta: vy += (a_gravity - a_drag)
     *
     * Corrección HRFC FASE 2:
     *   - Removido factor mediumDensity (0.5 × ρ) del cálculo.
     *   - dragCoefficient ahora está escalado para unidades del juego (0.0001-0.001).
     *   - Esto corrige el problema de drag excesivo que causaba caída lenta/hover.
     *
     * Nota física importante:
     *   - La gravedad produce aceleración constante g independiente de la masa.
     *   - El drag produce fuerza proporcional a v², que se divide por masa.
     *   - Objetos más masivos alcanzan mayor velocidad terminal (menor a_drag relativa).
     *   - Objetos con mayor área alcanzan menor velocidad terminal (mayor F_drag).
     *
     * @param onGround si true, no aplica gravedad (objeto en contacto con suelo).
     */
    public void applyGravity(boolean onGround) {
        if (onGround) return;

        // ── 1. Aceleración gravitatoria (constante, independiente de masa) ──
        double a_gravity = gravity;

        // ── 2. Resistencia aerodinámica (escalada para px/frame) ─────────
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

        // ── 3. Aceleración por drag (F / m) ──────────────────────────────
        double a_drag = (dragForce / mass) * dragDirection;

        // ── 4. Integración de aceleración neta ───────────────────────────
        double a_net = a_gravity + a_drag;
        double newVy = vy + a_net;

        velocity.setY(newVy);
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

    public void updateMoves(Vector2D position) {
        position.setX(position.getX() + velocity.getX());
        position.setY(position.getY() + velocity.getY());
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
     * Aplica todas las fuerzas acumuladas como un único impulso (F/mass)
     * y limpia el acumulador para el siguiente frame.
     *
     * Llamar desde CollisionsSystem FASE 0.5, después de applyGravity()
     * y antes del SweptAABB, para que las fuerzas de zona se integren
     * correctamente en el mismo step que la gravedad.
     *
     * No hace nada si no hay fuerzas acumuladas.
     */
    public void flushAccumulatedForces() {
        if (accumulatedFx == 0.0 && accumulatedFy == 0.0) return;
        velocity.setX(velocity.getX() + (accumulatedFx / mass));
        velocity.setY(velocity.getY() + (accumulatedFy / mass));
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
