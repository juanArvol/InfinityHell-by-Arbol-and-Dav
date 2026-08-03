package Game.Engine.World.Physics.Contact;

/**
 * Estado instantáneo de interacción con otros cuerpos.
 *
 * ── HRFC-031 — Descomposición de PhysicalState en SimulationContext ───────
 *
 * ── RESPONSABILIDAD ──────────────────────────────────────────────────────
 * ContactState describe el estado de contacto actual de un objeto con el
 * entorno y otros cuerpos. Es temporal por naturaleza: describe únicamente
 * la interacción del frame actual.
 *
 * ── PRINCIPIO FUNDAMENTAL ─────────────────────────────────────────────────
 * ContactState solo describe. No calcula fuerzas. No modifica velocidades.
 * No produce fenómenos. Es una instantánea del estado de contacto.
 *
 *   ContactState  →  describe (qué está tocando el objeto ahora)
 *   Relation      →  interpreta (qué consecuencias tiene ese contacto)
 *
 * ── DOMINIO ───────────────────────────────────────────────────────────────
 * Propiedades incluidas:
 *
 *   Estado de contacto básico:
 *     onGround            si el objeto está en contacto con una superficie
 *     onWall              si el objeto está en contacto con una pared
 *     onCeiling           si el objeto está en contacto con el techo
 *
 *   Superficie de contacto:
 *     surfaceFriction     coeficiente de fricción de la superficie actual
 *     surfaceNormalX      componente X de la normal de contacto
 *     surfaceNormalY      componente Y de la normal de contacto
 *
 *   Fuerzas de contacto:
 *     normalForce         fuerza normal de la superficie sobre el objeto
 *     frictionForce       fuerza de fricción aplicada en el frame actual
 *
 *   Penetración:
 *     penetrationDepth    profundidad de penetración en la superficie [0, +∞)
 *
 *   Impulso:
 *     appliedImpulse      impulso recibido en el frame de contacto
 *
 *   Tiempo de contacto:
 *     contactDuration     tiempo acumulado de contacto continuo, en segundos
 *
 * ── NOTAS SOBRE on_ground vs materialState.frictionCoefficient ────────────
 * onGround y surfaceFriction describen el contacto activo con el entorno:
 *   - onGround = el objeto está en contacto con alguna superficie.
 *   - surfaceFriction = coeficiente de la SUPERFICIE contactada (no del material propio).
 *
 * materialState.frictionCoefficient describe la propiedad del OBJETO.
 *
 * Un evaluador de fricción combina ambos: μ_efectivo = combinación(objeto, superficie).
 *
 * ── INMUTABILIDAD ─────────────────────────────────────────────────────────
 * ContactState es completamente inmutable tras su construcción.
 * Cada frame produce un nuevo ContactState si el estado de contacto cambia.
 *
 * ── ESTADO VACÍO ──────────────────────────────────────────────────────────
 * ContactState.NONE representa la ausencia de contacto. Es el estado por
 * defecto para entidades en el aire o sin detección de colisiones activa.
 *
 * ── THREAD SAFETY ─────────────────────────────────────────────────────────
 * Inmutable → thread-safe por diseño.
 */
public final class ContactState implements Game.Engine.World.Physics.Core.DomainState {

    // ── Estado de contacto básico ─────────────────────────────────────────

    /** True si el objeto está en contacto con una superficie inferior (suelo). */
    private final boolean onGround;

    /** True si el objeto está en contacto con una superficie lateral (pared). */
    private final boolean onWall;

    /** True si el objeto está en contacto con una superficie superior (techo). */
    private final boolean onCeiling;

    // ── Superficie de contacto ────────────────────────────────────────────

    /**
     * Coeficiente de fricción de la superficie actualmente contactada.
     * 0.0 si no hay contacto o si la superficie no tiene rozamiento.
     */
    private final double surfaceFriction;

    /**
     * Componente X de la normal de contacto.
     * (0, 0) si no hay contacto activo.
     */
    private final double surfaceNormalX;

    /**
     * Componente Y de la normal de contacto.
     * (0, 0) si no hay contacto activo.
     */
    private final double surfaceNormalY;

    // ── Fuerzas de contacto ───────────────────────────────────────────────

    /**
     * Módulo de la fuerza normal de la superficie sobre el objeto, en este frame.
     * 0.0 si no hay contacto.
     */
    private final double normalForce;

    /**
     * Módulo de la fuerza de fricción aplicada en este frame.
     * 0.0 si no hay contacto o no hay movimiento relativo.
     */
    private final double frictionForce;

    // ── Penetración ───────────────────────────────────────────────────────

    /**
     * Profundidad de penetración en la superficie de contacto [0, +∞).
     * 0.0 si no hay superposición o no hay contacto.
     */
    private final double penetrationDepth;

    // ── Impulso ───────────────────────────────────────────────────────────

    /**
     * Impulso recibido en este frame de contacto (cambio de momentum).
     * 0.0 si no hay colisión activa.
     */
    private final double appliedImpulse;

    // ── Tiempo de contacto ────────────────────────────────────────────────

    /**
     * Duración del contacto continuo actual, en segundos.
     * Se reinicia a 0.0 cuando el contacto se interrumpe.
     */
    private final double contactDuration;

    // ── Constructor privado — usar Builder ────────────────────────────────

    private ContactState(Builder b) {
        this.onGround         = b.onGround;
        this.onWall           = b.onWall;
        this.onCeiling        = b.onCeiling;
        this.surfaceFriction  = b.surfaceFriction;
        this.surfaceNormalX   = b.surfaceNormalX;
        this.surfaceNormalY   = b.surfaceNormalY;
        this.normalForce      = b.normalForce;
        this.frictionForce    = b.frictionForce;
        this.penetrationDepth = b.penetrationDepth;
        this.appliedImpulse   = b.appliedImpulse;
        this.contactDuration  = b.contactDuration;
    }

    // ── Factories ─────────────────────────────────────────────────────────

    /** Punto de entrada del Builder. */
    public static Builder builder() { return new Builder(); }

    /**
     * Estado de sin contacto.
     * Representa una entidad completamente en el aire, sin colisiones activas.
     */
    public static final ContactState NONE = builder().build();

    /**
     * Crea un ContactState básico de suelo con la fricción de la superficie dada.
     * Shortcut para el caso más frecuente: objeto apoyado sobre una superficie plana.
     *
     * @param surfaceFriction coeficiente de fricción de la superficie.
     * @param normalForce     fuerza normal de la superficie.
     * @return ContactState con onGround=true, normal=(0,-1) (superficie horizontal).
     */
    public static ContactState onGround(double surfaceFriction, double normalForce) {
        return builder()
            .onGround(true)
            .surfaceFriction(surfaceFriction)
            .surfaceNormalX(0.0)
            .surfaceNormalY(-1.0)   // normal apunta hacia arriba (y negativo en AWT)
            .normalForce(normalForce)
            .build();
    }

    // ── Accesores — estado de contacto ────────────────────────────────────

    /** True si el objeto está en contacto con el suelo. */
    public boolean isOnGround()          { return onGround; }

    /** True si el objeto está en contacto con una pared. */
    public boolean isOnWall()            { return onWall; }

    /** True si el objeto está en contacto con el techo. */
    public boolean isOnCeiling()         { return onCeiling; }

    /** True si el objeto tiene algún tipo de contacto activo. */
    public boolean hasContact()          { return onGround || onWall || onCeiling; }

    // ── Accesores — superficie ────────────────────────────────────────────

    /** Coeficiente de fricción de la superficie contactada. */
    public double getSurfaceFriction()   { return surfaceFriction; }

    /** Componente X de la normal de contacto. */
    public double getSurfaceNormalX()    { return surfaceNormalX; }

    /** Componente Y de la normal de contacto. */
    public double getSurfaceNormalY()    { return surfaceNormalY; }

    // ── Accesores — fuerzas ───────────────────────────────────────────────

    /** Fuerza normal de la superficie, en este frame. */
    public double getNormalForce()       { return normalForce; }

    /** Fuerza de fricción aplicada, en este frame. */
    public double getFrictionForce()     { return frictionForce; }

    // ── Accesores — penetración e impulso ─────────────────────────────────

    /** Profundidad de penetración en la superficie [0, +∞). */
    public double getPenetrationDepth()  { return penetrationDepth; }

    /** Impulso recibido en este frame. */
    public double getAppliedImpulse()    { return appliedImpulse; }

    // ── Accesores — tiempo ────────────────────────────────────────────────

    /** Duración del contacto continuo actual, en segundos. */
    public double getContactDuration()   { return contactDuration; }

    // ── Object ────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return String.format(
            "ContactState[ground=%b wall=%b ceil=%b μ=%.2f Fn=%.2f impulse=%.2f]",
            onGround, onWall, onCeiling,
            surfaceFriction, normalForce, appliedImpulse);
    }

    // ═════════════════════════════════════════════════════════════════════
    // Builder
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Builder de ContactState.
     *
     * Valores por defecto (sin contacto):
     *   onGround         = false
     *   onWall           = false
     *   onCeiling        = false
     *   surfaceFriction  = 0.0
     *   surfaceNormalX   = 0.0
     *   surfaceNormalY   = 0.0
     *   normalForce      = 0.0
     *   frictionForce    = 0.0
     *   penetrationDepth = 0.0
     *   appliedImpulse   = 0.0
     *   contactDuration  = 0.0
     */
    public static final class Builder {

        private boolean onGround         = false;
        private boolean onWall           = false;
        private boolean onCeiling        = false;
        private double  surfaceFriction  = 0.0;
        private double  surfaceNormalX   = 0.0;
        private double  surfaceNormalY   = 0.0;
        private double  normalForce      = 0.0;
        private double  frictionForce    = 0.0;
        private double  penetrationDepth = 0.0;
        private double  appliedImpulse   = 0.0;
        private double  contactDuration  = 0.0;

        private Builder() {}

        public Builder onGround(boolean v)         { this.onGround         = v;                        return this; }
        public Builder onWall(boolean v)           { this.onWall           = v;                        return this; }
        public Builder onCeiling(boolean v)        { this.onCeiling        = v;                        return this; }
        public Builder surfaceFriction(double v)   { this.surfaceFriction  = Math.max(0.0, v);         return this; }
        public Builder surfaceNormalX(double v)    { this.surfaceNormalX   = v;                        return this; }
        public Builder surfaceNormalY(double v)    { this.surfaceNormalY   = v;                        return this; }
        public Builder normalForce(double v)       { this.normalForce      = Math.max(0.0, v);         return this; }
        public Builder frictionForce(double v)     { this.frictionForce    = Math.max(0.0, v);         return this; }
        public Builder penetrationDepth(double v)  { this.penetrationDepth = Math.max(0.0, v);         return this; }
        public Builder appliedImpulse(double v)    { this.appliedImpulse   = v;                        return this; }
        public Builder contactDuration(double v)   { this.contactDuration  = Math.max(0.0, v);         return this; }

        /** Construye el ContactState. */
        public ContactState build() { return new ContactState(this); }
    }
}
