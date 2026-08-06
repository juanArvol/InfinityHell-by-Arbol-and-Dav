package Game.Engine.RenderEngine.Scene;

import Game.Engine.Camera.GameCamera;
import Game.Engine.RenderEngine.Context.RenderCamera;
import Game.Engine.RenderEngine.Context.RenderContext;
import Game.Engine.RenderEngine.Culling.RenderBounds;
import Game.Engine.RenderEngine.Culling.RenderBoundsStrategy;
import Game.Engine.Systems.DebugRenderSystem;
import Game.Engine.Systems.DebugSettings;
import Game.Engine.Systems.DepthSortedRenderSystem;
import Game.Engine.Systems.RenderSystem;
import Game.World.Core.World;
import java.awt.Graphics2D;

/**
 * Compositor de la escena del mundo — infraestructura de render del Engine.
 *
 * ── HRFC: Desacoplamiento del RenderBounds ────────────────────────────────
 *
 * ANTES: SceneRenderer propagaba virtualWidth/virtualHeight directamente a
 * RenderSystem y DepthSortedRenderSystem via setVirtualSize(). El área de
 * render era siempre el rectángulo exacto del framebuffer. Simétrico e
 * inflexible.
 *
 * AHORA: SceneRenderer calcula un RenderBounds usando una RenderBoundsStrategy
 * inyectable. El RenderBounds se propaga a los sistemas de render y determina
 * qué objetos son visibles para el culling.
 *
 * Por defecto: RenderBoundsStrategy.SYMMETRIC — comportamiento idéntico al
 * anterior, retrocompatible con todo el código existente.
 *
 * ── ESTRATEGIAS DISPONIBLES ───────────────────────────────────────────────
 *   RenderBoundsStrategy.SYMMETRIC     → framebuffer exacto (default)
 *   RenderBoundsStrategy.extended(...)  → márgenes asimétricos configurables
 *
 * Estrategias futuras (implementar en RenderBoundsStrategy):
 *   DirectionalRender   → ampliar en la dirección de movimiento del player
 *   PredictiveRender    → ampliar donde el player probablemente irá
 *   FocusedRender       → ampliar donde apunta el mouse/cursor
 *
 * ── RETROCOMPATIBILIDAD ───────────────────────────────────────────────────
 * draw(World, GameCamera, Graphics2D, int, int) sigue siendo la firma de
 * llamada principal — WorldManager no necesita cambios. La estrategia por
 * defecto (SYMMETRIC) produce el resultado visual idéntico al anterior.
 *
 * ── SEPARACIÓN World / SceneRenderer ─────────────────────────────────────
 * World contiene el estado del dominio (entidades, física, lógica).
 * SceneRenderer contiene las decisiones de presentación visual.
 *
 * ── Limitación conocida: zoom y rotación ─────────────────────────────────
 * RenderCamera solo captura (x, y). Con zoom ≠ 1 o rotation ≠ 0, los
 * componentes de render que calculen el offset manualmente producirán
 * resultados incorrectos. El path correcto es RenderContext.withCamera(GameCamera).
 * El modo actual (zoom=1, rotation=0) no se ve afectado.
 */
public class SceneRenderer {

    private final RenderSystem            renderSystem      = new RenderSystem();
    private final DepthSortedRenderSystem depthRenderSystem = new DepthSortedRenderSystem();
    private final DebugRenderSystem       debugRenderSystem;

    private boolean useDepthSort = true;

    /** Estrategia de cálculo del área de render. Por defecto: simétrico. */
    private RenderBoundsStrategy renderBoundsStrategy = RenderBoundsStrategy.SYMMETRIC;

    /**
     * @param settings DebugSettings del Engine (no la clase concreta de Debug).
     */
    public SceneRenderer(DebugSettings settings) {
        this.debugRenderSystem = new DebugRenderSystem(settings);
    }

    // ── Configuración ─────────────────────────────────────────────────────

    /**
     * Reemplaza la estrategia de cálculo del área de render.
     *
     * @param strategy la nueva estrategia; null restaura SYMMETRIC.
     */
    public void setRenderBoundsStrategy(RenderBoundsStrategy strategy) {
        this.renderBoundsStrategy = (strategy != null) ? strategy : RenderBoundsStrategy.SYMMETRIC;
    }

    public RenderBoundsStrategy getRenderBoundsStrategy() {
        return renderBoundsStrategy;
    }

    // ── Render ────────────────────────────────────────────────────────────

    /**
     * Dibuja el mundo usando la cámara del Engine.
     *
     * Calcula el RenderBounds usando la estrategia activa y lo propaga
     * a los sistemas de render para culling preciso.
     *
     * @param world          el mundo a dibujar (solo lectura durante render)
     * @param camera         la GameCamera activa del Engine
     * @param g              Graphics2D del framebuffer virtual
     * @param virtualWidth   ancho del framebuffer virtual
     * @param virtualHeight  alto del framebuffer virtual
     */
    public void draw(World world, GameCamera camera, Graphics2D g,
                     int virtualWidth, int virtualHeight) {

        // Calcular el área de render usando la estrategia activa
        RenderBounds bounds = renderBoundsStrategy.compute(camera);

        // Propagar a los sistemas de render para culling
        // Los sistemas usan RenderBounds para determinar visibilidad
        renderSystem.setRenderBounds(bounds, virtualWidth, virtualHeight);
        depthRenderSystem.setRenderBounds(bounds, virtualWidth, virtualHeight);

        RenderCamera renderCamera = new RenderCamera(camera);
        RenderContext ctx = new RenderContext(g, virtualWidth, virtualHeight);

        if (useDepthSort) {
            depthRenderSystem.render(world.getObjectsContainer().getObjects(), ctx, renderCamera);
        } else {
            renderSystem.render(world.getObjectsContainer().getObjects(), ctx, renderCamera);
        }

        debugRenderSystem.render(world.getObjectsContainer().getObjects(), ctx, renderCamera);
    }

    // ── Control de depth sort ─────────────────────────────────────────────

    /**
     * Activa o desactiva el depth sorting.
     * false = render en orden de lista (más rápido, sin 2.5D correcto).
     * true  = depth sorting por Y+Z (Painter's Algorithm, necesario para 2.5D).
     */
    public void setUseDepthSort(boolean useDepthSort) {
        this.useDepthSort = useDepthSort;
    }

    public boolean isUsingDepthSort() { return useDepthSort; }
}
