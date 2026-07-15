package Sprites.Around.Blocks;

import Sprites.Core.AssetLoader;
import Sprites.Core.AssetRegistry;
import Sprites.Core.SpriteDefinition;
import Sprites.Core.SpriteFrame;
import Sprites.Core.SpriteHandle;

/**
 * BlocksAssets — recursos visuales de bloques del mundo.
 *
 * ── CAMBIO ARQUITECTÓNICO ────────────────────────────────────────────────
 * ANTES:  Clase Suelo wrapeando una BufferedImage.
 *
 * AHORA:  Registro en AssetRegistry. BlocksAssets expone SpriteHandles y
 *         provee un atajo de compatibilidad getSprite() para los consumidores
 *         que todavía pasen BufferedImage al constructor de BlockWorld.
 *
 * ── CLAVES ───────────────────────────────────────────────────────────────
 *   "world.suelo" → textura del suelo / pasto
 */
public final class BlocksAssets {

    /** Handle del suelo. */
    public static SpriteHandle sueloHandle;

    private BlocksAssets() {}

    public static void init() {

        SpriteFrame sueloFrame = AssetLoader.loadFrame("/Sprites/Source/ambiente/pasto.png");
        SpriteDefinition sueloDef = new SpriteDefinition(sueloFrame);

        sueloHandle = new SpriteHandle(sueloDef, "world.suelo");
        AssetRegistry.getInstance().register("world.suelo", sueloHandle);
    }

    /**
     * Atajo de compatibilidad: devuelve la BufferedImage del suelo.
     * Usado por TerrainLayer mientras el constructor de BlockWorld siga
     * aceptando BufferedImage. Eliminar una vez que BlockWorld migre a SpriteHandle.
     *
     * @return BufferedImage del suelo, o null si no está cargado
     */
    public static java.awt.image.BufferedImage getSueloImage() {
        if (sueloHandle == null) return null;
        return sueloHandle.resolveDefault().getImage();
    }
}
