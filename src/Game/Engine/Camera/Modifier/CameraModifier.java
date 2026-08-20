package Game.Engine.Camera.Modifier;

/**
 * Modificador temporal del estado de la cámara.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * Un CameraModifier altera algún aspecto del estado de la cámara de forma
 * temporal y acumulable. Se apilan en CameraModifierStack y se aplican
 * DESPUÉS de que el CameraController calculó la posición base.
 *
 * ── SEPARACIÓN CON CameraController ───────────────────────────────────────
 * CameraController decide HACIA DÓNDE se mueve la cámara.
 * CameraModifier altera CÓMO se ve la imagen resultante.
 *
 * Un shake no cambia a dónde apunta la cámara — agita la imagen.
 * Un zoom temporal no cambia el target — escala la vista.
 *
 * ── ESTADO DEL MODIFICADOR ────────────────────────────────────────────────
 * Cada modificador lleva su propio estado:
 *   - Timer de duración restante
 *   - Parámetros de animación (amplitud de shake, etc.)
 *
 * ── ACUMULACIÓN ───────────────────────────────────────────────────────────
 * Múltiples modificadores del mismo tipo se acumulan.
 * CameraModifierStack itera todos y aplica sus efectos en orden.
 *
 * ── CICLO DE VIDA ─────────────────────────────────────────────────────────
 * apply(state) → modifica el CameraState antes de que GameCamera lo consuma
 * update()     → avanza el timer, actualiza animaciones
 * isExpired()  → true cuando el modificador debe ser eliminado
 */
public interface CameraModifier {

    /**
     * Aplica el efecto de este modificador al estado de la cámara.
     *
     * @param state el estado acumulado de la cámara para este frame.
     *              apply() modifica state in-place.
     */
    void apply(CameraState state);

    /**
     * Avanza el estado interno del modificador.
     *
     * @param deltaTime tiempo transcurrido desde el último frame en segundos
     */
    void update(double deltaTime);

    /**
     * True cuando este modificador ha terminado y debe ser eliminado.
     */
    boolean isExpired();

    /**
     * Identificador único de este tipo de modificador.
     * Los modificadores con el mismo tipo pueden ser reemplazados
     * en lugar de apilarse (comportamiento configurado en CameraModifierStack).
     */
    default String getTypeId() { return getClass().getSimpleName(); }
}
