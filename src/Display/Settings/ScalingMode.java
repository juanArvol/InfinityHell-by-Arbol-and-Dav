package Display.Settings;

/**
 * Modos de escalado del framebuffer virtual hacia pantalla física.
 *
 * FIT            – escala manteniendo aspect ratio, con letterbox/pillarbox.
 *                  El juego nunca se deforma. Recomendado por defecto.
 *
 * FILL           – escala para cubrir toda la pantalla manteniendo aspect ratio.
 *                  Puede recortar partes del juego en los bordes.
 *
 * STRETCH        – deforma el juego para llenar toda la pantalla.
 *                  No recomendado salvo casos específicos.
 *
 * INTEGER_SCALE  – escala solo en factores enteros (1x, 2x, 3x…).
 *                  Para pixel art: elimina deformación de píxeles.
 *                  Puede dejar barras grandes en resoluciones raras.
 *
 * PIXEL_PERFECT  – igual que INTEGER_SCALE pero siempre hacia abajo.
 *                  Garantiza que cada píxel virtual = N píxeles exactos.
 *                  Útil para juegos retro donde la pureza pixel es crítica.
 */
public enum ScalingMode {
    FIT,
    FILL,
    STRETCH,
    INTEGER_SCALE,
    PIXEL_PERFECT
}
