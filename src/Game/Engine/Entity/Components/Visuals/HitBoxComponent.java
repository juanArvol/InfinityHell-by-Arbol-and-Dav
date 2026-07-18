package Game.Engine.Entity.Components.Visuals;

import Game.Engine.Component;
import Game.Engine.Entity.Components.Collisions.ColliderComponent;
import Game.Engine.RenderEngine.Context.RenderCamera;
import Game.Engine.RenderEngine.Context.RenderContext;
import Game.Engine.RenderEngine.Contracts.DebugRenderable;
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
 *
 * ── CORRECCIÓN: camera offset aplicado ──────────────────────────────────
 * Problema anterior:
 *   debugRender() obtenía los bounds del ColliderComponent (coordenadas de
 *   mundo absolutas) y los pasaba directamente a ctx.drawHitbox() sin
 *   aplicar el offset de cámara. La hitbox aparecía desplazada respecto
 *   al sprite, que sí aplica el offset vía SpriteRenderer.render().
 *
 * Solución:
 *   debugRender(RenderContext, Camera) recibe la cámara y aplica el offset
 *   antes de dibujar. La firma de DebugRenderable se actualiza para aceptar
 *   Camera como segundo parámetro, igual que Renderable.render(ctx, camera).
 *   Esto mantiene consistencia entre los dos contratos visuales del Engine.
 */
public class HitBoxComponent extends Component implements DebugRenderable {

    private boolean visible  = true;
    private Color debugColor = Color.RED;

    public HitBoxComponent() {}

    public HitBoxComponent(Color color) {
        this.debugColor = color;
    }

    @Override
    public void debugRender(RenderContext ctx, RenderCamera camera) {
        if (!visible) return;

        ColliderComponent col = gameObject.getComponent(ColliderComponent.class);
        if (col == null) return;

        Rectangle bounds = col.getBounds();

        // Aplicar camera offset para que la hitbox coincida con el sprite en pantalla.
        int x = (int)(bounds.x - camera.getX());
        int y = (int)(bounds.y - camera.getY());

        ctx.drawHitbox(new Rectangle(x, y, bounds.width, bounds.height), debugColor);
    }

    public void setVisible(boolean v)    { this.visible = v; }
    public boolean isVisible()           { return visible; }
    public void setDebugColor(Color c)   { this.debugColor = c; }
    public Color getDebugColor()         { return debugColor; }
}
