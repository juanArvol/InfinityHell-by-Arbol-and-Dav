package Game.World.Core;

import Game.Engine.Camera.CameraController;
import Game.Engine.Camera.GameCamera;
import Game.Engine.Camera.Target.CameraTarget;
import Game.Engine.Camera.Target.PlayerCameraTarget;
import Game.Engine.Camera.Target.PriorityCameraTarget;
import Game.Engine.Camera.Target.TargetCameraController;
import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Engine.GameObjects;

/**
 * Sistema de cámara del mundo.
 *
 * ── ETAPA 6: Cámara global sin chunk-local bounds ni snap ────────────────
 *
 * ANTES:
 *   - setWorldBounds(logicalWidth, logicalHeight) se llamaba 3 veces por sector:
 *     constructor, post-transición, resize. Limitaba la cámara al sector actual.
 *   - onSectorChanged() hacía snap instantáneo al cruzar un límite de chunk.
 *     Eso producía un salto visual brusco.
 *
 * AHORA:
 *   - La cámara opera en coordenadas globales continuas.
 *   - No hay chunk-local bounds. WorldBoundsConstraint se deshabilita por defecto.
 *   - onSectorChanged() es un NO-OP — no hay salto de cámara al cruzar chunks.
 *   - setWorldBounds(width, height) solo aplica si se quieren bounds explícitos
 *     para mundos finitos (llamar desde código de gameplay si es necesario).
 *   - Para mundos infinitos/expandibles: clearWorldBounds() (por defecto).
 *
 * ── RESULTADO ────────────────────────────────────────────────────────────
 *   - La cámara puede moverse de x=1279 a x=1280 sin ningún efecto visible.
 *   - No hay "escena A → escena B" desde el punto de vista de la cámara.
 *   - Los bounds globales reales del mundo pueden configurarse externamente.
 */
public final class CameraSystem {

    private final GameCamera           camera;
    private final PriorityCameraTarget priorityTarget;
    private CameraController           cameraController;

    public CameraSystem(int virtualWidth, int virtualHeight) {
        this.camera         = new GameCamera(virtualWidth, virtualHeight);
        this.priorityTarget = new PriorityCameraTarget();
        this.cameraController = new TargetCameraController(priorityTarget, 0.10f);

        // ── ETAPA 6: Sin chunk-local bounds por defecto ────────────────────
        // La cámara opera en coordenadas globales continuas.
        // clearWorldBounds() deshabilita WorldBoundsConstraint.
        // Para mundos finitos, llamar setGlobalWorldBounds(minX, minY, maxX, maxY).
        camera.clearWorldBounds();
    }

    // ── Update ────────────────────────────────────────────────────────────

    public void update(double deltaTime) {
        if (cameraController != null) {
            cameraController.update(camera, deltaTime);
        }
        camera.commitFrame();
    }

    // ── Tracking ─────────────────────────────────────────────────────────

    public void setTrackedObject(GameObjects obj) {
        if (obj == null) return;
        PlayerCameraTarget playerTarget = PlayerCameraTarget.of(obj);
        priorityTarget.add(playerTarget);
        var pos = obj.getTransform().getPosition();
        camera.centerOn(pos.getX(), pos.getY());
    }

    /**
     * Notificación de cambio de sector — ahora es un NO-OP.
     *
     * En el nuevo modelo, cruzar un límite de chunk es transparente para la cámara.
     * La cámara sigue al player con lerp continuo sin saltos.
     *
     * @deprecated Este método no hace nada. Queda para compatibilidad de compilación
     *             hasta Etapa 9, donde se eliminará junto con el TransitionService legacy.
     *
     * @param newPosition ignorado
     */
    @Deprecated(forRemoval = true)
    public void onSectorChanged(Vector2D newPosition) {
        // Intencionalmente vacío.
        // En el modelo anterior hacía camera.centerOn() al cruzar sector → snap visual.
        // En el nuevo modelo: la cámara sigue al player con lerp continuo, sin saltos.
    }

    // ── Targets ──────────────────────────────────────────────────────────

    public PriorityCameraTarget getTargets()            { return priorityTarget; }
    public void addTarget(CameraTarget target)          { priorityTarget.add(target); }
    public void setCameraController(CameraController c) { this.cameraController = c; }
    public CameraController getCameraController()       { return cameraController; }

    // ── Resize ────────────────────────────────────────────────────────────

    public void onVirtualResize(int newVirtualWidth, int newVirtualHeight) {
        camera.onVirtualResolutionChanged(newVirtualWidth, newVirtualHeight);
    }

    /**
     * Configura bounds del mundo en coordenadas GLOBALES para mundos finitos.
     *
     * Para mundos infinitos o expandibles, no llamar este método
     * (la cámara se mueve libremente).
     *
     * @param minX  borde izquierdo global del mundo
     * @param minY  borde superior global del mundo
     * @param maxX  borde derecho global del mundo
     * @param maxY  borde inferior global del mundo
     */
    public void setGlobalWorldBounds(double minX, double minY, double maxX, double maxY) {
        camera.getConstraints().clear();
        var constraint = new Game.Engine.Camera.Constraint.WorldBoundsConstraint();
        constraint.update(minX, minY, maxX, maxY);
        camera.getConstraints().add(constraint);
        camera.commitFrame();
    }

    /**
     * Configura bounds simétricos del mundo (origen en 0,0).
     * Para compatibilidad con código que llama setWorldBounds(w, h).
     *
     * @deprecated Usar setGlobalWorldBounds(minX,minY,maxX,maxY) o clearWorldBounds().
     *             Este método asume que el mundo empieza en (0,0), lo cual solo
     *             es correcto si el player no puede ir a chunks con coords negativas.
     */
    @Deprecated(forRemoval = true)
    public void setWorldBounds(int worldWidth, int worldHeight) {
        // En Etapa 4, WorldManager aún llama esto 3 veces por sector.
        // En Etapa 6, lo ignoramos — la cámara ya no tiene chunk-local bounds.
        // Si se quieren bounds reales, usar setGlobalWorldBounds().
    }

    // ── Acceso ────────────────────────────────────────────────────────────

    public GameCamera getCamera() { return camera; }
}
