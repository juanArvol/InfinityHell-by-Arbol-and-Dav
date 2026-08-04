package Game.Engine;

import Game.Engine.GameMath.Logic2D.Transform2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Clase base de todos los objetos del juego.
 *
 * ── Qué hace ────────────────────────────────────────────────────────────
 * - Tiene un Transform (posición, rotación).
 * - Tiene una lista de Component (renderer, collider, physics, etc.).
 * - Expone onCollisionWith(GameObjects) que las subclases sobreescriben
 *   cuando necesitan reaccionar a una colisión.
 *
 * ── REFACTOR: Colisiones genéricas ──────────────────────────────────────
 *
 * PROBLEMA ORIGINAL:
 *   GameObjects declaraba sobrecargas tipadas:
 *     onCollisionWith(Player), onCollisionWith(Enemy), onCollisionWith(Bullet)...
 *   Eso obligaba al Engine a importar Player, Enemy, Bullet, BlockWorld y Obstacle
 *   — todos tipos concretos del Game. Una violación directa del principio
 *   de que el Engine nunca debe conocer el Game.
 *
 * SOLUCIÓN:
 *   Un único método genérico:
 *     public void onCollisionWith(GameObjects other) {}
 *
 *   Las subclases del Game que necesitan distinguir tipos hacen instanceof
 *   internamente:
 *
 *     // En Bullet:
 *     @Override
 *     public void onCollisionWith(GameObjects other) {
 *         if (other instanceof Enemy e) { e.damage((int) damage); bulletLife.setDead(); }
 *     }
 *
 *     // En WorldItem:
 *     @Override
 *     public void onCollisionWith(GameObjects other) {
 *         if (other instanceof Player p) { attemptPickup(p); }
 *     }
 *
 *   El Engine no necesita saber nada de los tipos concretos.
 *   CollisionDispatcher queda sin ningún import de Game.*.
 *
 * ── Transform inyectable (soporte 2D y 2.5D/3D) ─────────────────────────
 *
 * PROBLEMA:
 *   GameObjects siempre creaba `new Transform2D()` hardcodeado. El sistema
 *   2.5D (Physics3DComponent, ShadowComponent, DepthSortedRenderSystem)
 *   depende de que ciertos objetos tengan un Transform3D — que extiende
 *   Transform2D — como su transform. La comprobación `getTransform()
 *   instanceof Transform3D` nunca era verdadera porque no había mecanismo
 *   para instanciar un GameObjects con Transform3D.
 *
 * SOLUCIÓN:
 *   Un constructor protegido acepta un Transform2D externo. Las subclases
 *   que necesitan 3D pasan `new Transform3D()`:
 *
 *     // Objeto 2D normal — sin cambios:
 *     super(); // usa Transform2D por defecto
 *
 *     // Objeto con altura (enemigo volador, proyectil en arco):
 *     super(new Transform3D());
 *
 *   Retro-compatible: el constructor sin argumento sigue creando Transform2D.
 *   Nada en el código existente necesita cambiar para objetos 2D.
 *   Physics3DComponent.syncTransform3D() ahora puede sincronizar z correctamente
 *   en los objetos que declaran Transform3D.
 */
public class GameObjects {

    private final Transform2D transform;
    private final List<Component> components = new ArrayList<>();

    /** Constructor por defecto — Transform2D (comportamiento original). */
    public GameObjects() {
        this.transform = new Transform2D();
    }

    /**
     * Constructor con transform inyectable para soporte 2.5D/3D.
     *
     * Las subclases que necesiten altura Z pasan {@code new Transform3D()}.
     * El transform es final una vez fijado: no puede reemplazarse en runtime.
     *
     * @param transform transform a usar; no puede ser null.
     */
    protected GameObjects(Transform2D transform) {
        if (transform == null) throw new IllegalArgumentException("transform cannot be null");
        this.transform = transform;
    }

    // ── Transform ─────────────────────────────────────────────────────────

    public Transform2D getTransform() { return transform; }

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

    /**
     * Vista no modificable de los componentes.
     *
     * CORRECCIÓN: la implementación anterior devolvía la lista interna mutable
     * directamente. RenderSystem y DepthSortedRenderSystem la iteran desde el
     * GameLoop thread; si addComponent() se llamara concurrentemente se produciría
     * ConcurrentModificationException. Devolver una vista no modificable hace
     * explícito el contrato de solo lectura y detecta modificaciones accidentales
     * en tiempo de ejecución.
     */
    public List<Component> getComponents() {
        return Collections.unmodifiableList(components);
    }

    public void update() {
        for (Component c : components) {
            c.update();
        }
    }

    // ── Colisiones ────────────────────────────────────────────────────────

    /**
     * Notificación genérica de colisión.
     *
     * Sobreescribir en subclases del Game para reaccionar al tipo correcto:
     *
     *   @Override
     *   public void onCollisionWith(GameObjects other) {
     *       if (other instanceof Enemy e)  { e.damage(10); }
     *       if (other instanceof Player p) { p.receiveDamage(5); }
     *   }
     *
     * El default vacío permite que la mayoría de objetos ignore colisiones
     * sin boilerplate.
     */
    public void onCollisionWith(GameObjects other) {}
}
