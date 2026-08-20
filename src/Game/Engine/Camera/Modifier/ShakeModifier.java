package Game.Engine.Camera.Modifier;

/**
 * Modificador de shake (vibración) de cámara.
 *
 * ── USOS ──────────────────────────────────────────────────────────────────
 *   // Impacto de bala (corto y fuerte):
 *   modifierStack.add(ShakeModifier.impact(6.0f, 0.2));
 *
 *   // Explosión (más largo y potente):
 *   modifierStack.add(ShakeModifier.explosion(12.0f, 0.5));
 *
 *   // Terremoto (continuo mientras dura):
 *   modifierStack.add(ShakeModifier.sustained(4.0f, 2.0));
 *
 * ── ALGORITMO ─────────────────────────────────────────────────────────────
 * Aplica un offset aleatorio en cada frame, con amplitud decreciente
 * (decay linear desde maxAmplitude hasta 0 a lo largo de la duración).
 *
 * ── ACUMULACIÓN ───────────────────────────────────────────────────────────
 * Múltiples ShakeModifiers se suman. El shake resultante es la suma
 * de todos los offsets, lo que produce sacudidas más intensas en eventos
 * simultáneos (correcto comportamiento en un juego de acción).
 */
public final class ShakeModifier implements CameraModifier {

    private final float  maxAmplitude;
    private final double durationSeconds;
    private double       elapsedSeconds = 0.0;
    private boolean      decays;

    /**
     * @param maxAmplitude amplitud máxima en píxeles
     * @param durationSeconds duración total en segundos
     * @param decays        si true, la amplitud decrece hasta cero al final
     */
    public ShakeModifier(float maxAmplitude, double durationSeconds, boolean decays) {
        this.maxAmplitude     = maxAmplitude;
        this.durationSeconds  = Math.max(0.001, durationSeconds);
        this.decays           = decays;
    }

    public ShakeModifier(float maxAmplitude, double durationSeconds) {
        this(maxAmplitude, durationSeconds, true);
    }

    // ── Factories ─────────────────────────────────────────────────────────

    /** Shake de impacto corto con decay. */
    public static ShakeModifier impact(float amplitude, double durationSeconds) {
        return new ShakeModifier(amplitude, durationSeconds, true);
    }

    /** Shake de explosión más prolongado. */
    public static ShakeModifier explosion(float amplitude, double durationSeconds) {
        return new ShakeModifier(amplitude, durationSeconds, true);
    }

    /** Shake continuo sin decay (terremoto, vibración de motor). */
    public static ShakeModifier sustained(float amplitude, double durationSeconds) {
        return new ShakeModifier(amplitude, durationSeconds, false);
    }

    // ── CameraModifier ────────────────────────────────────────────────────

    @Override
    public void apply(CameraState state) {
        float amplitude = currentAmplitude();
        if (amplitude <= 0) return;

        // Offset aleatorio dentro del rango [-amplitude, +amplitude]
        state.offsetX += (Math.random() * 2 - 1) * amplitude;
        state.offsetY += (Math.random() * 2 - 1) * amplitude;
    }

    @Override
    public void update(double deltaTime) {
        elapsedSeconds += deltaTime;
    }

    @Override
    public boolean isExpired() {
        return elapsedSeconds >= durationSeconds;
    }

    private float currentAmplitude() {
        if (!decays) return maxAmplitude;
        float progress = (float) (elapsedSeconds / durationSeconds);
        return maxAmplitude * (1.0f - progress);
    }
}
