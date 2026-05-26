package Entradas.Listeners;

/**
 * Listener de acciones de teclado del juego.
 *
 * En lugar de que los sistemas consulten campos estáticos de KeyBoard
 * cada frame, se suscriben a los eventos que realmente les importan.
 *
 * DISEÑO:
 *  - Todos los métodos tienen implementación vacía por defecto (interface con defaults).
 *    Los suscriptores solo sobreescriben los eventos que necesitan.
 *  - Los eventos son semánticos ("saltar", "recargar"), NO teclas ("VK_SPACE", "VK_R").
 *    Esto desacopla la lógica del juego de los keybindings concretos.
 *  - Los eventos "pressed" son edge-triggered (un disparo al pulsar).
 *    Los estados continuos ("isMovingLeft", etc.) siguen disponibles
 *    como consulta directa en KeyBoard para los sistemas que los necesiten cada frame.
 *
 * CUÁNDO USAR LISTENER vs CONSULTA DIRECTA:
 *  - Listener   → acciones puntuales: recargar, saltar, abrir menú, toggle fullscreen.
 *  - Consulta   → estado continuo: moverse izquierda/derecha, correr, agacharse.
 */
public interface KeyActionListener {

    // ─── Movimiento ───────────────────────────────────────────────────────────

    /** Tecla de salto/subir pulsada (edge). */
    default void onJump() {}

    /** Tecla de agacharse pulsada (edge). */
    default void onCrouch() {}

    // ─── Combate ──────────────────────────────────────────────────────────────

    /** Tecla de recarga pulsada (edge). */
    default void onReload() {}

    /** Tecla de habilidad especial pulsada (edge - C). */
    default void onSpecial() {}

    // ─── Sistema ──────────────────────────────────────────────────────────────

    /** F11 pulsado (edge) — toggle fullscreen. */
    default void onToggleFullscreen() {}

    /** Escape pulsado (edge) — pausa / menú. */
    default void onPause() {}
}
