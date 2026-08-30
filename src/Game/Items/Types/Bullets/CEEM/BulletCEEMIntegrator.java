package Game.Items.Types.Bullets.CEEM;

import Game.Engine.Camera.GameCamera;
import Game.Items.Types.Bullets.Definition.Bullet;

/**
 * Integrador del BulletCEEM con el ciclo de vida de Bullet.
 * 
 * ── HRFC — CEEM Especializado para Bullets ───────────────────────────────
 * 
 * RESPONSABILIDAD:
 *   Conectar el modelo de evaluación BulletCEEM con el lifecycle de Bullet.
 *   Evalúa bullets, obtiene decisión, y ejecuta acción apropiada (kill si EXPIRE).
 * 
 * ARQUITECTURA:
 *   BulletCEEM (modelo) → BulletCEEMDecision (resultado) 
 *   → BulletCEEMIntegrator (integración) → Bullet.getBulletLife().kill()
 *   → ProjectilePool.release() (auto-devolución)
 * 
 * SEPARACIÓN DE RESPONSABILIDADES:
 *   - BulletCEEM: modelo puro de evaluación, no conoce el lifecycle de Bullet
 *   - BulletCEEMIntegrator: ejecuta la decisión del modelo en el mundo
 *   - BulletLife: marca muerte
 *   - ProjectilePool: gestiona reciclaje
 * 
 * USO TÍPICO:
 * <pre>
 * // Configuración inicial (una vez)
 * BulletCEEM ceem = new BulletCEEM();
 * BulletCEEMIntegrator integrator = new BulletCEEMIntegrator(ceem);
 * 
 * // Cada frame, después de bullet.update()
 * for (Bullet bullet : activeBullets) {
 *     integrator.evaluate(bullet, camera, deltaTime);
 * }
 * </pre>
 * 
 * GARANTÍAS:
 *   - bulletLifeTime NO es modificado (solo reading de BulletLife)
 *   - El integrador solo invoca kill() cuando CEEM decide EXPIRE
 *   - La decisión se respeta completamente
 *   - El pooling sigue su flujo normal (Bullet.isPendingDestruction → release)
 */
public final class BulletCEEMIntegrator {
    
    private final BulletCEEM ceem;
    
    // Diagnósticos opcionales
    private int evaluationCount = 0;
    private int expiredCount = 0;
    private int graceCount = 0;
    private int activeCount = 0;
    
    /**
     * Crea un integrador con el modelo CEEM especificado.
     * 
     * @param ceem el modelo de evaluación (no null)
     */
    public BulletCEEMIntegrator(BulletCEEM ceem) {
        if (ceem == null) {
            throw new IllegalArgumentException("CEEM cannot be null");
        }
        this.ceem = ceem;
    }
    
    /**
     * Evalúa un bullet y ejecuta la decisión del CEEM.
     * 
     * Este método debe llamarse cada frame para cada bullet activo,
     * después de bullet.update() y después de bullet.updateOffScreenTracking().
     * 
     * PRECONDICIÓN:
     *   - bullet.updateOffScreenTracking(camera, deltaTime) ya fue llamado
     *   - bullet tiene OffScreenTracker configurado (si no, se ignora)
     * 
     * POSTCONDICIÓN:
     *   - Si decisión es EXPIRE: bullet.getBulletLife().kill() es invocado
     *   - Si decisión es GRACE o ACTIVE: no se modifica el bullet
     * 
     * @param bullet el proyectil a evaluar
     * @param camera cámara activa del juego
     * @param deltaTime tiempo transcurrido (para contexto del CEEM)
     * @return la decisión tomada (ACTIVE, GRACE, o EXPIRE)
     */
    public BulletCEEMDecision evaluate(Bullet bullet, GameCamera camera, double deltaTime) {
        evaluationCount++;
        
        // Evaluar con el modelo
        BulletCEEMDecision decision = ceem.evaluate(bullet, camera, deltaTime);
        
        // Ejecutar la decisión
        switch (decision.state()) {
            case EXPIRE:
                // Marcar para destrucción
                bullet.getBulletLife().kill();
                expiredCount++;
                break;
                
            case GRACE:
                // Continuar vivo, pero bajo observación
                graceCount++;
                break;
                
            case ACTIVE:
                // Dentro del área activa, sin presión
                activeCount++;
                break;
        }
        
        return decision;
    }
    
    /**
     * Evalúa un bullet sin ejecutar la decisión.
     * 
     * Útil para análisis o debugging sin efectos secundarios.
     * 
     * @param bullet el proyectil a evaluar
     * @param camera cámara activa del juego
     * @param deltaTime tiempo transcurrido
     * @return la decisión del CEEM (sin ejecutar)
     */
    public BulletCEEMDecision evaluateOnly(Bullet bullet, GameCamera camera, double deltaTime) {
        return ceem.evaluate(bullet, camera, deltaTime);
    }
    
    // ── Diagnósticos ──────────────────────────────────────────────────────
    
    /**
     * @return número total de evaluaciones realizadas desde la creación
     */
    public int getEvaluationCount() {
        return evaluationCount;
    }
    
    /**
     * @return número de bullets marcados como EXPIRE desde la creación
     */
    public int getExpiredCount() {
        return expiredCount;
    }
    
    /**
     * @return número de bullets en estado GRACE desde la creación
     */
    public int getGraceCount() {
        return graceCount;
    }
    
    /**
     * @return número de bullets en estado ACTIVE desde la creación
     */
    public int getActiveCount() {
        return activeCount;
    }
    
    /**
     * Resetea los contadores de diagnóstico.
     */
    public void resetDiagnostics() {
        evaluationCount = 0;
        expiredCount = 0;
        graceCount = 0;
        activeCount = 0;
    }
    
    /**
     * @return el modelo CEEM usado por este integrador
     */
    public BulletCEEM getCEEM() {
        return ceem;
    }
}
