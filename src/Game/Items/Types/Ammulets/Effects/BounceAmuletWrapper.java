package Game.Items.Types.Ammulets.Effects;

import Game.Enemys.Enemy;
import Game.Items.Types.Bullets.Bullet;
import Game.Items.Types.Bullets.BulletComport.BulletBehavior;
import Game.Items.Types.Weapons.Modifiers.BulletBehaviorWrapper;

/**
 * Wrapper de rebote para amuletos — "Piedra del Eco".
 *
 * Al impactar un enemigo, busca el enemigo más cercano dentro de un radio
 * y redirige la bala hacia él. Cada copia del amuleto añade +1 salto.
 *
 * ── IMPLEMENTACIÓN ────────────────────────────────────────────────────────
 * La lógica de búsqueda del enemigo más cercano requiere acceso al World
 * (lista de objetos activos). Hay dos opciones:
 *
 *   A) Pasar la referencia al World en el constructor (recomendado).
 *   B) Usar el GameEventBus para disparar un evento "BulletBounceRequest"
 *      que el World escucha y resuelve.
 *
 * Por ahora es un stub que no hace nada. Implementar cuando el sistema
 * de world-query esté disponible.
 *
 * TODO: implementar onHitEnemy con búsqueda del enemigo más cercano.
 */
public class BounceAmuletWrapper extends BulletBehaviorWrapper {

    private final int maxBounces;
    private int bounceCount = 0;

    public BounceAmuletWrapper(BulletBehavior inner, int maxBounces) {
        super(inner);
        this.maxBounces = maxBounces;
    }

    @Override
    protected void onHitEnemy(Bullet bullet, Enemy enemy) {
        if (bounceCount >= maxBounces) return;
        bounceCount++;

        // TODO: buscar enemigo más cercano distinto al actual y redirigir la bala.
        // Ejemplo aproximado:
        //   Enemy target = world.findNearestEnemy(bullet.getPosition(), enemy, BOUNCE_RADIUS);
        //   if (target != null) {
        //       Vector2D dir = target.getPosition().subtract(bullet.getPosition()).normalize();
        //       bullet.setVelocity(dir.scale(bullet.getSpeed()));
        //       bullet.getBulletLife().revive();
        //   }
    }
}
