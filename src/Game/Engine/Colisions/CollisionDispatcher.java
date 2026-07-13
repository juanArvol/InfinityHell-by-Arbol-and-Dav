package Game.Engine.Colisions;

import Game.Engine.GameObjects;

/**
 * Despacha la colisión a ambos objetos del par.
 *
 * ── REFACTOR: sin imports de Game.* ──────────────────────────────────────
 *
 * PROBLEMA ORIGINAL:
 *   CollisionDispatcher detectaba el tipo concreto de cada objeto (Player,
 *   Enemy, Bullet, BlockWorld, Obstacle) con instanceof para llamar la
 *   sobrecarga tipada correcta de GameObjects. Eso requería importar 5 tipos
 *   del Game en el corazón del Engine — dependencia inversa.
 *
 * SOLUCIÓN:
 *   GameObjects ahora tiene un único método genérico:
 *     onCollisionWith(GameObjects other)
 *
 *   CollisionDispatcher simplemente llama ese método en ambas direcciones.
 *   La distinción de tipos, cuando es necesaria, la hace cada subclase con
 *   instanceof en su propia implementación de onCollisionWith().
 *
 *   El Engine no sabe nada de Player, Enemy, Bullet ni de ningún tipo del Game.
 *
 * ── Funcionamiento ───────────────────────────────────────────────────────
 *   dispatch(A, B):
 *     A.onCollisionWith(B)   — A reacciona al tipo de B
 *     B.onCollisionWith(A)   — B reacciona al tipo de A
 */
public final class CollisionDispatcher {

    private CollisionDispatcher() {}

    /**
     * Notifica la colisión a ambos objetos.
     * Cada objeto decide cómo reaccionar en su propia implementación.
     */
    public static void dispatch(GameObjects a, GameObjects b) {
        a.onCollisionWith(b);
        b.onCollisionWith(a);
    }
}
