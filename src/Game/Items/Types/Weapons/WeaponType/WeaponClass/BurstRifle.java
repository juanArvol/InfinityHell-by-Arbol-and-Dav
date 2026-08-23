package Game.Items.Types.Weapons.WeaponType.WeaponClass;

import Game.Items.Types.Weapons.WeaponType.FireMode.FireModeTypes.AutoMode;
import Game.Items.Types.Weapons.WeaponType.WeaponComport;
import Game.Items.Types.Weapons.WeaponType.WeaponStats;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * EJEMPLO DE WEAPON CUSTOM - BurstRifle
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * Este es un ejemplo de cómo implementar un WeaponComport custom sin modificar
 * el sistema central de Items.
 *
 * ── COMPORTAMIENTO ────────────────────────────────────────────────────────
 * • Dispara 3 proyectiles en ráfaga rápida
 * • Cooldown: 0.5 segundos (entre ráfagas completas)
 * • Velocidad de proyectil: 600 u/s (media-alta)
 * • Dispersión baja (0.05 radianes)
 * • Bonus de daño: +5
 *
 * ── MECÁNICA DE RÁFAGA ────────────────────────────────────────────────────
 * Al presionar disparo:
 *   1. Dispara primer proyectil (sin consumir cooldown)
 *   2. Dispara segundo proyectil (sin consumir cooldown)
 *   3. Dispara tercer proyectil (AHORA entra en cooldown)
 *   4. Esperar cooldown antes de la próxima ráfaga
 *
 * ── INTEGRACIÓN ───────────────────────────────────────────────────────────
 * Este comportamiento se registra en WeaponType sin modificar WeaponType.java:
 *
 * ```java
 * WeaponType BURST_RIFLE = WeaponType.register(new WeaponType(
 *     "burst_rifle",
 *     BurstRifle::new,
 *     ItemRarity.UNCOMMON,
 *     "Rifle de Ráfaga",
 *     "Dispara 3 proyectiles en secuencia rápida."
 * ));
 * ```
 *
 * Una vez registrado, funciona igual que las armas builtin:
 *
 * ```java
 * WeaponComport comport = BURST_RIFLE.createComport();
 * ```
 *
 * @see Game.Items.Types.Weapons.WeaponType.WeaponComport clase base
 * @see Game.Mods.ExampleMod.ExampleModInitializer inicialización del mod
 */
public class BurstRifle extends WeaponComport {

    // ══════════════════════════════════════════════════════════════════════
    // ESTADO DE RÁFAGA
    // ══════════════════════════════════════════════════════════════════════

    private int burstCounter = 0;
    private static final int BURST_SIZE = 3;
    private static final double BURST_DELAY = 0.05; // 50ms entre disparos de la ráfaga

    private double burstTimer = 0.0;
    private boolean inBurst = false;

    // ══════════════════════════════════════════════════════════════════════
    // STATS BASE
    // ══════════════════════════════════════════════════════════════════════

    
    public BurstRifle(){ 
        super( new WeaponStats(
            0.5,
            1, 
            0.05,
            5.0,
            600.0
        ), new AutoMode(), 
        10,
        10,
        "Gun.wav");
    }

    // ══════════════════════════════════════════════════════════════════════
    // LÓGICA DE DISPARO
    // ══════════════════════════════════════════════════════════════════════

   
    /* public boolean tryShoot() {
        // Si estamos en cooldown, no se puede disparar
        if (!isReady() && !inBurst) {
            return false;
        }
        
        // Iniciar ráfaga si no estamos en una
        if (!inBurst) {
            inBurst = true;
            burstCounter = 0;
            burstTimer = 0.0;
        }
        
        // Disparar si es el turno del proyectil actual
        if (burstTimer <= 0.0) {
            burstCounter++;
            
            // Si completamos la ráfaga, entrar en cooldown
            if (burstCounter >= BURST_SIZE) {
                startCooldown();
                inBurst = false;
                burstCounter = 0;
            } else {
                // Preparar siguiente disparo de la ráfaga
                burstTimer = BURST_DELAY;
            }
            
            return true; // ✅ Disparar este frame
        }
        
        return false; // ⏳ Esperar delay entre disparos de ráfaga
    } */

    @Override
    public void update(double deltaTime) {
        super.update(deltaTime);
        
        // Actualizar timer de ráfaga
        if (inBurst && burstTimer > 0.0) {
            burstTimer -= deltaTime;
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // METADATA
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Información adicional para UI (opcional).
     */
    public int getBurstSize() {
        return BURST_SIZE;
    }

    public double getBurstDelay() {
        return BURST_DELAY;
    }

    public boolean isInBurst() {
        return inBurst;
    }

    public int getCurrentBurstCount() {
        return burstCounter;
    }

    // ══════════════════════════════════════════════════════════════════════
    // NOTAS DE IMPLEMENTACIÓN
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Este es un ejemplo funcional de arma de ráfaga. Posibles mejoras:
     *
     * 1. Aumentar dispersión progresivamente en la ráfaga (recoil)
     * 2. Sonidos diferentes para cada disparo de la ráfaga
     * 3. Animación de retroceso visual
     * 4. Reducir daño de disparos 2 y 3 para balance
     * 5. Permitir cancelar ráfaga con soltar input
     *
     * El punto clave es que TODO esto se puede implementar sin modificar
     * ningún archivo del sistema central de Items.
     */
}
