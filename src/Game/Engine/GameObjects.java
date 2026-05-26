package Game.Engine;

import Game.Bullets.Bullet;
import Game.Enemys.Enemy;
import Game.Player.Player;
import Game.World.WorldObjects.BlockWorld;
import Game.World.WorldObjects.Obstacle;

import java.util.ArrayList;
import java.util.List;

/**
 * Clase base de todos los objetos del juego.
 *
 * ── Qué hace ────────────────────────────────────────────────────────────
 * - Tiene un Transform (posición, rotación).
 * - Tiene una lista de Component (renderer, collider, physics, etc.).
 * - Tiene métodos onCollisionWith() vacíos por defecto que las subclases
 *   sobreescriben solo cuando les importa esa colisión.
 *
 * ── Qué se eliminó respecto a la versión anterior ───────────────────────
 * - La interfaz Collidable: era redundante, GameObjects ya es la base.
 * - La interfaz VisitorsAcepts con su "default vacío con :D".
 * - handleCollision() / resolveExits(): el estado enter/stay/exit se
 *   trasladó a CollisionListener en Component. Si tu componente necesita
 *   saber cuándo entra o sale una colisión, implementa CollisionListener.
 *   La mayoría de objetos (balas, enemigos) solo necesita onCollisionWith().
 * - acceptCollision() / acceptVisitor(): reemplazado por CollisionDispatcher.
 * - scale(): causaba bugs (BUG-007), eliminado.
 */
public class GameObjects {

    private final Transform transform = new Transform();
    private final List<Component> components = new ArrayList<>();

    // ── Transform ─────────────────────────────────────────────────────────

    public Transform getTransform() { return transform; }

    // ── Components ────────────────────────────────────────────────────────

    public void addComponent(Component c) {
        c.setGameObject(this);
        components.add(c);
        c.start();
    }

    public <T> T getComponent(Class<T> type) {
        for (Component c : components) {
            if (type.isInstance(c)) return type.cast(c);
        }
        return null;
    }

    public List<Component> getComponents() {
        return components;
    }

    public void update() {
        for (Component c : components) {
            c.update();
        }
    }

    // ── Colisiones ────────────────────────────────────────────────────────
    // Sobreescribir solo los que te importen. Los demás no hacen nada.

    public void onCollisionWith(Player player)     {}
    public void onCollisionWith(Enemy enemy)       {}
    public void onCollisionWith(Bullet bullet)     {}
    public void onCollisionWith(BlockWorld block)  {}
    public void onCollisionWith(Obstacle obstacle) {}
}
