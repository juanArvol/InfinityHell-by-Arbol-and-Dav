package Display.Commands;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.logging.Logger;

/**
 * Cola de comandos centralizada para el subsistema Display.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * CORRECCIÓN: RACE CONDITION EN replaceLastIfSameType()
 *
 * Problema anterior:
 *   La cola usaba ConcurrentLinkedQueue, que no ofrece operaciones compuestas
 *   atómicas. replaceLastIfSameType() hacía toArray() + clear() + re-offer:
 *   entre clear() y el re-offer, cualquier thread concurrente que llamara
 *   enqueue() encolaba un comando que era descartado silenciosamente por el
 *   clear(). Esto podía perder comandos bajo concurrencia.
 *
 * Solución:
 *   La cola usa ArrayDeque protegida con synchronized. El acceso de escritura
 *   ocurre solo desde el thread encolador (cualquier thread) y desde el
 *   drain (EDT). La contención real es mínima — enqueue es O(1) y drain
 *   solo ocurre en el EDT — por lo que synchronized es suficiente y no
 *   introduce latencia perceptible.
 *
 *   Con synchronized, toArray/clear/re-offer son una región crítica atómica.
 *   No existe ventana de carrera en ninguna operación compuesta.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * POLÍTICA DE COLAPSO
 *
 * Comandos colapsables (solo el último prevalece):
 *   - ResizeCanvas            → el tamaño final del canvas importa, no los intermedios
 *   - ToggleFullscreen        → dos toggles consecutivos sin ejecutar = sin-op
 *   - ChangeResolution        → la resolución final importa
 *   - RecreateBufferStrategy  → múltiples solicitudes = una ejecución
 *
 * ──────────────────────────────────────────────────────────────────────────
 * THREADING
 *
 *   enqueue()    → cualquier thread; synchronized sobre la cola.
 *   drainToEDT() → solo EDT; synchronized sobre la cola durante poll.
 */
public final class DisplayCommandQueue {

    private static final Logger LOG = Logger.getLogger(DisplayCommandQueue.class.getName());

    private static final int MAX_QUEUE_SIZE = 32;

    /** Protegida con synchronized en todos los accesos. */
    private final Deque<DisplayCommand> queue = new ArrayDeque<>();

    // ── Encolado ─────────────────────────────────────────────────────────────

    /**
     * Encola un comando para ejecución posterior en el EDT.
     *
     * Thread-safe. Comandos colapsables sustituyen al anterior del mismo tipo.
     */
    public void enqueue(DisplayCommand command) {
        if (command == null) throw new IllegalArgumentException("command cannot be null");

        synchronized (queue) {
            if (queue.size() >= MAX_QUEUE_SIZE) {
                LOG.warning("DisplayCommandQueue: queue full (" + MAX_QUEUE_SIZE
                            + ") — dropping command: " + command.getClass().getSimpleName());
                return;
            }

            if (isCollapsible(command)) {
                DisplayCommand last = queue.peekLast();
                if (last != null && last.getClass() == command.getClass()) {
                    // Reemplazar el último del mismo tipo — atómico dentro del lock.
                    replaceLastIfSameType(command);
                    LOG.fine("DisplayCommandQueue: collapsed " + command.getClass().getSimpleName());
                    return;
                }
            }

            queue.offerLast(command);
            LOG.fine("DisplayCommandQueue: enqueued " + command.getClass().getSimpleName());
        }
    }

    /**
     * Drena todos los comandos pendientes y los pasa al executor.
     *
     * DEBE llamarse desde el EDT. Procesa en orden FIFO.
     * Si el executor lanza, se registra y se continúa (robustez).
     */
    public void drainToEDT(CommandExecutor executor) {
        assertEDT();

        // Extraer todos los comandos actuales en una sola región crítica.
        // Así el executor corre fuera del lock y no bloquea enqueue().
        DisplayCommand[] snapshot;
        synchronized (queue) {
            if (queue.isEmpty()) return;
            snapshot = queue.toArray(new DisplayCommand[0]);
            queue.clear();
        }

        for (DisplayCommand command : snapshot) {
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

    public boolean hasPending() {
        synchronized (queue) { return !queue.isEmpty(); }
    }

    public int size() {
        synchronized (queue) { return queue.size(); }
    }

    // ── Privados ─────────────────────────────────────────────────────────────

    /**
     * Reemplaza el último elemento de la cola si es del mismo tipo que newCmd.
     * Llamar únicamente dentro de un bloque synchronized(queue).
     */
    private void replaceLastIfSameType(DisplayCommand newCmd) {
        // Reconstruir la cola reemplazando el último elemento del mismo tipo.
        // Operación completamente contenida dentro del lock — atómica para el exterior.
        DisplayCommand[] snapshot = queue.toArray(new DisplayCommand[0]);
        queue.clear();
        boolean replaced = false;
        for (int i = 0; i < snapshot.length; i++) {
            if (!replaced && i == snapshot.length - 1
                    && snapshot[i].getClass() == newCmd.getClass()) {
                queue.offerLast(newCmd);
                replaced = true;
            } else {
                queue.offerLast(snapshot[i]);
            }
        }
        if (!replaced) queue.offerLast(newCmd);
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
