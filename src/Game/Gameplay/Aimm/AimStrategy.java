package Game.Gameplay.Aimm;

import Game.Player.Player;

/**
 * Estrategia de apuntado.
 *
 * ─── REFACTOR (Entradas v2) ───────────────────────────────────────────────────
 *
 *  · Eliminados los campos de estado de teclado (up/down/left/right/c) y la
 *    lectura directa de KeyBoard.* dentro de aim(). Esos campos eran un segundo
 *    snapshot redundante del input — el primero ya ocurre en KeyBoard.update().
 *
 *  · aim() ahora simplemente delega en calculateDirection(player), que es el
 *    método que cada estrategia concreta debe implementar. Cualquier consulta
 *    de input se hace allí via KeyBoard.getState("stateKey").
 *
 *  · dir y AorA se mantienen como estado interno pero sus setters/getters se
 *    limpian (naming corregido: AorA → aimingUpOrDown).
 */
public abstract class AimStrategy {

    private boolean dir;           // true = mirando derecha
    private boolean aimingUpOrDown;

    /** Delegación directa a la estrategia concreta. */
    public AimDirection aim(Player player) {
        return calculateDirection(player);
    }

    protected void setDir(boolean facingRight) {
        this.dir = facingRight;
    }
    public boolean getDir() {
        return dir;
    }

    protected void setAimingUpOrDown(boolean value) {
        this.aimingUpOrDown = value;
    }
    public boolean getAimingUpOrDown() {
        return aimingUpOrDown;
    }

    /**
     * Cada estrategia concreta calcula la dirección de apuntado en este método.
     * Para leer input usa KeyBoard.getState("stateKey").
     */
    protected abstract AimDirection calculateDirection(Player player);
}
