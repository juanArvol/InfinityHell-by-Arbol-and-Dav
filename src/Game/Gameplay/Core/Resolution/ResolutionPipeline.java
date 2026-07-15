package Game.Gameplay.Core.Resolution;

import Game.Engine.GameObjects;
import Game.Gameplay.Core.Causality.CausalNode;
import Game.Gameplay.Core.Causality.InfluenceRegistry;
import Game.Gameplay.Core.Causality.ModifierContext;
import Game.Gameplay.Core.Dependencies.DependencyPropagator;
import Game.Gameplay.Core.Dependencies.PropagationResult;
import Game.Gameplay.Core.Dependencies.PropertyDependencyGraph;
import Game.Gameplay.Core.Modifiers.ModifierComponent;
import Game.Gameplay.Core.Modifiers.PropertyModifier;
import Game.Gameplay.Core.Operations.OperationContext;
import Game.Gameplay.Core.Operations.OperationRegistry;
import Game.Gameplay.Core.Properties.PropertyComponent;
import Game.Gameplay.Core.Properties.PropertyKey;
import Game.Gameplay.Core.Properties.PropertyMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Pipeline unificado de resolución, propagación y operaciones para una entidad.
 *
 * ── RESPONSABILIDAD ÚNICA ─────────────────────────────────────────────────
 * ResolutionPipeline orquesta el flujo completo descrito en CFCC-003:
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
 * concreta de gameplay. Trabaja exclusivamente mediante:
 *   - GameObjects (para obtener componentes)
 *   - PropertyKey (para identificar propiedades)
 *   - PropertyDependencyGraph (para conocer las relaciones)
 *   - OperationRegistry (para conocer las consecuencias)
 *   - InfluenceRegistry (para influencias externas sobre modificadores)
 *   - ModifierContext (para el contexto rico de resolución)
 *
 * ── INSTANCIABILIDAD ─────────────────────────────────────────────────────
 * ResolutionPipeline es instanciable. Cada entidad o sistema puede tener el
 * suyo, o pueden compartirse si las dependencias y operaciones son las mismas.
 *
 * Construcción mediante Builder para composición flexible:
 *
 *   ResolutionPipeline pipeline = ResolutionPipeline.builder()
 *       .dependencyGraph(sharedGraph)
 *       .operationRegistry(entityRegistry)
 *       .influenceRegistry(playerRegistry)
 *       .applyPropagatedDeltas(true)
 *       .collectCausalNodes(true)
 *       .build();
 *
 * ── APLICACIÓN DE DELTAS PROPAGADOS ──────────────────────────────────────
 * Cuando {@code applyPropagatedDeltas} es true, los deltas calculados por
 * el DependencyPropagator se escriben en el PropertyMap de la entidad destino
 * como modificadores aditivos con sourceId "propagation:<key_origen>".
 *
 * Si {@code applyPropagatedDeltas} es false, los deltas se calculan pero
 * no se aplican — el PropagationResult queda disponible en PipelineResult
 * para que el caller decida cómo usarlos.
 *
 * ── FLUJO DE resolve() ────────────────────────────────────────────────────
 *
 *   1. Leer valor anterior de la propiedad (del PropertyMap de la entidad).
 *   2. Invocar PropertyResolver con los parámetros configurados.
 *      → produce el valor final con todos los modificadores aplicados.
 *   3. Si hay DependencyPropagator: propagar el cambio.
 *      → produce PropagationResult con deltas por propiedad dependiente.
 *   4. Si applyPropagatedDeltas: escribir los deltas en el PropertyMap.
 *   5. Si hay OperationRegistry: construir OperationContext y ejecutar operaciones.
 *   6. Emitir CausalNode si hay collector.
 *   7. Retornar PipelineResult con todo el estado del frame.
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

    /** Grafo de dependencias entre propiedades. Null = sin propagación. */
    private final PropertyDependencyGraph dependencyGraph;

    /** Propagador asociado al grafo. Null si no hay grafo. */
    private final DependencyPropagator propagator;

    /** Registro de operaciones a ejecutar tras la resolución. Null = sin operaciones. */
    private final OperationRegistry operationRegistry;

    /** Registro de influencias externas sobre modificadores. Null = sin registry. */
    private final InfluenceRegistry influenceRegistry;

    /**
     * Si true, los deltas propagados se aplican al PropertyMap de la entidad
     * como modificadores aditivos (sourceId: "propagation:<origen>").
     * Si false, los deltas se calculan pero no se escriben.
     */
    private final boolean applyPropagatedDeltas;

    /**
     * Si true, se recolectan CausalNode por cada modificador efectivo.
     * Los nodos quedan en PipelineResult.getCausalNodes().
     */
    private final boolean collectCausalNodes;

    // ── Constructor privado ───────────────────────────────────────────────

    private ResolutionPipeline(Builder b) {
        this.dependencyGraph      = b.dependencyGraph;
        this.propagator           = (b.dependencyGraph != null)
            ? new DependencyPropagator(b.dependencyGraph)
            : null;
        this.operationRegistry    = b.operationRegistry;
        this.influenceRegistry    = b.influenceRegistry;
        this.applyPropagatedDeltas = b.applyPropagatedDeltas;
        this.collectCausalNodes   = b.collectCausalNodes;
    }

    // ── Factory ───────────────────────────────────────────────────────────

    /** Punto de entrada del Builder. */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Crea un pipeline mínimo sin dependencias ni operaciones.
     * Equivale a llamar PropertyResolver directamente, pero produce
     * un PipelineResult para uniformidad de la interfaz.
     */
    public static ResolutionPipeline minimal() {
        return builder().build();
    }

    // ── API pública: resolución completa ─────────────────────────────────

    /**
     * Ejecuta el pipeline completo para la propiedad indicada sobre la entidad dada.
     *
     * Pasos internos:
     *   1. Leer valor anterior del PropertyMap.
     *   2. Resolver la propiedad con PropertyResolver.
     *   3. Propagar dependencias (si hay grafo).
     *   4. Aplicar deltas propagados al PropertyMap (si applyPropagatedDeltas).
     *   5. Ejecutar operaciones del OperationRegistry (si hay registry).
     *   6. Recolectar CausalNode (si collectCausalNodes).
     *   7. Retornar PipelineResult.
     *
     * @param entity   entidad cuya propiedad se resuelve
     * @param key      propiedad a resolver
     * @param timestamp frame actual (para CausalNode y OperationContext)
     * @return resultado completo del pipeline para este frame
     */
    public PipelineResult resolve(GameObjects entity, PropertyKey<?> key, long timestamp) {
        return resolve(entity, key, null, timestamp);
    }

    /**
     * Versión con ModifierContext explícito. Permite pasar source, target,
     * triggeringEvent y demás campos de causalidad.
     *
     * Si {@code externalContext} es null, el PropertyResolver construirá
     * un contexto mínimo internamente.
     *
     * @param entity          entidad cuya propiedad se resuelve
     * @param key             propiedad a resolver
     * @param externalContext contexto de resolución externo (puede ser null)
     * @param timestamp       frame actual
     * @return resultado completo del pipeline para este frame
     */
    public PipelineResult resolve(
            GameObjects     entity,
            PropertyKey<?>  key,
            ModifierContext externalContext,
            long            timestamp) {

        if (entity == null || key == null) {
            return PipelineResult.empty(key);
        }

        // ── Paso 1: extraer componentes ───────────────────────────────────
        PropertyComponent pc = entity.getComponent(PropertyComponent.class);
        ModifierComponent mc = entity.getComponent(ModifierComponent.class);

        PropertyMap propertyMap = (pc != null)
            ? pc.getMap()
            : new PropertyMap();

        // Valor anterior (antes de cualquier modificador de este frame)
        double previousValue = propertyMap.getBase(key);

        // ── Paso 2: resolver propiedad ────────────────────────────────────
        List<CausalNode> causalLog = collectCausalNodes ? new ArrayList<>() : null;

        double finalValue;
        if (mc != null) {
            finalValue = PropertyResolver.resolveWithCausalLog(
                propertyMap,
                key,
                mc.getContainer(),
                externalContext,
                influenceRegistry,
                causalLog
            );
        } else {
            // Sin modificadores: el valor final es el base
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
            key,
            previousValue,
            finalValue,
            propagation,
            causalLog != null ? causalLog : List.of(),
            timestamp
        );
    }

    /**
     * Versión simplificada: resuelve la propiedad a través de un ResolutionContext
     * ya construido (sin entidad directa). Solo ejecuta resolución y operaciones;
     * no propaga dependencias (porque no hay acceso al PropertyMap para actualizarlo).
     *
     * Útil para sistemas de combate que ya tienen el ResolutionContext preparado.
     *
     * @param resolutionContext contexto de resolución ya construido
     * @param key               propiedad a resolver
     * @param externalContext   contexto de modificadores (puede ser null)
     * @param timestamp         frame actual
     * @return resultado del pipeline (sin propagación)
     */
    public PipelineResult resolveFromContext(
            ResolutionContext resolutionContext,
            PropertyKey<?>    key,
            ModifierContext   externalContext,
            long              timestamp) {

        if (resolutionContext == null || key == null) {
            return PipelineResult.empty(key);
        }

        double previousValue = resolutionContext.getMap().getBase(key);

        List<CausalNode> causalLog = collectCausalNodes ? new ArrayList<>() : null;

        double finalValue = PropertyResolver.resolveWithCausalLog(
            resolutionContext.getMap(),
            key,
            resolutionContext.getContainer(),
            externalContext,
            influenceRegistry,
            causalLog
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
            key,
            previousValue,
            finalValue,
            PropagationResult.empty(),
            causalLog != null ? causalLog : List.of(),
            timestamp
        );
    }

    // ── Implementación interna ────────────────────────────────────────────

    /**
     * Escribe los deltas calculados por el propagador en el PropertyMap
     * de la entidad como modificadores aditivos.
     *
     * Los modificadores se insertan directamente en el PropertyMap (setBase)
     * sumando el delta al valor base actual. Esto modifica el "valor base"
     * de la propiedad destino para el siguiente frame.
     *
     * Alternativa que el caller puede usar en su lugar: leer
     * PipelineResult.getPropagation().allDeltas() y aplicarlos como
     * PropertyModifier temporales en el ModifierContainer.
     */
    private static void applyPropagatedDeltas(
            PropagationResult propagation,
            PropertyMap       propertyMap,
            PropertyKey<?>    sourceKey) {

        for (Map.Entry<String, Double> entry : propagation.allDeltas().entrySet()) {
            PropertyKey<?> targetKey = propagation.affectedKeys().get(entry.getKey());
            if (targetKey == null) continue;

            double currentBase = propertyMap.getBase(targetKey);
            double delta       = entry.getValue();

            // Solo actualizar si el delta es significativo (evitar ruido de punto flotante)
            if (Math.abs(delta) > 1e-12) {
                propertyMap.setBase(targetKey, currentBase + delta);
            }
        }
    }

    /**
     * Construye el OperationContext para las operaciones de este paso del pipeline.
     */
    private static OperationContext buildOperationContext(
            GameObjects     entity,
            PropertyKey<?>  key,
            double          previousValue,
            double          finalValue,
            ModifierContext modifierContext,
            List<CausalNode> causalLog,
            long            timestamp) {

        // Si hay nodos causales, usar el primero como nodo de referencia
        CausalNode representativeNode = (causalLog != null && !causalLog.isEmpty())
            ? causalLog.get(0)
            : null;

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

    /** El grafo de dependencias configurado, o null. */
    public PropertyDependencyGraph getDependencyGraph()    { return dependencyGraph; }

    /** El registro de operaciones configurado, o null. */
    public OperationRegistry getOperationRegistry()        { return operationRegistry; }

    /** El registro de influencias configurado, o null. */
    public InfluenceRegistry getInfluenceRegistry()        { return influenceRegistry; }

    /** True si los deltas propagados se aplican automáticamente al PropertyMap. */
    public boolean isApplyPropagatedDeltas()               { return applyPropagatedDeltas; }

    /** True si se recolectan CausalNode en cada resolución. */
    public boolean isCollectCausalNodes()                  { return collectCausalNodes; }

    // ── Object ────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "ResolutionPipeline["
            + "graph=" + (dependencyGraph != null ? dependencyGraph.edgeCount() + " edges" : "none")
            + ", ops=" + (operationRegistry != null ? operationRegistry.size() : 0)
            + ", applyDeltas=" + applyPropagatedDeltas
            + "]";
    }

    // ═════════════════════════════════════════════════════════════════════
    // PipelineResult — resultado inmutable de una ejecución del pipeline
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Resultado inmutable de una ejecución completa del pipeline.
     *
     * Contiene:
     *   - La propiedad resuelta y sus valores anterior y final.
     *   - El PropagationResult con los deltas de propiedades dependientes.
     *   - Los CausalNode emitidos durante la resolución (si collectCausalNodes).
     *   - El timestamp del frame.
     */
    public static final class PipelineResult {

        private final PropertyKey<?>       resolvedKey;
        private final double               previousValue;
        private final double               finalValue;
        private final PropagationResult    propagation;
        private final List<CausalNode>     causalNodes;
        private final long                 timestamp;

        PipelineResult(
                PropertyKey<?>    resolvedKey,
                double            previousValue,
                double            finalValue,
                PropagationResult propagation,
                List<CausalNode>  causalNodes,
                long              timestamp) {
            this.resolvedKey   = resolvedKey;
            this.previousValue = previousValue;
            this.finalValue    = finalValue;
            this.propagation   = propagation;
            this.causalNodes   = causalNodes;
            this.timestamp     = timestamp;
        }

        /** La propiedad que fue resuelta. */
        public PropertyKey<?> getResolvedKey()         { return resolvedKey; }

        /** Valor anterior a la resolución (valor base sin modificadores del frame). */
        public double getPreviousValue()               { return previousValue; }

        /** Valor final tras la resolución completa del pipeline. */
        public double getFinalValue()                  { return finalValue; }

        /** Delta de la resolución principal: finalValue - previousValue. */
        public double getDelta()                       { return finalValue - previousValue; }

        /** Resultado de la propagación de dependencias. Nunca null. */
        public PropagationResult getPropagation()      { return propagation; }

        /**
         * Nodos causales emitidos durante la resolución.
         * Vacío si collectCausalNodes era false en la configuración del pipeline.
         */
        public List<CausalNode> getCausalNodes()       { return causalNodes; }

        /** Timestamp del frame en que ocurrió esta resolución. */
        public long getTimestamp()                     { return timestamp; }

        /** True si el valor cambió durante la resolución. */
        public boolean valueChanged()                  { return finalValue != previousValue; }

        /** True si alguna propiedad dependiente recibió cambios por propagación. */
        public boolean hadPropagation()                { return propagation.hasChanges(); }

        /** True si se detectaron ciclos en la propagación. */
        public boolean hadCycles()                     { return propagation.hadCycles(); }

        @Override
        public String toString() {
            return "PipelineResult["
                + "key=" + (resolvedKey != null ? resolvedKey.id() : "null")
                + ", prev=" + previousValue
                + ", final=" + finalValue
                + ", propagated=" + propagation.allDeltas().size()
                + ", t=" + timestamp
                + "]";
        }

        // ── Resultado vacío ───────────────────────────────────────────────

        /** Resultado vacío cuando la resolución no pudo ejecutarse. */
        static PipelineResult empty(PropertyKey<?> key) {
            return new PipelineResult(key, 0.0, 0.0,
                PropagationResult.empty(), List.of(), 0L);
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // Builder
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Builder de ResolutionPipeline.
     *
     * Todos los campos son opcionales. Un pipeline sin configuración adicional
     * funciona como una llamada directa a PropertyResolver.
     */
    public static final class Builder {

        private PropertyDependencyGraph dependencyGraph;
        private OperationRegistry       operationRegistry;
        private InfluenceRegistry       influenceRegistry;
        private boolean                 applyPropagatedDeltas = true;
        private boolean                 collectCausalNodes    = false;

        private Builder() {}

        /**
         * Grafo de dependencias entre propiedades.
         * Si es null, no se realiza propagación.
         */
        public Builder dependencyGraph(PropertyDependencyGraph graph) {
            this.dependencyGraph = graph;
            return this;
        }

        /**
         * Registro de operaciones a ejecutar tras la resolución.
         * Si es null, no se ejecutan operaciones.
         */
        public Builder operationRegistry(OperationRegistry registry) {
            this.operationRegistry = registry;
            return this;
        }

        /**
         * Registro de influencias externas sobre modificadores.
         * Si es null, no se aplican influencias externas.
         */
        public Builder influenceRegistry(InfluenceRegistry registry) {
            this.influenceRegistry = registry;
            return this;
        }

        /**
         * Si true (por defecto), los deltas propagados se escriben en el PropertyMap.
         * Si false, los deltas están disponibles en PipelineResult pero no se aplican.
         */
        public Builder applyPropagatedDeltas(boolean apply) {
            this.applyPropagatedDeltas = apply;
            return this;
        }

        /**
         * Si true, se recolectan CausalNode por cada modificador efectivo.
         * Por defecto false para minimizar allocaciones en el game loop.
         */
        public Builder collectCausalNodes(boolean collect) {
            this.collectCausalNodes = collect;
            return this;
        }

        /** Construye el ResolutionPipeline. */
        public ResolutionPipeline build() {
            return new ResolutionPipeline(this);
        }
    }
}
