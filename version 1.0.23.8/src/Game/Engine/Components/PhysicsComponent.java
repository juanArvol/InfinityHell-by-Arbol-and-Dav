package Game.Engine.Components;

import Game.Engine.Component;
import Game.Fisics.Physics;

/**
 * Componente que le da física a un GameObjects.
 * Solo es un wrapper — la lógica real está en Physics y sus subclases.
 */
public class PhysicsComponent extends Component {

    private final Physics physics;

    public PhysicsComponent(Physics physics) {
        this.physics = physics;
    }

    public Physics getPhysics() {
        return physics;
    }
}
