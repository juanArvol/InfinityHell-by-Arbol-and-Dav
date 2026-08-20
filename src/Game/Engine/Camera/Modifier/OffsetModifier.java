package Game.Engine.Camera.Modifier;

/**
 * Modificador de offset posicional temporal de la cámara.
 *
 * ── USOS ──────────────────────────────────────────────────────────────────
 *   // Desplazar la vista 100px hacia abajo durante 1 segundo (revelar suelo):
 *   modifiers.add(new OffsetModifier(0, 100, 1.0, 0.08f));
 *
 *   // Offset permanente (ventana dividida, interfaz específica):
 *   modifiers.add(new OffsetModifier(200, 0, 0, 1.0f));
 *
 * ── DIFERENCIA CON SHAKE ──────────────────────────────────────────────────
 * ShakeModifier aplica offsets aleatorios en cada frame.
 * OffsetModifier aplica un offset suave y determinista (lerp hacia un punto).
 */
public final class OffsetModifier implements CameraModifier {

    private final double targetOffsetX;
    private final double targetOffsetY;
    private final double durationSeconds;   // 0 = permanente
    private final float  lerpFactor;

    private double currentOffsetX = 0.0;
    private double currentOffsetY = 0.0;
    private double elapsedSeconds = 0.0;

    /**
     * @param targetOffsetX   desplazamiento X objetivo en píxeles
     * @param targetOffsetY   desplazamiento Y objetivo en píxeles
     * @param durationSeconds duración en segundos (0 = permanente)
     * @param lerpFactor      velocidad de interpolación
     */
    public OffsetModifier(double targetOffsetX, double targetOffsetY,
                          double durationSeconds, float lerpFactor) {
        this.targetOffsetX   = targetOffsetX;
        this.targetOffsetY   = targetOffsetY;
        this.durationSeconds = durationSeconds;
        this.lerpFactor      = Math.max(0.01f, Math.min(1.0f, lerpFactor));
    }

    @Override
    public void apply(CameraState state) {
        state.offsetX += currentOffsetX;
        state.offsetY += currentOffsetY;
    }

    @Override
    public void update(double deltaTime) {
        elapsedSeconds += deltaTime;

        if (durationSeconds <= 0) {
            // Permanente: lerp hacia el objetivo
            currentOffsetX += (targetOffsetX - currentOffsetX) * lerpFactor;
            currentOffsetY += (targetOffsetY - currentOffsetY) * lerpFactor;
            return;
        }

        double halfway = durationSeconds / 2.0;
        if (elapsedSeconds <= halfway) {
            currentOffsetX += (targetOffsetX - currentOffsetX) * lerpFactor;
            currentOffsetY += (targetOffsetY - currentOffsetY) * lerpFactor;
        } else {
            // Volver a cero
            currentOffsetX += (0.0 - currentOffsetX) * lerpFactor;
            currentOffsetY += (0.0 - currentOffsetY) * lerpFactor;
        }
    }

    @Override
    public boolean isExpired() {
        if (durationSeconds <= 0) return false;
        return elapsedSeconds >= durationSeconds;
    }
}
