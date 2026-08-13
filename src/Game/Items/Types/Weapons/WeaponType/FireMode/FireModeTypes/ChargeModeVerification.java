package Game.Items.Types.Weapons.WeaponType.FireMode.FireModeTypes;

import Game.Items.Types.Weapons.WeaponType.WeaponComport;
import Game.Items.Types.Weapons.WeaponType.FireMode.FireModeResult;
import Game.Items.Types.Weapons.WeaponType.FireMode.FireModeResolution;

/**
 * Verificación de separación Query/Execution en ChargeMode.
 * 
 * ── HRFC — Separación de consulta de FireMode de su ejecución ─────────────
 * 
 * Esta clase demuestra que la separación funciona correctamente:
 * 
 * 1. handleInput() procesa input y muta estado (chargeTime)
 * 2. queryResolution() consulta estado sin mutarlo
 * 3. Múltiples llamadas a queryResolution() son idempotentes
 * 4. El preview puede consultar sin interferir con el disparo real
 * 
 * CASO DE PRUEBA: ChargeMode cargando
 * - Estado inicial: chargeTime = 0
 * - Simular botón mantenido por 30 frames
 * - Verificar que queryResolution() no incrementa chargeTime
 * - Verificar que handleInput() sí incrementa chargeTime
 * - Verificar que ambos calculan multiplicadores correctamente
 */
public class ChargeModeVerification {
    
    /**
     * Ejecuta verificación completa de separación Query/Execution.
     * 
     * @param args argumentos de línea de comandos (no utilizados)
     */
    public static void main(String[] args) {
        System.out.println("=== VERIFICACIÓN SEPARACIÓN FIREMODE QUERY/EXECUTION ===");
        System.out.println();
        
        // ── SETUP ─────────────────────────────────────────────────────────
        ChargeMode chargeMode = new ChargeMode();
        WeaponComport mockWeapon = null; // ChargeMode no usa este parámetro
        
        System.out.println("Estado inicial:");
        System.out.println("  chargeTime: " + getChargeTime(chargeMode));
        System.out.println();
        
        // ── FASE 1: Simular carga de 30 frames con handleInput() ─────────
        System.out.println("FASE 1: Simulando carga con handleInput()");
        for (int i = 0; i < 30; i++) {
            FireModeResult result = chargeMode.handleInput(true, false, mockWeapon);
            if (i % 10 == 9) { // Mostrar cada 10 frames
                System.out.printf("  Frame %d: chargeTime=%d, shouldShoot=%b, mult=%.2f%n",
                    i + 1, getChargeTime(chargeMode), result.shouldShoot(), result.getDamageMultiplier());
            }
        }
        System.out.println("  Estado final: chargeTime = " + getChargeTime(chargeMode));
        System.out.println();
        
        // ── FASE 2: Verificar que queryResolution() no muta estado ──────
        System.out.println("FASE 2: Verificando idempotencia de queryResolution()");
        int chargeTimeBeforeQuery = getChargeTime(chargeMode);
        
        // Múltiples llamadas a queryResolution()
        for (int i = 0; i < 5; i++) {
            FireModeResolution resolution = chargeMode.queryResolution(true, mockWeapon);
            System.out.printf("  Query %d: chargeTime=%d (sin cambio), mult=%.2f%n",
                i + 1, getChargeTime(chargeMode), resolution.damageMultiplier());
        }
        
        int chargeTimeAfterQuery = getChargeTime(chargeMode);
        boolean isIdempotent = (chargeTimeBeforeQuery == chargeTimeAfterQuery);
        
        System.out.printf("  Idempotencia: %s (chargeTime: %d → %d)%n",
            isIdempotent ? "✓ CORRECTA" : "✗ FALLIDA", chargeTimeBeforeQuery, chargeTimeAfterQuery);
        System.out.println();
        
        // ── FASE 3: Verificar consistencia de multiplicadores ────────────
        System.out.println("FASE 3: Verificando consistencia de multiplicadores");
        
        // Obtener multiplicadores via queryResolution()
        FireModeResolution resolution = chargeMode.queryResolution(true, mockWeapon);
        double queryMultiplier = resolution.damageMultiplier();
        
        // Calcular multiplicador esperado manualmente
        double expectedMultiplier = 1.0 + ((double) getChargeTime(chargeMode) / 60);
        
        boolean isConsistent = Math.abs(queryMultiplier - expectedMultiplier) < 0.001;
        
        System.out.printf("  Multiplicador via query: %.3f%n", queryMultiplier);
        System.out.printf("  Multiplicador esperado:  %.3f%n", expectedMultiplier);
        System.out.printf("  Consistencia: %s%n", isConsistent ? "✓ CORRECTA" : "✗ FALLIDA");
        System.out.println();
        
        // ── FASE 4: Verificar disparo real no interferido ────────────────
        System.out.println("FASE 4: Verificando que disparo real funciona normalmente");
        
        // Soltar botón para disparar
        FireModeResult shootResult = chargeMode.handleInput(false, false, mockWeapon);
        
        System.out.printf("  Disparo: shouldShoot=%b, mult=%.3f%n", 
            shootResult.shouldShoot(), shootResult.getDamageMultiplier());
        System.out.printf("  Estado post-disparo: chargeTime=%d (debería ser 0)%n", 
            getChargeTime(chargeMode));
        
        boolean shootingWorks = shootResult.shouldShoot() && getChargeTime(chargeMode) == 0;
        System.out.printf("  Disparo funcional: %s%n", shootingWorks ? "✓ CORRECTO" : "✗ FALLIDO");
        System.out.println();
        
        // ── RESUMEN ────────────────────────────────────────────────────────
        System.out.println("=== RESUMEN DE VERIFICACIÓN ===");
        System.out.printf("  ✓ Idempotencia de queryResolution(): %s%n", isIdempotent ? "PASS" : "FAIL");
        System.out.printf("  ✓ Consistencia de multiplicadores:   %s%n", isConsistent ? "PASS" : "FAIL");
        System.out.printf("  ✓ Funcionalidad de disparo:          %s%n", shootingWorks ? "PASS" : "FAIL");
        
        boolean allTestsPass = isIdempotent && isConsistent && shootingWorks;
        System.out.println();
        System.out.printf("RESULTADO FINAL: %s%n", 
            allTestsPass ? "✓ SEPARACIÓN CORRECTA" : "✗ SEPARACIÓN FALLIDA");
        
        if (allTestsPass) {
            System.out.println();
            System.out.println("La separación Query/Execution cumple con el criterio de aceptación:");
            System.out.println("- ProjectilePreview puede consultar resolución sin side-effects");
            System.out.println("- Múltiples llamadas a queryResolution() son idempotentes");
            System.out.println("- El disparo real permanece funcional e independiente");
        }
    }
    
    /**
     * Obtiene el chargeTime actual usando reflexión (para testing).
     * En implementación real, esto sería un getter público o package-private.
     */
    private static int getChargeTime(ChargeMode chargeMode) {
        try {
            java.lang.reflect.Field field = ChargeMode.class.getDeclaredField("chargeTime");
            field.setAccessible(true);
            return field.getInt(chargeMode);
        } catch (Exception e) {
            System.err.println("Error accediendo a chargeTime: " + e.getMessage());
            return -1;
        }
    }
}