package Game.Engine.Events;

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
 *   enter = actuales - anteriores  → onCollisionEnter / onTriggerEnter
 *   stay  = actuales ∩ anteriores  → onCollisionStay  / onTriggerStay
 *   exit  = anteriores - actuales  → onCollisionExit  / onTriggerExit
 *
 * ── Solid vs Trigger ──────────────────────────────────────────────────────
 *
 * Si el ColliderComponent del objeto es SOLID:
 *   → se llaman onCollisionEnter / onCollisionStay / onCollisionExit
 *
 * Si el ColliderComponent del objeto es TRIGGER:
 *   → se llaman onTriggerEnter / onTriggerStay / onTriggerExit
 *
 * ── Uso ───────────────────────────────────────────────────────────────────
 *
 *   // Zona de daño continuo (trigger):
 *   public class LavaDamageZone extends Component
 *           implements CollisionListenerEvent {
 *
 *       {@literal @}Override
 *       public void onTriggerEnter(GameObjects other) {
 *           if (other instanceof AbstractEntity e) e.addEffect(new BurningEffect(120));
 *       }
 *
 *       {@literal @}Override
 *       public void onTriggerExit(GameObjects other) {
 *           if (other instanceof AbstractEntity e) e.removeEffects(BurningEffect.class);
 *       }
 *   }
 *
 *   // Sensor de pared (solid):
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
public interface CollisionListenerEvent {

    default void onCollisionEnter(GameObjects other) {}
    default void onCollisionStay(GameObjects other)  {}
    default void onCollisionExit(GameObjects other)  {}

    default void onTriggerEnter(GameObjects other) {}
    default void onTriggerStay(GameObjects other)  {}
    default void onTriggerExit(GameObjects other)  {}
}
