package Game.Engine.Camera;

import Game.Engine.Camera.Target.CameraTarget;
import Game.Engine.GameMath.Logic2D.Vector2D;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Controlador de cámara con seguimiento suave (lerp).
 *
 * ── Responsabilidad ───────────────────────────────────────────────────────
 * Sigue a un objetivo con interpolación lineal, produciendo el efecto de
 * "cámara pegajosa" característico de juegos de acción. El objetivo puede
 * expresarse como {@link Supplier}{@code <Vector2D>} (API legada) o como
 * {@link CameraTarget} (API nueva, composable y con prioridad).
 *
 * ── Uso recomendado (código nuevo) ────────────────────────────────────────
 *
 *   // Con CameraTarget:
 *   CameraController ctrl = FollowCameraController.following(playerTarget, 0.10f);
 *
 *   // Con Supplier (simple, directo):
 *   CameraController ctrl = FollowCameraController.following(() -> pos, 0.10f);
 *
 * ── Parámetros de lerpFactor ──────────────────────────────────────────────
 *   0.05–0.08 → seguimiento muy suave (cinemático)
 *   0.10–0.15 → seguimiento normal (acción)
 *   1.0       → snap instantáneo (sin suavizado)
 *
 * ── Diseño interno ────────────────────────────────────────────────────────
 * Todos los constructores públicos y las factories convergen en el
 * constructor privado canónico {@link #FollowCameraController(Supplier, float, Void)},
 * que es el único punto donde los campos se asignan. Las validaciones de
 * parámetros se realizan antes de llamarlo, en el constructor o factory
 * que recibe cada tipo, sin delegación encadenada que pueda producir
 * ambigüedad de resolución en el compilador.
 */
public final class FollowCameraController implements CameraController {

    /** Factor de lerp por defecto para seguimiento de acción. */
    public static final float DEFAULT_LERP = 0.10f;

    private final Supplier<Vector2D> positionSupplier;
    private final float              lerpFactor;

    // ── Constructor canónico (privado) ────────────────────────────────────

    /**
     * Constructor canónico: único punto de asignación de campos.
     *
     * El parámetro {@code disambiguator} (siempre {@code null}) existe
     * exclusivamente para diferenciar esta sobrecarga de los constructores
     * públicos y evitar ambigüedad. No tiene semántica propia.
     */
    private FollowCameraController(Supplier<Vector2D> supplier, float lerpFactor,
                                    @SuppressWarnings("unused") Void disambiguator) {
        this.positionSupplier = supplier;
        this.lerpFactor       = lerpFactor;
    }

    // ── Constructores públicos: Supplier<Vector2D> ────────────────────────

    /**
     * Crea un controlador que sigue la posición provista por {@code supplier}.
     *
     * @param supplier   proveedor de la posición del objetivo; no puede ser null
     * @param lerpFactor velocidad de interpolación en {@code (0.0, 1.0]}
     * @throws NullPointerException     si {@code supplier} es null
     * @throws IllegalArgumentException si {@code lerpFactor} no está en {@code (0.0, 1.0]}
     */
    public FollowCameraController(Supplier<Vector2D> supplier, float lerpFactor) {
        this(
            Objects.requireNonNull(supplier, "supplier cannot be null"),
            validateLerpFactor(lerpFactor),
            null
        );
    }

    /**
     * Crea un controlador con {@link #DEFAULT_LERP} (10 %).
     *
     * @param supplier proveedor de la posición del objetivo; no puede ser null
     * @throws NullPointerException si {@code supplier} es null
     */
    public FollowCameraController(Supplier<Vector2D> supplier) {
        this(
            Objects.requireNonNull(supplier, "supplier cannot be null"),
            DEFAULT_LERP,
            null
        );
    }

    // ── Constructores públicos: CameraTarget ──────────────────────────────

    /**
     * Crea un controlador que sigue un {@link CameraTarget}.
     *
     * @param target     el target a seguir; no puede ser null
     * @param lerpFactor velocidad de interpolación en {@code (0.0, 1.0]}
     * @throws NullPointerException     si {@code target} es null
     * @throws IllegalArgumentException si {@code lerpFactor} no está en {@code (0.0, 1.0]}
     */
    public FollowCameraController(CameraTarget target, float lerpFactor) {
        this(
            Objects.requireNonNull(target, "target cannot be null")::getPosition,
            validateLerpFactor(lerpFactor),
            null
        );
    }

    /**
     * Crea un controlador con {@link #DEFAULT_LERP} (10 %).
     *
     * @param target el target a seguir; no puede ser null
     * @throws NullPointerException si {@code target} es null
     */
    public FollowCameraController(CameraTarget target) {
        this(
            Objects.requireNonNull(target, "target cannot be null")::getPosition,
            DEFAULT_LERP,
            null
        );
    }

    // ── Factories ─────────────────────────────────────────────────────────

    /**
     * Factory para código nuevo: sigue un {@link CameraTarget}.
     *
     * @param target     el target a seguir; no puede ser null
     * @param lerpFactor velocidad de interpolación en {@code (0.0, 1.0]}
     */
    public static FollowCameraController following(CameraTarget target, float lerpFactor) {
        return new FollowCameraController(target, lerpFactor);
    }

    /**
     * Factory para código nuevo: sigue un {@link Supplier}{@code <Vector2D>}.
     *
     * @param supplier   proveedor de posición; no puede ser null
     * @param lerpFactor velocidad de interpolación en {@code (0.0, 1.0]}
     */
    public static FollowCameraController following(Supplier<Vector2D> supplier,
                                                    float lerpFactor) {
        return new FollowCameraController(supplier, lerpFactor);
    }

    // ── CameraController ──────────────────────────────────────────────────

    @Override
    public void update(GameCamera camera, double deltaTime) {
        Vector2D targetPos = positionSupplier.get();
        if (targetPos == null) return;

        camera.lerpCenterOn(targetPos.getX(), targetPos.getY(), lerpFactor);
        // commitFrame() se llama internamente en GameCamera.setBasePosition(),
        // invocado por lerpCenterOn(). No es necesario llamarlo aquí.
    }

    // ── Acceso de solo lectura ─────────────────────────────────────────────

    /**
     * El factor de lerp configurado.
     * Útil para debug o para crear un controlador derivado con lerp ajustado.
     */
    public float getLerpFactor() { return lerpFactor; }

    // ── Validación privada ────────────────────────────────────────────────

    /**
     * Valida el {@code lerpFactor} y lo retorna si es válido.
     * Diseñado para ser llamado dentro de expresiones de constructor ({@code this(...)}).
     *
     * @param lerpFactor valor a validar
     * @return el mismo valor si es válido
     * @throws IllegalArgumentException si no está en {@code (0.0, 1.0]}
     */
    private static float validateLerpFactor(float lerpFactor) {
        if (lerpFactor <= 0f || lerpFactor > 1f)
            throw new IllegalArgumentException(
                "lerpFactor must be in (0.0, 1.0], got: " + lerpFactor);
        return lerpFactor;
    }
}
