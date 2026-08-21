package Main.States;

import Display.Surface.RenderFrame;
import Inputs.MouseInput;
import Main.TemporalContext;

/**
 * Gestor de estados del juego.
 *
 * ── HRFC-DT-003 — Temporal State Pipeline ────────────────────────────────
 *
 * RESPONSABILIDAD:
 *
 * StateManager es el punto único donde el contexto temporal entra al
 * sistema de estados. Recibe el TemporalContext de GameLoop y lo delega
 * al estado activo, sin conocer detalles de gameplay.
 *
 * ARQUITECTURA:
 *
 *   GameLoop
 *     ↓ (calcula TemporalContext)
 *   StateManager
 *     ↓ (delega al estado activo)
 *   State (GameState, MenuState, PauseState, etc.)
 *
 * DESACOPLAMIENTO:
 *
 * GameLoop no conoce:
 *   - Physics2D, Player, Bullet, Weapon, World, Enemy
 *   - Detalles específicos de GameState
 *   - Qué estado está activo
 *
 * StateManager no conoce:
 *   - Cómo se calcula el tiempo (responsabilidad de GameLoop)
 *   - Detalles internos de cada estado
 *
 * State no conoce:
 *   - GameLoop
 *   - Cómo se calcula el tiempo
 *   - Otros estados
 *
 * FLUJO TEMPORAL:
 *
 * GameLoop es la ÚNICA FUENTE DE VERDAD del tiempo.
 * StateManager es el DISTRIBUIDOR del tiempo.
 * State es el CONSUMIDOR del tiempo.
 *
 * TRANSICIONES:
 *
 * Al cambiar de estado:
 *   1. activeState.onExit() — libera listeners, desconecta subsistemas
 *   2. activeState = newState
 *   3. activeState.onEnter() — registra listeners, conecta subsistemas
 *
 * El TemporalContext sigue fluyendo sin interrupción — solo cambia
 * el estado que lo consume.
 *
 * GARANTÍAS:
 *
 * - Un estado puede decidir no ejecutar simulación física
 * - Un estado pausado mantiene el flujo temporal sin romperlo
 * - No se introducen accesos directos al reloj desde estados
 * - No se introducen cálculos alternativos de deltaTime
 */
public final class StateManager {

    private State activeState;
    private final MouseInput mouseInput;

    /**
     * Crea un StateManager sin estado inicial.
     *
     * @param mouseInput referencia al sistema de input para gestionar listeners
     */
    public StateManager(MouseInput mouseInput) {
        this.mouseInput = mouseInput;
        this.activeState = null;
    }

    /**
     * Establece el estado activo.
     *
     * Si ya existe un estado activo, llama onExit() antes de reemplazarlo.
     * Luego activa el nuevo estado con onEnter().
     *
     * CONTRATO DE TRANSICIÓN:
     *   1. Si existe estado anterior → onExit()
     *   2. Cambiar referencia
     *   3. onEnter() en nuevo estado
     *
     * @param newState nuevo estado activo (no null)
     */
    public void setState(State newState) {
        if (newState == null) {
            throw new IllegalArgumentException("Estado no puede ser null");
        }

        if (activeState != null) {
            activeState.onExit();
        }

        activeState = newState;
        activeState.onEnter();
    }

    /**
     * Actualiza el estado activo con el contexto temporal.
     *
     * AUTORIDAD TEMPORAL:
     *
     * Este método recibe el TemporalContext calculado por GameLoop
     * y lo delega al estado activo. StateManager NO calcula tiempo,
     * NO modifica el contexto, NO decide si se ejecuta simulación.
     *
     * El estado activo decide cómo usar el tiempo:
     *   - GameState: propaga a WorldManager para física
     *   - MenuState: puede ignorarlo
     *   - PauseState: puede mantener simulación detenida
     *
     * @param temporalContext contexto temporal del simulation step
     */
    public void update(TemporalContext temporalContext) {
        if (activeState != null) {
            activeState.update(temporalContext);
        }
    }

    /**
     * Dibuja el estado activo en el RenderFrame.
     *
     * @param frame superficie de renderizado con sistema de capas
     */
    public void draw(RenderFrame frame) {
        if (activeState != null) {
            activeState.draw(frame);
        }
    }

    /**
     * Notifica al estado activo que las dimensiones virtuales cambiaron.
     *
     * @param newVirtualWidth nueva anchura virtual
     * @param newVirtualHeight nueva altura virtual
     */
    public void onVirtualDimensionsChanged(int newVirtualWidth, int newVirtualHeight) {
        if (activeState != null) {
            activeState.onVirtualDimensionsChanged(newVirtualWidth, newVirtualHeight);
        }
    }

    /**
     * Retorna el estado activo.
     *
     * Útil para transiciones que necesitan consultar el estado actual
     * antes de cambiarlo.
     *
     * @return estado activo o null si no hay estado
     */
    public State getActiveState() {
        return activeState;
    }

    /**
     * Verifica si hay un estado activo.
     *
     * @return true si existe un estado activo
     */
    public boolean hasActiveState() {
        return activeState != null;
    }

    /**
     * Libera recursos del estado activo y destruye el StateManager.
     *
     * Llamado al cerrar la aplicación. Ejecuta onExit() y shutdown()
     * sobre el estado activo si existe.
     */
    public void shutdown() {
        if (activeState != null) {
            activeState.onExit();
            activeState.shutdown();
            activeState = null;
        }
    }

    /**
     * Retorna la referencia a MouseInput para uso interno.
     *
     * Package-private — solo accesible desde Main.States.
     * Permite que estados concretos registren/desregistren listeners.
     *
     * @return sistema de input de ratón
     */
    MouseInput getMouseInput() {
        return mouseInput;
    }
}
