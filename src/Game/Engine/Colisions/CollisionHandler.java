package Game.Engine.Colisions;

import Game.Engine.GameObjects;

/**
 * Interfaz para componentes que necesitan saber cuándo ENTRA, CONTINÚA
 * o SALE una colisión — no solo el impacto puntual.
 *
 * ── HRFC-014 — GAP-1: ACTIVA ─────────────────────────────────────────────
 *
 * CollisionsSystem implementa FASE 4 que invoca estos callbacks. Implementar
 * esta interfaz en un Component produce efectos observables cada frame.
 *
 * ── Mecanismo ─────────────────────────────────────────────────────────────
 *
 * CollisionsSystem mantiene un mapa de contactos del frame anterior.
 * En cada frame, después del despacho de colisiones (FASE 3), compara:
 *
 *   contactosActuales = detectados en FASE 2
 *   enter = actuales - anteriores  → onCollisionEnter
 *   stay  = actuales ∩ anteriores  → onCollisionStay
 *   exit  = anteriores - actuales  → onCollisionExit
 *
 * ── Uso ───────────────────────────────────────────────────────────────────
 *
 *   // Zona de daño continuo (sensor):
 *   public class LavaDamageZone extends Component
 *           implements CollisionListenerEvent {
 *
 *       {@literal @}Override
 *       public void onCollisionEnter(GameObjects other) {
 *           if (other instanceof AbstractEntity e) e.addEffect(new BurningEffect(120));
 *       }
 *
 *       {@literal @}Override
 *       public void onCollisionExit(GameObjects other) {
 *           if (other instanceof AbstractEntity e) e.removeEffects(BurningEffect.class);
 *       }
 *   }
 *
 *   // Sensor de pared:
 *   public class WallSensor extends Component
 *           implements CollisionListenerEvent {
 *
 *       {@literal @}Override
 *       public void onCollisionEnter(GameObjects other) {
 *           if (other instanceof BlockWorld) onWallContact();
 *       }
 *   }
 *
 * ── Limpieza en transiciones de mundo ────────────────────────────────────
 *
 * Al cambiar de mundo, WorldManager llama clearCollisionContactHistory()
 * en WorldObjectsContainer, que limpia el historial interno de CollisionsSystem.
 * Esto previene eventos "exit" espurios para contactos del mundo anterior.
 */
public interface CollisionHandler {

    default void onCollisionEnter(GameObjects other) {}
    default void onCollisionStay(GameObjects other)  {}
    default void onCollisionExit(GameObjects other)  {}
}
