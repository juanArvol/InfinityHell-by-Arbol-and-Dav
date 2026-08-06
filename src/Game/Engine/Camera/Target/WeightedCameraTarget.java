package Game.Engine.Camera.Target;

import Game.Engine.GameMath.Logic2D.Vector2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Target de cámara que pondera la posición de múltiples targets por peso.
 *
 * ── USO ───────────────────────────────────────────────────────────────────
 *   // 70% player, 30% cursor del ratón:
 *   WeightedCameraTarget weighted = new WeightedCameraTarget()
 *       .add(playerTarget,  0.70f)
 *       .add(cursorTarget,  0.30f);
 *
 *   // Transición gradual: a medida que un boss aparece, la cámara
 *   // se desplaza desde el player (peso decrece) hacia el boss (peso crece):
 *   weighted.setWeight(playerTarget, 0.4f);
 *   weighted.setWeight(bossTarget,   0.6f);
 *
 * ── NORMALIZACIÓN ─────────────────────────────────────────────────────────
 * Los pesos se normalizan automáticamente: no es necesario que sumen 1.0.
 * Un peso de 0.0 excluye efectivamente ese target del promedio.
 */
public final class WeightedCameraTarget implements CameraTarget {

    private record WeightedEntry(CameraTarget target, float weight) {}

    private final List<WeightedEntry> entries  = new ArrayList<>();
    private final int                 priority;

    public WeightedCameraTarget() {
        this.priority = 100;
    }

    public WeightedCameraTarget(int priority) {
        this.priority = priority;
    }

    public WeightedCameraTarget add(CameraTarget target, float weight) {
        entries.add(new WeightedEntry(target, Math.max(0.0f, weight)));
        return this;
    }

    public void setWeight(CameraTarget target, float weight) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).target() == target) {
                entries.set(i, new WeightedEntry(target, Math.max(0.0f, weight)));
                return;
            }
        }
    }

    @Override
    public Vector2D getPosition() {
        double totalWeight = 0;
        double sumX        = 0;
        double sumY        = 0;

        for (WeightedEntry entry : entries) {
            if (!entry.target().isActive() || entry.target().isExpired()) continue;
            if (entry.weight() <= 0) continue;

            Vector2D pos = entry.target().getPosition();
            if (pos == null) continue;

            sumX        += pos.getX() * entry.weight();
            sumY        += pos.getY() * entry.weight();
            totalWeight += entry.weight();
        }

        if (totalWeight == 0) return null;
        return new Vector2D(sumX / totalWeight, sumY / totalWeight);
    }

    @Override
    public void update() {
        entries.removeIf(e -> e.target().isExpired());
        for (WeightedEntry e : entries) e.target().update();
    }

    @Override
    public boolean isExpired() {
        if (entries.isEmpty()) return true;
        return entries.stream().allMatch(e -> !e.target().isActive() || e.target().isExpired());
    }

    @Override
    public int getPriority() { return priority; }
}
