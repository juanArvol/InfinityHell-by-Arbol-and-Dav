package Game.Engine.Components;

import Game.Engine.Component;
import Game.Fisics.Physics;

public class PhysicsComponent extends Component {

    private final Physics physics;

    public PhysicsComponent(Physics physics) {
        this.physics = physics;
    }

    public Physics getPhysics() {
        return physics;
    }
}