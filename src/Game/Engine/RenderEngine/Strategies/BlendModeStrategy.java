package Game.Engine.RenderEngine.Strategies;

import Game.Engine.RenderEngine.Transform.TransformData.BlendMode;
import java.awt.Composite;
import java.awt.Graphics2D;

/**
 * BlendModeStrategy — procesador de pipeline que configura el blend mode.
 *
 * ── HRFC-003.5: ROL COMPLETAMENTE REDEFINIDO ──────────────────────────────
 * BlendModeStrategy ya NO dibuja. Solo establece el Composite correcto
 * en el Graphics2D ANTES de que SpriteDrawer llame g.drawImage().
 * SpriteDrawer restaura el composite original después del draw.
 *
 * ── BLEND MODES MATEMÁTICAMENTE CORRECTOS ─────────────────────────────────
 * Cada modo delega en BlendComposites, que implementa la operación
 * pixel-level exacta, idéntica a Photoshop/Krita/Aseprite/GIMP:
 *
 *   NORMAL    → AlphaComposite.SRC_OVER (nativo Java2D — máximo rendimiento)
 *   ADDITIVE  → Dst.RGB + Src.RGB * Src.A   (Linear Dodge)
 *   MULTIPLY  → Dst.RGB * Src.RGB            (Multiply)
 *   SCREEN    → 1-(1-Dst)*(1-Src)            (Screen)
 *   OVERLAY   → 2*Dst*Src  /  1-2*(1-D)*(1-S) según canal  (Overlay)
 *
 * Ver BlendComposites.java para las fórmulas completas con alpha.
 *
 * ── CONTRATO CON SPRITEDRAWER ─────────────────────────────────────────────
 * apply() establece el composite y retorna inmediatamente.
 * SpriteDrawer llama apply(), luego drawImage() una sola vez, luego restaura.
 * BlendModeStrategy nunca llama drawImage().
 *
 * ── NORMAL ES NO-OP ───────────────────────────────────────────────────────
 * Para NORMAL, SpriteDrawer no llama esta estrategia (optimización).
 * apply() con NORMAL es no-op por seguridad defensiva.
 *
 * ── INSTANCIA SINGLETON ───────────────────────────────────────────────────
 * Stateless. Una sola instancia compartida.
 */
public final class BlendModeStrategy implements RenderStrategy {

    public static final BlendModeStrategy INSTANCE = new BlendModeStrategy();

    private BlendModeStrategy() {}

    /**
     * Establece el Composite correcto para el BlendMode del TransformData.
     * NO dibuja. El caller dibuja después con el composite activo, luego restaura.
     *
     * @param g   Graphics2D cuyo Composite se modificará
     * @param ctx contexto del draw — se lee blendMode y alpha
     */
    @Override
    public void apply(Graphics2D g, DrawContext ctx) {
        BlendMode mode = ctx.transform().blendMode;
        if (mode == BlendMode.NORMAL) return; // no-op defensivo

        Composite blendComposite = BlendComposites.get(mode, ctx.transform().alpha);
        g.setComposite(blendComposite);
    }
}
