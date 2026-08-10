package Game.Engine.Physics.World.Graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Motor genérico de grafos dirigidos con nodos tipados.
 *
 * ── HRFC-016 — Consolidación del modelo emergente ────────────────────────
 * ── Mini-HRFC correctivo — Identidad de nodo por referencia ─────────────
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
 *   DependencyGraph<PropertyKey<?>>     → dependencias generales entre propiedades
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
 *   bySource → nodo T  → lista de aristas salientes
 *   byTarget → nodo T  → lista de aristas entrantes
 *   byTag    → tag     → lista de aristas con ese tag (para remoción O(1))
 *   allEdges → lista completa ordenada por prioridad (lazy sort)
 *
 * ── DETECCIÓN DE CICLOS ──────────────────────────────────────────────────
 * addEdge() verifica que la arista A → B no cree un ciclo (no exista ya un
 * camino de B → A). Usar addEdgeUnchecked() para ciclos controlados.
 *
 * ── IDENTIDAD DE NODO ────────────────────────────────────────────────────
 * La identidad del nodo se mantiene mediante referencia de objeto Java.
 * Los índices bySource y byTarget usan IdentityHashMap<T, ...> para garantizar
 * que dos nodos con el mismo equals() pero distinta referencia se traten como
 * nodos distintos. El grafo no exige ninguna implementación particular de
 * equals()/hashCode() en T, ni ningún método id()/displayName().
 *
 * El tipo T es el propio nodo; no existe ninguna transformación T → String,
 * T → int ni T → hash para identificar nodos internamente.
 *
 * ── THREAD SAFETY ─────────────────────────────────────────────────────────
 * No es thread-safe. Usar exclusivamente desde el game loop thread.
 *
 * @param <T> tipo de nodo del grafo.
 */
public final class DependencyGraph<T> {

    /**
     * Índice de aristas salientes: referencia del nodo origen → lista de aristas.
     * IdentityHashMap garantiza identidad referencial independientemente de
     * equals()/hashCode() del tipo T.
     */
    private final Map<T, List<GraphEdge<T>>> bySource = new IdentityHashMap<>();

    /**
     * Índice de aristas entrantes: referencia del nodo destino → lista de aristas.
     * IdentityHashMap garantiza identidad referencial independientemente de
     * equals()/hashCode() del tipo T.
     */
    private final Map<T, List<GraphEdge<T>>> byTarget = new IdentityHashMap<>();

    /** Índice por tag para remoción eficiente. El tag es metadata de arista, no identidad de nodo. */
    private final Map<String, List<GraphEdge<T>>> byTag = new HashMap<>();

    /** Lista completa de aristas en orden de inserción / prioridad. */
    private final List<GraphEdge<T>> allEdges = new ArrayList<>();

    /** True si allEdges necesita reordenarse por prioridad. */
    private boolean dirty = false;

    // ── Constructor ───────────────────────────────────────────────────────

    /** Crea un grafo vacío. La identidad de nodo se mantiene por referencia de objeto. */
    public DependencyGraph() {}

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

        T source = edge.getSource();
        T target = edge.getTarget();

        if (target != null && canReach(target, source)) {
            throw new IllegalStateException(
                "Ciclo detectado al añadir '" + source + " → " + target
                + "': ya existe un camino '" + target + " → " + source + "'."
            );
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
            List<GraphEdge<T>> srcList = bySource.get(edge.getSource());
            if (srcList != null) srcList.removeIf(e -> e.getTag().equals(tag));

            if (edge.getTarget() != null) {
                List<GraphEdge<T>> dstList = byTarget.get(edge.getTarget());
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
        List<GraphEdge<T>> edges = bySource.get(node);
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
        List<GraphEdge<T>> edges = byTarget.get(node);
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
        List<GraphEdge<T>> edges = bySource.get(node);
        return edges != null && !edges.isEmpty();
    }

    /**
     * True si el nodo dado tiene al menos una arista entrante.
     *
     * @param node nodo a consultar.
     */
    public boolean hasEdgesTo(T node) {
        if (node == null) return false;
        List<GraphEdge<T>> edges = byTarget.get(node);
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
     * True si existe un camino dirigido desde el nodo {@code from}
     * hasta el nodo {@code to}.
     *
     * La comparación usa identidad referencial (mismo objeto Java),
     * consistente con el uso de IdentityHashMap en los índices.
     *
     * Usado internamente para la verificación de ciclos en addEdge() y
     * expuesto como API pública para consultas de alcanzabilidad.
     *
     * @param from nodo de inicio.
     * @param to   nodo destino buscado.
     * @return true si existe camino de {@code from} a {@code to}.
     */
    public boolean canReach(T from, T to) {
        if (from == null || to == null) return false;
        if (from == to) return true;
        // El conjunto de visitados también usa identidad referencial.
        Set<T> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        return dfsCanReach(from, to, visited);
    }

    // ── Implementación interna ────────────────────────────────────────────

    private void insertEdge(GraphEdge<T> edge) {
        bySource.computeIfAbsent(edge.getSource(), k -> new ArrayList<>()).add(edge);

        if (edge.getTarget() != null) {
            byTarget.computeIfAbsent(edge.getTarget(), k -> new ArrayList<>()).add(edge);
        }

        byTag.computeIfAbsent(edge.getTag(), k -> new ArrayList<>()).add(edge);
        allEdges.add(edge);
        dirty = true;
    }

    private boolean dfsCanReach(T current, T target, Set<T> visited) {
        if (visited.contains(current)) return false;
        visited.add(current);

        List<GraphEdge<T>> edges = bySource.get(current);
        if (edges == null) return false;

        for (GraphEdge<T> edge : edges) {
            T next = edge.getTarget();
            if (next == null) continue;
            if (next == target) return true;
            if (dfsCanReach(next, target, visited)) return true;
        }
        return false;
    }
}
