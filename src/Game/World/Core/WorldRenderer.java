package Game.World.Core;

import Game.Engine.Systems.DebugRenderSystem;
import Game.Engine.Systems.DepthSortedRenderSystem;
import Game.Engine.Systems.RenderSystem;
import Game.UI.POV.Camera;
import Game.UI.POV.RenderContext;
import Main.Debug.DebugGameSettings;

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
 * USO:
 *   WorldRenderer renderer = new WorldRenderer(gameSettings);
 *   renderer.draw(world, g);  // llamado por WorldManager o GameState
 *
 * ESTRATEGIA DE RENDER:
 *   Por defecto usa DepthSortedRenderSystem (Painter's Algorithm para 2.5D).
 *   El flag useDepthSort permite alternar en runtime si fuera necesario.
 *   En el futuro, esto podría convertirse en una interfaz IRenderStrategy
 *   inyectada por constructor, pero por ahora no hay suficientes variantes
 *   para justificar esa abstracción extra.
 */
public class WorldRenderer {

    private final RenderSystem            renderSystem      = new RenderSystem();
    private final DepthSortedRenderSystem depthRenderSystem = new DepthSortedRenderSystem();
    private final DebugRenderSystem       debugRenderSystem;

    private boolean useDepthSort = true;

    /**
     * Constructor que recibe GameSettings por inyección de dependencia.
     *
     * Justificación: DebugRenderSystem necesita saber si el debug está activo.
     * En lugar de llamar GameSettings.getInstance() desde DebugRenderSystem
     * (dependencia estática invisible), recibimos GameSettings aquí y lo pasamos.
     * Esto hace la dependencia explícita y testeable.
     */
    public WorldRenderer(DebugGameSettings settings) {
        this.debugRenderSystem = new DebugRenderSystem(settings);
    }

    /**
     * Dibuja el mundo actual en el Graphics2D del framebuffer virtual.
     *
     * @param world  el mundo a dibujar (solo lectura durante render)
     * @param g      Graphics2D del framebuffer virtual (de DisplayManager.beginFrame())
     */
    public void draw(World world, Graphics2D g) {
        Camera camera = world.getCamera();
        RenderContext ctx = new RenderContext(g, world.getWidth(), world.getHeight());

        if (useDepthSort) {
            depthRenderSystem.render(world.getObjectsContainer().getObjects(), ctx, camera);
        } else {
            renderSystem.render(world.getObjectsContainer().getObjects(), ctx, camera);
        }

        debugRenderSystem.render(world.getObjectsContainer().getObjects(), ctx, camera);
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
