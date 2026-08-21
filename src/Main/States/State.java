package Main.States;

import Display.Surface.RenderFrame;
import Main.TemporalContext;

/**
 * Contrato de un estado del juego.
 *
 * ── HRFC-DT-003 — Temporal State Pipeline ────────────────────────────────
 *
 * ARQUITECTURA OBJETIVO:
 *
 * El flujo temporal debe estar separado del concepto específico de GameState:
 *
 *   GameLoop
 *     ↓
 *   TemporalContext
 *     ↓
 *   StateManager
 *     ↓
 *   State (activo)
 *     │
 *     ├── GameState
 *     ├── MenuState
 *     ├── PauseState
 *     └── etc.
 *
 * PRINCIPIO FUNDAMENTAL:
 *
 * El tiempo pertenece al ciclo de ejecución (GameLoop).
 * El estado activo decide cómo utilizar ese tiempo.
 *
 * CONTRATO:
 *
 * Todo estado implementa esta interfaz y recibe:
 *   - TemporalContext: el contexto temporal calculado por GameLoop
 *   - RenderFrame: la superficie de renderizado con sistema de capas
 *
 * DECISIÓN DEL ESTADO:
 *
 * Un estado puede:
 *   - Ejecutar simulación física (GameState)
 *   - No ejecutar simulación (MenuState, PauseState)
 *   - Pausar el tiempo sin destruir el flujo temporal global
 *
 * PROHIBICIÓN:
 *
 * Un estado NO debe:
 *   - Obtener tiempo directamente del sistema (System.nanoTime())
 *   - Calcular su propio deltaTime
 *   - Acceder al GameLoop para consultar tiempo
 *
 * El flujo es unidireccional:
 *   GameLoop → TemporalContext → State
 */
public interface State {

    /**
     * Actualiza la lógica del estado.
     *
     * AUTORIDAD TEMPORAL:
     *
     * El contexto temporal es calculado por GameLoop y propagado
     * inmutablemente a través del StateManager hasta este método.
     * El estado decide si utiliza simulationDeltaTime para física
     * o ignora el tiempo (menú estático, pausa, etc.).
     *
     * EJEMPLOS:
     *   - GameState: propaga contexto a WorldManager para física
     *   - MenuState: puede animar UI pero no ejecutar física
     *   - PauseState: puede mantener simulación detenida
     *
     * @param temporalContext contexto temporal del simulation step
     */
    void update(TemporalContext temporalContext);

    /**
     * Dibuja el estado en el RenderFrame.
     *
     * SISTEMA DE CAPAS:
     *
     * El estado dibuja en las capas apropiadas del RenderFrame
     * según la arquitectura HRFC-001. El orden visual está declarado
     * en LayerIndex.ordinal(), no en el orden de llamada.
     *
     * EJEMPLOS:
     *   - GameState: dibuja mundo en WORLD_ENTITIES, HUD en HUD, etc.
     *   - MenuState: puede dibujar solo en UI o OVERLAY
     *   - PauseState: puede dibujar overlay sobre el juego pausado
     *
     * @param frame superficie de renderizado con capas
     */
    void draw(RenderFrame frame);

    /**
     * Notifica al estado que las dimensiones virtuales han cambiado.
     *
     * Llamado cuando el DisplayManager cambia la resolución virtual.
     * El estado debe actualizar sus subsistemas (cámara, UI, etc.).
     *
     * @param newVirtualWidth nueva anchura virtual
     * @param newVirtualHeight nueva altura virtual
     */
    void onVirtualDimensionsChanged(int newVirtualWidth, int newVirtualHeight);

    /**
     * Llamado cuando el estado se activa.
     *
     * Permite al estado realizar inicialización o reconexión de listeners.
     * Por ejemplo, GameState registra PlayerCombat en MouseInput.
     */
    void onEnter();

    /**
     * Llamado cuando el estado se desactiva.
     *
     * Permite al estado realizar limpieza o desconexión de listeners.
     * Por ejemplo, GameState desregistra PlayerCombat de MouseInput.
     */
    void onExit();

    /**
     * Libera recursos del estado.
     *
     * Llamado cuando el estado es destruido permanentemente.
     * Debe liberar ExecutorServices, cerrar recursos, etc.
     */
    void shutdown();
}
