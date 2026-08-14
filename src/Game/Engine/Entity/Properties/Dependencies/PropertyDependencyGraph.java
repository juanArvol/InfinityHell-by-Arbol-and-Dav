package Game.Engine.Entity.Properties.Dependencies;

import Game.Engine.Entity.Properties.PropertyKey;
import Game.Engine.Physics.SimulaticWorld.Graph.DependencyGraph;
import Game.Engine.Physics.SimulaticWorld.Graph.GraphEdge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Grafo dirigido de dependencias entre propiedades.
 *
 * ── Mini-HRFC correctivo — Identidad de nodo por referencia ─────────────
 *
 * PropertyDependencyGraph es una fachada de dominio sobre
 * {@link DependencyGraph}{@code <PropertyKey<?>>}.
 *
 * Toda la lógica de almacenamiento, recorrido y remoción está en el motor
 * genérico. Esta fachada:
 *   - Delega en DependencyGraph<PropertyKey<?>> sin aportar ninguna estrategia
 *     de identidad propia: el motor ya garantiza identidad referencial mediante
 *     IdentityHashMap<T, ...> internamente.
 *   - Expone la API familiar de alto nivel.
 *   - Traduce entre {@link PropertyDependency} y {@link GraphEdge}.
 *
 * ── IDENTIDAD DE NODO ────────────────────────────────────────────────────
 * La identidad de PropertyKey<?> es por referencia de objeto, coherente con
 * PropertyMap y PropertyModifierContainer. No existe ninguna transformación
 * PropertyKey → String, PropertyKey → int ni PropertyKey → hash.
 * El motor gestiona la identidad directamente sobre las referencias T.
 *
 * ── RESPONSABILIDAD ÚNICA ─────────────────────────────────────────────────
 * Almacena y organiza las relaciones "La propiedad A influye sobre la propiedad B".
 *
 * NO calcula valores. NO aplica cambios. NO conoce entidades del juego.
 *
 * @see PropertyDependency
 * @see DependencyPropagator
 */
public final class PropertyDependencyGraph {

    private final DependencyGraph<PropertyKey<?>> graph = new DependencyGraph<>();

    // ── Mutación ──────────────────────────────────────────────────────────

    /**
     * Añade una dependencia al grafo con verificación de ciclos.
     *
     * @throws IllegalStateException si la arista crearía un ciclo estructural.
     */
    public void addEdge(PropertyDependency dependency) {
        if (dependency == null)
            throw new IllegalArgumentException("dependency no puede ser null.");
        graph.addEdge(toGraphEdge(dependency));
    }

    /**
     * Añade una dependencia al grafo sin verificación de ciclos.
     * Usar cuando el diseño requiere dependencias cíclicas controladas.
     */
    public void addEdgeUnchecked(PropertyDependency dependency) {
        if (dependency == null)
            throw new IllegalArgumentException("dependency no puede ser null.");
        graph.addEdgeUnchecked(toGraphEdge(dependency));
    }

    public void addEdges(PropertyDependency... dependencies) {
        if (dependencies == null) return;
        for (PropertyDependency dep : dependencies) addEdge(dep);
    }

    public void removeByTag(String tag) { graph.removeByTag(tag); }
    public void clear()                 { graph.clear(); }

    // ── Consultas de aristas ──────────────────────────────────────────────

    public List<PropertyDependency> getDependenciesFrom(PropertyKey<?> sourceKey) {
        if (sourceKey == null) return Collections.emptyList();
        return toDependencies(graph.getEdgesFrom(sourceKey));
    }

    public List<PropertyDependency> getDependenciesTo(PropertyKey<?> targetKey) {
        if (targetKey == null) return Collections.emptyList();
        return toDependencies(graph.getEdgesTo(targetKey));
    }

    public boolean hasDependenciesFrom(PropertyKey<?> sourceKey) { return graph.hasEdgesFrom(sourceKey); }
    public boolean hasDependenciesTo(PropertyKey<?> targetKey)   { return graph.hasEdgesTo(targetKey); }
    public boolean hasTag(String tag)                            { return graph.hasTag(tag); }
    public int edgeCount()                                       { return graph.edgeCount(); }
    public boolean isEmpty()                                     { return graph.isEmpty(); }

    public DependencyGraph<PropertyKey<?>> getGraph() { return graph; }

    /**
     * True si existe un camino dirigido desde la clave origen hasta la clave destino.
     * La comparación usa identidad referencial de objeto, delegada al motor.
     */
    public boolean canReach(PropertyKey<?> from, PropertyKey<?> to) {
        if (from == null || to == null) return false;
        return graph.canReach(from, to);
    }

    // ── Conversión ────────────────────────────────────────────────────────

    private static GraphEdge<PropertyKey<?>> toGraphEdge(PropertyDependency dep) {
        return GraphEdge.of(
            dep.getSourceKey(), dep.getTargetKey(),
            dep, dep.getTag(), dep.getPriority()
        );
    }

    private static List<PropertyDependency> toDependencies(
            List<GraphEdge<PropertyKey<?>>> edges) {
        if (edges.isEmpty()) return Collections.emptyList();
        List<PropertyDependency> result = new ArrayList<>(edges.size());
        for (GraphEdge<PropertyKey<?>> edge : edges) {
            result.add(edge.getPayloadAs(PropertyDependency.class));
        }
        return Collections.unmodifiableList(result);
    }
}
