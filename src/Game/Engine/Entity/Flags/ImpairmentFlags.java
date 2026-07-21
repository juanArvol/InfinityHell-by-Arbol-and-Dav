package Game.Engine.Entity.Flags;

/**
 * Estado derivado de fenómenos de incapacitación activos en la entidad.
 *
 * ── HRFC-014 — GAP-11: Derived State, encapsulado por diseño ────────────
 *
 * ImpairmentFlags representa fenómenos de incapacitación observables, no
 * causas concretas. "isStunned" significa "la entidad presenta actualmente
 * el fenómeno de estar aturdida", independientemente de si lo causó un
 * StunEffect, StaggerEffect, ConcussionEffect, etc.
 *
 * ── Restricción de escritura — impuesta por diseño, no por convención ─────
 *
 * Los setters son package-private. Solo EntityFlags (mismo paquete) puede
 * invocarlos, a través de EntityFlags.synchronize(StatusEffectComponent).
 * El código externo al paquete únicamente puede leer el estado derivado.
 *
 * ── Fenómenos representados ───────────────────────────────────────────────
 *   stunned   — aturdido: no puede moverse ni atacar
 *   rooted    — inmovilizado: no puede moverse, sí puede atacar
 *   frozen    — congelado: no puede moverse ni atacar
 *   silenced  — silenciado: no puede usar habilidades mágicas
 *   confused  — confundido: movimiento/ataques invertidos o aleatorios
 *   sleeping  — dormido: inactivo hasta recibir daño o estímulo
 *   feared    — aterrado: huye sin control
 *   disarmed  — desarmado: no puede ejecutar ataques físicos
 */
public final class ImpairmentFlags {

    private boolean stunned  = false;
    private boolean rooted   = false;
    private boolean frozen   = false;
    private boolean silenced = false;
    private boolean confused = false;
    private boolean sleeping = false;
    private boolean feared   = false;
    private boolean disarmed = false;

    // ── Consultas públicas ────────────────────────────────────────────────

    /** True si la entidad presenta el fenómeno de aturdimiento activo. */
    public boolean isStunned()  { return stunned; }

    /** True si la entidad presenta el fenómeno de inmovilización activa. */
    public boolean isRooted()   { return rooted; }

    /** True si la entidad presenta el fenómeno de congelación activa. */
    public boolean isFrozen()   { return frozen; }

    /** True si la entidad presenta el fenómeno de silencio activo. */
    public boolean isSilenced() { return silenced; }

    /** True si la entidad presenta el fenómeno de confusión activa. */
    public boolean isConfused() { return confused; }

    /** True si la entidad presenta el fenómeno de sueño activo. */
    public boolean isSleeping() { return sleeping; }

    /** True si la entidad presenta el fenómeno de huida incontrolada. */
    public boolean isFeared()   { return feared; }

    /** True si la entidad presenta el fenómeno de desarme activo. */
    public boolean isDisarmed() { return disarmed; }

    // ── Consultas compuestas por fenómeno ─────────────────────────────────

    /**
     * True si algún fenómeno activo impide el movimiento.
     * Pregunta por el fenómeno, no por el efecto concreto que lo produce.
     */
    public boolean isMovementInhibited() {
        return stunned || frozen || sleeping || rooted;
    }

    /**
     * True si algún fenómeno activo impide ataques físicos.
     */
    public boolean isAttackInhibited() {
        return stunned || frozen || sleeping || disarmed;
    }

    /**
     * True si algún fenómeno activo impide el uso de habilidades mágicas.
     */
    public boolean isCastInhibited() {
        return silenced || confused;
    }

    // ── Escritura — package-private ────────────────────────────────────────
    // Solo EntityFlags puede invocar estos métodos (mismo paquete).
    // El punto de entrada externo es EntityFlags.synchronize().

    void setStunned(boolean v)  { stunned  = v; }
    void setRooted(boolean v)   { rooted   = v; }
    void setFrozen(boolean v)   { frozen   = v; }
    void setSilenced(boolean v) { silenced = v; }
    void setConfused(boolean v) { confused = v; }
    void setSleeping(boolean v) { sleeping = v; }
    void setFeared(boolean v)   { feared   = v; }
    void setDisarmed(boolean v) { disarmed = v; }

    void clearAll() {
        stunned = rooted = frozen = silenced =
        confused = sleeping = feared = disarmed = false;
    }
}
