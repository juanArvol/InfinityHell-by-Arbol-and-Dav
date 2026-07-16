package Game.Engine.RenderEngine.Strategies;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;

/**
 * TintStrategy — superpone un color semitransparente sobre el sprite.
 *
 * ── TÉCNICA ───────────────────────────────────────────────────────────────
 * Dibuja un rectángulo del color de tinte con AlphaComposite.SRC_ATOP.
 * SRC_ATOP pinta el color SOLO sobre los píxeles no transparentes del sprite,
 * preservando el canal alfa original. Resultado: el sprite conserva su forma
 * pero adquiere el color del tinte en proporción a tintAlpha.
 *
 * Funciona correctamente con sprites con transparencia (PNG con alfa).
 *
 * ── CASOS DE USO ──────────────────────────────────────────────────────────
 *   Daño recibido  → tint(Color.RED,    0.4f)
 *   Congelado      → tint(Color.CYAN,   0.5f)
 *   Envenenado     → tint(Color.GREEN,  0.35f)
 *   Eléctrico      → tint(Color.YELLOW, 0.6f)
 *   Invulnerable   → tint(Color.WHITE,  0.3f)
 *
 * TintStrategy se aplica DESPUÉS de que el sprite ya fue dibujado.
 * Por eso SpriteDrawer la llama en la fase post-draw.
 *
 * ── INSTANCIA SINGLETON ───────────────────────────────────────────────────
 * Stateless. Una sola instancia compartida.
 */
public final class TintStrategy implements RenderStrategy {

    public static final TintStrategy INSTANCE = new TintStrategy();

    private TintStrategy() {}

    @Override
    public void apply(Graphics2D g, DrawContext ctx) {
        if (!ctx.transform().hasTint()) return;

        Color  tint      = ctx.transform().tintColor;
        float  tintAlpha = ctx.transform().tintAlpha;

        // Guardar estado
        Composite savedComposite = g.getComposite();
        Color     savedColor     = g.getColor();

        // SRC_ATOP: pinta solo sobre píxeles opacos del destino.
        // El sprite debe haber sido dibujado primero para que funcione.
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, tintAlpha));
        g.setColor(tint);
        g.fillRect(ctx.screenX(), ctx.screenY(), ctx.renderWidth(), ctx.renderHeight());

        // Restaurar
        g.setComposite(savedComposite);
        g.setColor(savedColor);
    }
}
