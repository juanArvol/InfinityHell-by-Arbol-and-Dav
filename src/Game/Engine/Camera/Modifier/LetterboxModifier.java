package Game.Engine.Camera.Modifier;

/**
 * Modificador de letterbox (bandas negras) de la cámara.
 *
 * ── USOS ──────────────────────────────────────────────────────────────────
 *   // Cinemática con bandas de 80px arriba y abajo:
 *   modifiers.add(new LetterboxModifier(80, 80, 1.0, 0.08f));
 *
 *   // Solo banda inferior (subtítulos):
 *   modifiers.add(new LetterboxModifier(0, 60, 0, 0.05f));
 *
 * ── FUNCIONAMIENTO ────────────────────────────────────────────────────────
 * El letterbox sube/baja suavemente (lerp).
 * La aplicación visual del letterbox la realiza el renderer, que observa
 * CameraState.letterboxTop y CameraState.letterboxBottom.
 *
 * Este modificador solo gestiona el valor; el renderer decide cómo dibujarlo.
 */
public final class LetterboxModifier implements CameraModifier {

    private final int    targetTop;
    private final int    targetBottom;
    private final double durationSeconds;  // 0 = permanente
    private final float  lerpFactor;

    private float  currentTop     = 0.0f;
    private float  currentBottom  = 0.0f;
    private double elapsedSeconds = 0.0;

    /**
     * @param targetTop       píxeles de banda superior objetivo
     * @param targetBottom    píxeles de banda inferior objetivo
     * @param durationSeconds duración total en segundos (0 = permanente)
     * @param lerpFactor      velocidad de interpolación
     */
    public LetterboxModifier(int targetTop, int targetBottom,
                              double durationSeconds, float lerpFactor) {
        this.targetTop       = targetTop;
        this.targetBottom    = targetBottom;
        this.durationSeconds = durationSeconds;
        this.lerpFactor      = Math.max(0.01f, Math.min(1.0f, lerpFactor));
    }

    /** Letterbox cinematic estándar (permanente hasta eliminar). */
    public static LetterboxModifier cinematic(int pixels, float lerpFactor) {
        return new LetterboxModifier(pixels, pixels, 0, lerpFactor);
    }

    @Override
    public void apply(CameraState state) {
        // Acumular: tomar el máximo de varios letterbox activos
        state.letterboxTop    = Math.max(state.letterboxTop,    (int) currentTop);
        state.letterboxBottom = Math.max(state.letterboxBottom, (int) currentBottom);
    }

    @Override
    public void update(double deltaTime) {
        elapsedSeconds += deltaTime;

        if (durationSeconds <= 0) {
            currentTop    += (targetTop    - currentTop)    * lerpFactor;
            currentBottom += (targetBottom - currentBottom) * lerpFactor;
            return;
        }

        double halfway = durationSeconds / 2.0;
        if (elapsedSeconds <= halfway) {
            currentTop    += (targetTop    - currentTop)    * lerpFactor;
            currentBottom += (targetBottom - currentBottom) * lerpFactor;
        } else {
            currentTop    += (0.0f - currentTop)    * lerpFactor;
            currentBottom += (0.0f - currentBottom) * lerpFactor;
        }
    }

    @Override
    public boolean isExpired() {
        if (durationSeconds <= 0) return false;
        return elapsedSeconds >= durationSeconds;
    }
}
