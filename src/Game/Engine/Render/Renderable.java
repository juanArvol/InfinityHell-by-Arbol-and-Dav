package Game.Engine.Render;

/**
 * Contrato de renderizado — infraestructura del Engine.
 *
 * Cualquier componente que quiera dibujarse implementa esta interfaz.
 * RenderSystem y DepthSortedRenderSystem la usan para iterar componentes
 * sin conocer sus tipos concretos.
 *
 * MIGRADO DESDE: Graficos.Renderable
 * RAZÓN: Renderable es un contrato de infraestructura. Los componentes del
 * Engine (SpriteRenderer, RectRenderer, ShadowComponent) lo implementan.
 * Tenerlo en el paquete Graficos creaba una dependencia Engine → Graficos,
 * paquete que es específico de los assets del Game (sprites, animaciones).
 */
public interface Renderable {
    void render(RenderContext ctx, Camera camera);
}
