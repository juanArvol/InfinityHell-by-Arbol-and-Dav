package Game.Engine.Lifecycle;

/**
 * Contrato de contexto para una entidad simulada.
 *
 * ── MOTIVACIÓN ────────────────────────────────────────────────────────────
 *
 * El ciclo de vida de una entidad no debe depender únicamente de su
 * distancia física al jugador o de un flag "isAlive". Debe depender de
 * si existe algún contexto que la mantenga relevante para la simulación.
 *
 * Regla conceptual:
 *
 *   Entidad
 *      ↓
 *   ¿Tiene un contexto relevante?
 *      ├── Sí → continúa simulándose
 *      └── No → puede ser despachada/destruida
 *
 * ── EJEMPLOS ──────────────────────────────────────────────────────────────
 *
 *   Bullet → su contexto es "está en vuelo" (lifeTicks > 0 && dentro de mundo).
 *            Sin contexto (vida agotada): se destruye.
 *
 *   Boss (Sans) → su contexto es "hay un combate activo".
 *                 Aunque el jugador esté muy lejos, si el contexto de combate
 *                 sigue activo, el boss continúa simulándose.
 *
 *   Enemy → su contexto es "está en una región activa del mundo".
 *           Sin contexto (región descargada): se pausa o destruye.
 *
 * ── SEPARACIÓN DE RESPONSABILIDADES ──────────────────────────────────────
 *
 *   EntityContext         → ¿el contexto está activo?
 *   Destroyable           → ¿el objeto debe ser eliminado del mundo?
 *   SimulationLifecycle   → conecta EntityContext con la decisión de simular
 *
 * EntityContext responde solo a "¿tengo razón para seguir existiendo?".
 * La decisión de qué hacer cuando no hay contexto es responsabilidad del
 * sistema que gestiona el ciclo de vida (SimulationLifecycle, LifecycleSystem).
 *
 * ── IMPLEMENTACIONES ──────────────────────────────────────────────────────
 *
 *   AlwaysActiveContext      → always returns true (objetos permanentes).
 *   LifetimeContext          → activo mientras queden ticks de vida.
 *   CombatContext            → activo mientras hay un combate en curso.
 *   RegionContext            → activo mientras la región está cargada.
 *   CompositeEntityContext   → activo si CUALQUIERA de sus contextos está activo.
 */
public interface EntityContext {

    /**
     * Retorna true si este contexto está activo — la entidad tiene razón
     * para seguir siendo simulada.
     *
     * Retorna false si el contexto dejó de ser relevante — la entidad
     * puede ser destruida o pausada por el LifecycleSystem.
     *
     * @return true si el contexto sigue activo
     */
    boolean isActive();

    // ── Implementaciones predefinidas ─────────────────────────────────────

    /**
     * Contexto siempre activo.
     * Para entidades permanentes (Player, bosses con fase de combate explícita).
     */
    EntityContext ALWAYS_ACTIVE = () -> true;

    /**
     * Contexto siempre inactivo.
     * Útil para marcar entidades que deben destruirse en el próximo ciclo.
     */
    EntityContext INACTIVE = () -> false;
}
