package Game.Engine.Physics.Core;

import java.util.EnumMap;
import java.util.Map;

/**
 * Registro de evaluadores especializados por RelationType.
 *
 * ── HRFC-022 — Eliminación del Paradigma de Ley Ejecutable ───────────────
 * ── HRFC — Cierre del Refactor Arquitectónico ─────────────────────────────
 * ── HRFC — Auditoría Arquitectónica Final ────────────────────────────────
 *
 * ── RESPONSABILIDAD ──────────────────────────────────────────────────────
 * EvaluatorRegistry mapea cada RelationType a su RelationEvaluator
 * especializado. El PhysicsSolver lo consulta para obtener el evaluador
 * correcto dado el tipo de una PhysicalRelation.
 *
 * ── COMPOSICIÓN ───────────────────────────────────────────────────────────
 * El registro se construye desde la capa de composición (Physics.World),
 * no desde el Core. Cada PhysicsModule registra sus propios evaluadores
 * en el EvaluatorRegistry interno del PhysicsCoordinator.Builder:
 *
 *   module.registerEvaluators(coordinatorBuilder.evaluators())
 *       → PhysicsCoordinator.Builder
 *           → RelationResolver
 *               → PhysicsSolver
 *
 * Esto mantiene Core libre de dependencias hacia dominios concretos.
 *
 * ── POLÍTICA DE SOBREESCRITURA ────────────────────────────────────────────
 * Registrar el mismo RelationType más de una vez reemplaza silenciosamente
 * el evaluador anterior. Este comportamiento es una decisión de diseño
 * explícita, no un defecto.
 *
 * Varios módulos pueden registrar el mismo evaluador genérico (por ejemplo,
 * AmbientDissipationEvaluator, HookeEvaluator, NewtonEvaluator) porque cada
 * módulo debe ser autónomo — no puede depender de que otro módulo esté
 * instalado para que sus relaciones tengan evaluador.
 *
 * La sobreescritura es inofensiva porque todos los módulos registran
 * instancias equivalentes del mismo evaluador. El resultado es siempre
 * el mismo independientemente del orden de instalación.
 *
 * Consecuencia deliberada: instalar un módulo dos veces produce el mismo
 * resultado que instalarlo una vez. El registro es idempotente respecto a
 * módulos equivalentes.
 *
 * ── EXTENSIBILIDAD ────────────────────────────────────────────────────────
 * Para reemplazar un evaluador del motor (personalización, mods):
 *   registry.register(RelationType.FOURIER, new MiFourierPersonalizado());
 * El último registro prevalece — esto permite a módulos de gameplay
 * sobreescribir comportamientos base sin alterar el módulo original.
 *
 * ── INVARIANTE ────────────────────────────────────────────────────────────
 *   ✗ No contiene lógica física.
 *   ✗ No conoce ningún evaluador concreto de dominio.
 *   ✓ Solo mapea RelationType a RelationEvaluator.
 */
public final class EvaluatorRegistry {

    private final Map<RelationType, RelationEvaluator> evaluators =
        new EnumMap<>(RelationType.class);

    // ── Constructor ───────────────────────────────────────────────────────

    /** Crea un registro vacío. */
    public EvaluatorRegistry() {}

    // ── Mutación ──────────────────────────────────────────────────────────

    /**
     * Registra el evaluador para un RelationType.
     *
     * Si ya existe un evaluador para ese tipo, es reemplazado. Ver sección
     * POLÍTICA DE SOBREESCRITURA en el Javadoc de clase.
     *
     * @param type      el tipo de relación. No puede ser null.
     * @param evaluator el evaluador especializado. No puede ser null.
     * @return this (para encadenado).
     */
    public EvaluatorRegistry register(RelationType type, RelationEvaluator evaluator) {
        if (type == null)      throw new IllegalArgumentException("type no puede ser null");
        if (evaluator == null) throw new IllegalArgumentException("evaluator no puede ser null");
        evaluators.put(type, evaluator);
        return this;
    }

    // ── Consulta ──────────────────────────────────────────────────────────

    /**
     * Retorna el evaluador registrado para el tipo dado.
     *
     * @param type el tipo de relación.
     * @return el evaluador, o null si no hay ninguno registrado para ese tipo.
     */
    public RelationEvaluator get(RelationType type) {
        if (type == null) return null;
        return evaluators.get(type);
    }

    /**
     * True si hay un evaluador registrado para el tipo dado.
     *
     * @param type el tipo de relación.
     * @return true si existe un evaluador.
     */
    public boolean has(RelationType type) {
        return type != null && evaluators.containsKey(type);
    }

    /** Número de evaluadores registrados. */
    public int size() { return evaluators.size(); }
}
