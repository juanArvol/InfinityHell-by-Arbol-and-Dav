package Game.Items.Types.Bullets;

/**
 * Protocolo de reset para movimientos con estado interno reutilizables.
 *
 * ── HRFC — Projectile Construction & Transformation Pipeline ─────────────
 *
 * ProjectileMovement es @FunctionalInterface — no puede tener métodos abstractos
 * adicionales sin romper la ergonomía de lambdas y method references.
 *
 * ResettableMovement es una interfaz separada que extiende ProjectileMovement
 * y añade el contrato de reset. Los movimientos stateful que quieran soportar
 * reutilización segura en el pool implementan esta interfaz.
 *
 * ── SEPARACIÓN DE CONCEPTOS ───────────────────────────────────────────────
 *
 * ProjectileMovement:
 *   - Contrato de movimiento (tick).
 *   - @FunctionalInterface — permite lambdas como: bullet -> { ... }
 *   - isStateless() default false — conservador por defecto.
 *
 * ResettableMovement extends ProjectileMovement:
 *   - Contrato adicional para movimientos que PUEDEN resetearse.
 *   - No es @FunctionalInterface (tiene dos métodos abstractos: tick + reset).
 *   - Permite al pool reutilizar instancias stateful con seguridad.
 *
 * ── PROTOCOLO ─────────────────────────────────────────────────────────────
 *
 * ProjectilePool.acquire():
 *   1. Obtiene el movement del behavior.
 *   2. Si movement.isStateless() → puede compartir la instancia (caso simple).
 *   3. Si !movement.isStateless() && movement instanceof ResettableMovement:
 *        → llama reset() y reutiliza la instancia de Bullet del pool.
 *   4. Si !movement.isStateless() && !(movement instanceof ResettableMovement):
 *        → crea una nueva instancia de Bullet (no puede resetear el movement).
 *
 * ── IMPLEMENTACIONES ACTUALES ─────────────────────────────────────────────
 *
 *   SinusoidalMovement  → implementa ResettableMovement (frameCount reseteable).
 *   BoomerangMovement   → implementa ResettableMovement (frameCount reseteable).
 *   OrbitalMovement     → implementa ResettableMovement (angle reseteable).
 *   HomingMovement      → stateless en cuanto a campos propios (sin estado frame).
 *
 * ── USO ───────────────────────────────────────────────────────────────────
 *
 *   public final class SinusoidalMovement implements ResettableMovement {
 *       private int frameCount = 0;
 *
 *       @Override
 *       public void tick(Bullet bullet) { frameCount++; ... }
 *
 *       @Override
 *       public void reset() { frameCount = 0; }
 *
 *       @Override
 *       public boolean isStateless() { return false; } // tiene estado
 *   }
 */
public interface ResettableMovement extends ProjectileMovement {

    /**
     * Resetea el estado interno del movimiento al estado inicial.
     *
     * Después de llamar reset(), el comportamiento del movimiento debe ser
     * idéntico al de una instancia recién creada con los mismos parámetros.
     *
     * Si el reset no es posible de forma segura (ej: el estado depende de
     * una posición de origen capturada al construir), implementar reset()
     * como no-op Y sobreescribir isStateless() retornando false — el pool
     * no reutilizará la instancia.
     */
    void reset();
}
