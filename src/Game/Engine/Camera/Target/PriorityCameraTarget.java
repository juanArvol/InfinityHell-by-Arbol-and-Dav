package Game.Engine.Camera.Target;

import Game.Engine.GameMath.Logic2D.Vector2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Target de cámara que gestiona múltiples candidatos por prioridad.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * PriorityCameraTarget mantiene una pila de targets candidatos y delega
 * en el de mayor prioridad que esté activo y no haya expirado.
 *
 * Es el punto de composición central del sistema de cámara:
 *   - El player siempre está registrado con prioridad base (100).
 *   - Un script cinemático se registra temporalmente con prioridad alta (200).
 *   - Al expirar el script, la cámara vuelve automáticamente al player.
 *   - Un boss puede registrar un ProjectileCameraTarget con prioridad media (150).
 *
 * ── CICLO DE VIDA ─────────────────────────────────────────────────────────
 * Los targets expirados se eliminan automáticamente en cada update().
 * Si todos los targets expiran, getPosition() retorna null.
 *
 * ── USO ───────────────────────────────────────────────────────────────────
 *   PriorityCameraTarget priority = new PriorityCameraTarget();
 *   priority.add(playerTarget);    // prioridad 100 (del target)
 *   priority.add(cinematicTarget); // prioridad 200 (del target)
 *   cameraSystem.setTarget(priority);
 *
 *   // Más tarde, cuando la cinemática expira, la cámara vuelve al player
 *   // automáticamente sin ninguna intervención del código de gameplay.
 */
public final class PriorityCameraTarget implements CameraTarget {

    private final List<CameraTarget> candidates  = new ArrayList<>();
    private       CameraTarget       activeTarget = null;

    public PriorityCameraTarget() {}

    // ── Gestión de candidatos ─────────────────────────────────────────────

    /**
     * Añade un target candidato. La prioridad la decide el target (getPriority()).
     */
    public PriorityCameraTarget add(CameraTarget target) {
        candidates.add(target);
        return this;
    }

    /**
     * Elimina un target candidato.
     */
    public PriorityCameraTarget remove(CameraTarget target) {
        candidates.remove(target);
        if (activeTarget == target) activeTarget = null;
        return this;
    }

    /**
     * Limpia todos los candidatos.
     */
    public void clear() {
        candidates.clear();
        activeTarget = null;
    }

    // ── CameraTarget ──────────────────────────────────────────────────────

    @Override
    public Vector2D getPosition() {
        if (activeTarget == null) return null;
        return activeTarget.getPosition();
    }

    @Override
    public void update() {
        // Eliminar expirados
        candidates.removeIf(t -> {
            if (t.isExpired()) {
                if (t == activeTarget) activeTarget = null;
                return true;
            }
            return false;
        });

        // Actualizar todos
        for (CameraTarget t : candidates) t.update();

        // Seleccionar el activo de mayor prioridad
        CameraTarget best = candidates.stream()
            .filter(CameraTarget::isActive)
            .filter(t -> !t.isExpired())
            .max(Comparator.comparingInt(CameraTarget::getPriority))
            .orElse(null);

        // Notificar al nuevo target si cambió
        if (best != activeTarget) {
            activeTarget = best;
            if (activeTarget != null) activeTarget.onSelected();
        }
    }

    @Override
    public boolean isExpired() {
        return candidates.isEmpty();
    }

    @Override
    public boolean isActive() {
        return activeTarget != null;
    }

    /** Retorna el target activo actualmente (para debug/monitoreo). */
    public CameraTarget getActiveTarget() { return activeTarget; }

    /** Número de candidatos registrados. */
    public int getCandidateCount() { return candidates.size(); }
}
