package Game.Engine.Physics.Core;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registro genérico de Domain States del simulador.
 *
 * ── HRFC-032 — Evolución del SimulationContext hacia un registro extensible ─
 *
 * ── RESPONSABILIDAD ──────────────────────────────────────────────────────
 * DomainStateRegistry almacena instancias de DomainState indexadas por su
 * tipo concreto (Class<T>). Es el mecanismo que desacopla SimulationContext
 * de los dominios específicos que aloja.
 *
 * En lugar de exponer getters específicos (getKinematicState(),
 * getMaterialState()…), SimulationContext delega en este registro para
 * ofrecer acceso genérico y tipado en tiempo de compilación:
 *
 *   context.state(KinematicState.class)    // devuelve KinematicState
 *   context.state(MaterialState.class)     // devuelve MaterialState
 *   context.state(ChemicalState.class)     // devuelve ChemicalState
 *
 * ── TYPE SAFETY ───────────────────────────────────────────────────────────
 * El cast interno es seguro porque la clave y el valor son el mismo tipo T:
 * register(T) almacena entry (T.class → T), por lo que get(T.class) siempre
 * produce una instancia de T o null. No puede producir ClassCastException.
 *
 * ── PRINCIPIO OPEN/CLOSED ─────────────────────────────────────────────────
 * Añadir un nuevo dominio NO requiere modificar SimulationContext.
 * Solo requiere:
 *   1. Crear la clase del nuevo estado implementando DomainState.
 *   2. Registrar una instancia en el Builder de SimulationContext.
 *   3. Consumirla con context.state(NuevoDominio.class).
 *
 * ── ORDEN DE INSERCIÓN ────────────────────────────────────────────────────
 * El registro preserva el orden de inserción (LinkedHashMap). Esto hace que
 * la colección all() sea predecible para depuración e introspección.
 *
 * ── THREAD SAFETY ─────────────────────────────────────────────────────────
 * No es thread-safe. Usar exclusivamente desde el game loop thread.
 */
public final class DomainStateRegistry {

    /**
     * Mapa interno: tipo concreto → instancia del estado.
     * LinkedHashMap para preservar el orden de inserción.
     * La clave es Class<?> y el cast en get() es seguro por invariante de register().
     */
    private final Map<Class<? extends DomainState>, DomainState> states =
        new LinkedHashMap<>();

    // ── Constructor ───────────────────────────────────────────────────────

    /** Crea un registro vacío. */
    public DomainStateRegistry() {}

    // ── Escritura ─────────────────────────────────────────────────────────

    /**
     * Registra (o reemplaza) un estado de dominio.
     *
     * La clave de acceso es el tipo concreto en tiempo de ejecución del estado.
     * Un segundo registro del mismo tipo reemplaza el anterior.
     *
     * @param state el estado a registrar. Ignorado si null.
     * @return this (para encadenado en el Builder de SimulationContext).
     */
    public DomainStateRegistry register(DomainState state) {
        if (state == null) return this;
        states.put(state.getClass(), state);
        return this;
    }

    // ── Lectura ───────────────────────────────────────────────────────────

    /**
     * Retorna el estado del dominio identificado por el tipo dado.
     *
     * El cast es seguro: register(T) almacena (T.class → T), por lo que
     * get(T.class) siempre devuelve T o null.
     *
     * Ejemplo de uso en un evaluador:
     *   KinematicState kin = context.state(KinematicState.class);
     *   if (kin == null) continue;
     *
     * @param type tipo del estado de dominio a buscar. No puede ser null.
     * @param <T>  el tipo del estado de dominio.
     * @return la instancia registrada, o null si no hay ninguna para ese tipo.
     */
    @SuppressWarnings("unchecked")
    public <T extends DomainState> T get(Class<T> type) {
        if (type == null) return null;
        return (T) states.get(type);
    }

    /**
     * True si hay un estado registrado para el tipo dado.
     *
     * @param type tipo del estado a verificar. No puede ser null.
     * @return true si existe una instancia registrada.
     */
    public boolean has(Class<? extends DomainState> type) {
        return type != null && states.containsKey(type);
    }

    /**
     * Colección inmutable de todos los estados registrados, en orden de inserción.
     *
     * Útil para introspección, serialización y depuración.
     *
     * @return colección inmutable de DomainState. Nunca null.
     */
    public Collection<DomainState> all() {
        return Collections.unmodifiableCollection(states.values());
    }

    /** Número de dominios registrados. */
    public int size() { return states.size(); }

    /** True si no hay ningún dominio registrado. */
    public boolean isEmpty() { return states.isEmpty(); }

    // ── Object ────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "DomainStateRegistry[" + states.size() + " domains: "
            + states.keySet().stream()
                   .map(Class::getSimpleName)
                   .reduce((a, b) -> a + ", " + b)
                   .orElse("")
            + "]";
    }
}
