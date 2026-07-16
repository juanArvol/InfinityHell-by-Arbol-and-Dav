package Game.Engine.RenderEngine.Strategies;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;

/**
 * FlipStrategy — procesador de pipeline que configura el flip geométrico.
 *
 * ── HRFC-003.5: ROL REDEFINIDO ────────────────────────────────────────────
 * FlipStrategy ya NO dibuja. Solo modifica el AffineTransform del Graphics2D.
 * SpriteDrawer es el único responsable de llamar g.drawImage().
 *
 * ── CONTRATO ──────────────────────────────────────────────────────────────
 * apply() aplica la transformación de flip al Graphics2D y retorna.
 * El caller (SpriteDrawer) se encarga de:
 *   1. Llamar apply() para configurar el transform.
 *   2. Llamar g.drawImage() exactamente una vez.
 *   3. Restaurar el transform guardado antes de apply().
 *
 * ── IMPLEMENTACIÓN ────────────────────────────────────────────────────────
 * Reflejo geométrico alrededor del pivot usando escala negativa:
 *   translate(pivot) → scale(-1, 1) → translate(-pivot)
 *
 * Este método es equivalente a AffineTransform.getScaleInstance(-1,1)
 * pero centrado en el pivot correcto del sprite.
 *
 * ── INSTANCIA SINGLETON ───────────────────────────────────────────────────
 * Stateless. Una sola instancia compartida.
 */
public final class FlipStrategy implements RenderStrategy {

    public static final FlipStrategy INSTANCE = new FlipStrategy();

    private FlipStrategy() {}

    /**
     * Aplica la transformación de flip al transform activo del Graphics2D.
     * NO dibuja. El caller dibuja después de llamar este método.
     *
     * @param g   Graphics2D cuyo transform se modificará
     * @param ctx contexto del draw — usado solo para leer el pivot y el transform
     */
    @Override
    public void apply(Graphics2D g, DrawContext ctx) {
        if (!ctx.transform().hasFlip()) return;

        int px = ctx.pivotScreenX();
        int py = ctx.pivotScreenY();

        AffineTransform current = g.getTransform();
        AffineTransform at = new AffineTransform(current);
        at.translate(px, py);
        at.scale(
            ctx.transform().flipH ? -1.0 : 1.0,
            ctx.transform().flipV ? -1.0 : 1.0
        );
        at.translate(-px, -py);
        g.setTransform(at);
    }
}
