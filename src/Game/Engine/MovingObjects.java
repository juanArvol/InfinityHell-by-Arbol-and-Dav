package Game.Engine;

import Game.Engine.Components.Collisions.ColliderComponent;
import Game.Engine.Components.Physics2DComponent;
import Game.Engine.Components.Visuals.SizeSyncMode;
import Game.Engine.Components.Visuals.SpriteRenderer;
import Game.Engine.GameMath.Physics.PhysicsStepper;
import Game.Engine.GameMath.Physics.Types.Physics2D;
import Game.Engine.GameMath.SpaceLogic.Logic2D.Transform2D;
import Game.Engine.GameMath.SpaceLogic.Logic2D.Vector2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

/**
 * Base de todos los objetos que se mueven con física.
 *
 * ── REFACTOR: EXTIENDE Entity EN LUGAR DE GameObjects ────────────────────
 *
 * PROBLEMA ORIGINAL:
 *   MovingObjects extendía GameObjects directamente, saltando la capa Entity.
 *   Esto dejaba Entity fuera de la jerarquía real:
 *
 *     Entity    → GameObjects        (rama huérfana, no usada)
 *     MovingObjects → GameObjects    (la jerarquía real, que ignoraba Entity)
 *     Player → MovingObjects
 *     Enemy  → MovingObjects
 *
 *   Entity existía en el código pero Player y Enemy no la heredaban porque
 *   MovingObjects no la incluía. Era una capa declarada pero no activa.
 *
 * SOLUCIÓN:
 *   MovingObjects extiende Entity en lugar de GameObjects.
 *   Entity extiende GameObjects.
 *   La cadena queda completa:
 *
 *     GameObjects → Entity → MovingObjects → Player/Enemy/Bullet
 *
 *   Ahora Player y Enemy heredan automáticamente los shortcuts de gameplay
 *   de Entity (damage, isDead, addEffect...) sin ningún cambio en su código.
 *
 * BENEFICIO:
 *   - La jerarquía refleja la intención arquitectónica real.
 *   - Player y Enemy tienen acceso a entity.damage(), entity.isDead(),
 *     entity.addEffect() sin boilerplate adicional.
 *   - Cualquier sistema que reciba Entity puede operar sobre Player y Enemy
 *     sin saber cuál de los dos es.
 *
 * ── RESPONSABILIDAD DE MovingObjects (sin cambios) ────────────────────────
 *
 * MovingObjects se mantiene enfocado exclusivamente en movimiento y física:
 * - Agrega ColliderComponent, SpriteRenderer y PhysicsComponent.
 * - Provee acceso a la física (getPhysics, getVelocity).
 * - Provee utilidades de movimiento (moveByPhysics, getCenter, getBounds).
 * - Provee sincronización de tamaños (syncRendererToCollider).
 *
 * No contiene lógica de gameplay. Eso es responsabilidad de Entity y sus
 * subclases concretas.
 */
public abstract class MovingObjects extends Entity {

    protected final Physics2DComponent physicsComponent;

    // ── Constructor base ──────────────────────────────────────────────────

    public MovingObjects(Vector2D position, BufferedImage texture, Physics2D physics) {
        this(position, texture, physics, SizeSyncMode.NONE);
    }

    // ── Constructor con modo de sync ──────────────────────────────────────

    public MovingObjects(Vector2D position,
                         BufferedImage texture,
                         Physics2D physics,
                         SizeSyncMode syncMode) {

        getTransform().setPosition(position);

        addComponent(new ColliderComponent());
        addComponent(new SpriteRenderer(texture, syncMode));

        physicsComponent = new Physics2DComponent(physics);
        addComponent(physicsComponent);
    }

    // ── Constructores con Transform inyectable (soporte 2.5D/3D) ─────────
    //
    // Permiten que subclases concretas pasen un Transform3D para activar
    // la sincronización de altura Z en Physics3DComponent, ShadowComponent
    // y DepthSortedRenderSystem sin cambios en la API del Engine.
    //
    // Uso en una subclase:
    //   super(new Transform3D(), position, texture, physics);
    //   addComponent(new Physics3DComponent());

    protected MovingObjects(Transform2D transform,
                            Vector2D position,
                            BufferedImage texture,
                            Physics2D physics) {
        this(transform, position, texture, physics, SizeSyncMode.NONE);
    }

    protected MovingObjects(Transform2D transform,
                            Vector2D position,
                            BufferedImage texture,
                            Physics2D physics,
                            SizeSyncMode syncMode) {
        super(transform);
        getTransform().setPosition(position);

        addComponent(new ColliderComponent());
        addComponent(new SpriteRenderer(texture, syncMode));

        physicsComponent = new Physics2DComponent(physics);
        addComponent(physicsComponent);
    }

    // ── Sync manual post-construcción ─────────────────────────────────────

    /**
     * Sincroniza el SpriteRenderer al ColliderComponent actual.
     * Llamar desde la subclase DESPUÉS de definir collider.setSize().
     */
    protected void syncRendererToCollider() {
        ColliderComponent col = getComponent(ColliderComponent.class);
        SpriteRenderer    ren = getComponent(SpriteRenderer.class);
        if (col == null || ren == null) return;

        ren.setRenderSize(col.getWidth(), col.getHeight());
        ren.setOffset(col.getOffsetX(), col.getOffsetY());
    }

    // ── API de movimiento y física ────────────────────────────────────────

    public Physics2D getPhysics()     { return physicsComponent.getPhysics(); }
    public Vector2D getVelocity()   { return getPhysics().getVelocity(); }

    /**
     * Mueve el objeto según su velocidad actual.
     * Llamar solo desde objetos TRIGGER (Bullet).
     * Objetos SOLID son movidos por CollisionsSystem (SweptAABB).
     */
    public void moveByPhysics() {
        Vector2D vel = getVelocity();
        PhysicsStepper.moveWith(this, vel.getX(), vel.getY());
    }

    public Vector2D getCenter() {
        var pos = getTransform().getPosition();
        ColliderComponent col = getComponent(ColliderComponent.class);
        if (col != null) {
            return pos.add(new Vector2D(col.getWidth() / 2.0, col.getHeight() / 2.0));
        }
        return pos;
    }

    public Rectangle getBounds() {
        ColliderComponent col = getComponent(ColliderComponent.class);
        if (col != null) return col.getBounds();
        var pos = getTransform().getPosition();
        return new Rectangle((int) pos.getX(), (int) pos.getY(), 0, 0);
    }
}
