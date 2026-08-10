package Game.Engine.Entity.Properties.Resolution;

import Game.Engine.Entity.Properties.Dependencies.DependencyPropagator;
import Game.Engine.Entity.Properties.Dependencies.PropagationResult;
import Game.Engine.Entity.Properties.Dependencies.PropertyDependencyGraph;
import Game.Engine.Entity.Properties.Modifier.Causality.CausalNode;
import Game.Engine.Entity.Properties.Modifier.Causality.InfluenceRegistry;
import Game.Engine.Entity.Properties.Modifier.Causality.ModifierContext;
import Game.Engine.Entity.Properties.Modifier.PropertyModifierComponent;
import Game.Engine.Entity.Properties.Operations.OperationContext;
import Game.Engine.Entity.Properties.Operations.OperationRegistry;
import Game.Engine.Entity.Properties.PropertyComponent;
import Game.Engine.Entity.Properties.PropertyKey;
import Game.Engine.Entity.Properties.PropertyMap;
import Game.Engine.GameObjects;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Pipeline unificado de resolución, propagación y operaciones para una entidad.
 *
 * ── RESPONSABILIDAD ÚNICA ─────────────────────────────────────────────────
 * ResolutionPipeline orquesta el flujo completo:
 *
 *   Resolver propiedad
 *       ↓
 *   Propagar dependencias
 *       ↓
 *   Generar nuevos valores en PropertyMap
 *       ↓
 *   Ejecutar operaciones asociadas
 *       ↓
 *   Actualizar CausalNode
 *
 * ── QUÉ NO HACE ───────────────────────────────────────────────────────────
 * ResolutionPipeline NO modifica PropertyResolver.
 * PropertyResolver sigue siendo stateless y sin conocimiento de operaciones
 * ni dependencias. ResolutionPipeline lo llama como un paso dentro de un
 * flujo más amplio.
 *
 * ── DESACOPLAMIENTO ──────────────────────────────────────────────────────
 * ResolutionPipeline no conoce Player, Enemy, Bullet, ni ninguna clase
 * concreta de gameplay. Trabaja exclusivamente mediante GameObjects,
 * PropertyKey, PropertyDependencyGraph, OperationRegistry e InfluenceRegistry.
 *
 * ── CONSTRUCCIÓN MEDIANTE BUILDER ────────────────────────────────────────
 *   ResolutionPipeline pipeline = ResolutionPipeline.builder()
 *       .dependencyGraph(sharedGraph)
 *       .operationRegistry(entityRegistry)
 *       .influenceRegistry(playerRegistry)
 *       .applyPropagatedDeltas(true)
 *       .collectCausalNodes(true)
 *       .build();
 *
 * ── THREAD SAFETY ────────────────────────────────────────────────────────
 * No es thread-safe. Usar desde el game loop thread.
 *
 * @see PropertyResolver
 * @see DependencyPropagator
 * @see OperationRegistry
 */
public final class ResolutionPipeline {

    // ── Configuración ─────────────────────────────────────────────────────

    private final PropertyDependencyGraph dependencyGraph;
    private final DependencyPropagator    propagator;
    private final OperationRegistry       operationRegistry;
    private final InfluenceRegistry       influenceRegistry;
    private final boolean                 applyPropagatedDeltas;
    private final boolean                 collectCausalNodes;

    // ── Constructor privado ───────────────────────────────────────────────

    private ResolutionPipeline(Builder b) {
        this.dependencyGraph       = b.dependencyGraph;
        this.propagator            = (b.dependencyGraph != null)
            ? new DependencyPropagator(b.dependencyGraph)
            : null;
        this.operationRegistry     = b.operationRegistry;
        this.influenceRegistry     = b.influenceRegistry;
        this.applyPropagatedDeltas = b.applyPropagatedDeltas;
        this.collectCausalNodes    = b.collectCausalNodes;
    }

    // ── Factory ───────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    /** Pipeline mínimo sin dependencias ni operaciones. */
    public static ResolutionPipeline minimal() { return builder().build(); }

    // ── API pública: resolución completa ─────────────────────────────────

    /**
     * Ejecuta el pipeline completo para la propiedad indicada sobre la entidad dada.
     */
    public PipelineResult resolve(GameObjects entity, PropertyKey<?> key, long timestamp) {
        return resolve(entity, key, null, timestamp);
    }

    /**
     * Versión con ModifierContext explícito.
     */
    public PipelineResult resolve(
            GameObjects     entity,
            PropertyKey<?>  key,
            ModifierContext externalContext,
            long            timestamp) {

        if (entity == null || key == null) return PipelineResult.empty(key);

        // ── Paso 1: extraer componentes ───────────────────────────────────
        PropertyComponent pc = entity.getComponent(PropertyComponent.class);
        PropertyModifierComponent mc = entity.getComponent(PropertyModifierComponent.class);

        PropertyMap propertyMap = (pc != null) ? pc.getMap() : new PropertyMap();
        double previousValue = propertyMap.getBase(key);

        // ── Paso 2: resolver propiedad ────────────────────────────────────
        List<CausalNode> causalLog = collectCausalNodes ? new ArrayList<>() : null;

        double finalValue;
        if (mc != null) {
            finalValue = PropertyResolver.resolveWithCausalLog(
                propertyMap, key, mc.getContainer(),
                externalContext, influenceRegistry, causalLog
            );
        } else {
            finalValue = previousValue;
        }

        // ── Paso 3: propagar dependencias ─────────────────────────────────
        PropagationResult propagation = PropagationResult.empty();
        if (propagator != null && propagator.getGraph().hasDependenciesFrom(key)) {
            propagation = propagator.propagate(key, previousValue, finalValue);
        }

        // ── Paso 4: aplicar deltas propagados al PropertyMap ─────────────
        if (applyPropagatedDeltas && propagation.hasChanges() && pc != null) {
            applyPropagatedDeltas(propagation, propertyMap, key);
        }

        // ── Paso 5: ejecutar operaciones ──────────────────────────────────
        if (operationRegistry != null && operationRegistry.hasOperations()) {
            OperationContext opCtx = buildOperationContext(
                entity, key, previousValue, finalValue,
                externalContext, causalLog, timestamp
            );
            operationRegistry.execute(opCtx);
        }

        // ── Paso 6: retornar resultado ────────────────────────────────────
        return new PipelineResult(
            key, previousValue, finalValue,
            propagation,
            causalLog != null ? causalLog : List.of(),
            timestamp
        );
    }

    /**
     * Versión simplificada: resuelve la propiedad a través de un ResolutionContext
     * ya construido. Solo ejecuta resolución y operaciones; no propaga dependencias.
     */
    public PipelineResult resolveFromContext(
            ResolutionContext resolutionContext,
            PropertyKey<?>    key,
            ModifierContext   externalContext,
            long              timestamp) {

        if (resolutionContext == null || key == null) return PipelineResult.empty(key);

        double previousValue = resolutionContext.getMap().getBase(key);
        List<CausalNode> causalLog = collectCausalNodes ? new ArrayList<>() : null;

        double finalValue = PropertyResolver.resolveWithCausalLog(
            resolutionContext.getMap(), key, resolutionContext.getContainer(),
            externalContext, influenceRegistry, causalLog
        );

        if (operationRegistry != null && operationRegistry.hasOperations()) {
            OperationContext opCtx = OperationContext.builder()
                .affectedProperty(key)
                .previousValue(previousValue)
                .finalValue(finalValue)
                .modifierContext(externalContext)
                .resolutionContext(resolutionContext)
                .timestamp(timestamp)
                .build();
            operationRegistry.execute(opCtx);
        }

        return new PipelineResult(
            key, previousValue, finalValue,
            PropagationResult.empty(),
            causalLog != null ? causalLog : List.of(),
            timestamp
        );
    }

    // ── Implementación interna ────────────────────────────────────────────

    private static void applyPropagatedDeltas(
            PropagationResult propagation,
            PropertyMap       propertyMap,
            PropertyKey<?>    sourceKey) {

        for (Map.Entry<PropertyKey<?>, Double> entry : propagation.allDeltas().entrySet()) {
            PropertyKey<?> targetKey = entry.getKey();
            double delta             = entry.getValue();

            if (Math.abs(delta) > 1e-12) {
                double currentBase = propertyMap.getBase(targetKey);
                propertyMap.setBase(targetKey, currentBase + delta);
            }
        }
    }

    private static OperationContext buildOperationContext(
            GameObjects      entity,
            PropertyKey<?>   key,
            double           previousValue,
            double           finalValue,
            ModifierContext  modifierContext,
            List<CausalNode> causalLog,
            long             timestamp) {

        CausalNode representativeNode = (causalLog != null && !causalLog.isEmpty())
            ? causalLog.get(0) : null;

        return OperationContext.builder()
            .target(entity)
            .affectedProperty(key)
            .previousValue(previousValue)
            .finalValue(finalValue)
            .modifierContext(modifierContext)
            .causalNode(representativeNode)
            .timestamp(timestamp)
            .build();
    }

    // ── Acceso a la configuración ─────────────────────────────────────────

    public PropertyDependencyGraph getDependencyGraph()   { return dependencyGraph; }
    public OperationRegistry getOperationRegistry()       { return operationRegistry; }
    public InfluenceRegistry getInfluenceRegistry()       { return influenceRegistry; }
    public boolean isApplyPropagatedDeltas()              { return applyPropagatedDeltas; }
    public boolean isCollectCausalNodes()                 { return collectCausalNodes; }

    @Override
    public String toString() {
        return "ResolutionPipeline["
            + "graph=" + (dependencyGraph != null ? dependencyGraph.edgeCount() + " edges" : "none")
            + ", ops=" + (operationRegistry != null ? operationRegistry.size() : 0)
            + ", applyDeltas=" + applyPropagatedDeltas
            + "]";
    }

    // ═════════════════════════════════════════════════════════════════════
    // PipelineResult
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Resultado inmutable de una ejecución completa del pipeline.
     */
    public static final class PipelineResult {

        private final PropertyKey<?>      resolvedKey;
        private final double              previousValue;
        private final double              finalValue;
        private final PropagationResult   propagation;
        private final List<CausalNode>    causalNodes;
        private final long                timestamp;

        PipelineResult(
                PropertyKey<?>   resolvedKey,
                double           previousValue,
                double           finalValue,
                PropagationResult propagation,
                List<CausalNode> causalNodes,
                long             timestamp) {
            this.resolvedKey   = resolvedKey;
            this.previousValue = previousValue;
            this.finalValue    = finalValue;
            this.propagation   = propagation;
            this.causalNodes   = causalNodes;
            this.timestamp     = timestamp;
        }

        public PropertyKey<?> getResolvedKey()       { return resolvedKey; }
        public double getPreviousValue()             { return previousValue; }
        public double getFinalValue()                { return finalValue; }
        public double getDelta()                     { return finalValue - previousValue; }
        public PropagationResult getPropagation()    { return propagation; }
        public List<CausalNode> getCausalNodes()     { return causalNodes; }
        public long getTimestamp()                   { return timestamp; }
        public boolean valueChanged()                { return finalValue != previousValue; }
        public boolean hadPropagation()              { return propagation.hasChanges(); }
        public boolean hadCycles()                   { return propagation.hadCycles(); }

        @Override
        public String toString() {
            return "PipelineResult["
                + "key=" + (resolvedKey != null ? resolvedKey.displayName() : "null")
                + ", prev=" + previousValue
                + ", final=" + finalValue
                + ", propagated=" + propagation.allDeltas().size()
                + ", t=" + timestamp
                + "]";
        }

        static PipelineResult empty(PropertyKey<?> key) {
            return new PipelineResult(key, 0.0, 0.0,
                PropagationResult.empty(), List.of(), 0L);
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // Builder
    // ═════════════════════════════════════════════════════════════════════

    public static final class Builder {

        private PropertyDependencyGraph dependencyGraph;
        private OperationRegistry       operationRegistry;
        private InfluenceRegistry       influenceRegistry;
        private boolean                 applyPropagatedDeltas = true;
        private boolean                 collectCausalNodes    = false;

        private Builder() {}

        public Builder dependencyGraph(PropertyDependencyGraph graph) {
            this.dependencyGraph = graph; return this;
        }

        public Builder operationRegistry(OperationRegistry registry) {
            this.operationRegistry = registry; return this;
        }

        public Builder influenceRegistry(InfluenceRegistry registry) {
            this.influenceRegistry = registry; return this;
        }

        public Builder applyPropagatedDeltas(boolean apply) {
            this.applyPropagatedDeltas = apply; return this;
        }

        public Builder collectCausalNodes(boolean collect) {
            this.collectCausalNodes = collect; return this;
        }

        public ResolutionPipeline build() { return new ResolutionPipeline(this); }
    }
}
