package Display.Commands;

import Display.Background.DisplayBackground;
import Display.State.DisplayMode;
import Display.State.Resolution;

/**
 * Comando sellado para el subsistema Display.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * EVOLUCIÓN: CICLO DE VIDA DE VENTANA COMPLETO
 *
 * Todos los eventos que afectan al estado de la ventana o de la superficie
 * de render se representan ahora como comandos explícitos. El invariante
 * central del subsistema — todo cambio de estado pasa por la CommandQueue —
 * cubre ahora el ciclo de vida completo definido en el diseño arquitectónico:
 *
 *   Creación / init           → gestionado en DisplayManager.init() (invokeAndWait)
 *   Resize del canvas         → ResizeCanvas
 *   Fullscreen ↔ windowed     → ToggleFullscreen / EnterFullscreen / ExitFullscreen
 *   Cambio de resolución      → ChangeResolution
 *   Cambio de monitor         → ChangeMonitor
 *   Cambio de fondo           → ChangeBackground
 *   Restaurar ventana         → RestoreWindow
 *   Recrear BufferStrategy    → RecreateBufferStrategy
 *   Pérdida de foco / Alt+Tab → SuspendRendering
 *   Recuperación de foco      → ResumeRendering
 *   Minimización              → gestionado en WindowManager (suppressResize)
 *                               + ResumeRendering al deiconificar
 *   Cierre de ventana         → gestionado en GameOrquester (windowCloseListener)
 *
 * ──────────────────────────────────────────────────────────────────────────
 * SuspendRendering / ResumeRendering
 *
 * Motivación:
 *   Antes del HRFC-002, los eventos focus-lost / windowDeactivated no tenían
 *   representación en el sistema de comandos. La surface permanecía publicada
 *   durante Alt+Tab. El GameLoop seguía intentando renderizar sobre una BS
 *   que el OS podía haber invalidado (especialmente en FULLSCREEN_EXCLUSIVE).
 *
 *   Los eventos windowActivated / windowDeactivated y windowGainedFocus /
 *   windowLostFocus no disparaban ninguna acción. El deiconify ya encolaba
 *   RecreateBufferStrategy, pero el caso de Alt+Tab sin iconificación
 *   (frecuente en BORDERLESS_FULLSCREEN) quedaba sin cubrir.
 *
 * SuspendRendering:
 *   Señala al pipeline que la ventana perdió activación o foco.
 *   El pipeline establece SurfaceState.SUSPENDED y desactiva la ReadinessGate
 *   sin destruir la superficie. El GameLoop descarta frames silenciosamente.
 *   Esto es más eficiente que RecreateBufferStrategy porque no destruye ni
 *   reconstruye la BS cuando no es necesario.
 *
 * ResumeRendering:
 *   Señala que la ventana recuperó activación o foco.
 *   El pipeline verifica si la BS sigue siendo válida (contentsLost) y, si
 *   es necesario, la reconstruye. Si la BS sigue siendo válida, simplemente
 *   vuelve a abrir la ReadinessGate estableciendo SurfaceState.READY.
 *   Esto cubre Alt+Tab en BORDERLESS_FULLSCREEN sin reconstrucción innecesaria.
 *
 * Ambos comandos son last-wins en la cola: si hay dos SuspendRendering
 * consecutivos, se colapsan a uno. Si hay un Suspend seguido de un Resume,
 * ambos quedan en cola y se ejecutan en orden (semántica de cancelación
 * solo aplica a comandos invertibles, no a estos).
 *
 * ──────────────────────────────────────────────────────────────────────────
 * POLÍTICA DE COLAPSO
 *
 *   LAST-WINS  (isLastWins):   ResizeCanvas, ChangeResolution,
 *                               RecreateBufferStrategy, ChangeBackground,
 *                               SuspendRendering, ResumeRendering.
 *
 *   INVERTIBLE (isInvertible): ToggleFullscreen.
 *
 *   SIN POLÍTICA:              todos los demás (se encolan sin colapso).
 *
 * Nota sobre SuspendRendering/ResumeRendering y last-wins:
 *   Cada tipo colapsa solo con sí mismo. Un Suspend + un Resume son tipos
 *   distintos, por lo que no se cancelan entre sí — ambos se procesan en
 *   orden FIFO. Solo si llegan dos Suspend seguidos el segundo reemplaza
 *   al primero (idempotente). Igual para dos Resume seguidos.
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
            DisplayCommand.ResizeCanvas,
            DisplayCommand.ChangeBackground,
            DisplayCommand.SuspendRendering,
            DisplayCommand.ResumeRendering {

    // ── Comandos de modo de presentación ─────────────────────────────────────

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

    // ── Comandos de configuración ─────────────────────────────────────────────

    record ChangeResolution(Resolution resolution) implements DisplayCommand {}

    record ChangeMonitor(int monitorIndex) implements DisplayCommand {
        public ChangeMonitor {
            if (monitorIndex < 0) throw new IllegalArgumentException(
                "monitorIndex must be >= 0, got: " + monitorIndex);
        }
    }

    record RestoreWindow() implements DisplayCommand {}

    record RecreateBufferStrategy() implements DisplayCommand {}

    // ── Comando de resize de canvas ───────────────────────────────────────────

    /**
     * Notifica que el canvas ha cambiado de tamaño a las dimensiones dadas.
     *
     * Encolado por el ComponentListener de WindowManager. La cola colapsa
     * ráfagas de resize al último valor. El pipeline lo descarta si las
     * dimensiones son idénticas al estado actual (idempotencia).
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

    // ── Comando de fondo ──────────────────────────────────────────────────────

    /**
     * Cambia el fondo del framebuffer virtual.
     *
     * El pipeline actualiza su background activo y reconstruye la superficie
     * para que el nuevo fondo se aplique desde el siguiente frame.
     * Colapsable: si se encolan varios seguidos, solo el último importa.
     *
     * @param background nuevo fondo (no puede ser null)
     */
    record ChangeBackground(DisplayBackground background) implements DisplayCommand {
        public ChangeBackground {
            if (background == null) throw new IllegalArgumentException("background cannot be null");
        }
    }

    // ── Comandos de ciclo de vida de activación ───────────────────────────────

    /**
     * Señala que la ventana perdió activación o foco del sistema operativo.
     *
     * Causas típicas:
     *   - Alt+Tab a otra aplicación (windowDeactivated).
     *   - La ventana perdió el foco del teclado (windowLostFocus).
     *   - Minimización (windowIconified) — gestionada independientemente por
     *     WindowManager, pero el resultado final es el mismo: la ventana no
     *     está activa y el GameLoop no debe intentar renderizar.
     *
     * Efecto en el pipeline:
     *   SurfaceState → SUSPENDED. La ReadinessGate se cierra.
     *   La superficie NO se destruye: si la BS sigue siendo válida cuando
     *   se recupere el foco, no habrá coste de reconstrucción.
     *
     * Colapsable (last-wins): múltiples SuspendRendering consecutivos
     * colapsan a uno solo — es idempotente entrar en estado suspendido.
     */
    record SuspendRendering() implements DisplayCommand {}

    /**
     * Señala que la ventana recuperó activación o foco del sistema operativo.
     *
     * Causas típicas:
     *   - El usuario volvió a la ventana del juego (windowActivated).
     *   - La ventana recuperó el foco del teclado (windowGainedFocus).
     *   - Desiconificación (windowDeiconified) — la surface puede necesitar
     *     reconstrucción tras una minimización en FULLSCREEN_EXCLUSIVE.
     *
     * Efecto en el pipeline:
     *   Si la BS sigue siendo válida: SurfaceState → READY. Gate se abre.
     *   Si la BS perdió contenido: reconstruye la superficie antes de abrir.
     *   En FULLSCREEN_EXCLUSIVE tras iconificación, siempre reconstruye.
     *
     * Colapsable (last-wins): múltiples ResumeRendering consecutivos
     * colapsan a uno solo — es idempotente recuperar el estado activo.
     *
     * @param requiresRebuild si true, el pipeline reconstruirá la BS
     *                        incondicionalmente sin verificar contentsLost.
     *                        Usado al restaurar desde iconificación.
     */
    record ResumeRendering(boolean requiresRebuild) implements DisplayCommand {}
}
