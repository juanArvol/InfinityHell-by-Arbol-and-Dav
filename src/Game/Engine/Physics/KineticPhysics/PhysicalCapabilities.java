package Game.Engine.Physics.KineticPhysics;

/**
 * Capacidades físicas de una entidad — separadas de la masa.
 *
 * ── HRFC — Kinetic Physics: Forces, Impulses & Motion Intent ─────────────
 *
 * Este objeto almacena las capacidades MUSCULARES y DE FUERZA de una entidad,
 * independientes de su masa física. Permite que dos entidades con la misma
 * masa puedan tener diferente capacidad de salto o producir diferente fuerza.
 *
 * Ejemplo conceptual:
 *
 *   Entity A: mass=40, jumpCapacity=15, strengthMultiplier=1.0
 *   Entity B: mass=40, jumpCapacity=15, strengthMultiplier=2.0
 *
 * Ambas tienen la misma masa, pero B puede producir el doble de fuerza.
 *
 * ── SEPARACIÓN MASA vs FUERZA ─────────────────────────────────────────────
 *
 * NO usar:
 *   mass → representación de strength
 *
 * Son conceptos DIFERENTES:
 *   - mass:     propiedad física (afecta Δv = F/m)
 *   - strength: capacidad muscular (afecta F generada)
 *
 * Un personaje puede tener:
 *   mass=80, strengthMultiplier=2.0  → pesado pero fuerte
 * Otro:
 *   mass=40, strengthMultiplier=0.8  → ligero pero débil
 *
 * ── MODIFICADORES EN RUNTIME ──────────────────────────────────────────────
 *
 * Las capacidades físicas deben poder cambiar durante runtime:
 *
 *   - Entrenamiento/progresión: strengthMultiplier = 1.0 → 1.3
 *   - Amuletos/equipamiento:    superJumpMultiplier = 1.0 → 2.0
 *   - Buffs temporales:         forceOutput ×8 (Gym Rat)
 *   - Debuffs:                  jumpCapacity ×0.5 (exhaustion)
 *
 * El sistema de física NO debe conocer los nombres de buffs/amuletos.
 * Solo debe recibir el resultado numérico.
 *
 * ── USO ───────────────────────────────────────────────────────────────────
 *
 * PlayerPhysics/BulletPhysics pueden contener una instancia de
 * PhysicalCapabilities. Los Motion Intent resolvers consultan estas
 * capacidades para calcular los impulsos necesarios.
 *
 * Ejemplo:
 *
 *   JumpIntentResolver.resolve(capabilities, physics) {
 *       double effectiveHeight = capabilities.getEffectiveJumpHeight();
 *       double v0 = sqrt(2 × g × effectiveHeight);
 *       double impulse = physics.getMass() × v0;
 *       physics.addForce(0, -impulse);
 *   }
 *
 * ── INMUTABILIDAD vs MUTABILIDAD ──────────────────────────────────────────
 *
 * Esta versión es MUTABLE para permitir modificaciones en runtime sin
 * reconstruir la entidad completa. Los modificadores pueden cambiar
 * dinámicamente mientras el juego está en ejecución.
 *
 * Thread-safety: NO thread-safe. Se asume que las capacidades se modifican
 * y consultan desde el mismo thread (game loop).
 */
public class PhysicalCapabilities {

    // ── Capacidades Base ──────────────────────────────────────────────────

    /**
     * Altura base de salto (en unidades del mundo, típicamente píxeles).
     * Representa la altura que la entidad puede alcanzar bajo condiciones
     * físicas normales (gravity actual, sin modificadores).
     *
     * NO es la velocidad de salto — es la altura física objetivo.
     */
    private double baseJumpHeight;

    /**
     * Multiplicador de fuerza muscular.
     * Escala la capacidad de producir fuerza en todas las mecánicas cinéticas.
     *
     * Rango típico:
     *   1.0  = fuerza normal
     *   1.5  = 50% más fuerte (entrenamiento)
     *   2.0  = doble fuerza (buff significativo)
     *   0.5  = debilitado (herido, exhausted)
     */
    private double strengthMultiplier;

    /**
     * Multiplicador de producción de fuerza para mecánicas específicas.
     * Permite buffs/debuffs que amplifican la salida de fuerza sin
     * cambiar la fuerza base (ej: Gym Rat buff ×8).
     *
     * Se aplica DESPUÉS de strengthMultiplier:
     *   effectiveForce = baseForce × strengthMultiplier × forceOutputMultiplier
     */
    private double forceOutputMultiplier;

    /**
     * Multiplicador de capacidad de salto.
     * Permite modificar específicamente el salto sin afectar otras
     * capacidades de fuerza (ej: Super Jump amulet ×2).
     *
     * Se aplica DESPUÉS de strengthMultiplier:
     *   effectiveJumpHeight = baseJumpHeight × strengthMultiplier × jumpCapacityMultiplier
     */
    private double jumpCapacityMultiplier;

    // ── Constructor ───────────────────────────────────────────────────────

    /**
     * Crea capacidades físicas con valores base.
     *
     * @param baseJumpHeight altura base de salto en unidades del mundo
     */
    public PhysicalCapabilities(double baseJumpHeight) {
        this(baseJumpHeight, 1.0, 1.0, 1.0);
    }

    /**
     * Crea capacidades físicas con modificadores iniciales.
     *
     * @param baseJumpHeight         altura base de salto
     * @param strengthMultiplier     multiplicador de fuerza muscular
     * @param forceOutputMultiplier  multiplicador de salida de fuerza
     * @param jumpCapacityMultiplier multiplicador de capacidad de salto
     */
    public PhysicalCapabilities(double baseJumpHeight,
                                double strengthMultiplier,
                                double forceOutputMultiplier,
                                double jumpCapacityMultiplier) {
        if (baseJumpHeight < 0) {
            throw new IllegalArgumentException("baseJumpHeight debe ser >= 0");
        }
        if (strengthMultiplier < 0) {
            throw new IllegalArgumentException("strengthMultiplier debe ser >= 0");
        }
        if (forceOutputMultiplier < 0) {
            throw new IllegalArgumentException("forceOutputMultiplier debe ser >= 0");
        }
        if (jumpCapacityMultiplier < 0) {
            throw new IllegalArgumentException("jumpCapacityMultiplier debe ser >= 0");
        }

        this.baseJumpHeight = baseJumpHeight;
        this.strengthMultiplier = strengthMultiplier;
        this.forceOutputMultiplier = forceOutputMultiplier;
        this.jumpCapacityMultiplier = jumpCapacityMultiplier;
    }

    // ── Consultas Efectivas ───────────────────────────────────────────────

    /**
     * Calcula la altura de salto efectiva considerando todos los modificadores.
     *
     * Fórmula:
     *   effectiveJumpHeight = baseJumpHeight × strengthMultiplier × jumpCapacityMultiplier
     *
     * @return altura de salto efectiva en unidades del mundo
     */
    public double getEffectiveJumpHeight() {
        return baseJumpHeight * strengthMultiplier * jumpCapacityMultiplier;
    }

    /**
     * Calcula el multiplicador de fuerza efectivo para mecánicas generales.
     *
     * Fórmula:
     *   effectiveForceMultiplier = strengthMultiplier × forceOutputMultiplier
     *
     * Úsalo para escalar impulsos de knockback, recoil, dash, etc.
     *
     * @return multiplicador de fuerza efectivo
     */
    public double getEffectiveForceMultiplier() {
        return strengthMultiplier * forceOutputMultiplier;
    }

    // ── Modificación de Capacidades ───────────────────────────────────────

    /**
     * Establece la altura base de salto.
     *
     * @param height altura en unidades del mundo (debe ser >= 0)
     */
    public void setBaseJumpHeight(double height) {
        if (height < 0) {
            throw new IllegalArgumentException("baseJumpHeight debe ser >= 0");
        }
        this.baseJumpHeight = height;
    }

    /**
     * Establece el multiplicador de fuerza muscular.
     *
     * @param multiplier multiplicador (debe ser >= 0)
     */
    public void setStrengthMultiplier(double multiplier) {
        if (multiplier < 0) {
            throw new IllegalArgumentException("strengthMultiplier debe ser >= 0");
        }
        this.strengthMultiplier = multiplier;
    }

    /**
     * Establece el multiplicador de salida de fuerza.
     *
     * @param multiplier multiplicador (debe ser >= 0)
     */
    public void setForceOutputMultiplier(double multiplier) {
        if (multiplier < 0) {
            throw new IllegalArgumentException("forceOutputMultiplier debe ser >= 0");
        }
        this.forceOutputMultiplier = multiplier;
    }

    /**
     * Establece el multiplicador de capacidad de salto.
     *
     * @param multiplier multiplicador (debe ser >= 0)
     */
    public void setJumpCapacityMultiplier(double multiplier) {
        if (multiplier < 0) {
            throw new IllegalArgumentException("jumpCapacityMultiplier debe ser >= 0");
        }
        this.jumpCapacityMultiplier = multiplier;
    }

    // ── Acceso Directo a Valores Base ─────────────────────────────────────

    /** @return altura base de salto (sin modificadores) */
    public double getBaseJumpHeight() {
        return baseJumpHeight;
    }

    /** @return multiplicador de fuerza muscular */
    public double getStrengthMultiplier() {
        return strengthMultiplier;
    }

    /** @return multiplicador de salida de fuerza */
    public double getForceOutputMultiplier() {
        return forceOutputMultiplier;
    }

    /** @return multiplicador de capacidad de salto */
    public double getJumpCapacityMultiplier() {
        return jumpCapacityMultiplier;
    }

    // ── Debug ─────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return String.format(
            "PhysicalCapabilities{baseJump=%.2f, strength=%.2f, forceOut=%.2f, jumpCap=%.2f, effectiveJump=%.2f, effectiveForce=%.2f}",
            baseJumpHeight, strengthMultiplier, forceOutputMultiplier, jumpCapacityMultiplier,
            getEffectiveJumpHeight(), getEffectiveForceMultiplier()
        );
    }
}
