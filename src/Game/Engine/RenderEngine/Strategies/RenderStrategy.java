package Game.Engine.RenderEngine.Strategies;

import Game.Engine.RenderEngine.Transform.TransformData;
import Sprites.Core.SpriteFrame;
import java.awt.Graphics2D;

/**
 * RenderStrategy — contrato de un procesador del pipeline de render.
 *
 * ── HRFC-003.5: ROL DEFINITIVO ────────────────────────────────────────────
 * Una RenderStrategy es un PROCESADOR DEL PIPELINE, no un dibujante.
 *
 * Existen dos categorías:
 *
 *   PRE-DRAW PROCESSORS (modifican estado antes del drawImage):
 *     AlphaStrategy      → establece AlphaComposite
 *     BlendModeStrategy  → establece Composite personalizado
 *     FlipStrategy       → modifica AffineTransform (flip)
 *     ScaleStrategy      → modifica AffineTransform (escala)
 *     RotationStrategy   → modifica AffineTransform (rotación)
 *     Ninguna de estas llama drawImage(). Solo preparan el estado.
 *
 *   POST-DRAW EFFECTS (dibujan su propio elemento, no el sprite):
 *     TintStrategy  → fillRect con SRC_ATOP (superpone color sobre sprite ya dibujado)
 *     ShadowStrategy → fillOval (dibuja elipse ANTES del sprite, fase pre-shadow)
 *     Estas dibujan geometría propia, nunca la imagen del sprite.
 *
 * ── ÚNICO RESPONSABLE DEL drawImage ─────────────────────────────────────
 * Solo SpriteDrawer llama g.drawImage() para el sprite.
 * Exactamente una vez por sprite en el caso base.
 * Ninguna implementación de RenderStrategy puede llamar drawImage()
 * con la imagen del frame — hacerlo es una violación del contrato.
 *
 * ── CONTRATO DE ESTADO ────────────────────────────────────────────────────
 * PRE-DRAW PROCESSORS: SpriteDrawer guarda el estado (transform, composite)
 * ANTES de invocarlos y lo restaura DESPUÉS del drawImage.
 * Los processors no necesitan hacer save/restore propio.
 *
 * POST-DRAW EFFECTS: gestionan su propio save/restore porque operan
 * después de que SpriteDrawer ya restauró el estado base.
 *
 * ── EXTENSIBILIDAD ────────────────────────────────────────────────────────
 * Para añadir un nuevo efecto (outline, glow, normal map):
 *   1. Implementar RenderStrategy.
 *   2. Si es pre-draw: llamarlo desde SpriteDrawer antes del drawImage.
 *   3. Si es post-draw: llamarlo desde SpriteDrawer después del drawImage.
 *   Sin modificar ninguna clase existente salvo SpriteDrawer.
 */
public interface RenderStrategy {

    /**
     * Aplica el procesamiento de esta estrategia sobre el Graphics2D.
     *
     * PRE-DRAW PROCESSORS: modificar estado (transform, composite) y retornar.
     *   No llamar drawImage() con la imagen del frame.
     *   SpriteDrawer gestiona el save/restore del estado.
     *
     * POST-DRAW EFFECTS: dibujar geometría propia (rect, oval, borde) y retornar.
     *   Gestionar save/restore propio del estado que modifiquen.
     *
     * @param g   Graphics2D del framebuffer — NO disponer
     * @param ctx contexto del draw actual — inmutable
     */
    void apply(Graphics2D g, DrawContext ctx);

    // ── DrawContext ───────────────────────────────────────────────────────────

    /**
     * Contexto inmutable de un draw individual.
     *
     * Contiene todo lo que una estrategia necesita:
     *   - screenX/Y: posición final en pantalla (ya con offsetX/Y del TransformData)
     *   - renderWidth/Height: tamaño final de render
     *   - frame: el SpriteFrame a dibujar (siempre isValid() == true)
     *   - transform: el TransformData completo
     *
     * pivotScreenX/Y: coordenadas absolutas del pivot en pantalla,
     * calculadas a partir de screenX/Y + renderSize * pivotNormalized.
     * Usadas por FlipStrategy, ScaleStrategy y RotationStrategy.
     */
    record DrawContext(
        /** Posición X final en pantalla (incluye offsetX del TransformData). */
        int screenX,
        /** Posición Y final en pantalla (incluye offsetY del TransformData). */
        int screenY,
        /** Ancho de render en píxeles. */
        int renderWidth,
        /** Alto de render en píxeles. */
        int renderHeight,
        /** Frame a dibujar. Siempre isValid() == true al construir. */
        SpriteFrame frame,
        /** TransformData completo. Nunca null. */
        TransformData transform
    ) {
        /**
         * Posición X absoluta del pivot en pantalla.
         * screenX + renderWidth * pivotX (normalizado).
         */
        public int pivotScreenX() {
            return screenX + (int)(renderWidth * transform.pivotX);
        }

        /**
         * Posición Y absoluta del pivot en pantalla.
         */
        public int pivotScreenY() {
            return screenY + (int)(renderHeight * transform.pivotY);
        }
    }
}
