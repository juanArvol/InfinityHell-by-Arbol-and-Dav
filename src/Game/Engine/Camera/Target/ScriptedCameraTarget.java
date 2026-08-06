package Game.Engine.Camera.Target;

import Game.Engine.GameMath.Logic2D.Vector2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Target de cámara controlado por una secuencia de keyframes.
 *
 * ── USO ───────────────────────────────────────────────────────────────────
 *   ScriptedCameraTarget script = ScriptedCameraTarget.builder()
 *       .moveTo(640, 300, 60)   // moverse a (640,300) en 60 ticks
 *       .hold(30)               // mantener posición 30 ticks
 *       .moveTo(200, 400, 45)   // luego a (200,400) en 45 ticks
 *       .build();
 *
 *   cameraSystem.pushTarget(script, 300); // prioridad muy alta
 *
 * ── INTERPOLACIÓN ─────────────────────────────────────────────────────────
 * La posición entre keyframes se interpola linealmente.
 * Para cinemáticas suaves, usar keyframes con duraciones largas (60-120 ticks).
 *
 * ── CICLO DE VIDA ─────────────────────────────────────────────────────────
 * El target expira cuando se completan todos los keyframes.
 * Si loop() es true, vuelve al primero indefinidamente.
 */
public final class ScriptedCameraTarget implements CameraTarget {

    // ── Keyframe ──────────────────────────────────────────────────────────

    public record Keyframe(double x, double y, int durationTicks) {}

    // ── Estado ────────────────────────────────────────────────────────────

    private final List<Keyframe> keyframes;
    private final boolean        loop;
    private final int            priority;

    private int     currentKeyframe = 0;
    private int     tickInKeyframe  = 0;
    private boolean expired         = false;

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

        /** Mover la cámara hasta (x, y) durante durationTicks ticks. */
        public Builder moveTo(double x, double y, int durationTicks) {
            keyframes.add(new Keyframe(x, y, Math.max(1, durationTicks)));
            return this;
        }

        /** Mantener la última posición durante durationTicks ticks. */
        public Builder hold(int durationTicks) {
            if (!keyframes.isEmpty()) {
                Keyframe last = keyframes.get(keyframes.size() - 1);
                keyframes.add(new Keyframe(last.x(), last.y(), Math.max(1, durationTicks)));
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
        double t = (double) tickInKeyframe / kf.durationTicks();
        t = Math.min(1.0, t);

        double x = prev.x() + (kf.x() - prev.x()) * t;
        double y = prev.y() + (kf.y() - prev.y()) * t;
        return new Vector2D(x, y);
    }

    @Override
    public void update() {
        if (expired || keyframes.isEmpty()) return;

        Keyframe kf = keyframes.get(currentKeyframe);
        tickInKeyframe++;

        if (tickInKeyframe >= kf.durationTicks()) {
            tickInKeyframe = 0;
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
