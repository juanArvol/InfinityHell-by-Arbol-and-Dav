package Game.Items.Types.Bullets.BulletComport.BulletClass;

import Game.Engine.AbstractEntity;
import Game.Engine.GameObjects;
import Game.Items.Types.Bullets.BulletComport.BulletBehavior;
import Game.Items.Types.Bullets.BulletComport.BulletPhysics;
import Game.Items.Types.Bullets.Definition.Bullet;
import Game.Items.Types.Bullets.Definition.ProjectileData;
import Game.Items.Types.Bullets.Movement.GravityMovement;
import Game.Items.Types.Bullets.ProjectileMovement;

/**
 * Proyectil rebotante — rebota en superficies y destruye entidades al impactar.
 *
 * ── HRFC — Collision System Unificado ────────────────────────────────────
 *
 * PROBLEMA ANTERIOR:
 *   BulletJump leía physics.getOnGround(), physics.getOnCeiling() y
 *   physics.getOnWall() para determinar desde qué cara llegó el impacto.
 *   Esos flags se escriben en FASE 1 de CollisionsSystem, que solo procesa
 *   objetos SÓLIDOS (no-trigger). Los bullets son TRIGGERS, por lo que esos
 *   flags NUNCA se establecían. BulletJump siempre caía al else (fallback
 *   heurístico por velocidad), produciendo reflexiones incorrectas.
 *
 * SOLUCIÓN (HRFC principal):
 *   CollisionsSystem FASE 1B ahora procesa TRIGGERS con SweptAABB 2D completo
 *   (CCD) y escribe la normal del impacto en BulletPhysics.setLastContactNormal()
 *   ANTES de llamar a CollisionDispatcher.dispatch(). Cuando onCollision() llega
 *   aquí, la normal es exacta y está garantizada.
 *
 *   BulletJump ahora lee directamente physics.getLastContactNormalX/Y() en
 *   lugar de los flags de Physics2D que nunca se establecían para triggers.
 *
 * ── Mini-HRFC — Corrección de oscilación tangencial ──────────────────────
 *
 * PROBLEMA RESIDUAL ANTERIOR:
 *   El parche anterior usaba CONTACT_SEPARATION = 1.0px fijo para separar
 *   la bullet del collider tras el contacto. Esto producía oscilación:
 *
 *     Frame N:   bullet en borde → separar 1.0px en normal → dispatch
 *     Frame N+1: velocidad reflejada devuelve la bullet al borde (1px/frame)
 *     Frame N+2: contacto de nuevo con time ≈ 0 → separar 1.0px → loop
 *
 * SOLUCIÓN (Mini-HRFC consolidado):
 *   La separación ahora vive en CollisionsSystem y es geométricamente correcta:
 *   - Contactos CCD limpios (time > 0): CONTACT_EPSILON = 0.5px sub-pixel.
 *   - Contactos de penetración (time = 0): penetración real + PENETRATION_EPSILON.
 *
 *   CollisionsSystem también aplica un guard post-contacto en FASE 1B:
 *   después del dispatch verifica que V·N >= 0 (la velocidad apunta fuera
 *   del collider). Si no, neutraliza la componente de velocidad en el eje
 *   de la normal. Esto rompe el ciclo de re-contacto inmediato.
 *
 *   BulletJump no contiene correcciones de posición ni penetración.
 *   Solo consume la normal de contacto y aplica su política de rebote.
 *
 * ── HRFC — Kinetic Physics: Forces, Impulses & Motion Intent ─────────────
 *
 * MIGRADO A IMPULSOS:
 *   BulletJump ahora usa addForce() en lugar de setXspeed()/setYspeed().
 *   
 *   Proceso de conversión:
 *     1. Calcular velocidad objetivo (target velocity) según la normal
 *     2. Calcular cambio de velocidad: Δv = v_target - v_current
 *     3. Calcular impulso: J = m × Δv
 *     4. Aplicar via physics.addForce(Jx, Jy)
 *
 *   Esto hace que los rebotes sean consistentes con el sistema de fuerzas/impulsos
 *   y permite que la masa participe correctamente en el proceso físico.
 *
 * REFLEXIONES IDIOMÁTICAS:
 *   Las reflexiones usan Math.abs() donde corresponde para garantizar el
 *   signo correcto independientemente del estado de velocidad al llegar:
 *
 *   ny == -1 (suelo):   vy_target ← JUMP_BOOST (siempre negativo — hacia arriba)
 *   ny == +1 (techo):   vy_target ← Math.abs(vy) (positivo — hacia abajo)
 *   nx != 0  (pared):   vx_target ← Math.abs(vx) * nx / FRICTION (refleja según normal)
 *
 * FALLBACK:
 *   El fallback heurístico por velocidad permanece como último recurso para
 *   casos donde ni FASE 1B ni CollisionDetector pudieron producir una normal.
 *   No debe ejecutarse en condiciones normales del sistema.
 *
 * CONVENCIÓN DE NORMALES (igual que SweptAABB):
 *   normalY == -1 → impacto desde abajo → cara TOP del obstáculo → suelo
 *   normalY == +1 → impacto desde arriba → cara BOTTOM del obstáculo → techo
 *   normalX != 0  → impacto lateral → cara lateral → pared
 *
 * COMPORTAMIENTO:
 *   - Al impactar con una AbstractEntity → destruye el proyectil.
 *   - normalY == -1 (suelo) → rebote vertical hacia arriba (JUMP_BOOST fijo).
 *   - normalY == +1 (techo) → refleja componente vertical hacia abajo.
 *   - normalX != 0 (pared)  → refleja componente horizontal.
 *   - Con cada rebote pierde un poco de velocidad horizontal (FRICTION divisor).
 */
public class BulletJump extends BulletBehavior {

    private static final ProjectileData DEFAULT_DATA =
            ProjectileData.flat(5000, 80, 60);

    private static final ProjectileMovement GRAVITY =
            new GravityMovement(1);

    private static final double INTERACTION_RADIUS = 100.0;

    private static final double JUMP_BOOST = -14.0;
    private static final double FRICTION   = 1.01; // divisor de vx en cada rebote

    @Override
    public ProjectileData getDefaultData() {
        return DEFAULT_DATA;
    }

    @Override
    public ProjectileMovement getDefaultMovement() {
        return GRAVITY;
    }

    @Override
    public double getInteractionRadius(Bullet bullet) {
        return INTERACTION_RADIUS;
    }

    @Override
    public void onCollision(Bullet bullet, GameObjects other) {
        if (other instanceof AbstractEntity) {
            // Impacto con entidad viva — el proyectil muere
            bullet.getBulletLife().kill();
            return;
        }

        BulletPhysics physics = bullet.getPhysics();

        // Leer la normal escrita por CollisionsSystem ANTES de este dispatch.
        // Esta normal es el resultado del SweptAABB 2D de FASE 1B — exacta y fiable.
        // Si la normal es (0,0) (impacto detectado solo por FASE 2 sin swept),
        // se usa el fallback heurístico por velocidad como última defensa.
        int nx = physics.getLastContactNormalX();
        int ny = physics.getLastContactNormalY();

        bullet.getBulletLife().extend(1);
        if (ny == -1) {
            // Cara TOP del obstáculo → suelo → rebote hacia arriba.
            // HRFC — Motion Intent: Convertir a impulso.
            // Queremos velocidad final = JUMP_BOOST (negativo, hacia arriba).
            // Δv = vf - vi
            // Impulso = masa × Δv
            double currentVy = physics.getYspeed();
            double currentVx = physics.getXspeed();
            double targetVy = JUMP_BOOST;
            double targetVx = currentVx / FRICTION;
            
            double deltaVy = targetVy - currentVy;
            double deltaVx = targetVx - currentVx;
            double impulseY = physics.getMass() * deltaVy;
            double impulseX = physics.getMass() * deltaVx;
            
            physics.addForce(impulseX, impulseY);

        } else if (ny == 1) {
            // Cara BOTTOM del obstáculo → techo → refleja hacia abajo.
            // HRFC — Motion Intent: Convertir a impulso.
            // Queremos velocidad final = Math.abs(vy) (positivo, hacia abajo).
            double currentVy = physics.getYspeed();
            double currentVx = physics.getXspeed();
            double targetVy = Math.abs(currentVy);
            double targetVx = currentVx / FRICTION;
            
            double deltaVy = targetVy - currentVy;
            double deltaVx = targetVx - currentVx;
            double impulseY = physics.getMass() * deltaVy;
            double impulseX = physics.getMass() * deltaVx;
            
            physics.addForce(impulseX, impulseY);

        } else if (nx != 0) {
            // Cara lateral del obstáculo → pared → refleja componente horizontal.
            // HRFC — Motion Intent: Convertir a impulso.
            //
            // Convención de normales (SweptAABB / BulletPhysics):
            //   nx = -1 → bullet venía desde la IZQUIERDA → golpea cara izquierda
            //             → para alejarse: vx debe ser NEGATIVO (vuelve a la izquierda)
            //   nx = +1 → bullet venía desde la DERECHA → golpea cara derecha
            //             → para alejarse: vx debe ser POSITIVO (vuelve a la derecha)
            //
            // La fórmula correcta es: vx_target = |vx| * nx / FRICTION
            //   nx=-1 → vx = -|vx| / FRICTION → negativo ✓
            //   nx=+1 → vx = +|vx| / FRICTION → positivo ✓
            double currentVx = physics.getXspeed();
            double targetVx = Math.abs(currentVx) * nx / FRICTION;
            
            double deltaVx = targetVx - currentVx;
            double impulseX = physics.getMass() * deltaVx;
            
            physics.addForce(impulseX, 0);

        } else {
            // ── Fallback: sin normal disponible ──────────────────────────────
            //
            // Rutas que llegan aquí:
            //   1. FASE 2 (overlap estático): CollisionDetector.computeOverlapNormal()
            //      retornó (0,0). Con el fix de CollisionDetector, esto solo puede
            //      ocurrir si los centros son exactamente superpuestos (caso degenerado
            //      extremo), o si propagateNormalToTrigger() decidió no escribir la
            //      normal (hasContactNormal() ya era true de FASE 1B).
            //   2. Behaviors que llaman onCollision() directamente sin pasar por
            //      CollisionsSystem (raro, pero posible en tests o escenarios custom).
            //
            // El fallback usa la velocidad como heurística espacial:
            //   - Es menos preciso que la normal geométrica.
            //   - Es preferible a no hacer nada (que dejaría la bullet stuck).
            //   - NO debe ser la ruta normal: CollisionDetector ya garantiza
            //     normal != (0,0) para overlaps reales después del fix.
            //
            // ── Caso degenerado: velocidad también es (0,0) ──────────────────
            //   Si la bullet llegó aquí sin velocidad y sin normal, aplicar un
            //   impulso arbitrario para sacarla del estado stuck. Usar JUMP_BOOST
            //   en Y como último recurso documentado.
            //
            // HRFC — Motion Intent: Convertir fallback a impulsos.
            double vy = physics.getYspeed();
            double vx = physics.getXspeed();
            
            if (vy > 0) {
                // Venía bajando → tratar como suelo
                double targetVy = JUMP_BOOST;
                double targetVx = vx / FRICTION;
                double deltaVy = targetVy - vy;
                double deltaVx = targetVx - vx;
                physics.addForce(physics.getMass() * deltaVx, physics.getMass() * deltaVy);
                bullet.getBulletLife().extend(1);
                
            } else if (vy < 0) {
                // Venía subiendo → tratar como techo
                double targetVy = Math.abs(vy);
                double targetVx = vx / FRICTION;
                double deltaVy = targetVy - vy;
                double deltaVx = targetVx - vx;
                physics.addForce(physics.getMass() * deltaVx, physics.getMass() * deltaVy);
                
            } else if (vx != 0) {
                // Solo movimiento horizontal → tratar como pared
                double targetVx = -vx / FRICTION;
                double deltaVx = targetVx - vx;
                physics.addForce(physics.getMass() * deltaVx, 0);
                
            } else {
                // Velocidad completamente nula y sin normal — estado fully stuck.
                // Aplicar JUMP_BOOST para sacar la bullet del estado inválido.
                // Documentado como last resort: no debe ocurrir en condiciones normales
                // si CollisionDetector y CollisionsSystem funcionan correctamente.
                double targetVy = JUMP_BOOST;
                double deltaVy = targetVy - vy;
                physics.addForce(0, physics.getMass() * deltaVy);
            }
        }
    }
}
