package Game.Engine.World.Graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Motor genérico de grafos dirigidos con nodos tipados.
 *
 * ── HRFC-016 — Consolidación del modelo emergente ────────────────────────
 *
 * ── RESPONSABILIDAD ──────────────────────────────────────────────────────
 * DependencyGraph<T> es el único motor de grafos del Engine.
 * Toda representación basada en nodos y relaciones se construye sobre esta
 * infraestructura. No existen motores de grafos independientes.
 *
 * El parámetro T representa el tipo de nodo. Las distintas especializaciones
 * del Engine son simplemente instancias con distintos tipos de nodo:
 *
 *   DependencyGraph<PhysicalProperty>   → relaciones entre propiedades físicas
 *   DependencyGraph<GameplayProperty>   → dependencias propias del Gameplay
 *   DependencyGraph<Property>           → dependencias generales entre propiedades
 *
 * Estas especializaciones representan únicamente distintos tipos de nodo.
 * Nunca distintos motores. Toda la lógica de almacenamiento, recorrido,
 * resolución y propagación vive exclusivamente aquí.
 *
 * ── MODELO ────────────────────────────────────────────────────────────────
 * El grafo es dirigido. Cada arista (GraphEdge<T>) conecta un nodo T origen
 * con un nodo T destino. Las aristas pueden tener destino nulo para relaciones
 * que producen efectos sin modificar un dominio específico.
 *
 * Estructura de índices:
 *   bySource → nodoId → lista de aristas salientes
 *   byTarget → nodoId → lista de aristas entrantes
 *   byTag    → tag    → lista de aristas con ese tag (para remoción O(1))
 *   allEdges → lista completa ordenada por prioridad (lazy sort)
 *
 * ── DETECCIÓN DE CICLOS ──────────────────────────────────────────────────
 * addEdge() verifica que la arista A → B no cree un ciclo (no exista ya un
 * camino de B → A). Usar addEdgeUnchecked() para ciclos controlados.
 *
 * ── IDENTIDAD DE NODO ────────────────────────────────────────────────────
 * El grafo necesita identificar nodos por una cadena única. Esto se provee
 * mediante una Function<T, String> nodeId inyectada en construcción, que
 * extrae el identificador string del nodo T.
 *
 * Por ejemplo:
 *   DependencyGraph<PhysicalProperty>  → nodeId = PhysicalProperty::id
 *   DependencyGraph<PropertyKey<?>>    → nodeId = PropertyKey::id
 *
 * ── THREAD SAFETY ─────────────────────────────────────────────────────────
 * No es thread-safe. Usar exclusivamente desde el game loop thread.
 *
 * @param <T> tipo de nodo del grafo.
 */
public final class DependencyGraph<T> {

    /** Función que extrae el id único de un nodo. */
    private final Function<T, String> nodeId;

    /** Índice de aristas salientes: id del nodo origen → lista de aristas. */
    private final Map<String, List<GraphEdge<T>>> bySource = new HashMap<>();

    /** Índice de aristas entrantes: id del nodo destino → lista de aristas. */
    private final Map<String, List<GraphEdge<T>>> byTarget = new HashMap<>();

    /** Índice por tag para remoción eficiente. */
    private final Map<String, List<GraphEdge<T>>> byTag = new HashMap<>();

    /** Lista completa de aristas en orden de inserción / prioridad. */
    private final List<GraphEdge<T>> allEdges = new ArrayList<>();

    /** True si allEdges necesita reordenarse por prioridad. */
    private boolean dirty = false;

    // ── Constructor ───────────────────────────────────────────────────────

    /**
     * Crea un grafo con la función de identidad de nodo indicada.
     *
     * @param nodeId función que extrae el id único de un nodo T. No puede ser null.
     */
    public DependencyGraph(Function<T, String> nodeId) {
        if (nodeId == null) throw new IllegalArgumentException("nodeId no puede ser null");
        this.nodeId = nodeId;
    }

    // ── Mutación ──────────────────────────────────────────────────────────

    /**
     * Añade una arista al grafo con verificación de ciclos.
     *
     * Si la arista A → B crearía un ciclo (existe camino B → A),
     * lanza IllegalStateException. Usar {@link #addEdgeUnchecked} si se
     * requieren ciclos controlados.
     *
     * @param edge arista a añadir. No hace nada si null.
     * @throws IllegalStateException si la arista crearía un ciclo estructural.
     */
    public void addEdge(GraphEdge<T> edge) {
        if (edge == null) return;

        if (edge.getTarget() != null) {
            String srcId = nodeId.apply(edge.getSource());
            String dstId = nodeId.apply(edge.getTarget());
            if (canReach(dstId, srcId)) {
                throw new IllegalStateException(
                    "Ciclo detectado al añadir '" + srcId + " → " + dstId
                    + "': ya existe un camino '" + dstId + " → " + srcId + "'."
                );
            }
        }

        insertEdge(edge);
    }

    /**
     * Añade una arista sin verificación de ciclos.
     * Usar cuando el diseño requiere dependencias cíclicas controladas.
     *
     * @param edge arista a añadir. No hace nada si null.
     */
    public void addEdgeUnchecked(GraphEdge<T> edge) {
        if (edge == null) return;
        insertEdge(edge);
    }

    /**
     * Añade múltiples aristas con verificación de ciclos.
     *
     * @param edges aristas a añadir.
     */
    @SafeVarargs
    public final void addEdges(GraphEdge<T>... edges) {
        if (edges == null) return;
        for (GraphEdge<T> e : edges) addEdge(e);
    }

    /**
     * Elimina todas las aristas con el tag dado.
     * Útil para desactivar grupos de relaciones en runtime.
     *
     * @param tag tag de las aristas a eliminar.
     */
    public void removeByTag(String tag) {
        List<GraphEdge<T>> toRemove = byTag.remove(tag);
        if (toRemove == null) return;

        for (GraphEdge<T> edge : toRemove) {
            String srcId = nodeId.apply(edge.getSource());
            List<GraphEdge<T>> srcList = bySource.get(srcId);
            if (srcList != null) srcList.removeIf(e -> e.getTag().equals(tag));

            if (edge.getTarget() != null) {
                String dstId = nodeId.apply(edge.getTarget());
                List<GraphEdge<T>> dstList = byTarget.get(dstId);
                if (dstList != null) dstList.removeIf(e -> e.getTag().equals(tag));
            }
        }

        allEdges.removeIf(e -> e.getTag().equals(tag));
        dirty = true;
    }

    /** Elimina todas las aristas del grafo. */
    public void clear() {
        bySource.clear();
        byTarget.clear();
        byTag.clear();
        allEdges.clear();
        dirty = false;
    }

    // ── Consultas ─────────────────────────────────────────────────────────

    /**
     * Retorna todas las aristas que parten del nodo dado, ordenadas por prioridad.
     *
     * @param node nodo de origen.
     * @return lista inmutable de aristas salientes (vacía si no hay).
     */
    public List<GraphEdge<T>> getEdgesFrom(T node) {
        if (node == null) return Collections.emptyList();
        List<GraphEdge<T>> edges = bySource.get(nodeId.apply(node));
        if (edges == null || edges.isEmpty()) return Collections.emptyList();
        List<GraphEdge<T>> sorted = new ArrayList<>(edges);
        sorted.sort(Comparator.comparingInt(GraphEdge::getPriority));
        return Collections.unmodifiableList(sorted);
    }

    /**
     * Retorna todas las aristas que apuntan al nodo dado, ordenadas por prioridad.
     *
     * @param node nodo de destino.
     * @return lista inmutable de aristas entrantes (vacía si no hay).
     */
    public List<GraphEdge<T>> getEdgesTo(T node) {
        if (node == null) return Collections.emptyList();
        List<GraphEdge<T>> edges = byTarget.get(nodeId.apply(node));
        if (edges == null || edges.isEmpty()) return Collections.emptyList();
        List<GraphEdge<T>> sorted = new ArrayList<>(edges);
        sorted.sort(Comparator.comparingInt(GraphEdge::getPriority));
        return Collections.unmodifiableList(sorted);
    }

    /**
     * Retorna todas las aristas del grafo como lista inmutable,
     * ordenadas por prioridad ascendente. Lazy sort — solo reordena cuando
     * se añaden o eliminan aristas.
     *
     * @return lista inmutable de todas las aristas ordenadas por prioridad.
     */
    public List<GraphEdge<T>> allEdges() {
        if (dirty) {
            allEdges.sort(Comparator.comparingInt(GraphEdge::getPriority));
            dirty = false;
        }
        return Collections.unmodifiableList(allEdges);
    }

    /**
     * Retorna todas las aristas con el tag dado.
     *
     * @param tag identificador del grupo.
     * @return lista inmutable de aristas con ese tag (vacía si no hay).
     */
    public List<GraphEdge<T>> getEdgesByTag(String tag) {
        if (tag == null) return Collections.emptyList();
        List<GraphEdge<T>> edges = byTag.get(tag);
        if (edges == null || edges.isEmpty()) return Collections.emptyList();
        return Collections.unmodifiableList(edges);
    }

    /**
     * True si el nodo dado tiene al menos una arista saliente.
     *
     * @param node nodo a consultar.
     */
    public boolean hasEdgesFrom(T node) {
        if (node == null) return false;
        List<GraphEdge<T>> edges = bySource.get(nodeId.apply(node));
        return edges != null && !edges.isEmpty();
    }

    /**
     * True si el nodo dado tiene al menos una arista entrante.
     *
     * @param node nodo a consultar.
     */
    public boolean hasEdgesTo(T node) {
        if (node == null) return false;
        List<GraphEdge<T>> edges = byTarget.get(nodeId.apply(node));
        return edges != null && !edges.isEmpty();
    }

    /**
     * True si existe alguna arista con el tag indicado.
     *
     * @param tag tag a buscar.
     */
    public boolean hasTag(String tag) {
        return byTag.containsKey(tag) && !byTag.get(tag).isEmpty();
    }

    /** Número total de aristas en el grafo. */
    public int edgeCount() { return allEdges.size(); }

    /** True si el grafo no tiene aristas. */
    public boolean isEmpty() { return allEdges.isEmpty(); }

    // ── Detección de ciclos ───────────────────────────────────────────────

    /**
     * True si existe un camino dirigido desde el nodo con id {@code fromId}
     * hasta el nodo con id {@code toId}.
     *
     * Usado internamente para la verificación de ciclos en addEdge().
     *
     * @param fromId id del nodo de inicio.
     * @param toId   id del nodo destino buscado.
     * @return true si existe camino de fromId a toId.
     */
    public boolean canReach(String fromId, String toId) {
        if (fromId.equals(toId)) return true;
        Set<String> visited = new HashSet<>();
        return dfsCanReach(fromId, toId, visited);
    }

    // ── Implementación interna ────────────────────────────────────────────

    private void insertEdge(GraphEdge<T> edge) {
        String srcId = nodeId.apply(edge.getSource());

        bySource.computeIfAbsent(srcId, k -> new ArrayList<>()).add(edge);

        if (edge.getTarget() != null) {
            String dstId = nodeId.apply(edge.getTarget());
            byTarget.computeIfAbsent(dstId, k -> new ArrayList<>()).add(edge);
        }

        byTag.computeIfAbsent(edge.getTag(), k -> new ArrayList<>()).add(edge);
        allEdges.add(edge);
        dirty = true;
    }

    private boolean dfsCanReach(String current, String target, Set<String> visited) {
        if (visited.contains(current)) return false;
        visited.add(current);

        List<GraphEdge<T>> edges = bySource.get(current);
        if (edges == null) return false;

        for (GraphEdge<T> edge : edges) {
            if (edge.getTarget() == null) continue;
            String nextId = nodeId.apply(edge.getTarget());
            if (nextId.equals(target)) return true;
            if (dfsCanReach(nextId, target, visited)) return true;
        }
        return false;
    }
}
