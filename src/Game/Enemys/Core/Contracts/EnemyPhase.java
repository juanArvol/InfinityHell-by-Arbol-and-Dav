package Game.Enemys.Core.Contracts;

import Game.Enemys.Core.Enemy;

/**
 * Fase de un enemigo.
 *
 * Una EnemyPhase NO es un entero. Es un estado completo del enemigo:
 * una configuración integral que define cómo se comporta en un momento dado.
 *
 * ── Qué puede modificar una fase ────────────────────────────────────────
 * Al activarse, una fase puede reemplazar dinámicamente:
 *   - La estrategia de movimiento (GroundMovement → FlyingMovement).
 *   - Los patrones de ataque disponibles.
 *   - El comportamiento de IA (AggressiveBehavior → BerserkBehavior).
 *   - Las variables del enemy (velocidad, daño, defensa).
 *   - Los componentes activos (aura ON, regeneración OFF).
 *   - Las resistencias y efectos.
 *
 * ── Responsabilidad del EnemyPhaseController ────────────────────────────
 * EnemyPhaseController decide CUÁNDO transicionar entre fases.
 * EnemyPhase decide QUÉ cambia cuando la fase se activa.
 *
 * ── Ciclo de vida ────────────────────────────────────────────────────────
 *   onEnter(enemy)  — se activa esta fase; configurar el Enemy.
 *   update(enemy)   — actualización de la fase (evaluar condición de salida).
 *   onExit(enemy)   — se abandona esta fase; limpieza si es necesario.
 *
 * ── Ejemplo de uso ───────────────────────────────────────────────────────
 *
 *   public class Phase2 implements EnemyPhase {
 *       public void onEnter(Enemy enemy) {
 *           // Más veloz, vuela, ataque distinto
 *           enemy.getMovementController().setStrategy(new FlyingMovement());
 *           enemy.getAttackController().clearPatterns();
 *           enemy.getAttackController().addPattern(new SpreadBulletPattern());
 *           enemy.getStats().setSpeed(5.0);   // velocidad vive en EntityStats
 *       }
 *       ...
 *   }
 */
public interface EnemyPhase {

    /**
     * Identificador único de la fase (para logs y condiciones).
     * Ejemplo: "phase_1", "phase_2", "rage_mode".
     */
    String id();

    /**
     * Llamado cuando el EnemyPhaseController activa esta fase.
     * Aquí se reconfigura el Enemy: movimiento, ataques, IA, variables, etc.
     *
     * @param enemy el Enemy que entra en esta fase.
     */
    void onEnter(Enemy enemy);

    /**
     * Actualización por frame mientras esta fase esté activa.
     * Usar para lógica interna de la fase (ej: timer de duración).
     *
     * @param enemy el Enemy en esta fase.
     */
    default void update(Enemy enemy) {}

    /**
     * Llamado cuando el EnemyPhaseController desactiva esta fase.
     * Usar para limpieza de efectos temporales o suscripciones.
     *
     * @param enemy el Enemy que abandona esta fase.
     */
    default void onExit(Enemy enemy) {}
}
