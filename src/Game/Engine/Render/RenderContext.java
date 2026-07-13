package Game.Engine.Render;

import Game.Engine.Camera.GameCamera;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

/**
 * Contexto de render — infraestructura de render del Engine.
 *
 * Envuelve un Graphics2D del framebuffer virtual y expone operaciones
 * de dibujo tipadas, evitando que los componentes dependan de Graphics2D
 * directamente y permitiendo transformaciones de cámara seguras.
 *
 * REGLA: los componentes de render reciben RenderContext y dibujan en
 * coordenadas de MUNDO. RenderContext aplica la transformación de cámara.
 *
 * ── HRFC-001: soporte para GameCamera con viewTransform ─────────────────
 *
 * withCamera(GameCamera) aplica la transformación completa de la cámara
 * (translación + zoom + rotación) usando getViewTransform(). Esto permite
 * que zoom y rotación funcionen correctamente sin cambiar la firma de los
 * componentes existentes.
 *
 * withCamera(Camera) — adaptador de compatibilidad — sigue disponible para
 * el código de render que usa la Camera adaptador. Solo aplica translación.
 */
public class RenderContext {

    private final Graphics2D g;
    private final int virtualWidth;
    private final int virtualHeight;

    public RenderContext(Graphics2D g, int virtualWidth, int virtualHeight) {
        this.g             = g;
        this.virtualWidth  = virtualWidth;
        this.virtualHeight = virtualHeight;
    }

    // ── withCamera: GameCamera (nuevo flujo, HRFC-001) ────────────────────────

    /**
     * Sub-contexto con la transformación completa de GameCamera aplicada.
     *
     * Aplica getViewTransform() que incluye translación, zoom y rotación.
     * Crear una copia del Graphics2D garantiza que el contexto raíz nunca
     * queda en estado inconsistente si el caller olvida llamar dispose().
     *
     * Uso correcto:
     *   RenderContext world = ctx.withCamera(gameCamera);
     *   try {
     *       world.drawImage(...);
     *   } finally {
     *       world.dispose();
     *   }
     */
    public RenderContext withCamera(GameCamera camera) {
        Graphics2D copy = (Graphics2D) g.create();
        copy.transform(camera.getViewTransform());
        return new RenderContext(copy, virtualWidth, virtualHeight) {
            @Override public void dispose() { copy.dispose(); }
        };
    }

    // ── withCamera: Camera adaptador (compatibilidad) ─────────────────────────

    /**
     * Sub-contexto con translación de Camera adaptador aplicada.
     *
     * Equivalente al comportamiento anterior: solo translación (sin zoom ni
     * rotación). Los componentes que reciben Camera en su render() usan este
     * método implícitamente a través de los sistemas de render.
     *
     * Recordar llamar dispose() en el contexto retornado.
     *
     * @deprecated Preferir {@link #withCamera(GameCamera)} para nueva lógica.
     *             Este overload existe para compatibilidad durante HRFC-001.
     */
    @Deprecated(since = "hrfc-001")
    public RenderContext withCamera(Camera camera) {
        Graphics2D copy = (Graphics2D) g.create();
        copy.translate(-camera.getX(), -camera.getY());
        return new RenderContext(copy, virtualWidth, virtualHeight) {
            @Override public void dispose() { copy.dispose(); }
        };
    }

    /**
     * Sub-contexto de cámara aislado — equivalente a withCamera(Camera).
     *
     * @deprecated Usar {@link #withCamera(GameCamera)} o {@link #withCamera(Camera)}.
     */
    @Deprecated(since = "hrfc-001")
    public RenderContext withCameraIsolated(Camera camera) {
        return withCamera(camera);
    }

    /**
     * Libera el Graphics2D de este contexto.
     * La implementación base es no-op (el contexto raíz no es dueño de su Graphics2D).
     * withCamera() sobreescribe con el comportamiento correcto.
     */
    public void dispose() {
        // no-op: el contexto raíz no gestiona el ciclo de vida de su Graphics2D
    }

    // ── Dibujo ────────────────────────────────────────────────────────────────

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

    /** Sombra semi-transparente para objetos 2.5D. */
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

    /** Acceso directo para operaciones avanzadas no cubiertas por el wrapper. */
    public Graphics2D getGraphics2D() { return g; }

    public int getVirtualWidth()  { return virtualWidth;  }
    public int getVirtualHeight() { return virtualHeight; }
}
