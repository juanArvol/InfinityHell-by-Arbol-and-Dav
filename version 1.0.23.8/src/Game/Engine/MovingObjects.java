package Game.Engine;

import Game.Engine.Components.Collisions.ColliderComponent;
import Game.Engine.Components.PhysicsComponent;
import Game.Engine.Components.Visuals.SpriteRenderer;
import Game.Engine.Filter.CollisionProfile;
import Game.Fisics.Physics;
import Game.Fisics.PhysicsStepper;
import GameMath.Vector2D;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

/**
 * Base de todos los objetos que se mueven con física.
 *
 * Agrega automáticamente:
 *   - SpriteRenderer  (con la textura dada)
 *   - ColliderComponent (sin perfil — la subclase lo define llamando setProfile())
 *   - PhysicsComponent  (con el Physics dado)
 *
 * La subclase (Player, Enemy) debe llamar:
 *   getComponent(ColliderComponent.class).setProfile(CollisionProfile.PLAYER);
 * en su constructor para que las capas queden bien configuradas.
 */
public abstract class MovingObjects extends GameObjects {

    protected final PhysicsComponent physicsComponent;

    public MovingObjects(Vector2D position,
                         BufferedImage texture,
                         Physics physics) {

        getTransform().setPosition(position);

        addComponent(new SpriteRenderer(texture));

        // Collider sin tamaño ni perfil aún — la subclase los define
        addComponent(new ColliderComponent());

        physicsComponent = new PhysicsComponent(physics);
        addComponent(physicsComponent);
    }

    public Physics getPhysics() {
        return physicsComponent.getPhysics();
    }

    public Vector2D getVelocity() {
        return getPhysics().getVelocity();
    }

    /**
     * Mueve el objeto según su velocidad actual.
     * Llamar desde update() DESPUÉS de calcular inputs y física.
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
