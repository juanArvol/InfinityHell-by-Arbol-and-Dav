package Game.Gameplay.UI;

import java.awt.Graphics2D;

/**
 * Interface de elementos de UI — refactorizada para coordenadas virtuales.
 *
 * CAMBIO: draw() recibe Graphics2D (en lugar de Graphics) porque ya
 * estamos siempre en el framebuffer virtual donde Graphics2D es garantizado.
 *
 * CAMBIO: onResize() ya NO recibe screenWidth/screenHeight reales.
 * Recibe virtualWidth y virtualHeight (que son constantes de DisplaySettings,
 * pero se pasan explícitamente para que los elementos no dependan del singleton).
 *
 * La UI trabaja en coordenadas virtuales. La transformación a pantalla
 * la hace ScalingManager automáticamente al escalar el framebuffer.
 */
public interface UIElement {

    /** Lógica de update (no dibujo). */
    void update();

    /**
     * Dibuja el elemento en coordenadas VIRTUALES.
     *
     * @param g Graphics2D del framebuffer virtual — NO de la pantalla real.
     */
    void draw(Graphics2D g);

    /**
     * Notificación de "resize" — en realidad la resolución virtual nunca cambia,
     * pero se llama cuando el juego necesita que los elementos recalculen sus
     * posiciones de anchor.
     *
     * Implementaciones deben recalcular x/y usando UIAnchor con estos valores.
     *
     * @param virtualWidth  siempre DisplaySettings.virtualWidth (constante)
     * @param virtualHeight siempre DisplaySettings.virtualHeight (constante)
     */
    void onResize(int virtualWidth, int virtualHeight);
}
