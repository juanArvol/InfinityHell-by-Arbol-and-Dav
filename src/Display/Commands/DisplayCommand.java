package Display.Commands;

import Display.State.DisplayMode;
import Display.State.Resolution;

/**
 * Comando sellado para el subsistema Display.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * CAMBIO: ADICIÓN DE ResizeCanvas
 *
 * ResizeCanvas encapsula un evento de resize del canvas como un comando
 * explícito que pasa por la cola. Esto permite:
 *
 *   1. DEBOUNCE: La cola colapsa los ResizeCanvas consecutivos al último,
 *      de modo que solo se procesa el tamaño final de una ráfaga de resize.
 *      Elimina destroyBS+createBS en cada pixel de arrastre del ratón.
 *
 *   2. IDEMPOTENCIA: Si el canvas ya tiene ese tamaño (mismo w y h que el
 *      viewport actual), el pipeline lo descarta sin recalcular nada.
 *
 *   3. UNIFICACIÓN: El resize pasa por el mismo pipeline que fullscreen
 *      y todos los demás comandos. No existe un camino especial.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * DISEÑO
 *
 * Interfaz sellada con records para cada tipo. Inmutables y thread-safe.
 */
public sealed interface DisplayCommand
    permits DisplayCommand.ToggleFullscreen,
            DisplayCommand.EnterFullscreen,
            DisplayCommand.ExitFullscreen,
            DisplayCommand.SetDisplayMode,
            DisplayCommand.ChangeResolution,
            DisplayCommand.ChangeMonitor,
            DisplayCommand.RestoreWindow,
            DisplayCommand.RecreateBufferStrategy,
            DisplayCommand.ResizeCanvas {

    // ── Comandos existentes ───────────────────────────────────────────────────

    record ToggleFullscreen() implements DisplayCommand {}

    record EnterFullscreen(DisplayMode targetMode) implements DisplayCommand {
        public EnterFullscreen {
            if (!targetMode.isFullscreen()) {
                throw new IllegalArgumentException(
                    "EnterFullscreen requires a fullscreen mode, got: " + targetMode);
            }
        }
    }

    record ExitFullscreen() implements DisplayCommand {}

    record SetDisplayMode(DisplayMode mode) implements DisplayCommand {}

    record ChangeResolution(Resolution resolution) implements DisplayCommand {}

    record ChangeMonitor(int monitorIndex) implements DisplayCommand {
        public ChangeMonitor {
            if (monitorIndex < 0) throw new IllegalArgumentException(
                "monitorIndex must be >= 0, got: " + monitorIndex);
        }
    }

    record RestoreWindow() implements DisplayCommand {}

    record RecreateBufferStrategy() implements DisplayCommand {}

    // ── Nuevo comando ─────────────────────────────────────────────────────────

    /**
     * Notifica que el canvas ha cambiado de tamaño a las dimensiones dadas.
     *
     * Este comando es encolado por el ComponentListener de WindowManager
     * en lugar de ejecutarse directamente. La cola colapsa ráfagas de resize
     * al último valor, evitando destroyBS+createBS por cada pixel de arrastre.
     *
     * El pipeline lo descarta si las dimensiones son idénticas al estado actual,
     * garantizando idempotencia completa.
     *
     * @param width   nuevo ancho del canvas (> 0)
     * @param height  nuevo alto del canvas (> 0)
     */
    record ResizeCanvas(int width, int height) implements DisplayCommand {
        public ResizeCanvas {
            if (width  <= 0) throw new IllegalArgumentException("width must be > 0");
            if (height <= 0) throw new IllegalArgumentException("height must be > 0");
        }
    }
}
