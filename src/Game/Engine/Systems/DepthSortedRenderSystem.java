package Game.Engine.Systems;

import Game.Engine.Component;
import Game.Engine.Entity.Components.Visuals.SpriteRendererComponent;
import Game.Engine.Entity.Components.Visuals.SpriteSkeletonComponent;
import Game.Engine.GameMath.Logic3D.Transform3D;
import Game.Engine.GameObjects;
import Game.Engine.RenderEngine.Context.RenderCamera;
import Game.Engine.RenderEngine.Context.RenderContext;
import Game.Engine.RenderEngine.Contracts.Renderable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Sistema de render con ordenamiento por profundidad (Painter's Algorithm).
 *
 * ── HRFC-003: CULLING DE VIEWPORT ─────────────────────────────────────────
 * Propaga virtualWidth/Height a todos los SpriteRenderer y SpriteComposite
 * antes del render para que ViewportCuller tenga las dimensiones correctas
 * y descarte automáticamente los sprites fuera de pantalla.
 *
 * setVirtualSize() se llama desde SceneRenderer en cada frame con las
 * dimensiones actuales del framebuffer, de modo que un cambio de resolución
 * virtual (DisplayCommand.ChangeResolution) se propaga correctamente.
 *
 * ── PAINTER'S ALGORITHM 2.5D ──────────────────────────────────────────────
 * 1. Ordena objetos por depthValue = Y + Z*0.5.
 * 2. Objetos con mayor valor se dibujan después → aparecen encima.
 * 3. Si no hay Transform3D, usa solo Y (retro-compatible con 2D puro).
 *
 * ── STATELESS / REENTRANTE ────────────────────────────────────────────────
 * Buffer de ordenación local por frame — seguro para renders secundarios
 * (reflejo, screenshot, preview) sin riesgo de corrupción concurrente.
 */
public class DepthSortedRenderSystem {

    /**
     * Dimensiones del framebuffer virtual para culling.
     * Actualizadas desde SceneRenderer en cada draw().
     */
    private int virtualWidth  = 1280;
    private int virtualHeight = 720;

    public void setVirtualSize(int vw, int vh) {
        this.virtualWidth  = vw;
        this.virtualHeight = vh;
    }

    /**
     * Renderiza los objetos ordenados por profundidad Y+Z.
     *
     * @param objects lista de todos los objetos del mundo (no se modifica)
     * @param ctx     contexto de render
     * @param camera  cámara actual
     */
    public void render(List<GameObjects> objects,
                       RenderContext ctx,
                       RenderCamera camera) {

        List<GameObjects> sortBuffer = new ArrayList<>(objects);
        sortBuffer.sort(Comparator.comparingDouble(this::getDepthValue));

        for (GameObjects obj : sortBuffer) {
            for (Component c : obj.getComponents()) {
                // Propagar virtual size a componentes que hacen culling
                if (c instanceof SpriteRendererComponent sr) {
                    sr.setVirtualSize(virtualWidth, virtualHeight);
                } else if (c instanceof SpriteSkeletonComponent sc) {
                    sc.setVirtualSize(virtualWidth, virtualHeight);
                }
                if (c instanceof Renderable renderable) {
                    renderable.render(ctx, camera);
                }
            }
        }
    }

    /**
     * Valor de profundidad: Y + Z*0.5 si Transform3D, solo Y si Transform2D.
     */
    private double getDepthValue(GameObjects obj) {
        if (obj.getTransform() instanceof Transform3D t3d) {
            return t3d.getDepthSortValue();
        }
        return obj.getTransform().getY();
    }
}
