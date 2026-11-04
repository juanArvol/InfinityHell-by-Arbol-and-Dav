package Game.Engine.Systems;

import Game.Engine.Camera.GameCamera;
import Game.Engine.Entity.Components.Collisions.ColliderComponent;
import Game.Engine.GameMath.Logic2D.RectangleH;
import Game.Engine.GameObjects;
import Game.Engine.RenderEngine.Culling.ViewportCuller;

/**
 * CollisionCulling — optimización para reducir trabajo de colisiones fuera de cámara.
 *
 * ── HRFC — Deep Optimization: Collision Culling ───────────────────────────
 *
 * PROPÓSITO:
 *   Evitar procesar colisiones de bullets que están completamente fuera del
 *   área relevante. Los bullets fuera de cámara generalmente no necesitan
 *   detección de colisión activa (con excepciones para gameplay crítico).
 *
 * ESTRATEGIA:
 *   - Bullets DENTRO de cámara + margen: colisiones completas
 *   - Bullets FUERA de cámara: skip collision broadphase/narrowphase
 *   - Objetos sólidos (Player, Enemies, Walls): SIEMPRE procesados
 *
 * MARGEN DE SEGURIDAD DINÁMICO:
 *   ── HRFC FIX: Margen adaptativo basado en velocidad ───────────────────
 *   El margen ahora se calcula dinámicamente considerando la velocidad máxima
 *   esperada de bullets para prevenir tunneling percibido:
 *
 *     margen = max(DEFAULT_MARGIN, maxBulletSpeed × deltaTime × SAFETY_FACTOR)
 *
 *   Esto garantiza que bullets muy rápidos no atraviesen colliders que están
 *   justo fuera del margen fijo.
 *
 * CORRECTITUD:
 *   - Player y enemigos: SIEMPRE se procesan (están en cámara o cerca)
 *   - Bullets que salen de pantalla: pueden seguir colisionando brevemente
 *   - Bullets muy lejos: skip colisiones (serán destruidos por OffScreenTracker)
 *
 * USO EN CollisionsSystem:
 *   if (CollisionCulling.shouldSkipCollision(bullet, camera, deltaTime)) {
 *       continue;  // Skip este bullet en broadphase
 *   }
 *
 * NOTA:
 *   Este culling es OPCIONAL y conservador. Si causa problemas de gameplay
 *   (bullets atravesando paredes al salir de pantalla), se puede desactivar
 *   fácilmente retornando false en shouldSkipCollision().
 */
public final class CollisionCulling {

    /**
     * Margen base para el culling de colisiones (píxeles).
     * Se aumenta dinámicamente basado en velocidad de bullets.
     */
    public static final int DEFAULT_COLLISION_MARGIN = 128;

    /**
     * Velocidad máxima esperada de bullets en px/s.
     * Usado para calcular margen dinámico que previene tunneling.
     * 
     * NOTA: Este valor debe ser >= la velocidad real máxima de bullets.
     * Si hay bullets más rápidos, aumentar este valor.
     */
    private static final double MAX_BULLET_SPEED = 600.0;  // px/s

    /**
     * Factor de seguridad para el margen dinámico.
     * 2.0 = margen de 2× el desplazamiento máximo por frame.
     */
    private static final double SAFETY_FACTOR = 2.0;

    /**
     * Feature flag global para activar/desactivar collision culling.
     * Si es false, shouldSkipCollision() siempre retorna false (sin culling).
     */
    private static boolean ENABLED = true;

    // Clase utilitaria — no instanciable
    private CollisionCulling() {}

    /**
     * Activa o desactiva el collision culling globalmente.
     *
     * @param enabled true para activar culling, false para desactivar
     */
    public static void setEnabled(boolean enabled) {
        ENABLED = enabled;
    }

    /**
     * Verifica si el collision culling está activo.
     *
     * @return true si está activo
     */
    public static boolean isEnabled() {
        return ENABLED;
    }

    /**
     * Determina si un objeto debería ser omitido del procesamiento de colisiones
     * por estar fuera del área relevante.
     *
     * ── HRFC FIX: Margen dinámico basado en velocidad ────────────────────
     * El margen se calcula considerando la velocidad máxima de bullets y el
     * tiempo del frame para prevenir tunneling percibido.
     *
     * REGLAS:
     *   - Si collision culling está desactivado: false (siempre procesar)
     *   - Si el objeto NO es trigger: false (objetos sólidos siempre se procesan)
     *   - Si el objeto es trigger Y está fuera de cámara + margen dinámico: true (skip)
     *   - Caso contrario: false (procesar)
     *
     * @param obj       objeto a verificar
     * @param camera    cámara activa del juego
     * @param deltaTime tiempo del frame en segundos (usado para margen dinámico)
     * @return true si el objeto debería ser omitido de colisiones
     */
    public static boolean shouldSkipCollision(GameObjects obj, GameCamera camera, double deltaTime) {
        if (!ENABLED) {
            return false;  // Feature desactivada
        }

        // Solo hacer culling de triggers (bullets)
        // Los objetos que bloquean (Player, Enemies, Walls) SIEMPRE se procesan
        ColliderComponent collider = obj.getComponent(ColliderComponent.class);
        if (collider == null || collider.blocksMovement()) {
            return false;  // Bloquea movimiento → procesar siempre
        }

        // Calcular margen dinámico basado en velocidad máxima de bullets
        // margen = max(DEFAULT, maxSpeed × deltaTime × SAFETY_FACTOR)
        double dynamicMargin = Math.max(
            DEFAULT_COLLISION_MARGIN,
            MAX_BULLET_SPEED * deltaTime * SAFETY_FACTOR
        );

        // Verificar si el trigger está dentro del área relevante
        RectangleH bounds = collider.getBounds();
        
        boolean isVisible = ViewportCuller.isVisible(
            (int) bounds.x,
            (int) bounds.y,
            (int) bounds.width,
            (int) bounds.height,
            (int) camera.getX(),
            (int) camera.getY(),
            camera.getVirtualWidth(),
            camera.getVirtualHeight(),
            (int) dynamicMargin
        );

        // Si NO está visible (fuera del área con margen), skip collision
        return !isVisible;
    }

    /**
     * Variante sin deltaTime (usa margen por defecto).
     * @deprecated Usar la versión con deltaTime para margen dinámico correcto.
     */
    @Deprecated
    public static boolean shouldSkipCollision(GameObjects obj, GameCamera camera) {
        return shouldSkipCollision(obj, camera, 1.0 / 60.0);  // Asume 60fps
    }

    /**
     * Variante que acepta posición y tamaño directamente.
     * Útil cuando los bounds ya fueron calculados.
     *
     * @param worldX posición X en el mundo
     * @param worldY posición Y en el mundo
     * @param width  ancho del collider
     * @param height alto del collider
     * @param camera cámara activa
     * @return true si está fuera del área relevante
     */
    public static boolean isOutsideCollisionArea(double worldX, double worldY,
                                                  double width, double height,
                                                  GameCamera camera) {
        if (!ENABLED) {
            return false;
        }

        boolean isVisible = ViewportCuller.isVisible(
            (int) worldX,
            (int) worldY,
            (int) width,
            (int) height,
            (int) camera.getX(),
            (int) camera.getY(),
            camera.getVirtualWidth(),
            camera.getVirtualHeight(),
            DEFAULT_COLLISION_MARGIN
        );

        return !isVisible;
    }
}
