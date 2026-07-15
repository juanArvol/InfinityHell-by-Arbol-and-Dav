package Game.Gameplay.Core.Events;

/**
 * Contrato base de un evento de gameplay.
 *
 * ── QUÉ ES UN EVENTO DE GAMEPLAY ─────────────────────────────────────────
 * Un evento de gameplay representa una transición de fase o una acción
 * significativa en el ciclo de vida del juego. A diferencia de los eventos
 * del GameEventBus (puramente observacionales), los eventos de gameplay
 * pueden ser INTERCEPTADOS y MODIFICADOS antes de resolverse.
 *
 * ── DIFERENCIA CON GameEventBus ──────────────────────────────────────────
 *
 *   GameEventBus    → notificación post-hecho (ya ocurrió, se notifica)
 *                     los listeners observan pero no pueden modificar
 *
 *   GameplayEvent   → pre-resolución (va a ocurrir, puede modificarse)
 *                     los interceptores pueden alterar parámetros antes
 *                     de que el evento se consuma
 *
 * Ejemplo: un evento OnDamage con damage=100 puede pasar por:
 *   - Interceptor 1: escudo → reduce damage a 60
 *   - Interceptor 2: resistencia de fuego → reduce damage a 42
 *   - Interceptor 3: buff de vulnerabilidad → incrementa a 63
 *   → Resultado final: se aplica 63 de daño
 *
 * ── MUTABILIDAD CONTROLADA ───────────────────────────────────────────────
 * Los eventos de gameplay son intencionalmente mutables durante la fase de
 * interceptación. Una vez que GameplayEventChannel.fire() retorna, el evento
 * ya fue consumido y no debe modificarse.
 *
 * Los campos que representan el resultado final de una decisión deben ser
 * mutables (setters o campos package-private). Los campos de contexto
 * inmutable (quién dispara, cuándo, desde dónde) pueden ser final.
 *
 * ── CANCELACIÓN ───────────────────────────────────────────────────────────
 * Cualquier evento puede cancelarse durante la interceptación.
 * Si cancelled() retorna true, el productor del evento NO debe ejecutar
 * la acción que el evento representa.
 *
 * Ejemplo: OnDamage cancelado → el daño no se aplica.
 *
 * ── IMPLEMENTACIÓN ────────────────────────────────────────────────────────
 * Los eventos concretos implementan esta interfaz y añaden los campos
 * relevantes para su contexto:
 *
 *   public class OnDamageEvent implements GameplayEvent {
 *       private double damage;
 *       // ...
 *   }
 */
public interface GameplayEvent {

    /**
     * True si el evento fue cancelado por algún interceptor.
     * Si es true, el productor del evento NO debe ejecutar la acción.
     */
    boolean isCancelled();

    /**
     * Cancela el evento. Una vez cancelado, el estado no puede revertirse.
     * Llamar desde un interceptor para impedir que el evento surta efecto.
     */
    void cancel();
}
