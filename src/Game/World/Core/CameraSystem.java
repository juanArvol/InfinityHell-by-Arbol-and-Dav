package Game.World.Core;

import Game.Engine.Camera.CameraController;
import Game.Engine.Camera.FollowCameraController;
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
 * ── EXTRACCIÓN DESDE WorldManager ─────────────────────────────────────────
 * La gestión de la cámara fue extraída de WorldManager para cumplir SRP.
 * WorldManager delegaba la inicialización del CameraController directamente
 * en setTrackedObject(), mezclando responsabilidades de mundo y de cámara.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * CameraSystem:
 *   - Posee el GameCamera del Engine.
 *   - Gestiona el CameraController activo.
 *   - Expone el PriorityCameraTarget para añadir/quitar targets en runtime.
 *   - Propaga commitFrame() al final de cada tick.
 *   - Gestiona snap inicial y reposicionamiento tras transición de sector.
 *
 * ── LO QUE NO HACE ────────────────────────────────────────────────────────
 * - No conoce World ni WorldManager.
 * - No decide qué mundo es el activo.
 * - No produce ningún render.
 *
 * ── INTEGRACIÓN CON WorldManager ─────────────────────────────────────────
 * WorldManager posee un CameraSystem. En cada update():
 *   1. world.update()
 *   2. cameraSystem.update(deltaTime)
 *   3. prewarmService.update(tracked, w, h)
 *   4. transitionService.processTransitions(...)
 *   5. Si hubo transición: cameraSystem.onSectorChanged(newPos)
 *
 * ── TARGET PRIORITY SYSTEM ────────────────────────────────────────────────
 * CameraSystem usa internamente un PriorityCameraTarget.
 * El player siempre está registrado con prioridad 100 (PlayerCameraTarget).
 * Cualquier sistema externo puede añadir targets de mayor prioridad:
 *
 *   cameraSystem.getTargets().add(ScriptedCameraTarget.builder()
 *       .moveTo(640, 300, 60).build());  // prioridad 150 — toma el control
 *   // Al expirar, la cámara vuelve automáticamente al player.
 *
 * ── CAMERA_DELTA_TIME ─────────────────────────────────────────────────────
 * deltaTime es ahora un parámetro de update() en lugar de una constante
 * hardcodeada. WorldManager lo pasa usando su targetFps.
 */
public final class CameraSystem {

    private final GameCamera           camera;
    private final PriorityCameraTarget priorityTarget;

    /**
     * Controlador activo. Por defecto: TargetCameraController sobre priorityTarget.
     * Reemplazable en runtime via setCameraController().
     */
    private CameraController cameraController;

    /**
     * @param virtualWidth  ancho virtual del juego
     * @param virtualHeight alto virtual del juego
     */
    public CameraSystem(int virtualWidth, int virtualHeight) {
        this.camera         = new GameCamera(virtualWidth, virtualHeight);
        this.priorityTarget = new PriorityCameraTarget();

        // Controlador por defecto: sigue el target de mayor prioridad activa
        this.cameraController = new TargetCameraController(priorityTarget, 0.10f);
    }

    // ── Update ────────────────────────────────────────────────────────────

    /**
     * Actualiza el CameraController y consolida el estado del frame.
     *
     * @param deltaTime tiempo transcurrido desde el último tick en segundos
     */
    public void update(double deltaTime) {
        if (cameraController != null) {
            cameraController.update(camera, deltaTime);
        }
        // commitFrame() es llamado internamente por GameCamera.setBasePosition()
        // cada vez que el controller actualiza la posición.
        // La llamada explícita aquí garantiza que los modificadores con decay
        // se actualicen incluso si la cámara no se movió en este tick.
        // computeState() ya llama update() internamente — no duplicar.
        camera.commitFrame();
    }

    // ── Configuración de tracking ─────────────────────────────────────────

    /**
     * Registra el player (o cualquier objeto controlado) como target de cámara.
     *
     * Crea un PlayerCameraTarget con prioridad 100 y lo añade al sistema de
     * prioridades. Si ya existe un target de mayor prioridad (cinemática, boss),
     * el player espera en la cola y retoma el control al expirar el otro.
     *
     * Hace snap inicial de la cámara al objeto para evitar el lerp desde (0,0).
     *
     * @param obj el objeto a rastrear (generalmente el player)
     */
    public void setTrackedObject(GameObjects obj) {
        if (obj == null) return;

        PlayerCameraTarget playerTarget = PlayerCameraTarget.of(obj);
        priorityTarget.add(playerTarget);

        // Snap inicial: colocar la cámara directamente sobre el objeto
        var pos = obj.getTransform().getPosition();
        camera.centerOn(pos.getX(), pos.getY());
    }

    /**
     * Notifica que el sector activo cambió (transición completada).
     * Hace snap de la cámara a la nueva posición para evitar lerp visual largo.
     *
     * @param newPosition posición del objeto rastreado en el nuevo sector
     */
    public void onSectorChanged(Vector2D newPosition) {
        if (newPosition == null) return;
        camera.centerOn(newPosition.getX(), newPosition.getY());
    }

    // ── Acceso al target system ───────────────────────────────────────────

    /**
     * El PriorityCameraTarget activo.
     * Añadir targets de mayor prioridad para cinemáticas, proyectiles especiales, etc.
     *
     * Ejemplos:
     *   // Cinemática: toma el control durante 90 ticks:
     *   getTargets().add(ScriptedCameraTarget.builder()
     *       .moveTo(640, 300, 60).hold(30).build());
     *
     *   // Proyectil especial: sigue el proyectil mientras viva:
     *   getTargets().add(ProjectileCameraTarget.of(bullet, bullet::isPendingDestruction));
     */
    public PriorityCameraTarget getTargets() {
        return priorityTarget;
    }

    /**
     * Añade un CameraTarget con la prioridad que trae el target.
     */
    public void addTarget(CameraTarget target) {
        priorityTarget.add(target);
    }

    // ── Control del CameraController ──────────────────────────────────────

    /**
     * Reemplaza el CameraController activo.
     *
     * Permite cambiar a cámara libre, cinemática, debug, etc. en runtime.
     * null desactiva el controlador (cámara estática).
     */
    public void setCameraController(CameraController controller) {
        this.cameraController = controller;
    }

    public CameraController getCameraController() {
        return cameraController;
    }

    // ── Resize ────────────────────────────────────────────────────────────

    /**
     * Actualiza las dimensiones virtuales y los bounds de la cámara.
     */
    public void onVirtualResize(int newVirtualWidth, int newVirtualHeight) {
        camera.onVirtualResolutionChanged(newVirtualWidth, newVirtualHeight);
    }

    /**
     * Actualiza los límites del mundo (llamar al cambiar de sector o resize).
     */
    public void setWorldBounds(int worldWidth, int worldHeight) {
        camera.setWorldBounds(worldWidth, worldHeight);
    }

    // ── Acceso a GameCamera ───────────────────────────────────────────────

    /**
     * La GameCamera del Engine.
     * UIBootstrap, CrossHairHUD y cualquier sistema que necesite la vista
     * deben acceder a ella a través de este método.
     */
    public GameCamera getCamera() {
        return camera;
    }
}
