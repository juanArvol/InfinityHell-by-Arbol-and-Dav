package Game.Engine.Colisions;

import Game.Enemys.Enemy;
import Game.Engine.GameObjects;
import Game.Items.Types.Bullets.Bullet;
import Game.Player.Player;
import Game.World.WorldObjects.BlockWorld;
import Game.World.WorldObjects.Obstacle;

/**
 * Despacha la colisión al método correcto de cada objeto según su tipo.
 *
 * ── Por qué reemplaza el Visitor ─────────────────────────────────────────
 * El sistema Visitor original (Collidable + CollisionVisitor + CollisionVisitorInstance
 * + VisitorsAcepts) requería 4 clases/interfaces para hacer un simple instanceof.
 * El default vacío en VisitorsAcepts hacía que colisiones se "procesaran"
 * silenciosamente sin hacer nada — difícil de debuggear.
 *
 * Este dispatcher hace lo mismo con 1 clase, de forma explícita y legible.
 *
 * ── Cómo funciona ────────────────────────────────────────────────────────
 * Dado un par (A, B) que colisionaron:
 *   dispatch(A, B) → A.onCollisionWith( tipo correcto de B )
 *                  → B.onCollisionWith( tipo correcto de A )
 *
 * Cada objeto solo implementa los onCollisionWith() que le importan.
 * Los que no implementa tienen el default vacío en GameObjects.
 */
public final class CollisionDispatcher {

    private CollisionDispatcher() {}

    /**
     * Notifica la colisión a ambos objetos con el tipo correcto del otro.
     */
    public static void dispatch(GameObjects a, GameObjects b) {
        notifyOne(a, b);
        notifyOne(b, a);
    }

    /**
     * Notifica a `receiver` que colisionó con `other`, casteando other al tipo correcto.
     */
    private static void notifyOne(GameObjects receiver, GameObjects other) {
        if (other instanceof Player p)     { receiver.onCollisionWith(p); return; }
        if (other instanceof Bullet b)     { receiver.onCollisionWith(b); return; }
        if (other instanceof Enemy e)      { receiver.onCollisionWith(e); return; }
        if (other instanceof BlockWorld w) { receiver.onCollisionWith(w); return; }
        if (other instanceof Obstacle o)   { receiver.onCollisionWith(o); return; }
        // Tipo desconocido — sin efecto. No lanzar excepción.
    }
}
