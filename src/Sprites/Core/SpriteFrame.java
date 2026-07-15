package Sprites.Core;

import java.awt.image.BufferedImage;

/**
 * Un frame individual dentro del pipeline de sprites.
 *
 * SpriteFrame es la unidad mínima de representación visual. Encapsula la
 * BufferedImage junto con metadatos opcionales que el RenderEngine puede
 * usar para posicionar el frame correctamente (pivot, offset nativo).
 *
 * ── DESACOPLAMIENTO ────────────────────────────────────────────────────────
 * El Gameplay nunca toca BufferedImage directamente. Trabaja con SpriteHandle,
 * que internamente resuelve SpriteFrame cuando el RenderEngine lo necesita.
 *
 * ── PIVOT ──────────────────────────────────────────────────────────────────
 * El pivot define el "ancla" del frame en coordenadas locales (0,0 = esquina
 * superior izquierda, 0.5,0.5 = centro). El RenderEngine lo usa para alinear
 * el frame con la posición del objeto (hitbox, bounding box, etc.).
 * Por defecto (0,0): se dibuja desde la esquina superior izquierda.
 *
 * ── OFFSET NATIVO ──────────────────────────────────────────────────────────
 * Desplazamiento nativo en píxeles del frame respecto al origen del sprite.
 * Útil para frames de animación que tienen diferentes cantidades de relleno
 * en el atlas. Normalmente (0,0).
 */
public final class SpriteFrame {

    private final BufferedImage image;

    /** Pivot X normalizado [0..1]. 0 = izquierda, 0.5 = centro, 1 = derecha. */
    private final float pivotX;

    /** Pivot Y normalizado [0..1]. 0 = arriba, 0.5 = centro, 1 = abajo. */
    private final float pivotY;

    /** Offset nativo en píxeles lógicos (para frames de atlas irregulares). */
    private final int nativeOffsetX;
    private final int nativeOffsetY;

    // ── Constructores ────────────────────────────────────────────────────

    /**
     * Frame simple sin pivot ni offset. El RenderEngine dibuja desde
     * la esquina superior izquierda.
     */
    public SpriteFrame(BufferedImage image) {
        this(image, 0f, 0f, 0, 0);
    }

    /**
     * Frame con pivot configurable y sin offset nativo.
     *
     * @param pivotX pivot X normalizado [0..1]
     * @param pivotY pivot Y normalizado [0..1]
     */
    public SpriteFrame(BufferedImage image, float pivotX, float pivotY) {
        this(image, pivotX, pivotY, 0, 0);
    }

    /**
     * Frame completo con pivot y offset nativo.
     *
     * @param image         imagen del frame (puede ser null — se dibuja nada)
     * @param pivotX        pivot X normalizado [0..1]
     * @param pivotY        pivot Y normalizado [0..1]
     * @param nativeOffsetX desplazamiento X en píxeles lógicos
     * @param nativeOffsetY desplazamiento Y en píxeles lógicos
     */
    public SpriteFrame(BufferedImage image,
                       float pivotX, float pivotY,
                       int nativeOffsetX, int nativeOffsetY) {
        this.image         = image;
        this.pivotX        = pivotX;
        this.pivotY        = pivotY;
        this.nativeOffsetX = nativeOffsetX;
        this.nativeOffsetY = nativeOffsetY;
    }

    // ── Fábricas de conveniencia ─────────────────────────────────────────

    /** Frame con pivot en el centro (0.5, 0.5). */
    public static SpriteFrame centered(BufferedImage image) {
        return new SpriteFrame(image, 0.5f, 0.5f);
    }

    /** Frame con pivot en la parte inferior-centro (0.5, 1.0). Útil para personajes parados. */
    public static SpriteFrame bottomCenter(BufferedImage image) {
        return new SpriteFrame(image, 0.5f, 1.0f);
    }

    /** Frame vacío (null-safe, el RenderEngine lo omite). */
    public static SpriteFrame empty() {
        return new SpriteFrame(null);
    }

    // ── API de consulta ──────────────────────────────────────────────────

    /** Imagen del frame. Puede ser null si el recurso no se cargó correctamente. */
    public BufferedImage getImage()       { return image; }

    /** Ancho en píxeles. 0 si la imagen es null. */
    public int getWidth()  { return image != null ? image.getWidth()  : 0; }

    /** Alto en píxeles. 0 si la imagen es null. */
    public int getHeight() { return image != null ? image.getHeight() : 0; }

    public float getPivotX()        { return pivotX;        }
    public float getPivotY()        { return pivotY;        }
    public int   getNativeOffsetX() { return nativeOffsetX; }
    public int   getNativeOffsetY() { return nativeOffsetY; }

    /** true si el frame tiene imagen cargada y puede representarse. */
    public boolean isValid() { return image != null; }

    @Override
    public String toString() {
        return "SpriteFrame[" + getWidth() + "x" + getHeight()
               + " pivot=(" + pivotX + "," + pivotY + ")]";
    }
}
