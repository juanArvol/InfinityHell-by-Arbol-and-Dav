package Game.Engine;

import Game.Engine.Components.Collisions.ColliderComponent;
import Game.Engine.Components.PhysicsComponent;
import Game.Engine.Components.Visuals.SizeSyncMode;
import Game.Engine.Components.Visuals.SpriteRenderer;
import Game.Fisics.Physics;
import Game.Fisics.PhysicsStepper;
import GameMath.Vector2D;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

/**
 * Base de todos los objetos que se mueven con física.
 *
 * Agrega automáticamente:
 *   - SpriteRenderer  (con la textura dada, y el SizeSyncMode indicado)
 *   - ColliderComponent (sin perfil ni tamaño — la subclase los define)
 *   - PhysicsComponent  (con el Physics dado)
 *
 * El SizeSyncMode se aplica en SpriteRenderer.start(), que se llama
 * DENTRO de addComponent(). En ese momento el ColliderComponent ya está
 * en la lista porque se agrega antes del SpriteRenderer.
 *
 * Orden de addComponent() importa:
 *   1. ColliderComponent  ← sin tamaño todavía
 *   2. SpriteRenderer     ← start() lee el ColliderComponent del paso 1
 *   3. PhysicsComponent
 *   4. (subclase) collider.setProfile() + collider.setSize()
 *      → si el modo es COLLIDER_TO_SPRITE, start() ya copió el tamaño
 *        del sprite al collider. Si el modo es SPRITE_TO_COLLIDER,
 *        el SpriteRenderer ya copió el tamaño del collider al sprite.
 *
 * NOTA sobre el orden y SPRITE_TO_COLLIDER:
 *   La subclase define collider.setSize() DESPUÉS de MovingObjects constructor.
 *   Eso significa que cuando SpriteRenderer.start() se ejecuta, el collider
 *   todavía tiene tamaño 0×0. Por eso, si querés SPRITE_TO_COLLIDER en
 *   MovingObjects, usá SPRITE_TO_COLLIDER_WITH_OFFSET y llamá a
 *   syncRendererToCollider() DESPUÉS de definir el collider en la subclase.
 *   Ver método syncRendererToCollider() más abajo.
 */
public abstract class MovingObjects extends GameObjects {

    protected final PhysicsComponent physicsComponent;

    // ── Constructor base: sin sync ────────────────────────────────────────

    public MovingObjects(Vector2D position, BufferedImage texture, Physics physics) {
        this(position, texture, physics, SizeSyncMode.NONE);
    }

    // ── Constructor con modo de sync declarativo ──────────────────────────

    public MovingObjects(Vector2D position,
                         BufferedImage texture,
                         Physics physics,
                         SizeSyncMode syncMode) {

        getTransform().setPosition(position);

        // Collider primero (sin tamaño/perfil aún — la subclase los define)
        addComponent(new ColliderComponent());

        // SpriteRenderer con su modo de sync
        addComponent(new SpriteRenderer(texture, syncMode));

        physicsComponent = new PhysicsComponent(physics);
        addComponent(physicsComponent);
    }

    // ── Sync manual post-construcción ─────────────────────────────────────

    /**
     * Sincroniza el SpriteRenderer al ColliderComponent actual.
     *
     * Llamar desde la subclase DESPUÉS de definir collider.setSize(),
     * cuando querés que el sprite se escale al tamaño del collider.
     *
     * Ejemplo en Player:
     *   collider.setSize(15, 24);
     *   collider.setOffset(4, 0);
     *   syncRendererToCollider();  // sprite queda 15×24, offsetX=4
     */
    protected void syncRendererToCollider() {
        ColliderComponent col = getComponent(ColliderComponent.class);
        SpriteRenderer    ren = getComponent(SpriteRenderer.class);
        if (col == null || ren == null) return;

        ren.setRenderSize(col.getWidth(), col.getHeight());
        ren.setOffset(col.getOffsetX(), col.getOffsetY());
    }

    // ── API pública ───────────────────────────────────────────────────────

    public Physics getPhysics() { return physicsComponent.getPhysics(); }

    public Vector2D getVelocity() { return getPhysics().getVelocity(); }

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
