package Game.Engine.RenderEngine.Strategies;

import Game.Engine.RenderEngine.Transform.TransformData;
import java.awt.Composite;
import java.awt.CompositeContext;
import java.awt.RenderingHints;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

/**
 * BlendComposites — implementaciones matemáticamente correctas de Blend Modes.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * Provee implementaciones de java.awt.Composite que producen exactamente el
 * mismo resultado que software profesional (Photoshop, Krita, Aseprite, GIMP).
 *
 * Java2D no tiene soporte nativo para ADDITIVE, MULTIPLY, SCREEN, OVERLAY.
 * Se implementan operando directamente sobre los canales ARGB del Raster
 * con la fórmula matemática exacta de cada modo.
 *
 * ── HRFC-003.6: OPTIMIZACIÓN DE ALLOCATIONS ──────────────────────────────
 * BlendContext almacena los buffers de píxel como campos de instancia en
 * lugar de crearlos dentro de compose(). El contexto es creado una vez por
 * llamada a createContext() (una por drawImage) y reutilizado en cada tile.
 * Resultado: cero allocations de int[] durante compose(), independientemente
 * del número de tiles que AWT genere para el sprite.
 *
 * ── CONVENCIÓN MATEMÁTICA ─────────────────────────────────────────────────
 * Canales normalizados [0.0, 1.0]:
 *   Src = píxel fuente (sprite a dibujar)
 *   Dst = píxel destino (framebuffer)
 *
 *   Out.RGB = lerp(Dst.RGB, blendOp(Src.RGB, Dst.RGB), Src.A * opacity)
 *   Out.A   = Dst.A + Src.A * (1 - Dst.A)   [alpha compositing SRC_OVER]
 *
 * ── ACCESO ────────────────────────────────────────────────────────────────
 * BlendComposites.get(BlendMode, opacity) → instancia para el drawImage.
 * opacity == 1.0f → instancia cacheada estática (zero allocation).
 * opacity < 1.0f  → nueva instancia con el valor exacto.
 */
final class BlendComposites {

    // ── Instancias cacheadas para opacity = 1.0 (zero allocation) ────────────

    private static final BlendComposite ADDITIVE_FULL =
        new BlendComposite(BlendOp.ADDITIVE, 1.0f);
    private static final BlendComposite MULTIPLY_FULL =
        new BlendComposite(BlendOp.MULTIPLY, 1.0f);
    private static final BlendComposite SCREEN_FULL =
        new BlendComposite(BlendOp.SCREEN,   1.0f);
    private static final BlendComposite OVERLAY_FULL =
        new BlendComposite(BlendOp.OVERLAY,  1.0f);

    private BlendComposites() {}

    /**
     * Obtiene un Composite para el BlendMode y opacity dados.
     * Para opacity == 1.0f retorna instancias cacheadas (zero allocation).
     */
    static Composite get(TransformData.BlendMode mode, float opacity) {
        if (opacity >= 1.0f) {
            return switch (mode) {
                case ADDITIVE -> ADDITIVE_FULL;
                case MULTIPLY -> MULTIPLY_FULL;
                case SCREEN   -> SCREEN_FULL;
                case OVERLAY  -> OVERLAY_FULL;
                default       -> throw new IllegalArgumentException(
                    "BlendComposites: NORMAL no requiere composite personalizado");
            };
        }
        BlendOp op = switch (mode) {
            case ADDITIVE -> BlendOp.ADDITIVE;
            case MULTIPLY -> BlendOp.MULTIPLY;
            case SCREEN   -> BlendOp.SCREEN;
            case OVERLAY  -> BlendOp.OVERLAY;
            default       -> throw new IllegalArgumentException(
                "BlendComposites: modo no soportado: " + mode);
        };
        return new BlendComposite(op, Math.max(0f, Math.min(1f, opacity)));
    }

    // ── Operaciones de blend pixel-level ─────────────────────────────────────

    enum BlendOp {

        /**
         * ADDITIVE (Linear Dodge) — min(1, Dst + Src)
         * Acumula brillo. Uso: fuego, luz, electricidad, magia.
         * Equivalente a Photoshop "Linear Dodge (Add)".
         */
        ADDITIVE {
            @Override float blend(float src, float dst) { return Math.min(1.0f, dst + src); }
        },

        /**
         * MULTIPLY — Dst * Src
         * Oscurece. Blanco neutro. Uso: sombras, veneno, oscurecimiento.
         * Equivalente a Photoshop/Krita/GIMP/Aseprite "Multiply".
         */
        MULTIPLY {
            @Override float blend(float src, float dst) { return src * dst; }
        },

        /**
         * SCREEN — 1 - (1-Dst) * (1-Src)
         * Aclara. Negro neutro. Uso: glow, bloom suave, halos.
         * Equivalente a Photoshop/Krita/GIMP/Aseprite "Screen".
         */
        SCREEN {
            @Override float blend(float src, float dst) {
                return 1.0f - (1.0f - dst) * (1.0f - src);
            }
        },

        /**
         * OVERLAY — Multiply en sombras, Screen en luces.
         *   Dst <= 0.5 : 2 * Dst * Src
         *   Dst >  0.5 : 1 - 2*(1-Dst)*(1-Src)
         * Aumenta contraste. 50% gris neutro.
         * Uso: daño dramático, congelación, impacto.
         * Equivalente a Photoshop/Krita/GIMP/Aseprite "Overlay".
         */
        OVERLAY {
            @Override float blend(float src, float dst) {
                return dst <= 0.5f
                    ? 2.0f * dst * src
                    : 1.0f - 2.0f * (1.0f - dst) * (1.0f - src);
            }
        };

        /** Blend de un canal normalizado [0..1]. */
        abstract float blend(float src, float dst);
    }

    // ── Composite ─────────────────────────────────────────────────────────────

    private static final class BlendComposite implements Composite {

        private final BlendOp op;
        private final float   opacity;

        BlendComposite(BlendOp op, float opacity) {
            this.op      = op;
            this.opacity = opacity;
        }

        @Override
        public CompositeContext createContext(ColorModel srcColorModel,
                                             ColorModel dstColorModel,
                                             RenderingHints hints) {
            // Nuevo BlendContext por drawImage — los buffers son campos de instancia.
            return new BlendContext(op, opacity);
        }
    }

    // ── CompositeContext ──────────────────────────────────────────────────────

    private static final class BlendContext implements CompositeContext {

        private final BlendOp op;
        private final float   opacity;

        // ── HRFC-003.6: buffers como campos de instancia ──────────────────────
        // BlendContext es creado una vez por drawImage() y reutilizado para
        // todos los tiles del mismo sprite. Los buffers se asignan aquí una
        // sola vez y se reutilizan en cada llamada a compose().
        // Cero allocations de int[] durante el procesamiento de píxeles.
        private final int[] srcPixel = new int[4];
        private final int[] dstPixel = new int[4];
        private final int[] outPixel = new int[4];

        BlendContext(BlendOp op, float opacity) {
            this.op      = op;
            this.opacity = opacity;
        }

        @Override
        public void dispose() { /* campos primitivos — nada que limpiar */ }

        /**
         * Aplica el blend sobre el área de intersección tile.
         *
         * Los buffers srcPixel/dstPixel/outPixel son campos de instancia
         * reutilizados en cada invocación — zero allocation por compose().
         *
         * AWT entrega píxeles en TYPE_INT_ARGB: canales [R, G, B, A] en [0..255].
         * Se normaliza a [0..1] para las fórmulas y se desnormaliza al final.
         */
        @Override
        public void compose(Raster src, Raster dstIn, WritableRaster dstOut) {
            int w = Math.min(src.getWidth(),  dstIn.getWidth());
            int h = Math.min(src.getHeight(), dstIn.getHeight());

            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    src.getPixel(x, y, srcPixel);
                    dstIn.getPixel(x, y, dstPixel);

                    // Normalizar [0..255] → [0..1]
                    float sA = srcPixel[3] / 255.0f;
                    float sR = srcPixel[0] / 255.0f;
                    float sG = srcPixel[1] / 255.0f;
                    float sB = srcPixel[2] / 255.0f;

                    float dA = dstPixel[3] / 255.0f;
                    float dR = dstPixel[0] / 255.0f;
                    float dG = dstPixel[1] / 255.0f;
                    float dB = dstPixel[2] / 255.0f;

                    // Operación de blend por canal RGB
                    float bR = op.blend(sR, dR);
                    float bG = op.blend(sG, dG);
                    float bB = op.blend(sB, dB);

                    // Factor de mezcla: lerp entre destino y resultado del blend
                    float t = sA * opacity;
                    float oR = dR + (bR - dR) * t;
                    float oG = dG + (bG - dG) * t;
                    float oB = dB + (bB - dB) * t;

                    // Alpha SRC_OVER: compositing premultiplicado correcto
                    float oA = dA + sA * (1.0f - dA);

                    // Desnormalizar y clamp [0..1] → [0..255]
                    outPixel[0] = clamp(oR);
                    outPixel[1] = clamp(oG);
                    outPixel[2] = clamp(oB);
                    outPixel[3] = clamp(oA);

                    dstOut.setPixel(x, y, outPixel);
                }
            }
        }

        private static int clamp(float v) {
            return (int)(Math.max(0.0f, Math.min(1.0f, v)) * 255.0f + 0.5f);
        }
    }
}
