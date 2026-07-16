package Game.Engine.RenderEngine.Strategies;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;

/**
 * ShadowStrategy — dibuja una sombra elíptica debajo del sprite.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * Dibuja la sombra ANTES que el sprite principal (pre-draw).
 * SpriteDrawer la llama en la fase shadow, antes de la imagen.
 *
 * ── CONFIGURACIÓN ─────────────────────────────────────────────────────────
 * ShadowStrategy es configurable (no singleton): cada instancia tiene sus
 * propios parámetros de sombra.
 *
 *   new ShadowStrategy(20, 6, 0.4f)   → sombra de 40x12 con 40% opacidad
 *
 * Parámetros:
 *   radiusX  → semieje horizontal de la elipse en píxeles
 *   radiusY  → semieje vertical
 *   opacity  → opacidad [0..1]
 *   offsetX  → desplazamiento horizontal respecto al centro del sprite
 *   offsetY  → desplazamiento vertical (positivo = hacia abajo)
 *
 * ── SOMBRA SIMPLE (2D) VS SOMBRA 3D ──────────────────────────────────────
 * Esta estrategia dibuja una sombra simple 2D bajo el sprite en su posición.
 * Para sombras proyectadas con altura Z (2.5D), usar ShadowComponent que
 * calcula la posición en el suelo (Z=0) a partir del Transform3D.
 * Ambos sistemas son complementarios.
 *
 * ── POSICIÓN DE LA SOMBRA ─────────────────────────────────────────────────
 * La sombra se centra en la parte inferior-centro del sprite por defecto.
 * Se puede desplazar con offsetX/offsetY.
 */
public final class ShadowStrategy implements RenderStrategy {

    /** Sombra estándar recomendada para personajes. */
    public static final ShadowStrategy STANDARD = new ShadowStrategy(16, 5, 0.38f, 0, 2);

    private final int   radiusX;
    private final int   radiusY;
    private final float opacity;
    private final int   offsetX;
    private final int   offsetY;

    /**
     * @param radiusX semi-eje horizontal de la elipse en píxeles
     * @param radiusY semi-eje vertical de la elipse en píxeles
     * @param opacity opacidad [0..1]
     */
    public ShadowStrategy(int radiusX, int radiusY, float opacity) {
        this(radiusX, radiusY, opacity, 0, 2);
    }

    /**
     * @param radiusX semi-eje horizontal
     * @param radiusY semi-eje vertical
     * @param opacity opacidad [0..1]
     * @param offsetX desplazamiento horizontal respecto al centro inferior
     * @param offsetY desplazamiento vertical adicional (positivo = más abajo)
     */
    public ShadowStrategy(int radiusX, int radiusY, float opacity, int offsetX, int offsetY) {
        this.radiusX = Math.max(1, radiusX);
        this.radiusY = Math.max(1, radiusY);
        this.opacity = Math.max(0, Math.min(1, opacity));
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    @Override
    public void apply(Graphics2D g, DrawContext ctx) {
        // Centro inferior del sprite
        int cx = ctx.screenX() + ctx.renderWidth()  / 2 + offsetX;
        int cy = ctx.screenY() + ctx.renderHeight()     + offsetY;

        Composite savedComposite = g.getComposite();
        Color     savedColor     = g.getColor();

        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
        g.setColor(Color.BLACK);
        g.fillOval(cx - radiusX, cy - radiusY, radiusX * 2, radiusY * 2);

        g.setComposite(savedComposite);
        g.setColor(savedColor);
    }
}
