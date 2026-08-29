package Game.Engine.Systems;

import Game.Engine.Camera.GameCamera;
import Game.Engine.Entity.Components.Collisions.ColliderComponent;
import Game.Engine.GameObjects;
import Game.Engine.RenderEngine.Culling.ViewportCuller;
import java.awt.Rectangle;

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
 * MARGEN DE SEGURIDAD:
 *   Usamos un margen extra (DEFAULT_COLLISION_MARGIN = 128px) más grande
 *   que el margen de rendering (32px) para garantizar que bullets rápidos
 *   que están a punto de entrar en pantalla no "atraviesen" objetos por
 *   no detectar la colisión a tiempo.
 *
 * CORRECTITUD:
 *   - Player y enemigos: SIEMPRE se procesan (están en cámara o cerca)
 *   - Bullets que salen de pantalla: pueden seguir colisionando brevemente
 *   - Bullets muy lejos: skip colisiones (serán destruidos por OffScreenTracker)
 *
 * USO EN CollisionsSystem:
 *   if (CollisionCulling.shouldSkipCollision(bullet, camera)) {
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
     * Margen extra para el culling de colisiones (píxeles).
     * Más grande que el margen de rendering para evitar bullets rápidos
     * que "atraviesan" porque su colisión no se detectó a tiempo.
     */
    public static final int DEFAULT_COLLISION_MARGIN = 128;

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
     * REGLAS:
     *   - Si collision culling está desactivado: false (siempre procesar)
     *   - Si el objeto NO es trigger: false (objetos sólidos siempre se procesan)
     *   - Si el objeto es trigger Y está fuera de cámara + margen: true (skip)
     *   - Caso contrario: false (procesar)
     *
     * @param obj    objeto a verificar
     * @param camera cámara activa del juego
     * @return true si el objeto debería ser omitido de colisiones
     */
    public static boolean shouldSkipCollision(GameObjects obj, GameCamera camera) {
        if (!ENABLED) {
            return false;  // Feature desactivada
        }

        // Solo hacer culling de triggers (bullets)
        // Los sólidos (Player, Enemies, Walls) SIEMPRE se procesan
        ColliderComponent collider = obj.getComponent(ColliderComponent.class);
        if (collider == null || !collider.isTrigger()) {
            return false;  // No es trigger → procesar siempre
        }

        // Verificar si el trigger está dentro del área relevante
        Rectangle bounds = collider.getBounds();
        
        boolean isVisible = ViewportCuller.isVisible(
            bounds.x,
            bounds.y,
            bounds.width,
            bounds.height,
            camera.getX(),
            camera.getY(),
            camera.getVirtualWidth(),
            camera.getVirtualHeight(),
            DEFAULT_COLLISION_MARGIN
        );

        // Si NO está visible (fuera del área con margen), skip collision
        return !isVisible;
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
                                                  int width, int height,
                                                  GameCamera camera) {
        if (!ENABLED) {
            return false;
        }

        boolean isVisible = ViewportCuller.isVisible(
            worldX,
            worldY,
            width,
            height,
            camera.getX(),
            camera.getY(),
            camera.getVirtualWidth(),
            camera.getVirtualHeight(),
            DEFAULT_COLLISION_MARGIN
        );

        return !isVisible;
    }
}
