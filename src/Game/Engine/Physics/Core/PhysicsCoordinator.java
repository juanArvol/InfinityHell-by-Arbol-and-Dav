package Game.Engine.Physics.Core;

import Game.Engine.GameObjects;
import java.util.List;

/**
 * Coordinador del Physics Core. Orquesta PropertyResolver y RelationResolver.
 *
 * ── HRFC-022 — Eliminación del Paradigma de Ley Ejecutable ───────────────
 *
 * ── RESPONSABILIDAD ──────────────────────────────────────────────────────
 * PhysicsCoordinator es el único punto de entrada del ciclo de simulación
 * física del modelo Property-Driven.
 *
 * Su única responsabilidad es coordinar, en orden garantizado, los dos
 * resolvers que producen el estado físico de cada frame:
 *
 *   [1] PropertyResolver — determina qué propiedades recalcular y en qué
 *                          orden, consultando el PropertyDependencyGraph.
 *   [2] RelationResolver — evalúa las relaciones físicas indicadas por el
 *                          plan, vía evaluadores especializados.
 *
 * PhysicsCoordinator NO resuelve propiedades.
 * PhysicsCoordinator NO aplica relaciones directamente.
 * PhysicsCoordinator NO conoce fenómenos físicos.
 * PhysicsCoordinator NO conoce materiales.
 * PhysicsCoordinator NO conoce entidades concretas.
 *
 * ── FLUJO DEFINITIVO (HRFC-022) ───────────────────────────────────────────
 *
 *   PropertyDependencyGraph (relaciones físicas del universo)
 *       ↓
 *   PropertyResolver.resolve() → ResolutionPlan
 *       ↓
 *   RelationResolver.evaluate(plan, objects, deltaTime)
 *       ↓
 *   PhysicalState actualizado — única fuente de verdad
 *       ↓
 *   Gameplay observa el estado resultante
 *
 * ── PRIMER FRAME ──────────────────────────────────────────────────────────
 * En el primer frame (o tras un reset), PhysicsCoordinator llama a
 * evaluateAll() para asegurar que el estado inicial es coherente con
 * todas las relaciones registradas.
 *
 * ── FALLBACK A EVALUACIÓN COMPLETA ────────────────────────────────────────
 * Si el grafo de dependencias está vacío, PhysicsCoordinator cae al modo de
 * evaluación completa (todas las relaciones, todos los frames).
 *
 * ── COMPOSICIÓN ──────────────────────────────────────────────────────────
 *
 *   // Coordinator vacío (sin física)
 *   PhysicsCoordinator coordinator = PhysicsCoordinator.empty();
 *
 *   // Coordinator con relaciones del catálogo base
 *   PhysicsCoordinator coordinator = PhysicsCoordinator.builder()
 *       .registerAll(new RelationRegistry())
 *       .build();
 *
 * ── EXTENSIBILIDAD ────────────────────────────────────────────────────────
 * Añadir un fenómeno nuevo:
 *
 *   1. Crear PropertyDescriptor para la nueva propiedad.
 *   2. Crear PhysicalProperty.of(descriptor) para el nodo de grafo.
 *   3. Registrar la dependencia en el grafo via coordinator.graph().
 *   4. Registrar la relación via coordinator.register(relation).
 *
 *   PhysicsCoordinator no cambia. PropertyResolver no cambia.
 *   RelationResolver no cambia.
 *
 * ── THREAD SAFETY ─────────────────────────────────────────────────────────
 * No es thread-safe. Usar exclusivamente desde el game loop thread.
 */
public final class PhysicsCoordinator {

    /** Grafo de dependencias físicas del universo. */
    private final PropertyDependencyGraph graph;

    /** Resolver que determina qué propiedades recalcular. */
    private final PropertyResolver propertyResolver;

    /** Resolver que evalúa las relaciones físicas. */
    private final RelationResolver relationResolver;

    /**
     * True hasta que se ejecuta el primer frame.
     * En el primer frame se fuerza evaluación completa para coherencia inicial.
     */
    private boolean firstFrame = true;

    // ── Constructor privado — usar Builder ────────────────────────────────

    private PhysicsCoordinator(Builder b) {
        this.graph            = b.graph;
        this.propertyResolver = new PropertyResolver(b.graph);
        this.relationResolver = new RelationResolver(b.registry);
    }

    // ── Factories ─────────────────────────────────────────────────────────

    /** Punto de entrada del Builder. */
    public static Builder builder() { return new Builder(); }

    /**
     * PhysicsCoordinator vacío — sin relaciones ni dependencias.
     * Útil para mundos sin simulación física activa.
     */
    public static PhysicsCoordinator empty() {
        return builder().build();
    }

    // ── Ciclo de simulación ───────────────────────────────────────────────

    /**
     * Ejecuta el ciclo completo de simulación física para este frame.
     *
     * Orden garantizado e invariante:
     *   1. PropertyResolver.resolve() → ResolutionPlan
     *   2. RelationResolver.evaluate(plan, objects, deltaTime)
     *   3. PropertyResolver.clearChanges()
     *
     * Si el grafo está vacío o es el primer frame, cae al modo de evaluación
     * completa (RelationResolver.evaluateAll).
     *
     * @param objects   lista de objetos activos en el mundo este frame.
     * @param deltaTime tiempo transcurrido desde el último frame, en segundos.
     */
    public void simulate(List<GameObjects> objects, double deltaTime) {
        if (objects == null || objects.isEmpty()) return;
        if (relationResolver.isEmpty()) return;

        if (firstFrame || graph.isEmpty()) {
            relationResolver.evaluateAll(objects, deltaTime);
            firstFrame = false;
        } else {
            PropertyResolver.ResolutionPlan plan = propertyResolver.resolve();
            if (plan.isEmpty()) return;
            relationResolver.evaluate(plan, objects, deltaTime);
        }

        propertyResolver.clearChanges();
    }

    // ── Acceso a subsistemas ──────────────────────────────────────────────

    /**
     * El grafo de dependencias físicas del universo.
     *
     * @return el PropertyDependencyGraph de este coordinator.
     */
    public PropertyDependencyGraph graph() {
        return graph;
    }

    /**
     * El PropertyResolver de este coordinator.
     *
     * Usar para marcar propiedades cambiadas externamente:
     *   coordinator.propertyResolver().markChanged(temperatureProperty)
     *
     * @return el PropertyResolver de este coordinator.
     */
    public PropertyResolver propertyResolver() {
        return propertyResolver;
    }

    /**
     * El RelationResolver de este coordinator.
     *
     * Usar para registrar relaciones adicionales en runtime:
     *   coordinator.relationResolver().register(gravityRelation)
     *
     * @return el RelationResolver de este coordinator.
     */
    public RelationResolver relationResolver() {
        return relationResolver;
    }

    /**
     * Registra una PhysicalRelation directamente en el RelationResolver.
     * Shortcut para el patrón más común de extensión en runtime.
     *
     * @param relation la relación a registrar. Ignorada si null.
     * @return this (para encadenado).
     */
    public PhysicsCoordinator register(PhysicalRelation relation) {
        relationResolver.register(relation);
        return this;
    }

    /**
     * Registra todas las relaciones de un RelationRegistry.
     *
     * @param registry el registro. Ignorado si null.
     * @return this (para encadenado).
     */
    public PhysicsCoordinator registerAll(RelationRegistry registry) {
        relationResolver.registerAll(registry);
        return this;
    }

    /**
     * Fuerza evaluación completa en el próximo frame.
     * Usar cuando se cambia el estado del mundo drásticamente.
     */
    public void forceFullEvaluation() {
        firstFrame = true;
        propertyResolver.clearChanges();
    }

    // ═════════════════════════════════════════════════════════════════════
    // Builder
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Builder de PhysicsCoordinator.
     */
    public static final class Builder {

        private PropertyDependencyGraph graph    = new PropertyDependencyGraph();
        private RelationRegistry        registry = new RelationRegistry();

        private Builder() {}

        /**
         * Establece el grafo de dependencias físicas del universo.
         *
         * @param graph el grafo de dependencias. Ignorado si null.
         * @return this.
         */
        public Builder graph(PropertyDependencyGraph graph) {
            if (graph != null) this.graph = graph;
            return this;
        }

        /**
         * Registra una PhysicalRelation.
         *
         * @param relation la relación. Ignorada si null.
         * @return this.
         */
        public Builder register(PhysicalRelation relation) {
            registry.register(relation);
            return this;
        }

        /**
         * Registra todas las relaciones de un RelationRegistry.
         *
         * @param other el registro. Ignorado si null.
         * @return this.
         */
        public Builder registerAll(RelationRegistry other) {
            if (other != null) registry.registerAll(other);
            return this;
        }

        /** Construye el PhysicsCoordinator con la configuración acumulada. */
        public PhysicsCoordinator build() {
            return new PhysicsCoordinator(this);
        }
    }
}
