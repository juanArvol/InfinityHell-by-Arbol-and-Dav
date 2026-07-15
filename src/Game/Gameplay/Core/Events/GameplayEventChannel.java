package Game.Gameplay.Core.Events;

import java.util.*;
import java.util.function.Consumer;

/**
 * Canal de eventos de gameplay con soporte de interceptación y modificación.
 *
 * ── DIFERENCIA CON GameEventBus ──────────────────────────────────────────
 *
 *   GameEventBus.post()  → notifica a todos los listeners SIN ORDEN garantizado.
 *                          Los listeners no pueden modificar el evento.
 *                          Pensado para observación post-hecho.
 *
 *   GameplayEventChannel → llama interceptores en ORDEN DE PRIORIDAD.
 *                          Los interceptores PUEDEN modificar el evento.
 *                          El productor del evento lee el evento modificado
 *                          tras fire() para conocer el resultado final.
 *                          Pensado para resolución pre-acción.
 *
 * ── FLUJO DE RESOLUCIÓN ───────────────────────────────────────────────────
 *
 *   1. El productor crea el evento con los valores iniciales.
 *   2. fire(event) llama a los interceptores en orden de prioridad (menor = primero).
 *   3. Cada interceptor puede:
 *      - Leer y modificar los campos del evento.
 *      - Llamar event.cancel() para impedir la acción.
 *      - No hacer nada (ignorar el evento).
 *   4. Si un interceptor cancela, los interceptores de menor prioridad
 *      aún se ejecutan (observabilidad). Para parar completamente usar
 *      fireAndStop() que detiene al primer cancel().
 *   5. El productor lee event.isCancelled() y los campos finales.
 *
 * ── INSTANCIABILIDAD ─────────────────────────────────────────────────────
 * GameplayEventChannel es instanciable para permitir canales aislados por
 * entidad, por sistema, o por escena. No hay singleton global de canal.
 *
 * Uso típico: un canal por sistema que emite eventos (CombatChannel,
 * MovementChannel, SpawnChannel). Cada entidad que produce ese tipo de
 * evento tiene acceso a su canal relevante.
 *
 * ── PRIORIDAD ────────────────────────────────────────────────────────────
 * Menor valor = se ejecuta primero. Rango recomendado: 0–1000.
 *   0–99   : interceptores de negación (escudo absoluto, inmunidad)
 *   100–299: interceptores de reducción (resistencias, armadura)
 *   300–499: interceptores de amplificación (vulnerabilidad, debuffs)
 *   500+   : interceptores de observación (logging, efectos visuales)
 *
 * @param <E> tipo de evento que maneja este canal.
 */
public final class GameplayEventChannel<E extends GameplayEvent> {

    private record Entry<E>(int priority, Consumer<E> interceptor) {}

    private final List<Entry<E>> entries = new ArrayList<>();
    private boolean sorted = true;

    // ── Registro de interceptores ─────────────────────────────────────────

    /**
     * Registra un interceptor con prioridad explícita.
     *
     * @param priority    menor = se ejecuta antes (0 = máxima prioridad)
     * @param interceptor Consumer que recibe el evento y puede modificarlo
     */
    public void register(int priority, Consumer<E> interceptor) {
        if (interceptor == null) throw new IllegalArgumentException("Interceptor no puede ser null.");
        entries.add(new Entry<>(priority, interceptor));
        sorted = false;
    }

    /**
     * Registra un interceptor con prioridad por defecto (500).
     */
    public void register(Consumer<E> interceptor) {
        register(500, interceptor);
    }

    /**
     * Elimina un interceptor registrado.
     * Si el mismo Consumer fue registrado múltiples veces, elimina la primera
     * ocurrencia de menor índice.
     */
    public void unregister(Consumer<E> interceptor) {
        entries.removeIf(e -> e.interceptor() == interceptor);
    }

    /**
     * Elimina todos los interceptores de este canal.
     * Llamar al cambiar de escena o al destruir el sistema propietario.
     */
    public void clear() {
        entries.clear();
        sorted = true;
    }

    // ── Disparo ───────────────────────────────────────────────────────────

    /**
     * Dispara el evento a todos los interceptores en orden de prioridad.
     *
     * Los interceptores que se registraron después de la última llamada a
     * fire() se ordenan antes de ejecutar. El coste de ordenación es
     * amortizado: solo se ordena cuando hay cambios en los registros.
     *
     * Todos los interceptores se ejecutan, incluso si el evento fue cancelado.
     * Para parar al primer cancel, usar {@link #fireAndStop(GameplayEvent)}.
     *
     * @param event evento a disparar. Sus campos pueden ser modificados in-place.
     */
    public void fire(E event) {
        ensureSorted();
        for (Entry<E> entry : entries) {
            entry.interceptor().accept(event);
        }
    }

    /**
     * Dispara el evento y detiene la cadena en cuanto algún interceptor lo cancele.
     *
     * Útil cuando la cancelación debe impedir que interceptores posteriores
     * "observen" el evento cancelado (ej: no disparar efectos visuales de un
     * daño que fue bloqueado completamente).
     *
     * @param event evento a disparar.
     */
    public void fireAndStop(E event) {
        ensureSorted();
        for (Entry<E> entry : entries) {
            if (event.isCancelled()) break;
            entry.interceptor().accept(event);
        }
    }

    /**
     * True si hay al menos un interceptor registrado.
     */
    public boolean hasInterceptors() {
        return !entries.isEmpty();
    }

    // ── Orden ─────────────────────────────────────────────────────────────

    private void ensureSorted() {
        if (!sorted) {
            entries.sort(Comparator.comparingInt(Entry::priority));
            sorted = true;
        }
    }
}
