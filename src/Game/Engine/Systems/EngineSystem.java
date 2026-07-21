package Game.Engine.Systems;

import Game.Engine.GameObjects;
import java.util.List;

/**
 * Contrato base de todos los sistemas del Engine.
 *
 * ── HRFC-014 — Extensibilidad de sistemas ────────────────────────────────
 *
 * Un System opera sobre una lista de objetos del mundo en cada frame.
 * No tiene estado de objeto — es una operación sin acoplamiento.
 *
 * Sistemas existentes que implementan este contrato:
 *   CollisionsSystem     — física y colisiones
 *   RenderSystem         — render sin depth sort
 *   DepthSortedRenderSystem — render con depth sort (Painter's Algorithm)
 *   StatusEffectSystem   — sincronización de flags derivados (GAP-11)
 *
 * ── Orden de ejecución recomendado en el game loop ────────────────────────
 *
 *   1. object.update()  para cada objeto (actualiza Components, incluyendo
 *      StatusEffectComponent que hace tick + onExpire de efectos)
 *   2. StatusEffectSystem.update()  — proyecta flags derivados
 *   3. CollisionsSystem.update()    — física y colisiones
 *   4. DepthSortedRenderSystem / RenderSystem — render
 *
 * ── Extensión ─────────────────────────────────────────────────────────────
 *
 * Nuevos sistemas se añaden implementando esta interfaz y registrándolos
 * en el game loop. No es necesario modificar ningún sistema existente.
 *
 * Ejemplos de sistemas futuros:
 *   ThermalSystem       — transferencia de calor entre entidades
 *   ForceFieldSystem    — aplicar fuerzas acumuladas desde zonas
 *   AudioSystem         — disparar sonidos según eventos
 *   ParticleSystem      — gestionar efectos de partículas
 */
public interface EngineSystem {

    /**
     * Ejecuta el sistema sobre la lista de objetos del mundo.
     *
     * @param objects lista de todos los GameObjects activos en el mundo.
     *                No modificar la lista desde dentro del sistema.
     */
    void update(List<GameObjects> objects);
}
