package Sprites.Entity.Bullets;

import Sprites.Core.AssetLoader;
import Sprites.Core.AssetRegistry;
import Sprites.Core.SpriteDefinition;
import Sprites.Core.SpriteFrame;
import Sprites.Core.SpriteHandle;

/**
 * BulletAssets — recursos visuales de proyectiles.
 *
 * ── CAMBIO ARQUITECTÓNICO ────────────────────────────────────────────────
 * ANTES:  Clases Bala y CometaBala, cada una wrapeando una BufferedImage.
 *
 * AHORA:  Registros en el AssetRegistry. Los frames de bala son sprites
 *         estáticos (un solo frame). El sistema de animación los soporta
 *         igual que cualquier animación multi-frame.
 *
 * ── CLAVES ───────────────────────────────────────────────────────────────
 *   "bullet.bala"   → proyectil estándar
 *   "bullet.cometa" → proyectil cometa/especial
 */
public final class BulletAssets {

    /** Handle de bala estándar. */
    public static SpriteHandle balaHandle;

    /** Handle de bala cometa. */
    public static SpriteHandle cometaHandle;

    private BulletAssets() {}

    public static void init() {

        // ── Bala estándar ─────────────────────────────────────────────────
        SpriteFrame balaFrame = AssetLoader.loadFrame("/Sprites/Source/Entity/Bullets/bala.png");
        SpriteDefinition balaDef = new SpriteDefinition(balaFrame);

        balaHandle = new SpriteHandle(balaDef, "bullet.bala");
        AssetRegistry.getInstance().register("bullet.bala", balaHandle);

        // ── Bala cometa ───────────────────────────────────────────────────
        SpriteFrame cometaFrame = AssetLoader.loadFrame("/Sprites/Source/Entity/Bullets/cometa.png");
        SpriteDefinition cometaDef = new SpriteDefinition(cometaFrame);

        cometaHandle = new SpriteHandle(cometaDef, "bullet.cometa");
        AssetRegistry.getInstance().register("bullet.cometa", cometaHandle);
    }
}
