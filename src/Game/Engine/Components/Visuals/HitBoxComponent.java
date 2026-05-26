package Game.Engine.Components.Visuals;

import Game.Engine.Component;
import Game.Engine.Components.Collisions.ColliderComponent;
import Game.Render.Camera;
import Game.Render.DebugRenderable;
import Game.Render.RenderContext;

import java.awt.Color;
import java.awt.Rectangle;

/**
 * Componente visual que dibuja el área de colisión en modo debug.
 *
 * ── Rol clarificado ──────────────────────────────────────────────────────
 * Antes HitBoxComponent definía el tamaño del colisionador Y lo dibujaba.
 * Eso creaba una dependencia confusa: ColliderComponent leía a HitBoxComponent
 * cada frame para saber su tamaño, y HitBoxComponent a veces leía a
 * SpriteRenderer para saber el suyo.
 *
 * Ahora:
 *   ColliderComponent → define el área real de colisión (física)
 *   HitBoxComponent   → solo dibuja esa área en debug mode (visual)
 *
 * HitBoxComponent lee los bounds del ColliderComponent del mismo objeto
 * para dibujarlos. Sin lógica de colisión propia.
 *
 * Si no usás debug mode, no necesitás este componente en producción.
 */
public class HitBoxComponent extends Component implements DebugRenderable {

    private boolean visible   = true;
    private Color debugColor  = Color.RED;

    public HitBoxComponent() {}

    public HitBoxComponent(Color color) {
        this.debugColor = color;
    }

    @Override
    public void debugRender(RenderContext ctx, Camera camera) {
        if (!visible) return;

        ColliderComponent col = gameObject.getComponent(ColliderComponent.class);
        if (col == null) return;

        Rectangle bounds = col.getBounds();
        int x = (int)(bounds.x - camera.getX());
        int y = (int)(bounds.y - camera.getY());

        ctx.drawHitbox(new Rectangle(x, y, bounds.width, bounds.height), debugColor);
    }

    public void setVisible(boolean v)    { this.visible = v; }
    public boolean isVisible()           { return visible; }
    public void setDebugColor(Color c)   { this.debugColor = c; }
    public Color getDebugColor()         { return debugColor; }
}
