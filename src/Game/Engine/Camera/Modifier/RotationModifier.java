package Game.Engine.Camera.Modifier;

/**
 * Modificador de rotación temporal de la cámara.
 *
 * ── USOS ──────────────────────────────────────────────────────────────────
 *   // Inclinación sutil al disparar (±3°):
 *   modifiers.add(new RotationModifier((float)Math.toRadians(3), 20, 0.15f));
 *
 *   // Rotación de 180° para cinemática de portal:
 *   modifiers.add(new RotationModifier((float)Math.PI, 90, 0.05f));
 *
 * ── COMPOSICIÓN ───────────────────────────────────────────────────────────
 * La rotación del modificador se suma al rotationDelta de CameraState.
 * GameCamera aplica la rotación total desde el centro de la pantalla.
 */
public final class RotationModifier implements CameraModifier {

    private final float targetRotation;  // en radianes
    private final int   durationTicks;
    private final float lerpFactor;

    private float currentRotation = 0.0f;
    private int   ticksElapsed    = 0;

    /**
     * @param targetRotation rotación objetivo en radianes
     * @param durationTicks  duración total (0 = permanente)
     * @param lerpFactor     velocidad de interpolación
     */
    public RotationModifier(float targetRotation, int durationTicks, float lerpFactor) {
        this.targetRotation = targetRotation;
        this.durationTicks  = durationTicks;
        this.lerpFactor     = Math.max(0.01f, Math.min(1.0f, lerpFactor));
    }

    @Override
    public void apply(CameraState state) {
        state.rotationDelta += currentRotation;
    }

    @Override
    public void update() {
        ticksElapsed++;

        if (durationTicks <= 0) {
            currentRotation += (targetRotation - currentRotation) * lerpFactor;
            return;
        }

        float halfway = durationTicks / 2.0f;
        if (ticksElapsed <= halfway) {
            currentRotation += (targetRotation - currentRotation) * lerpFactor;
        } else {
            currentRotation += (0.0f - currentRotation) * lerpFactor;
        }
    }

    @Override
    public boolean isExpired() {
        if (durationTicks <= 0) return false;
        return ticksElapsed >= durationTicks;
    }
}
