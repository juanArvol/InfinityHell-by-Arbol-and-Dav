package Game.UI.POV;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/**
 * Contexto de render — refactorizado para el pipeline de framebuffer virtual.
 *
 * CAMBIOS:
 *  - Constructor recibe Graphics2D directamente (no Graphics).
 *    El framebuffer virtual siempre produce Graphics2D.
 *  - Añadido getVirtualWidth/Height para que los componentes puedan
 *    consultar el espacio de render sin depender de un singleton.
 *  - Añadido withCamera() para obtener un sub-contexto con translación
 *    de cámara aplicada — evita que cada componente haga la translación.
 *
 * REGLA: los componentes de render reciben RenderContext y dibujan en
 * coordenadas de MUNDO. RenderContext aplica el offset de cámara.
 * Los componentes NO conocen la cámara directamente.
 */
public class RenderContext {

    private final Graphics2D g;
    private final int virtualWidth;
    private final int virtualHeight;

    /**
     * Constructor principal.
     *
     * @param g             Graphics2D del framebuffer virtual (de RenderSurfaceManager)
     * @param virtualWidth  DisplaySettings.virtualWidth
     * @param virtualHeight DisplaySettings.virtualHeight
     */
    public RenderContext(Graphics2D g, int virtualWidth, int virtualHeight) {
        this.g             = g;
        this.virtualWidth  = virtualWidth;
        this.virtualHeight = virtualHeight;
    }

    /**
     * Crea un sub-contexto con la translación de cámara aplicada.
     *
     * FIX A-02: la implementación original mutaba el Graphics2D compartido sin
     * restaurar el transform, acumulando traslaciones si se llamaba más de una
     * vez por frame (fuga de transformación garantizada).
     *
     * Ahora hace push del AffineTransform original antes de transladar, y
     * el RenderContext retornado restaura ese transform al llamar dispose().
     * Uso correcto:
     *
     *   RenderContext world = ctx.withCamera(camera);
     *   try {
     *       world.drawImage(...);
     *   } finally {
     *       world.dispose();   // restaura el transform del padre
     *   }
     *
     * @param camera cámara actual
     * @return nuevo RenderContext con transform de cámara aplicado y restaurable
     */
    public RenderContext withCamera(Camera camera) {
        AffineTransform saved = g.getTransform();
        g.translate(-camera.getX(), -camera.getY());
        // Retorna un nuevo RenderContext que, al hacer dispose(), restaura el
        // transform guardado en lugar de hacer g.dispose() (que descartaría g).
        return new RenderContext(g, virtualWidth, virtualHeight) {
            @Override
            public void dispose() {
                g.setTransform(saved);   // pop del transform — no descarta g
            }
        };
    }

    /**
     * Crea un contexto de cámara aislado (safe para uso en subsistemas).
     * El Graphics2D crea una copia — el transform no afecta al contexto padre.
     * Recordar llamar dispose() en el contexto retornado.
     */
    /**
     * Crea un contexto de cámara aislado (safe para uso en subsistemas).
     * El Graphics2D crea una copia — el transform no afecta al contexto padre.
     * Recordar llamar dispose() en el contexto retornado.
     */
    public RenderContext withCameraIsolated(Camera camera) {
        Graphics2D copy = (Graphics2D) g.create();
        copy.translate(-camera.getX(), -camera.getY());
        return new RenderContext(copy, virtualWidth, virtualHeight) {
            @Override
            public void dispose() {
                copy.dispose();   // este sí es dueño de su copia
            }
        };
    }

    /**
     * Dispone el Graphics2D subyacente.
     *
     * Solo llamar si este contexto fue creado con withCameraIsolated() o
     * con withCamera() — ambos retornan contextos con semántica de dispose
     * apropiada. El contexto raíz (el que construye RenderSurfaceManager)
     * NO debe ser dispuesto aquí; su ciclo de vida lo gestiona el llamador.
     *
     * La implementación base es no-op para proteger el contexto raíz.
     * withCameraIsolated() sobreescribe esto para disponer la copia.
     * withCamera() sobreescribe esto para restaurar el transform guardado.
     */
    public void dispose() {
        // no-op: el contexto raíz no es dueño de su Graphics2D
    }

    // ─── Dibujo ───────────────────────────────────────────────────────────────

    public void drawImage(BufferedImage img, int x, int y, int w, int h) {
        g.drawImage(img, x, y, w, h, null);
    }

    public void drawHitbox(Rectangle r, Color color) {
        g.setColor(color);
        g.drawRect(r.x, r.y, r.width, r.height);
    }

    public void setColor(Color c)                    { g.setColor(c); }
    public void fillRect(int x, int y, int w, int h) { g.fillRect(x, y, w, h); }
    public void drawRect(int x, int y, int w, int h) { g.drawRect(x, y, w, h); }
    public void drawString(String s, int x, int y)   { g.drawString(s, x, y); }

    /**
     * Sombra semi-transparente para objetos 2.5D.
     */
    public void drawShadowEllipse(int x, int y, int w, int h, int alpha) {
        Composite original = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(
            AlphaComposite.SRC_OVER,
            alpha / 255.0f
        ));
        g.setColor(Color.BLACK);
        g.fillOval(x, y, w, h);
        g.setComposite(original);
    }

    // ─── Acceso ───────────────────────────────────────────────────────────────

    /** Acceso directo para operaciones avanzadas no cubiertas por el wrapper. */
    public Graphics2D getGraphics2D() { return g; }

    /** Ancho del espacio virtual de render (constante). */
    public int getVirtualWidth()  { return virtualWidth;  }

    /** Alto del espacio virtual de render (constante). */
    public int getVirtualHeight() { return virtualHeight; }
}
