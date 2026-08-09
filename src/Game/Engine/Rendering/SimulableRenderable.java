package Game.Engine.Rendering;

/**
 * Abstracción para entidades que pueden tener ciclos de simulación y render
 * independientes.
 *
 * ── MOTIVACIÓN ────────────────────────────────────────────────────────────
 *
 * En un engine naive, el render y la simulación están acoplados: si un objeto
 * no es visible, deja de existir. Esto es incorrecto para entidades que deben
 * continuar simulándose aunque no se rendericen.
 *
 * SimulableRenderable separa explícitamente ambas responsabilidades:
 *
 *   SIMULATION → continuar si el contexto lo justifica
 *   RENDER     → omitirse cuando no corresponde (fuera de viewport, ocluido)
 *
 * ── REGLAS ────────────────────────────────────────────────────────────────
 *
 *   isRenderVisible() == false → skip render, pero NO destruir.
 *   shouldSimulate()  == false → NO simular (la entidad puede destruirse).
 *
 * La visibilidad de render NO determina por sí misma si una entidad sigue
 * existiendo o simulándose.
 *
 * ── EJEMPLOS ──────────────────────────────────────────────────────────────
 *
 *   Bullet fuera de viewport:
 *     - isRenderVisible() → false (culling de viewport ya existente)
 *     - shouldSimulate()  → true mientras tenga vida
 *     La bala sigue moviéndose y colisionando aunque no se vea.
 *
 *   Boss Sans en sala contigua (jugador no lo ve):
 *     - isRenderVisible() → false
 *     - shouldSimulate()  → true (CombatContext activo)
 *     Sans sigue moviéndose y disparando aunque el jugador no lo vea.
 *
 *   Enemigo en chunk descargado:
 *     - isRenderVisible() → false
 *     - shouldSimulate()  → false (RegionContext inactivo)
 *     El enemigo se pausa o destruye.
 *
 * ── RELACIÓN CON SimulationLifecycle ─────────────────────────────────────
 *
 * SimulationLifecycle (Engine.Lifecycle) también declara shouldSimulate().
 * Si una entidad implementa ambas interfaces, puede delegar directamente:
 *
 *   {@literal @}Override
 *   public boolean shouldSimulate() {
 *       return getSimulationContext().isActive();
 *   }
 *
 * En ese caso, SimulableRenderable aporta isRenderVisible() y SimulationLifecycle
 * aporta el EntityContext. No hay conflicto de semántica — ambos métodos
 * shouldSimulate() expresan lo mismo; el compilador requerirá una implementación
 * explícita que resuelva la ambigüedad entre el default de SimulationLifecycle
 * y la ausencia de default en SimulableRenderable.
 *
 * ── RELACIÓN CON Renderable ───────────────────────────────────────────────
 *
 * Renderable (ya existente en Engine.RenderEngine.Contracts) es el contrato
 * de "cómo dibujarse". SimulableRenderable añade la dimensión de "cuándo
 * dibujarse" y la desacopla de "cuándo simularse".
 *
 * Una entidad puede implementar ambas interfaces. El RenderSystem consulta
 * isRenderVisible() antes de llamar render() para omitir el dibujado sin
 * destruir la entidad.
 *
 * ── IMPLEMENTACIÓN POR DEFECTO ────────────────────────────────────────────
 *
 * isRenderVisible() retorna true por defecto — no rompe comportamiento de
 * entidades que no implementen la lógica de culling propia.
 *
 * shouldSimulate() no tiene default — la entidad debe decidir explícitamente
 * si sigue simulándose según su contexto.
 */
public interface SimulableRenderable {

    /**
     * Retorna true si esta entidad debe renderizarse este frame.
     *
     * false → el RenderSystem omite el render de esta entidad.
     * NO implica destrucción ni pausa de simulación.
     *
     * Implementaciones típicas:
     *   - Culling de viewport: retorna false si está fuera de pantalla.
     *   - Culling de distancia: retorna false si está muy lejos.
     *   - Efectos invisibles: retorna false siempre (entidad lógica sin visual).
     *
     * Default: true — siempre visible (comportamiento original sin cambios).
     *
     * @return true si debe renderizarse
     */
    default boolean isRenderVisible() {
        return true;
    }

    /**
     * Retorna true si esta entidad debe seguir siendo simulada este frame.
     *
     * false → la entidad puede detenerse o destruirse según la política
     * del sistema de lifecycle.
     *
     * La decisión de qué hacer cuando shouldSimulate() retorna false
     * es del sistema que gestiona el ciclo de vida (LifecycleSystem,
     * DynamicEntityRegistry), no de esta interfaz.
     *
     * @return true si la simulación debe continuar
     */
    boolean shouldSimulate();
}
