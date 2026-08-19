package Game.Items.Types.Weapons.WeaponType.FireMode.FireModeTypes;

import Game.Items.Types.Weapons.WeaponType.FireMode.FireModeResolution;
import Game.Items.Types.Weapons.WeaponType.FireMode.FireModeResult;
import Game.Items.Types.Weapons.WeaponType.FireMode.iFireMode;
import Game.Items.Types.Weapons.WeaponType.WeaponComport;

/**
 * Modo de disparo por carga — el daño y velocidad escalan con el tiempo
 * que el botón se mantiene presionado.
 *
 * ── HRFC — Unified DeltaTime Migration & Temporal Model Completion ────────
 *
 * MIGRACIÓN TEMPORAL:
 *   ChargeMode ahora usa tiempo real en segundos en lugar de contadores
 *   de frames. Esto garantiza que el tiempo de carga es independiente
 *   del framerate.
 *
 *   ANTES (frame-based):
 *     chargeTime++ en cada update
 *     maxCharge = 60 frames
 *     A 31 FPS: 60 frames = 1.94 segundos de carga
 *     A 60 FPS: 60 frames = 1.00 segundos de carga
 *     A 120 FPS: 60 frames = 0.50 segundos de carga
 *
 *   AHORA (time-based):
 *     chargeElapsed += deltaTime
 *     maxChargeSeconds = 1.0 segundos
 *     A cualquier FPS: maxCharge = 1.00 segundos de carga
 *
 * ── COMPORTAMIENTO ────────────────────────────────────────────────────────
 *
 * Mientras se mantiene presionado:
 *   - chargeElapsed incrementa con el tiempo real
 *   - El multiplicador escala linealmente: 1.0 → 2.0
 *
 * Al soltar:
 *   - Se dispara con los multiplicadores acumulados
 *   - chargeElapsed se resetea a 0
 *
 * Si se suelta sin carga acumulada:
 *   - No se dispara (retorna shouldFire=false)
 *
 * ── CONFIGURACIÓN ─────────────────────────────────────────────────────────
 *
 * maxChargeSeconds: tiempo para alcanzar el multiplicador máximo (2.0x)
 * Por defecto: 1.0 segundos (equivalente a 60 frames @ 60 FPS legacy)
 */
/**
 * Modo de disparo por carga — el daño y velocidad escalan con el tiempo
 * que el botón se mantiene presionado.
 *
 * ── HRFC — Unified DeltaTime Migration & Temporal Model Completion ────────
 *
 * MIGRACIÓN TEMPORAL:
 *   ChargeMode ahora usa tiempo real en segundos en lugar de contadores
 *   de frames. Esto garantiza que el tiempo de carga es independiente
 *   del framerate.
 *
 *   ANTES (frame-based):
 *     chargeTime++ en cada update
 *     maxCharge = 60 frames
 *     A 31 FPS: 60 frames = 1.94 segundos de carga
 *     A 60 FPS: 60 frames = 1.00 segundos de carga
 *     A 120 FPS: 60 frames = 0.50 segundos de carga
 *
 *   AHORA (time-based):
 *     chargeElapsed += deltaTime
 *     maxChargeSeconds = 1.0 segundos
 *     A cualquier FPS: maxCharge = 1.00 segundos de carga
 *
 * ── COMPORTAMIENTO ────────────────────────────────────────────────────────
 *
 * Mientras se mantiene presionado:
 *   - handleInput() detecta held=true y marca isCharging=true
 *   - update(deltaTime) acumula chargeElapsed mientras isCharging
 *   - El multiplicador escala linealmente: 1.0 → 2.0
 *
 * Al soltar:
 *   - handleInput() detecta held=false con carga acumulada
 *   - Se dispara con los multiplicadores acumulados
 *   - chargeElapsed se resetea a 0
 *
 * Si se suelta sin carga acumulada:
 *   - No se dispara (retorna shouldFire=false)
 *
 * ── CONFIGURACIÓN ─────────────────────────────────────────────────────────
 *
 * maxChargeSeconds: tiempo para alcanzar el multiplicador máximo (2.0x)
 * Por defecto: 1.0 segundos (equivalente a 60 frames @ 60 FPS legacy)
 */
public class ChargeMode implements iFireMode {

    private double chargeElapsed = 0.0;
    private boolean isCharging = false;  // true mientras se mantiene presionado
    private final double maxChargeSeconds;

    /**
     * Constructor con tiempo de carga configurable.
     *
     * @param maxChargeSeconds tiempo en segundos para carga completa
     */
    public ChargeMode(double maxChargeSeconds) {
        this.maxChargeSeconds = Math.max(0.1, maxChargeSeconds); // mínimo 0.1s
    }

    /**
     * Constructor por defecto (1 segundo de carga).
     * Equivalente al comportamiento legacy de 60 frames @ 60 FPS.
     */
    public ChargeMode() {
        this(1.0);
    }

    /**
     * Factory method para compatibilidad con código legacy que usaba frames.
     *
     * @param maxChargeFrames cantidad de frames que tardaba la carga completa
     * @param targetFps framerate asumido por el código legacy (típicamente 60)
     * @return ChargeMode configurado con tiempo equivalente en segundos
     *
     * @deprecated Usar constructor con segundos directamente
     */
    @Deprecated
    public static ChargeMode fromFrames(int maxChargeFrames, double targetFps) {
        return new ChargeMode(maxChargeFrames / targetFps);
    }

    @Override
    public FireModeResult handleInput(
            boolean held,
            boolean pressed,
            WeaponComport weapon) {

        if (held) {
            // Mantiene presionado: activar flag de carga
            // (update(deltaTime) acumulará tiempo mientras isCharging=true)
            isCharging = true;
            return new FireModeResult(false, 1, 1);
        }

        // Soltar el botón
        isCharging = false;

        if (chargeElapsed > 0.0) {
            // Suelta tras cargar: disparar con multiplicadores acumulados

            // Calcular multiplicador basado en tiempo de carga
            // Escala linealmente de 1.0 (sin carga) a 2.0 (carga completa)
            double chargeRatio = Math.min(1.0, chargeElapsed / maxChargeSeconds);
            double multiplier = 1.0 + chargeRatio;

            // Resetear carga para el próximo disparo
            chargeElapsed = 0.0;

            return new FireModeResult(
                    true,
                    multiplier,
                    multiplier
            );
        }

        // No dispara: sin carga acumulada
        return new FireModeResult(false, 1, 1);
    }

    @Override
    public FireModeResolution queryResolution(
            boolean held,
            WeaponComport weapon) {
        
        // ── CONSULTA IDEMPOTENTE DE MULTIPLICADORES ───────────────────────
        // 
        // ChargeMode calcula multiplicadores basándose en el tiempo de carga actual.
        // Esta operación NO muta chargeElapsed, solo lee su valor actual.
        // 
        // COMPORTAMIENTO:
        // - Si held=false y chargeElapsed=0: multiplicadores neutros (1.0, 1.0)
        // - Si held=false y chargeElapsed>0: multiplicadores de la carga acumulada
        // - Si held=true: multiplicadores de la carga actual (sin incrementar tiempo)
        // 
        // DIFERENCIA CON handleInput():
        // - handleInput(): procesa held → puede resetear chargeElapsed → calcula
        // - queryResolution(): lee chargeElapsed actual → calcula (sin resetear)
        
        if (!held && chargeElapsed == 0.0) {
            // Sin carga acumulada y no presionando → multiplicadores neutros
            return FireModeResolution.NEUTRAL;
        }
        
        // Calcular multiplicadores basándose en carga actual
        // (misma fórmula que handleInput, pero sin mutar estado)
        double chargeRatio = Math.min(1.0, chargeElapsed / maxChargeSeconds);
        double multiplier = 1.0 + chargeRatio;
        
        return new FireModeResolution(multiplier, multiplier);
    }

    /**
     * Actualiza el estado de carga con el tiempo transcurrido.
     *
     * ── HRFC — Unified DeltaTime Migration ───────────────────────────────
     *
     * Este método acumula tiempo de carga cuando isCharging=true.
     * isCharging se activa en handleInput() cuando held=true.
     *
     * @param deltaTime tiempo real del simulation step en segundos
     */
    @Override
    public void update(double deltaTime) {
        if (isCharging) {
            chargeElapsed += deltaTime;
            // Clamp para evitar overflow en cargas extremadamente largas
            if (chargeElapsed > maxChargeSeconds * 2.0) {
                chargeElapsed = maxChargeSeconds * 2.0;
            }
        }
    }

    /**
     * Consulta el tiempo de carga actual (para UI, debug, etc.).
     *
     * @return tiempo acumulado en segundos
     */
    public double getChargeElapsed() {
        return chargeElapsed;
    }

    /**
     * Consulta el tiempo máximo de carga configurado.
     *
     * @return tiempo máximo en segundos
     */
    public double getMaxChargeSeconds() {
        return maxChargeSeconds;
    }

    /**
     * Consulta el ratio de carga actual [0.0, 1.0].
     *
     * @return 0.0 = sin carga, 1.0 = carga completa
     */
    public double getChargeRatio() {
        return Math.min(1.0, chargeElapsed / maxChargeSeconds);
    }

    /**
     * Consulta si actualmente está acumulando carga.
     *
     * @return true si el botón está siendo mantenido presionado
     */
    public boolean isCharging() {
        return isCharging;
    }
}
