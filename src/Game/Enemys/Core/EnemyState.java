package Game.Enemys.Core;

/**
 * Estado observable del Enemy — flags de animación y física.
 *
 * Migrado desde Game.Enemys.Components.EnemyState al Core del framework.
 *
 * ── Responsabilidad ──────────────────────────────────────────────────────
 * EnemyState es el puente entre la física del engine y el sistema de
 * animación. MoveCommand.moveX() lee enElSuelo para calcular la aceleración.
 * AnimationController puede leer isMoving(), isAttacking(), etc.
 *
 * EnemyState no contiene lógica de comportamiento — solo flags.
 * La lógica vive en los controladores y estrategias.
 *
 * ── Ciclo de reset ───────────────────────────────────────────────────────
 * Enemy.update() hace reset de los flags volátiles (moving, attacking) al
 * inicio de cada frame para que los controladores los sobreescriban si
 * corresponde.
 */
public final class EnemyState {

    private boolean enElSuelo;
    private boolean mirandoDerecha = true;
    private boolean moving;
    private boolean attacking;
    private boolean jumping;
    private boolean flying;

    // ── Suelo ─────────────────────────────────────────────────────────────
    public boolean isEnElSuelo()             { return enElSuelo; }
    public void    setEnElSuelo(boolean v)   { enElSuelo = v; }

    // ── Dirección ─────────────────────────────────────────────────────────
    public boolean isMirandoDerecha()           { return mirandoDerecha; }
    public void    setMirandoDerecha(boolean v) { mirandoDerecha = v; }

    // ── Movimiento ────────────────────────────────────────────────────────
    public boolean isMoving()           { return moving; }
    public void    setMoving(boolean v) { moving = v; }

    // ── Ataque ────────────────────────────────────────────────────────────
    public boolean isAttacking()           { return attacking; }
    public void    setAttacking(boolean v) { attacking = v; }

    // ── Salto ─────────────────────────────────────────────────────────────
    public boolean isJumping()           { return jumping; }
    public void    setJumping(boolean v) { jumping = v; }

    // ── Vuelo ─────────────────────────────────────────────────────────────
    public boolean isFlying()           { return flying; }
    public void    setFlying(boolean v) { flying = v; }

    /**
     * Resetea los flags volátiles al inicio de cada frame.
     * Los flags persistentes (enElSuelo, flying, mirandoDerecha) NO se resetean
     * aquí — los sincroniza la física en cada frame.
     */
    public void resetFrameFlags() {
        moving    = false;
        attacking = false;
        jumping   = false;
    }
}
