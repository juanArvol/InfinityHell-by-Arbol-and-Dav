package Game.Enemys.Core.Controllers;

import Game.Enemys.Core.Contracts.AttackPattern;
import Game.Enemys.Core.Enemy;
import Game.Enemys.Core.AI.EnemyContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Controlador de ataques del Enemy.
 *
 * Enemy nunca implementa lógica de ataque directamente.
 * Este controlador gestiona qué AttackPatterns están activos y los ejecuta
 * cuando sus condiciones se cumplen.
 *
 * ── Modelo de ejecución ──────────────────────────────────────────────────
 * Cada frame, el controlador:
 *   1. Llama update() en cada patrón (avanza cooldowns).
 *   2. Por cada patrón, evalúa canExecute().
 *   3. Si canExecute() → llama execute() y activa el flag de ataque.
 *
 * Los patrones son independientes entre sí: pueden dispararse en el mismo
 * frame o en frames distintos según su cooldown individual.
 *
 * ── Uso en assembler ─────────────────────────────────────────────────────
 *   enemy.getAttackController().addPattern(new MeleeSlamPattern());
 *   enemy.getAttackController().addPattern(new ProjectilePattern());
 *
 * ── Uso en una fase ──────────────────────────────────────────────────────
 *   // Reemplazar todos los patrones al entrar en fase 2:
 *   controller.clearPatterns();
 *   controller.addPattern(new SpreadBulletPattern());
 */
public final class EnemyAttackController {

    private final List<AttackPattern> patterns = new ArrayList<>();

    // ── Gestión de patrones ───────────────────────────────────────────────

    public void addPattern(AttackPattern pattern) {
        patterns.add(pattern);
    }

    public void removePattern(String id) {
        patterns.removeIf(p -> p.id().equals(id));
    }

    public void clearPatterns() {
        patterns.clear();
    }

    public boolean hasPattern(String id) {
        return patterns.stream().anyMatch(p -> p.id().equals(id));
    }

    // ── Update ────────────────────────────────────────────────────────────

    /**
     * Actualiza todos los patrones y ejecuta los que estén listos.
     * Llamado por Enemy.update() cada frame.
     *
     * ── HRFC — Real DeltaTime Authority ──────────────────────────────────
     * Recibe deltaTime para propagar a cada AttackPattern.
     * Los patrones usan deltaTime para cooldowns independientes del framerate.
     *
     * @param enemy el Enemy atacante.
     * @param ctx   contexto del objetivo; puede ser null.
     * @param deltaTime tiempo real del simulation step en segundos
     * @return true si al menos un 
     * patrón se ejecutó este frame.
     */
    public boolean update(Enemy enemy, EnemyContext ctx, double deltaTime) {
        boolean attacked = false;

        for (AttackPattern pattern : patterns) {
            pattern.update(enemy, deltaTime);

            if (ctx != null && pattern.canExecute(enemy, ctx)) {
                pattern.execute(enemy, ctx);
                attacked = true;
            }
        }

        return attacked;
    }

    public List<AttackPattern> getPatterns() {
        return List.copyOf(patterns);
    }
}
