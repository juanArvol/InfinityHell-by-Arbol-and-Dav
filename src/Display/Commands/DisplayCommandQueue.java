package Display.Commands;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

/**
 * Cola de comandos centralizada para el subsistema Display.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * CAMBIO: COLAPSO DE ResizeCanvas (DEBOUNCE DE RESIZE)
 *
 * Problema anterior:
 *   El resize del canvas (arrastre con ratón) disparaba componentResized
 *   por cada pixel, y cada evento llamaba directamente onCanvasResized()
 *   que hacía destroyBS + createBS. Esto era extremadamente costoso y
 *   podía crear un bucle de retroalimentación si createBS disparaba otro
 *   componentResized desde el peer nativo.
 *
 * Solución:
 *   ResizeCanvas es ahora un comando colapsable. La cola mantiene solo el
 *   ÚLTIMO ResizeCanvas encolado: cuando llega uno nuevo, reemplaza al
 *   anterior sin ejecutarlo. Solo se procesa el tamaño final de la ráfaga.
 *
 *   Esto convierte el comportamiento de "polling por evento" en verdadero
 *   event-driven: una sola reconfiguración al final del resize, no una
 *   por pixel.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * POLÍTICA DE COLAPSO
 *
 * Comandos colapsables (solo el último prevalece):
 *   - ResizeCanvas          → el tamaño final del canvas importa, no los intermedios
 *   - ToggleFullscreen      → dos toggles consecutivos sin ejecutar = sin-op
 *   - ChangeResolution      → la resolución final importa
 *   - RecreateBufferStrategy → múltiples solicitudes = una ejecución
 *
 * ──────────────────────────────────────────────────────────────────────────
 * THREADING
 *
 *   enqueue()    → cualquier thread (ConcurrentLinkedQueue es thread-safe).
 *   drainToEDT() → solo EDT.
 */
public final class DisplayCommandQueue {

    private static final Logger LOG = Logger.getLogger(DisplayCommandQueue.class.getName());

    private static final int MAX_QUEUE_SIZE = 32;

    private final ConcurrentLinkedQueue<DisplayCommand> queue = new ConcurrentLinkedQueue<>();

    // ── Encolado ─────────────────────────────────────────────────────────────

    /**
     * Encola un comando para ejecución posterior en el EDT.
     *
     * Thread-safe. Comandos colapsables sustituyen al anterior del mismo tipo.
     */
    public void enqueue(DisplayCommand command) {
        if (command == null) throw new IllegalArgumentException("command cannot be null");

        if (queue.size() >= MAX_QUEUE_SIZE) {
            LOG.warning("DisplayCommandQueue: queue full (" + MAX_QUEUE_SIZE
                        + ") — dropping command: " + command.getClass().getSimpleName());
            return;
        }

        if (isCollapsible(command)) {
            // Para comandos colapsables: si el último es del mismo tipo, reemplazar.
            DisplayCommand last = peekLast();
            if (last != null && last.getClass() == command.getClass()) {
                replaceLastIfSameType(command);
                LOG.fine("DisplayCommandQueue: collapsed " + command.getClass().getSimpleName());
                return;
            }
        }

        queue.offer(command);
        LOG.fine("DisplayCommandQueue: enqueued " + command.getClass().getSimpleName());
    }

    /**
     * Drena todos los comandos pendientes y los pasa al executor.
     *
     * DEBE llamarse desde el EDT. Procesa en orden FIFO.
     * Si el executor lanza, se registra y se continúa (robustez).
     */
    public void drainToEDT(CommandExecutor executor) {
        assertEDT();
        DisplayCommand command;
        while ((command = queue.poll()) != null) {
            LOG.fine("DisplayCommandQueue: executing " + command.getClass().getSimpleName());
            try {
                executor.execute(command);
            } catch (Exception e) {
                LOG.warning("DisplayCommandQueue: command execution failed ["
                            + command.getClass().getSimpleName() + "]: " + e.getMessage());
            }
        }
    }

    // ── Consultas ────────────────────────────────────────────────────────────

    public boolean hasPending() { return !queue.isEmpty(); }
    public int size()           { return queue.size();     }

    // ── Privados ─────────────────────────────────────────────────────────────

    private DisplayCommand peekLast() {
        DisplayCommand last = null;
        for (DisplayCommand c : queue) last = c;
        return last;
    }

    /**
     * Reemplaza el último elemento de la cola si es del mismo tipo.
     * Reconstruye la cola preservando el orden de los otros comandos.
     */
    private void replaceLastIfSameType(DisplayCommand newCmd) {
        DisplayCommand[] snapshot = queue.toArray(new DisplayCommand[0]);
        queue.clear();
        boolean replaced = false;
        for (int i = 0; i < snapshot.length; i++) {
            if (!replaced && i == snapshot.length - 1
                    && snapshot[i].getClass() == newCmd.getClass()) {
                queue.offer(newCmd);
                replaced = true;
            } else {
                queue.offer(snapshot[i]);
            }
        }
        if (!replaced) queue.offer(newCmd);
    }

    private static boolean isCollapsible(DisplayCommand cmd) {
        return cmd instanceof DisplayCommand.ResizeCanvas
            || cmd instanceof DisplayCommand.ToggleFullscreen
            || cmd instanceof DisplayCommand.ChangeResolution
            || cmd instanceof DisplayCommand.RecreateBufferStrategy;
    }

    private static void assertEDT() {
        if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException(
                "DisplayCommandQueue.drainToEDT() must be called from the EDT");
        }
    }

    @FunctionalInterface
    public interface CommandExecutor {
        void execute(DisplayCommand command);
    }
}
