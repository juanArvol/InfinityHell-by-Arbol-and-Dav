package Sprites.Core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Animation — definición inmutable de una animación orientada a datos.
 *
 * ── HRFC-004: REDISEÑO COMPLETO ───────────────────────────────────────────
 * El sistema anterior dependía de arrays manuales de SpriteFrame pasados
 * desde fuera. El nuevo sistema introduce:
 *
 *   1. Animation.Builder — DSL declarativo para describir animaciones.
 *      El desarrollador describe QUÉ quiere; el Builder construye la secuencia.
 *
 *   2. Duración por frame — cada frame puede tener un tiempo individual.
 *      Si no se configura, usa la duración por defecto de la animación.
 *
 *   3. loopFromToHere — define el rango de loop interno (ping-pong parcial,
 *      repeat de secciones, intro + loop).
 *
 * ── COMPATIBILIDAD ────────────────────────────────────────────────────────
 * Las fábricas estáticas (loop, once, pingPong, still) siguen disponibles
 * y funcionan igual que antes. No se rompe ningún código existente.
 *
 * ── DISEÑO ────────────────────────────────────────────────────────────────
 * Animation es inmutable. El estado de reproducción (frame actual, tick)
 * sigue siendo EXTERNO — AnimationController lo mantiene por entidad.
 * Animation es reutilizable entre múltiples instancias.
 *
 * ── VELOCIDAD ─────────────────────────────────────────────────────────────
 * defaultTicksPerFrame: duración base cuando un frame no tiene duración propia.
 * ticksForFrame(i): duración efectiva del frame i (individual o base).
 *
 * ── LOOP MODE ─────────────────────────────────────────────────────────────
 * LOOP:       vuelve al frame loopStart al terminar (o 0 si no hay loopStart).
 * ONCE:       se detiene en el último frame.
 * PING_PONG:  va y viene entre loopStart y loopEnd (o 0 y length-1).
 *
 * ── USO — Builder ─────────────────────────────────────────────────────────
 *
 *   Animation attack = Animation.builder(frames)
 *       .frame(0, 12)           // rango 0..12
 *       .remove(7)              // elimina el frame 7
 *       .insertAfter(3, 15)     // inserta frame 15 después del 3
 *       .insertBefore(8, 18)    // inserta frame 18 antes del 8
 *       .frameDuration(3, 120)  // frame índice 3 dura 120 ticks
 *       .frameDuration(15, 180) // frame índice 15 dura 180 ticks
 *       .loopFromToHere(4, 10)  // el loop interno va de 4 a 10
 *       .build();
 */
public final class Animation {

    // ── Enums ─────────────────────────────────────────────────────────────

    public enum LoopMode { LOOP, ONCE, PING_PONG }

    // ── Campos ────────────────────────────────────────────────────────────

    /** Secuencia de frames en orden de reproducción. */
    private final SpriteFrame[] frames;

    /** Duración base en ticks por frame (cuando no hay duración individual). */
    private final int defaultTicksPerFrame;

    /** Modo de loop. */
    private final LoopMode loopMode;

    /**
     * Duraciones individuales por posición en la secuencia.
     * Clave: índice de posición en frames[]. Valor: ticks para ese frame.
     * Si el índice no aparece aquí, se usa defaultTicksPerFrame.
     */
    private final Map<Integer, Integer> frameDurations;

    /**
     * Inicio del rango de loop interno.
     * Cuando la animación llega al final, vuelve a este índice (no al 0).
     * -1 = no configurado (vuelve al frame 0).
     */
    private final int loopStart;

    /**
     * Fin del rango de loop interno.
     * La animación hace loop solo entre loopStart y loopEnd.
     * -1 = no configurado (loop hasta el último frame).
     */
    private final int loopEnd;

    // ── Constructor privado ───────────────────────────────────────────────

    private Animation(SpriteFrame[] frames,
                      int defaultTicksPerFrame,
                      LoopMode loopMode,
                      Map<Integer, Integer> frameDurations,
                      int loopStart,
                      int loopEnd) {
        if (frames == null || frames.length == 0) {
            throw new IllegalArgumentException("Animation: frames no puede ser null ni vacío");
        }
        if (defaultTicksPerFrame <= 0) {
            throw new IllegalArgumentException("Animation: defaultTicksPerFrame debe ser > 0");
        }
        this.frames               = frames.clone();
        this.defaultTicksPerFrame = defaultTicksPerFrame;
        this.loopMode             = loopMode;
        this.frameDurations       = Map.copyOf(frameDurations);
        this.loopStart            = loopStart;
        this.loopEnd              = loopEnd;
    }

    // ── Fábricas de conveniencia (API legacy — compatibilidad total) ──────

    /** Animación en loop a partir de un array de frames y velocidad base. */
    public static Animation loop(SpriteFrame[] frames, int ticksPerFrame) {
        return new Animation(frames, ticksPerFrame, LoopMode.LOOP, Map.of(), -1, -1);
    }

    /** Animación de un solo ciclo (golpe, muerte). */
    public static Animation once(SpriteFrame[] frames, int ticksPerFrame) {
        return new Animation(frames, ticksPerFrame, LoopMode.ONCE, Map.of(), -1, -1);
    }

    /** Animación ping-pong (respiración, hover). */
    public static Animation pingPong(SpriteFrame[] frames, int ticksPerFrame) {
        return new Animation(frames, ticksPerFrame, LoopMode.PING_PONG, Map.of(), -1, -1);
    }

    /** Animación de frame único (sprite estático). */
    public static Animation still(SpriteFrame frame) {
        return new Animation(new SpriteFrame[]{ frame }, 1, LoopMode.LOOP, Map.of(), -1, -1);
    }

    // ── Builder entry point ───────────────────────────────────────────────

    /**
     * Inicia la construcción de una animación con el banco de frames del sheet.
     *
     * El Builder trabaja con índices que referencian el array {@code bank}.
     * Los frames del bank nunca se reordenan en el bank — solo la secuencia
     * interna del Builder cambia.
     *
     * @param bank array de SpriteFrame del SpriteSheet (fuente de índices)
     */
    public static Builder builder(SpriteFrame[] bank) {
        return new Builder(bank);
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
    public SpriteFrame getLastFrame()  { return frames[frames.length - 1]; }

    // ── Duración ──────────────────────────────────────────────────────────

    /**
     * Duración efectiva del frame en la posición {@code index} de la secuencia.
     * Si el frame tiene duración individual, la usa; si no, usa la base.
     *
     * @param index posición en la secuencia de frames
     * @return ticks que debe durar ese frame
     */
    public int ticksForFrame(int index) {
        Integer custom = frameDurations.get(index);
        return (custom != null && custom > 0) ? custom : defaultTicksPerFrame;
    }

    // ── Avance de estado ──────────────────────────────────────────────────

    /**
     * Calcula el índice del frame siguiente dado el estado actual.
     *
     * A diferencia de la versión anterior (que recibía currentTick y comparaba
     * con un ticksPerFrame global), ahora el AnimationController lleva la cuenta
     * de ticks por frame y llama a nextIndex() solo cuando es momento de avanzar.
     *
     * @param currentIndex índice del frame actual
     * @return índice del siguiente frame
     */
    public int nextIndex(int currentIndex) {
        int effectiveEnd  = (loopEnd   >= 0 && loopEnd   < frames.length) ? loopEnd   : frames.length - 1;
        int effectiveStart = (loopStart >= 0 && loopStart < frames.length) ? loopStart : 0;

        return switch (loopMode) {
            case LOOP -> {
                if (currentIndex >= effectiveEnd) yield effectiveStart;
                yield currentIndex + 1;
            }
            case ONCE -> Math.min(currentIndex + 1, frames.length - 1);
            case PING_PONG -> {
                // La dirección la gestiona AnimationController; aquí avanzamos solo
                // en la dirección positiva. AnimationController detecta los extremos
                // y reversa la dirección usando nextIndexReverse().
                if (currentIndex >= effectiveEnd) yield effectiveEnd;
                yield currentIndex + 1;
            }
        };
    }

    /**
     * Calcula el índice del frame anterior (para PING_PONG en retroceso).
     *
     * @param currentIndex índice del frame actual
     * @return índice del frame anterior
     */
    public int nextIndexReverse(int currentIndex) {
        int effectiveStart = (loopStart >= 0 && loopStart < frames.length) ? loopStart : 0;
        if (currentIndex <= effectiveStart) return effectiveStart;
        return currentIndex - 1;
    }

    /**
     * Versión de compatibilidad: recibe currentTick para decidir si avanzar.
     *
     * @param currentIndex índice del frame actual
     * @param currentTick  tick actual dentro del frame (0-based)
     * @return índice del siguiente frame (puede ser igual si no es tiempo de avanzar)
     * @deprecated AnimationController debe llevar la cuenta de ticks por frame
     *             y llamar a {@link #nextIndex(int)} solo cuando corresponde.
     */
    @Deprecated(since = "hrfc-004")
    public int nextIndex(int currentIndex, int currentTick) {
        if (currentTick < defaultTicksPerFrame - 1) return currentIndex;
        return nextIndex(currentIndex);
    }

    /** true si la animación ya llegó al final y no va a avanzar más (ONCE). */
    public boolean isFinished(int currentIndex) {
        return loopMode == LoopMode.ONCE && currentIndex >= frames.length - 1;
    }

    // ── Getters ───────────────────────────────────────────────────────────

    public int      getFrameCount()          { return frames.length;          }
    public int      getDefaultTicksPerFrame() { return defaultTicksPerFrame;  }
    public LoopMode getLoopMode()             { return loopMode;              }
    public int      getLoopStart()            { return loopStart;             }
    public int      getLoopEnd()              { return loopEnd;               }

    /** true si algún frame tiene duración individual distinta de la base. */
    public boolean hasPerFrameDurations()    { return !frameDurations.isEmpty(); }

    /** Duración total mínima en ticks (usa siempre la duración base). */
    public int getTotalDuration() {
        int total = 0;
        for (int i = 0; i < frames.length; i++) {
            total += ticksForFrame(i);
        }
        return total;
    }

    @Override
    public String toString() {
        return "Animation[" + frames.length + " frames"
               + ", base=" + defaultTicksPerFrame + " ticks"
               + ", " + loopMode
               + (loopStart >= 0 ? ", loop=" + loopStart + ".." + loopEnd : "")
               + (hasPerFrameDurations() ? ", customDurations=" + frameDurations.size() : "")
               + "]";
    }

    // ═════════════════════════════════════════════════════════════════════
    // Builder
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Builder — DSL declarativo para construir animaciones.
     *
     * ── FILOSOFÍA ─────────────────────────────────────────────────────────
     * El Builder no es un generador de listas. Es un pequeño DSL especializado
     * en describir animaciones. El desarrollador expresa QUÉ comportamiento
     * desea y el Builder construye la secuencia consistente.
     *
     * ── REPRESENTACIÓN INTERNA ────────────────────────────────────────────
     * El Builder mantiene una lista mutable de índices (referenciando el bank).
     * Cada operación trabaja sobre el estado producido por la anterior.
     * No se reconstruye la lista desde cero en cada operación.
     *
     * ── OPERACIONES ACUMULATIVAS ──────────────────────────────────────────
     * Cada método modifica la secuencia actual y retorna this.
     * El orden de llamada importa: remove() elimina de la secuencia actual,
     * insertAfter() inserta en la posición actual, etc.
     *
     * ── OPERACIONES DISPONIBLES ───────────────────────────────────────────
     *
     *   .frame(n)             → añade el frame n a la secuencia
     *   .frame(from, to)      → añade los frames from..to (inclusive)
     *   .remove(n)            → elimina la primera ocurrencia del índice n
     *   .removeAll(n)         → elimina todas las ocurrencias del índice n
     *   .add(n)               → añade el frame n al final de la secuencia
     *   .insertAfter(ref, n)  → inserta n después de la primera ocurrencia de ref
     *   .insertBefore(ref, n) → inserta n antes de la primera ocurrencia de ref
     *   .frameDuration(pos,t) → asigna duración individual (ticks) a la posición pos
     *   .loopFromToHere(s,e)  → define el rango de loop interno
     *   .defaultDuration(t)   → cambia la duración base de todos los frames
     *   .loopMode(mode)       → establece el LoopMode
     *   .once()               → atajo para loopMode(ONCE)
     *   .loop()               → atajo para loopMode(LOOP)
     *   .pingPong()           → atajo para loopMode(PING_PONG)
     */
    public static final class Builder {

        /** Banco de frames fuente (el array del SpriteSheet). */
        private final SpriteFrame[] bank;

        /** Secuencia de índices que conforma la animación. Mutable durante la construcción. */
        private final List<Integer> sequence = new ArrayList<>();

        /** Duraciones individuales por posición en la secuencia. */
        private final Map<Integer, Integer> frameDurations = new HashMap<>();

        /** Duración base por defecto. */
        private int defaultTicksPerFrame = 6;

        /** Modo de loop. */
        private LoopMode loopMode = LoopMode.LOOP;

        /** Inicio del rango de loop interno (-1 = no configurado). */
        private int loopStart = -1;

        /** Fin del rango de loop interno (-1 = no configurado). */
        private int loopEnd   = -1;

        private Builder(SpriteFrame[] bank) {
            if (bank == null || bank.length == 0) {
                throw new IllegalArgumentException("Animation.Builder: bank no puede ser null ni vacío");
            }
            this.bank = bank;
        }

        // ── Construcción de la secuencia ──────────────────────────────────

        /**
         * Añade un único frame a la secuencia.
         *
         *   .frame(4)  →  secuencia: [4]
         */
        public Builder frame(int frameIndex) {
            validateIndex(frameIndex);
            sequence.add(frameIndex);
            return this;
        }

        /**
         * Añade un rango de frames (inclusivo en ambos extremos).
         *
         *   .frame(0, 8)  →  secuencia: [0, 1, 2, 3, 4, 5, 6, 7, 8]
         */
        public Builder frame(int from, int to) {
            if (from <= to) {
                for (int i = from; i <= to; i++) { validateIndex(i); sequence.add(i); }
            } else {
                for (int i = from; i >= to; i--) { validateIndex(i); sequence.add(i); }
            }
            return this;
        }

        /**
         * Añade el frame al final de la secuencia actual.
         * Equivale a .frame(n) cuando la secuencia ya tiene contenido.
         *
         *   .frame(0, 8).add(10).add(11)  →  [0..8, 10, 11]
         */
        public Builder add(int frameIndex) {
            return frame(frameIndex);
        }

        /**
         * Elimina la PRIMERA ocurrencia del índice {@code frameIndex} en la secuencia.
         *
         *   .frame(0, 8).remove(2).remove(5)  →  [0,1,3,4,6,7,8]
         */
        public Builder remove(int frameIndex) {
            for (int i = 0; i < sequence.size(); i++) {
                if (sequence.get(i) == frameIndex) {
                    sequence.remove(i);
                    // Ajustar frameDurations: las posiciones > i se desplazan -1
                    shiftDurationsAfterRemove(i);
                    return this;
                }
            }
            return this; // No lanzar error si no existe — operación idempotente
        }

        /**
         * Elimina TODAS las ocurrencias del índice {@code frameIndex} en la secuencia.
         */
        public Builder removeAll(int frameIndex) {
            int pos = 0;
            while (pos < sequence.size()) {
                if (sequence.get(pos) == frameIndex) {
                    sequence.remove(pos);
                    shiftDurationsAfterRemove(pos);
                } else {
                    pos++;
                }
            }
            return this;
        }

        /**
         * Inserta {@code frameIndex} DESPUÉS de la primera ocurrencia de {@code after}.
         *
         *   .frame(0, 8).insertAfter(3, 10)  →  [0,1,2,3,10,4,5,6,7,8]
         */
        public Builder insertAfter(int after, int frameIndex) {
            validateIndex(frameIndex);
            int pos = findFirst(after);
            if (pos >= 0) {
                int insertPos = pos + 1;
                sequence.add(insertPos, frameIndex);
                shiftDurationsAfterInsert(insertPos);
            } else {
                // Si la referencia no existe, añadir al final
                sequence.add(frameIndex);
            }
            return this;
        }

        /**
         * Inserta {@code frameIndex} ANTES de la primera ocurrencia de {@code before}.
         *
         *   .frame(0, 8).insertBefore(5, 11)  →  [0,1,2,3,4,11,5,6,7,8]
         */
        public Builder insertBefore(int before, int frameIndex) {
            validateIndex(frameIndex);
            int pos = findFirst(before);
            if (pos >= 0) {
                sequence.add(pos, frameIndex);
                shiftDurationsAfterInsert(pos);
            } else {
                sequence.add(frameIndex);
            }
            return this;
        }

        // ── Duraciones individuales ───────────────────────────────────────

        /**
         * Asigna una duración individual (en ticks) al frame en la posición
         * {@code sequencePos} de la secuencia actual.
         *
         * El índice referencia la POSICIÓN en la secuencia (0-based), no el
         * índice del frame en el bank. Esto permite que el mismo frame aparezca
         * dos veces con distintas duraciones.
         *
         *   .frame(0, 8)
         *   .frameDuration(0, 120)  // posición 0 → frame 0 del bank, dura 120 ticks
         *   .frameDuration(3, 90)   // posición 3 → frame 3 del bank, dura 90 ticks
         */
        public Builder frameDuration(int sequencePos, int ticks) {
            if (ticks <= 0) throw new IllegalArgumentException(
                "Animation.Builder.frameDuration: ticks debe ser > 0");
            frameDurations.put(sequencePos, ticks);
            return this;
        }

        /**
         * Cambia la duración base (ticks por frame) para todos los frames
         * que no tengan duración individual asignada.
         */
        public Builder defaultDuration(int ticks) {
            if (ticks <= 0) throw new IllegalArgumentException(
                "Animation.Builder.defaultDuration: ticks debe ser > 0");
            this.defaultTicksPerFrame = ticks;
            return this;
        }

        // ── Loop interno ──────────────────────────────────────────────────

        /**
         * Define el rango de loop interno.
         *
         * Cuando la animación llega al frame {@code loopEnd}, vuelve a
         * {@code loopStart} en lugar de ir al frame 0.
         * Útil para intro + loop (reproduce frames 0..3 una vez, luego
         * loops sobre 4..10 indefinidamente).
         *
         *   .frame(0, 12).loopFromToHere(4, 10)
         *
         * @param loopStart primer frame del rango de loop (posición en secuencia)
         * @param loopEnd   último frame del rango de loop (posición en secuencia)
         */
        public Builder loopFromToHere(int loopStart, int loopEnd) {
            this.loopStart = loopStart;
            this.loopEnd   = loopEnd;
            return this;
        }

        // ── Modo de loop ──────────────────────────────────────────────────

        /** Establece el LoopMode explícitamente. */
        public Builder loopMode(LoopMode mode) {
            this.loopMode = mode;
            return this;
        }

        /** Atajo: animación en loop continuo. */
        public Builder loop()     { return loopMode(LoopMode.LOOP);      }

        /** Atajo: animación de un solo ciclo. */
        public Builder once()     { return loopMode(LoopMode.ONCE);      }

        /** Atajo: animación ping-pong. */
        public Builder pingPong() { return loopMode(LoopMode.PING_PONG); }

        // ── Build ─────────────────────────────────────────────────────────

        /**
         * Construye la Animation inmutable a partir del estado actual del Builder.
         *
         * @throws IllegalStateException si la secuencia está vacía
         */
        public Animation build() {
            if (sequence.isEmpty()) {
                throw new IllegalStateException(
                    "Animation.Builder.build(): la secuencia está vacía. "
                    + "Llamar .frame() antes de .build().");
            }

            // Resolver la secuencia de frames desde el bank
            SpriteFrame[] resolved = new SpriteFrame[sequence.size()];
            for (int i = 0; i < sequence.size(); i++) {
                resolved[i] = bank[sequence.get(i)];
            }

            // Clonar el mapa de duraciones para evitar mutación post-build
            Map<Integer, Integer> durCopy = new HashMap<>(frameDurations);

            return new Animation(resolved, defaultTicksPerFrame, loopMode, durCopy, loopStart, loopEnd);
        }

        // ── Utilidades internas ───────────────────────────────────────────

        /** Índice de la primera aparición de frameIndex en la secuencia. -1 si no existe. */
        private int findFirst(int frameIndex) {
            for (int i = 0; i < sequence.size(); i++) {
                if (sequence.get(i) == frameIndex) return i;
            }
            return -1;
        }

        /** Valida que frameIndex esté dentro del bank. */
        private void validateIndex(int frameIndex) {
            if (frameIndex < 0 || frameIndex >= bank.length) {
                throw new IndexOutOfBoundsException(
                    "Animation.Builder: frameIndex " + frameIndex
                    + " fuera del rango del bank [0," + bank.length + ")");
            }
        }

        /**
         * Ajusta el mapa frameDurations cuando se elimina un elemento en la posición
         * {@code removedPos}. Las posiciones mayores se desplazan -1.
         */
        private void shiftDurationsAfterRemove(int removedPos) {
            Map<Integer, Integer> adjusted = new HashMap<>();
            for (Map.Entry<Integer, Integer> e : frameDurations.entrySet()) {
                int pos = e.getKey();
                if (pos < removedPos) {
                    adjusted.put(pos, e.getValue());
                } else if (pos > removedPos) {
                    adjusted.put(pos - 1, e.getValue());
                }
                // pos == removedPos → se descarta (la posición ya no existe)
            }
            frameDurations.clear();
            frameDurations.putAll(adjusted);
        }

        /**
         * Ajusta el mapa frameDurations cuando se inserta un elemento en la posición
         * {@code insertedPos}. Las posiciones >= insertedPos se desplazan +1.
         */
        private void shiftDurationsAfterInsert(int insertedPos) {
            Map<Integer, Integer> adjusted = new HashMap<>();
            for (Map.Entry<Integer, Integer> e : frameDurations.entrySet()) {
                int pos = e.getKey();
                if (pos < insertedPos) {
                    adjusted.put(pos, e.getValue());
                } else {
                    adjusted.put(pos + 1, e.getValue());
                }
            }
            frameDurations.clear();
            frameDurations.putAll(adjusted);
        }
    }
}
