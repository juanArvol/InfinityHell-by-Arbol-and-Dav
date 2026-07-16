package Game.Engine.RenderEngine.Transform;

import java.awt.Color;

/**
 * TransformData — descripción inmutable de cómo debe representarse visualmente un sprite.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * Encapsula TODOS los parámetros de transformación visual:
 *   flip, escala independiente X/Y, rotación, pivot, offset, alpha, tint, blend.
 *
 * Es un value object inmutable. Se construye con el Builder y se pasa
 * al RenderEngine. El Gameplay nunca toca Graphics2D.
 *
 * ── PATRÓN ────────────────────────────────────────────────────────────────
 * Immutable + Builder fluido.
 *
 *   TransformData t = TransformData.builder()
 *       .flipH(true)
 *       .scaleX(2.0f)
 *       .alpha(0.5f)
 *       .tint(Color.RED, 0.3f)
 *       .build();
 *
 * ── PIVOT ─────────────────────────────────────────────────────────────────
 * Pivot normalizado [0..1]:
 *   (0,0) = esquina superior izquierda.
 *   (0.5, 0.5) = centro.
 *   (0.5, 1.0) = centro inferior (útil para personajes de pie).
 *
 * La rotación y la escala se aplican alrededor del pivot.
 *
 * ── OFFSET ────────────────────────────────────────────────────────────────
 * Desplazamiento adicional en píxeles aplicado después del pivot.
 * Permite ajustar la posición visual de una pieza del SpriteComposite
 * sin mover el transform del gameObject.
 *
 * ── BLEND MODE ────────────────────────────────────────────────────────────
 * Preparado para futuras técnicas de composición. Por ahora NORMAL (SRC_OVER).
 *
 * ── INSTANCIA IDENTIDAD ───────────────────────────────────────────────────
 * TransformData.IDENTITY representa la ausencia de transformación.
 * Es la instancia por defecto: sin flip, escala 1, sin rotación, alpha 1.
 * Los renderizadores la usan para tomar el path rápido sin transformaciones.
 */
public final class TransformData {

    // ── Constante identidad ───────────────────────────────────────────────────

    /** Sin ninguna transformación. Path de render más rápido. */
    public static final TransformData IDENTITY = new Builder().build();

    // ── Campos ────────────────────────────────────────────────────────────────

    /** Flip horizontal. */
    public final boolean flipH;

    /** Flip vertical. */
    public final boolean flipV;

    /** Escala horizontal. 1.0 = sin escala. */
    public final float scaleX;

    /** Escala vertical. 1.0 = sin escala. */
    public final float scaleY;

    /** Rotación en radianes. 0 = sin rotación. */
    public final float rotation;

    /** Pivot X normalizado [0..1]. Centro de rotación/escala. */
    public final float pivotX;

    /** Pivot Y normalizado [0..1]. */
    public final float pivotY;

    /** Offset adicional en píxeles (X). */
    public final int offsetX;

    /** Offset adicional en píxeles (Y). */
    public final int offsetY;

    /** Transparencia [0..1]. 1 = opaco, 0 = invisible. */
    public final float alpha;

    /**
     * Color de tinte aplicado sobre el sprite. null = sin tinte.
     * El tinte se aplica con transparencia tintAlpha.
     */
    public final Color tintColor;

    /** Intensidad del tinte [0..1]. 0 = invisible, 1 = completamente cubierto. */
    public final float tintAlpha;

    /** Modo de blend. Por defecto NORMAL. Preparado para futuras técnicas. */
    public final BlendMode blendMode;

    // ── Constructor privado (solo Builder) ────────────────────────────────────

    private TransformData(Builder b) {
        this.flipH      = b.flipH;
        this.flipV      = b.flipV;
        this.scaleX     = b.scaleX;
        this.scaleY     = b.scaleY;
        this.rotation   = b.rotation;
        this.pivotX     = b.pivotX;
        this.pivotY     = b.pivotY;
        this.offsetX    = b.offsetX;
        this.offsetY    = b.offsetY;
        this.alpha      = b.alpha;
        this.tintColor  = b.tintColor;
        this.tintAlpha  = b.tintAlpha;
        this.blendMode  = b.blendMode;
    }

    // ── Consultas de estado rápido ────────────────────────────────────────────

    /**
     * true si no hay ninguna transformación activa.
     * Permite a los renderizadores tomar el path rápido sin composición.
     */
    public boolean isIdentity() {
        return !flipH
            && !flipV
            && scaleX     == 1.0f
            && scaleY     == 1.0f
            && rotation   == 0.0f
            && offsetX    == 0
            && offsetY    == 0
            && alpha      == 1.0f
            && tintColor  == null
            && blendMode  == BlendMode.NORMAL;
    }

    /** true si requiere cualquier transformación geométrica (flip, escala, rotación). */
    public boolean hasGeometricTransform() {
        return flipH || flipV
            || scaleX != 1.0f || scaleY != 1.0f
            || rotation != 0.0f;
    }

    /** true si requiere modificación de alpha o tinte. */
    public boolean hasColorTransform() {
        return alpha < 1.0f || tintColor != null;
    }

    /** true si tiene flip (horizontal o vertical). */
    public boolean hasFlip() { return flipH || flipV; }

    /** true si tiene escala diferente de 1 en algún eje. */
    public boolean hasScale() { return scaleX != 1.0f || scaleY != 1.0f; }

    /** true si tiene rotación activa. */
    public boolean hasRotation() { return rotation != 0.0f; }

    /** true si tiene offset activo. */
    public boolean hasOffset() { return offsetX != 0 || offsetY != 0; }

    /** true si tiene tinte activo. */
    public boolean hasTint() { return tintColor != null && tintAlpha > 0.0f; }

    // ── Builder ───────────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static final class Builder {

        private boolean   flipH      = false;
        private boolean   flipV      = false;
        private float     scaleX     = 1.0f;
        private float     scaleY     = 1.0f;
        private float     rotation   = 0.0f;
        private float     pivotX     = 0.5f;
        private float     pivotY     = 0.5f;
        private int       offsetX    = 0;
        private int       offsetY    = 0;
        private float     alpha      = 1.0f;
        private Color     tintColor  = null;
        private float     tintAlpha  = 0.0f;
        private BlendMode blendMode  = BlendMode.NORMAL;

        private Builder() {}

        /** Flip horizontal. */
        public Builder flipH(boolean v)      { this.flipH     = v;  return this; }

        /** Flip vertical. */
        public Builder flipV(boolean v)      { this.flipV     = v;  return this; }

        /** Escala uniforme. */
        public Builder scale(float s)        { this.scaleX = s; this.scaleY = s; return this; }

        /** Escala independiente X e Y. */
        public Builder scaleX(float x)       { this.scaleX    = x;  return this; }
        public Builder scaleY(float y)       { this.scaleY    = y;  return this; }

        /** Rotación en radianes. */
        public Builder rotation(float r)     { this.rotation  = r;  return this; }

        /** Pivot normalizado [0..1]. Default: centro (0.5, 0.5). */
        public Builder pivot(float px, float py) { this.pivotX = px; this.pivotY = py; return this; }

        /** Pivot X normalizado [0..1]. */
        public Builder pivotX(float px)      { this.pivotX    = px; return this; }

        /** Pivot Y normalizado [0..1]. */
        public Builder pivotY(float py)      { this.pivotY    = py; return this; }

        /** Offset adicional en píxeles. */
        public Builder offset(int ox, int oy){ this.offsetX = ox; this.offsetY = oy; return this; }

        /** Alpha [0..1]. 1 = opaco. */
        public Builder alpha(float a)        { this.alpha     = Math.max(0, Math.min(1, a)); return this; }

        /**
         * Tinte de color.
         *
         * @param color color del tinte
         * @param tintAlpha intensidad [0..1]; 0 = sin efecto, 1 = color sólido
         */
        public Builder tint(Color color, float tintAlpha) {
            this.tintColor = color;
            this.tintAlpha = Math.max(0, Math.min(1, tintAlpha));
            return this;
        }

        /** Modo de blend. */
        public Builder blendMode(BlendMode m){ this.blendMode  = m;  return this; }

        /** Construye el TransformData inmutable. */
        public TransformData build() { return new TransformData(this); }
    }

    // ── Blend Mode ────────────────────────────────────────────────────────────

    /**
     * Modo de composición visual.
     *
     * Preparado para futuras técnicas. El RenderEngine interpreta este enum
     * y aplica el AlphaComposite o la estrategia correspondiente.
     *
     *   NORMAL       → AlphaComposite.SRC_OVER (comportamiento por defecto)
     *   ADDITIVE     → brillo acumulativo (efectos de luz, fuego, electricidad)
     *   MULTIPLY     → oscurecimiento multiplicativo (sombras, veneno)
     *   SCREEN       → aclarado (glow, bloom soft)
     *   OVERLAY      → contraste (daño, congelación)
     */
    public enum BlendMode {
        NORMAL,
        ADDITIVE,
        MULTIPLY,
        SCREEN,
        OVERLAY
    }

    // ── toString ──────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        if (isIdentity()) return "TransformData[IDENTITY]";
        return "TransformData["
            + (flipH || flipV    ? "flip=(" + flipH + "," + flipV + ") " : "")
            + (hasScale()        ? "scale=(" + scaleX + "," + scaleY + ") " : "")
            + (rotation != 0     ? "rot=" + rotation + " " : "")
            + (hasOffset()       ? "offset=(" + offsetX + "," + offsetY + ") " : "")
            + (alpha < 1         ? "alpha=" + alpha + " " : "")
            + (hasTint()         ? "tint=" + tintColor + " " : "")
            + "blend=" + blendMode
            + "]";
    }
}
