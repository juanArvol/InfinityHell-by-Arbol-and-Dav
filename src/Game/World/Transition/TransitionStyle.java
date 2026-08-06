package Game.World.Transition;

/**
 * Estilo visual de una transición entre sectores.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * TransitionStyle define el COMPORTAMIENTO VISUAL de la transición,
 * separado completamente de la lógica de transferencia de entidades.
 *
 * ── EXTENSIBILIDAD ────────────────────────────────────────────────────────
 * TransitionStyle es una interfaz. Las implementaciones concretas definen
 * el efecto visual, la duración y la interpolación.
 *
 * Implementaciones base incluidas:
 *   TransitionStyle.INSTANT     → cambio inmediato, sin efecto visual
 *   TransitionStyle.FADE        → fade a negro y vuelta
 *   TransitionStyle.SCROLL      → desplazamiento continuo entre sectores
 *
 * Implementaciones futuras (extensibles sin modificar nada aquí):
 *   BlurTransitionStyle         → desenfoque durante la transición
 *   WhiteFlashTransitionStyle   → flash blanco
 *   CircleWipeTransitionStyle   → wipe circular (Zelda style)
 *   CustomShaderTransitionStyle → shader custom
 *   CinematicTransitionStyle    → control total de la cámara
 *
 * ── CICLO DE VIDA ─────────────────────────────────────────────────────────
 * Un TransitionStyle tiene tres fases:
 *   1. begin()     → se llama cuando la transición se inicia
 *   2. update()    → se llama cada tick durante la transición (para animaciones)
 *   3. isComplete()→ retorna true cuando el efecto visual terminó
 *
 * TransitionSystem ejecuta la lógica de transferencia (mover el objeto entre
 * mundos) cuando el style reporta que está listo para hacerlo (readyToTransfer()).
 *
 * ── SEPARACIÓN DE RESPONSABILIDADES ───────────────────────────────────────
 * La transferencia real de la entidad (remover del mundo origen, añadir al
 * mundo destino) la hace TransitionSystem. El style solo controla el efecto
 * visual y el timing.
 */
public interface TransitionStyle {

    // ── Ciclo de vida ─────────────────────────────────────────────────────

    /**
     * Notifica que la transición comienza.
     * El style puede inicializar timers, efectos o estado aquí.
     *
     * @param request el request de transición que dispara este estilo.
     */
    default void begin(TransitionRequest request) {}

    /**
     * Actualiza el estado del estilo (un tick).
     * Para estilos animados, incrementa contadores, aplica efectos, etc.
     */
    default void update() {}

    /**
     * True cuando el sistema debe ejecutar la transferencia real de la entidad.
     *
     * Para INSTANT: true inmediatamente.
     * Para FADE: true cuando el alpha alcanza el máximo (pantalla negra).
     * Para SCROLL: true después de que la cámara termina de desplazarse.
     */
    boolean readyToTransfer();

    /**
     * True cuando el efecto visual está completamente terminado
     * (la entidad ya fue transferida y el efecto post-transición finalizó).
     *
     * Para INSTANT: true inmediatamente.
     * Para FADE: true cuando el fade-in vuelve a alpha 0.
     */
    boolean isComplete();

    // ── Instancias base ───────────────────────────────────────────────────

    /**
     * Transición instantánea. Sin efecto visual.
     * La entidad aparece en el destino en el mismo frame.
     */
    TransitionStyle INSTANT = new TransitionStyle() {
        @Override public boolean readyToTransfer() { return true;  }
        @Override public boolean isComplete()      { return true;  }
        @Override public String  toString()        { return "INSTANT"; }
    };

    /**
     * Fade a negro (duración: 30 ticks = 1 segundo a 30fps).
     * La transferencia ocurre en el punto de máxima oscuridad.
     *
     * NOTA: el efecto visual del fade necesita ser implementado por el renderer.
     * TransitionStyle.FADE solo gestiona el timing; el renderer observa
     * TransitionSystem.getCurrentStyle() para aplicar el efecto.
     */
    static FadeTransitionStyle fade(int halfDurationTicks) {
        return new FadeTransitionStyle(halfDurationTicks);
    }

    /** Fade estándar de 15 ticks en cada dirección (total 30 ticks). */
    static FadeTransitionStyle fade() { return fade(15); }

    // ── FadeTransitionStyle ───────────────────────────────────────────────

    /**
     * Estilo de fade a negro.
     *
     * Fase 1 (ticks 0 → halfDuration): fade OUT (alpha 0→1).
     * Fase 2 (ticks halfDuration+1 → halfDuration*2): fade IN (alpha 1→0).
     * La transferencia ocurre al final de la fase 1.
     *
     * alpha() puede ser consultado por el renderer para aplicar el efecto.
     */
    final class FadeTransitionStyle implements TransitionStyle {

        private final int halfDuration;
        private int       tick           = 0;
        private boolean   transferDone   = false;

        FadeTransitionStyle(int halfDuration) {
            this.halfDuration = Math.max(1, halfDuration);
        }

        @Override
        public void update() {
            tick++;
        }

        @Override
        public boolean readyToTransfer() {
            return !transferDone && tick >= halfDuration;
        }

        @Override
        public boolean isComplete() {
            return tick >= halfDuration * 2;
        }

        /** Alpha del fade [0.0 = transparente, 1.0 = negro total]. */
        public float alpha() {
            if (tick <= halfDuration) {
                return (float) tick / halfDuration;
            } else {
                int remaining = tick - halfDuration;
                return 1.0f - (float) remaining / halfDuration;
            }
        }

        /** Llamar cuando la transferencia fue ejecutada. */
        public void markTransferDone() { transferDone = true; }
    }
}
