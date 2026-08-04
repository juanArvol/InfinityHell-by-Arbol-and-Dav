package Game.Engine.Physics.Core;

import Game.Engine.World.Graph.DependencyGraph;
import Game.Engine.World.Graph.GraphEdge;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Grafo de dependencias físicas del universo.
 *
 * ── HRFC-022 — Eliminación del Paradigma de Ley Ejecutable ──────────────────
 *
 * ── RESPONSABILIDAD ──────────────────────────────────────────────────────
 * PropertyDependencyGraph representa el tejido de relaciones físicas del
 * universo. Sus nodos son PhysicalProperty; sus aristas son PhysicalRelation.
 *
 * Cada arista declara:
 *   "La propiedad A influye sobre la propiedad B mediante la ley L."
 *
 * El conjunto de todas esas aristas conforma la topología física del mundo.
 * De esa topología emerge todo el comportamiento — no de reglas escritas
 * para combinaciones concretas de objetos.
 *
 * ── PRINCIPIO FUNDAMENTAL ─────────────────────────────────────────────────
 * Este grafo representa dependencias físicas del universo.
 * No representa dependencias de implementación.
 * No representa relaciones entre entidades.
 * No representa relaciones entre materiales.
 *
 * ── EJEMPLOS DE DEPENDENCIAS REALES ──────────────────────────────────────
 *
 *   temperatura → presión          (expansión volumétrica)
 *   carga eléctrica → temperatura  (efecto Joule)
 *   temperatura → conductividad    (resistencia eléctrica térmica)
 *   conductividad → corriente      (ley de Ohm)
 *   corriente → temperatura        (Joule, ciclo cerrado vía addEdgeUnchecked)
 *
 * El grafo soporta ciclos físicos controlados mediante addRelationUnchecked().
 * Un ciclo en el grafo no es un error — es electricidad generando calor que
 * modifica la conductividad que modifica la corriente que genera más calor.
 *
 * ── PROPAGACIÓN ───────────────────────────────────────────────────────────
 * propagationOrderFrom(root) retorna el orden topológico de evaluación a
 * partir de un nodo raíz dado. El PropertyResolver usa este orden para
 * determinar qué propiedades recalcular y en qué secuencia.
 *
 * ── CICLOS FÍSICOS ────────────────────────────────────────────────────────
 * Los ciclos físicos reales (como corriente → calor → conductividad → corriente)
 * se registran con addRelationUnchecked(). El PropertyResolver detecta los
 * ciclos y los gestiona mediante iteración convergente en lugar de propagación
 * directa, limitando las iteraciones para garantizar terminación.
 *
 * ── EXTENSIBILIDAD ────────────────────────────────────────────────────────
 * Añadir un fenómeno nuevo (magnetismo, radiación, plasma):
 *
 *   1. Crear PropertyDescriptor para la nueva propiedad.
 *   2. Crear PhysicalProperty.of(descriptor) para el nuevo nodo.
 *   3. addRelation(propExistente, propNueva, PhysicalRelation.of(ley)).
 *
 * Ningún archivo existente se modifica. El sistema comienza a interactuar
 * con la nueva propiedad automáticamente.
 *
 * ── THREAD SAFETY ─────────────────────────────────────────────────────────
 * No es thread-safe. Usar exclusivamente desde el game loop thread.
 */
public final class PropertyDependencyGraph {

    /**
     * Motor de grafo subyacente.
     * Nodo: PhysicalProperty. Payload: PhysicalRelation.
     * nodeId extrae el id del descriptor del nodo.
     */
    private final DependencyGraph<PhysicalProperty> graph =
        new DependencyGraph<>(PhysicalProperty::getId);

    // ── Mutación ──────────────────────────────────────────────────────────

    /**
     * Registra una dependencia física entre dos propiedades con verificación
     * de ciclos. Si la arista A → B crearía un ciclo (existe B → A),
     * lanza IllegalStateException.
     *
     * Para ciclos físicos controlados (corriente → calor → corriente),
     * usar {@link #addRelationUnchecked}.
     *
     * @param source   propiedad de origen (la que influye). No puede ser null.
     * @param target   propiedad de destino (la influenciada). No puede ser null.
     * @param relation semántica de la relación. No puede ser null.
     * @param tag      identificador de grupo para remoción eficiente. No puede
     *                 ser null ni vacío.
     * @throws IllegalStateException si la arista crearía un ciclo estructural.
     */
    public void addRelation(PhysicalProperty source,
                            PhysicalProperty target,
                            PhysicalRelation  relation,
                            String            tag) {
        graph.addEdge(buildEdge(source, target, relation, tag, 100));
    }

    /**
     * Registra una dependencia física con prioridad explícita.
     *
     * @param source   propiedad de origen.
     * @param target   propiedad de destino.
     * @param relation semántica de la relación.
     * @param tag      identificador de grupo.
     * @param priority prioridad de evaluación (menor = antes).
     */
    public void addRelation(PhysicalProperty source,
                            PhysicalProperty target,
                            PhysicalRelation  relation,
                            String            tag,
                            int               priority) {
        graph.addEdge(buildEdge(source, target, relation, tag, priority));
    }

    /**
     * Registra una dependencia física sin verificación de ciclos.
     *
     * Usar para relaciones que forman ciclos físicos reales:
     *   temperatura → conductividad → corriente → temperatura
     *
     * El PropertyResolver gestiona estos ciclos mediante iteración convergente.
     *
     * @param source   propiedad de origen.
     * @param target   propiedad de destino.
     * @param relation semántica de la relación.
     * @param tag      identificador de grupo.
     */
    public void addRelationUnchecked(PhysicalProperty source,
                                     PhysicalProperty target,
                                     PhysicalRelation  relation,
                                     String            tag) {
        graph.addEdgeUnchecked(buildEdge(source, target, relation, tag, 100));
    }

    /**
     * Elimina todas las dependencias con el tag dado.
     * Útil para desactivar grupos de relaciones en runtime (p.ej. al eliminar
     * un módulo de física de un mundo concreto).
     *
     * @param tag tag de las relaciones a eliminar.
     */
    public void removeByTag(String tag) {
        graph.removeByTag(tag);
    }

    /** Elimina todas las dependencias del grafo. */
    public void clear() {
        graph.clear();
    }

    // ── Consultas ─────────────────────────────────────────────────────────

    /**
     * Lista de relaciones físicas que parten de la propiedad dada, ordenadas
     * por prioridad. Representa "qué propiedades influye esta propiedad."
     *
     * @param property propiedad de origen.
     * @return lista inmutable de aristas salientes (vacía si no hay).
     */
    public List<GraphEdge<PhysicalProperty>> relationsFrom(PhysicalProperty property) {
        return graph.getEdgesFrom(property);
    }

    /**
     * Lista de relaciones físicas que apuntan a la propiedad dada, ordenadas
     * por prioridad. Representa "qué propiedades influyen sobre esta propiedad."
     *
     * @param property propiedad de destino.
     * @return lista inmutable de aristas entrantes (vacía si no hay).
     */
    public List<GraphEdge<PhysicalProperty>> relationsTo(PhysicalProperty property) {
        return graph.getEdgesTo(property);
    }

    /**
     * Todas las relaciones del grafo ordenadas por prioridad.
     *
     * @return lista inmutable de todas las aristas.
     */
    public List<GraphEdge<PhysicalProperty>> allRelations() {
        return graph.allEdges();
    }

    /**
     * True si la propiedad dada influye sobre alguna otra (tiene aristas
     * salientes).
     *
     * @param property propiedad a consultar.
     * @return true si tiene dependientes.
     */
    public boolean hasDownstream(PhysicalProperty property) {
        return graph.hasEdgesFrom(property);
    }

    /**
     * True si alguna otra propiedad influye sobre la propiedad dada (tiene
     * aristas entrantes).
     *
     * @param property propiedad a consultar.
     * @return true si tiene dependencias.
     */
    public boolean hasUpstream(PhysicalProperty property) {
        return graph.hasEdgesTo(property);
    }

    /** Número total de dependencias registradas. */
    public int size() { return graph.edgeCount(); }

    /** True si no hay dependencias registradas. */
    public boolean isEmpty() { return graph.isEmpty(); }

    // ── Propagación ───────────────────────────────────────────────────────

    /**
     * Orden de propagación a partir de una propiedad raíz.
     *
     * Retorna la secuencia de propiedades que deben recalcularse cuando la
     * propiedad raíz cambia, en orden topológico (BFS por prioridad).
     *
     * Las propiedades que forman ciclos con la raíz quedan al final de la lista
     * con un marcador de ciclo (isCyclic = true en la entrada correspondiente
     * del propagation path). El PropertyResolver las gestiona por iteración.
     *
     * @param root propiedad cuyo cambio inicia la propagación.
     * @return lista de PropagationStep en orden de evaluación. Nunca null.
     */
    public List<PropagationStep> propagationOrderFrom(PhysicalProperty root) {
        if (root == null || !graph.hasEdgesFrom(root))
            return Collections.emptyList();

        List<PropagationStep> result  = new ArrayList<>();
        Set<String>           visited = new LinkedHashSet<>();
        Set<String>           cyclic  = new LinkedHashSet<>();

        visited.add(root.getId());
        collectBFS(root, visited, cyclic, result);
        return Collections.unmodifiableList(result);
    }

    // ── Implementación interna ────────────────────────────────────────────

    private void collectBFS(PhysicalProperty           node,
                            Set<String>                visited,
                            Set<String>                cyclic,
                            List<PropagationStep>      result) {
        List<GraphEdge<PhysicalProperty>> edges = graph.getEdgesFrom(node);
        for (GraphEdge<PhysicalProperty> edge : edges) {
            if (!edge.hasTarget()) continue;
            PhysicalProperty target   = edge.getTarget();
            PhysicalRelation relation = edge.getPayloadAs(PhysicalRelation.class);
            String           targetId = target.getId();

            if (visited.contains(targetId)) {
                // ciclo detectado — marcar pero no volver a expandir
                if (!cyclic.contains(targetId)) {
                    cyclic.add(targetId);
                    result.add(new PropagationStep(target, relation, true));
                }
            } else {
                visited.add(targetId);
                result.add(new PropagationStep(target, relation, false));
                collectBFS(target, visited, cyclic, result);
            }
        }
    }

    private static GraphEdge<PhysicalProperty> buildEdge(
            PhysicalProperty source,
            PhysicalProperty target,
            PhysicalRelation relation,
            String tag,
            int priority) {
        return GraphEdge.<PhysicalProperty>builder()
            .source(source)
            .target(target)
            .payload(relation)
            .tag(tag)
            .priority(priority)
            .build();
    }

    // ─────────────────────────────────────────────────────────────────────
    // PropagationStep — entrada del orden de propagación
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Un paso en el orden de propagación de dependencias.
     *
     * Contiene la propiedad que debe recalcularse, la relación física que
     * activa ese recálculo, y si ese paso forma parte de un ciclo.
     *
     * El PropertyResolver usa esta información para:
     *   - propiedades no cíclicas: recalcular una vez en orden.
     *   - propiedades cíclicas: recalcular iterativamente hasta convergencia.
     */
    public static final class PropagationStep {

        /** Propiedad que debe recalcularse en este paso. */
        private final PhysicalProperty property;

        /** Relación física que produce el cambio en esta propiedad. */
        private final PhysicalRelation relation;

        /**
         * True si este paso pertenece a un ciclo físico.
         * El PropertyResolver lo gestiona por iteración convergente.
         */
        private final boolean cyclic;

        PropagationStep(PhysicalProperty property,
                        PhysicalRelation relation,
                        boolean          cyclic) {
            this.property = property;
            this.relation = relation;
            this.cyclic   = cyclic;
        }

        /** Propiedad a recalcular. */
        public PhysicalProperty getProperty()  { return property; }

        /** Relación que activa el recálculo. */
        public PhysicalRelation getRelation()  { return relation; }

        /** True si este paso pertenece a un ciclo físico. */
        public boolean isCyclic()              { return cyclic; }

        @Override
        public String toString() {
            return "PropagationStep[" + property.getId()
                + (cyclic ? " (cyclic)" : "") + "]";
        }
    }
}
