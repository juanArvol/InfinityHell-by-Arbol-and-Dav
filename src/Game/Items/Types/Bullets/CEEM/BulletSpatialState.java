package Game.Items.Types.Bullets.CEEM;

/**
 * Estados del ciclo de persistencia espacial de un proyectil.
 * 
 * ── HRFC — CEEM Especializado para Bullets ───────────────────────────────
 * 
 * RESPONSABILIDAD:
 *   Representa la decisión del CEEM sobre la persistencia espacial de un
 *   proyectil basándose en su relación con el área activa (viewport).
 * 
 * SEPARACIÓN DE RESPONSABILIDADES:
 *   - bulletLifeTime (BulletLife) → tiempo de vida intrínseco del proyectil
 *   - BulletSpatialState (CEEM)   → persistencia espacial fuera del área activa
 * 
 * Son dos dimensiones independientes:
 *   - Un bullet puede estar ACTIVE espacialmente pero cerca de expirar por bulletLifeTime
 *   - Un bullet puede tener bulletLifeTime largo pero EXPIRE espacialmente por estar muy lejos
 * 
 * ESTADOS:
 * 
 *   ACTIVE
 *     El proyectil está dentro del área activa (viewport).
 *     No existe presión de expiración espacial.
 *     El tiempo fuera de área está en cero.
 * 
 *   GRACE
 *     El proyectil salió del área activa recientemente.
 *     Tiene un presupuesto de persistencia basado en:
 *       - velocidad final runtime
 *       - rareza del tipo de proyectil
 *       - distancia respecto al área activa
 *       - tiempo acumulado fuera del área
 *     Continúa vivo mientras su permanencia exterior sea razonable.
 * 
 *   EXPIRE
 *     El proyectil excedió su presupuesto de persistencia espacial.
 *     Debe ser marcado para destrucción por el sistema de lifecycle.
 *     La liberación física sigue siendo responsabilidad de ProjectilePool.
 * 
 * GUARANTÍAS:
 *   - bulletLifeTime NO es reemplazado ni duplicado
 *   - El CEEM solo produce una DECISIÓN, no ejecuta pooling
 *   - La transición EXPIRE → release es responsabilidad del caller
 */
public enum BulletSpatialState {
    
    /**
     * Proyectil dentro del área activa.
     * No existe presión de expiración espacial.
     */
    ACTIVE,
    
    /**
     * Proyectil fuera del área activa con presupuesto de persistencia válido.
     * Continúa activo mientras la permanencia exterior sea razonable.
     */
    GRACE,
    
    /**
     * Proyectil que excedió su presupuesto de persistencia espacial.
     * Debe ser marcado para destrucción.
     */
    EXPIRE
}
