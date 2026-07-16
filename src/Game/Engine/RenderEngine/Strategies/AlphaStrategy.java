package Game.Engine.RenderEngine.Strategies;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;

/**
 * AlphaStrategy — procesador de pipeline que configura la transparencia.
 *
 * ── HRFC-003.5: ROL REDEFINIDO ────────────────────────────────────────────
 * AlphaStrategy ya NO dibuja. Solo modifica el Composite del Graphics2D.
 * SpriteDrawer es el único responsable de llamar g.drawImage().
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * Establece AlphaComposite.SRC_OVER con el alpha del TransformData.
 * SpriteDrawer llama apply() ANTES de drawImage; el composite queda activo
 * durante el draw y SpriteDrawer lo restaura después.
 *
 * ── CUANDO SE USA ─────────────────────────────────────────────────────────
 * Solo cuando alpha < 1.0f Y blendMode == NORMAL.
 * Si blendMode != NORMAL, BlendModeStrategy gestiona el composite completo
 * (que incluye el alpha del source en su fórmula de lerp).
 * SpriteDrawer nunca activa ambos simultáneamente.
 *
 * ── INSTANCIA SINGLETON ───────────────────────────────────────────────────
 * Stateless. Una sola instancia compartida.
 */
public final class AlphaStrategy implements RenderStrategy {

    public static final AlphaStrategy INSTANCE = new AlphaStrategy();

    private AlphaStrategy() {}

    /**
     * Establece el composite de transparencia en el Graphics2D.
     * NO dibuja. El caller aplica el composite y dibuja, luego lo restaura.
     *
     * Este método solo actúa si alpha < 1.0f. Si alpha == 1.0f es no-op.
     */
    @Override
    public void apply(Graphics2D g, DrawContext ctx) {
        float alpha = ctx.transform().alpha;
        if (alpha >= 1.0f) return;
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
    }
}
