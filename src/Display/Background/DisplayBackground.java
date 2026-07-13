package Display.Background;

import java.awt.Graphics2D;

/**
 * Contrato para el sistema de fondo del Display.
 *
 * El Display delega la limpieza y relleno del framebuffer virtual en una
 * implementación de esta interfaz antes de renderizar la escena.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * MOTIVACIÓN
 *
 * Problema original:
 *   El inicio de cada frame tenía el fondo hardcodeado:
 *
 *     g.setBackground(Color.BLACK);
 *     g.clearRect(0, 0, virtualWidth, virtualHeight);
 *
 *   El color negro no era configurable, y la responsabilidad de "cómo se
 *   limpia el frame" estaba mezclada con la de "gestionar el framebuffer".
 *
 * Solución:
 *   DisplayBackground desacopla el "qué se hace para preparar el canvas"
 *   del mecanismo de buffer. SurfaceBuilder lo aplica al construir la
 *   superficie inicial; RenderFrame lo aplica en beginVirtual() si se
 *   configura en el pipeline.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * EXTENSIBILIDAD
 *
 * Implementaciones previstas (y cómo se añaden sin tocar Display):
 *
 *   SolidColorBackground   – color sólido configurable (incluida en el módulo)
 *   GradientBackground     – gradiente lineal o radial
 *   TexturedBackground     – imagen de fondo que se repite o escala
 *   NoBackground           – no limpiar (útil cuando el juego garantiza cobertura total)
 *   AnimatedBackground     – fondo animado basado en tiempo
 *
 * Para agregar un modo nuevo: implementar esta interfaz, sin modificar Display.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * USO
 *
 *   // Configurar
 *   DisplayBackground bg = new SolidColorBackground(new Color(20, 20, 30));
 *
 *   // El Display lo guarda y llama automáticamente
 *   displayManager.setBackground(bg);
 *
 * ──────────────────────────────────────────────────────────────────────────
 * THREAD SAFETY
 *
 *   apply() se llama solo desde el GameLoop thread (igual que beginFrame()).
 *   Las implementaciones no necesitan ser thread-safe a menos que su estado
 *   interno sea modificado concurrentemente (e.g. AnimatedBackground con clock
 *   actualizado desde otro thread).
 */
public interface DisplayBackground {

    /**
     * Prepara el canvas para el frame actual.
     *
     * Típicamente: limpiar y rellenar con el color/imagen de fondo.
     * Se llama al inicio de cada frame, antes de que la escena dibuje nada.
     *
     * @param g             Graphics2D del framebuffer virtual
     * @param virtualWidth  ancho virtual en píxeles
     * @param virtualHeight alto virtual en píxeles
     */
    void apply(Graphics2D g, int virtualWidth, int virtualHeight);
}
