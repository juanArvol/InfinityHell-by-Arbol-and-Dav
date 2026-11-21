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
 * ── DIMENSIONES CENTRALIZADAS ────────────────────────────────────────────
 * TODOS los frames del jugador comparten las mismas dimensiones por defecto.
 * Estos valores son la "fuente única de verdad" para el tamaño de los sprites.
 *
 * Si en el futuro cambias el tamaño de los sprites del jugador (ej: de 15×24
 * a 32×48), SOLO necesitas cambiar estos valores aquí. Todos los loaders
 * específicos (idle, walking, jumping, falling) los usarán automáticamente.
 *
 * En casos excepcionales donde un spritesheet específico tenga dimensiones
 * diferentes, el loader individual puede sobreescribir estos valores.
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

    // ══════════════════════════════════════════════════════════════════════
    // DIMENSIONES GLOBALES DEL JUGADOR
    // ══════════════════════════════════════════════════════════════════════
    
    /**
     * Ancho de cada frame del jugador en píxeles.
     * 
     * FUENTE ÚNICA DE VERDAD: Todos los spritesheets del jugador (idle, walking,
     * jumping, falling) usan este valor por defecto.
     * 
     * Cambiar este valor actualiza automáticamente todas las animaciones.
     * 
     * VALOR ACTUAL: 15 px (tamaño original de los sprites)
     * 
     * Para cambiar el tamaño de TODOS los sprites del jugador:
     *   1. Actualiza este valor (ej: 32)
     *   2. Actualiza FRAME_HEIGHT si es necesario
     *   3. Recompila
     *   4. ¡Listo! Todas las animaciones usarán el nuevo tamaño.
     */
    public static final int FRAME_WIDTH = 32;
    
    /**
     * Alto de cada frame del jugador en píxeles.
     * 
     * FUENTE ÚNICA DE VERDAD: Todos los spritesheets del jugador (idle, walking,
     * jumping, falling) usan este valor por defecto.
     * 
     * Cambiar este valor actualiza automáticamente todas las animaciones.
     * 
     * VALOR ACTUAL: 24 px (tamaño original de los sprites)
     */
    public static final int FRAME_HEIGHT = 48;

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

        // ── Crear animaciones con transición a walking ────────────────────
        // Estas animaciones combinan los frames de jumping/falling con los
        // frames de walking para crear transiciones suaves cuando hay input horizontal
        Animation jumpToWalkAnim = PlayerJumpingAssets.loadJumpingToWalkAnimation();
        Animation fallToWalkAnim = PlayerFallingAssets.loadFallingToWalkAnimation();

        // ── Definición completa ───────────────────────────────────────────
        SpriteDefinition def = new SpriteDefinition(defaultFrame)
            .addAnimation("idle",           idleAnim)
            .addAnimation("walk_right",     walkAnim)
            .addAnimation("jumping",        jumpingAnim)
            .addAnimation("falling",        fallingAnim)
            .addAnimation("jump_to_walk",   jumpToWalkAnim)
            .addAnimation("fall_to_walk",   fallToWalkAnim);
        
        // Nota: "walk_left" no existe — PlayerRenderer aplica flipH=true

        // ── Registrar ─────────────────────────────────────────────────────
        handle = new SpriteHandle(def, "player");
        AssetRegistry.getInstance().register("player", handle);

        System.out.println("[PlayerAssets] Carga completa: idle, walk, jump, fall (+ transiciones)");
    }
}
