package Sprites.Player;

import Sprites.Core.Animation;
import Sprites.Core.AssetLoader;
import Sprites.Core.AssetRegistry;
import Sprites.Core.SpriteDefinition;
import Sprites.Core.SpriteFrame;
import Sprites.Core.SpriteHandle;

/**
 * PlayerAssets — definición de todos los recursos visuales del jugador.
 *
 * ── CAMBIO ARQUITECTÓNICO ────────────────────────────────────────────────
 * ANTES:  Tres clases separadas (WalkD, WalkIzq, PlayerIdle), cada una
 *         cargando sus propias BufferedImage con Loader.imageLoader().
 *         PlayerRenderer accedía a las clases directamente como statics.
 *
 * AHORA:  Una sola clase orientada a datos. Las animaciones son datos
 *         (Animation + SpriteFrame[]) registrados en el AssetRegistry.
 *         PlayerRenderer trabaja con claves de string y SpriteHandles.
 *
 * ── CLAVES DE ANIMACIÓN ──────────────────────────────────────────────────
 *   "idle"       → sprite estático parado
 *   "walk_right" → caminar a la derecha
 *   "walk_left"  → caminar a la izquierda
 *
 * ── HANDLE PRINCIPAL ─────────────────────────────────────────────────────
 * El handle "player" contiene TODAS las animaciones del jugador.
 * PlayerRenderer lo obtiene una vez y lo usa para todas sus animaciones.
 *
 * ── COMPATIBILIDAD ───────────────────────────────────────────────────────
 * handle campo estático mantenido para compatibilidad con GameWorldBootstrap
 * que necesita la imagen inicial. Internamente usa resolveImage() del registry.
 */
public final class PlayerAssets {

    /** Handle principal del jugador — contiene idle + walk_right + walk_left. */
    public static SpriteHandle handle;

    private PlayerAssets() {}

    public static void init() {

        // ── Idle ──────────────────────────────────────────────────────────
        SpriteFrame idleFrame = AssetLoader.loadFrame("/Sprites/Source/player/eee.png");

        // ── Caminar derecha (2 frames) ────────────────────────────────────
        SpriteFrame[] walkRightFrames = AssetLoader.loadFrames(
            "/Sprites/Source/player/dereuno.png",
            "/Sprites/Source/player/deredos.png"
        );

        // ── Caminar izquierda (3 frames) ──────────────────────────────────
        SpriteFrame[] walkLeftFrames = AssetLoader.loadFrames(
            "/Sprites/Source/player/ladoHizquierdo.png",
            "/Sprites/Source/player/ladoHizquierd1o.png",
            "/Sprites/Source/player/ladoHizquierd2o.png"
        );

        // ── Definición del sprite del jugador ─────────────────────────────
        SpriteDefinition def = new SpriteDefinition(idleFrame)
            .addAnimation("idle",       Animation.still(idleFrame))
            .addAnimation("walk_right", Animation.loop(walkRightFrames, 10))
            .addAnimation("walk_left",  Animation.loop(walkLeftFrames,  10));

        // ── Registrar en el registry global ──────────────────────────────
        handle = new SpriteHandle(def, "player");
        AssetRegistry.getInstance().register("player", handle);
    }
}
