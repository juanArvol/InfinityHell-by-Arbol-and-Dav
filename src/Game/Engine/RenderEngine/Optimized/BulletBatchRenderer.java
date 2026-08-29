package Game.Engine.RenderEngine.Optimized;

import Game.Engine.Camera.GameCamera;
import Game.Engine.Entity.Components.Visuals.SpriteRendererComponent;
import Game.Engine.GameObjects;
import Game.Engine.RenderEngine.Cache.RenderDataCache;
import Game.Engine.RenderEngine.Culling.ViewportCuller;
import Game.Engine.RenderEngine.Strategies.SpriteDrawer;
import Game.Items.Types.Bullets.Definition.Bullet;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

/**
 * BulletBatchRenderer — optimized renderer para proyectiles.
 *
 * ── HRFC — Deep Optimization: Render Data Snapshot + Batching Preparation ──
 *
 * ARQUITECTURA:
 *   FASE 1: Preparation
 *     - Iterar todos los bullets activos
 *     - Para cada bullet, extraer render data UNA VEZ
 *     - Almacenar en RenderDataCache[]
 *     - Aplicar culling temprano
 *
 *   FASE 2: Rendering
 *     - Iterar solo los bullets visibles (post-culling)
 *     - Para cada cache entry, llamar SpriteDrawer.draw()
 *     - Sin component lookups, sin flyweight lookups
 *
 * BENEFICIOS INMEDIATOS:
 *   - Elimina trabajo repetido por bullet (component/flyweight lookup)
 *   - Culling temprano reduce trabajo de rendering
 *   - Datos contiguos en memoria (mejor cache locality)
 *
 * BENEFICIOS FUTUROS (BATCHING):
 *   - El cache permite agrupar bullets con mismo frame/transform
 *   - Reducir Graphics2D state changes
 *   - Preparación para instanced rendering si se migra a GPU
 *
 * USO:
 *   BulletBatchRenderer renderer = new BulletBatchRenderer();
 *   renderer.render(bullets, camera, graphics);
 *
 * COMPATIBILIDAD:
 *   Este renderer es OPCIONAL. El sistema existente sigue funcionando.
 *   Para usar este renderer, llamarlo desde SceneRenderer o RenderSystem.
 */
public class BulletBatchRenderer {

    /**
     * Cache temporal de render data.
     * Se reutiliza entre frames para evitar allocations.
     */
    private final List<RenderDataCache> visibleBullets = new ArrayList<>(2000);

    /**
     * Contador de bullets procesados (para profiling).
     */
    private int processedCount = 0;
    private int culledCount = 0;

    /**
     * Renderiza una lista de bullets usando el optimized pipeline.
     *
     * @param bullets lista de bullets activos
     * @param camera  cámara activa
     * @param g       Graphics2D del framebuffer
     * @param virtualWidth  ancho del viewport
     * @param virtualHeight alto del viewport
     */
    public void render(List<GameObjects> bullets,
                      GameCamera camera,
                      Graphics2D g,
                      int virtualWidth,
                      int virtualHeight) {

        // Reset counters y cache
        visibleBullets.clear();
        processedCount = 0;
        culledCount = 0;

        // ── FASE 1: PREPARATION ───────────────────────────────────────────
        // Extraer render data de cada bullet y aplicar culling temprano.

        for (GameObjects obj : bullets) {
            if (!(obj instanceof Bullet)) {
                continue;  // Solo procesar bullets
            }

            processedCount++;

            // Extraer componente de render
            SpriteRendererComponent renderer = obj.getComponent(SpriteRendererComponent.class);
            if (renderer == null) {
                continue;  // Sin renderer → skip
            }

            // Extraer frame (esto hace el flyweight lookup UNA VEZ)
            var frame = renderer.getCurrentFrame();
            if (frame == null || !frame.isValid()) {
                continue;  // Sin frame válido → skip
            }

            // Extraer transform data
            var transform = renderer.getTransform();
            if (transform == null) {
                continue;  // Sin transform → skip
            }

            // Extraer posición y dimensiones
            var pos = obj.getTransform().getPosition();
            int rw = renderer.getRenderWidth();
            int rh = renderer.getRenderHeight();

            // Crear cache entry
            RenderDataCache cache = new RenderDataCache(
                pos.getX(),
                pos.getY(),
                frame,
                transform,
                rw,
                rh
            );

            // Culling temprano: solo agregar si es visible
            if (cache.isVisible(
                camera.getX(),
                camera.getY(),
                virtualWidth,
                virtualHeight,
                ViewportCuller.DEFAULT_MARGIN
            )) {
                visibleBullets.add(cache);
            } else {
                culledCount++;
            }
        }

        // ── FASE 2: RENDERING ──────────────────────────────────────────────
        // Renderizar solo los bullets visibles usando el cache.

        double cameraX = camera.getX();
        double cameraY = camera.getY();

        for (RenderDataCache cache : visibleBullets) {
            int screenX = cache.getScreenX(cameraX);
            int screenY = cache.getScreenY(cameraY);

            // Llamar al drawer con datos pre-extraídos
            SpriteDrawer.INSTANCE.draw(
                g,
                cache.frame,
                screenX,
                screenY,
                cache.renderWidth,
                cache.renderHeight,
                cache.transform
            );
        }
    }

    /**
     * Variante que acepta solo bullets (sin necesidad de filtrar).
     *
     * @param bullets lista de Bullet objects
     * @param camera  cámara activa
     * @param g       Graphics2D del framebuffer
     * @param virtualWidth  ancho del viewport
     * @param virtualHeight alto del viewport
     */
    public void renderBullets(List<Bullet> bullets,
                             GameCamera camera,
                             Graphics2D g,
                             int virtualWidth,
                             int virtualHeight) {

        visibleBullets.clear();
        processedCount = 0;
        culledCount = 0;

        for (Bullet bullet : bullets) {
            processedCount++;

            SpriteRendererComponent renderer = bullet.getComponent(SpriteRendererComponent.class);
            if (renderer == null) continue;

            var frame = renderer.getCurrentFrame();
            if (frame == null || !frame.isValid()) continue;

            var transform = renderer.getTransform();
            if (transform == null) continue;

            var pos = bullet.getTransform().getPosition();
            int rw = renderer.getRenderWidth();
            int rh = renderer.getRenderHeight();

            RenderDataCache cache = new RenderDataCache(
                pos.getX(), pos.getY(), frame, transform, rw, rh
            );

            if (cache.isVisible(
                camera.getX(), camera.getY(),
                virtualWidth, virtualHeight,
                ViewportCuller.DEFAULT_MARGIN
            )) {
                visibleBullets.add(cache);
            } else {
                culledCount++;
            }
        }

        double cameraX = camera.getX();
        double cameraY = camera.getY();

        for (RenderDataCache cache : visibleBullets) {
            int screenX = cache.getScreenX(cameraX);
            int screenY = cache.getScreenY(cameraY);

            SpriteDrawer.INSTANCE.draw(
                g, cache.frame, screenX, screenY,
                cache.renderWidth, cache.renderHeight,
                cache.transform
            );
        }
    }

    // ── Profiling / Diagnostics ───────────────────────────────────────────

    /**
     * Retorna el número de bullets procesados en el último render.
     */
    public int getProcessedCount() {
        return processedCount;
    }

    /**
     * Retorna el número de bullets culled (no renderizados) en el último render.
     */
    public int getCulledCount() {
        return culledCount;
    }

    /**
     * Retorna el número de bullets efectivamente renderizados en el último render.
     */
    public int getRenderedCount() {
        return visibleBullets.size();
    }

    /**
     * Imprime estadísticas del último render.
     */
    public void printStats() {
        System.out.printf(
            "[BulletBatchRenderer] Processed=%d, Rendered=%d, Culled=%d (%.1f%% saved)%n",
            processedCount,
            getRenderedCount(),
            culledCount,
            culledCount > 0 ? (culledCount * 100.0 / processedCount) : 0.0
        );
    }
}
