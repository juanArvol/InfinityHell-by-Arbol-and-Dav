package Game.Items.Types.Weapons.WeaponType.FireMode;

/**
 * Resolución de parámetros de FireMode — resultado de consulta idempotente.
 * 
 * ── HRFC — Separación de consulta de FireMode de su ejecución ─────────────
 * 
 * Este record encapsula únicamente los multiplicadores de resolución del FireMode,
 * sin incluir la decisión de disparo (shouldShoot). Esto permite que ProjectilePreview
 * consulte los parámetros actuales sin ejecutar la lógica de input ni mutar estado.
 * 
 * DIFERENCIA CON FireModeResult:
 * 
 *   FireModeResult    → operación de ejecución (handleInput)
 *   FireModeResolution → operación de consulta (queryResolution)
 * 
 *   FireModeResult:
 *     - Incluye shouldShoot (decisión de disparo)
 *     - Resultado de procesar input y mutar estado
 *     - Usado en disparo real
 * 
 *   FireModeResolution:
 *     - Solo multiplicadores (sin decisión de disparo)
 *     - Resultado de consultar estado actual
 *     - Usado en ProjectilePreview
 * 
 * ── INMUTABILIDAD ─────────────────────────────────────────────────────────
 * 
 * Record inmutable que garantiza que la consulta no produce side-effects.
 * Los multiplicadores reflejan el estado actual del FireMode sin modificarlo.
 * 
 * ── CASOS DE USO ──────────────────────────────────────────────────────────
 * 
 * 1. ProjectilePreview:
 *    - Consulta multiplicadores actuales para calcular trayectoria visual
 *    - No debe disparar ni consumir recursos
 *    - Puede llamarse repetidamente sin efectos
 * 
 * 2. UI de estadísticas:
 *    - Mostrar damage/speed potencial según estado actual del FireMode
 *    - Ejemplo: ChargeMode mostrando damage creciente mientras se carga
 * 
 * @param damageMultiplier multiplicador de daño según estado actual del FireMode
 * @param speedMultiplier multiplicador de velocidad según estado actual del FireMode
 */
public record FireModeResolution(
    double damageMultiplier,
    double speedMultiplier
) {
    
    /**
     * Resolución neutra — sin modificaciones de FireMode.
     * Equivale a multiplicadores 1.0 en ambos parámetros.
     */
    public static final FireModeResolution NEUTRAL = new FireModeResolution(1.0, 1.0);
    
    /**
     * Construye FireModeResolution validando que los multiplicadores sean positivos.
     * 
     * @param damageMultiplier multiplicador de daño (debe ser > 0)
     * @param speedMultiplier multiplicador de velocidad (debe ser > 0)
     * @throws IllegalArgumentException si algún multiplicador es <= 0
     */
    public FireModeResolution {
        if (damageMultiplier <= 0) {
            throw new IllegalArgumentException("damageMultiplier debe ser positivo, recibido: " + damageMultiplier);
        }
        if (speedMultiplier <= 0) {
            throw new IllegalArgumentException("speedMultiplier debe ser positivo, recibido: " + speedMultiplier);
        }
    }
    
    /**
     * Convierte esta resolución a FireModeResult añadiendo decisión de disparo.
     * 
     * Útil cuando se necesita compatibilidad con APIs que esperan FireModeResult
     * pero la información de disparo se determina externamente.
     * 
     * @param shouldShoot true si se debe disparar, false en caso contrario
     * @return FireModeResult equivalente con la decisión de disparo especificada
     */
    public FireModeResult toFireModeResult(boolean shouldShoot) {
        return new FireModeResult(shouldShoot, damageMultiplier, speedMultiplier);
    }
}