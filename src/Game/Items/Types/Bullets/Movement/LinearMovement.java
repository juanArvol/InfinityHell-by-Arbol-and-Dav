package Game.Items.Types.Bullets.Movement;

import Game.Items.Types.Bullets.Definition.Bullet;
import Game.Items.Types.Bullets.ProjectileMovement;

/**
 * Movimiento lineal — velocidad constante.
 *
 * El comportamiento por defecto de cualquier proyectil recién creado.
 * La velocidad inicial se fija al construir el Bullet y LinearMovement
 * no la modifica. Equivale a "no hacer nada" pero es explícito en el contrato.
 *
 * Uso:
 *   ProjectileMovement m = new LinearMovement();
 *   // o directamente: ProjectileMovement m = bullet -> {};
 *   // LinearMovement es preferible por legibilidad en el factory.
 */
public final class LinearMovement implements ProjectileMovement {

    /** Singleton — no tiene estado, es reutilizable. */
    public static final LinearMovement INSTANCE = new LinearMovement();

    @Override
    public void tick(Bullet bullet, double deltaTime) {
        // Velocidad constante — no se modifica nada.
        // deltaTime no se usa porque no hay aceleración ni cambio de velocidad.
    }

    /**
     * LinearMovement no tiene estado interno.
     * Su singleton es seguro para compartir entre cualquier número de proyectiles.
     */
    @Override
    public boolean isStateless() {
        return true;
    }
}
