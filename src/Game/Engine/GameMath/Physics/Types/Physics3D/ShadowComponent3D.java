package Game.Engine.GameMath.Physics.Types.Physics3D;

import Game.Engine.Component;
import Game.Engine.Entity.Components.Physics3DComponent;
import Game.Engine.RenderEngine.Context.RenderCamera;
import Game.Engine.RenderEngine.Context.RenderContext;
import Game.Engine.RenderEngine.Contracts.Renderable;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;

/**
 * Sombra dinámica que refleja la altura Z del objeto.
 *
 * MEJORA vs. ShadowComponent original:
 *
 * 1. SI el objeto tiene Physics3DComponent, la sombra se escala y desvanece
 *    según la altura Z: más alto → sombra más pequeña y transparente.
 *    Efecto visual idéntico a Project Zomboid y otros juegos top-down 2.5D.
 *
 * 2. SI no tiene Physics3DComponent (objetos 2D normales), se comporta
 *    exactamente como el ShadowComponent original — retro-compatible.
 *
 * 3. La sombra siempre se dibuja en Z=0 (en el suelo), independientemente
 *    de dónde esté el objeto.
 *
 * Parámetros:
 *   new ShadowComponent3D(30, 8)  → elipse de 30x8 en el suelo
 *
 * La sombra se dibuja ANTES que el sprite (debe estar antes en los componentes
 * o en una capa separada de render). En la práctica, con DepthSortedRenderSystem
 * el orden de componentes dentro del objeto se respeta tal como se añadieron.
 */
public class ShadowComponent3D extends Component implements Renderable {

    private final int baseWidth;
    private final int baseHeight;

    /** Opacidad base (sin altura). 0.0 = invisible, 1.0 = opaco. */
    private final float baseAlpha;

    public ShadowComponent3D(int width, int height) {
        this(width, height, 0.45f);
    }

    public ShadowComponent3D(int width, int height, float baseAlpha) {
        this.baseWidth  = width;
        this.baseHeight = height;
        this.baseAlpha  = baseAlpha;
    }

    @Override
    public void render(RenderContext ctx, RenderCamera camera) {
        var pos = gameObject.getTransform().getPosition();

        // Posición base de la sombra (en el suelo, bajo el objeto)
        double worldX = pos.getX();
        double worldY = pos.getY();   // Y del suelo — no ajustamos por Z aquí

        // Factor de escala según la altura Z (1.0 en suelo, 0 a gran altura)
        float scale = 1.0f;
        float alpha = baseAlpha;

        Physics3DComponent p3d = gameObject.getComponent(Physics3DComponent.class);
        if (p3d != null && p3d.getZ() > 0) {
            // Reducir sombra proporcionalmente a la altura
            // A Z=200 la sombra desaparece completamente (ajustable)
            float heightFactor = (float) Math.max(0, 1.0 - p3d.getZ() / 200.0);
            scale = 0.3f + 0.7f * heightFactor;  // mínimo 30% del tamaño base
            alpha = baseAlpha * heightFactor;
        }

        double screenX = worldX - camera.getX();
        double screenY = worldY - camera.getY();

        // Centrar la elipse bajo el objeto
        double ellipseW = baseWidth  * scale;
        double ellipseH = baseHeight * scale;
        double ex = screenX - ellipseW / 2.0;
        double ey = screenY - ellipseH / 2.0;

        Graphics2D g = ctx.getGraphics2D();

        // Guardar estado completo del graphics antes de modificarlo.
        // Es imprescindible restaurar: composite, color Y el hint de antialiasing,
        // porque este componente accede directamente al Graphics2D base del contexto.
        // Un estado no restaurado contamina todos los Renderable posteriores del frame.
        Composite originalComposite = g.getComposite();
        Color     originalColor     = g.getColor();
        Object    originalHint      = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g.setColor(Color.BLACK);
        g.fill(new Ellipse2D.Double(ex, ey, ellipseW, ellipseH));

        // Restaurar estado completo.
        // Si originalHint es null, se elimina el hint explícitamente para no dejar
        // un antialias forzado activo en el contexto base entre renderizables.
        g.setComposite(originalComposite);
        g.setColor(originalColor);
        if (originalHint != null) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, originalHint);
        } else {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                               RenderingHints.VALUE_ANTIALIAS_DEFAULT);
        }
    }
}
