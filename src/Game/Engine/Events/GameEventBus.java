package Game.Engine.Events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Bus de eventos — instanciable con Subscription handles para lifecycle explícito.
 *
 * ── PROBLEMA ANTERIOR ────────────────────────────────────────────────────
 *   subscribe() registraba un listener sin retornar ninguna referencia.
 *   El caller no podía desregistrarlo salvo guardando manualmente el Consumer
 *   y llamando unsubscribe(type, consumer). Esto era propenso a:
 *   - Acumulación de listeners duplicados si subscribe() se llamaba varias veces.
 *   - Imposibilidad de limpiar listeners capturados en lambdas anónimas.
 *   - Fuga de referencias a objetos del mundo destruido (World, WorldManager,
 *     ProjectileRegistry) porque sus listeners permanecían en GLOBAL.
 *
 * ── SOLUCIÓN: Subscription handle ────────────────────────────────────────
 *   subscribe() ahora retorna una {@link Subscription}. El caller conserva esa
 *   referencia y llama subscription.cancel() para desregistrar limpiamente.
 *
 *   La Subscription es idempotente — cancel() puede llamarse múltiples veces
 *   sin efecto adverso. Después del primer cancel() se convierte en no-op.
 *
 * ── LIFECYCLE POR SCOPE ───────────────────────────────────────────────────
 *
 *   Listeners globales de aplicación (UISystem, AudioSystem, analytics):
 *     Viven durante toda la aplicación. No necesitan Subscription activa
 *     si el listener y el suscriptor tienen el mismo lifetime.
 *
 *   Listeners asociados a un World/Scene:
 *     Deben guardarse como Subscription y cancelarse cuando el World muere.
 *     Ejemplo: ProjectileRegistry instala un listener cuando el World arranca
 *     y lo cancela (via shutdown()) cuando el World se destruye.
 *
 *   Listeners temporales (por duración de un projectile o behavior):
 *     El behavior guarda la Subscription en onAttached() y la cancela en
 *     onRelease() o onDetached(). Nunca en onExpire() — eso es demasiado tarde.
 *
 * ── THREAD SAFETY ─────────────────────────────────────────────────────────
 *   subscribe()/unsubscribe() son seguros para llamar desde cualquier thread.
 *   post() está diseñado para single-thread (game loop). Si en el futuro se
 *   necesita post() multi-thread, añadir sincronización en los listeners.
 *
 * ── COMPATIBILIDAD ────────────────────────────────────────────────────────
 *   subscribe() ahora retorna Subscription. El código existente que no usa
 *   el valor de retorno sigue compilando sin cambios. Solo cambia la firma
 *   de retorno: void → Subscription.
 *
 *   unsubscribe(type, listener) se mantiene para desregistro puntual cuando
 *   el listener es una referencia de método conservada explícitamente.
 */
public final class GameEventBus {

    /**
     * Instancia global para compatibilidad con código existente.
     * Usar esta instancia es correcto para un juego single-player single-scene.
     * Para escenas aisladas o multiplayer, crear instancias separadas.
     *
     * NOTA DE LIFECYCLE:
     *   Los listeners registrados en GLOBAL que capturan objetos con lifetime
     *   de World (WorldManager, ProjectileRegistry, LootSystem) DEBEN cancelar
     *   su Subscription cuando ese objeto se destruye. Si no lo hacen,
     *   GLOBAL retiene una referencia fuerte al objeto destruido impidiendo GC.
     *
     *   Listeners globales legítimos (UI permanente, analytics de aplicación)
     *   pueden vivir indefinidamente en GLOBAL sin problema.
     */
    public static final GameEventBus GLOBAL = new GameEventBus();

    // ── Subscription handle ───────────────────────────────────────────────────

    /**
     * Handle que representa una suscripción activa en un GameEventBus.
     *
     * Retornado por {@link GameEventBus#subscribe}. El caller conserva este
     * handle para cancelar la suscripción cuando el listener ya no es necesario.
     *
     * Idempotente: llamar cancel() múltiples veces es seguro — solo la primera
     * llamada tiene efecto.
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
         * Útil como placeholder cuando no se necesita cancelación.
         */
        Subscription NOOP = new Subscription() {
            @Override public void cancel()           {}
            @Override public boolean isActive()      { return false; }
        };
    }

    // ── Estado de instancia ───────────────────────────────────────────────────

    private final Map<Class<?>, List<Consumer<Object>>> listeners = new HashMap<>();

    public GameEventBus() {}

    // ── API de instancia ──────────────────────────────────────────────────────

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

        // Subscription concreta que tiene referencia directa al listener y al bus.
        // Es la única forma de cancelar correctamente una lambda anónima.
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
     * Usar con cuidado — cancela TODOS los listeners incluyendo los globales.
     * Para limpieza selectiva, cancelar las Subscriptions individuales de
     * los listeners con lifecycle de World o Scene.
     *
     * Legítimo para limpiar una instancia de bus que pertenece a un World:
     *   worldBus.clear(); // el bus muere junto con el world
     *
     * NO recomendado para GLOBAL a menos que la aplicación entera reinicie.
     */
    public void clear() {
        listeners.clear();
    }

    public boolean hasListeners(Class<?> eventType) {
        var list = listeners.get(eventType);
        return list != null && !list.isEmpty();
    }

    // ── API estática (compatibilidad) ─────────────────────────────────────────
    //
    // Estos métodos delegan a GLOBAL. Mantienen la API original sin cambios.
    // Si en el futuro se migra a instancias explícitas, eliminar estos métodos
    // y actualizar los llamadores para usar su instancia local.

    public static <T> Subscription staticSubscribe(Class<T> eventType, Consumer<T> listener) {
        return GLOBAL.subscribe(eventType, listener);
    }

    public static <T> void staticUnsubscribe(Class<T> eventType, Consumer<T> listener) {
        GLOBAL.unsubscribe(eventType, listener);
    }

    public static void staticPost(Object event) {
        GLOBAL.post(event);
    }

    public static void staticClear() {
        GLOBAL.clear();
    }

    // ── Implementación interna de Subscription ────────────────────────────────

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
