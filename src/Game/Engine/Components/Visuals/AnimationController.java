package Game.Engine.Components.Visuals;

import Game.Engine.Component;
import Sprites.Core.Animation;
import Sprites.Core.SpriteFrame;
import Sprites.Core.SpriteHandle;

/**
 * AnimationController — maneja el estado de reproducción de animaciones.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * Mantiene el estado de la animación actual (qué animación, qué frame,
 * qué tick) y notifica al SpriteRenderer con el frame correcto cada update.
 *
 * ── SEPARACIÓN DE RESPONSABILIDADES ──────────────────────────────────────
 *
 *   Animation         → define CÓMO avanza una animación (inmutable, datos)
 *   AnimationController → mantiene EL ESTADO de reproducción por entidad
 *   SpriteRenderer    → DIBUJA el frame que le pasa AnimationController
 *
 * ── USO ───────────────────────────────────────────────────────────────────
 *
 *   // En el constructor de la entidad:
 *   addComponent(new AnimationController(PlayerAssets.handle));
 *
 *   // En el Renderer de la entidad (ejemplo: PlayerRenderer):
 *   animController.play("walk_right");
 *   animController.play("idle");
 *
 * ── TRANSICIÓN DE ANIMACIONES ─────────────────────────────────────────────
 * play(key) es idempotente: si se llama con la misma clave que ya está
 * reproduciéndose, no resetea el frame. Esto evita el parpadeo cuando
 * el Renderer llama play() cada frame mientras la misma animación sigue.
 *
 * ── ANIMACIÓN NO ENCONTRADA ───────────────────────────────────────────────
 * Si la clave no existe en el handle, se mantiene la animación actual.
 * Nunca lanza excepción. Loguea warning en stderr.
 */
public class AnimationController extends Component {

    private final SpriteHandle handle;

    /** Clave de la animación actualmente en reproducción. */
    private String currentKey = null;

    /** Animación actualmente en reproducción. */
    private Animation currentAnimation = null;

    /** Índice del frame actual dentro de la animación. */
    private int frameIndex = 0;

    /** Tick acumulado dentro del frame actual. */
    private int tick = 0;

    /** Referencia cacheada al SpriteRenderer del mismo objeto. */
    private SpriteRenderer renderer;

    // ── Constructor ──────────────────────────────────────────────────────

    /**
     * @param handle handle del sprite con todas las animaciones disponibles
     */
    public AnimationController(SpriteHandle handle) {
        if (handle == null) {
            throw new IllegalArgumentException("AnimationController: handle no puede ser null");
        }
        this.handle = handle;
    }

    // ── Ciclo de vida ────────────────────────────────────────────────────

    @Override
    public void start() {
        renderer = gameObject.getComponent(SpriteRenderer.class);
        if (renderer == null) {
            System.err.println("[AnimationController] No se encontró SpriteRenderer en "
                + gameObject.getClass().getSimpleName()
                + ". AnimationController no tiene efecto.");
        }
        // Actualizar el handle en el renderer para que los tamaños queden correctos
        if (renderer != null) {
            renderer.setHandle(handle);
        }
    }

    @Override
    public void update() {
        if (currentAnimation == null || renderer == null) return;

        // Avanzar el tick
        tick++;
        if (tick >= currentAnimation.getTicksPerFrame()) {
            tick = 0;
            // Avanzar frame (Animation maneja el loop/once/pingpong)
            if (!currentAnimation.isFinished(frameIndex)) {
                frameIndex = currentAnimation.nextIndex(frameIndex, currentAnimation.getTicksPerFrame() - 1);
            }
        }

        // Empujar el frame actual al SpriteRenderer
        SpriteFrame frame = currentAnimation.getFrame(frameIndex);
        renderer.setCurrentFrame(frame);
    }

    // ── API pública ──────────────────────────────────────────────────────

    /**
     * Inicia o continúa la reproducción de la animación con la clave dada.
     *
     * Idempotente: si ya se está reproduciendo esta animación, no resetea
     * el frame ni el tick. Seguro llamarlo cada frame desde el Renderer.
     *
     * @param key clave de la animación (ej: "idle", "walk_right")
     */
    public void play(String key) {
        if (key == null) return;

        // Si ya está reproduciéndose esta animación, no interrumpir
        if (key.equals(currentKey)) return;

        Animation anim = handle.getAnimation(key);
        if (anim == null) {
            System.err.println("[AnimationController] Animación '" + key
                + "' no encontrada en handle '" + handle.getId() + "'");
            return;
        }

        currentKey       = key;
        currentAnimation = anim;
        frameIndex       = 0;
        tick             = 0;

        // Actualizar inmediatamente para no mostrar el frame anterior un tick
        if (renderer != null) {
            renderer.setCurrentFrame(anim.getFirstFrame());
        }
    }

    /**
     * Fuerza el reinicio de la animación actual desde el frame 0.
     * Útil para animaciones ONCE que necesitan reproducirse de nuevo.
     */
    public void restart() {
        frameIndex = 0;
        tick       = 0;
        if (currentAnimation != null && renderer != null) {
            renderer.setCurrentFrame(currentAnimation.getFirstFrame());
        }
    }

    /**
     * Detiene la animación y muestra el frame por defecto del handle.
     */
    public void stop() {
        currentKey       = null;
        currentAnimation = null;
        frameIndex       = 0;
        tick             = 0;
        if (renderer != null) {
            renderer.setCurrentFrame(handle.resolveDefault());
        }
    }

    // ── Consulta de estado ────────────────────────────────────────────────

    /** Clave de la animación actualmente en reproducción. Puede ser null. */
    public String getCurrentKey() { return currentKey; }

    /** true si la animación activa es la que tiene esa clave. */
    public boolean isPlaying(String key) { return key != null && key.equals(currentKey); }

    /** true si la animación actual terminó (solo aplica a LoopMode.ONCE). */
    public boolean isFinished() {
        return currentAnimation != null && currentAnimation.isFinished(frameIndex);
    }

    /** Índice del frame actual dentro de la animación. */
    public int getFrameIndex() { return frameIndex; }

    /** Handle del sprite asociado. */
    public SpriteHandle getHandle() { return handle; }
}
