package Game.Engine.Events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Bus de eventos — refactorizado para ser instanciable.
 *
 * PROBLEMA ANTERIOR:
 *   GameEventBus era completamente estático: listeners guardados en un Map
 *   estático compartido por toda la JVM. Esto implica:
 *   - En tests: los listeners de un test contaminan los siguientes.
 *   - En multiplayer: dos sesiones de juego comparirían el mismo bus.
 *   - No es posible crear un bus aislado por escena/nivel.
 *
 * SOLUCIÓN:
 *   GameEventBus es ahora una clase normal e instanciable.
 *   Cada instancia tiene su propio mapa de listeners independiente.
 *
 * COMPATIBILIDAD:
 *   Se mantiene una instancia global `GameEventBus.GLOBAL` para no romper
 *   el código existente que usa GameEventBus.post() y GameEventBus.subscribe().
 *   Los métodos estáticos delegan a GLOBAL.
 *
 *   Plan de migración:
 *   1. Hoy: todo usa GameEventBus.post() → funciona igual via GLOBAL.
 *   2. Cuando se necesite aislamiento: pasar la instancia por constructor
 *      y dejar de usar los métodos estáticos.
 *   3. En multiplayer: cada sesión tiene su propia instancia.
 *
 * THREAD SAFETY:
 *   subscribe()/unsubscribe() son seguros para llamar desde cualquier thread.
 *   post() está diseñado para single-thread (game loop). Si en el futuro se
 *   necesita post() multi-thread, añadir sincronización en los listeners.
 *
 * MEMORY LEAKS:
 *   Llamar clear() al cambiar de escena o nivel para liberar listeners
 *   que capturan objetos del mundo anterior.
 */
public final class GameEventBus {

    /**
     * Instancia global para compatibilidad con código existente.
     * Usar esta instancia es correcto para un juego single-player single-scene.
     * Para escenas aisladas o multiplayer, crear instancias separadas.
     */
    public static final GameEventBus GLOBAL = new GameEventBus();

    // ── Estado de instancia ────────────────────────────────────────────────────

    private final Map<Class<?>, List<Consumer<Object>>> listeners = new HashMap<>();

    public GameEventBus() {}

    // ── API de instancia ──────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public <T> void subscribe(Class<T> eventType, Consumer<T> listener) {
        listeners.computeIfAbsent(eventType, k -> new ArrayList<>())
                 .add((Consumer<Object>) listener);
    }

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
     * IMPORTANTE: llamar al cambiar de escena o nivel para evitar memory leaks
     * causados por listeners que capturan objetos del mundo anterior.
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

    public static <T> void staticSubscribe(Class<T> eventType, Consumer<T> listener) {
        GLOBAL.subscribe(eventType, listener);
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
}
