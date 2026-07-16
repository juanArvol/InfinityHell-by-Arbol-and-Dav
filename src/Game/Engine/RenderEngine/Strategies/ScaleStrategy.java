package Game.Engine.RenderEngine.Strategies;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;

/**
 * ScaleStrategy — procesador de pipeline que configura la escala geométrica.
 *
 * ── HRFC-003.5: ROL REDEFINIDO ────────────────────────────────────────────
 * ScaleStrategy ya NO dibuja. Solo modifica el AffineTransform del Graphics2D.
 * SpriteDrawer es el único responsable de llamar g.drawImage().
 *
 * ── ESCALA INDEPENDIENTE X/Y ──────────────────────────────────────────────
 * Soporta scaleX != scaleY para:
 *   - Squash and stretch animado
 *   - Sprites aplastados (perspectiva)
 *   - Escala proporcional uniforme
 *
 * ── PIVOT ─────────────────────────────────────────────────────────────────
 * La escala se aplica alrededor del pivot del TransformData.
 * Pivot (0.5, 1.0): crece/encoge desde la base — ideal para personajes.
 *
 * ── INSTANCIA SINGLETON ───────────────────────────────────────────────────
 * Stateless. Una sola instancia compartida.
 */
public final class ScaleStrategy implements RenderStrategy {

    public static final ScaleStrategy INSTANCE = new ScaleStrategy();

    private ScaleStrategy() {}

    /**
     * Aplica la transformación de escala al transform activo del Graphics2D.
     * NO dibuja.
     */
    @Override
    public void apply(Graphics2D g, DrawContext ctx) {
        if (!ctx.transform().hasScale()) return;

        int px = ctx.pivotScreenX();
        int py = ctx.pivotScreenY();

        AffineTransform current = g.getTransform();
        AffineTransform at = new AffineTransform(current);
        at.translate(px, py);
        at.scale(ctx.transform().scaleX, ctx.transform().scaleY);
        at.translate(-px, -py);
        g.setTransform(at);
    }
}
