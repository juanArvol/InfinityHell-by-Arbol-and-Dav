package Game.Engine.Events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Bus de eventos — instanciable con Subscription handles para lifecycle explícito.
 *
 * ── ARQUITECTURA ──────────────────────────────────────────────────────────
 *
 * GameEventBus es infraestructura pura del Engine:
 *   - Instanciable explícitamente.
 *   - Sin instancia global estática.
 *   - Sin métodos estáticos de conveniencia.
 *
 * La capa de composición (Bootstrap, GameState) crea la instancia del bus
 * y la inyecta en los sistemas que la necesitan.
 *
 * ── SUBSCRIPTION HANDLE ───────────────────────────────────────────────────
 *
 * subscribe() retorna una {@link Subscription}. El caller conserva esa
 * referencia y llama subscription.cancel() para desregistrar limpiamente.
 *
 * La Subscription es idempotente — cancel() puede llamarse múltiples veces
 * sin efecto adverso.
 *
 * ── LIFECYCLE POR SCOPE ───────────────────────────────────────────────────
 *
 *   Listeners con lifecycle de World/Scene:
 *     Deben guardarse como Subscription y cancelarse cuando el World muere.
 *     Ejemplo: ProjectileRegistry instala un listener cuando el World arranca
 *     y lo cancela (via uninstallListener()) cuando el World se destruye.
 *
 *   Listeners temporales (por duración de un behavior):
 *     El behavior guarda la Subscription en onAttached() y la cancela en
 *     onRelease() o onDetached(). Nunca en onExpire() — eso es demasiado tarde.
 *
 * ── THREAD SAFETY ─────────────────────────────────────────────────────────
 *
 * subscribe()/unsubscribe() son seguros para llamadas concurrentes.
 * post() está orientado al game loop single-thread.
 * No se introduce asincronía — el bus es síncrono y directo:
 *
 *   evento → listeners registrados → notificación
 */
public final class GameEventBus {

    // ── Subscription handle ───────────────────────────────────────────────

    /**
     * Handle que representa una suscripción activa en un GameEventBus.
     *
     * Retornado por {@link GameEventBus#subscribe}. El caller conserva este
     * handle para cancelar la suscripción cuando el listener ya no es necesario.
     *
     * Idempotente: llamar cancel() múltiples veces es seguro.
     *
     * Uso:
     * <pre>
     *   // Al inicializar:
     *   Subscription sub = bus.subscribe(MyEvent.class, this::onMyEvent);
     *
     *   // Al destruir:
     *   sub.cancel();
     * </pre>
     */
    public interface Subscription {

        /**
         * Cancela esta suscripción y elimina el listener del bus.
         * Idempotente — puede llamarse múltiples veces sin error.
         */
        void cancel();

        /**
         * @return true si esta suscripción sigue activa (no ha sido cancelada).
         */
        boolean isActive();

        /**
         * Suscripción vacía — cancel() es no-op, isActive() siempre false.
         * Útil como placeholder cuando no se necesita cancelación real.
         */
        Subscription NOOP = new Subscription() {
            @Override public void cancel()      {}
            @Override public boolean isActive() { return false; }
        };
    }

    // ── Estado de instancia ───────────────────────────────────────────────

    private final Map<Class<?>, List<Consumer<Object>>> listeners = new HashMap<>();

    public GameEventBus() {}

    // ── API de instancia ──────────────────────────────────────────────────

    /**
     * Suscribe un listener a un tipo de evento.
     *
     * @param <T>       tipo del evento
     * @param eventType clase del evento a escuchar
     * @param listener  callback invocado al recibir el evento
     * @return Subscription handle para cancelar la suscripción cuando ya no sea necesaria
     */
    @SuppressWarnings("unchecked")
    public <T> Subscription subscribe(Class<T> eventType, Consumer<T> listener) {
        Consumer<Object> wrapped = (Consumer<Object>) listener;
        listeners.computeIfAbsent(eventType, k -> new ArrayList<>()).add(wrapped);
        return new SubscriptionImpl(eventType, wrapped);
    }

    /**
     * Desregistra un listener por referencia.
     * Preferir el uso de {@link Subscription#cancel()} cuando sea posible.
     *
     * @param <T>       tipo del evento
     * @param eventType clase del evento
     * @param listener  la referencia exacta del Consumer registrado
     */
    public <T> void unsubscribe(Class<T> eventType, Consumer<T> listener) {
        var list = listeners.get(eventType);
        if (list != null) list.remove(listener);
    }

    /**
     * Publica un evento a todos los listeners registrados para su tipo.
     * Orientado al game loop single-thread.
     *
     * @param event el evento a publicar
     */
    public void post(Object event) {
        var list = listeners.get(event.getClass());
        if (list == null) return;
        for (var listener : List.copyOf(list)) {
            listener.accept(event);
        }
    }

    /**
     * Limpia todos los listeners de esta instancia.
     *
     * Legítimo para limpiar una instancia de bus que pertenece a un World
     * cuando ese World muere. No recomendado para buses de aplicación
     * si hay listeners con lifecycle más largo.
     */
    public void clear() {
        listeners.clear();
    }

    /**
     * Comprueba si hay listeners registrados para un tipo de evento.
     * Útil para evitar construir objetos de evento cuando no hay suscriptores.
     *
     * @param eventType tipo del evento a comprobar
     * @return true si hay al menos un listener registrado
     */
    public boolean hasListeners(Class<?> eventType) {
        var list = listeners.get(eventType);
        return list != null && !list.isEmpty();
    }

    // ── Implementación interna de Subscription ────────────────────────────

    /**
     * Implementación concreta de Subscription.
     * Mantiene referencia al bus y al wrapped listener para cancelación exacta.
     * package-private — los callers solo ven la interfaz Subscription.
     */
    private final class SubscriptionImpl implements Subscription {

        private final Class<?> eventType;
        private final Consumer<Object> listener;
        private boolean cancelled = false;

        SubscriptionImpl(Class<?> eventType, Consumer<Object> listener) {
            this.eventType = eventType;
            this.listener  = listener;
        }

        @Override
        public void cancel() {
            if (cancelled) return; // idempotente
            cancelled = true;
            var list = listeners.get(eventType);
            if (list != null) list.remove(listener);
        }

        @Override
        public boolean isActive() {
            return !cancelled;
        }
    }
}
