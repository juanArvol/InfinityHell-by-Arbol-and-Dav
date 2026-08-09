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

    // La gravedad es intrínseca a este behavior y se declara en getDefaultMovement().
    // gravityValue = 0.0 en ProjectileData — la fuente de verdad es GRAVITY abajo,
    // no un campo de datos. ProjectileBlueprint.from() no añadirá doble gravedad.
    private static final ProjectileData DEFAULT_DATA =
            ProjectileData.flat(5, 0.9, 60);

    private static final ProjectileMovement GRAVITY =
            new GravityMovement(1);

    private static final double INTERACTION_RADIUS = 100.0;

    private static final double JUMP_BOOST    = -14.0;
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

        // Impacto con el mundo — usar los flags de contacto ya escritos por
        // CollisionsSystem en Physics2D antes de que llegue el dispatch.
        BulletPhysics physics = bullet.getPhysics();

        if (physics.getOnGround()) {
            // Suelo → rebote vertical hacia arriba
            physics.setYspeed(JUMP_BOOST);
            physics.setXspeed(physics.getXspeed() / FRICTION);
            bullet.getBulletLife().extend(1);

        } else if (physics.getOnCeiling()) {
            // Techo → reflejar componente vertical (invertir)
            physics.setYspeed(-physics.getYspeed());
            physics.setXspeed(physics.getXspeed() / FRICTION);

        } else if (physics.getOnWall()) {
            // Pared → reflejar componente horizontal (invertir)
            physics.setXspeed(-physics.getXspeed() / FRICTION);

        } else {
            // Fallback: sin flag de contacto disponible (p.ej. contacto diagonal
            // o colisión resuelta por trigger), usar la velocidad como heurística.
            if (physics.getYspeed() > 0) {
                // Venía bajando → tratar como suelo
                physics.setYspeed(JUMP_BOOST);
                physics.setXspeed(physics.getXspeed() / FRICTION);
                bullet.getBulletLife().extend(1);
            } else if (physics.getYspeed() < 0) {
                // Venía subiendo → tratar como techo
                physics.setYspeed(-physics.getYspeed());
                physics.setXspeed(physics.getXspeed() / FRICTION);
            } else {
                // Solo movimiento horizontal → tratar como pared
                physics.setXspeed(-physics.getXspeed() / FRICTION);
            }
        }
    }
}
