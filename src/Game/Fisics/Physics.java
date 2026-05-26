package Game.Fisics;

import Game.World.Surface.SurfaceMaterial;
import GameMath.Vector2D;

/**
 * Clase base de física.
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
 * ── drag vs friction ────────────────────────────────────────────────────
 *
 *   friction — escala la aceleración ACTIVA (cuando hay input).
 *   drag     — amortiguación PASIVA de vx (cuando NO hay input).
 *              Se multiplica por el slide base de la subclase.
 */
public class Physics {

    protected Vector2D velocity = new Vector2D(0, 0);
    protected double gravity;

    protected double mass        = 1.0;
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
    protected boolean salto;
    protected boolean running;

    protected double maxFallSpeed = 20.0;

    protected SurfaceMaterial currentSurface = SurfaceMaterial.DEFAULT;

    // Pilas de modificadores acumulables (buff/debuff, zona, etc.)
    private final ModifierStack statusStack      = new ModifierStack();
    private final ModifierStack environmentStack = new ModifierStack();

    public Physics(double gravity) {
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
        // Es responsabilidad exclusiva de CollisionsSystem (FASE 0) via setOnGround().
        // Sobreescribirlo acá causaba que el groundCheck correcto de FASE 0 quedara
        // pisado por el valor del frame anterior antes de que applyGravity() lo viera.
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
        if (Math.abs(vx) > speedMax - accel) {
            speedMax = Math.max(0, speedMax - accel);
        }
    }

    // ── Gravedad ──────────────────────────────────────────────────────────

    public void applyGravity(boolean onGround) {
        if (!onGround) {
            double newVy = velocity.getY() + (gravity * mass);
            velocity.setY(Math.min(newVy, maxFallSpeed));
        }
    }

    // ── Salto ─────────────────────────────────────────────────────────────

    public void jump(double force) {
        velocity.setY(-force / mass);
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
    public void setMaxFallSpeed(double s)    { this.maxFallSpeed = s; }
    public double getGravity()               { return gravity; }
    public void setGravity(double g)         { this.gravity = g; }
    public double getOposite(double x)       { return -x; }
    public boolean getOnGround()             { return onGround; }
    public void setOnGround(boolean v)       { this.onGround = v; }

    public void addForce(double fx, double fy) {
        velocity.setX(velocity.getX() + (fx / mass));
        velocity.setY(velocity.getY() + (fy / mass));
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
