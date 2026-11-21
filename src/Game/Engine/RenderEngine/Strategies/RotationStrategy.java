package Game.Engine.RenderEngine.Strategies;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;

/**
 * RotationStrategy — procesador de pipeline que configura la rotación geométrica.
 *
 * ── HRFC-003.5: ROL REDEFINIDO ────────────────────────────────────────────
 * RotationStrategy ya NO dibuja. Solo modifica el AffineTransform del Graphics2D.
 * SpriteDrawer es el único responsable de llamar g.drawImage().
 *
 * ── CONVENCIÓN ────────────────────────────────────────────────────────────
 * Ángulo en radianes. Positivo = sentido horario (convención AWT/Java2D).
 *
 * ── PIVOT ─────────────────────────────────────────────────────────────────
 * La rotación ocurre alrededor del pivot del TransformData.
 * Pivot (0.5, 1.0): rota desde la base — útil para espadas, brazos, puertas.
 *
 * ── INSTANCIA SINGLETON ───────────────────────────────────────────────────
 * Stateless. Una sola instancia compartida.
 */
public final class RotationStrategy implements RenderStrategy {

    public static final RotationStrategy INSTANCE = new RotationStrategy();

    private RotationStrategy() {}

    /**
     * Aplica la transformación de rotación al transform activo del Graphics2D.
     * NO dibuja.
     * 
     * ── PIXEL-PERFECT FIX: Integer pivot coordinates ──────────────────────
     * El pivot se redondea explícitamente para evitar sub-pixel positioning
     * que causa artefactos de rendering en pixel-art.
     */
    @Override
    public void apply(Graphics2D g, DrawContext ctx) {
        if (!ctx.transform().hasRotation()) return;

        // PIXEL-PERFECT: Redondear pivot a coordenadas enteras
        // Sub-pixel pivots causan interpolación incluso con NEAREST_NEIGHBOR
        int px = Math.round(ctx.pivotScreenX());
        int py = Math.round(ctx.pivotScreenY());

        AffineTransform current = g.getTransform();
        AffineTransform at = new AffineTransform(current);
        at.rotate(ctx.transform().rotation, px, py);
        g.setTransform(at);
    }
}
