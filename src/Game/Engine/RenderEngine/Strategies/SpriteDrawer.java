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
 * ── PIXEL-PERFECT v9.0: REFACTORIZACIÓN COMPLETA ──────────────────────────
 * PROBLEMA RAÍZ IDENTIFICADO:
 *   El pipeline anterior tenía múltiples fuentes de error acumulativas:
 *   
 *   1. ViewportCalculator usaba Math.round() → viewport incorrecto
 *   2. RenderFrame usaba STROKE_NORMALIZE en algunas partes → coordenadas ajustadas
 *   3. SpriteRendererComponent usaba Math.round() → error de redondeo
 *   4. SpriteDrawer usaba STROKE_NORMALIZE → más ajustes de coordenadas
 *   5. FlipH con AffineTransform causaba errores de sub-pixel
 *   6. Logs de debug contaminaban el rendimiento
 *
 * SOLUCIÓN DEFINITIVA:
 *   - ViewportCalculator: truncamiento (int cast) SIEMPRE
 *   - RenderFrame: STROKE_PURE en TODOS los hints
 *   - SpriteRendererComponent: cast directo (sin Math.round)
 *   - SpriteDrawer: STROKE_PURE + flip con dimensiones negativas
 *   - Eliminación de pre-flip en memoria (innecesario)
 *   - Eliminación de logs de debug en hot path
 *
 * ── PIPELINE (orden exacto) ───────────────────────────────────────────────
 *
 *  Gameplay → SpriteHandle → TransformData → SpriteDrawer → Graphics2D → Display
 *
 *  1. GUARD      — validar frame y dimensiones
 *  2. FAST PATH  — si IDENTITY o solo flipH: drawImage directo con hints correctos
 *  3. PRE-SHADOW — ShadowStrategy.apply() si presente
 *  4. SAVE STATE — guardar AffineTransform y Composite (solo si necesario)
 *  5. BLEND/ALPHA — BlendModeStrategy o AlphaStrategy
 *  6. GEOMETRY   — RotationStrategy → ScaleStrategy (flip manejado en drawImage)
 *  7. DRAW       — g.drawImage() con hints pixel-perfect — exactamente UNA vez
 *  8. RESTORE    — restaurar estado guardado
 *  9. POST-TINT  — TintStrategy.apply() si hasTint()
 *
 * ── GARANTÍAS PIXEL-PERFECT ───────────────────────────────────────────────
 * 
 * Para que un sprite se renderice pixel-perfect DEBE cumplir:
 * 
 *   1. Coordenadas enteras: screenX, screenY deben ser int sin decimales
 *   2. Tamaño 1:1: renderWidth == frame.getWidth() && renderHeight == frame.getHeight()
 *   3. Hints correctos: NEAREST_NEIGHBOR + STROKE_PURE + ANTIALIAS_OFF
 *   4. Sin transform: o AffineTransform identity, o flip con dimensiones negativas
 *
 * Si alguna de estas condiciones no se cumple, puede haber interpolación.
 *
 * ── CONTRATO DE drawImage() ──────────────────────────────────────────────
 * Por cada SpriteDrawer.draw() → exactamente 1 drawImage().
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
     * ── PIXEL-PERFECT v9.0: GARANTÍAS ─────────────────────────────────────
     * Para renderizado pixel-perfect sin deformaciones:
     *   1. screenX, screenY deben ser coordenadas enteras exactas
     *   2. renderWidth == frame.getWidth() && renderHeight == frame.getHeight()
     *   3. transform sin escala, rotación ni blend (o solo flipH/flipV)
     *
     * Si hay escala no-entera o rotación, Java DEBE interpolar píxeles.
     * Esto no es un bug, es matemáticamente necesario.
     *
     * @param g            Graphics2D del framebuffer (no disponer)
     * @param frame        frame a dibujar — debe ser válido
     * @param screenX      X en pantalla (con offset de cámara ya aplicado) — DEBE ser int exacto
     * @param screenY      Y en pantalla — DEBE ser int exacto
     * @param renderWidth  ancho de render en píxeles — para pixel-perfect DEBE == frame.getWidth()
     * @param renderHeight alto de render en píxeles — para pixel-perfect DEBE == frame.getHeight()
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

        // ── PASO 2: FAST PATH COMPLETO ────────────────────────────────────────
        // Casos donde podemos hacer drawImage directo sin AffineTransform:
        //   - IDENTITY: sin ninguna transformación
        //   - Solo flipH: flip horizontal con width negativo
        //   - Solo flipV: flip vertical con height negativo
        //   - flipH + flipV: ambos flips con dimensiones negativas
        //
        // ⭐ PIXEL-PERFECT v9.0: El flip con dimensiones negativas es LA forma
        // correcta de hacer flip en Java2D. NO requiere AffineTransform y es
        // completamente pixel-perfect sin errores de redondeo.
        
        boolean canUseFastPath = (transform.scaleX == 1.0f && transform.scaleY == 1.0f
                                  && transform.rotation == 0.0f
                                  && transform.alpha == 1.0f
                                  && transform.blendMode == BlendMode.NORMAL
                                  && transform.tintColor == null);
        
        if (canUseFastPath) {
            // Configurar hints pixel-perfect (lightweight — solo lectura/escritura)
            applyPixelPerfectHints(g);
            
            // Calcular posición y dimensiones con flip aplicado
            int drawX = screenX + transform.offsetX;
            int drawY = screenY + transform.offsetY;
            int drawW = renderWidth;
            int drawH = renderHeight;
            
            if (transform.flipH) {
                drawX += renderWidth;  // Mover al borde derecho
                drawW = -renderWidth;  // Ancho negativo = flip horizontal
            }
            
            if (transform.flipV) {
                drawY += renderHeight; // Mover al borde inferior
                drawH = -renderHeight; // Alto negativo = flip vertical
            }
            
            // ⭐ ÚNICO drawImage del fast path
            g.drawImage(frame.getImage(), drawX, drawY, drawW, drawH, null);
            
            return; // Fast path completo — sin restore necesario
        }

        // ── PASO 3 y siguientes: SLOW PATH (transformaciones complejas) ───────

        // DrawContext compartido por todas las estrategias del pipeline.
        // offsetX/Y del TransformData incorporado en screenX/Y del contexto.
        int finalScreenX = screenX + transform.offsetX;
        int finalScreenY = screenY + transform.offsetY;
        
        RenderStrategy.DrawContext ctx = new RenderStrategy.DrawContext(
            finalScreenX,
            finalScreenY,
            renderWidth, renderHeight,
            frame, transform
        );

        // ── PASO 4: SAVE STATE (solo si modificaremos el contexto) ────────────
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

        // ── PASO 6: GEOMETRY (sin FlipStrategy — manejado en drawImage) ───────
        // ⭐ PIXEL-PERFECT v9.0: FlipStrategy no se usa más.
        // El flip se maneja con dimensiones negativas en drawImage (ver abajo).
        if (transform.hasGeometricTransform()) {
            if (transform.hasRotation()) RotationStrategy.INSTANCE.apply(g, ctx);
            if (transform.hasScale())    ScaleStrategy.INSTANCE.apply(g, ctx);
        }

        // ── PASO 7: DRAW — único drawImage del pipeline ───────────────────────
        applyPixelPerfectHints(g);
        
        // ⭐ PIXEL-PERFECT v9.0: Flip con dimensiones negativas (sin AffineTransform)
        int drawX = ctx.screenX();
        int drawY = ctx.screenY();
        int drawW = ctx.renderWidth();
        int drawH = ctx.renderHeight();
        
        // Flip se maneja con width/height negativos (no requiere AffineTransform)
        if (transform.flipH) {
            drawX += drawW;
            drawW = -drawW;
        }
        
        if (transform.flipV) {
            drawY += drawH;
            drawH = -drawH;
        }
        
        g.drawImage(frame.getImage(), drawX, drawY, drawW, drawH, null);

        // ── PASO 8: RESTORE STATE ─────────────────────────────────────────────
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
    
    // ── Helpers privados ──────────────────────────────────────────────────────
    
    /**
     * Configura los RenderingHints para rendering pixel-perfect.
     * 
     * ⭐ PIXEL-PERFECT v9.0: Este es el conjunto MÍNIMO y SUFICIENTE de hints
     * para garantizar que drawImage() NO interpole píxeles:
     * 
     *   - INTERPOLATION_NEAREST_NEIGHBOR: cada píxel source mapea a 1 píxel dest
     *   - ANTIALIAS_OFF: sin suavizado de bordes
     *   - RENDER_SPEED: priorizar velocidad sobre calidad (desactiva optimizaciones que interpolan)
     *   - STROKE_PURE: coordenadas exactas sin ajustes a la grid
     *   - ALPHA_INTERPOLATION_SPEED: sin smooth alpha blending
     * 
     * NO guardamos ni restauramos hints. El Graphics2D del framebuffer ya tiene
     * estos hints configurados por RenderFrame.applyRenderHints(). Esta llamada
     * es defensiva para garantizar que estén activos incluso si algo los cambió.
     */
    private void applyPixelPerfectHints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,
            RenderingHints.VALUE_RENDER_SPEED);
        g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
            RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
            RenderingHints.VALUE_STROKE_PURE);
    }
}
