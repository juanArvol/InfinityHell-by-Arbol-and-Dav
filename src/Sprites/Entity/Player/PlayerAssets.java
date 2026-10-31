package Sprites.Entity.Player;

import Sprites.Core.Animation;
import Sprites.Core.AssetRegistry;
import Sprites.Core.SpriteDefinition;
import Sprites.Core.SpriteFrame;
import Sprites.Core.SpriteHandle;
import Sprites.Entity.Player.Animations.PlayerFallingAssets;
import Sprites.Entity.Player.Animations.PlayerIdleAssets;
import Sprites.Entity.Player.Animations.PlayerJumpingAssets;
import Sprites.Entity.Player.Animations.PlayerWalkingAssets;

/**
 * PlayerAssets — orquestador de recursos visuales del jugador.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * Este archivo SOLO carga recursos desde las subcarpetas especializadas:
 *
 *   • Animations/     → Spritesheets animados (idle, walking, jumping, falling)
 *   • SingleSprites/  → Sprites estáticos (ninguno por ahora)
 *
 * NO contiene paths hardcodeados. Los loaders específicos encapsulan la
 * carga de cada spritesheet con responsabilidad única.
 *
 * ── LOADERS ESPECIALIZADOS ───────────────────────────────────────────────
 *   - PlayerIdleAssets.loadIdleAnimation()
 *   - PlayerWalkingAssets.loadWalkAnimation()
 *   - PlayerJumpingAssets.loadJumpingAnimation()
 *   - PlayerFallingAssets.loadFallingAnimation()
 *
 * ── ESTRUCTURA ────────────────────────────────────────────────────────────
 * El jugador usa CUATRO spritesheets separados:
 *
 * 1. Player_Idle_Spritesheet.png (60×24 px)
 *    - 4 frames de 15×24 px
 *    - Animación idle completa
 *
 * 2. Player_Walking_Spritesheet.png (225×24 px)
 *    - 15 frames de 15×24 px
 *    - Animación de caminar completa
 *
 * 3. Player_Jumping_Spritesheet.png
 *    - Frames de 15×24 px
 *    - Animación de salto (loop continuo mientras sube)
 *
 * 4. Player_Falling_Spritesheet.png
 *    - Frames de 15×24 px
 *    - Animación de caída (loop continuo mientras baja)
 *
 * ── ANIMACIONES ──────────────────────────────────────────────────────────
 *   "idle"       → Loop de todos los frames del idle sheet
 *   "walk_right" → Reproduce frames 0-4 una vez, luego loop desde frame 5
 *   "walk_left"  → NO existe. PlayerRenderer aplica flipH=true
 *   "jumping"    → Loop continuo mientras el jugador sube (velocityY < 0)
 *   "falling"    → Loop continuo mientras el jugador baja (velocityY ≥ 0)
 *
 * ── COMPORTAMIENTO ───────────────────────────────────────────────────────
 * • Walk: playOnce(0, 4) + thenLoop(5, end) para transición suave
 * • Jumping: loop continuo, PlayerRenderer cambia a falling cuando velocityY ≥ 0
 * • Falling: loop continuo, puede activarse con o sin salto previo
 */
public final class PlayerAssets {

    /**
     * Handle principal del jugador.
     * Contiene todas las animaciones: idle, walk_right
     */
    public static SpriteHandle handle;

    private PlayerAssets() {}

    /**
     * Carga los spritesheets desde loaders especializados, define las
     * animaciones y registra el handle en AssetRegistry.
     *
     * Llamar una sola vez durante la inicialización del juego (Assets.init()).
     */
    public static void init() {
        // ── Cargar animaciones desde loaders especializados ───────────────
        Animation idleAnim    = PlayerIdleAssets.loadIdleAnimation();
        Animation walkAnim    = PlayerWalkingAssets.loadWalkAnimation();
        Animation jumpingAnim = PlayerJumpingAssets.loadJumpingAnimation();
        Animation fallingAnim = PlayerFallingAssets.loadFallingAnimation();
        SpriteFrame defaultFrame = PlayerIdleAssets.loadDefaultFrame();

        // ── Validar carga exitosa ─────────────────────────────────────────
        if (idleAnim == null || walkAnim == null || jumpingAnim == null || 
            fallingAnim == null || defaultFrame == null) {
            System.err.println("[PlayerAssets] Error cargando uno o más spritesheets");
            handle = SpriteHandle.EMPTY;
            return;
        }

        // ── Definición completa ───────────────────────────────────────────
        SpriteDefinition def = new SpriteDefinition(defaultFrame)
            .addAnimation("idle",       idleAnim)
            .addAnimation("walk_right", walkAnim)
            .addAnimation("jumping",    jumpingAnim)
            .addAnimation("falling",    fallingAnim);
        
        // Nota: "walk_left" no existe — PlayerRenderer aplica flipH=true

        // ── Registrar ─────────────────────────────────────────────────────
        handle = new SpriteHandle(def, "player");
        AssetRegistry.getInstance().register("player", handle);

        System.out.println("[PlayerAssets] Carga completa: idle, walk, jump, fall");
    }
}
