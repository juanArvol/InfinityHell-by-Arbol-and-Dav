package Game.Engine.RenderEngine.Contracts;

import Game.Engine.RenderEngine.Context.RenderContext;
import Game.Engine.RenderEngine.Context.RenderCamera;

/**
 * Contrato de renderizado — infraestructura del Engine.
 *
 * Cualquier componente que quiera dibujarse implementa esta interfaz.
 * RenderSystem y DepthSortedRenderSystem la usan para iterar componentes
 * sin conocer sus tipos concretos.
 *
 * ── Limitación conocida: zoom y rotación ────────────────────────────────
 *
 * {@link RenderCamera} solo transporta (x, y). Los implementadores que
 * calculan la posición en pantalla como {@code worldX - camera.getX()} solo
 * compensan traslación. Con zoom ≠ 1 o rotation ≠ 0 el resultado es incorrecto.
 * La corrección completa requiere migrar a
 * {@link RenderContext#withCamera(Game.Engine.Camera.GameCamera)} y es trabajo
 * de una refactorización posterior del sistema de render.
 *
 * ── Historial de paquete ─────────────────────────────────────────────────
 *
 * MIGRADO DESDE: Game.Engine.RenderEngine (raíz del módulo)
 * MOTIVO: reorganización RFC RenderEngine — los contratos del pipeline se
 * agrupan en el subpaquete Contracts para separar claramente las interfaces
 * del contrato de los objetos de trabajo del frame (Context) y del compositor
 * de escena (Scene).
 *
 * MIGRADO ANTES DESDE: Graficos.Renderable
 * RAZÓN ORIGINAL: Renderable es un contrato de infraestructura. Los componentes
 * del Engine (SpriteRenderer, RectRenderer, ShadowComponent) lo implementan.
 * Tenerlo en Graficos creaba una dependencia Engine → Graficos, paquete
 * específico de los assets del Game.
 */
public interface Renderable {
    void render(RenderContext ctx, RenderCamera camera);
}
