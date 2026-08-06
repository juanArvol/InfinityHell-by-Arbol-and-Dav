package Game.Items.Types.Bullets.BulletComport.BulletClass;

import Game.Engine.AbstractEntity;
import Game.Engine.GameObjects;
import Game.Items.Types.Bullets.Bullet;
import Game.Items.Types.Bullets.BulletComport.BulletBehavior;
import Game.Items.Types.Bullets.Movement.GravityMovement;
import Game.Items.Types.Bullets.ProjectileData;
import Game.Items.Types.Bullets.ProjectileMovement;

/**
 * Proyectil rebotante — rebota en el suelo y destruye entidades al impactar.
 *
 * Comportamiento:
 *   - Al impactar con una AbstractEntity → destruye el proyectil.
 *   - Al impactar con un objeto del mundo que está debajo → rebota hacia arriba.
 *   - Con cada rebote pierde un poco de velocidad horizontal.
 *
 * El movimiento de gravedad se declara en getDefaultMovement(), no en el
 * onUpdate(). Esto separa claramente el "cómo se mueve" del "qué hace al impactar".
 */
public class BulletJump extends BulletBehavior {

    // gravityValue=1.0 activa la gravedad (> 0 implica gravedad activa)
    private static final ProjectileData DEFAULT_DATA =
            new ProjectileData(5, 0.9, 20, 1.0);

    private static final ProjectileMovement GRAVITY =
            new GravityMovement(1.0);

    private static final double JUMP_BOOST    = -10.0;
    private static final double FRICTION      = 1.01; // divisor de vx en cada rebote

    @Override
    public ProjectileData getDefaultData() {
        return DEFAULT_DATA;
    }

    @Override
    public ProjectileMovement getDefaultMovement() {
        return GRAVITY;
    }

    @Override
    public void onCollision(Bullet bullet, GameObjects other) {
        if (other instanceof AbstractEntity) {
            // Impacto con entidad viva — el proyectil muere
            bullet.getBulletLife().kill();
        } else {
            // Impacto con objeto del mundo — rebotar si baja
            if (bullet.getPhysics().getYspeed() > 0) {
                bullet.getPhysics().setYspeed(JUMP_BOOST);
                bullet.getBulletLife().extend(1);
            }
            // Reducir ligeramente la velocidad horizontal en cada rebote
            bullet.getPhysics().setXspeed(bullet.getPhysics().getXspeed() / FRICTION);
        }
    }
}
