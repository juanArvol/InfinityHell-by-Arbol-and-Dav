package Game.World.WorldObjects;

import Game.Engine.Colisions.Filter.CollisionProfile;
import Game.Engine.Entity.Components.Collisions.ColliderComponent;
import Game.Engine.Entity.Components.Visuals.HitBoxComponent;
import Game.Engine.Entity.Components.Visuals.SpriteRendererComponent;
import Game.Engine.GameMath.Physics.SurfaceMaterial;
import Game.Engine.GameMath.SpaceLogic.Logic2D.Vector2D;
import Game.Engine.GameObjects;
import Game.Engine.RenderEngine.Sprites.SizeSyncMode;
import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * FIX: Se agregaron getAirControl() y getAccelScale() que faltaban tras la
 * refactorización de SurfaceMaterial de clase a interface con 4 métodos.
 * Su ausencia causaba AbstractMethodError/crash al colisionar con obstáculos.
 */
public class Obstacle extends GameObjects implements SurfaceMaterial {

    private final SurfaceMaterial material;

    public Obstacle(int x, int y, int width, int height, BufferedImage texture) {
        this(x, y, width, height, texture, SurfaceMaterial.DEFAULT);
    }

    public Obstacle(int x, int y, int width, int height,
                    BufferedImage texture, SurfaceMaterial material) {

        this.material = material;
        getTransform().setPosition(new Vector2D(x, y));

        ColliderComponent collider = new ColliderComponent(width, height, CollisionProfile.WORLD);
        addComponent(collider);

        addComponent(new HitBoxComponent(Color.RED));

        if (texture != null) {
            addComponent(new SpriteRendererComponent(texture, SizeSyncMode.COLLIDER_TO_SPRITE));
        }
    }

    @Override public double getFriction()   { return material.getFriction();   }
    @Override public double getDrag()       { return material.getDrag();       }
    @Override public double getAirControl() { return material.getAirControl(); }  // FIX: faltaba
    @Override public double getAccelScale() { return material.getAccelScale(); }  // FIX: faltaba

    @Override
    public void update() { super.update(); }
}
