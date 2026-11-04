package Game.Engine.Physics.Movement;

import Game.Engine.GameObjects;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * RaycastState — Estado de Raycast Anti-Tunneling por Objeto
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 *
 * Mantiene el estado del raycast anti-tunneling para un objeto específico.
 * Persiste entre frames hasta que el contacto se resuelve o el obstáculo
 * deja de estar en el path.
 *
 * ESTADO POR EJE:
 *   - Eje X: ¿está en contacto? ¿cuál es el obstáculo?
 *   - Eje Y: ¿está en contacto? ¿cuál es el obstáculo?
 *
 * ── CICLO DE VIDA ─────────────────────────────────────────────────────────
 *
 * Frame N (primer encuentro):
 *   1. Raycast detecta obstáculo en X
 *   2. setObstacleX(obstacle)
 *   3. Tras mover, bounds.intersects(obstacle) == true
 *   4. setContactX(true)
 *
 * Frame N+1 (en contacto):
 *   5. isContactX() == true
 *   6. Raycast NO se evalúa para X
 *   7. CollisionSystem resuelve contacto
 *   8. Si velocity.x hacia obstáculo → mantener contacto
 *   9. Si velocity.x == 0 o alejándose → clearX()
 *
 * Frame N+2 (contacto resuelto):
 *   10. isContactX() == false
 *   11. Si velocity.x >= 1 → raycast se evalúa nuevamente
 *
 * ── DESACTIVACIÓN DE RAYCAST ──────────────────────────────────────────────
 *
 * El raycast en un eje se desactiva cuando:
 *   1. Se detecta contacto (bounds.intersects)
 *   2. CollisionSystem resuelve el overlap
 *   3. La velocidad en ese eje cambia (se invierte o detiene)
 *
 * NO se desactiva si:
 *   - La velocidad sigue apuntando hacia el obstáculo
 *   - El objeto sigue en contacto
 *
 * ── EJEMPLO ───────────────────────────────────────────────────────────────
 *
 * Bullet disparado hacia la derecha contra muro:
 *
 * Frame 0: X=50, vx=100
 *   → Raycast detecta muro en X=55
 *   → setObstacleX(muro)
 *   → Mover a X=54.7
 *   → bounds.intersects(muro) = true
 *   → setContactX(true)
 *
 * Frame 1: X=54.7, vx=100 (CollisionSystem aún no resuelve)
 *   → isContactX() = true
 *   → NO evaluar raycast
 *   → CollisionSystem detecta overlap
 *   → CollisionSystem setea vx=0
 *
 * Frame 2: X=54.7, vx=0
 *   → isContactX() = true
 *   → vx == 0 → clearX()
 *   → isContactX() = false
 *
 * Frame 3: X=54.7, vx=0
 *   → No raycast (vx < 1)
 *   → Sin movimiento
 */
public final class RaycastState {

    // ── Estado Eje X ──────────────────────────────────────────────────────
    private boolean contactX = false;
    private GameObjects obstacleX = null;

    // ── Estado Eje Y ──────────────────────────────────────────────────────
    private boolean contactY = false;
    private GameObjects obstacleY = null;

    // ═════════════════════════════════════════════════════════════════════
    // ══ EJE X
    // ═════════════════════════════════════════════════════════════════════

    /**
     * ¿Está actualmente en contacto en el eje X?
     * Si true, el raycast NO se evalúa para este eje.
     */
    public boolean isContactX() {
        return contactX;
    }

    /**
     * ¿Tiene un obstáculo registrado en X?
     */
    public boolean hasObstacleX() {
        return obstacleX != null;
    }

    /**
     * Obtiene el obstáculo en X (puede ser null).
     */
    public GameObjects getObstacleX() {
        return obstacleX;
    }

    /**
     * Registra que el raycast detectó un obstáculo en X.
     * No marca contacto todavía (eso se hace tras verificar bounds.intersects).
     */
    public void setObstacleX(GameObjects obstacle) {
        this.obstacleX = obstacle;
        this.contactX = false; // Todavía no confirmado
    }

    /**
     * Marca que el objeto está ahora en contacto físico con el obstáculo en X.
     * El raycast se desactiva para este eje.
     */
    public void setContactX(boolean contact) {
        this.contactX = contact;
    }

    /**
     * Limpia el estado del eje X.
     * Llamar cuando el contacto se resuelve o el obstáculo desaparece.
     */
    public void clearX() {
        this.contactX = false;
        this.obstacleX = null;
    }

    // ═════════════════════════════════════════════════════════════════════
    // ══ EJE Y
    // ═════════════════════════════════════════════════════════════════════

    /**
     * ¿Está actualmente en contacto en el eje Y?
     * Si true, el raycast NO se evalúa para este eje.
     */
    public boolean isContactY() {
        return contactY;
    }

    /**
     * ¿Tiene un obstáculo registrado en Y?
     */
    public boolean hasObstacleY() {
        return obstacleY != null;
    }

    /**
     * Obtiene el obstáculo en Y (puede ser null).
     */
    public GameObjects getObstacleY() {
        return obstacleY;
    }

    /**
     * Registra que el raycast detectó un obstáculo en Y.
     * No marca contacto todavía (eso se hace tras verificar bounds.intersects).
     */
    public void setObstacleY(GameObjects obstacle) {
        this.obstacleY = obstacle;
        this.contactY = false; // Todavía no confirmado
    }

    /**
     * Marca que el objeto está ahora en contacto físico con el obstáculo en Y.
     * El raycast se desactiva para este eje.
     */
    public void setContactY(boolean contact) {
        this.contactY = contact;
    }

    /**
     * Limpia el estado del eje Y.
     * Llamar cuando el contacto se resuelve o el obstáculo desaparece.
     */
    public void clearY() {
        this.contactY = false;
        this.obstacleY = null;
    }

    // ═════════════════════════════════════════════════════════════════════
    // ══ UTILIDADES
    // ═════════════════════════════════════════════════════════════════════

    /**
     * ¿Tiene algún contacto activo (X o Y)?
     */
    public boolean hasAnyContact() {
        return contactX || contactY;
    }

    /**
     * Limpia TODO el estado (ambos ejes).
     * Llamar cuando el objeto se destruye o cambia de mundo.
     */
    public void clear() {
        clearX();
        clearY();
    }

    /**
     * Información de debug.
     */
    @Override
    public String toString() {
        return String.format("RaycastState[X: %s(%s), Y: %s(%s)]",
            contactX ? "CONTACT" : (obstacleX != null ? "DETECTED" : "CLEAR"),
            obstacleX != null ? obstacleX.getClass().getSimpleName() : "none",
            contactY ? "CONTACT" : (obstacleY != null ? "DETECTED" : "CLEAR"),
            obstacleY != null ? obstacleY.getClass().getSimpleName() : "none"
        );
    }
}
