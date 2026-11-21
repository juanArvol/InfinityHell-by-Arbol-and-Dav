package Game.Engine.RenderEngine.Strategies;

import java.awt.Graphics2D;

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
     * ── PIXEL-PERFECT FIX v6.0: Eliminación de scale(-1) ──────────────────
     * PROBLEMA RAÍZ: scale(-1, 1) en AffineTransform + drawImage() causa
     * errores de redondeo de 1 pixel durante el rasterizado en pixel-art.
     *
     * SOLUCIÓN: NO usar scale() en absoluto. En su lugar, modificar
     * directamente las coordenadas de drawImage() para lograr el flip:
     *
     * Flip horizontal:
     *   - Sin flip:  drawImage(img, x, y, w, h)
     *   - Con flip:  drawImage(img, x+w, y, -w, h)
     *
     * Java interpreta ancho/alto negativos como flip automático, pero
     * renderiza con coordenadas exactas de píxeles enteros, sin la capa
     * de AffineTransform que introduce errores de redondeo en doubles.
     *
     * NOTA: Esta estrategia NO modifica el transform. El caller (SpriteDrawer)
     * debe detectar flip y ajustar los parámetros de drawImage() directamente.
     * Esta función se mantiene por compatibilidad pero no hace nada.
     *
     * @param g   Graphics2D (sin modificar)
     * @param ctx contexto del draw
     */
    @Override
    public void apply(Graphics2D g, DrawContext ctx) {
        // ── NUEVO APPROACH: NO modificar el transform ─────────────────────
        // El flip se maneja en SpriteDrawer usando width/height negativos
        // en drawImage(), que es pixel-perfect sin errores de redondeo.
        // Esta función está vacía intencionalmente.
    }
}
