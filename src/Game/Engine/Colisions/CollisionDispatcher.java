package Game.Engine.Colisions;

import Game.Engine.Destroyable;
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
 * ── HRFC — Collision System Refactor ──────────────────────────────────────
 *
 * MEJORA: Guard contra objetos destruidos.
 *   Previene dispatch a objetos pendientes de destrucción (error silencioso).
 *
 * ── Funcionamiento ───────────────────────────────────────────────────────
 *   dispatch(A, B):
 *     1. Valida que A y B NO estén destruidos
 *     2. A.onCollisionWith(B)   — A reacciona al tipo de B
 *     3. B.onCollisionWith(A)   — B reacciona al tipo de A
 */
public final class CollisionDispatcher {

    private CollisionDispatcher() {}

    /**
     * Notifica la colisión a ambos objetos.
     * Cada objeto decide cómo reaccionar en su propia implementación.
     * 
     * Si alguno de los objetos está pendiente de destrucción, se skip el dispatch.
     */
    public static void dispatch(GameObjects a, GameObjects b) {
        // Guard: no despachar a objetos destruidos
        if (a instanceof Destroyable da && da.isPendingDestruction()) return;
        if (b instanceof Destroyable db && db.isPendingDestruction()) return;
        
        a.onCollisionWith(b);
        b.onCollisionWith(a);
    }
}
