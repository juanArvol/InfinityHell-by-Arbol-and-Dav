package Game.Player;

import Game.Engine.GameMath.Logic2D.Vector2D;

/**
 * Estado lógico específico del jugador utilizado por sus subsistemas.
 *
 * ── HRFC — Player Reengineering ───────────────────────────────────────────
 *
 * ── AUDITORÍA DE RESPONSABILIDADES ───────────────────────────────────────
 *
 * PlayerState gestiona únicamente el estado lógico que los subsistemas
 * del Player (Controller, Combat, Renderer, AimSystem) necesitan leer
 * y escribir frame a frame. NO duplica estado que ya vive en EntityFlags.
 *
 * Mapa de propietarios definitivo:
 *
 *   Movement
 *     enElSuelo  → aquí  (estado de contacto físico, leído por Controller)
 *     running    → aquí  (modificador de movimiento, leído por Controller)
 *     agachado   → aquí  (postura, leído por Controller y Renderer)
 *
 *   Aim
 *     mirandoDerecha  → aquí  (orientación, única fuente de verdad)
 *     verticalAim     → aquí  (enum mutuamente excluyente: NONE/ARRIBA/ABAJO)
 *     aimDirection    → aquí  (vector normalizado para combate)
 *     aiming          → aquí  (modo apuntado activo por tecla C)
 *
 *   Combat
 *     reloading  → aquí  (estado de recarga, leído por Combat y Renderer)
 *
 *   Gameplay — estados genéricos que podrían ir a EntityFlags:
 *     congelado  → aquí  (flag de gameplay específico del Player — ver nota)
 *
 * ── NOTA SOBRE 'congelado' ────────────────────────────────────────────────
 *
 * 'congelado' representa una inhibición total del jugador (cutscenes, trampas,
 * diálogos). EntityFlags.impairments.frozen representa el efecto de estado
 * "Frozen" (hielo) que proviene del sistema de StatusEffects.
 *
 * Son conceptualmente distintos:
 *   - congelado = intención del sistema de juego de bloquear al Player
 *   - frozen    = efecto físico/elemental del sistema de efectos de estado
 *
 * La consulta compuesta correcta para "¿puede el jugador actuar?" es:
 *   !state.isCongelado() && !entityFlags.isAbleToMove() == false
 *
 * Para evitar que ambos sean necesarios simultáneamente, en el futuro
 * 'congelado' podría redirigirse a EntityFlags.states (StateFlags), pero
 * ese refactor requiere que el sistema que lo activa (cutscenes, trampas)
 * conozca EntityFlags. Mientras ese sistema no exista, congelado vive aquí
 * con el propietario actual bien documentado.
 *
 * ── CAMBIOS RESPECTO A LA VERSIÓN ANTERIOR ───────────────────────────────
 *
 *   AÑADIDO:
 *     boolean aiming — true mientras la tecla C está presionada.
 *     Getters/setters: isAiming() / setAiming(boolean).
 *
 *   SIN CAMBIOS:
 *     VerticalAim enum — sigue siendo la representación mutuamente excluyente
 *     del eje vertical. Los booleans mirandoArriba/mirandoAbajo se mantienen
 *     como delegaciones de compatibilidad.
 */
public class PlayerState {

    /**
     * Dirección vertical de apuntado.
     * Enum mutuamente excluyente: no puede ser ARRIBA y ABAJO a la vez.
     */
    public enum VerticalAim {
        NONE,
        ARRIBA,
        ABAJO
    }

    // ── Movement ──────────────────────────────────────────────────────────
    private boolean enElSuelo;
    private boolean running;
    private boolean agachado;

    // ── Aim ───────────────────────────────────────────────────────────────
    private boolean     mirandoDerecha = true;
    private VerticalAim verticalAim    = VerticalAim.NONE;
    private Vector2D    aimDirection   = new Vector2D(1, 0);

    /**
     * true mientras la tecla C está presionada (modo apuntado activo).
     *
     * Cuando aiming == true:
     *   - PlayerController inhibe el movimiento horizontal normal.
     *   - AimSelection solo actualiza el eje vertical.
     *   - El drop-through es evaluado por PlayerController si verticalAim == ABAJO
     *     y el jugador está sobre una plataforma traversable.
     */
    private boolean aiming;

    // ── Combat ────────────────────────────────────────────────────────────
    private boolean reloading;

    // ── Gameplay ──────────────────────────────────────────────────────────
    /**
     * Inhibición total del jugador por el sistema de juego, permite cancelar input para apuntado
     * Antes llamado "congelado "
     */
    private boolean apuntando;

    // ── Movement — getters/setters ────────────────────────────────────────

    public boolean isEnElSuelo()           { return enElSuelo; }
    public void    setEnElSuelo(boolean v) { enElSuelo = v; }

    public boolean isRunning()             { return running; }
    public void    setRunning(boolean v)   { running = v; }

    public boolean isAgachado()            { return agachado; }
    public void    setAgachado(boolean v)  { agachado = v; }

    // ── Aim — getters/setters ─────────────────────────────────────────────

    public boolean isDer()           { return mirandoDerecha; }
    public void    setDer(boolean v) { mirandoDerecha = v; }

    public VerticalAim getVerticalAim()              { return verticalAim; }
    public void        setVerticalAim(VerticalAim v) { verticalAim = v; }

    /** Getters de compatibilidad — delegan en el enum. */
    public boolean isMirandoArriba() { return verticalAim == VerticalAim.ARRIBA; }
    public boolean isMirandoAbajo()  { return verticalAim == VerticalAim.ABAJO;  }

    /** Setters de compatibilidad — redirigen al enum. */
    public void setMirandoArriba(boolean v) {
        if (v) verticalAim = VerticalAim.ARRIBA;
        else if (verticalAim == VerticalAim.ARRIBA) verticalAim = VerticalAim.NONE;
    }
    public void setMirandoAbajo(boolean v) {
        if (v) verticalAim = VerticalAim.ABAJO;
        else if (verticalAim == VerticalAim.ABAJO) verticalAim = VerticalAim.NONE;
    }

    /** True si hay apuntado vertical activo (ARRIBA o ABAJO). */
    public boolean isMirandoArribaOAbajo() { return verticalAim != VerticalAim.NONE; }

    public Vector2D getAimDirection()           { return aimDirection; }
    public void     setAimDirection(Vector2D d) { aimDirection = d; }

    /**
     * True mientras la tecla C está presionada (modo apuntado activo).
     * PlayerController inhibe el movimiento horizontal cuando esto es true.
     */
    public boolean isAiming()           { return aiming; }
    public void    setAiming(boolean v) { aiming = v; }

    // ── Combat — getters/setters ──────────────────────────────────────────

    public boolean isReloading()           { return reloading; }
    public void    setReloading(boolean v) { reloading = v; }

    // ── Gameplay — getters/setters ────────────────────────────────────────

    public boolean isApuntando()           { return apuntando; }
    public void    setApuntando(boolean v) { apuntando = v; }
}
