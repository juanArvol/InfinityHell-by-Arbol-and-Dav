package Game.Engine.Physics.KineticPhysics.Intent;

import Game.Engine.Physics.KineticPhysics.Types.Physics2D;

/**
 * Intención de movimiento — representa una mecánica de gameplay que solicita
 * determinado movimiento físico.
 *
 * ── HRFC — Kinetic Physics: Forces, Impulses & Motion Intent ─────────────
 *
 * MotionIntent NO ES UNA FUERZA ni UN IMPULSO.
 *
 * Es una INTENCIÓN DE GAMEPLAY que expresa:
 *   "El jugador quiere saltar"
 *   "El jugador quiere hacer dash"
 *   "El jugador quiere lanzarse"
 *
 * SIN especificar:
 *   "Pon su velocidad Y en -15"
 *
 * ── SEPARACIÓN DE RESPONSABILIDADES ──────────────────────────────────────
 *
 * Gameplay:
 *   - Detecta input del jugador
 *   - Crea MotionIntent
 *   - Invoca resolve() con el contexto físico
 *
 * MotionIntent Resolver:
 *   - Convierte la intención en operación física
 *   - Calcula velocidad/impulso necesarios
 *   - Aplica via Physics2D.addForce()
 *
 * Physics2D:
 *   - Ejecuta el estado cinético
 *   - Integra fuerzas/impulsos
 *   - Mantiene velocidad/masa
 *
 * ── VENTAJAS ──────────────────────────────────────────────────────────────
 *
 * 1. Consistencia física: masa, gravedad, modificadores participan correctamente
 * 2. Extensibilidad: nuevos modificadores no requieren cambiar el resolver
 * 3. Testability: intenciones son objetos separados, fáciles de testear
 * 4. Separation of concerns: gameplay no necesita saber física interna
 *
 * ── EJEMPLO: JUMP ─────────────────────────────────────────────────────────
 *
 * Antes (directo):
 *   physics.setYspeed(-15);
 *
 * Después (intent):
 *   JumpIntent intent = new JumpIntent();
 *   intent.resolve(physics, capabilities);
 *
 * El resolver internamente:
 *   1. Consulta effectiveJumpHeight de capabilities
 *   2. Calcula v₀ = sqrt(2 × g × h)
 *   3. Calcula impulso J = m × v₀
 *   4. Aplica physics.addForce(0, -J)
 *
 * ── CLASIFICACIÓN ─────────────────────────────────────────────────────────
 *
 * Motion Intent:
 *   - Jump: altura objetivo → velocidad inicial calculada
 *   - Dash: distancia/velocidad objetivo → impulso calculado
 *   - Launch: objetivo específico de movimiento
 *
 * Impulse (NO Motion Intent):
 *   - Knockback: cambio directo de momentum
 *   - Explosion: impulso radial
 *   - Recoil: impulso en dirección opuesta
 *
 * Force (NO Motion Intent):
 *   - Gravity: aceleración continua
 *   - Wind: fuerza ambiental continua
 *   - Drag: resistencia aerodinámica
 */
public interface MotionIntent {

    /**
     * Resuelve la intención de movimiento aplicando el impulso necesario.
     *
     * @param physics contexto físico que ejecutará el movimiento
     */
    void resolve(Physics2D physics);
}
