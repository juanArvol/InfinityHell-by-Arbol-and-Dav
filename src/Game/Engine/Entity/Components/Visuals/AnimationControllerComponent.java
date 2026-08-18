package Game.Engine.Entity.Components.Visuals;

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
 * ── HRFC-004: DURACIÓN POR FRAME ─────────────────────────────────────────
 * AnimationController ahora respeta las duraciones individuales por frame
 * definidas en Animation.Builder con .frameDuration(pos, ticks).
 *
 * El tick acumulado se compara con Animation.ticksForFrame(frameIndex) en
 * lugar del defaultTicksPerFrame global. Esto soporta animaciones donde
 * ciertos frames duran más (hitboxes extendidas, énfasis visual, pauses).
 *
 * ── SEPARACIÓN DE RESPONSABILIDADES ──────────────────────────────────────
 *
 *   Animation           → define CÓMO avanza una animación (inmutable, datos)
 *   AnimationController → mantiene EL ESTADO de reproducción por entidad
 *   SpriteRenderer      → DIBUJA el frame que le pasa AnimationController
 *
 * ── TRANSICIÓN DE ANIMACIONES ─────────────────────────────────────────────
 * play(key) es idempotente: si se llama con la misma clave que ya está
 * reproduciéndose, no resetea el frame. Esto evita el parpadeo cuando
 * el Renderer llama play() cada frame mientras la misma animación sigue.
 *
 * ── ANIMACIÓN NO ENCONTRADA ───────────────────────────────────────────────
 * Si la clave no existe en el handle, se mantiene la animación actual.
 * Nunca lanza excepción. Loguea warning en stderr.
 *
 * ── PING-PONG ─────────────────────────────────────────────────────────────
 * En LoopMode.PING_PONG, AnimationController gestiona la dirección interna
 * (pingPongForward). Llama a nextIndex() en avance y nextIndexReverse() en
 * retroceso. Al alcanzar un extremo, invierte la dirección.
 */
public class AnimationControllerComponent extends Component {

    private final SpriteHandle handle;

    /** Clave que se reproduce automáticamente en start(). null = no auto-play. */
    private String autoPlayKey = null;

    /** Clave de la animación actualmente en reproducción. */
    private String currentKey = null;

    /** Animación actualmente en reproducción. */
    private Animation currentAnimation = null;

    /** Índice del frame actual dentro de la animación. */
    private int frameIndex = 0;

    /**
     * Tiempo acumulado dentro del frame actual en segundos.
     * Se compara con Animation.ticksForFrame(frameIndex) / 60.0 para determinar
     * cuándo avanzar al siguiente frame.
     *
     * ── HRFC — Unified DeltaTime Migration ───────────────────────────────
     *
     * MIGRACIÓN: Cambiado de int tick (frames) a double elapsedTime (segundos).
     * La conversión desde Animation.ticksForFrame() se hace dividiendo por 60.0.
     */
    private double elapsedTime = 0.0;

    /**
     * Dirección de avance para PING_PONG.
     * true = avanzando (índice creciente), false = retrocediendo.
     */
    private boolean pingPongForward = true;

    /** Referencia cacheada al SpriteRenderer del mismo objeto. */
    private SpriteRendererComponent renderer;

    // ── Constructores ────────────────────────────────────────────────────

    /**
     * @param handle      handle del sprite con todas las animaciones disponibles
     * @param autoPlayKey clave de animación a reproducir automáticamente en start().
     *                    Si null, no hay auto-play (el caller llama play() manualmente).
     */
    public AnimationControllerComponent(SpriteHandle handle, String autoPlayKey) {
        if (handle == null) {
            throw new IllegalArgumentException("AnimationController: handle no puede ser null");
        }
        this.handle      = handle;
        this.autoPlayKey = autoPlayKey;
    }

    /**
     * Constructor sin auto-play. El caller llama play() explícitamente.
     */
    public AnimationControllerComponent(SpriteHandle handle) {
        this(handle, null);
    }

    // ── Ciclo de vida ────────────────────────────────────────────────────

    @Override
    public void start() {
        renderer = gameObject.getComponent(SpriteRendererComponent.class);
        if (renderer == null) {
            System.err.println("[AnimationController] No se encontró SpriteRenderer en "
                + gameObject.getClass().getSimpleName()
                + ". AnimationController no tiene efecto.");
        }
        if (renderer != null) {
            renderer.setHandle(handle);
        }
        if (autoPlayKey != null) {
            play(autoPlayKey);
        }
    }

    /**
     * ── HRFC — Unified DeltaTime Migration ───────────────────────────────
     *
     * CAMBIO: Ahora recibe deltaTime y acumula tiempo en segundos.
     * La duración del frame se convierte de ticks → segundos @ 60 FPS.
     *
     * @param deltaTime tiempo del simulation step en segundos
     */
    @Override
    public void update(double deltaTime) {
        if (currentAnimation == null || renderer == null) return;

        // Duración efectiva del frame actual (ticks → segundos)
        int frameTicks = currentAnimation.ticksForFrame(frameIndex);
        double frameDuration = frameTicks / 60.0; // TODO: Animation should migrate to seconds

        elapsedTime += deltaTime;
        if (elapsedTime >= frameDuration) {
            elapsedTime = 0.0;
            advanceFrame();
        }

        // Empujar el frame actual al SpriteRenderer
        SpriteFrame frame = currentAnimation.getFrame(frameIndex);
        renderer.setCurrentFrame(frame);
    }

    // ── Avance de frame ──────────────────────────────────────────────────

    /**
     * Avanza al siguiente frame según el LoopMode y la dirección ping-pong.
     */
    private void advanceFrame() {
        if (currentAnimation.isFinished(frameIndex)) return;

        switch (currentAnimation.getLoopMode()) {
            case LOOP, ONCE -> {
                frameIndex = currentAnimation.nextIndex(frameIndex);
            }
            case PING_PONG -> {
                int effectiveEnd   = resolveLoopEnd();
                int effectiveStart = resolveLoopStart();

                if (pingPongForward) {
                    if (frameIndex >= effectiveEnd) {
                        pingPongForward = false;
                        frameIndex = currentAnimation.nextIndexReverse(frameIndex);
                    } else {
                        frameIndex = currentAnimation.nextIndex(frameIndex);
                    }
                } else {
                    if (frameIndex <= effectiveStart) {
                        pingPongForward = true;
                        frameIndex = currentAnimation.nextIndex(frameIndex);
                    } else {
                        frameIndex = currentAnimation.nextIndexReverse(frameIndex);
                    }
                }
            }
        }
    }

    private int resolveLoopEnd() {
        int le = currentAnimation.getLoopEnd();
        return (le >= 0 && le < currentAnimation.getFrameCount())
            ? le : currentAnimation.getFrameCount() - 1;
    }

    private int resolveLoopStart() {
        int ls = currentAnimation.getLoopStart();
        return (ls >= 0 && ls < currentAnimation.getFrameCount()) ? ls : 0;
    }

    // ── API pública ──────────────────────────────────────────────────────

    /**
     * Inicia o continúa la reproducción de la animación con la clave dada.
     *
     * Idempotente: si ya se está reproduciéndose esta animación, no resetea
     * el frame ni el tick. Seguro llamarlo cada frame desde el Renderer.
     *
     * @param key clave de la animación (ej: "idle", "walk_right")
     */
    public void play(String key) {
        if (key == null) return;
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
        elapsedTime      = 0.0;
        pingPongForward  = true;

        if (renderer != null) {
            renderer.setCurrentFrame(anim.getFirstFrame());
        }
    }

    /**
     * Fuerza el reinicio de la animación actual desde el frame 0.
     * Útil para animaciones ONCE que necesitan reproducirse de nuevo.
     */
    public void restart() {
        frameIndex      = 0;
        elapsedTime     = 0.0;
        pingPongForward = true;
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
        elapsedTime      = 0.0;
        pingPongForward  = true;
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
