package Game.Items.Types.Bullets.CEEM;

/**
 * Decisión inmutable producida por el BulletCEEM sobre la persistencia espacial de un proyectil.
 * 
 * ── HRFC — CEEM Especializado para Bullets ───────────────────────────────
 * 
 * ARQUITECTURA:
 *   BulletCEEM evalúa → produce BulletCEEMDecision → caller actúa
 * 
 * SEPARACIÓN DE RESPONSABILIDADES:
 *   - BulletCEEM: modelo de evaluación, cálculo de presupuesto, lógica de decisión
 *   - BulletCEEMDecision: resultado inmutable de la evaluación
 *   - Bullet lifecycle: ejecuta la decisión (kill si EXPIRE)
 *   - ProjectilePool: gestiona el reciclaje físico
 * 
 * CONTENIDO:
 *   - state: ACTIVE, GRACE, o EXPIRE
 *   - diagnostic: información contextual para debugging/análisis
 * 
 * USO TÍPICO:
 * <pre>
 * BulletCEEMDecision decision = ceem.evaluate(bullet, camera, deltaTime);
 * if (decision.state() == BulletSpatialState.EXPIRE) {
 *     bullet.getBulletLife().kill();  // Marcar para destrucción
 * }
 * </pre>
 */
public final class BulletCEEMDecision {
    
    private final BulletSpatialState state;
    private final String diagnostic;
    
    /**
     * Constructor package-private — solo BulletCEEM produce decisiones.
     */
    BulletCEEMDecision(BulletSpatialState state, String diagnostic) {
        if (state == null) {
            throw new IllegalArgumentException("State cannot be null");
        }
        this.state = state;
        this.diagnostic = diagnostic != null ? diagnostic : "";
    }
    
    /**
     * Estado espacial del proyectil.
     * 
     * @return ACTIVE, GRACE, o EXPIRE
     */
    public BulletSpatialState state() {
        return state;
    }
    
    /**
     * Información diagnóstica sobre la decisión.
     * 
     * Ejemplos:
     *   "Inside active region"
     *   "Outside 2.3s, budget 5.0s (velocity=300, rarity=RARE)"
     *   "Exceeded spatial budget - distance=800px, time=5.2s"
     * 
     * @return diagnostic string (never null, may be empty)
     */
    public String diagnostic() {
        return diagnostic;
    }
    
    /**
     * Convenience: ¿requiere expiración inmediata?
     * 
     * @return true si state == EXPIRE
     */
    public boolean shouldExpire() {
        return state == BulletSpatialState.EXPIRE;
    }
    
    @Override
    public String toString() {
        return String.format("BulletCEEMDecision[state=%s, diagnostic=%s]", 
                             state, diagnostic);
    }
}
