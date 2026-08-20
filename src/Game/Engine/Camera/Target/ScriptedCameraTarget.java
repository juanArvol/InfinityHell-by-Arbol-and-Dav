package Game.Engine.Camera.Target;

import Game.Engine.GameMath.Logic2D.Vector2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Target de cámara controlado por una secuencia de keyframes.
 *
 * ── USO ───────────────────────────────────────────────────────────────────
 *   ScriptedCameraTarget script = ScriptedCameraTarget.builder()
 *       .moveTo(640, 300, 1.0)   // moverse a (640,300) en 1 segundo
 *       .hold(0.5)               // mantener posición 0.5 segundos
 *       .moveTo(200, 400, 0.75)  // luego a (200,400) en 0.75 segundos
 *       .build();
 *
 *   cameraSystem.pushTarget(script, 300); // prioridad muy alta
 *
 * ── INTERPOLACIÓN ─────────────────────────────────────────────────────────
 * La posición entre keyframes se interpola linealmente.
 * Para cinemáticas suaves, usar keyframes con duraciones largas (1-2 segundos).
 *
 * ── CICLO DE VIDA ─────────────────────────────────────────────────────────
 * El target expira cuando se completan todos los keyframes.
 * Si loop() es true, vuelve al primero indefinidamente.
 */
public final class ScriptedCameraTarget implements CameraTarget {

    // ── Keyframe ──────────────────────────────────────────────────────────

    public record Keyframe(double x, double y, double durationSeconds) {}

    // ── Estado ────────────────────────────────────────────────────────────

    private final List<Keyframe> keyframes;
    private final boolean        loop;
    private final int            priority;

    private int     currentKeyframe  = 0;
    private double  elapsedInKeyframe = 0.0;
    private boolean expired          = false;

    // ── Constructor ───────────────────────────────────────────────────────

    private ScriptedCameraTarget(List<Keyframe> keyframes, boolean loop, int priority) {
        if (keyframes.isEmpty())
            throw new IllegalArgumentException("ScriptedCameraTarget requiere al menos un keyframe");
        this.keyframes = List.copyOf(keyframes);
        this.loop      = loop;
        this.priority  = priority;
    }

    // ── Builder ───────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private final List<Keyframe> keyframes = new ArrayList<>();
        private boolean              loop      = false;
        private int                  priority  = 150;

        /** Mover la cámara hasta (x, y) durante durationSeconds segundos. */
        public Builder moveTo(double x, double y, double durationSeconds) {
            keyframes.add(new Keyframe(x, y, Math.max(0.001, durationSeconds)));
            return this;
        }

        /** Mantener la última posición durante durationSeconds segundos. */
        public Builder hold(double durationSeconds) {
            if (!keyframes.isEmpty()) {
                Keyframe last = keyframes.get(keyframes.size() - 1);
                keyframes.add(new Keyframe(last.x(), last.y(), Math.max(0.001, durationSeconds)));
            }
            return this;
        }

        /** La secuencia vuelve al principio cuando termina. */
        public Builder loop() {
            this.loop = true;
            return this;
        }

        public Builder priority(int p) {
            this.priority = p;
            return this;
        }

        public ScriptedCameraTarget build() {
            return new ScriptedCameraTarget(keyframes, loop, priority);
        }
    }

    // ── CameraTarget ──────────────────────────────────────────────────────

    @Override
    public Vector2D getPosition() {
        if (expired || keyframes.isEmpty()) return null;

        Keyframe kf = keyframes.get(currentKeyframe);

        if (currentKeyframe == 0) {
            // Primer keyframe: posición inicial directa
            return new Vector2D(kf.x(), kf.y());
        }

        // Interpolar desde el keyframe anterior
        Keyframe prev = keyframes.get(currentKeyframe - 1);
        double t = elapsedInKeyframe / kf.durationSeconds();
        t = Math.min(1.0, t);

        double x = prev.x() + (kf.x() - prev.x()) * t;
        double y = prev.y() + (kf.y() - prev.y()) * t;
        return new Vector2D(x, y);
    }

    @Override
    public void update(double deltaTime) {
        if (expired || keyframes.isEmpty()) return;

        Keyframe kf = keyframes.get(currentKeyframe);
        elapsedInKeyframe += deltaTime;

        if (elapsedInKeyframe >= kf.durationSeconds()) {
            elapsedInKeyframe = 0.0;
            currentKeyframe++;

            if (currentKeyframe >= keyframes.size()) {
                if (loop) {
                    currentKeyframe = 0;
                } else {
                    expired = true;
                }
            }
        }
    }

    @Override
    public boolean isExpired() { return expired; }

    @Override
    public int getPriority() { return priority; }
}
