package Game.Engine.Camera.Modifier;

/**
 * Estado acumulado de la cámara para el frame actual.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * CameraState es un objeto mutable de corta duración que acumula los
 * efectos de todos los CameraModifiers activos antes de que sean
 * aplicados a GameCamera al final del frame.
 *
 * ── CAMPOS ────────────────────────────────────────────────────────────────
 * offsetX, offsetY → desplazamiento adicional a aplicar (shake, etc.)
 * zoomDelta        → factor multiplicativo sobre el zoom base (1.0 = sin cambio)
 * rotationDelta    → rotación adicional en radianes
 * letterboxTop     → píxeles de recorte superior (0 = sin letterbox)
 * letterboxBottom  → píxeles de recorte inferior
 *
 * ── PIPELINE ──────────────────────────────────────────────────────────────
 * 1. CameraModifierStack.apply() crea un CameraState en cero.
 * 2. Cada CameraModifier.apply(state) acumula sus efectos.
 * 3. CameraSystem aplica CameraState a GameCamera.
 *
 * ── DISEÑO INTENCIONAL ────────────────────────────────────────────────────
 * CameraState se instancia nueva cada frame — no tiene identidad persistente.
 * Los modificadores no guardan referencias al estado entre frames.
 */
public final class CameraState {

    public double offsetX      = 0.0;
    public double offsetY      = 0.0;
    public float  zoomDelta    = 1.0f;   // multiplicativo: 1.0 = sin cambio
    public float  rotationDelta= 0.0f;   // aditivo: 0.0 = sin cambio
    public int    letterboxTop    = 0;
    public int    letterboxBottom = 0;

    public CameraState() {}

    /** True si algún modificador afecta la posición (para optimización). */
    public boolean hasPositionOffset() {
        return offsetX != 0.0 || offsetY != 0.0;
    }

    /** True si algún modificador afecta el zoom. */
    public boolean hasZoomDelta() {
        return zoomDelta != 1.0f;
    }

    /** True si algún modificador afecta la rotación. */
    public boolean hasRotationDelta() {
        return rotationDelta != 0.0f;
    }

    /** True si hay letterbox activo. */
    public boolean hasLetterbox() {
        return letterboxTop > 0 || letterboxBottom > 0;
    }

    /** Resetea todos los campos a sus valores neutros. */
    public void reset() {
        offsetX       = 0.0;
        offsetY       = 0.0;
        zoomDelta     = 1.0f;
        rotationDelta = 0.0f;
        letterboxTop  = 0;
        letterboxBottom = 0;
    }
}
