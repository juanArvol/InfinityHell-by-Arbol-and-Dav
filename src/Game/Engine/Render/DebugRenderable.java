package Game.Engine.Render;

/**
 * Contrato de renderizado de debug — infraestructura del Engine.
 *
 * Los componentes que quieren dibujarse en modo debug (hitboxes, gizmos)
 * implementan esta interfaz. DebugRenderSystem la usa para iterar sin
 * conocer tipos concretos.
 *
 * ── CORRECCIÓN: Camera como parámetro explícito ──────────────────────────
 * La firma anterior era debugRender(RenderContext ctx) sin cámara.
 * Esto obligaba a los implementadores a trabajar en coordenadas de mundo
 * absolutas, produciendo hitboxes desplazadas respecto a los sprites.
 *
 * La firma correcta es igual que Renderable.render(ctx, camera):
 *   void debugRender(RenderContext ctx, Camera camera)
 *
 * Cada implementador aplica el camera offset que necesite, con la misma
 * convención que el sistema de render principal. Esto garantiza que
 * hitboxes, gizmos y cualquier helper visual siempre aparezcan alineados
 * con los objetos que representan.
 *
 * MIGRADO DESDE: Game.UI.POV.DebugRenderable
 * RAZÓN: DebugRenderable es un contrato de infraestructura usado por
 * HitBoxComponent (Engine.Components.Visuals) y DebugRenderSystem (Engine.Systems).
 * Tenerlo en Game.UI creaba una dependencia Engine → Game.UI.
 */
public interface DebugRenderable {
    void debugRender(RenderContext ctx, Camera camera);
}
