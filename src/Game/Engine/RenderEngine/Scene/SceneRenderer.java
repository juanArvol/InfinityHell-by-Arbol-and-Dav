package Game.Engine.RenderEngine.Scene;

import Game.Engine.Camera.GameCamera;
import Game.Engine.RenderEngine.Context.RenderCamera;
import Game.Engine.RenderEngine.Context.RenderContext;
import Game.Engine.Systems.DebugRenderSystem;
import Game.Engine.Systems.DebugSettings;
import Game.Engine.Systems.DepthSortedRenderSystem;
import Game.Engine.Systems.RenderSystem;
import Game.World.Core.World;
import java.awt.Graphics2D;

/**
 * Compositor de la escena del mundo — infraestructura de render del Engine.
 *
 * ── Responsabilidad ───────────────────────────────────────────────────────
 *
 * SceneRenderer construye la representación visual del mundo antes de que
 * el Display la presente en pantalla. Su responsabilidad es exactamente ésa:
 * transformar el estado del mundo (entidades, posiciones, componentes visuales)
 * en píxeles sobre el framebuffer virtual.
 *
 * No pertenece al dominio del mundo (World no sabe cómo se dibuja).
 * No pertenece al Display (Display no sabe qué hay en el mundo).
 * Pertenece al RenderEngine como compositor de la capa de escena del juego.
 *
 * ── Por qué no se llama WorldRenderer ────────────────────────────────────
 *
 * WorldRenderer era un nombre que acoplaba conceptualmente el compositor al
 * dominio del mundo. SceneRenderer nombra la responsabilidad real: renderizar
 * la escena, sin asumir que la escena está necesariamente compuesta por un
 * único mundo. En el futuro, la misma clase puede renderizar escenas de
 * menú, cinemáticas o cualquier composición de entidades sin cambiar de nombre.
 *
 * ── Separación World / SceneRenderer ─────────────────────────────────────
 *
 * World contiene el estado del dominio (entidades, física, lógica).
 * SceneRenderer contiene las decisiones de presentación visual.
 *
 * Esta separación permite:
 *   - Testear World sin contexto gráfico (sin Graphics2D, sin AWT).
 *   - Cambiar el modo de render (flat, depth-sorted, isométrico) sin tocar World.
 *   - Usar World en un servidor headless sin dependencias de Display.
 *   - Agregar efectos post-proceso en SceneRenderer sin contaminar el dominio.
 *
 * ── Flujo de cámara ───────────────────────────────────────────────────────
 *
 * SceneRenderer recibe la GameCamera del Engine (autoritativa) y las
 * dimensiones virtuales del Display. A partir de ellas crea un RenderCamera
 * — snapshot (x, y) de solo lectura — para los sistemas de render existentes.
 *
 *   RenderCamera renderCamera = new RenderCamera(camera);
 *   renderSystem.render(objects, ctx, renderCamera);
 *
 * Los sistemas de render pasan RenderCamera a cada componente. Los componentes
 * calculan la posición en pantalla como {@code worldX - camera.getX()}.
 *
 * ── Limitación conocida: zoom y rotación ─────────────────────────────────
 *
 * RenderCamera solo captura (x, y). Si GameCamera tiene zoom ≠ 1 o
 * rotation ≠ 0, los componentes producirán posiciones incorrectas porque
 * solo compensan traslación. El path correcto para transformaciones completas
 * es {@link RenderContext#withCamera(GameCamera)}, que aplica getViewTransform().
 *
 * Mientras los componentes de render calculen el offset manualmente, esta
 * limitación persiste. La corrección requiere cambiar las firmas de
 * {@link Game.Engine.RenderEngine.Contracts.Renderable} y
 * {@link Game.Engine.RenderEngine.Contracts.DebugRenderable} y es trabajo de
 * una refactorización posterior del sistema de render.
 *
 * El modo de operación actual (zoom=1, rotation=0) no se ve afectado.
 *
 * ── Dimensiones virtuales vs. dimensiones del mundo ───────────────────────
 *
 * RenderContext recibe las dimensiones VIRTUALES del Display, no las del mundo.
 * Las dimensiones virtuales definen el tamaño del framebuffer (e.g. 1280×720).
 * Las dimensiones del mundo son el espacio lógico del juego (e.g. 1280×1280).
 * Pueden ser distintas. RenderContext las usa para centrar las transformaciones
 * de zoom y rotación; necesita las virtuales para que ese cálculo sea correcto.
 *
 * ── Estrategia de render ──────────────────────────────────────────────────
 *
 * Por defecto usa DepthSortedRenderSystem (Painter's Algorithm para 2.5D).
 * El flag useDepthSort permite alternar en runtime si fuera necesario.
 *
 * ── Historial de clase ────────────────────────────────────────────────────
 *
 * RENOMBRADO DESDE: Game.World.Core.WorldRenderer
 * MOTIVO: reorganización RFC RenderEngine — el compositor de escena es
 * responsabilidad del RenderEngine, no del dominio del mundo. El nuevo
 * nombre describe la responsabilidad real (renderizar la escena) sin
 * acoplarse al concepto de mundo.
 */
public class SceneRenderer {

    private final RenderSystem            renderSystem      = new RenderSystem();
    private final DepthSortedRenderSystem depthRenderSystem = new DepthSortedRenderSystem();
    private final DebugRenderSystem       debugRenderSystem;

    private boolean useDepthSort = true;

    /**
     * Recibe la interfaz DebugSettings del Engine, no la clase concreta de Main.Debug.
     * Cualquier implementación de DebugSettings puede inyectarse (producción, mock de test).
     */
    public SceneRenderer(DebugSettings settings) {
        this.debugRenderSystem = new DebugRenderSystem(settings);
    }

    /**
     * Dibuja el mundo usando la cámara del Engine.
     *
     * @param world          el mundo a dibujar (solo lectura durante render)
     * @param camera         la GameCamera activa del Engine
     * @param g              Graphics2D del framebuffer virtual
     * @param virtualWidth   ancho del framebuffer virtual (Display.virtualWidth)
     * @param virtualHeight  alto del framebuffer virtual (Display.virtualHeight)
     */
    public void draw(World world, GameCamera camera, Graphics2D g,
                     int virtualWidth, int virtualHeight) {
        // Propagar dimensiones virtuales a los sistemas de render para culling.
        renderSystem.setVirtualSize(virtualWidth, virtualHeight);
        depthRenderSystem.setVirtualSize(virtualWidth, virtualHeight);

        RenderCamera renderCamera = new RenderCamera(camera);
        RenderContext ctx = new RenderContext(g, virtualWidth, virtualHeight);

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
