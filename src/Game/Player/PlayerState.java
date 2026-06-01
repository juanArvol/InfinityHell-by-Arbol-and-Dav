package Game.Player;

import Game.Engine.GameMath.SpaceLogic.Logic2D.Vector2D;

/**
 * Estado lógico del jugador.
 *
 * ── REFACTOR: DETECTAR Y DOCUMENTAR ESTADOS IMPOSIBLES ───────────────────
 *
 * PROBLEMA ORIGINAL:
 *   PlayerState usaba booleans separados para direcciones verticales:
 *     private boolean mirandoArriba;
 *     private boolean mirandoAbajo;
 *
 *   Nada impedía que ambos fueran true simultáneamente — un estado
 *   imposible que podría causar bugs en animaciones o lógica de disparo.
 *   El flag "congelado" convivía con "running" sin relación explícita
 *   entre ellos (si estás congelado, ¿puedes seguir corriendo?).
 *
 * SOLUCIÓN:
 *   Introducir VerticalAim enum para reemplazar los dos booleans de
 *   dirección vertical. Un enum es mutuamente excluyente por definición:
 *   no puede ser ARRIBA y ABAJO a la vez.
 *
 *   Se mantienen los getters boolean (isMirandoArriba/isMirandoAbajo)
 *   para retrocompatibilidad; internamente delegan al enum.
 *
 * BENEFICIO:
 *   - Estado imposible (arriba Y abajo) es inexpresable en el tipo.
 *   - isMirandoArribaOAbajo() sigue funcionando: verticalAim != NONE.
 *   - Agregar más estados verticales (DIAGONAL) requiere solo ampliar el enum.
 *   - El flag "congelado" queda documentado para futura refactorización.
 *
 * ── NOTA SOBRE "congelado" ────────────────────────────────────────────────
 *
 * "congelado" es un flag de gameplay con implicaciones en múltiples sistemas
 * (movement, combat, animation). Actualmente PlayerController no lo verifica —
 * si se activa, el jugador sigue moviéndose. Esto es un TODO de lógica, no
 * de arquitectura: cuando se implemente, PlayerController debe verificar
 * state.isCongelado() antes de procesar input.
 */
public class PlayerState {

    /**
     * Dirección vertical de apuntado.
     * Reemplaza los dos booleans mirandoArriba/mirandoAbajo para
     * eliminar el estado imposible "ambos true".
     */
    public enum VerticalAim {
        NONE,
        ARRIBA,
        ABAJO
    }

    private boolean     congelado;
    private boolean     enElSuelo;
    private boolean     mirandoDerecha = true;
    private VerticalAim verticalAim    = VerticalAim.NONE;
    private boolean     agachado;
    private boolean     running;
    private boolean     reloading;

    private Vector2D aimDirection = new Vector2D(1, 0);

    // ── Recarga ───────────────────────────────────────────────────────────

    public boolean isReloading()        { return reloading; }
    public void    setReloading(boolean v) { reloading = v; }

    // ── Estados básicos ───────────────────────────────────────────────────

    public boolean isCongelado()           { return congelado; }
    public void    setCongelado(boolean v) { congelado = v; }

    public boolean isEnElSuelo()           { return enElSuelo; }
    public void    setEnElSuelo(boolean v) { enElSuelo = v; }

    public boolean isDer()           { return mirandoDerecha; }
    public void    setDer(boolean v) { mirandoDerecha = v; }

    public boolean isAgachado()           { return agachado; }
    public void    setAgachado(boolean v) { agachado = v; }

    public boolean isRunning()           { return running; }
    public void    setRunning(boolean v) { running = v; }

    // ── Dirección vertical (via enum — mutuamente excluyente) ─────────────

    public VerticalAim getVerticalAim()           { return verticalAim; }
    public void        setVerticalAim(VerticalAim v) { verticalAim = v; }

    // Getters de compatibilidad — mantienen la API original
    public boolean isMirandoArriba() { return verticalAim == VerticalAim.ARRIBA; }
    public boolean isMirandoAbajo()  { return verticalAim == VerticalAim.ABAJO; }

    // Setters de compatibilidad — redirigen al enum
    public void setMirandoArriba(boolean v) {
        if (v) verticalAim = VerticalAim.ARRIBA;
        else if (verticalAim == VerticalAim.ARRIBA) verticalAim = VerticalAim.NONE;
    }
    public void setMirandoAbajo(boolean v) {
        if (v) verticalAim = VerticalAim.ABAJO;
        else if (verticalAim == VerticalAim.ABAJO) verticalAim = VerticalAim.NONE;
    }

    public boolean isMirandoArribaOAbajo() {
        return verticalAim != VerticalAim.NONE;
    }

    // ── Aim ───────────────────────────────────────────────────────────────

    public Vector2D getAimDirection()           { return aimDirection; }
    public void     setAimDirection(Vector2D d) { aimDirection = d; }
}
