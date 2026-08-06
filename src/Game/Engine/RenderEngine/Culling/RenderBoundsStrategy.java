package Game.Engine.RenderEngine.Culling;

import Game.Engine.Camera.GameCamera;

/**
 * Estrategia para calcular el área de render (RenderBounds) cada frame.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * Una RenderBoundsStrategy decide qué área del mundo debe procesarse en
 * cada frame para el culling y el render. El resultado es un RenderBounds
 * que puede ser simétrico, asimétrico o calculado dinámicamente.
 *
 * ── EXTENSIBILIDAD ────────────────────────────────────────────────────────
 * Nuevas estrategias se añaden implementando esta interfaz sin tocar
 * SceneRenderer ni los sistemas de render existentes.
 *
 * Implementaciones incluidas:
 *   SYMMETRIC           → framebuffer exacto (comportamiento original)
 *   extended(...)       → márgenes configurables por dirección
 *
 * Implementaciones futuras sin necesidad de cambiar el Engine:
 *   DirectionalRender   → ampliar en la dirección de movimiento
 *   PredictiveRender    → ampliar donde el player probablemente irá
 *   FocusedRender       → ampliar donde apunta el cursor/mouse
 *   DebugRender         → mostrar área extra para inspección
 */
@FunctionalInterface
public interface RenderBoundsStrategy {

    /**
     * Calcula el RenderBounds para el frame actual.
     *
     * @param camera la cámara activa del Engine
     * @return el área de render a usar en este frame
     */
    RenderBounds compute(GameCamera camera);

    // ── Implementaciones base ─────────────────────────────────────────────

    /**
     * Simétrico: el framebuffer exacto sin extensión.
     * Comportamiento idéntico al culling anterior — retrocompatible.
     */
    RenderBoundsStrategy SYMMETRIC = RenderBounds::symmetric;

    /**
     * Crea una estrategia con márgenes asimétricos fijos.
     *
     * @param extraLeft   píxeles extra hacia la izquierda
     * @param extraTop    píxeles extra hacia arriba
     * @param extraRight  píxeles extra hacia la derecha
     * @param extraBottom píxeles extra hacia abajo
     */
    static RenderBoundsStrategy extended(double extraLeft, double extraTop,
                                          double extraRight, double extraBottom) {
        return camera -> RenderBounds.extended(camera, extraLeft, extraTop,
                                                extraRight, extraBottom);
    }

    /**
     * Márgenes uniformes en todas las direcciones.
     *
     * @param extra píxeles extra en cada dirección
     */
    static RenderBoundsStrategy uniform(double extra) {
        return extended(extra, extra, extra, extra);
    }

    /**
     * Margen solo hacia abajo (útil para ver plataformas debajo).
     */
    static RenderBoundsStrategy extraBelow(double pixels) {
        return extended(0, 0, 0, pixels);
    }

    /**
     * Margen hacia adelante en la dirección indicada.
     * Útil para estrategias predictivas simples.
     *
     * @param dx dirección X normalizada [-1, 1]
     * @param dy dirección Y normalizada [-1, 1]
     * @param distance píxeles de extensión
     */
    static RenderBoundsStrategy directional(double dx, double dy, double distance) {
        double left   = dx < 0 ? -dx * distance : 0;
        double top    = dy < 0 ? -dy * distance : 0;
        double right  = dx > 0 ?  dx * distance : 0;
        double bottom = dy > 0 ?  dy * distance : 0;
        return extended(left, top, right, bottom);
    }
}
