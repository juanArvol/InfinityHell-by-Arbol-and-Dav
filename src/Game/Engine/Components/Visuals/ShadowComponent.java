package Game.Engine.Components.Visuals;

import Game.Engine.Component;
import Game.Engine.GameMath.SpaceLogic.Logic3D.Transform3D;
import Game.UI.POV.Camera;
import Game.UI.POV.RenderContext;
import Graficos.Renderable;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Componente de sombra proyectada para objetos con altura Z.
 *
 * NUEVO SISTEMA 2.5D: objetos elevados (Z > 0) proyectan una sombra
 * elíptica en el suelo (Z=0). La sombra crece y se hace más transparente
 * cuanto más alto está el objeto.
 *
 * Funciona con Transform3D. Si el transform no es Transform3D, no dibuja nada.
 *
 * Uso:
 *   // En EnemyFlying:
 *   addComponent(new ShadowComponent(20, 8)); // radio base de sombra
 *
 * Resultado visual: cuando el volador está en Z=0, la sombra es
 * oscura y pequeña. Cuando está en Z=100, la sombra es grande y muy
 * transparente (como en juegos 3D).
 */
public class ShadowComponent extends Component implements Renderable {

    private final int baseRadiusX;
    private final int baseRadiusY;

    // Perspectiva: cuántos píxeles de offset en Y por unidad de Z
    private static final double PERSPECTIVE = 0.5;

    /**
     * @param baseRadiusX radio horizontal de la sombra en el suelo
     * @param baseRadiusY radio vertical de la sombra en el suelo
     */
    public ShadowComponent(int baseRadiusX, int baseRadiusY) {
        this.baseRadiusX = baseRadiusX;
        this.baseRadiusY = baseRadiusY;
    }

    @Override
    public void render(RenderContext ctx, Camera camera) {
        if (!(gameObject.getTransform() instanceof Transform3D t3d)) {
            return; // sin Z, sin sombra
        }

        double z = t3d.getZ();
        if (z <= 0) return; // en el suelo, sin sombra proyectada

        // Posición de sombra en el suelo (Z=0)
        double shadowWorldX = t3d.getX();
        double shadowWorldY = t3d.getY(); // sin offset de perspectiva = en el suelo

        int screenX = (int)(shadowWorldX - camera.getX());
        int screenY = (int)(shadowWorldY - camera.getY());

        // La sombra crece y se desvanece con la altura
        double heightFactor = Math.min(z / 200.0, 1.0); // normalizado a [0, 1]

        int rx = (int)(baseRadiusX * (1 + heightFactor * 0.5));
        int ry = (int)(baseRadiusY * (1 + heightFactor * 0.3));
        int alpha = (int)(180 * (1.0 - heightFactor * 0.7)); // más alto = más transparente

        ctx.drawShadowEllipse(screenX - rx, screenY - ry, rx * 2, ry * 2, alpha);
    }
}
