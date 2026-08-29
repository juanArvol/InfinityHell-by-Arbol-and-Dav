package Game.Engine.RenderEngine.Strategies;

import Game.Engine.RenderEngine.Transform.TransformData;
import Game.Engine.RenderEngine.Transform.TransformData.BlendMode;
import Sprites.Core.SpriteFrame;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;

/**
 * SpriteDrawer — orquestador del pipeline de render. Única fuente de verdad.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * SpriteDrawer es el ÚNICO lugar que llama g.drawImage() para sprites.
 * No contiene lógica de transformación propia: delega cada responsabilidad
 * en la estrategia correspondiente.
 *
 * ── HRFC-003.6: applyGeometricTransform() ELIMINADO ──────────────────────
 * La implementación inline de flip+scale+rotation fue eliminada.
 * Ahora SpriteDrawer invoca directamente RotationStrategy, ScaleStrategy y
 * FlipStrategy en el orden correcto. Las estrategias son realmente usadas.
 * No existe duplicación de lógica geométrica.
 *
 * ── PIPELINE (orden exacto) ───────────────────────────────────────────────
 *
 *  Gameplay → SpriteHandle → TransformData → SpriteDrawer → Graphics2D → Display
 *
 *  1. GUARD      — validar frame
 *  2. FAST PATH  — si IDENTITY: g.drawImage directo, fin. Cero overhead.
 *  3. PRE-SHADOW — ShadowStrategy.apply() si presente (dibuja elipse)
 *  4. SAVE STATE — guardar AffineTransform y Composite actuales
 *  5. BLEND/ALPHA — BlendModeStrategy o AlphaStrategy configuran Composite
 *  6. GEOMETRY   — RotationStrategy → ScaleStrategy → FlipStrategy
 *                  (cada una modifica g.getTransform(); SpriteDrawer no
 *                   construye ningún AffineTransform propio)
 *  7. DRAW       — g.drawImage() — exactamente UNA vez
 *  8. RESTORE    — restaurar AffineTransform y Composite originales
 *  9. POST-TINT  — TintStrategy.apply() si hasTint()
 *
 * ── ORDEN DE ESTRATEGIAS GEOMÉTRICAS ─────────────────────────────────────
 * Cada estrategia lee g.getTransform() (que ya incluye las anteriores) y
 * añade su operación. Orden de invocación:
 *
 *   RotationStrategy → ScaleStrategy → FlipStrategy
 *
 * Esto produce la composición visual estándar de motores 2D:
 *   flip aplicado primero al espacio local,
 *   luego escala,
 *   luego rotación como la transformación más exterior.
 *
 * ── PATH RÁPIDO ───────────────────────────────────────────────────────────
 * Si TransformData.isIdentity() → g.drawImage directo.
 * Cero allocations, cero strategies, cero transform changes.
 * Es el path más frecuente durante el gameplay actual.
 *
 * ── CONTRATO DE drawImage() ──────────────────────────────────────────────
 * "Un único drawImage() por sprite" aplica exclusivamente a cada invocación
 * de SpriteDrawer.draw(). Por cada llamada, exactamente un g.drawImage() es
 * emitido para el frame (PASO 7).
 *
 * Los modos FillMode.TILE / TILE_X / TILE_Y requieren múltiples drawImage()
 * para llenar el área. Esto no contradice el contrato porque:
 *   - FillModeRenderer gestiona esos draws, no SpriteDrawer.
 *   - SpriteDrawer.draw() nunca es invocado para TILE — ese path va
 *     directamente por SpriteRenderer → FillModeRenderer.
 *   - Cada repetición individual en FillModeRenderer es un drawImage()
 *     sobre el mismo SpriteFrame (no un pipeline completo por celda).
 *
 * Contrato resumido: por cada SpriteDrawer.draw() → exactamente 1 drawImage().
 *
 * ── STATELESS ─────────────────────────────────────────────────────────────
 * Sin estado mutable. INSTANCE es seguro en cualquier contexto de render.
 */
public final class SpriteDrawer {

    public static final SpriteDrawer INSTANCE = new SpriteDrawer();

    private SpriteDrawer() {}

    // ── API principal ─────────────────────────────────────────────────────────

    /**
     * Dibuja un sprite aplicando el TransformData completo.
     *
     * @param g            Graphics2D del framebuffer (no disponer)
     * @param frame        frame a dibujar — debe ser válido
     * @param screenX      X en pantalla (con offset de cámara ya aplicado)
     * @param screenY      Y en pantalla
     * @param renderWidth  ancho de render en píxeles
     * @param renderHeight alto de render en píxeles
     * @param transform    transformaciones a aplicar (nunca null; usar IDENTITY)
     */
    public void draw(Graphics2D g,
                     SpriteFrame frame,
                     int screenX, int screenY,
                     int renderWidth, int renderHeight,
                     TransformData transform) {

        // ── PASO 1: GUARD ────────────────────────────────────────────────────
        if (frame == null || !frame.isValid() || frame.getImage() == null) return;
        if (renderWidth <= 0 || renderHeight <= 0) return;

        // ── PASO 2: FAST PATH ─────────────────────────────────────────────────
        if (transform.isIdentity()) {
            // NEAREST_NEIGHBOR: los sprites son pixel-art y deben escalarse sin
            // suavizado. RenderFrame puede haber configurado BILINEAR globalmente
            // (useInterpolation=true). Se sobreescribe localmente y se restaura
            // para no afectar otros elementos del frame (fondos, UI, etc.).
            Object prevHint = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(frame.getImage(), screenX, screenY, renderWidth, renderHeight, null);
            if (prevHint != null) {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, prevHint);
            }
            return;
        }

        // DrawContext compartido por todas las estrategias del pipeline.
        // offsetX/Y del TransformData incorporado en screenX/Y del contexto.
        // HRFC: Reusing DrawContext between calls would require mutation,
        // but record is immutable by design. The allocation cost is minimal
        // (6 fields on stack/young-gen). The real cost is Graphics2D state management.
        RenderStrategy.DrawContext ctx = new RenderStrategy.DrawContext(
            screenX + transform.offsetX,
            screenY + transform.offsetY,
            renderWidth, renderHeight,
            frame, transform
        );

        // ── PASO 4: SAVE STATE (only if we'll modify it) ──────────────────
        // HRFC Performance: Only save Graphics2D state if we're actually going
        // to modify it. This avoids AffineTransform.clone() cost when possible.
        boolean willModifyComposite  = (transform.blendMode != BlendMode.NORMAL || transform.alpha < 1.0f);
        boolean willModifyTransform  = transform.hasGeometricTransform();
        
        AffineTransform savedTransform = willModifyTransform ? g.getTransform() : null;
        Composite       savedComposite = willModifyComposite ? g.getComposite() : null;

        // ── PASO 5: BLEND / ALPHA ─────────────────────────────────────────────
        if (transform.blendMode != BlendMode.NORMAL) {
            BlendModeStrategy.INSTANCE.apply(g, ctx);
        } else if (transform.alpha < 1.0f) {
            AlphaStrategy.INSTANCE.apply(g, ctx);
        }

        // ── PASO 6: GEOMETRY ─────────────────────────────────────────────────
        // Delegado completamente en las RenderStrategy.
        // SpriteDrawer no construye ningún AffineTransform propio.
        // Orden: Rotation → Scale → Flip.
        if (transform.hasGeometricTransform()) {
            if (transform.hasRotation()) RotationStrategy.INSTANCE.apply(g, ctx);
            if (transform.hasScale())    ScaleStrategy.INSTANCE.apply(g, ctx);
            if (transform.hasFlip())     FlipStrategy.INSTANCE.apply(g, ctx);
        }

        // ── PASO 7: DRAW — único drawImage del pipeline ───────────────────────
        // NEAREST_NEIGHBOR se fuerza aquí independientemente del hint global.
        //
        // Razón: RenderFrame configura el Graphics2D del framebuffer con
        // BILINEAR cuando useInterpolation=true. Ese hint afecta el escalado
        // del sprite al tamaño de render. Para pixel-art NEAREST_NEIGHBOR
        // produce el resultado correcto (sin suavizado).
        //
        // Con la extracción vía Raster.setRect() el buffer del frame ya es
        // pixel-perfect — no hay artefactos en la extracción. Este hint opera
        // únicamente sobre el escalado final (frame nativo → tamaño de render).
        Object prevHint = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(frame.getImage(),
            ctx.screenX(), ctx.screenY(),
            ctx.renderWidth(), ctx.renderHeight(),
            null);
        if (prevHint != null) {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, prevHint);
        }

        // ── PASO 8: RESTORE STATE (only if we modified it) ────────────────────
        if (savedTransform != null) g.setTransform(savedTransform);
        if (savedComposite != null) g.setComposite(savedComposite);

        // ── PASO 9: POST-TINT ─────────────────────────────────────────────────
        if (transform.hasTint()) {
            TintStrategy.INSTANCE.apply(g, ctx);
        }
    }

    /**
     * Variante con ShadowStrategy opcional (PASO 3 — pre-shadow).
     *
     * @param shadowStrategy estrategia de sombra, o null para omitirla
     */
    public void draw(Graphics2D g,
                     SpriteFrame frame,
                     int screenX, int screenY,
                     int renderWidth, int renderHeight,
                     TransformData transform,
                     ShadowStrategy shadowStrategy) {

        // ── PASO 3: PRE-SHADOW ────────────────────────────────────────────────
        if (shadowStrategy != null && frame != null && frame.isValid()) {
            RenderStrategy.DrawContext shadowCtx = new RenderStrategy.DrawContext(
                screenX + transform.offsetX,
                screenY + transform.offsetY,
                renderWidth, renderHeight,
                frame, transform
            );
            shadowStrategy.apply(g, shadowCtx);
        }

        draw(g, frame, screenX, screenY, renderWidth, renderHeight, transform);
    }
}
