package Game.Engine.Camera.Modifier;

/**
 * Modificador de zoom temporal de la cámara.
 *
 * ── USOS ──────────────────────────────────────────────────────────────────
 *   // Zoom in durante una cinemática (1.5x durante 1 segundo):
 *   modifiers.add(new ZoomModifier(1.5f, 1.0, 0.08f));
 *
 *   // Zoom out para mostrar área grande (0.8x durante 1.5 segundos):
 *   modifiers.add(new ZoomModifier(0.8f, 1.5, 0.05f));
 *
 * ── FUNCIONAMIENTO ────────────────────────────────────────────────────────
 * El zoom del modificador se interpola (lerp) hacia el objetivo durante
 * la primera mitad de la duración, y vuelve a 1.0 en la segunda mitad.
 * Si durationSeconds es 0, el zoom se aplica permanentemente hasta que
 * se elimine manualmente de la pila.
 *
 * ── COMPOSICIÓN ───────────────────────────────────────────────────────────
 * El zoomDelta de CameraState es multiplicativo: 1.0 = sin cambio.
 * Múltiples ZoomModifiers multiplican sus efectos (1.5 * 0.8 = 1.2).
 */
public final class ZoomModifier implements CameraModifier {

    private final float  targetZoom;
    private final double durationSeconds;  // 0 = permanente hasta eliminar
    private final float  lerpFactor;

    private float  currentZoom    = 1.0f;
    private double elapsedSeconds = 0.0;

    /**
     * @param targetZoom       zoom objetivo (>1 = acercar, <1 = alejar)
     * @param durationSeconds  duración total del efecto en segundos (0 = permanente)
     * @param lerpFactor       velocidad de interpolación [0.0, 1.0]
     */
    public ZoomModifier(float targetZoom, double durationSeconds, float lerpFactor) {
        if (targetZoom <= 0) throw new IllegalArgumentException("targetZoom must be > 0");
        this.targetZoom       = targetZoom;
        this.durationSeconds  = durationSeconds;
        this.lerpFactor       = Math.max(0.01f, Math.min(1.0f, lerpFactor));
    }

    /** Zoom permanente (hasta eliminación manual). */
    public ZoomModifier(float targetZoom, float lerpFactor) {
        this(targetZoom, 0, lerpFactor);
    }

    @Override
    public void apply(CameraState state) {
        state.zoomDelta *= currentZoom;
    }

    @Override
    public void update(double deltaTime) {
        elapsedSeconds += deltaTime;

        if (durationSeconds <= 0) {
            // Permanente: lerp hacia el objetivo
            currentZoom = currentZoom + (targetZoom - currentZoom) * lerpFactor;
            return;
        }

        // Temporal: primera mitad lerp hacia objetivo, segunda mitad lerp hacia 1.0
        double halfway = durationSeconds / 2.0;
        if (elapsedSeconds <= halfway) {
            currentZoom = currentZoom + (targetZoom - currentZoom) * lerpFactor;
        } else {
            currentZoom = currentZoom + (1.0f - currentZoom) * lerpFactor;
        }
    }

    @Override
    public boolean isExpired() {
        if (durationSeconds <= 0) return false;
        return elapsedSeconds >= durationSeconds;
    }
}
