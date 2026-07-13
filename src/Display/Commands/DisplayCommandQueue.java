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
 * ──────────────────────────────────────────────────────────────────────────
 * POLÍTICA DE COLAPSO — DOS SEMÁNTICAS DISTINTAS
 *
 * 1. LAST-WINS (isLastWins): el último valor reemplaza al anterior.
 *    Correcto cuando solo importa el estado final, no la cantidad.
 *      - ResizeCanvas           → importa el tamaño final, no los intermedios.
 *      - ChangeResolution       → idem.
 *      - RecreateBufferStrategy → múltiples solicitudes = una ejecución.
 *      - ChangeBackground       → importa el fondo final.
 *
 * 2. INVERTIBLE (isInvertible): los pares se anulan, los impares se ejecutan.
 *    Correcto cuando el comando es su propio inverso — N ejecuciones impares
 *    producen el mismo resultado que 1, y N ejecuciones pares son un no-op.
 *      - ToggleFullscreen → 2 toggles pendientes = volver al estado original
 *                           = no-op. 3 toggles = un toggle neto.
 *
 * ── POR QUÉ ToggleFullscreen NO puede ser LAST-WINS ──────────────────────
 * Con la política anterior (last-wins), pulsar F11 seis veces rápido
 * colapsaba a 1 ToggleFullscreen: la cola siempre mantenía exactamente 1,
 * reemplazando cada nuevo toggle por el mismo comando. El resultado era
 * siempre 1 toggle ejecutado, independientemente de la paridad de pulsaciones.
 *
 * Con 6 pulsaciones, el usuario espera volver al estado original (6 toggles
 * = 3 ida/vuelta = sin cambio neto). Con last-wins obtenía 1 toggle = estado
 * contrario. El Display quedaba en el modo incorrecto tras alternar rápidamente,
 * produciendo el comportamiento erróneo observable con F11 mantenido.
 *
 * Con la política invertible:
 *   Pulsación 1: cola vacía → añadir ToggleFullscreen.
 *   Pulsación 2: hay 1 en cola → eliminar (paridad par = sin-op).
 *   Pulsación 3: cola vacía → añadir ToggleFullscreen.
 *   Pulsación 4: hay 1 en cola → eliminar.
 *   ...
 *   N impares → 1 ToggleFullscreen ejecutado. N pares → 0 ejecutados.
 *   El resultado siempre refleja la intención real del usuario.
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
     * Thread-safe. Aplica la política de colapso correspondiente al tipo:
     *   - Invertibles: se cancela con el anterior del mismo tipo si existe.
     *   - Last-wins:   reemplaza al anterior del mismo tipo si existe.
     *   - Sin política: se añade al final sin restricción.
     */
    public void enqueue(DisplayCommand command) {
        if (command == null) throw new IllegalArgumentException("command cannot be null");

        synchronized (queue) {
            if (queue.size() >= MAX_QUEUE_SIZE) {
                LOG.warning("DisplayCommandQueue: queue full (" + MAX_QUEUE_SIZE
                            + ") — dropping: " + command.getClass().getSimpleName());
                return;
            }

            if (isInvertible(command)) {
                // Política invertible: si ya existe uno del mismo tipo, eliminarlo
                // (paridad par = operaciones que se cancelan). Si no existe, añadir.
                if (removeLastOfType(command.getClass())) {
                    LOG.fine("DisplayCommandQueue: cancelled (invertible) "
                             + command.getClass().getSimpleName());
                    return;
                }
                // No había ninguno: añadir.
                queue.offerLast(command);
                LOG.fine("DisplayCommandQueue: enqueued (invertible) "
                         + command.getClass().getSimpleName());
                return;
            }

            if (isLastWins(command)) {
                // Política last-wins: si ya existe uno del mismo tipo, reemplazarlo.
                if (replaceLastOfType(command)) {
                    LOG.fine("DisplayCommandQueue: collapsed (last-wins) "
                             + command.getClass().getSimpleName());
                    return;
                }
                // No había ninguno: añadir.
                queue.offerLast(command);
                LOG.fine("DisplayCommandQueue: enqueued (last-wins) "
                         + command.getClass().getSimpleName());
                return;
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
     * Elimina la última ocurrencia del tipo dado de la cola.
     *
     * Llamar únicamente dentro de un bloque synchronized(queue).
     *
     * @return true si se encontró y eliminó una ocurrencia; false si no había ninguna.
     */
    private boolean removeLastOfType(Class<? extends DisplayCommand> type) {
        DisplayCommand[] snapshot = queue.toArray(new DisplayCommand[0]);

        int lastIdx = -1;
        for (int i = snapshot.length - 1; i >= 0; i--) {
            if (snapshot[i].getClass() == type) {
                lastIdx = i;
                break;
            }
        }

        if (lastIdx < 0) return false;

        // Reconstruir la cola sin el elemento en lastIdx.
        queue.clear();
        for (int i = 0; i < snapshot.length; i++) {
            if (i != lastIdx) queue.offerLast(snapshot[i]);
        }
        return true;
    }

    /**
     * Reemplaza la última ocurrencia del mismo tipo que newCmd en la cola.
     *
     * Llamar únicamente dentro de un bloque synchronized(queue).
     *
     * @return true si se encontró y reemplazó una ocurrencia; false si no había ninguna.
     */
    private boolean replaceLastOfType(DisplayCommand newCmd) {
        DisplayCommand[] snapshot = queue.toArray(new DisplayCommand[0]);

        int lastIdx = -1;
        for (int i = snapshot.length - 1; i >= 0; i--) {
            if (snapshot[i].getClass() == newCmd.getClass()) {
                lastIdx = i;
                break;
            }
        }

        if (lastIdx < 0) return false;

        // Reconstruir la cola sustituyendo la posición lastIdx.
        queue.clear();
        for (int i = 0; i < snapshot.length; i++) {
            queue.offerLast(i == lastIdx ? newCmd : snapshot[i]);
        }
        return true;
    }

    /**
     * Comandos invertibles: se cancelan con el anterior del mismo tipo.
     * N pares pendientes = sin-op. N impares pendientes = 1 ejecución neta.
     */
    private static boolean isInvertible(DisplayCommand cmd) {
        return cmd instanceof DisplayCommand.ToggleFullscreen;
    }

    /**
     * Comandos last-wins: el último valor reemplaza al anterior del mismo tipo.
     * Solo importa el estado final, no la cantidad de solicitudes.
     *
     * SuspendRendering y ResumeRendering son last-wins sobre sí mismos:
     *   - Dos Suspend seguidos colapsan a uno (idempotente).
     *   - Dos Resume seguidos colapsan a uno (idempotente).
     *   - Un Suspend + un Resume son tipos distintos: no se cancelan entre
     *     sí y se procesan en orden FIFO, lo cual es el comportamiento correcto.
     */
    private static boolean isLastWins(DisplayCommand cmd) {
        return cmd instanceof DisplayCommand.ResizeCanvas
            || cmd instanceof DisplayCommand.ChangeResolution
            || cmd instanceof DisplayCommand.RecreateBufferStrategy
            || cmd instanceof DisplayCommand.ChangeBackground
            || cmd instanceof DisplayCommand.SuspendRendering
            || cmd instanceof DisplayCommand.ResumeRendering;
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
