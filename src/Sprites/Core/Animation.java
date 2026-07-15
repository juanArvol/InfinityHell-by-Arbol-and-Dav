package Sprites.Core;

/**
 * Animation — definición de una animación orientada a datos.
 *
 * ── DISEÑO ────────────────────────────────────────────────────────────────
 * Una animación es un array de SpriteFrame + velocidad + modo de loop.
 * No hay clases Java por animación. Todo se declara con datos.
 *
 * El estado de reproducción (frame actual, tick) es EXTERNO a esta clase.
 * Animation es inmutable y reutilizable entre múltiples instancias del mismo
 * tipo de entidad. El AnimationController mantiene el estado por entidad.
 *
 * ── USO ───────────────────────────────────────────────────────────────────
 *
 *   // Definición (una vez, en los Assets):
 *   Animation walkDere = Animation.of(frames, 8, LoopMode.LOOP);
 *
 *   // Consulta por el AnimationController (cada frame):
 *   SpriteFrame current = walkDere.getFrame(controller.getCurrentIndex());
 *
 * ── VELOCIDAD ─────────────────────────────────────────────────────────────
 * ticksPerFrame: cuántos ticks (updates) dura cada frame antes de avanzar.
 * A 30 FPS, ticksPerFrame=10 → ~3 cambios de frame por segundo.
 *
 * ── LOOP MODE ─────────────────────────────────────────────────────────────
 * LOOP:       vuelve al frame 0 al terminar (animaciones cíclicas: caminar).
 * ONCE:       se detiene en el último frame (animaciones de hit, muerte).
 * PING_PONG:  va y viene entre el primer y último frame.
 */
public final class Animation {

    public enum LoopMode { LOOP, ONCE, PING_PONG }

    private final SpriteFrame[] frames;
    private final int           ticksPerFrame;
    private final LoopMode      loopMode;

    // ── Constructor ──────────────────────────────────────────────────────

    /**
     * @param frames        array de frames de la animación (no null, no vacío)
     * @param ticksPerFrame ticks de engine por cada frame (velocidad de animación)
     * @param loopMode      comportamiento al llegar al último frame
     */
    public Animation(SpriteFrame[] frames, int ticksPerFrame, LoopMode loopMode) {
        if (frames == null || frames.length == 0) {
            throw new IllegalArgumentException("Animation: frames no puede ser null ni vacío");
        }
        if (ticksPerFrame <= 0) {
            throw new IllegalArgumentException("Animation: ticksPerFrame debe ser > 0");
        }
        this.frames        = frames.clone(); // copia defensiva
        this.ticksPerFrame = ticksPerFrame;
        this.loopMode      = loopMode;
    }

    // ── Fábricas de conveniencia ─────────────────────────────────────────

    /** Animación en loop (caminar, idle, etc.). */
    public static Animation loop(SpriteFrame[] frames, int ticksPerFrame) {
        return new Animation(frames, ticksPerFrame, LoopMode.LOOP);
    }

    /** Animación de un solo ciclo (golpe, muerte). */
    public static Animation once(SpriteFrame[] frames, int ticksPerFrame) {
        return new Animation(frames, ticksPerFrame, LoopMode.ONCE);
    }

    /** Animación ping-pong (respiración, hover). */
    public static Animation pingPong(SpriteFrame[] frames, int ticksPerFrame) {
        return new Animation(frames, ticksPerFrame, LoopMode.PING_PONG);
    }

    /**
     * Animación de frame único (sprite estático, compatible con el sistema de animación).
     * Permite tratar idle de un solo frame igual que una animación multi-frame.
     */
    public static Animation still(SpriteFrame frame) {
        return new Animation(new SpriteFrame[]{ frame }, 1, LoopMode.LOOP);
    }

    // ── Consulta de frames ────────────────────────────────────────────────

    /**
     * Obtiene el frame en el índice indicado.
     * Clampea al rango válido — nunca lanza excepción por índice.
     */
    public SpriteFrame getFrame(int index) {
        if (frames.length == 0) return SpriteFrame.empty();
        int safe = Math.max(0, Math.min(index, frames.length - 1));
        return frames[safe];
    }

    /** Primer frame de la animación. */
    public SpriteFrame getFirstFrame() { return frames[0]; }

    /** Último frame de la animación. */
    public SpriteFrame getLastFrame() { return frames[frames.length - 1]; }

    // ── Avance de estado ──────────────────────────────────────────────────

    /**
     * Calcula el índice del frame siguiente dado el índice actual y el tick actual.
     * Toda la lógica de avance vive aquí para mantener Animation como la
     * fuente de verdad de cómo avanza esa animación concreta.
     *
     * @param currentIndex índice del frame actual
     * @param currentTick  tick actual dentro del frame (0..ticksPerFrame-1)
     * @return índice del frame siguiente (puede ser igual si no es tiempo de avanzar)
     */
    public int nextIndex(int currentIndex, int currentTick) {
        if (currentTick < ticksPerFrame - 1) return currentIndex; // No es tiempo de avanzar

        // Es tiempo de avanzar al siguiente frame
        return switch (loopMode) {
            case LOOP      -> (currentIndex + 1) % frames.length;
            case ONCE      -> Math.min(currentIndex + 1, frames.length - 1);
            case PING_PONG -> {
                // No se implementa ping-pong completo aquí porque requiere
                // saber la dirección actual. El AnimationController maneja eso.
                // Por ahora, comportamiento igual a LOOP.
                yield (currentIndex + 1) % frames.length;
            }
        };
    }

    /** true si la animación ya llegó al final y no va a avanzar más (ONCE). */
    public boolean isFinished(int currentIndex) {
        return loopMode == LoopMode.ONCE && currentIndex >= frames.length - 1;
    }

    // ── Getters ───────────────────────────────────────────────────────────

    public int        getFrameCount()    { return frames.length;  }
    public int        getTicksPerFrame() { return ticksPerFrame;  }
    public LoopMode   getLoopMode()      { return loopMode;       }

    /** Duración total en ticks (frames * ticksPerFrame). */
    public int getTotalDuration() { return frames.length * ticksPerFrame; }

    @Override
    public String toString() {
        return "Animation[" + frames.length + " frames, "
               + ticksPerFrame + " ticks/frame, " + loopMode + "]";
    }
}
