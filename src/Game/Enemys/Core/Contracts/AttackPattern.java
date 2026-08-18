package Game.Enemys.Core.Contracts;

import Game.Enemys.AI.EnemyContext;
import Game.Enemys.Core.Enemy;

/**
 * Patrón de ataque de un enemigo.
 *
 * Un AttackPattern encapsula una forma concreta de atacar: proyectil recto,
 * explosión radial, lluvia, tiro múltiple, melee, etc.
 *
 * ── Desacoplamiento ──────────────────────────────────────────────────────
 * Enemy nunca implementa lógica de ataque directamente.
 * EnemyAttackController administra qué patrones están disponibles y cuándo
 * ejecutarlos. Cada patrón decide internamente su timing, cooldown y forma.
 *
 * ── Reutilización ────────────────────────────────────────────────────────
 * Los patrones son stateless o llevan solo su propio estado de cooldown.
 * El mismo patrón puede reutilizarse en enemigos distintos sin modificación.
 *
 * ── Ciclo de vida ────────────────────────────────────────────────────────
 *   canExecute(enemy, ctx) — ¿está listo este patrón para dispararse?
 *   execute(enemy, ctx)    — ejecuta el ataque; llamado por AttackController.
 *   update(enemy)          — avanza cooldowns/timers internos cada frame.
 */
public interface AttackPattern {

    /**
     * Identificador único del patrón (para logs, debug, fases).
     * Ejemplo: "bullet_spread", "melee_slam", "death_explosion".
     */
    String id();

    /**
     * Avanza el estado interno del patrón (cooldowns, carga, etc.).
     * Llamado cada frame por EnemyAttackController.
     *
     * ── HRFC — Unified DeltaTime Migration ───────────────────────────────
     *
     * CAMBIO: Ahora recibe deltaTime para cooldowns independientes del framerate.
     *
     * @param enemy el Enemy propietario.
     * @param deltaTime tiempo del simulation step en segundos
     */
    void update(Enemy enemy, double deltaTime);

    /**
     * Indica si el patrón puede ejecutarse en el frame actual.
     *
     * @param enemy el Enemy atacante.
     * @param ctx   contexto del objetivo actual.
     * @return true si el ataque puede dispararse.
     */
    boolean canExecute(Enemy enemy, EnemyContext ctx);

    /**
     * Ejecuta el ataque.
     * Solo se llama si canExecute() retornó true.
     *
     * @param enemy el Enemy atacante.
     * @param ctx   contexto del objetivo actual.
     */
    void execute(Enemy enemy, EnemyContext ctx);
}
