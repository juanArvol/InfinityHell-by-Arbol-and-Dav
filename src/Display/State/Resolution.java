package Display.State;

/**
 * Resolución inmutable (ancho × alto en píxeles).
 *
 * ──────────────────────────────────────────────────────────────────────────
 * MOTIVACIÓN
 *
 * La resolución virtual y la resolución real se manejaban como pares de
 * int sueltos (virtualWidth, virtualHeight) en múltiples clases. Esto
 * impedía:
 *
 *   1. Comparar resoluciones de forma expresiva.
 *   2. Pasar resoluciones como unidad a comandos y eventos.
 *   3. Representar un cambio de resolución como una operación tipada.
 *
 * Resolution es un value object inmutable que encapsula ambos valores.
 * Se integra con DisplayState, DisplayCommand, y el Reconfiguration Pipeline.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * THREAD SAFETY
 *
 * Inmutable. Seguro compartir entre threads sin sincronización.
 */
public final class Resolution {

    public final int width;
    public final int height;

    public Resolution(int width, int height) {
        if (width  <= 0) throw new IllegalArgumentException("width must be > 0, got "  + width);
        if (height <= 0) throw new IllegalArgumentException("height must be > 0, got " + height);
        this.width  = width;
        this.height = height;
    }

    /** Relación de aspecto como float. */
    public float aspectRatio() {
        return (float) width / height;
    }

    /** True si este objeto representa la misma resolución que el otro. */
    public boolean isSameAs(Resolution other) {
        return other != null && this.width == other.width && this.height == other.height;
    }

    /** Devuelve una nueva Resolution escalada por el factor entero dado. */
    public Resolution scale(int factor) {
        if (factor <= 0) throw new IllegalArgumentException("factor must be > 0");
        return new Resolution(width * factor, height * factor);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Resolution r)) return false;
        return width == r.width && height == r.height;
    }

    @Override
    public int hashCode() {
        return 31 * width + height;
    }

    @Override
    public String toString() {
        return width + "x" + height;
    }

    // ── Constantes de uso frecuente ──────────────────────────────────────────

    public static final Resolution HD    = new Resolution(1280,  720);
    public static final Resolution FHD   = new Resolution(1920, 1080);
    public static final Resolution QHD   = new Resolution(2560, 1440);
    public static final Resolution UHD4K = new Resolution(3840, 2160);
}
