package Display.Surface;

/**
 * Contrato entre el GameLoop y el subsistema gráfico.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * PROPÓSITO
 *
 * El GameLoop conoce únicamente esta interfaz. No conoce BufferStrategy,
 * Canvas, DisplayManager, RenderSurfaceManager, resize, fullscreen,
 * ni ningún detalle del ciclo de vida gráfico.
 *
 * La implementación concreta (SurfacePublisher) está del lado del EDT
 * y gestiona el swap atómico de superficies. El GameLoop nunca ve
 * ese mecanismo: solo adquiere frames o recibe null.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * USO
 *
 *   RenderFrame frame = gateway.acquireFrame();
 *   if (frame == null) return;   // sin superficie: drop silencioso del frame
 *   try {
 *       // render...
 *   } finally {
 *       gateway.releaseFrame(frame);
 *   }
 *
 * ──────────────────────────────────────────────────────────────────────────
 * GARANTÍAS DEL CONTRATO
 *
 * - Un frame adquirido permanece válido hasta que se llama releaseFrame().
 * - La superficie subyacente no puede ser dispuesta mientras el frame existe.
 * - releaseFrame(null) es un no-op seguro.
 * - acquireFrame() nunca lanza excepciones: retorna null si no hay superficie.
 */
public interface RenderGateway {

    /**
     * Adquiere un frame para el ciclo de render actual.
     *
     * Retorna un {@link RenderFrame} listo para usar, o null si no hay
     * superficie publicada todavía (durante inicialización, transición
     * de pantalla completa, etc.).
     *
     * GameLoop thread únicamente.
     */
    RenderFrame acquireFrame();

    /**
     * Libera el frame y permite que la superficie subyacente sea descartada
     * si el EDT ya la reemplazó.
     *
     * DEBE llamarse siempre, idealmente en un bloque finally.
     * Seguro llamar con null (no-op).
     *
     * GameLoop thread únicamente.
     */
    void releaseFrame(RenderFrame frame);
}
