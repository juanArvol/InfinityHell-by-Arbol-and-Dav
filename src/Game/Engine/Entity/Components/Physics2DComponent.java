package Game.Engine.Entity.Components;

import Game.Engine.Component;
import Game.Engine.GameMath.Physics.Types.Physics2D;

/**
 * Componente que le da física a un GameObjects.
 * Solo es un wrapper — la lógica real está en Physics y sus subclases.
 */
public class Physics2DComponent extends Component {

    private final Physics2D physics;

    public Physics2DComponent(Physics2D physics) {
        this.physics = physics;
    }

    public Physics2D getPhysics() {
        return physics;
    }
}
