package Game.Engine.Camera.Target;

import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Engine.GameObjects;
import java.util.function.Supplier;

/**
 * Target de cámara que sigue al jugador (o cualquier entidad controlada).
 *
 * ── USO ───────────────────────────────────────────────────────────────────
 *   // Desde WorldManager/CameraSystem:
 *   CameraTarget target = new PlayerCameraTarget(() -> player.getTransform().getPosition());
 *   cameraSystem.setTarget(target);
 *
 * ── AHEAD-OF-TIME OFFSET ──────────────────────────────────────────────────
 * Opcionalmente acepta un "look-ahead" factor: la cámara se adelanta
 * en la dirección de movimiento del jugador, mejorando la sensación de juego.
 * Por defecto: 0 (comportamiento original, sin adelanto).
 *
 * ── SUPPLIER VS GAMEOBJECTS ───────────────────────────────────────────────
 * Usa Supplier<Vector2D> para:
 *   - No acoplar el Engine al tipo Player.
 *   - Permitir expresiones lambda en el wiring.
 *   - Testear sin un Player real.
 */
public final class PlayerCameraTarget implements CameraTarget {

    private final Supplier<Vector2D> positionSupplier;
    private final float              lookAheadFactor;

    /** Velocidad estimada del tick anterior (para look-ahead). */
    private Vector2D lastPosition = null;

    /**
     * @param positionSupplier proveedor de la posición del jugador
     */
    public PlayerCameraTarget(Supplier<Vector2D> positionSupplier) {
        this(positionSupplier, 0.0f);
    }

    /**
     * @param positionSupplier proveedor de la posición del jugador
     * @param lookAheadFactor  factor de adelanto en la dirección de movimiento
     *                         [0.0 = sin adelanto, 1.0 = un frame de adelanto]
     *                         Valores típicos: 0.3–0.6
     */
    public PlayerCameraTarget(Supplier<Vector2D> positionSupplier, float lookAheadFactor) {
        if (positionSupplier == null)
            throw new IllegalArgumentException("positionSupplier cannot be null");
        this.positionSupplier = positionSupplier;
        this.lookAheadFactor  = lookAheadFactor;
    }

    /**
     * Crea un PlayerCameraTarget a partir de un GameObjects.
     * Conveniente cuando se tiene la referencia directa al objeto.
     */
    public static PlayerCameraTarget of(GameObjects target) {
        return new PlayerCameraTarget(
            () -> target.getTransform().getPosition()
        );
    }

    @Override
    public Vector2D getPosition() {
        Vector2D current = positionSupplier.get();
        if (current == null) return null;

        if (lookAheadFactor == 0.0f || lastPosition == null) {
            return current;
        }

        // Look-ahead: aplicar offset en la dirección de movimiento
        double dx = (current.getX() - lastPosition.getX()) * lookAheadFactor;
        double dy = (current.getY() - lastPosition.getY()) * lookAheadFactor;
        return new Vector2D(current.getX() + dx, current.getY() + dy);
    }

    @Override
    public void update() {
        Vector2D pos = positionSupplier.get();
        if (pos != null) lastPosition = new Vector2D(pos.getX(), pos.getY());
    }

    @Override
    public int getPriority() { return 100; } // Alta prioridad por defecto para el player
}
