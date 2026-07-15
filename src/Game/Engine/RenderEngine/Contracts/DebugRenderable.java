package Game.Engine.RenderEngine.Contracts;

import Game.Engine.RenderEngine.Context.RenderContext;
import Game.Engine.RenderEngine.Context.RenderCamera;

/**
 * Contrato de renderizado de debug — infraestructura del Engine.
 *
 * Los componentes que quieren dibujarse en modo debug (hitboxes, gizmos)
 * implementan esta interfaz. DebugRenderSystem la usa para iterar sin
 * conocer tipos concretos.
 *
 * ── RenderCamera como parámetro explícito ────────────────────────────────
 *
 * La firma incluye {@link RenderCamera} para que cada implementador pueda
 * aplicar el offset de cámara y que hitboxes, gizmos y helpers visuales
 * aparezcan alineados con los objetos que representan.
 *
 * ── Limitación conocida: zoom y rotación ─────────────────────────────────
 *
 * Misma limitación que {@link Renderable}: RenderCamera solo transporta (x, y).
 * Con zoom ≠ 1 o rotation ≠ 0 el offset aplicado es incorrecto.
 * Ver {@link RenderCamera} para el detalle completo.
 *
 * ── Historial de paquete ──────────────────────────────────────────────────
 *
 * MIGRADO DESDE: Game.Engine.RenderEngine (raíz del módulo)
 * MOTIVO: reorganización RFC RenderEngine — los contratos del pipeline se
 * agrupan en el subpaquete Contracts.
 *
 * MIGRADO ANTES DESDE: Game.UI.POV.DebugRenderable
 * RAZÓN ORIGINAL: DebugRenderable es un contrato de infraestructura usado por
 * HitBoxComponent (Engine.Components.Visuals) y DebugRenderSystem (Engine.Systems).
 * Tenerlo en Game.UI creaba una dependencia Engine → Game.UI.
 */
public interface DebugRenderable {
    void debugRender(RenderContext ctx, RenderCamera camera);
}
