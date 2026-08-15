package Game.Engine.Physics.KineticPhysics.Intent;

import Game.Engine.Physics.KineticPhysics.PhysicalCapabilities;
import Game.Engine.Physics.KineticPhysics.Types.Physics2D;

/**
 * Intención de salto — expresa el deseo de saltar a determinada altura.
 *
 * ── HRFC — Kinetic Physics: Forces, Impulses & Motion Intent ─────────────
 *
 * JumpIntent NO especifica una velocidad arbitraria.
 * Especifica una ALTURA FÍSICA objetivo y el resolver calcula el impulso
 * necesario para alcanzarla bajo las condiciones físicas actuales.
 *
 * ── FÍSICA DEL SALTO ──────────────────────────────────────────────────────
 *
 * Para un salto vertical bajo gravedad constante:
 *
 *   v₀² = 2gh
 *
 * Por tanto:
 *
 *   v₀ = sqrt(2gh)
 *
 * Donde:
 *   v₀ = velocidad inicial necesaria
 *   g  = magnitud gravitacional
 *   h  = altura objetivo
 *
 * El impulso necesario:
 *
 *   J = m × v₀
 *
 * Y el cambio de velocidad resultante:
 *
 *   Δv = J / m = v₀
 *
 * ── CONSISTENCIA FÍSICA ───────────────────────────────────────────────────
 *
 * Este modelo garantiza que:
 *
 * 1. Cambiar masa NO destruye arbitrariamente el salto
 * 2. Cambiar gravedad modifica físicamente la altura alcanzada
 * 3. Modificadores de capacidad escalan correctamente
 * 4. El salto es predecible y testeable
 *
 * Ejemplo:
 *
 *   Player: mass=40, gravity=1.1, desiredHeight=15
 *     → v₀ = sqrt(2 × 1.1 × 15) ≈ 5.74
 *     → J = 40 × 5.74 ≈ 229.6
 *     → Δv ≈ 5.74
 *
 *   Si mass=80:
 *     → v₀ = sqrt(2 × 1.1 × 15) ≈ 5.74 (igual)
 *     → J = 80 × 5.74 ≈ 459.2 (mayor impulso necesario)
 *     → Δv ≈ 5.74 (misma velocidad inicial)
 *     → Alcanza la misma altura
 *
 * ── CAPACIDADES FÍSICAS ───────────────────────────────────────────────────
 *
 * La altura objetivo se obtiene de PhysicalCapabilities:
 *
 *   effectiveJumpHeight = baseJumpHeight
 *                       × strengthMultiplier
 *                       × jumpCapacityMultiplier
 *
 * Esto permite que:
 *   - Entrenamiento incremente strengthMultiplier
 *   - Amuletos incrementen jumpCapacityMultiplier
 *   - Buffs/debuffs modifiquen ambos
 *
 * Sin que el resolver necesite conocer los nombres de buffs/amuletos.
 *
 * ── USO ───────────────────────────────────────────────────────────────────
 *
 * Desde PlayerController o similar:
 *
 *   if (KeyBoard.getState("up") && playerState.isEnElSuelo()) {
 *       JumpIntent intent = new JumpIntent(capabilities);
 *       intent.resolve(physics);
 *       playerState.setEnElSuelo(false);
 *       physics.setOnGround(false);
 *   }
 *
 * El intent resuelve internamente:
 *   1. Consulta effectiveJumpHeight
 *   2. Calcula v₀ necesaria
 *   3. Calcula impulso J
 *   4. Aplica physics.addForce(0, -J)
 *
 * ── DIRECCIÓN DEL SALTO ───────────────────────────────────────────────────
 *
 * Esta implementación base es para salto vertical puro (dirección -Y).
 *
 * Para saltos direccionales (dash-jump, wall-jump), extender esta clase
 * y sobreescribir resolve() con la lógica específica de dirección.
 */
public class JumpIntent implements MotionIntent {

    private final PhysicalCapabilities capabilities;

    /**
     * Crea una intención de salto vertical.
     *
     * @param capabilities capacidades físicas que definen la altura de salto
     */
    public JumpIntent(PhysicalCapabilities capabilities) {
        if (capabilities == null) {
            throw new IllegalArgumentException("capabilities no puede ser null");
        }
        this.capabilities = capabilities;
    }

    /**
     * Resuelve el salto aplicando el impulso necesario para alcanzar la
     * altura efectiva definida en capabilities.
     *
     * Fórmula:
     *   v₀ = sqrt(2 × g × h)
     *   J = m × v₀
     *   physics.addForce(0, -J)  // -J porque Y positivo es hacia abajo
     *
     * El impulso es negativo (hacia arriba) porque en el sistema de
     * coordenadas del juego, Y positivo apunta hacia abajo.
     *
     * @param physics contexto físico sobre el que aplicar el impulso
     */
    @Override
    public void resolve(Physics2D physics) {
        double effectiveHeight = capabilities.getEffectiveJumpHeight();
        double gravity = physics.getGravity();
        double mass = physics.getMass();

        // Calcular velocidad inicial necesaria: v₀ = sqrt(2gh)
        // gravity ya es positivo (hacia abajo), así que usamos directamente
        double v0 = Math.sqrt(2.0 * gravity * effectiveHeight);

        // Calcular impulso necesario: J = m × v₀
        double impulse = mass * v0;

        // Aplicar impulso hacia arriba (negativo en Y)
        // addForce() internamente hace: velocity.y += (-impulse / mass) = -v₀
        physics.addForce(0, -impulse);

        // Marcar estado de salto en Physics2D (para lógica de gameplay)
        physics.setJumping(true);
    }

    /**
     * @return capacidades físicas asociadas a este intent
     */
    public PhysicalCapabilities getCapabilities() {
        return capabilities;
    }

    /**
     * Calcula la velocidad inicial requerida para alcanzar la altura objetivo
     * sin aplicar el impulso. Útil para debug y preview.
     *
     * @param physics contexto físico (para consultar gravedad)
     * @return velocidad inicial necesaria (positiva, magnitud)
     */
    public double calculateRequiredVelocity(Physics2D physics) {
        double effectiveHeight = capabilities.getEffectiveJumpHeight();
        double gravity = physics.getGravity();
        return Math.sqrt(2.0 * gravity * effectiveHeight);
    }

    /**
     * Calcula el impulso requerido para alcanzar la altura objetivo
     * sin aplicar el impulso. Útil para debug y preview.
     *
     * @param physics contexto físico (para consultar gravedad y masa)
     * @return impulso necesario (positiva, magnitud)
     */
    public double calculateRequiredImpulse(Physics2D physics) {
        double v0 = calculateRequiredVelocity(physics);
        return physics.getMass() * v0;
    }

    @Override
    public String toString() {
        return String.format("JumpIntent{effectiveHeight=%.2f}",
                capabilities.getEffectiveJumpHeight());
    }
}
