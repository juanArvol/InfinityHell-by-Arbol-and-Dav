package Game.Engine.RenderEngine.Cache;

import Game.Engine.RenderEngine.Transform.TransformData;
import Sprites.Core.SpriteFrame;

/**
 * RenderDataCache — snapshot inmutable de datos necesarios para renderizar un objeto.
 *
 * ── HRFC — Deep Optimization: Render Data Snapshot ────────────────────────
 *
 * PROBLEMA:
 *   Para renderizar cada bullet, el renderer actualmente realiza:
 *     1. obj.getComponent(SpriteRendererComponent.class)
 *     2. component.resolveFrame() → flyweight lookup
 *     3. component.getTransform() → transform data
 *     4. obj.getTransform().getPosition() → world position
 *     5. ViewportCuller.isVisible() → culling check
 *
 *   Con 1500 bullets, estas operaciones se repiten 1500 veces por frame.
 *
 * SOLUCIÓN:
 *   Preparar toda la información de render UNA VEZ por bullet antes del loop
 *   de rendering. El renderer solo itera sobre RenderDataCache[] y dibuja.
 *
 * CONTENIDO DEL CACHE:
 *   - worldX, worldY: posición en el mundo
 *   - frame: SpriteFrame a dibujar
 *   - transform: TransformData (rotation, scale, alpha, etc.)
 *   - renderWidth, renderHeight: dimensiones de render
 *
 * USO:
 *   RenderDataCache cache = new RenderDataCache(
 *       bullet.getTransform().getPosition().getX(),
 *       bullet.getTransform().getPosition().getY(),
 *       spriteFrame,
 *       transformData,
 *       width,
 *       height
 *   );
 *
 * BENEFICIOS:
 *   - Elimina component lookups repetidos
 *   - Elimina flyweight lookups repetidos
 *   - Datos contiguos en memoria (cache-friendly)
 *   - Facilita batching posterior (agrupar por frame/transform)
 *
 * NOTA:
 *   Este es un snapshot INMUTABLE del estado del frame. No debe mutar.
 *   Se crea una vez por bullet por frame, se usa para render, luego se descarta.
 */
public final class RenderDataCache {

    // Posición en el mundo
    public final double worldX;
    public final double worldY;

    // Sprite data
    public final SpriteFrame frame;
    
    // Transform data
    public final TransformData transform;
    
    // Dimensiones de render
    public final int renderWidth;
    public final int renderHeight;

    /**
     * Constructor completo.
     *
     * @param worldX       posición X en el mundo
     * @param worldY       posición Y en el mundo
     * @param frame        SpriteFrame a renderizar
     * @param transform    TransformData (rotation, scale, alpha, etc.)
     * @param renderWidth  ancho de render en píxeles
     * @param renderHeight alto de render en píxeles
     */
    public RenderDataCache(
            double worldX,
            double worldY,
            SpriteFrame frame,
            TransformData transform,
            int renderWidth,
            int renderHeight
    ) {
        this.worldX = worldX;
        this.worldY = worldY;
        this.frame = frame;
        this.transform = transform;
        this.renderWidth = renderWidth;
        this.renderHeight = renderHeight;
    }

    /**
     * Convierte posición de mundo a pantalla usando el offset de cámara.
     *
     * @param cameraX offset X de la cámara
     * @return posición X en pantalla
     */
    public int getScreenX(double cameraX) {
        return (int)(worldX - cameraX) + transform.offsetX;
    }

    /**
     * Convierte posición de mundo a pantalla usando el offset de cámara.
     *
     * @param cameraY offset Y de la cámara
     * @return posición Y en pantalla
     */
    public int getScreenY(double cameraY) {
        return (int)(worldY - cameraY) + transform.offsetY;
    }

    /**
     * Verifica si este objeto es visible en el viewport dado.
     *
     * @param cameraX       offset X de la cámara
     * @param cameraY       offset Y de la cámara
     * @param virtualWidth  ancho del viewport
     * @param virtualHeight alto del viewport
     * @param margin        margen extra para el culling
     * @return true si es visible
     */
    public boolean isVisible(double cameraX, double cameraY,
                             int virtualWidth, int virtualHeight,
                             int margin) {
        int screenX = getScreenX(cameraX);
        int screenY = getScreenY(cameraY);

        int vpLeft   = -margin;
        int vpTop    = -margin;
        int vpRight  = virtualWidth  + margin;
        int vpBottom = virtualHeight + margin;

        if (screenX + renderWidth  < vpLeft)   return false;
        if (screenY + renderHeight < vpTop)    return false;
        if (screenX                > vpRight)  return false;
        if (screenY                > vpBottom) return false;

        return true;
    }

    @Override
    public String toString() {
        return String.format(
            "RenderDataCache[pos=(%.1f,%.1f), size=(%d,%d), frame=%s]",
            worldX, worldY, renderWidth, renderHeight,
            frame != null ? frame.toString() : "null"
        );
    }
}
