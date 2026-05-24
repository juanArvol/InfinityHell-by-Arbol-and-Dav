package Game.World.WorldObjects;

import Game.Engine.GameObjects;
import Game.Engine.Components.Collisions.ColliderComponent;
import Game.Engine.Components.Visuals.HitBoxComponent;
import Game.Engine.Components.Visuals.SizeSyncMode;
import Game.Engine.Components.Visuals.SpriteRenderer;
import Game.Engine.Filter.CollisionProfile;
import GameMath.Vector2D;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class BlockWorld extends GameObjects {

    public BlockWorld(
            Vector2D position,
            BufferedImage texture,
            int width,
            int height
    ) {

        getTransform().setPosition(position);

        // ================= COLLIDER =================

        ColliderComponent collider =
                new ColliderComponent(
                        width,
                        height,
                        CollisionProfile.WORLD
                );

        addComponent(collider);

        // ================= DEBUG =================

        addComponent(new HitBoxComponent(Color.BLUE));

        // ================= RENDER =================

        if (texture != null) {
            // COLLIDER_TO_SPRITE: el sprite se dibuja estirado al tamaño del bloque (width x height).
            // Sin esto, el sprite se dibuja a su tamaño natural (ej: 64x64)
            // pero el bloque puede ser 1280x200 → sprite se repite o queda cortado visualmente.
            addComponent(new SpriteRenderer(texture, SizeSyncMode.COLLIDER_TO_SPRITE));
        }
    }

    @Override
    public void update() {
        super.update();
    }
}
