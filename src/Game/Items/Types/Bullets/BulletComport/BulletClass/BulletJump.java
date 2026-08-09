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
 * SOLUCIÓN:
 *   CollisionsSystem FASE 1B ahora procesa TRIGGERS con SweptAABB 2D completo
 *   (CCD) y escribe la normal del impacto en BulletPhysics.setLastContactNormal()
 *   ANTES de llamar a CollisionDispatcher.dispatch(). Cuando onCollision() llega
 *   aquí, la normal es exacta y está garantizada.
 *
 *   BulletJump ahora lee directamente physics.getLastContactNormalX/Y() en
 *   lugar de los flags de Physics2D que nunca se establecían para triggers.
 *
 * CONVENCIÓN DE NORMALES (igual que SweptAABB):
 *   normalY == -1 → impacto desde abajo → cara TOP del obstáculo → suelo
 *   normalY == +1 → impacto desde arriba → cara BOTTOM del obstáculo → techo
 *   normalX != 0  → impacto lateral → cara lateral → pared
 *
 * COMPORTAMIENTO:
 *   - Al impactar con una AbstractEntity → destruye el proyectil.
 *   - normalY == -1 (suelo) → rebote vertical hacia arriba.
 *   - normalY == +1 (techo) → refleja componente vertical.
 *   - normalX != 0 (pared)  → refleja componente horizontal.
 *   - Con cada rebote pierde un poco de velocidad horizontal (FRICTION).
 */
public class BulletJump extends BulletBehavior {

    private static final ProjectileData DEFAULT_DATA =
            ProjectileData.flat(5, 0.9, 60);

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

        if (ny == -1) {
            // Cara TOP del obstáculo → suelo → rebote hacia arriba
            physics.setYspeed(JUMP_BOOST);
            physics.setXspeed(physics.getXspeed() / FRICTION);
            bullet.getBulletLife().extend(1);

        } else if (ny == 1) {
            // Cara BOTTOM del obstáculo → techo → refleja componente vertical
            physics.setYspeed(-physics.getYspeed());
            physics.setXspeed(physics.getXspeed() / FRICTION);

        } else if (nx != 0) {
            // Cara lateral del obstáculo → pared → refleja componente horizontal
            physics.setXspeed(-physics.getXspeed() / FRICTION);

        } else {
            // Fallback: sin normal disponible (FASE 2 sin swept, overlap estático).
            // Usar la velocidad como heurística — solo en casos sin CCD activo.
            double vy = physics.getYspeed();
            double vx = physics.getXspeed();
            if (vy > 0) {
                // Venía bajando → tratar como suelo
                physics.setYspeed(JUMP_BOOST);
                physics.setXspeed(vx / FRICTION);
                bullet.getBulletLife().extend(1);
            } else if (vy < 0) {
                // Venía subiendo → tratar como techo
                physics.setYspeed(-vy);
                physics.setXspeed(vx / FRICTION);
            } else {
                // Solo movimiento horizontal → tratar como pared
                physics.setXspeed(-vx / FRICTION);
            }
        }
    }
}
