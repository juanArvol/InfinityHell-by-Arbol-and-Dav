package Game.Engine.Camera.Target;

import Game.Engine.GameMath.Logic2D.Vector2D;

/**
 * Objetivo de seguimiento de la cámara.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * Un CameraTarget provee la posición que la cámara debe seguir, junto con
 * metadatos que permiten a la cámara tomar decisiones sobre cómo seguirla
 * (peso, prioridad, lerp factor, si está activo, si ha expirado).
 *
 * ── SEPARACIÓN CON CameraController ───────────────────────────────────────
 * CameraController define CÓMO la cámara se mueve (lerp, snap, cinematic).
 * CameraTarget define HACIA DÓNDE se mueve.
 *
 * Un CameraController usa un CameraTarget para obtener la posición objetivo.
 * Esto permite que el mismo FollowCameraController pueda seguir a un player,
 * un proyectil, un punto cinemático o cualquier cosa futura sin cambios.
 *
 * ── CICLO DE VIDA ─────────────────────────────────────────────────────────
 * isActive()   → la cámara solo usa targets activos
 * isExpired()  → targets de duración fija se auto-desactivan
 * getWeight()  → para targets compuestos (WeightedCameraTarget)
 * getPriority()→ el target de mayor prioridad prevalece (PriorityCameraTarget)
 *
 * ── IMPLEMENTACIONES ──────────────────────────────────────────────────────
 *   PlayerCameraTarget        → sigue al jugador
 *   StaticLocationTarget      → posición fija
 *   ProjectileCameraTarget    → sigue un proyectil activo
 *   ScriptedCameraTarget      → secuencia de posiciones por script
 *   CompositeCameraTarget     → promedio de múltiples targets
 *   WeightedCameraTarget      → mezcla por peso
 *   PriorityCameraTarget      → usa el target de mayor prioridad
 */
public interface CameraTarget {

    /**
     * Retorna la posición objetivo actual en coordenadas de mundo.
     *
     * @return posición objetivo, o null si el target no puede proveer una posición.
     *         La cámara ignora el target cuando retorna null.
     */
    Vector2D getPosition();

    /**
     * True si este target está activo y debe ser considerado por la cámara.
     * Un target desactivado es ignorado pero no eliminado.
     */
    default boolean isActive() { return true; }

    /**
     * True si este target ha expirado y debe ser eliminado de la cámara.
     * Diferencia con isActive(): un target expirado se auto-elimina;
     * uno inactivo puede volver a activarse.
     */
    default boolean isExpired() { return false; }

    /**
     * Peso relativo de este target para composición ponderada.
     * Solo relevante en WeightedCameraTarget. Valor por defecto: 1.0.
     */
    default float getWeight() { return 1.0f; }

    /**
     * Prioridad de este target. Mayor valor = mayor prioridad.
     * Solo relevante en PriorityCameraTarget. Valor por defecto: 0.
     */
    default int getPriority() { return 0; }

    /**
     * Notificación de que este target fue seleccionado como target activo.
     * Los targets que necesitan inicialización (ej: snap de posición inicial)
     * lo hacen aquí.
     */
    default void onSelected() {}

    /**
     * Actualiza el estado interno del target (un tick).
     * Útil para targets con animación, duración finita o lógica propia.
     */
    default void update() {}
}
