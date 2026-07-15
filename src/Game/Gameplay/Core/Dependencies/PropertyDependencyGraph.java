package Game.Gameplay.Core.Dependencies;

import Game.Gameplay.Core.Properties.PropertyKey;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Grafo dirigido de dependencias entre propiedades de gameplay.
 *
 * ── RESPONSABILIDAD ÚNICA ─────────────────────────────────────────────────
 * PropertyDependencyGraph almacena y organiza las relaciones:
 *
 *   "La propiedad A influye sobre la propiedad B"
 *
 * Y responde a:
 *   "¿Qué propiedades dependen de A?"
 *   "¿De qué propiedades depende B?"
 *   "¿Existe un ciclo entre A y B?"
 *   "¿Cuál es el orden topológico de resolución desde A?"
 *
 * NO calcula valores.
 * NO aplica cambios.
 * NO conoce entidades del juego.
 *
 * Su única responsabilidad es la estructura del grafo: aristas dirigidas,
 * detección de ciclos, y consulta de dependencias.
 *
 * ── MODELO: GRAFO DIRIGIDO ────────────────────────────────────────────────
 * El grafo es un DAG (Directed Acyclic Graph) idealmente, pero soporta
 * ciclos — los detecta y los registra sin propagarlos.
 *
 * Ejemplo de grafo:
 *
 *   Temperature ──► MovementSpeed ──► AttackSpeed ──► AnimationSpeed
 *                        │
 *                        └──────────────────────────► ProjectileSpeed
 *
 * Cada flecha es una PropertyDependency (arista dirigida).
 * Una propiedad puede tener múltiples salidas (afecta múltiples propiedades).
 * Una propiedad puede tener múltiples entradas (recibe efectos de múltiples).
 *
 * ── DETECCIÓN DE CICLOS ──────────────────────────────────────────────────
 * Antes de añadir una arista A → B, el grafo verifica que B no sea ancestro
 * de A (lo que crearía un ciclo). Si lo es, la arista se rechaza con
 * excepción. Esto garantiza que el grafo estructural nunca tiene ciclos.
 *
 * Para casos donde los ciclos son inevitables en el diseño del juego,
 * existe addEdgeUnchecked() que omite la verificación. En ese caso,
 * el DependencyPropagator detecta ciclos en tiempo de propagación y los corta.
 *
 * ── SOPORTE DE CONVERGENCIA ──────────────────────────────────────────────
 * Una propiedad puede recibir influencia desde múltiples propiedades:
 *
 *   Temperature ──► Speed
 *   Mass        ──► Speed
 *   Fatigue     ──► Speed
 *
 * Speed tiene 3 aristas entrantes. El propagador las aplica todas.
 *
 * ── INSTANCIABILIDAD ─────────────────────────────────────────────────────
 * PropertyDependencyGraph es instanciable. No hay singleton global.
 * Cada entidad o sistema puede tener su propio grafo, o múltiples sistemas
 * pueden compartir uno pasado por referencia.
 *
 * ── THREAD SAFETY ────────────────────────────────────────────────────────
 * No es thread-safe. Usar desde el game loop thread.
 *
 * @see PropertyDependency
 * @see DependencyPropagator
 */
public final class PropertyDependencyGraph {

    /**
     * Índice de aristas salientes: clave del origen → lista de dependencias.
     * Permite encontrar rápidamente qué propiedades dependen de una dada.
     */
    private final Map<String, List<PropertyDependency>> outgoing = new HashMap<>();

    /**
     * Índice de aristas entrantes: clave del destino → lista de dependencias.
     * Permite encontrar rápidamente de qué depende una propiedad.
     */
    private final Map<String, List<PropertyDependency>> incoming = new HashMap<>();

    /**
     * Índice plano de todas las aristas por tag, para dar de baja eficientemente.
     */
    private final Map<String, List<PropertyDependency>> byTag = new HashMap<>();

    // ── Mutación ──────────────────────────────────────────────────────────

    /**
     * Añade una dependencia al grafo con verificación de ciclos.
     *
     * Si añadir esta arista crearía un ciclo (A → B y ya existe un camino B → A),
     * lanza IllegalStateException. Usar {@link #addEdgeUnchecked} si se requiere
     * admitir ciclos controlados.
     *
     * @param dependency arista dirigida a añadir
     * @throws IllegalArgumentException si dependency es null
     * @throws IllegalStateException    si la arista crearía un ciclo estructural
     */
    public void addEdge(PropertyDependency dependency) {
        if (dependency == null)
            throw new IllegalArgumentException("dependency no puede ser null.");

        String srcId = dependency.getSourceKey().id();
        String dstId = dependency.getTargetKey().id();

        // Verificación de ciclo: ¿existe ya un camino de dst → src?
        if (canReach(dstId, srcId)) {
            throw new IllegalStateException(
                "Ciclo de dependencia detectado: añadir '"
                + srcId + " → " + dstId
                + "' crearía un ciclo porque ya existe un camino '"
                + dstId + " → " + srcId + "'."
            );
        }

        insertEdge(dependency);
    }

    /**
     * Añade una dependencia al grafo sin verificación de ciclos.
     *
     * Usar cuando el diseño del juego requiere dependencias cíclicas
     * controladas. El DependencyPropagator cortará los ciclos en tiempo
     * de propagación usando el conjunto de visitados del frame.
     *
     * @param dependency arista dirigida a añadir
     * @throws IllegalArgumentException si dependency es null
     */
    public void addEdgeUnchecked(PropertyDependency dependency) {
        if (dependency == null)
            throw new IllegalArgumentException("dependency no puede ser null.");
        insertEdge(dependency);
    }

    /**
     * Añade múltiples dependencias al grafo con verificación de ciclos.
     *
     * @param dependencies aristas a añadir
     */
    public void addEdges(PropertyDependency... dependencies) {
        if (dependencies == null) return;
        for (PropertyDependency dep : dependencies) {
            addEdge(dep);
        }
    }

    /**
     * Elimina todas las dependencias registradas bajo el tag indicado.
     * Si el tag no existe, la operación no tiene efecto.
     *
     * @param tag tag de las dependencias a eliminar
     */
    public void removeByTag(String tag) {
        List<PropertyDependency> toRemove = byTag.remove(tag);
        if (toRemove == null) return;
        for (PropertyDependency dep : toRemove) {
            String srcId = dep.getSourceKey().id();
            String dstId = dep.getTargetKey().id();

            List<PropertyDependency> out = outgoing.get(srcId);
            if (out != null) out.removeIf(d -> d.getTag().equals(tag));

            List<PropertyDependency> in = incoming.get(dstId);
            if (in != null) in.removeIf(d -> d.getTag().equals(tag));
        }
    }

    /**
     * Elimina todas las dependencias del grafo.
     */
    public void clear() {
        outgoing.clear();
        incoming.clear();
        byTag.clear();
    }

    // ── Consultas de aristas ──────────────────────────────────────────────

    /**
     * Retorna las dependencias que parten de la propiedad indicada,
     * es decir, las propiedades que son afectadas cuando esta cambia.
     * Ordenadas por prioridad ascendente.
     *
     * @param sourceKey propiedad origen
     * @return lista ordenada de dependencias salientes (puede estar vacía)
     */
    public List<PropertyDependency> getDependenciesFrom(PropertyKey<?> sourceKey) {
        if (sourceKey == null) return Collections.emptyList();
        List<PropertyDependency> deps = outgoing.get(sourceKey.id());
        if (deps == null || deps.isEmpty()) return Collections.emptyList();
        List<PropertyDependency> sorted = new ArrayList<>(deps);
        sorted.sort(Comparator.comparingInt(PropertyDependency::getPriority));
        return Collections.unmodifiableList(sorted);
    }

    /**
     * Retorna las dependencias que llegan a la propiedad indicada,
     * es decir, las propiedades de las que depende esta.
     * Ordenadas por prioridad ascendente.
     *
     * @param targetKey propiedad destino
     * @return lista ordenada de dependencias entrantes (puede estar vacía)
     */
    public List<PropertyDependency> getDependenciesTo(PropertyKey<?> targetKey) {
        if (targetKey == null) return Collections.emptyList();
        List<PropertyDependency> deps = incoming.get(targetKey.id());
        if (deps == null || deps.isEmpty()) return Collections.emptyList();
        List<PropertyDependency> sorted = new ArrayList<>(deps);
        sorted.sort(Comparator.comparingInt(PropertyDependency::getPriority));
        return Collections.unmodifiableList(sorted);
    }

    /**
     * True si la propiedad origen tiene al menos una dependencia saliente.
     *
     * @param sourceKey propiedad a consultar
     */
    public boolean hasDependenciesFrom(PropertyKey<?> sourceKey) {
        if (sourceKey == null) return false;
        List<PropertyDependency> deps = outgoing.get(sourceKey.id());
        return deps != null && !deps.isEmpty();
    }

    /**
     * True si la propiedad destino tiene al menos una dependencia entrante.
     *
     * @param targetKey propiedad a consultar
     */
    public boolean hasDependenciesTo(PropertyKey<?> targetKey) {
        if (targetKey == null) return false;
        List<PropertyDependency> deps = incoming.get(targetKey.id());
        return deps != null && !deps.isEmpty();
    }

    /**
     * True si existe alguna dependencia con el tag indicado.
     *
     * @param tag tag a buscar
     */
    public boolean hasTag(String tag) {
        return byTag.containsKey(tag) && !byTag.get(tag).isEmpty();
    }

    /**
     * Número total de aristas en el grafo.
     */
    public int edgeCount() {
        int total = 0;
        for (List<PropertyDependency> deps : outgoing.values()) {
            total += deps.size();
        }
        return total;
    }

    /**
     * True si el grafo no tiene ninguna arista.
     */
    public boolean isEmpty() {
        return outgoing.isEmpty();
    }

    // ── Consultas de alcanzabilidad ───────────────────────────────────────

    /**
     * Retorna todas las propiedades alcanzables desde {@code sourceKey}
     * siguiendo las aristas del grafo en profundidad (DFS).
     *
     * Incluye los destinos directos y todos sus descendientes transitivos.
     * No incluye la propiedad origen.
     *
     * @param sourceKey propiedad desde la que buscar alcanzabilidad
     * @return conjunto de IDs de propiedades alcanzables (puede estar vacío)
     */
    public Set<String> reachableFrom(PropertyKey<?> sourceKey) {
        if (sourceKey == null || !outgoing.containsKey(sourceKey.id())) {
            return Collections.emptySet();
        }
        Set<String> visited = new HashSet<>();
        collectReachable(sourceKey.id(), visited);
        visited.remove(sourceKey.id()); // no incluir el origen
        return Collections.unmodifiableSet(visited);
    }

    /**
     * True si existe un camino dirigido desde {@code from} hasta {@code to}
     * siguiendo las aristas del grafo.
     *
     * @param fromId ID de la propiedad origen
     * @param toId   ID de la propiedad destino
     * @return true si to es alcanzable desde from
     */
    public boolean canReach(String fromId, String toId) {
        if (fromId == null || toId == null) return false;
        if (fromId.equals(toId)) return true; // trivialmente alcanzable
        Set<String> visited = new HashSet<>();
        return dfsCanReach(fromId, toId, visited);
    }

    // ── Implementación interna ────────────────────────────────────────────

    private void insertEdge(PropertyDependency dependency) {
        String srcId = dependency.getSourceKey().id();
        String dstId = dependency.getTargetKey().id();
        String tag   = dependency.getTag();

        outgoing.computeIfAbsent(srcId, k -> new ArrayList<>()).add(dependency);
        incoming.computeIfAbsent(dstId, k -> new ArrayList<>()).add(dependency);
        byTag.computeIfAbsent(tag,   k -> new ArrayList<>()).add(dependency);
    }

    private void collectReachable(String currentId, Set<String> visited) {
        if (visited.contains(currentId)) return;
        visited.add(currentId);
        List<PropertyDependency> deps = outgoing.get(currentId);
        if (deps != null) {
            for (PropertyDependency dep : deps) {
                collectReachable(dep.getTargetKey().id(), visited);
            }
        }
    }

    private boolean dfsCanReach(String currentId, String targetId, Set<String> visited) {
        if (visited.contains(currentId)) return false;
        visited.add(currentId);
        List<PropertyDependency> deps = outgoing.get(currentId);
        if (deps == null) return false;
        for (PropertyDependency dep : deps) {
            String nextId = dep.getTargetKey().id();
            if (nextId.equals(targetId)) return true;
            if (dfsCanReach(nextId, targetId, visited)) return true;
        }
        return false;
    }

    // ── Object ────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "PropertyDependencyGraph["
            + "edges=" + edgeCount()
            + ", origins=" + outgoing.size()
            + "]";
    }
}
