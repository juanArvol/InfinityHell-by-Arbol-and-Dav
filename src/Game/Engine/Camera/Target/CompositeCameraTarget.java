package Game.Engine.Camera.Target;

import Game.Engine.GameMath.Logic2D.Vector2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Target de cámara que combina múltiples targets por promedio simple.
 *
 * ── USO ───────────────────────────────────────────────────────────────────
 *   // La cámara se posiciona entre el jugador y el cursor del ratón:
 *   CameraTarget composite = CompositeCameraTarget.of(
 *       playerTarget,
 *       cursorTarget
 *   );
 *
 *   // La cámara se centra entre dos jugadores en co-op:
 *   CameraTarget coop = new CompositeCameraTarget()
 *       .add(PlayerCameraTarget.of(player1))
 *       .add(PlayerCameraTarget.of(player2));
 *
 * ── COMPORTAMIENTO ────────────────────────────────────────────────────────
 * Calcula el centroide (promedio simple) de todos los targets activos.
 * Los targets expirados se eliminan automáticamente.
 * Si todos los targets expiran o están inactivos, retorna null.
 *
 * Para ponderación diferenciada usar WeightedCameraTarget.
 */
public final class CompositeCameraTarget implements CameraTarget {

    private final List<CameraTarget> targets  = new ArrayList<>();
    private final int                priority;

    public CompositeCameraTarget() {
        this.priority = 100;
    }

    public CompositeCameraTarget(int priority) {
        this.priority = priority;
    }

    public static CompositeCameraTarget of(CameraTarget... targets) {
        CompositeCameraTarget c = new CompositeCameraTarget();
        for (CameraTarget t : targets) c.add(t);
        return c;
    }

    public CompositeCameraTarget add(CameraTarget target) {
        targets.add(target);
        return this;
    }

    public CompositeCameraTarget remove(CameraTarget target) {
        targets.remove(target);
        return this;
    }

    @Override
    public Vector2D getPosition() {
        double sumX   = 0;
        double sumY   = 0;
        int    count  = 0;

        for (CameraTarget t : targets) {
            if (!t.isActive() || t.isExpired()) continue;
            Vector2D pos = t.getPosition();
            if (pos == null) continue;
            sumX += pos.getX();
            sumY += pos.getY();
            count++;
        }

        if (count == 0) return null;
        return new Vector2D(sumX / count, sumY / count);
    }

    @Override
    public void update() {
        targets.removeIf(CameraTarget::isExpired);
        for (CameraTarget t : targets) t.update();
    }

    @Override
    public boolean isExpired() {
        if (targets.isEmpty()) return true;
        return targets.stream().allMatch(t -> !t.isActive() || t.isExpired());
    }

    @Override
    public int getPriority() { return priority; }
}
