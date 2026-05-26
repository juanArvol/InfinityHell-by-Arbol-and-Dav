package Game.Events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Bus de eventos global — comunicación desacoplada entre sistemas.
 *
 * ── POR QUÉ ──────────────────────────────────────────────────────────────
 * Sin bus de eventos, Player tendría que conocer AudioSystem, UISystem,
 * FXSystem, etc. para notificarles cuando pasa algo. Eso crea acoplamiento
 * enorme. Con el bus, Player solo emite un evento y cualquier listener reacciona.
 *
 * ── DISEÑO ───────────────────────────────────────────────────────────────
 * Simple map de tipo → lista de consumers. Ni reflection ni anotaciones.
 * Thread-safe a nivel de registro (subscribe en init), no en post (single-thread).
 *
 * ── USO ──────────────────────────────────────────────────────────────────
 *   // Registrar listener (en UISystem, AudioSystem, etc.)
 *   GameEventBus.subscribe(OnPickupEvent.class, e -> ui.showPickupNotif(e));
 *   GameEventBus.subscribe(OnWeaponFireEvent.class, e -> audio.playShot(e.sound()));
 *
 *   // Emitir (en PickupSystem, PlayerCombat, etc.)
 *   GameEventBus.post(new OnPickupEvent(player, def, count));
 */
public final class GameEventBus {

    private static final Map<Class<?>, List<Consumer<Object>>> listeners = new HashMap<>();

    private GameEventBus() {}

    @SuppressWarnings("unchecked")
    public static <T> void subscribe(Class<T> eventType, Consumer<T> listener) {
        listeners.computeIfAbsent(eventType, k -> new ArrayList<>())
                 .add((Consumer<Object>) listener);
    }

    public static <T> void unsubscribe(Class<T> eventType, Consumer<T> listener) {
        var list = listeners.get(eventType);
        if (list != null) list.remove(listener);
    }

    public static void post(Object event) {
        var list = listeners.get(event.getClass());
        if (list == null) return;
        // Copia para evitar ConcurrentModification si un listener se desregistra
        for (var listener : List.copyOf(list)) {
            listener.accept(event);
        }
    }

    /** Limpia todos los listeners. Útil entre sesiones/niveles. */
    public static void clear() {
        listeners.clear();
    }
}
