package Game.Enemys.Core.Contracts;

import Game.Enemys.AI.EnemyContext;
import Game.Enemys.Core.Enemy;

/**
 * Estrategia de movimiento de un enemigo.
 *
 * Cada MovementStrategy encapsula un tipo concreto de desplazamiento:
 * caminar, volar, teletransportarse, orbitar, escapar, perseguir, etc.
 *
 * ── Desacoplamiento ──────────────────────────────────────────────────────
 * Enemy nunca sabe cómo se mueve — delega 100% en esta estrategia.
 * La estrategia activa puede reemplazarse en runtime (ej: una fase
 * transiciona de caminar a volar sin reconstruir el Enemy).
 *
 * ── Diferencia con EnemyComport ─────────────────────────────────────────
 * EnemyComport decide QUÉ acción ejecutar (la lógica de IA).
 * MovementStrategy implementa CÓMO se mueve físicamente (la mecánica).
 *
 * Ambos colaboran: EnemyComport puede elegir qué MovementStrategy activar.
 *
 * ── Ejemplos ─────────────────────────────────────────────────────────────
 *   GroundMovement     — camina por el suelo con gravedad.
 *   FlyingMovement     — steering suave sin gravedad.
 *   TeleportMovement   — aparece en punto aleatorio cerca del objetivo.
 *   DashMovement       — impulso brusco en dirección al objetivo.
 *   OrbitMovement      — orbita alrededor de un punto fijo.
 */
public interface MovementStrategy {

    /**
     * Aplica el movimiento al enemigo para el frame actual.
     *
     * ── HRFC — Real DeltaTime Authority ──────────────────────────────────
     * Recibe deltaTime para movimientos temporales correctos.
     * La mayoría de estrategias modifican velocidad (que se integra en
     * CollisionsSystem con deltaTime), pero estrategias especiales como
     * teletransporte o dash pueden necesitar timing explícito.
     *
     * @param enemy el Enemy a mover.
     * @param ctx   contexto del objetivo actual (posición del player, etc.).
     *              Puede ser null si el Enemy no tiene objetivo activo.
     * @param deltaTime tiempo real del simulation step en segundos
     */
    void move(Enemy enemy, EnemyContext ctx, double deltaTime);

    /**
     * Inicialización de la estrategia al ser asignada.
     * Llamado por EnemyMovementController.setStrategy().
     * Usar para configurar el estado inicial de física, flags, etc.
     *
     * @param enemy el Enemy al que se asigna esta estrategia.
     */
    default void onActivate(Enemy enemy) {}

    /**
     * Limpieza al ser reemplazada por otra estrategia.
     * Llamado por EnemyMovementController.setStrategy() antes de activar
     * la nueva.
     *
     * @param enemy el Enemy que abandona esta estrategia.
     */
    default void onDeactivate(Enemy enemy) {}
}
