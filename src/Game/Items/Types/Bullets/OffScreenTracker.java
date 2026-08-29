package Game.Items.Types.Bullets;

import Game.Engine.Camera.GameCamera;
import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Engine.RenderEngine.Culling.ViewportCuller;

/**
 * OffScreenTracker — rastrea el tiempo que un proyectil permanece fuera de cámara.
 *
 * ── HRFC — Deep Optimization: Off-Screen Lifetime Tracking ────────────────
 *
 * PROPÓSITO:
 *   Eliminar proyectiles que han estado fuera de la cámara por demasiado
 *   tiempo para evitar acumulación infinita de bullets que ya no son
 *   visibles ni relevantes para el gameplay.
 *
 * COMPORTAMIENTO:
 *   - Mientras el bullet está DENTRO de cámara: offScreenTime = 0
 *   - Cuando sale de cámara: offScreenTime comienza a incrementarse
 *   - Si offScreenTime > maxOffScreenTime: shouldDestroy() = true
 *
 * CONFIGURACIÓN PER-BULLET:
 *   - Cada bullet puede tener su propio maxOffScreenTime
 *   - NEVER_DESTROY (-1.0) = nunca destruir por off-screen
 *   - 0.0 = destruir inmediatamente al salir de cámara
 *   - 5.0 = permitir 5 segundos fuera antes de destruir
 *
 * USO:
 *   OffScreenTracker tracker = new OffScreenTracker(5.0);  // 5 segundos
 *   tracker.update(bullet.getTransform().getPosition(), camera, deltaTime);
 *   if (tracker.shouldDestroy()) {
 *       bullet.getBulletLife().kill();
 *   }
 *
 * NOTA:
 *   Este tracker NO mata el bullet directamente — solo indica cuando
 *   debería morir. La responsabilidad de matar es del caller (Bullet.update).
 */
public class OffScreenTracker {

    /**
     * Valor especial que indica que el bullet nunca debe destruirse por
     * estar fuera de cámara.
     */
    public static final double NEVER_DESTROY = -1.0;

    /**
     * Margen extra para considerar "en pantalla". Usa el margen default del
     * ViewportCuller (32px) para consistencia con el rendering culling.
     */
    private static final int VISIBILITY_MARGIN = ViewportCuller.DEFAULT_MARGIN;

    /**
     * Tiempo máximo permitido fuera de cámara antes de autodestrucción (segundos).
     * NEVER_DESTROY = nunca autodestruir.
     */
    private final double maxOffScreenTime;

    /**
     * Tiempo acumulado fuera de cámara (segundos).
     */
    private double offScreenTime;

    /**
     * Ancho del sprite del bullet (para culling).
     * Se configura una vez y no cambia durante la vida del bullet.
     */
    private int spriteWidth;

    /**
     * Alto del sprite del bullet (para culling).
     */
    private int spriteHeight;

    /**
     * Constructor con tiempo máximo configurable.
     *
     * @param maxOffScreenTime segundos máximos fuera de cámara (NEVER_DESTROY para infinito)
     */
    public OffScreenTracker(double maxOffScreenTime) {
        this.maxOffScreenTime = maxOffScreenTime;
        this.offScreenTime = 0.0;
        this.spriteWidth = 0;
        this.spriteHeight = 0;
    }

    /**
     * Configura el tamaño del sprite para el culling.
     * Debe llamarse al menos una vez antes del primer update().
     *
     * @param width  ancho del sprite en píxeles
     * @param height alto del sprite en píxeles
     */
    public void setSpriteSize(int width, int height) {
        this.spriteWidth = width;
        this.spriteHeight = height;
    }

    /**
     * Actualiza el tracker con la posición actual del bullet y la cámara.
     *
     * @param bulletPos posición del bullet en el mundo
     * @param camera    cámara activa del juego
     * @param deltaTime tiempo transcurrido desde el último frame (segundos)
     */
    public void update(Vector2D bulletPos, GameCamera camera, double deltaTime) {
        // Si maxOffScreenTime es NEVER_DESTROY, no rastreamos nada
        if (maxOffScreenTime == NEVER_DESTROY) {
            return;
        }

        // Verificar si el bullet está visible
        boolean isVisible = ViewportCuller.isVisible(
            bulletPos.getX(),
            bulletPos.getY(),
            spriteWidth,
            spriteHeight,
            camera.getX(),
            camera.getY(),
            camera.getVirtualWidth(),
            camera.getVirtualHeight(),
            VISIBILITY_MARGIN
        );

        if (isVisible) {
            // Dentro de cámara: resetear contador
            offScreenTime = 0.0;
        } else {
            // Fuera de cámara: incrementar contador
            offScreenTime += deltaTime;
        }
    }

    /**
     * Indica si el bullet debería autodestruirse por haber estado demasiado
     * tiempo fuera de cámara.
     *
     * @return true si offScreenTime > maxOffScreenTime
     */
    public boolean shouldDestroy() {
        if (maxOffScreenTime == NEVER_DESTROY) {
            return false;
        }
        return offScreenTime > maxOffScreenTime;
    }

    /**
     * Indica si el bullet está actualmente fuera de cámara.
     *
     * @return true si offScreenTime > 0
     */
    public boolean isOffScreen() {
        return offScreenTime > 0.0;
    }

    /**
     * Retorna el tiempo acumulado fuera de cámara.
     *
     * @return segundos fuera de cámara
     */
    public double getOffScreenTime() {
        return offScreenTime;
    }

    /**
     * Retorna el tiempo máximo configurado.
     *
     * @return maxOffScreenTime
     */
    public double getMaxOffScreenTime() {
        return maxOffScreenTime;
    }

    /**
     * Resetea el tracker (para reutilización de pool).
     */
    public void reset() {
        this.offScreenTime = 0.0;
    }
}
