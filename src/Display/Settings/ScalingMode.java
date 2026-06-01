package Display.Settings;

/**
 * Modos de escalado del framebuffer virtual hacia pantalla física.
 *
 * Cada modo describe cómo se transforma el espacio virtual al espacio físico.
 * La lógica de cálculo de cada modo vive en ViewportCalculator, no aquí.
 *
 * ──────────────────────────────────────────────────────────────────────────────
 * NATIVE        – resolución 1:1. El framebuffer virtual se presenta sin escalar.
 *                 Útil para desarrollo, capturas o resoluciones exactas.
 *                 Si el canvas es más pequeño que la resolución virtual, se recorta.
 *
 * FIT           – escala manteniendo aspect ratio, con letterbox/pillarbox.
 *                 El juego nunca se deforma. Recomendado por defecto.
 *
 * INTEGER_SCALE – escala solo en factores enteros (1×, 2×, 3×…).
 *                 Para pixel art: elimina deformación de píxeles.
 *                 Puede dejar barras grandes en resoluciones no múltiplo exacto.
 *
 * PIXEL_PERFECT – igual que INTEGER_SCALE pero siempre ≥ 1× y siempre hacia abajo.
 *                 Garantiza que cada píxel virtual = N píxeles exactos.
 *                 Útil para juegos retro donde la pureza pixel es crítica.
 *
 * FREE_SCALE    – escala libre manteniendo aspect ratio, sin restricción entera.
 *                 Equivale al FIT anterior (alias semánticamente más claro).
 *
 * LETTERBOX     – equivalente a FIT. Nombre explícito para dejar claro que el
 *                 modo produce barras de relleno horizontales o verticales.
 *
 * PILLARBOX     – alias de LETTERBOX. El sistema calcula automáticamente si
 *                 las barras son horizontales o verticales según el aspect ratio.
 *
 * STRETCH       – deforma el juego para llenar toda la pantalla.
 *                 No recomendado salvo casos específicos (dashboards, debug).
 *
 * ──────────────────────────────────────────────────────────────────────────────
 * Agregar un modo nuevo:
 *   1. Añadir la constante aquí.
 *   2. Implementar el cálculo en ViewportCalculator.calculate().
 *   3. Ningún otro fichero del módulo Display requiere modificación.
 */
public enum ScalingMode {

    /** Resolución 1:1, sin escalar. */
    NATIVE,

    /** Escala libre manteniendo aspect ratio (con barras de relleno). */
    FIT,

    /**
     * Alias explícito de FIT.
     * Deja claro que el modo puede producir barras horizontales (letterbox)
     * o verticales (pillarbox) según el aspect ratio.
     */
    LETTERBOX,

    /**
     * Alias de LETTERBOX.
     * Semánticamente idéntico; la barra puede ser lateral o superior/inferior.
     */
    PILLARBOX,

    /** Escala en factores enteros únicamente. */
    INTEGER_SCALE,

    /** Escala entera hacia abajo (pixel-perfect). */
    PIXEL_PERFECT,

    /** Escala libre sin restricción de aspect ratio. */
    FREE_SCALE,

    /** Estira el contenido para llenar toda la superficie física. */
    STRETCH
}
