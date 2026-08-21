package Main.States;

import Display.Surface.LayerIndex;
import Display.Surface.RenderFrame;
import Main.TemporalContext;
import java.awt.Color;
import java.awt.Graphics2D;

/**
 * Estado mínimo de prueba arquitectónica.
 *
 * ── HRFC-DT-003 — Temporal State Pipeline ────────────────────────────────
 *
 * PROPÓSITO:
 *
 * IdleState es una prueba arquitectónica que demuestra que el flujo temporal
 * está correctamente desacoplado de GameState:
 *
 *   GameLoop
 *     ↓ (calcula TemporalContext)
 *   StateManager
 *     ↓ (delega)
 *   IdleState (o GameState)
 *
 * CARACTERÍSTICAS:
 *
 * - Recibe TemporalContext como cualquier otro estado
 * - NO ejecuta simulación física (decisión del estado)
 * - Dibuja una pantalla simple para verificar que el render funciona
 * - Implementa ciclo de vida completo (onEnter, onExit, shutdown)
 *
 * VERIFICACIÓN ARQUITECTÓNICA:
 *
 * Este estado prueba que:
 *   1. El tiempo no depende estructuralmente de GameState
 *   2. El contexto temporal puede fluir a estados alternativos
 *   3. Un estado puede decidir NO ejecutar simulación
 *   4. GameLoop no conoce detalles de gameplay
 *   5. La transición entre estados mantiene el flujo temporal
 *
 * NO ES:
 *
 * - Un menú completo funcional
 * - Un sistema de pausa con lógica compleja
 * - Una implementación final de estado
 *
 * ES:
 *
 * - Una prueba de concepto mínima
 * - Un demostrador arquitectónico
 * - Un validador del desacoplamiento temporal
 */
public final class IdleState implements State {

    private int virtualWidth;
    private int virtualHeight;

    /**
     * Contador de frames para animación simple.
     *
     * Acumula el tiempo real transcurrido para demostrar que
     * IdleState recibe correctamente el TemporalContext,
     * aunque no ejecute simulación física.
     */
    private double accumulatedTime = 0.0;

    /**
     * Crea un IdleState con dimensiones virtuales.
     *
     * @param virtualWidth anchura virtual inicial
     * @param virtualHeight altura virtual inicial
     */
    public IdleState(int virtualWidth, int virtualHeight) {
        this.virtualWidth = virtualWidth;
        this.virtualHeight = virtualHeight;
    }

    // ── State interface implementation ────────────────────────────────────────

    /**
     * Actualiza el estado idle.
     *
     * ── HRFC-DT-003: Estado sin simulación física ────────────────────────
     *
     * IdleState recibe el TemporalContext pero NO ejecuta simulación física.
     * Esto demuestra que:
     *   - El flujo temporal funciona independiente de GameState
     *   - Un estado puede decidir no utilizar simulationDeltaTime
     *   - El contexto fluye correctamente a cualquier State
     *
     * Para demostración, acumula tiempo para una animación simple.
     */
    @Override
    public void update(TemporalContext temporalContext) {
        // Acumular tiempo real para animación de prueba
        accumulatedTime += temporalContext.getDeltaTime();

        // NO ejecuta física, NO actualiza WorldManager, NO mueve entidades.
        // Esta es una decisión arquitectónica válida del estado.
    }

    /**
     * Dibuja el estado idle.
     *
     * ── HRFC-DT-003: Renderizado mínimo de prueba ────────────────────────
     *
     * Dibuja una pantalla simple que demuestra:
     *   - El render funciona con cualquier State
     *   - El sistema de capas está disponible
     *   - El estado recibe RenderFrame correctamente
     */
    @Override
    public void draw(RenderFrame frame) {
        Graphics2D g = frame.getLayerGraphics(LayerIndex.OVERLAY);

        // Fondo oscuro
        g.setColor(new Color(20, 20, 30));
        g.fillRect(0, 0, virtualWidth, virtualHeight);

        // Texto identificador
        g.setColor(Color.WHITE);
        g.drawString("IDLE STATE", 20, 30);
        g.drawString("Architectural Proof — HRFC-DT-003", 20, 50);

        // Demostración de que recibe tiempo correctamente
        g.drawString(String.format("Accumulated Time: %.2fs", accumulatedTime), 20, 80);
        g.drawString(String.format("Virtual Resolution: %dx%d", virtualWidth, virtualHeight), 20, 100);

        // Animación simple: círculo pulsante basado en tiempo real
        int centerX = virtualWidth / 2;
        int centerY = virtualHeight / 2;
        double pulse = Math.sin(accumulatedTime * 2.0) * 0.5 + 0.5;
        int radius = (int)(30 + pulse * 20);

        g.setColor(new Color(100, 150, 255));
        g.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);

        g.setColor(Color.WHITE);
        g.drawString("Temporal Context Active", centerX - 70, centerY + radius + 30);
    }

    /**
     * Notifica cambio de dimensiones virtuales.
     */
    @Override
    public void onVirtualDimensionsChanged(int newVirtualWidth, int newVirtualHeight) {
        this.virtualWidth = newVirtualWidth;
        this.virtualHeight = newVirtualHeight;
    }

    /**
     * Llamado al activar el estado.
     *
     * IdleState no tiene subsistemas que requieran inicialización.
     */
    @Override
    public void onEnter() {
        // Reset tiempo acumulado al entrar
        accumulatedTime = 0.0;
    }

    /**
     * Llamado al desactivar el estado.
     *
     * IdleState no tiene listeners que desconectar.
     */
    @Override
    public void onExit() {
        // Sin recursos que liberar en transición
    }

    /**
     * Libera recursos permanentemente.
     *
     * IdleState no tiene recursos permanentes que liberar.
     */
    @Override
    public void shutdown() {
        // Sin ExecutorService, sin listeners, sin recursos externos
    }
}
