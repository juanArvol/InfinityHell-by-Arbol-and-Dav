package Game.Engine.Entity.Capabilities;

import Game.Engine.Component;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Componente que registra las capacidades de una entidad.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * CapabilityComponent almacena el conjunto de cosas que una entidad PUEDE
 * HACER o RECIBIR. Los sistemas consultan este componente para decidir si
 * una interacción es aplicable, sin hacer instanceof sobre tipos concretos.
 *
 * ── MODELO DE ALMACENAMIENTO ─────────────────────────────────────────────
 * Las capacidades se guardan en un IdentityHashMap<Class, GameplayCapability>.
 * La clave es la clase de la capacidad (para lookup O(1) por tipo).
 * El valor es la instancia concreta (que puede tener datos si es una clase).
 *
 * Para capacidades simples (flags sin datos) se usa la instancia singleton
 * de CoreCapabilities. Para capacidades con datos se instancia la clase
 * concreta en el momento de añadirla.
 *
 * ── USO ──────────────────────────────────────────────────────────────────
 *
 *   // En el constructor de un proyectil:
 *   CapabilityComponent caps = new CapabilityComponent();
 *   caps.add(CoreCapabilities.CAN_BOUNCE);
 *   caps.add(CoreCapabilities.CAN_EXPLODE);
 *   addComponent(caps);
 *
 *   // En el sistema de efectos de hielo:
 *   CapabilityComponent caps = target.getComponent(CapabilityComponent.class);
 *   if (caps != null && caps.has(CoreCapabilities.CAN_FREEZE)) {
 *       applyFreezeEffect(target);
 *   }
 *
 *   // Capacidad con datos (BounceCapability implementa GameplayCapability):
 *   caps.add(new BounceCapability(3));
 *   BounceCapability bounce = caps.get(BounceCapability.class);
 *   if (bounce != null) { bounce.decrementBounces(); }
 *
 * ── DIFERENCIA HAS vs GET ─────────────────────────────────────────────────
 * - has(instance)    → busca por referencia de instancia (para singletons)
 * - has(Class)       → busca por tipo (para capacidades con datos)
 * - get(Class)       → retorna la instancia tipada (para acceder a sus datos)
 */
public final class CapabilityComponent extends Component {

    /**
     * Mapa de clase → instancia.
     * IdentityHashMap no: aquí queremos igualdad por clase, no por identidad.
     */
    private final Map<Class<? extends GameplayCapability>, GameplayCapability> capabilities
        = new IdentityHashMap<>();

    // ── Mutación ──────────────────────────────────────────────────────────

    /**
     * Añade una capacidad. Si ya existe una capacidad del mismo tipo, la reemplaza.
     *
     * @param capability instancia de la capacidad. No puede ser null.
     */
    public void add(GameplayCapability capability) {
        if (capability == null) throw new IllegalArgumentException("Capability no puede ser null.");
        capabilities.put(capability.getClass(), capability);
    }

    /**
     * Elimina la capacidad del tipo indicado.
     * Si no existe, la operación no tiene efecto.
     */
    public void remove(Class<? extends GameplayCapability> type) {
        capabilities.remove(type);
    }

    /**
     * Elimina una capacidad por su instancia.
     * Equivale a remove(capability.getClass()).
     */
    public void remove(GameplayCapability capability) {
        if (capability != null) capabilities.remove(capability.getClass());
    }

    // ── Consultas ─────────────────────────────────────────────────────────

    /**
     * True si esta entidad tiene una capacidad del tipo indicado.
     */
    public boolean has(Class<? extends GameplayCapability> type) {
        return capabilities.containsKey(type);
    }

    /**
     * Atajo: true si esta entidad tiene la capacidad representada por la instancia dada.
     * Internamente delega en has(capability.getClass()).
     *
     * Es el método de consulta más común para capacidades singleton:
     *   if (caps.has(CoreCapabilities.CAN_FREEZE)) { ... }
     */
    public boolean has(GameplayCapability capability) {
        return capability != null && capabilities.containsKey(capability.getClass());
    }

    /**
     * Retorna la instancia de la capacidad del tipo indicado, o null si no existe.
     * Útil para capacidades con datos que necesitas leer.
     *
     * @param type clase de la capacidad
     * @return instancia tipada, o null si no está presente
     */
    @SuppressWarnings("unchecked")
    public <T extends GameplayCapability> T get(Class<T> type) {
        return (T) capabilities.get(type);
    }

    /**
     * Vista no modificable de todas las capacidades registradas.
     */
    public Collection<GameplayCapability> all() {
        return Collections.unmodifiableCollection(capabilities.values());
    }

    /**
     * Número de capacidades registradas.
     */
    public int size() {
        return capabilities.size();
    }
}
