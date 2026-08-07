package Game.Engine.RenderEngine.Scene;

import Game.Engine.Camera.GameCamera;
import Game.Engine.GameObjects;
import Game.Engine.RenderEngine.Context.RenderCamera;
import Game.Engine.RenderEngine.Context.RenderContext;
import Game.Engine.RenderEngine.Culling.RenderBounds;
import Game.Engine.RenderEngine.Culling.RenderBoundsStrategy;
import Game.Engine.Systems.DebugRenderSystem;
import Game.Engine.Systems.DebugSettings;
import Game.Engine.Systems.DepthSortedRenderSystem;
import Game.Engine.Systems.RenderSystem;
import Game.World.Core.World;
import Game.World.Region.RenderRegion;
import java.awt.Graphics2D;
import java.util.List;

/**
 * Compositor de la escena del mundo — infraestructura de render del Engine.
 *
 * ── ETAPA 7: RenderRegion + SpatialIndex ──────────────────────────────────
 *
 * ANTES: draw(World, camera, ...) leía world.getObjectsContainer().getObjects()
 *   — la lista completa del WorldObjectsContainer del sector activo.
 *   Solo renderizaba objetos del chunk actual, aunque la cámara viera más allá.
 *
 * AHORA: draw(World, camera, ...) usa RenderRegion para obtener los objetos
 *   visibles desde múltiples chunks, filtrados por el viewport de la cámara.
 *
 *   Pipeline:
 *     1. renderRegion.update(camera)
 *        → calcula RenderBounds en coordenadas globales
 *     2. renderRegion.getVisibleObjectsFromStorage(...)
 *        → estáticos de todos los chunks visibles + todas las entidades dinámicas
 *     3. renderSystem.render(visibleObjects, ctx, renderCamera)
 *        → SpriteRendererComponent aplica culling fino por ViewportCuller
 *
 * ── DESACOPLAMIENTO DE SimulationRegion ───────────────────────────────────
 * RenderRegion es independiente de SimulationRegion. Puede renderizar objetos
 * decorativos estáticos que no se simulan (fondos, efectos lejanos, parallax).
 *
 * ── RETROCOMPATIBILIDAD ───────────────────────────────────────────────────
 * draw(World, GameCamera, Graphics2D, int, int) mantiene su firma.
 * WorldManager no necesita cambios. El render del chunk (0,0) es idéntico.
 */
public class SceneRenderer {

    private final RenderSystem            renderSystem      = new RenderSystem();
    private final DepthSortedRenderSystem depthRenderSystem = new DepthSortedRenderSystem();
    private final DebugRenderSystem       debugRenderSystem;

    private boolean useDepthSort = true;

    /** RenderRegion — determina qué objetos son visibles este frame. */
    private final RenderRegion renderRegion;

    /** Estrategia de cálculo del área de render. */
    private RenderBoundsStrategy renderBoundsStrategy = RenderBoundsStrategy.SYMMETRIC;

    public SceneRenderer(DebugSettings settings) {
        this.debugRenderSystem = new DebugRenderSystem(settings);
        this.renderRegion      = new RenderRegion(RenderBoundsStrategy.SYMMETRIC);
    }

    // ── Configuración ─────────────────────────────────────────────────────

    public void setRenderBoundsStrategy(RenderBoundsStrategy strategy) {
        this.renderBoundsStrategy = (strategy != null) ? strategy : RenderBoundsStrategy.SYMMETRIC;
        renderRegion.setBoundsStrategy(this.renderBoundsStrategy);
    }

    public RenderBoundsStrategy getRenderBoundsStrategy() {
        return renderBoundsStrategy;
    }

    // ── Render principal ──────────────────────────────────────────────────

    /**
     * Dibuja el mundo usando ChunkStorage y DynamicEntityRegistry globales.
     *
     * Esta es la variante correcta post-corrección del bug del Player.
     * Recibe el registry global directamente de WorldManager, no de World.
     * Garantiza que el Player y todas las entidades dinámicas aparezcan
     * independientemente del sector activo.
     *
     * @param chunkStorage   almacén de chunks cargados (para estáticos)
     * @param dynamics       registry global de entidades dinámicas
     * @param camera         la GameCamera activa del Engine
     * @param g              Graphics2D del framebuffer virtual
     * @param virtualWidth   ancho del framebuffer virtual
     * @param virtualHeight  alto del framebuffer virtual
     */
    public void drawGlobal(Game.World.Chunk.ChunkStorage chunkStorage,
                            Game.World.Entity.DynamicEntityRegistry dynamics,
                            GameCamera camera, Graphics2D g,
                            int virtualWidth, int virtualHeight) {

        // 1. Actualizar RenderRegion
        renderRegion.update(camera);

        // 2. Obtener objetos visibles
        List<GameObjects> visibleObjects;

        if (!chunkStorage.isEmpty()) {
            // Mundo con chunks: chunks visibles del storage + todos los dinámicos globales
            visibleObjects = renderRegion.getVisibleObjectsFromStorage(
                camera.getVirtualWidth(),
                camera.getVirtualHeight(),
                chunkStorage,
                dynamics
            );
        } else {
            // Sin chunks aún: solo entidades dinámicas globales
            visibleObjects = new java.util.ArrayList<>(dynamics.getAll());
        }

        // 3. Calcular RenderBounds
        RenderBounds bounds = renderBoundsStrategy.compute(camera);
        renderSystem.setRenderBounds(bounds, virtualWidth, virtualHeight);
        depthRenderSystem.setRenderBounds(bounds, virtualWidth, virtualHeight);

        // 4. Renderizar
        RenderCamera renderCamera = new RenderCamera(camera);
        RenderContext ctx = new RenderContext(g, virtualWidth, virtualHeight);

        if (useDepthSort) {
            depthRenderSystem.render(visibleObjects, ctx, renderCamera);
        } else {
            renderSystem.render(visibleObjects, ctx, renderCamera);
        }
        debugRenderSystem.render(visibleObjects, ctx, renderCamera);
    }

    /**
     * Dibuja el mundo usando la cámara del Engine y RenderRegion.
     * Variante legacy — obtiene los dinámicos de world.getDynamicEntityRegistry().
     *
     * @deprecated Usar {@link #drawGlobal} que recibe el registry global directamente.
     */
    @Deprecated
    public void draw(World world, GameCamera camera, Graphics2D g,
                     int virtualWidth, int virtualHeight) {

        renderRegion.update(camera);

        List<GameObjects> visibleObjects;
        if (!world.getChunkStorage().isEmpty()) {
            visibleObjects = renderRegion.getVisibleObjectsFromStorage(
                camera.getVirtualWidth(),
                camera.getVirtualHeight(),
                world.getChunkStorage(),
                world.getDynamicEntityRegistry()
            );
        } else {
            visibleObjects = new java.util.ArrayList<>(world.getDynamicEntityRegistry().getAll());
        }

        RenderBounds bounds = renderBoundsStrategy.compute(camera);
        renderSystem.setRenderBounds(bounds, virtualWidth, virtualHeight);
        depthRenderSystem.setRenderBounds(bounds, virtualWidth, virtualHeight);

        RenderCamera renderCamera = new RenderCamera(camera);
        RenderContext ctx = new RenderContext(g, virtualWidth, virtualHeight);

        if (useDepthSort) {
            depthRenderSystem.render(visibleObjects, ctx, renderCamera);
        } else {
            renderSystem.render(visibleObjects, ctx, renderCamera);
        }
        debugRenderSystem.render(visibleObjects, ctx, renderCamera);
    }

    /**
     * Variante de render que recibe la lista de objetos directamente.
     * Útil para render de UI, cutscenes, y sistemas externos.
     *
     * @param objects  lista de objetos a renderizar
     * @param camera   la GameCamera activa
     * @param g        Graphics2D del framebuffer
     * @param vw       ancho virtual
     * @param vh       alto virtual
     */
    public void draw(List<GameObjects> objects, GameCamera camera, Graphics2D g,
                     int vw, int vh) {
        RenderBounds bounds = renderBoundsStrategy.compute(camera);
        renderSystem.setRenderBounds(bounds, vw, vh);
        depthRenderSystem.setRenderBounds(bounds, vw, vh);

        RenderCamera renderCamera = new RenderCamera(camera);
        RenderContext ctx = new RenderContext(g, vw, vh);

        if (useDepthSort) {
            depthRenderSystem.render(objects, ctx, renderCamera);
        } else {
            renderSystem.render(objects, ctx, renderCamera);
        }
        debugRenderSystem.render(objects, ctx, renderCamera);
    }

    // ── Control de depth sort ─────────────────────────────────────────────

    public void setUseDepthSort(boolean useDepthSort) {
        this.useDepthSort = useDepthSort;
    }

    public boolean isUsingDepthSort() { return useDepthSort; }

    // ── Acceso ────────────────────────────────────────────────────────────

    public RenderRegion getRenderRegion() { return renderRegion; }
}
