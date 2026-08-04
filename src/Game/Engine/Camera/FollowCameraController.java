package Game.Engine.Camera;

import Game.Engine.GameMath.Logic2D.Vector2D;
import java.util.function.Supplier;

/**
 * Controlador de cámara con seguimiento suave (lerp).
 *
 * ── Qué hace ─────────────────────────────────────────────────────────────
 *
 * Sigue a un objetivo (target) con interpolación lineal, produciendo el
 * efecto de "cámara pegajosa" característico de juegos de acción de calidad.
 * La cámara nunca llega instantáneamente al objetivo: se aproxima suavemente,
 * amortiguando los movimientos bruscos del player.
 *
 * ── Por qué Supplier<Vector2D> en lugar de GameObjects ───────────────────
 *
 * FollowCameraController pertenece al Engine. Si recibiera GameObjects
 * directamente no habría problema de dependencia. Sin embargo, usar un
 * Supplier permite:
 *   - Cambiar el target sin recrear el controlador.
 *   - Que el objetivo sea cualquier fuente de posición (interpolada,
 *     promedio de varios objetos, posición de un punto cinemático, etc.).
 *   - Testear el controlador con un lambda sin necesitar un GameObjects real.
 *
 * ── Parámetros de follow ─────────────────────────────────────────────────
 *
 *   lerpFactor: velocidad de seguimiento [0.0, 1.0].
 *     - 0.05–0.08 → seguimiento muy suave (cinemático).
 *     - 0.10–0.15 → seguimiento normal (acción).
 *     - 1.0       → snap instantáneo (sin suavizado).
 *
 * ── Clamp de mundo ───────────────────────────────────────────────────────
 *
 * El clamp a los límites del mundo lo gestiona GameCamera internamente
 * (setWorldBounds). FollowCameraController no necesita conocer los límites;
 * solo calcula la posición objetivo y llama lerpCenterOn().
 */
public final class FollowCameraController implements CameraController {

    private final Supplier<Vector2D> targetPositionSupplier;
    private final float              lerpFactor;

    /**
     * Crea un controlador de seguimiento con lerp.
     *
     * @param targetPositionSupplier proveedor de la posición del objetivo
     *                               (evaluado en cada tick)
     * @param lerpFactor             velocidad de interpolación [0.0, 1.0]
     */
    public FollowCameraController(Supplier<Vector2D> targetPositionSupplier,
                                  float lerpFactor) {
        if (targetPositionSupplier == null)
            throw new IllegalArgumentException("targetPositionSupplier cannot be null");
        if (lerpFactor <= 0 || lerpFactor > 1)
            throw new IllegalArgumentException("lerpFactor must be in (0.0, 1.0]");

        this.targetPositionSupplier = targetPositionSupplier;
        this.lerpFactor             = lerpFactor;
    }

    /** Factor de lerp por defecto para seguimiento de acción (10%). */
    public FollowCameraController(Supplier<Vector2D> targetPositionSupplier) {
        this(targetPositionSupplier, 0.10f);
    }

    @Override
    public void update(GameCamera camera, double deltaTime) {
        Vector2D targetPos = targetPositionSupplier.get();
        if (targetPos == null) return;

        camera.lerpCenterOn(targetPos.getX(), targetPos.getY(), lerpFactor);
    }
}
