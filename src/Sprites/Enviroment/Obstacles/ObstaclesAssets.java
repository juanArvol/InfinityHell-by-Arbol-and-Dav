package Sprites.Enviroment.Obstacles;

import Sprites.Core.AssetLoader;
import Sprites.Core.AssetRegistry;
import Sprites.Core.SpriteDefinition;
import Sprites.Core.SpriteFrame;
import Sprites.Core.SpriteHandle;

/**
 * ObstaclesAssets — recursos visuales de obstáculos del mundo.
 *
 * ── CAMBIO ARQUITECTÓNICO ────────────────────────────────────────────────
 * ANTES:  Clase Mondongo wrapeando una BufferedImage.
 *
 * AHORA:  Registro en AssetRegistry. Expone SpriteHandle y atajo de
 *         compatibilidad getMondongoImage() para ObstacleLayer mientras
 *         migra gradualmente.
 *
 * ── CLAVES ───────────────────────────────────────────────────────────────
 *   "obstacle.mondongo" → textura del obstáculo mondongo
 */
public final class ObstaclesAssets {

    /** Handle del obstáculo mondongo. */
    public static SpriteHandle mondongoHandle;

    private ObstaclesAssets() {}

    public static void init() {

        SpriteFrame mondongoFrame = AssetLoader.loadFrame("/Sprites/Source/mondongo.png");
        SpriteDefinition mongongoDef = new SpriteDefinition(mondongoFrame);

        mondongoHandle = new SpriteHandle(mongongoDef, "obstacle.mondongo");
        AssetRegistry.getInstance().register("obstacle.mondongo", mondongoHandle);
    }

    /**
     * Atajo de compatibilidad: devuelve la BufferedImage del mondongo.
     * Usado por ObstacleLayer mientras el constructor de Obstacle siga
     * aceptando BufferedImage. Eliminar una vez que Obstacle migre a SpriteHandle.
     *
     * @return BufferedImage del mondongo, o null si no está cargado
     */
    public static java.awt.image.BufferedImage getMondongoImage() {
        if (mondongoHandle == null) return null;
        return mondongoHandle.resolveDefault().getImage();
    }
}
