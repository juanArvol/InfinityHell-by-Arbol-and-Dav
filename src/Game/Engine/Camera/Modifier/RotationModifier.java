package Game.Engine.Camera.Modifier;

/**
 * Modificador de rotación temporal de la cámara.
 *
 * ── USOS ──────────────────────────────────────────────────────────────────
 *   // Inclinación sutil al disparar (±3°):
 *   modifiers.add(new RotationModifier((float)Math.toRadians(3), 0.333, 0.15f));
 *
 *   // Rotación de 180° para cinemática de portal:
 *   modifiers.add(new RotationModifier((float)Math.PI, 1.5, 0.05f));
 *
 * ── COMPOSICIÓN ───────────────────────────────────────────────────────────
 * La rotación del modificador se suma al rotationDelta de CameraState.
 * GameCamera aplica la rotación total desde el centro de la pantalla.
 */
public final class RotationModifier implements CameraModifier {

    private final float  targetRotation;  // en radianes
    private final double durationSeconds;
    private final float  lerpFactor;

    private float  currentRotation = 0.0f;
    private double elapsedSeconds  = 0.0;

    /**
     * @param targetRotation  rotación objetivo en radianes
     * @param durationSeconds duración total en segundos (0 = permanente)
     * @param lerpFactor      velocidad de interpolación
     */
    public RotationModifier(float targetRotation, double durationSeconds, float lerpFactor) {
        this.targetRotation  = targetRotation;
        this.durationSeconds = durationSeconds;
        this.lerpFactor      = Math.max(0.01f, Math.min(1.0f, lerpFactor));
    }

    @Override
    public void apply(CameraState state) {
        state.rotationDelta += currentRotation;
    }

    @Override
    public void update(double deltaTime) {
        elapsedSeconds += deltaTime;

        if (durationSeconds <= 0) {
            currentRotation += (targetRotation - currentRotation) * lerpFactor;
            return;
        }

        double halfway = durationSeconds / 2.0;
        if (elapsedSeconds <= halfway) {
            currentRotation += (targetRotation - currentRotation) * lerpFactor;
        } else {
            currentRotation += (0.0f - currentRotation) * lerpFactor;
        }
    }

    @Override
    public boolean isExpired() {
        if (durationSeconds <= 0) return false;
        return elapsedSeconds >= durationSeconds;
    }
}
