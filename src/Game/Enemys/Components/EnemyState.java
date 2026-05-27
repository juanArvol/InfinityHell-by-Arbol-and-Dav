package Game.Enemys.Components;

/**
 * Estado observable del enemigo — flags de animación y física.
 * Sin cambios funcionales respecto al original.
 * Limpieza: comentarios agrupados, sin separadores innecesarios.
 */
public class EnemyState {

    private boolean enElSuelo;
    private boolean mirandoDerecha = true;
    private boolean moving;
    private boolean attacking;
    private boolean jumping;
    private boolean flying;

    // ── Suelo ─────────────────────────────────────────────────────────────
    public boolean isEnElSuelo()             { return enElSuelo; }
    public void    setEnElSuelo(boolean v)   { enElSuelo = v; }

    // ── Dirección ────────────────────────────────────────────────────────
    public boolean isMirandoDerecha()           { return mirandoDerecha; }
    public void    setMirandoDerecha(boolean v) { mirandoDerecha = v; }

    // ── Movimiento ───────────────────────────────────────────────────────
    public boolean isMoving()           { return moving; }
    public void    setMoving(boolean v) { moving = v; }

    // ── Ataque ───────────────────────────────────────────────────────────
    public boolean isAttacking()           { return attacking; }
    public void    setAttacking(boolean v) { attacking = v; }

    // ── Salto ────────────────────────────────────────────────────────────
    public boolean isJumping()           { return jumping; }
    public void    setJumping(boolean v) { jumping = v; }

    // ── Vuelo ────────────────────────────────────────────────────────────
    public boolean isFlying()           { return flying; }
    public void    setFlying(boolean v) { flying = v; }
}
