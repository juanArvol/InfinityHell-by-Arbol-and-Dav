package Game.Gameplay.Core.Resolution;

import Game.Gameplay.Core.Causality.CausalNode;
import Game.Gameplay.Core.Causality.InfluenceRegistry;
import Game.Gameplay.Core.Causality.ModifierContext;
import Game.Gameplay.Core.Causality.ModifierInfluence;
import Game.Gameplay.Core.Causality.ModifierPredicate;
import Game.Gameplay.Core.Modifiers.ModifierContainer;
import Game.Gameplay.Core.Modifiers.PropertyModifier;
import Game.Gameplay.Core.Properties.PropertyKey;
import Game.Gameplay.Core.Properties.PropertyMap;
import java.util.ArrayList;
import java.util.List;

/**
 * Resolvedor de propiedades — calcula el valor final de una propiedad aplicando
 * todos los modificadores activos sobre su valor base.
 *
 * ── PIPELINE DE RESOLUCIÓN (CFCC-001 + CFCC-002 + CFCC-002A) ─────────────
 *
 *   Valor Base (PropertyMap)
 *       ↓
 *   [CFCC-002]  Construir ModifierContext (mínimo o externo)
 *       ↓
 *   [CFCC-002A] Aplicar InfluenceRegistry (influencias EXTERNAS ordenadas)
 *               Un sistema externo puede amplificar / cancelar modificadores
 *               de cualquier entidad sin acoplamientos directos.
 *               Si una influencia retorna null → modificador excluido.
 *       ↓
 *   [CFCC-002]  Filtrar por Predicate
 *               Excluye modificadores cuyo predicado retorna false.
 *               Si no tiene predicado → equivale a ALWAYS → se incluye.
 *       ↓
 *   [CFCC-002]  Aplicar Influence PROPIA del modificador
 *               Si la influencia propia retorna null → excluido.
 *       ↓
 *   [CFCC-001]  Fase 1 — ADDITIVE       resultado = base + Σ(aditivos)
 *       ↓
 *   [CFCC-001]  Fase 2 — MULTIPLICATIVE resultado = resultado × Π(mult.)
 *       ↓
 *   [CFCC-001]  Fase 3 — OVERRIDE       sustituye si existe override
 *       ↓
 *   [CFCC-001]  Fase 4 — CLAMP          clamp al rango [min, max]
 *       ↓
 *   [CFCC-002A] Emitir CausalNode por cada modificador efectivo aplicado
 *               Solo si se proporcionó un collector no null.
 *       ↓
 *   Valor Final
 *
 * ── CAPAS DE CAUSALIDAD: ORDEN CONCEPTUAL ────────────────────────────────
 * 1. InfluenceRegistry  — "¿quieren influencias EXTERNAS alterar este modificador?"
 * 2. Predicate          — "¿aplica este modificador en este contexto?"
 * 3. Influence propia   — "¿cómo declara el modificador que puede ser transformado?"
 * 4. Pipeline numérico  — cálculo del valor final
 * 5. CausalNode         — registro del hecho para trazabilidad
 *
 * ── COMPATIBILIDAD COMPLETA ───────────────────────────────────────────────
 * - CFCC-001: los tres overloads originales (resolve, resolveWithClamp,
 *             applyAdditiveOnly, compositeMultiplier) funcionan exactamente igual.
 * - CFCC-002: resolveWithContext / resolveWithContextAndClamp sin cambios.
 * - CFCC-002A: nuevos overloads con InfluenceRegistry y/o collector son aditivos.
 *
 * ── STATELESS ─────────────────────────────────────────────────────────────
 * PropertyResolver es completamente stateless. Todos los métodos son estáticos.
 * InfluenceRegistry y collector son parámetros — no hay estado interno.
 *
 * ── USO ──────────────────────────────────────────────────────────────────
 *
 *   // CFCC-001 — sin cambios:
 *   double dmg = PropertyResolver.resolve(map, PropertyKeys.DAMAGE, container);
 *
 *   // CFCC-002 — con contexto rico:
 *   double dmg = PropertyResolver.resolveWithContext(map, key, container, ctx);
 *
 *   // CFCC-002A — con InfluenceRegistry externo:
 *   double dmg = PropertyResolver.resolveWithRegistry(
 *       map, key, container, ctx, entityInfluenceRegistry
 *   );
 *
 *   // CFCC-002A — con colector de CausalNode para trazabilidad:
 *   List<CausalNode> log = new ArrayList<>();
 *   double dmg = PropertyResolver.resolveWithCausalLog(
 *       map, key, container, ctx, registry, log
 *   );
 *   // log ahora contiene un CausalNode por cada modificador que actuó
 */
public final class PropertyResolver {

    private PropertyResolver() {}

    // ── API pública: overloads CFCC-001 (sin cambios) ────────────────────

    /**
     * Resuelve el valor final sin clamp. Compatible con CFCC-001.
     */
    public static double resolve(PropertyMap map, PropertyKey<?> key, ModifierContainer container) {
        return resolveCore(map, key, container, null, null, null,
            Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
    }

    /**
     * Resuelve el valor final con clamp [min, max]. Compatible con CFCC-001.
     */
    public static double resolveWithClamp(
            PropertyMap map, PropertyKey<?> key, ModifierContainer container,
            double min, double max) {
        return resolveCore(map, key, container, null, null, null, min, max);
    }

    // ── API pública: overloads CFCC-002 (sin cambios) ────────────────────

    /**
     * Resuelve con ModifierContext rico. Sin clamp. Compatible con CFCC-002.
     */
    public static double resolveWithContext(
            PropertyMap map, PropertyKey<?> key, ModifierContainer container,
            ModifierContext context) {
        return resolveCore(map, key, container, context, null, null,
            Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
    }

    /**
     * Resuelve con ModifierContext y clamp. Compatible con CFCC-002.
     */
    public static double resolveWithContextAndClamp(
            PropertyMap map, PropertyKey<?> key, ModifierContainer container,
            ModifierContext context, double min, double max) {
        return resolveCore(map, key, container, context, null, null, min, max);
    }

    // ── API pública: overloads CFCC-002A (nuevos) ────────────────────────

    /**
     * Resuelve con ModifierContext e InfluenceRegistry externos.
     * El registry aplica influencias externas antes de los predicados propios.
     *
     * @param map      mapa de valores base de la entidad
     * @param key      propiedad a resolver
     * @param container modificadores activos
     * @param context  contexto rico de resolución (puede ser null)
     * @param registry registro de influencias externas (puede ser null)
     * @return valor final calculado sin clamp
     */
    public static double resolveWithRegistry(
            PropertyMap map, PropertyKey<?> key, ModifierContainer container,
            ModifierContext context, InfluenceRegistry registry) {
        return resolveCore(map, key, container, context, registry, null,
            Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
    }

    /**
     * Resuelve con ModifierContext, InfluenceRegistry y clamp.
     */
    public static double resolveWithRegistryAndClamp(
            PropertyMap map, PropertyKey<?> key, ModifierContainer container,
            ModifierContext context, InfluenceRegistry registry,
            double min, double max) {
        return resolveCore(map, key, container, context, registry, null, min, max);
    }

    /**
     * Resuelve con ModifierContext, InfluenceRegistry y colector de CausalNode.
     *
     * Por cada modificador efectivamente aplicado, emite un {@link CausalNode}
     * en el {@code causalLog}. El caller puede usar esa lista para:
     *   - reconstruir la cadena completa de causalidad,
     *   - vincular los nodos a un grafo mayor via addChild(),
     *   - diagnosticar por qué un valor final resultó ser X.
     *
     * Si {@code causalLog} es null, no se emiten nodos (equivale a
     * {@link #resolveWithRegistry}).
     *
     * @param map       mapa de valores base de la entidad
     * @param key       propiedad a resolver
     * @param container modificadores activos
     * @param context   contexto rico (puede ser null)
     * @param registry  influencias externas (puede ser null)
     * @param causalLog lista donde se añadirán los CausalNode emitidos (puede ser null)
     * @return valor final calculado sin clamp
     */
    public static double resolveWithCausalLog(
            PropertyMap map, PropertyKey<?> key, ModifierContainer container,
            ModifierContext context, InfluenceRegistry registry,
            List<CausalNode> causalLog) {
        return resolveCore(map, key, container, context, registry, causalLog,
            Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
    }

    /**
     * Versión completa: contexto, registry, colector y clamp.
     */
    public static double resolveWithCausalLogAndClamp(
            PropertyMap map, PropertyKey<?> key, ModifierContainer container,
            ModifierContext context, InfluenceRegistry registry,
            List<CausalNode> causalLog, double min, double max) {
        return resolveCore(map, key, container, context, registry, causalLog, min, max);
    }

    // ── Implementación central ────────────────────────────────────────────

    /**
     * Implementación unificada del pipeline completo.
     * Todos los parámetros opcionales pueden ser null.
     */
    private static double resolveCore(
            PropertyMap      map,
            PropertyKey<?>   key,
            ModifierContainer container,
            ModifierContext  externalContext,
            InfluenceRegistry registry,
            List<CausalNode> causalLog,
            double min,
            double max) {

        // Paso 0: valor base
        double base   = map.getBase(key);
        double result = base;

        // Optimización: sin modificadores para esta clave → base + clamp
        if (container.isEmpty() || !container.hasModifiersFor(key)) {
            return clamp(result, min, max);
        }

        List<PropertyModifier> rawMods = container.getFor(key);

        // Construir el contexto base: externo si existe, mínimo si no
        ModifierContext baseContext = (externalContext != null)
            ? externalContext.withCurrentValue(base)
            : ModifierContext.minimal(key, base, base);

        // ── Fase de causalidad: registry + predicate + influence propia ───
        List<PropertyModifier> effectiveMods =
            filterWithRegistry(rawMods, baseContext, registry);

        // ── Pipeline numérico (CFCC-001) ──────────────────────────────────

        // Fase 1: ADDITIVE
        for (PropertyModifier m : effectiveMods) {
            if (m.getPhase() == PropertyModifier.Phase.ADDITIVE) {
                result += m.getValue();
            }
        }

        // Fase 2: MULTIPLICATIVE
        double multiplier = 1.0;
        for (PropertyModifier m : effectiveMods) {
            if (m.getPhase() == PropertyModifier.Phase.MULTIPLICATIVE) {
                multiplier *= m.getValue();
            }
        }
        result *= multiplier;

        // Fase 3: OVERRIDE — el último gana
        double  overrideValue = Double.NaN;
        boolean hasOverride   = false;
        for (PropertyModifier m : effectiveMods) {
            if (m.getPhase() == PropertyModifier.Phase.OVERRIDE) {
                overrideValue = m.getValue();
                hasOverride   = true;
            }
        }
        if (hasOverride) result = overrideValue;

        // Fase 4: CLAMP
        double finalResult = clamp(result, min, max);

        // ── Emisión de CausalNode (CFCC-002A) ─────────────────────────────
        // Solo si el caller proporcionó un colector.
        // Cada modificador efectivo produce un nodo que registra el hecho:
        // qué modificador actuó, sobre qué propiedad, cuánto cambió el valor,
        // bajo qué contexto y en qué frame.
        if (causalLog != null) {
            emitCausalNodes(effectiveMods, key, base, finalResult, baseContext, causalLog);
        }

        return finalResult;
    }

    // ── Fase de causalidad: registry + predicate + influence propia ───────

    /**
     * Aplica el InfluenceRegistry externo (si existe), luego el predicado y la
     * influencia propia de cada modificador, produciendo la lista efectiva.
     *
     * Orden por modificador m:
     *   1. InfluenceRegistry.apply(m, ctx)  → transformación o cancelación externa
     *      Si retorna null → excluido. Las siguientes etapas no se ejecutan.
     *   2. m.getPredicate().test(ctx)        → condición de activación
     *      Si retorna false → excluido.
     *   3. m.getInfluence().apply(m, ctx)    → transformación interna declarada
     *      Si retorna null → excluido.
     *   4. Incluir el modificador resultante en effectiveMods.
     */
    private static List<PropertyModifier> filterWithRegistry(
            List<PropertyModifier> mods,
            ModifierContext        context,
            InfluenceRegistry      registry) {

        List<PropertyModifier> result = new ArrayList<>(mods.size());

        for (PropertyModifier m : mods) {

            // Paso 1: influencias externas (InfluenceRegistry)
            PropertyModifier current = m;
            if (registry != null && registry.hasInfluences()) {
                current = registry.apply(current, context);
                if (current == null) continue;   // cancelado externamente
            }

            // Paso 2: predicado propio del modificador (post-registry,
            // porque el registry puede haber cambiado el modificador)
            ModifierPredicate pred = current.getPredicate();
            if (pred != null && !pred.test(context)) continue;

            // Paso 3: influencia propia del modificador
            ModifierInfluence inf = current.getInfluence();
            if (inf != null) {
                current = inf.apply(current, context);
                if (current == null) continue;   // cancelado internamente
            }

            result.add(current);
        }

        return result;
    }

    // ── Emisión de CausalNode ─────────────────────────────────────────────

    /**
     * Por cada modificador efectivo, emite un CausalNode al colector.
     *
     * El delta individual de cada modificador se aproxima a partir de su fase
     * y valor. Para la trazabilidad a nivel de "¿cuánto aportó cada modificador?"
     * esto es exacto en ADDITIVE, aproximado en MULTIPLICATIVE y OVERRIDE
     * (porque el valor final de esas fases depende de todos los modificadores
     * de esa fase juntos, no de uno solo).
     *
     * Los nodos se emiten en el orden en que los modificadores fueron aplicados.
     * No se vinculan automáticamente entre sí (eso es responsabilidad del caller
     * que conoce el grafo causal del frame).
     *
     * @param effectiveMods modificadores que llegaron al pipeline numérico
     * @param key           propiedad resuelta
     * @param base          valor base de la propiedad
     * @param finalResult   valor final tras todo el pipeline
     * @param context       contexto de resolución
     * @param causalLog     colector donde añadir los nodos
     */
    private static void emitCausalNodes(
            List<PropertyModifier> effectiveMods,
            PropertyKey<?>         key,
            double                 base,
            double                 finalResult,
            ModifierContext        context,
            List<CausalNode>       causalLog) {

        long timestamp = context.getTimestamp();

        // Para cada modificador emitimos un nodo con la contribución estimada.
        // valueBefore = base (referencia compartida: todos contribuyen al mismo valor).
        // valueAfter  = finalResult para el último modificador de la lista;
        //               para los intermedios usamos la contribución individual.
        //
        // Decisión de diseño: el caller que necesita precisión completa
        // (valor intermedio tras cada paso) puede resolver fase a fase con
        // los overloads applyAdditiveOnly / compositeMultiplier.
        // El objetivo de este log es trazabilidad de QUIÉN actuó, no solo CUÁNTO.

        for (PropertyModifier m : effectiveMods) {
            double contribution = estimateContribution(m, base, effectiveMods);
            CausalNode node = CausalNode.of(
                m,
                key,
                base,                    // valor antes (referencia: el base)
                base + contribution,     // valor estimado con esta contribución
                context,
                timestamp
            );
            causalLog.add(node);
        }
    }

    /**
     * Estima la contribución individual de un modificador al resultado final.
     *
     * ADDITIVE: contribución = valor del modificador (exacto)
     * MULTIPLICATIVE: contribución = (factor - 1.0) * base (aproxi lineal)
     * OVERRIDE: contribución = fixedValue - base (el override desplaza desde base)
     *
     * Esta es una aproximación razonable para trazabilidad. No es el valor exacto
     * porque los efectos multiplicativos y override interactúan entre sí en el pipeline.
     */
    private static double estimateContribution(
            PropertyModifier       m,
            double                 base,
            List<PropertyModifier> allEffective) {
        return switch (m.getPhase()) {
            case ADDITIVE       -> m.getValue();
            case MULTIPLICATIVE -> (m.getValue() - 1.0) * base;
            case OVERRIDE       -> m.getValue() - base;
        };
    }

    // ── Utilidades (CFCC-001, actualizadas para respetar registry) ────────

    /**
     * Aplica únicamente los modificadores aditivos de una propiedad sobre un valor.
     * Respeta predicados, influencia propia y el InfluenceRegistry si se proporciona.
     *
     * @param base      valor base
     * @param key       propiedad cuyos aditivos se aplicarán
     * @param container contenedor de modificadores activos
     * @return base + Σ(aditivos efectivos de esa clave)
     */
    public static double applyAdditiveOnly(
            double base, PropertyKey<?> key, ModifierContainer container) {
        return applyAdditiveOnly(base, key, container, null, null);
    }

    /**
     * Variante con InfluenceRegistry externo.
     */
    public static double applyAdditiveOnly(
            double base, PropertyKey<?> key, ModifierContainer container,
            ModifierContext context, InfluenceRegistry registry) {
        double result = base;
        ModifierContext ctx = context != null
            ? context.withCurrentValue(base)
            : ModifierContext.minimal(key, base, base);
        for (PropertyModifier m : filterWithRegistry(container.getFor(key), ctx, registry)) {
            if (m.getPhase() == PropertyModifier.Phase.ADDITIVE) {
                result += m.getValue();
            }
        }
        return result;
    }

    /**
     * Calcula el factor multiplicativo compuesto de una propiedad.
     * Respeta predicados, influencia propia.
     *
     * @param key       propiedad consultada
     * @param container contenedor de modificadores activos
     * @return producto de todos los multiplicativos efectivos, o 1.0 si no hay
     */
    public static double compositeMultiplier(
            PropertyKey<?> key, ModifierContainer container) {
        return compositeMultiplier(key, container, null, null);
    }

    /**
     * Variante con InfluenceRegistry externo.
     */
    public static double compositeMultiplier(
            PropertyKey<?> key, ModifierContainer container,
            ModifierContext context, InfluenceRegistry registry) {
        double result = 1.0;
        ModifierContext ctx = context != null
            ? context
            : ModifierContext.minimal(key, 1.0, 1.0);
        for (PropertyModifier m : filterWithRegistry(container.getFor(key), ctx, registry)) {
            if (m.getPhase() == PropertyModifier.Phase.MULTIPLICATIVE) {
                result *= m.getValue();
            }
        }
        return result;
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private static double clamp(double value, double min, double max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }
}
