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
 * ── CAMBIO ARQUITECTÓNICO ────────────────────────────────────────────────
 * ANTES:  Dos clases separadas (EnemyNormal, EnemyFlying) con arrays de
 *         BufferedImage cargados individualmente. EnemyFactory elegía un
 *         frame aleatorio al crear cada enemigo (sin sistema de animación).
 *
 * AHORA:  Una sola clase orientada a datos. Cada tipo de enemigo tiene un
 *         SpriteHandle con todas sus animaciones. EnemyFactory obtiene el
 *         handle correcto por tipo; AnimationController maneja la animación.
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

        SpriteDefinition normalDef = new SpriteDefinition(normalFrames[0])
            .addAnimation("idle", Animation.loop(normalFrames, 12));

        normalHandle = new SpriteHandle(normalDef, "enemy.normal");
        AssetRegistry.getInstance().register("enemy.normal", normalHandle);

        // ── Enemigo volador (misma imagen repetida — se reemplazará con sprites reales) ─
        SpriteFrame flyingFrame = AssetLoader.loadFrame("/Sprites/Source/gato.jpg");

        // Todos los frames apuntan al mismo recurso (gato.jpg se cachea una vez).
        SpriteFrame[] flyingFrames = new SpriteFrame[]{
            flyingFrame, flyingFrame, flyingFrame, flyingFrame
        };

        SpriteDefinition flyingDef = new SpriteDefinition(flyingFrame)
            .addAnimation("idle", Animation.loop(flyingFrames, 12));

        flyingHandle = new SpriteHandle(flyingDef, "enemy.flying");
        AssetRegistry.getInstance().register("enemy.flying", flyingHandle);
    }
}
