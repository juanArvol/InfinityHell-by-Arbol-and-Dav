package Game.World.Systems;

import Game.Enemys.Core.AI.EnemyContext;
import Game.Engine.ContextualUpdatable;
import Game.Engine.GameObjects;
import Game.Player.Player;
import java.util.List;

/**
 * Sistema explícito de IA — ejecuta comportamientos de entidades inteligentes.
 *
 * ── RESPONSABILIDAD ──────────────────────────────────────────────────────
 * AISystem identifica entidades con comportamiento de IA dentro de la
 * SimulationRegion y ejecuta su ciclo de decisión/acción.
 *
 * NO es un God Class:
 *   - No contiene lógica de IA específica de cada tipo de entidad
 *   - No conoce detalles internos de Enemy, NPC, etc.
 *   - Delega a los componentes/controllers de cada entidad
 *
 * ── CONTRATO ─────────────────────────────────────────────────────────────
 * Las entidades con IA implementan {@link ContextualUpdatable}:
 *   - Enemy implementa ContextualUpdatable (recibe EnemyContext)
 *   - Futuros NPCs implementarán ContextualUpdatable (reciben NPCContext)
 *   - Futuros Companions implementarán ContextualUpdatable
 *
 * El contexto específico (EnemyContext, NPCContext) es responsabilidad
 * de cada entidad filtrar. AISystem provee el contexto general del mundo.
 *
 * ── ORDEN DE EJECUCIÓN EN WorldManager ───────────────────────────────────
 *   1. globalDynamicRegistry.flush()
 *   2. SimulationRegion.rebuild()
 *   3. AISystem.update()              ← AQUÍ (decisión/comportamiento)
 *   4. StatusEffectSystem.update()
 *   5. CollisionsSystem.update()      (física/movimiento)
 *
 * La IA decide QUÉ hacer (mover, atacar, esperar) ANTES de que la física
 * integre el movimiento. Esto permite que las decisiones de IA del frame
 * actual afecten la física del mismo frame.
 *
 * ── EXTENSIBILIDAD ───────────────────────────────────────────────────────
 * Para añadir nuevos tipos de entidades inteligentes:
 *
 * 1. La entidad implementa ContextualUpdatable:
 *      public class NPC extends MovingObjects implements ContextualUpdatable {
 *          @Override
 *          public void updateWithContext(Object context, double deltaTime) {
 *              // Lógica de NPC
 *          }
 *      }
 *
 * 2. AISystem automáticamente la detecta y actualiza.
 *
 * 3. No se modifica AISystem, WorldManager ni ningún otro sistema.
 *
 * ── CONTEXTO ACTUAL ──────────────────────────────────────────────────────
 * Actualmente, AISystem provee EnemyContext (posición del player) porque
 * el juego solo tiene enemigos como entidades con IA.
 *
 * Cuando se agreguen NPCs, companions, etc., cada uno filtrará el contexto
 * que necesita dentro de su updateWithContext():
 *
 *   Enemy: if (context instanceof EnemyContext ctx) { ... }
 *   NPC:   if (context instanceof NPCContext ctx) { ... }
 *
 * O bien AISystem puede proveer un contexto compuesto que contenga múltiples
 * fuentes de información (player, quest state, world events).
 */
public final class AISystem {

    /**
     * Actualiza todas las entidades con comportamiento de IA dentro de la región activa.
     * Recibe deltaTime real del simulation step y lo propaga a cada entidad.
     *
     * @param activeObjects lista de objetos activos de SimulationRegion
     * @param player        el jugador actual (puede ser null si no hay player en escena)
     * @param deltaTime     tiempo real del simulation step en segundos
     */
    public void update(List<GameObjects> activeObjects, Player player, double deltaTime) {
        // Construir contexto de IA
        // Actualmente solo EnemyContext (posición del player)
        // Futuro: contexto compuesto con múltiples fuentes de información
        Object aiContext = (player != null) ? EnemyContext.of(player) : null;

        // Ejecutar IA de todas las entidades que implementan ContextualUpdatable
        for (GameObjects obj : activeObjects) {
            if (obj instanceof ContextualUpdatable cu) {
                cu.updateWithContext(aiContext, deltaTime);
            } else {
                // Entidades sin IA (terreno, decoración, items) se actualizan normalmente
                // Esto permite que sus componentes (animaciones, efectos) sigan funcionando
                obj.update(deltaTime);
            }
        }
    }

    /**
     * Actualiza solo las entidades con IA (optimización futura).
     *
     * Esta variante permite que el caller filtre previamente las entidades
     * sin IA para evitar el instanceof check en cada frame.
     *
     * Actualmente no se usa, pero queda disponible para optimización cuando
     * SimulationRegion pueda separar entidades con IA de decoración estática.
     *
     * @param aiEntities entidades pre-filtradas que implementan ContextualUpdatable
     * @param player     el jugador actual (puede ser null)
     * @param deltaTime  tiempo real del simulation step en segundos
     */
    public void updateAIOnly(List<ContextualUpdatable> aiEntities, Player player, double deltaTime) {
        Object aiContext = (player != null) ? EnemyContext.of(player) : null;

        for (ContextualUpdatable entity : aiEntities) {
            entity.updateWithContext(aiContext, deltaTime);
        }
    }
}
