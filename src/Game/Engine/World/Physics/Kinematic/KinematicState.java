package Game.Engine.World.Physics.Kinematic;

import Game.Engine.GameMath.KineticPhysics.SurfaceMaterial;

/**
 * Estado cinemático capturado al finalizar el paso de Kinematic Physics.
 *
 * ── HRFC-030 — Integración entre Kinematic Physics y World Physics ────────
 * ── HRFC-031 — Descomposición de PhysicalState en SimulationContext ───────
 *
 * ── QUÉ ES KinematicState ────────────────────────────────────────────────
 * KinematicState es el contrato de información que Kinematic Physics produce
 * y que World Physics consume.
 *
 * Es un DTO inmutable. Contiene exactamente lo que Physics2D sabe en cada
 * frame, expresado en términos interpretables por el simulador del mundo.
 *
 * ── PRINCIPIO FUNDAMENTAL ─────────────────────────────────────────────────
 * Kinematic Physics NO conoce calor, presión ni ningún fenómeno físico.
 * Únicamente produce este estado.
 *
 * World Physics NO mueve entidades ni modifica velocidades.
 * Únicamente consume este estado para producir consecuencias físicas.
 *
 * La desacoplación es total: ninguno de los dos sistemas conoce al otro.
 * Solo comparten este estado, que actúa como frontera limpia entre ellos.
 *
 * ── HRFC-031: ELIMINACIÓN DE previousVelocity ────────────────────────────
 * Antes de HRFC-031, KinematicState almacenaba previousVelocity y calculaba
 * directamente acceleration y deltaKineticEnergy comparando con ese campo.
 *
 * Ese diseño introducía historial específico dentro del DTO, que debía
 * propagarse desde KinematicBridge mediante un campo previousSpeed explícito.
 * Con la introducción de StateSnapshot<KinematicState> en KinematicBridge,
 * el historial ya no necesita vivir aquí:
 *
 *   KinematicBridge mantiene StateSnapshot<KinematicState>
 *       ↓
 *   snapshot.current()  = estado de este frame
 *   snapshot.previous() = estado del frame anterior
 *       ↓
 *   Los deltas se calculan comparando current con previous:
 *     acceleration       = (current.velocity - previous.velocity) / deltaTime
 *     deltaKineticEnergy = current.kineticEnergy - previous.kineticEnergy
 *
 * KinematicState describe SOLO el estado instantáneo del frame actual.
 * No contiene información del frame anterior.
 *
 * ── MAGNITUDES INCLUIDAS ──────────────────────────────────────────────────
 *
 *   velocity         → módulo de la velocidad actual (|v|), en u/s.
 *   velocityX        → componente X de la velocidad, en u/s.
 *   velocityY        → componente Y de la velocidad, en u/s.
 *   mass             → masa de la entidad, en kg relativos.
 *   momentum         → cantidad de movimiento (mass × |v|), en kg·u/s.
 *   kineticEnergy    → energía cinética (½ × mass × v²), en unidades relativas.
 *   onGround         → true si la entidad está en contacto con una superficie.
 *   surface          → material de la superficie de contacto (nunca null).
 *   frictionFactor   → coeficiente de fricción de la superficie actual [0, +∞).
 *   deltaTime        → duración del frame en segundos.
 *
 * ── MAGNITUDES DERIVADAS TEMPORALES ──────────────────────────────────────
 * Las magnitudes que dependen de la comparación con el frame anterior
 * (acceleration, deltaKineticEnergy) ya NO son campos de este DTO.
 * Se obtienen mediante StateSnapshot:
 *
 *   StateSnapshot<KinematicState> snap = ...;
 *   double accel = snap.current().getVelocity() - snap.previous().getVelocity()
 *                  / snap.current().getDeltaTime();
 *   double dKE   = snap.current().getKineticEnergy()
 *                - snap.previous().getKineticEnergy();
 *
 * Los evaluadores del mundo acceden a estos deltas a través de
 * SimulationContext.kinematic() que expone el StateSnapshot completo.
 *
 * ── EXTENSIBILIDAD ────────────────────────────────────────────────────────
 * Añadir nuevas magnitudes instantáneas (angularVelocity, torque):
 *   1. Añadir campo + accessor en este DTO.
 *   2. Actualizar from() para calcularlas.
 *   No se necesita modificar StateSnapshot ni KinematicBridge
 *   para obtener deltas: snapshot.current().X - snapshot.previous().X
 *   funciona automáticamente para cualquier campo nuevo.
 *
 * ── INMUTABILIDAD ─────────────────────────────────────────────────────────
 * KinematicState es completamente inmutable tras su construcción.
 * Physics2D produce una nueva instancia cada frame; nunca reutiliza la anterior.
 *
 * ── THREAD SAFETY ─────────────────────────────────────────────────────────
 * Inmutable → thread-safe por diseño.
 */
public final class KinematicState implements Game.Engine.World.Physics.Core.DomainState {

    // ── Velocidad ─────────────────────────────────────────────────────────

    /** Módulo de la velocidad actual, en u/s. Siempre ≥ 0. */
    private final double velocity;

    /** Componente X de la velocidad, en u/s. Con signo. */
    private final double velocityX;

    /** Componente Y de la velocidad, en u/s. Con signo. */
    private final double velocityY;

    // ── Dinámica ──────────────────────────────────────────────────────────

    /** Masa de la entidad, en kg relativos. Siempre > 0. */
    private final double mass;

    /** Cantidad de movimiento (mass × velocity), en kg·u/s. */
    private final double momentum;

    // ── Energía ───────────────────────────────────────────────────────────

    /** Energía cinética actual (½ × mass × v²). Siempre ≥ 0. */
    private final double kineticEnergy;

    // ── Contacto con superficie ───────────────────────────────────────────

    /** True si la entidad está en contacto con una superficie este frame. */
    private final boolean onGround;

    /** Material de la superficie de contacto. Nunca null (AIR si no hay contacto). */
    private final SurfaceMaterial surface;

    /**
     * Coeficiente de fricción de la superficie de contacto actual [0, +∞).
     * Derivado de surface.getFriction(). 0.0 si está en el aire.
     */
    private final double frictionFactor;

    // ── Temporización ─────────────────────────────────────────────────────

    /** Duración del frame en segundos. Siempre > 0. */
    private final double deltaTime;

    // ── Constructor privado — usar factories ──────────────────────────────

    private KinematicState(Builder b) {
        this.velocity       = b.velocity;
        this.velocityX      = b.velocityX;
        this.velocityY      = b.velocityY;
        this.mass           = b.mass;
        this.momentum       = b.momentum;
        this.kineticEnergy  = b.kineticEnergy;
        this.onGround       = b.onGround;
        this.surface        = b.surface != null ? b.surface : SurfaceMaterial.AIR;
        this.frictionFactor = b.frictionFactor;
        this.deltaTime      = b.deltaTime;
    }

    // ── Factories ─────────────────────────────────────────────────────────

    /** Punto de entrada del Builder. */
    public static Builder builder() { return new Builder(); }

    /**
     * Construye un KinematicState directamente desde los valores instantáneos
     * de Physics2D para el frame actual.
     *
     * Este factory encapsula el cálculo de las magnitudes derivadas instantáneas
     * (momentum, kineticEnergy) sin necesitar información del frame anterior.
     * Los deltas temporales (acceleration, deltaKineticEnergy) se obtienen
     * comparando este estado con el anterior a través de StateSnapshot.
     *
     * @param velocityX componente X de la velocidad actual.
     * @param velocityY componente Y de la velocidad actual.
     * @param mass      masa de la entidad.
     * @param onGround  true si está en contacto con el suelo.
     * @param surface   material de la superficie actual (null = AIR).
     * @param deltaTime duración del frame en segundos.
     * @return KinematicState calculado para el frame actual.
     */
    public static KinematicState from(double          velocityX,
                                      double          velocityY,
                                      double          mass,
                                      boolean         onGround,
                                      SurfaceMaterial surface,
                                      double          deltaTime) {
        double safeMass  = Math.max(mass, 0.01);
        double velocity  = Math.sqrt(velocityX * velocityX + velocityY * velocityY);
        double ke        = 0.5 * safeMass * velocity * velocity;
        double friction  = surface != null
            ? surface.getFriction()
            : SurfaceMaterial.AIR.getFriction();

        return builder()
            .velocity(velocity)
            .velocityX(velocityX)
            .velocityY(velocityY)
            .mass(safeMass)
            .momentum(safeMass * velocity)
            .kineticEnergy(ke)
            .onGround(onGround)
            .surface(surface)
            .frictionFactor(friction)
            .deltaTime(deltaTime)
            .build();
    }

    // ── Accesores — velocidad ─────────────────────────────────────────────

    /**
     * Módulo de la velocidad actual en u/s.
     * Siempre ≥ 0. Para la dirección usar getVelocityX() / getVelocityY().
     */
    public double getVelocity()       { return velocity; }

    /** Componente X de la velocidad, en u/s. Con signo. */
    public double getVelocityX()      { return velocityX; }

    /** Componente Y de la velocidad, en u/s. Con signo. */
    public double getVelocityY()      { return velocityY; }

    // ── Accesores — dinámica ──────────────────────────────────────────────

    /** Masa de la entidad, en kg relativos. Siempre > 0. */
    public double getMass()           { return mass; }

    /** Cantidad de movimiento (mass × velocity), en kg·u/s. */
    public double getMomentum()       { return momentum; }

    // ── Accesores — energía ───────────────────────────────────────────────

    /** Energía cinética actual (½ × mass × v²). */
    public double getKineticEnergy()  { return kineticEnergy; }

    // ── Accesores — contacto ──────────────────────────────────────────────

    /** True si la entidad está en contacto con una superficie este frame. */
    public boolean isOnGround()       { return onGround; }

    /** Material de la superficie de contacto. Nunca null. */
    public SurfaceMaterial getSurface() { return surface; }

    /**
     * Coeficiente de fricción de la superficie de contacto actual.
     * Relevante para FrictionThermalEvaluator:
     *   Q ≈ frictionFactor × mass × gravity × velocity × dt
     */
    public double getFrictionFactor() { return frictionFactor; }

    // ── Accesores — temporización ─────────────────────────────────────────

    /** Duración del frame en segundos. */
    public double getDeltaTime()      { return deltaTime; }

    // ── Helpers de conveniencia ───────────────────────────────────────────

    /**
     * True si la entidad se mueve a una velocidad superior al umbral.
     *
     * @param minSpeed velocidad mínima para considerarse "en movimiento".
     * @return true si velocity {@literal >} |minSpeed|.
     */
    public boolean isMoving(double minSpeed) {
        return velocity > Math.abs(minSpeed);
    }

    /**
     * Calcula la aceleración de este frame respecto a un estado anterior.
     *
     * Equivale a: (this.velocity - previous.velocity) / this.deltaTime
     *
     * Disponible como conveniencia para evitar que los evaluadores repitan
     * esta fórmula. El uso canónico es a través de StateSnapshot:
     *
     *   double accel = snap.current().accelerationFrom(snap.previous());
     *
     * @param previous el estado cinemático del frame anterior. No puede ser null.
     * @return aceleración en u/s². Positiva = aceleró. Negativa = desaceleró.
     */
    public double accelerationFrom(KinematicState previous) {
        if (previous == null || deltaTime <= 0.0) return 0.0;
        return (this.velocity - previous.velocity) / this.deltaTime;
    }

    /**
     * Calcula el cambio de energía cinética de este frame respecto a un estado anterior.
     *
     * Equivale a: this.kineticEnergy - previous.kineticEnergy
     *
     * Negativo = energía disipada (frenado, fricción, impacto).
     * Positivo = energía ganada (aceleración, impulso).
     *
     * Disponible como conveniencia para evaluadores:
     *   double dKE = snap.current().deltaKineticEnergyFrom(snap.previous());
     *
     * @param previous el estado cinemático del frame anterior. No puede ser null.
     * @return cambio de energía cinética en unidades relativas.
     */
    public double deltaKineticEnergyFrom(KinematicState previous) {
        if (previous == null) return 0.0;
        return this.kineticEnergy - previous.kineticEnergy;
    }

    /**
     * Calcula el cambio de momentum de este frame respecto a un estado anterior.
     *
     * Equivale a: this.momentum - previous.momentum
     *
     * Positivo = ganó cantidad de movimiento. Negativo = perdió.
     *
     * @param previous el estado cinemático del frame anterior.
     * @return cambio de momentum en kg·u/s.
     */
    public double deltaMomentumFrom(KinematicState previous) {
        if (previous == null) return 0.0;
        return this.momentum - previous.momentum;
    }

    // ── Object ────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return String.format(
            "KinematicState[v=%.2f vx=%.2f vy=%.2f m=%.1f ke=%.2f onGround=%b surface=%s]",
            velocity, velocityX, velocityY, mass, kineticEnergy, onGround, surface);
    }

    // ═════════════════════════════════════════════════════════════════════
    // Builder
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Builder de KinematicState. Preferir {@link #from} para construcción estándar.
     */
    public static final class Builder {

        private double          velocity      = 0.0;
        private double          velocityX     = 0.0;
        private double          velocityY     = 0.0;
        private double          mass          = 1.0;
        private double          momentum      = 0.0;
        private double          kineticEnergy = 0.0;
        private boolean         onGround      = false;
        private SurfaceMaterial surface       = null;
        private double          frictionFactor = SurfaceMaterial.AIR.getFriction();
        private double          deltaTime     = 1.0 / 60.0;

        private Builder() {}

        public Builder velocity(double v)        { this.velocity       = v;                    return this; }
        public Builder velocityX(double v)       { this.velocityX      = v;                    return this; }
        public Builder velocityY(double v)       { this.velocityY      = v;                    return this; }
        public Builder mass(double v)            { this.mass           = Math.max(0.01, v);    return this; }
        public Builder momentum(double v)        { this.momentum       = v;                    return this; }
        public Builder kineticEnergy(double v)   { this.kineticEnergy  = v;                    return this; }
        public Builder onGround(boolean v)       { this.onGround       = v;                    return this; }
        public Builder surface(SurfaceMaterial v){ this.surface        = v;                    return this; }
        public Builder frictionFactor(double v)  { this.frictionFactor = v;                    return this; }
        public Builder deltaTime(double v)       { this.deltaTime      = v;                    return this; }

        public KinematicState build() { return new KinematicState(this); }
    }
}
