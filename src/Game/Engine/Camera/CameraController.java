package Game.Engine.Camera;

/**
 * Contrato de comportamiento de cámara — Engine.
 *
 * Un CameraController actualiza el estado de una {@link GameCamera} cada tick
 * del game loop. Define CÓMO se mueve la cámara, no dónde ni qué pinta.
 *
 * ── Arquitectura ─────────────────────────────────────────────────────────
 *
 * La cámara (GameCamera) es el estado: posición, zoom, rotación.
 * El controlador (CameraController) es el comportamiento: seguir, libre,
 * cinemático, interpolado.
 *
 * Esta separación permite:
 *   - Cambiar el comportamiento en runtime sin recrear la cámara.
 *   - Implementar nuevos modos sin modificar GameCamera.
 *   - Testear el comportamiento de seguimiento de forma independiente.
 *   - Cinemáticas que toman el control temporalmente y lo devuelven.
 *
 * ── Implementaciones previstas ────────────────────────────────────────────
 *
 *   FollowCameraController   — sigue a un objeto con lerp suave y clamp de mundo.
 *   FreeCameraController     — cámara libre controlada por input (editor, debug).
 *   CinematicCameraController — secuencias predefinidas de posición/zoom/rotación.
 *   StaticCameraController   — cámara fija en una posición (pantallas de título, etc.).
 *
 * ── Contrato ─────────────────────────────────────────────────────────────
 *
 *   update() se llama UNA VEZ por tick del game loop, antes del render.
 *   El controlador modifica la cámara mediante los métodos de GameCamera.
 *   El controlador NO dibuja nada.
 */
public interface CameraController {

    /**
     * Actualiza el estado de la cámara para el tick actual.
     *
     * @param camera    la cámara a actualizar; nunca null.
     * @param deltaTime tiempo transcurrido desde el último tick en segundos.
     *                  Para un loop de 30fps: deltaTime ≈ 0.0333.
     */
    void update(GameCamera camera, double deltaTime);
}
