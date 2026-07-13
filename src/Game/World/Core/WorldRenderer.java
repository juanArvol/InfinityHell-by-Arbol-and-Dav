package Game.World.Core;

import Game.Engine.Camera.GameCamera;
import Game.Engine.Render.Camera;
import Game.Engine.Render.RenderContext;
import Game.Engine.Systems.DebugRenderSystem;
import Game.Engine.Systems.DebugSettings;
import Game.Engine.Systems.DepthSortedRenderSystem;
import Game.Engine.Systems.RenderSystem;
import java.awt.Graphics2D;

/**
 * Renderizador del mundo — separado de World para cumplir SRP.
 *
 * RAZÓN DE EXISTENCIA:
 *   World contiene el estado del dominio (entidades, física, lógica).
 *   WorldRenderer contiene las decisiones de presentación visual.
 *
 *   Esta separación permite:
 *   - Testear World sin contexto gráfico (sin Graphics2D, sin AWT).
 *   - Cambiar el modo de render (flat, depth-sorted, isométrico) sin tocar World.
 *   - Usar World en un servidor headless sin dependencias de Display.
 *   - Agregar efectos post-proceso en WorldRenderer sin contaminar el dominio.
 *
 * ── HRFC-001: WorldRenderer recibe GameCamera externamente ───────────────
 *
 * La cámara ya no se extrae de World.getCamera(). World no tiene cámara.
 * WorldRenderer recibe la GameCamera activa del Engine en draw() y crea
 * un adaptador Camera para pasarlo a los sistemas de render existentes
 * (que aún usan la API Camera por compatibilidad).
 *
 * Esto garantiza:
 *   - World es estado puro sin dependencias de render.
 *   - La cámara es un servicio del Engine, no del World.
 *   - Los sistemas de render existentes no necesitan cambios.
 *
 * USO:
 *   WorldRenderer renderer = new WorldRenderer(gameSettings);
 *   renderer.draw(world, camera, g);
 *
 * ESTRATEGIA DE RENDER:
 *   Por defecto usa DepthSortedRenderSystem (Painter's Algorithm para 2.5D).
 *   El flag useDepthSort permite alternar en runtime si fuera necesario.
 */
public class WorldRenderer {

    private final RenderSystem            renderSystem      = new RenderSystem();
    private final DepthSortedRenderSystem depthRenderSystem = new DepthSortedRenderSystem();
    private final DebugRenderSystem       debugRenderSystem;

    private boolean useDepthSort = true;

    /**
     * Recibe la interfaz DebugSettings del Engine, no la clase concreta de Main.Debug.
     * Cualquier implementación de DebugSettings puede inyectarse (producción, mock de test).
     */
    public WorldRenderer(DebugSettings settings) {
        this.debugRenderSystem = new DebugRenderSystem(settings);
    }

    /**
     * Dibuja el mundo usando la cámara del Engine.
     *
     * La GameCamera proviene del Engine (GameState / WorldManager la gestiona).
     * Se crea un adaptador Camera de solo lectura para los sistemas de render
     * existentes, que aún usan la API Camera.
     *
     * @param world   el mundo a dibujar (solo lectura durante render)
     * @param camera  la cámara activa del Engine
     * @param g       Graphics2D del framebuffer virtual
     */
    public void draw(World world, GameCamera camera, Graphics2D g) {
        // Crear adaptador de solo lectura: captura el estado actual de la cámara.
        // Los sistemas de render existentes (SpriteRenderer, etc.) usan Camera.getX/Y().
        // Con zoom=1 y rotation=0, el resultado es idéntico al anterior.
        // Con zoom≠1 o rotation≠0, el viewTransform aplicado en RenderContext.withCamera(GameCamera)
        // gestionará la transformación completa.
        Camera renderCamera = new Camera(camera);

        RenderContext ctx = new RenderContext(g, world.getWidth(), world.getHeight());

        if (useDepthSort) {
            depthRenderSystem.render(world.getObjectsContainer().getObjects(), ctx, renderCamera);
        } else {
            renderSystem.render(world.getObjectsContainer().getObjects(), ctx, renderCamera);
        }

        debugRenderSystem.render(world.getObjectsContainer().getObjects(), ctx, renderCamera);
    }

    /**
     * Activa o desactiva el depth sorting.
     * false = render en orden de lista (más rápido, sin 2.5D correcto).
     * true  = depth sorting por Y+Z (Painter's Algorithm, necesario para 2.5D).
     */
    public void setUseDepthSort(boolean useDepthSort) {
        this.useDepthSort = useDepthSort;
    }

    public boolean isUsingDepthSort() {
        return useDepthSort;
    }
}
