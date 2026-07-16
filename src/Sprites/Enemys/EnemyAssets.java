package Sprites.Enemys;

import Sprites.Core.Animation;
import Sprites.Core.AssetLoader;
import Sprites.Core.AssetRegistry;
import Sprites.Core.SpriteDefinition;
import Sprites.Core.SpriteFrame;
import Sprites.Core.SpriteHandle;

/**
 * EnemyAssets — definición de todos los recursos visuales de los enemigos.
 *
 * ── HRFC-004: ELIMINADO EL PATH LEGACY ───────────────────────────────────
 * Los enemigos ya no usan BufferedImage como puente. SpriteHandle se pasa
 * directamente al constructor de Enemy → MovingObjects → SpriteRenderer.
 * No hay legacySprite, no hay frame incorrecto en el primer tick.
 *
 * ── CLAVES DE HANDLE ─────────────────────────────────────────────────────
 *   "enemy.normal"  → enemigo terrestre (zotopias)
 *   "enemy.flying"  → enemigo volador (gatos)
 *
 * ── CLAVES DE ANIMACIÓN ──────────────────────────────────────────────────
 *   "idle"  → animación base en loop
 *
 * Cuando los enemigos tengan animaciones de ataque, muerte, etc., solo hay
 * que añadir la entrada aquí sin modificar nada más.
 */
public final class EnemyAssets {

    /** Handle del enemigo terrestre (zotopia). */
    public static SpriteHandle normalHandle;

    /** Handle del enemigo volador (gato). */
    public static SpriteHandle flyingHandle;

    private EnemyAssets() {}

    public static void init() {

        // ── Enemigo normal (4 frames individuales) ────────────────────────
        SpriteFrame[] normalFrames = AssetLoader.loadFrames(
            "/Sprites/Source/enemies/zotopia1.png",
            "/Sprites/Source/enemies/zotopia2.png",
            "/Sprites/Source/enemies/zotopia3.png",
            "/Sprites/Source/enemies/zotopia4.png"
        );

        // Construir la animación idle via Builder para consistencia con el nuevo sistema.
        // El banco de frames son los 4 sprites individuales cargados arriba.
        Animation normalIdle = Animation.builder(normalFrames)
            .frame(0, 3)       // frames 0..3 inclusive
            .defaultDuration(12)
            .loop()
            .build();

        SpriteDefinition normalDef = new SpriteDefinition(normalFrames[0])
            .addAnimation("idle", normalIdle);

        normalHandle = new SpriteHandle(normalDef, "enemy.normal");
        AssetRegistry.getInstance().register("enemy.normal", normalHandle);

        // ── Enemigo volador (misma imagen repetida — se reemplazará con sprites reales) ─
        // gato.jpg se cachea una sola vez por AssetLoader.
        SpriteFrame flyingFrame = AssetLoader.loadFrame("/Sprites/Source/gato.jpg");
        SpriteFrame[] flyingFrames = new SpriteFrame[]{ flyingFrame, flyingFrame, flyingFrame, flyingFrame };

        Animation flyingIdle = Animation.builder(flyingFrames)
            .frame(0, 3)
            .defaultDuration(12)
            .loop()
            .build();

        SpriteDefinition flyingDef = new SpriteDefinition(flyingFrame)
            .addAnimation("idle", flyingIdle);

        flyingHandle = new SpriteHandle(flyingDef, "enemy.flying");
        AssetRegistry.getInstance().register("enemy.flying", flyingHandle);
    }
}
