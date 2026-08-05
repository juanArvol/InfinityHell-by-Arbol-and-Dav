package Game.Gameplay.Core.Dependencies;

import Game.Engine.Physics.World.Graph.DependencyGraph;
import Game.Engine.Physics.World.Graph.GraphEdge;
import Game.Gameplay.Core.Properties.PropertyKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Grafo dirigido de dependencias entre propiedades de gameplay.
 *
 * ── HRFC-016 — Consolidación del modelo emergente ────────────────────────
 *
 * ── CAMBIO RESPECTO A HRFC-015 ────────────────────────────────────────────
 * PropertyDependencyGraph ya no contiene un motor de grafo propio.
 * Toda la lógica de almacenamiento, recorrido y remoción está en
 * {@link DependencyGraph}{@code <PropertyKey<?>>}.
 *
 * PropertyDependencyGraph es ahora una fachada de dominio que:
 *   - Configura el motor genérico con la identidad de nodo correcta (PropertyKey::id).
 *   - Expone la API familiar de alto nivel (addEdge, getDependenciesFrom, etc.).
 *   - Traduce entre {@link PropertyDependency} (payload de arista) y
 *     {@link GraphEdge}{@code <PropertyKey<?>>} (arista del motor).
 *
 * ── MOTOR SUBYACENTE ──────────────────────────────────────────────────────
 * El motor es {@code DependencyGraph<PropertyKey<?>>}.
 *   Nodo:    PropertyKey<?> — identificada por PropertyKey::id
 *   Arista:  GraphEdge<PropertyKey<?>>
 *   Payload: PropertyDependency (accedida via edge.getPayloadAs(PropertyDependency.class))
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
 *   "¿Cuál es el orden de resolución desde A?"
 *
 * NO calcula valores.
 * NO aplica cambios.
 * NO conoce entidades del juego.
 *
 * ── THREAD SAFETY ─────────────────────────────────────────────────────────
 * No es thread-safe. Usar desde el game loop thread.
 *
 * @see PropertyDependency
 * @see DependencyPropagator
 * @see DependencyGraph
 */
public final class PropertyDependencyGraph {

    /**
     * Motor genérico de grafos con nodos PropertyKey<?>.
     * La identidad de nodo es PropertyKey::id (String único por propiedad).
     */
    private final DependencyGraph<PropertyKey<?>> graph =
        new DependencyGraph<>(PropertyKey::id);

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

        GraphEdge<PropertyKey<?>> edge = toGraphEdge(dependency);
        graph.addEdge(edge);  // lanza IllegalStateException si hay ciclo
    }

    /**
     * Añade una dependencia al grafo sin verificación de ciclos.
     *
     * Usar cuando el diseño del juego requiere dependencias cíclicas controladas.
     * El DependencyPropagator cortará los ciclos en tiempo de propagación.
     *
     * @param dependency arista dirigida a añadir
     * @throws IllegalArgumentException si dependency es null
     */
    public void addEdgeUnchecked(PropertyDependency dependency) {
        if (dependency == null)
            throw new IllegalArgumentException("dependency no puede ser null.");
        graph.addEdgeUnchecked(toGraphEdge(dependency));
    }

    /**
     * Añade múltiples dependencias con verificación de ciclos.
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
     *
     * @param tag tag de las dependencias a eliminar
     */
    public void removeByTag(String tag) {
        graph.removeByTag(tag);
    }

    /** Elimina todas las dependencias del grafo. */
    public void clear() {
        graph.clear();
    }

    // ── Consultas de aristas ──────────────────────────────────────────────

    /**
     * Retorna las dependencias que parten de la propiedad indicada,
     * ordenadas por prioridad ascendente.
     *
     * @param sourceKey propiedad origen
     * @return lista ordenada de dependencias salientes (puede estar vacía)
     */
    public List<PropertyDependency> getDependenciesFrom(PropertyKey<?> sourceKey) {
        if (sourceKey == null) return Collections.emptyList();
        List<GraphEdge<PropertyKey<?>>> edges = graph.getEdgesFrom(sourceKey);
        return toDependencies(edges);
    }

    /**
     * Retorna las dependencias que llegan a la propiedad indicada,
     * ordenadas por prioridad ascendente.
     *
     * @param targetKey propiedad destino
     * @return lista ordenada de dependencias entrantes (puede estar vacía)
     */
    public List<PropertyDependency> getDependenciesTo(PropertyKey<?> targetKey) {
        if (targetKey == null) return Collections.emptyList();
        List<GraphEdge<PropertyKey<?>>> edges = graph.getEdgesTo(targetKey);
        return toDependencies(edges);
    }

    /**
     * True si la propiedad origen tiene al menos una dependencia saliente.
     *
     * @param sourceKey propiedad a consultar
     */
    public boolean hasDependenciesFrom(PropertyKey<?> sourceKey) {
        return graph.hasEdgesFrom(sourceKey);
    }

    /**
     * True si la propiedad destino tiene al menos una dependencia entrante.
     *
     * @param targetKey propiedad a consultar
     */
    public boolean hasDependenciesTo(PropertyKey<?> targetKey) {
        return graph.hasEdgesTo(targetKey);
    }

    /**
     * True si existe alguna dependencia con el tag indicado.
     *
     * @param tag tag a buscar
     */
    public boolean hasTag(String tag) {
        return graph.hasTag(tag);
    }

    /** Número total de aristas en el grafo. */
    public int edgeCount() {
        return graph.edgeCount();
    }

    /** True si el grafo no tiene ninguna arista. */
    public boolean isEmpty() {
        return graph.isEmpty();
    }

    // ── Acceso al motor subyacente ────────────────────────────────────────

    /**
     * Acceso directo al motor genérico para consultas avanzadas o
     * integración con otros sistemas del Engine.
     *
     * @return el DependencyGraph subyacente (nunca null).
     */
    public DependencyGraph<PropertyKey<?>> getGraph() {
        return graph;
    }

    // ── Soporte para DependencyPropagator ────────────────────────────────

    /**
     * True si existe un camino dirigido desde {@code fromId} hasta {@code toId}.
     * Usado por addEdge() para verificación de ciclos y por DependencyPropagator
     * para detección de ciclos en propagación.
     *
     * @param fromId id del nodo de inicio
     * @param toId   id del nodo destino buscado
     * @return true si existe camino
     */
    public boolean canReach(String fromId, String toId) {
        return graph.canReach(fromId, toId);
    }

    // ── Conversión PropertyDependency ↔ GraphEdge ─────────────────────────

    private static GraphEdge<PropertyKey<?>> toGraphEdge(PropertyDependency dep) {
        return GraphEdge.of(
            dep.getSourceKey(),
            dep.getTargetKey(),
            dep,
            dep.getTag(),
            dep.getPriority()
        );
    }

    private static List<PropertyDependency> toDependencies(
            List<GraphEdge<PropertyKey<?>>> edges) {
        if (edges.isEmpty()) return Collections.emptyList();
        List<PropertyDependency> result = new ArrayList<>(edges.size());
        for (GraphEdge<PropertyKey<?>> edge : edges) {
            result.add(edge.getPayloadAs(PropertyDependency.class));
        }
        // getEdgesFrom/To already return sorted by priority from DependencyGraph
        return Collections.unmodifiableList(result);
    }
}
