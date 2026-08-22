package Game.Enemys.Core.AI;

import Game.Engine.GameMath.Logic2D.Vector2D;

/**
 * Contexto de IA — abstracción del "objetivo" que persigue/ataca el enemigo.
 *
 * ── HRFC-005 ─────────────────────────────────────────────────────────────
 * Actualizado para referenciar Game.Enemys.Core.Enemy en el factory method
 * of(Enemy target). El contrato y la lógica son idénticos.
 *
 * ── POR QUÉ EXISTE ───────────────────────────────────────────────────────
 * Desacopla los comportamientos de IA de tipos concretos (Player, Enemy).
 * FlyingBehavior, AggressiveBehavior, etc. leen ctx.getPosition() sin
 * saber si el objetivo es un Player, otro Enemy o un punto fijo.
 *
 * ── Factory methods ──────────────────────────────────────────────────────
 *   EnemyContext.of(player)         — objetivo: jugador.
 *   EnemyContext.of(enemy)          — objetivo: otro Enemy (boss → minion).
 *   EnemyContext.fixed(x, y)        — objetivo: posición estática (patrulla).
 *   EnemyContext.dynamic(supplier)  — objetivo: posición dinámica arbitraria.
 */
public final class EnemyContext {

    private final java.util.function.Supplier<Vector2D> positionSupplier;
    private final java.util.function.Supplier<Vector2D> centerSupplier;

    private EnemyContext(
            java.util.function.Supplier<Vector2D> posSupplier,
            java.util.function.Supplier<Vector2D> centerSupplier) {
        this.positionSupplier = posSupplier;
        this.centerSupplier   = centerSupplier;
    }

    // ── Factory methods ───────────────────────────────────────────────────

    /** Objetivo: un Player. */
    public static EnemyContext of(Game.Player.Player player) {
        return new EnemyContext(
            player::getPosition,
            player::getCenter
        );
    }

    /** Objetivo: otro Enemy del framework Core. */
    public static EnemyContext of(Game.Enemys.Core.Enemy target) {
        return new EnemyContext(
            () -> target.getTransform().getPosition(),
            target::getCenter
        );
    }

    /** Objetivo: posición fija (patrulla). */
    public static EnemyContext fixed(double x, double y) {
        Vector2D pos = new Vector2D(x, y);
        return new EnemyContext(() -> pos, () -> pos);
    }

    /** Objetivo: posición dinámica (lambda / supplier externo). */
    public static EnemyContext dynamic(java.util.function.Supplier<Vector2D> posSupplier) {
        return new EnemyContext(posSupplier, posSupplier);
    }

    // ── API ───────────────────────────────────────────────────────────────

    public Vector2D getPosition() { return positionSupplier.get(); }
    public Vector2D getCenter()   { return centerSupplier.get(); }
}
