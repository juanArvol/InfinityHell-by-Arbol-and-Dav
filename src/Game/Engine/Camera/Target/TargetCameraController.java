package Game.Engine.Camera.Target;

import Game.Engine.Camera.CameraController;
import Game.Engine.Camera.GameCamera;
import Game.Engine.GameMath.Logic2D.Vector2D;

/**
 * Controlador de cámara que delega en un CameraTarget para obtener
 * la posición objetivo y sigue a ese target con lerp suave.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * Conecta el sistema de CameraTargets con el CameraController del Engine.
 * Reemplaza a FollowCameraController cuando se quiere usar el sistema
 * de targets en lugar de un Supplier<Vector2D> directo.
 *
 * FollowCameraController sigue disponible para casos simples.
 * TargetCameraController es el camino recomendado cuando se usan
 * PriorityCameraTarget, WeightedCameraTarget o CompositeCameraTarget.
 *
 * ── USO ───────────────────────────────────────────────────────────────────
 *   PriorityCameraTarget targets = new PriorityCameraTarget();
 *   targets.add(new PlayerCameraTarget(() -> player.getTransform().getPosition()));
 *
 *   CameraController controller = new TargetCameraController(targets, 0.10f);
 *   worldManager.setCameraController(controller);
 *
 *   // Añadir target cinemático en tiempo de ejecución:
 *   targets.add(ScriptedCameraTarget.builder().moveTo(640, 300, 60).build());
 *   // La cámara automáticamente sigue el script y vuelve al player al expirar.
 */
public final class TargetCameraController implements CameraController {

    private final CameraTarget target;
    private final float        lerpFactor;

    /**
     * @param target     el CameraTarget (puede ser PriorityCameraTarget con múltiples candidatos)
     * @param lerpFactor factor de lerp [0.0, 1.0]; típicamente 0.08–0.15
     */
    public TargetCameraController(CameraTarget target, float lerpFactor) {
        if (target == null) throw new IllegalArgumentException("target cannot be null");
        if (lerpFactor <= 0 || lerpFactor > 1)
            throw new IllegalArgumentException("lerpFactor must be in (0.0, 1.0]");
        this.target     = target;
        this.lerpFactor = lerpFactor;
    }

    public TargetCameraController(CameraTarget target) {
        this(target, 0.10f);
    }

    @Override
    public void update(GameCamera camera, double deltaTime) {
        target.update();

        Vector2D pos = target.getPosition();
        if (pos == null) return;

        camera.lerpCenterOn(pos.getX(), pos.getY(), lerpFactor);
    }

    public CameraTarget getTarget() { return target; }
}
