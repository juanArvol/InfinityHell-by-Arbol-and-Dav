package Game.Engine.Events;

import Game.Engine.GameObjects;

/**
 * Interfaz para componentes que necesitan saber cuándo ENTRA, CONTINÚA
 * o SALE una colisión — no solo el impacto puntual.
 *
 * Útil para:
 *   - Zonas de daño continuo (lava, gas)
 *   - Triggers de eventos (entrar a un área activa un script)
 *   - Detectores de suelo más refinados
 *
 * Uso:
 *   public class DamageTrigger extends Component implements CollisionListener {
 *       public void onTriggerStay(GameObjects other) {
 *           if (other instanceof Player p) p.damage(1);
 *       }
 *   }
 *
 * El sistema de colisiones no usa este mecanismo directamente —
 * si lo necesitás hay que conectarlo en WorldObjectsContainer.
 * La mayoría de objetos solo necesita onCollisionWith() en GameObjects.
 */
public interface CollisionListener {

    default void onCollisionEnter(GameObjects other) {}
    default void onCollisionStay(GameObjects other)  {}
    default void onCollisionExit(GameObjects other)  {}

    default void onTriggerEnter(GameObjects other) {}
    default void onTriggerStay(GameObjects other)  {}
    default void onTriggerExit(GameObjects other)  {}
}
