package Game.Enemys.AI;

import GameMath.Vector2D;

/**
 * Contexto de IA — abstracción del "objetivo" que persigue/ataca el enemigo.
 *
 * ── POR QUÉ EXISTE ───────────────────────────────────────────────────────
 * El sistema original pasaba `Player player` directamente a EnemyAI.update(),
 * EnemyComport.decideAction(), y constructores de Behaviors (FlyingBehavior).
 *
 * Eso significa:
 *   - Los comportamientos no se pueden testear sin un Player real.
 *   - Un enemigo no puede perseguir otro enemigo (boss → minion).
 *   - EnemyComport queda acoplado a Player, no a "algo que tiene posición".
 *
 * Con EnemyContext:
 *   - FlyingBehavior recibe EnemyContext, no Player.
 *   - AggressiveBehavior lee ctx.getPosition(), no player.getPosition().
 *   - Para un Player: EnemyContext.of(player).
 *   - Para un punto fijo de patrulla: EnemyContext.fixed(x, y).
 *   - Para otro Enemy (boss target): EnemyContext.of(otherEnemy).
 *
 * Retro-compatible: EnemyContext.of(player) produce el mismo resultado
 * que el código anterior que usaba player directamente.
 */
public final class EnemyContext {

    /** Posición actual del objetivo (llamada cada frame, puede cambiar). */
    private final java.util.function.Supplier<Vector2D> positionSupplier;

    /** Centro del objetivo (para steering). Puede ser mismo que posición. */
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

    /** Objetivo: otro Enemy (boss persiguiendo a un minion, por ejemplo). */
    public static EnemyContext of(Game.Enemys.Enemy target) {
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
