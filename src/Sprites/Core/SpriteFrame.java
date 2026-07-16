package Sprites.Core;

import java.awt.image.BufferedImage;

/**
 * SpriteFrame — unidad mínima e inmutable de representación visual.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * Encapsula una BufferedImage junto con metadatos de posicionamiento
 * (pivot y offset nativo). El RenderEngine la consume; el Gameplay
 * nunca toca BufferedImage directamente.
 *
 * ── GARANTÍAS DE INDEPENDENCIA ────────────────────────────────────────────
 * Un SpriteFrame es completamente independiente de cualquier otro frame:
 *
 *   1. La BufferedImage que recibe ya debe ser un buffer aislado.
 *      GridExtractor la produce via Raster.setRect() — copia exacta
 *      sin compositing, sin raster compartido, sin referencia al sheet.
 *
 *   2. SpriteFrame no copia ni modifica la imagen recibida.
 *      La referencia es final; no hay setters.
 *
 *   3. getImage() expone la referencia interna porque Graphics2D la necesita
 *      para drawImage(). El caller no debe modificar el contenido del buffer.
 *      (En la práctica el pipeline solo lee la imagen, nunca la modifica.)
 *
 *   4. No existe estado mutable. Múltiples entidades pueden compartir la
 *      misma instancia de SpriteFrame sin riesgo de contaminación.
 *      (Animation.still() hace exactamente esto para el frame idle.)
 *
 * ── CELDA VS CONTENIDO ────────────────────────────────────────────────────
 * El frame tiene el tamaño de la CELDA del spritesheet, no del sprite dibujado.
 * El sprite real puede ocupar solo una parte de esa celda; el resto son
 * píxeles transparentes. Esto es correcto y esperado.
 *
 * Ejemplo: Player (Carlitos) mide ~15×24 px dentro de una celda 24×24 px.
 * El SpriteFrame es 24×24, con ~9 px de transparencia lateral. No es un bug.
 *
 * Para conocer el área con contenido real se puede usar AutoRegionExtractor,
 * que calcula el bounding box de píxeles no transparentes.
 *
 * ── PIVOT ─────────────────────────────────────────────────────────────────
 * El pivot define el "ancla" del frame en coordenadas locales normalizadas
 * [0..1]. El RenderEngine lo usa para alinear el frame con la posición del
 * objeto (hitbox, bounding box, etc.).
 *
 *   (0.0, 0.0) → esquina superior izquierda (default)
 *   (0.5, 0.5) → centro
 *   (0.5, 1.0) → centro inferior — útil para personajes de pie
 *
 * ── OFFSET NATIVO ─────────────────────────────────────────────────────────
 * Desplazamiento en píxeles del frame respecto al origen del sprite.
 * Útil para frames de atlas con padding variable entre celdas.
 * Normalmente (0, 0) para spritesheets en cuadrícula uniforme.
 *
 * ── INSTANCIA VACÍA ───────────────────────────────────────────────────────
 * SpriteFrame.EMPTY es una constante nula-segura que el pipeline omite
 * silenciosamente. Usar en lugar de null.
 */
public final class SpriteFrame {

    /**
     * Frame vacío null-safe compartido. El RenderEngine lo omite.
     * Usar en lugar de null o de llamar empty() repetidamente.
     */
    public static final SpriteFrame EMPTY = new SpriteFrame(null, 0f, 0f, 0, 0);

    // ── Campos ────────────────────────────────────────────────────────────

    private final BufferedImage image;

    /** Pivot X normalizado [0..1]. 0 = izquierda, 0.5 = centro, 1 = derecha. */
    private final float pivotX;

    /** Pivot Y normalizado [0..1]. 0 = arriba, 0.5 = centro, 1 = abajo. */
    private final float pivotY;

    /** Offset nativo X en píxeles lógicos (para atlas con padding variable). */
    private final int nativeOffsetX;

    /** Offset nativo Y en píxeles lógicos. */
    private final int nativeOffsetY;

    // ── Constructores ────────────────────────────────────────────────────

    /**
     * Frame simple — pivot en (0,0), sin offset.
     * El RenderEngine dibuja desde la esquina superior izquierda.
     */
    public SpriteFrame(BufferedImage image) {
        this(image, 0f, 0f, 0, 0);
    }

    /**
     * Frame con pivot configurable, sin offset nativo.
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
     * @param image         imagen del frame (null → frame vacío, se omite al render)
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

    /**
     * Frame con pivot en el centro exacto (0.5, 0.5).
     * Útil para objetos que deben rotar o escalarse desde el centro.
     */
    public static SpriteFrame centered(BufferedImage image) {
        return new SpriteFrame(image, 0.5f, 0.5f);
    }

    /**
     * Frame con pivot en el centro-inferior (0.5, 1.0).
     * Ideal para personajes parados — la base del sprite queda en la posición del objeto.
     */
    public static SpriteFrame bottomCenter(BufferedImage image) {
        return new SpriteFrame(image, 0.5f, 1.0f);
    }

    /**
     * Frame vacío null-safe. El RenderEngine lo omite silenciosamente.
     *
     * Preferir la constante {@link #EMPTY} para evitar allocations innecesarias.
     * Este método existe por compatibilidad con código existente.
     */
    public static SpriteFrame empty() {
        return EMPTY;
    }

    // ── Consulta ──────────────────────────────────────────────────────────

    /**
     * Imagen del frame.
     *
     * Puede ser null si el recurso no se cargó o si es un frame vacío.
     * El caller no debe modificar el contenido de este buffer.
     */
    public BufferedImage getImage() { return image; }

    /** Ancho de la imagen en píxeles. 0 si la imagen es null. */
    public int getWidth()  { return image != null ? image.getWidth()  : 0; }

    /** Alto de la imagen en píxeles. 0 si la imagen es null. */
    public int getHeight() { return image != null ? image.getHeight() : 0; }

    public float getPivotX()        { return pivotX;        }
    public float getPivotY()        { return pivotY;        }
    public int   getNativeOffsetX() { return nativeOffsetX; }
    public int   getNativeOffsetY() { return nativeOffsetY; }

    /** true si el frame tiene imagen válida y puede ser renderizado. */
    public boolean isValid() { return image != null; }

    /** true si el frame no tiene imagen (es un placeholder vacío). */
    public boolean isEmpty() { return image == null; }

    @Override
    public String toString() {
        if (!isValid()) return "SpriteFrame[EMPTY]";
        return "SpriteFrame[" + getWidth() + "x" + getHeight()
               + " pivot=(" + pivotX + "," + pivotY + ")"
               + (nativeOffsetX != 0 || nativeOffsetY != 0
                   ? " offset=(" + nativeOffsetX + "," + nativeOffsetY + ")" : "")
               + "]";
    }
}
