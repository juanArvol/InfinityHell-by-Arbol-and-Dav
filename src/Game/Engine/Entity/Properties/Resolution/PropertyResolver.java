package Game.Engine.Entity.Properties.Resolution;

import Game.Engine.Entity.Properties.Modifier.Causality.CausalNode;
import Game.Engine.Entity.Properties.Modifier.Causality.InfluenceRegistry;
import Game.Engine.Entity.Properties.Modifier.Causality.ModifierContext;
import Game.Engine.Entity.Properties.Modifier.Causality.ModifierInfluence;
import Game.Engine.Entity.Properties.Modifier.Causality.ModifierPredicate;
import Game.Engine.Entity.Properties.Modifier.PropertyModifier;
import Game.Engine.Entity.Properties.Modifier.PropertyModifierContainer;
import Game.Engine.Entity.Properties.PropertyKey;
import Game.Engine.Entity.Properties.PropertyMap;

import java.util.ArrayList;
import java.util.List;

/**
 * Resolvedor de propiedades — calcula el valor final de una propiedad
 * aplicando todos los modificadores activos sobre su valor base.
 *
 * ── PIPELINE DE RESOLUCIÓN ────────────────────────────────────────────────
 *
 *   Valor Base (PropertyMap)
 *       ↓
 *   Construir ModifierContext (mínimo o externo)
 *       ↓
 *   Aplicar InfluenceRegistry (influencias EXTERNAS ordenadas)
 *       ↓
 *   Filtrar por Predicate (condición de activación)
 *       ↓
 *   Aplicar Influence PROPIA del modificador
 *       ↓
 *   Fase 1 — ADDITIVE       resultado = base + Σ(aditivos)
 *       ↓
 *   Fase 2 — MULTIPLICATIVE resultado = resultado × Π(mult.)
 *       ↓
 *   Fase 3 — OVERRIDE       sustituye si existe override
 *       ↓
 *   Fase 4 — CLAMP          clamp al rango [min, max]
 *       ↓
 *   Emitir CausalNode por cada modificador efectivo (si hay collector)
 *       ↓
 *   Valor Final
 *
 * ── STATELESS ─────────────────────────────────────────────────────────────
 * PropertyResolver es completamente stateless. Todos los métodos son estáticos.
 */
public final class PropertyResolver {

    private PropertyResolver() {}

    // ── API pública: overloads básicos (sin causalidad) ───────────────────

    public static double resolve(PropertyMap map, PropertyKey<?> key, PropertyModifierContainer container) {
        return resolveCore(map, key, container, null, null, null,
            Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
    }

    public static double resolveWithClamp(
            PropertyMap map, PropertyKey<?> key, PropertyModifierContainer container,
            double min, double max) {
        return resolveCore(map, key, container, null, null, null, min, max);
    }

    // ── API pública: overloads con contexto ───────────────────────────────

    public static double resolveWithContext(
            PropertyMap map, PropertyKey<?> key, PropertyModifierContainer container,
            ModifierContext context) {
        return resolveCore(map, key, container, context, null, null,
            Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
    }

    public static double resolveWithContextAndClamp(
            PropertyMap map, PropertyKey<?> key, PropertyModifierContainer container,
            ModifierContext context, double min, double max) {
        return resolveCore(map, key, container, context, null, null, min, max);
    }

    // ── API pública: overloads con registry ───────────────────────────────

    public static double resolveWithRegistry(
            PropertyMap map, PropertyKey<?> key, PropertyModifierContainer container,
            ModifierContext context, InfluenceRegistry registry) {
        return resolveCore(map, key, container, context, registry, null,
            Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
    }

    public static double resolveWithRegistryAndClamp(
            PropertyMap map, PropertyKey<?> key, PropertyModifierContainer container,
            ModifierContext context, InfluenceRegistry registry,
            double min, double max) {
        return resolveCore(map, key, container, context, registry, null, min, max);
    }

    // ── API pública: overloads con log causal ─────────────────────────────

    public static double resolveWithCausalLog(
            PropertyMap map, PropertyKey<?> key, PropertyModifierContainer container,
            ModifierContext context, InfluenceRegistry registry,
            List<CausalNode> causalLog) {
        return resolveCore(map, key, container, context, registry, causalLog,
            Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
    }

    public static double resolveWithCausalLogAndClamp(
            PropertyMap map, PropertyKey<?> key, PropertyModifierContainer container,
            ModifierContext context, InfluenceRegistry registry,
            List<CausalNode> causalLog, double min, double max) {
        return resolveCore(map, key, container, context, registry, causalLog, min, max);
    }

    // ── Implementación central ────────────────────────────────────────────

    private static double resolveCore(
            PropertyMap              map,
            PropertyKey<?>           key,
            PropertyModifierContainer container,
            ModifierContext          externalContext,
            InfluenceRegistry        registry,
            List<CausalNode>         causalLog,
            double min,
            double max) {

        double base   = map.getBase(key);
        double result = base;

        if (container.isEmpty() || !container.hasModifiersFor(key)) {
            return clamp(result, min, max);
        }

        List<PropertyModifier> rawMods = container.getFor(key);

        ModifierContext baseContext = (externalContext != null)
            ? externalContext.withCurrentValue(base)
            : ModifierContext.minimal(key, base, base);

        List<PropertyModifier> effectiveMods =
            filterWithRegistry(rawMods, baseContext, registry);

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

        // Emisión de CausalNode (solo si el caller proporcionó un colector)
        if (causalLog != null) {
            emitCausalNodes(effectiveMods, key, base, finalResult, baseContext, causalLog);
        }

        return finalResult;
    }

    // ── Fase de causalidad ────────────────────────────────────────────────

    private static List<PropertyModifier> filterWithRegistry(
            List<PropertyModifier> mods,
            ModifierContext        context,
            InfluenceRegistry      registry) {

        List<PropertyModifier> result = new ArrayList<>(mods.size());

        for (PropertyModifier m : mods) {
            PropertyModifier current = m;

            // Paso 1: influencias externas
            if (registry != null && registry.hasInfluences()) {
                current = registry.apply(current, context);
                if (current == null) continue;
            }

            // Paso 2: predicado propio
            ModifierPredicate pred = current.getPredicate();
            if (pred != null && !pred.test(context)) continue;

            // Paso 3: influencia propia
            ModifierInfluence inf = current.getInfluence();
            if (inf != null) {
                current = inf.apply(current, context);
                if (current == null) continue;
            }

            result.add(current);
        }

        return result;
    }

    private static void emitCausalNodes(
            List<PropertyModifier> effectiveMods,
            PropertyKey<?>         key,
            double                 base,
            double                 finalResult,
            ModifierContext        context,
            List<CausalNode>       causalLog) {

        long timestamp = context.getTimestamp();

        for (PropertyModifier m : effectiveMods) {
            double contribution = estimateContribution(m, base, effectiveMods);
            CausalNode node = CausalNode.of(
                m, key, base, base + contribution, context, timestamp
            );
            causalLog.add(node);
        }
    }

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

    // ── Utilidades ────────────────────────────────────────────────────────

    public static double applyAdditiveOnly(
            double base, PropertyKey<?> key, PropertyModifierContainer container) {
        return applyAdditiveOnly(base, key, container, null, null);
    }

    public static double applyAdditiveOnly(
            double base, PropertyKey<?> key, PropertyModifierContainer container,
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

    public static double compositeMultiplier(
            PropertyKey<?> key, PropertyModifierContainer container) {
        return compositeMultiplier(key, container, null, null);
    }

    public static double compositeMultiplier(
            PropertyKey<?> key, PropertyModifierContainer container,
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

    private static double clamp(double value, double min, double max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }
}
