package Game.Engine.Lifecycle;

import java.util.Arrays;
import java.util.List;

/**
 * Contexto compuesto — activo si CUALQUIERA de sus contextos está activo.
 *
 * ── SEMÁNTICA OR ──────────────────────────────────────────────────────────
 *
 * Una entidad puede mantenerse viva por múltiples razones independientes.
 * CompositeEntityContext modela esto: la entidad continúa simulándose
 * mientras al menos uno de sus contextos permanezca activo.
 *
 * Ejemplo clásico:
 *
 *   Boss (Sans)
 *     ├── CombatContext    → activo mientras hay combate en curso
 *     └── RegionContext    → activo mientras la región está cargada
 *
 *   El boss continúa simulándose si CUALQUIERA de los dos está activo.
 *   Solo se destruye cuando TODOS los contextos están inactivos.
 *
 * ── USO ───────────────────────────────────────────────────────────────────
 *
 *   EntityContext ctx = CompositeEntityContext.any(
 *       combatContext,
 *       regionContext
 *   );
 *
 *   // Cada frame:
 *   if (!ctx.isActive()) destroyEntity();
 */
public final class CompositeEntityContext implements EntityContext {

    private final List<EntityContext> contexts;

    /**
     * Construye un contexto compuesto a partir de una lista de contextos.
     *
     * @param contexts lista de contextos a combinar (no null, no vacío)
     */
    public CompositeEntityContext(List<EntityContext> contexts) {
        if (contexts == null || contexts.isEmpty())
            throw new IllegalArgumentException("CompositeEntityContext requiere al menos un contexto");
        this.contexts = List.copyOf(contexts);
    }

    /**
     * Factory estático — sintaxis conveniente para combinar contextos.
     *
     * @param contexts contextos a combinar
     * @return CompositeEntityContext que es activo si cualquiera lo está
     */
    public static CompositeEntityContext any(EntityContext... contexts) {
        return new CompositeEntityContext(Arrays.asList(contexts));
    }

    // ── EntityContext ─────────────────────────────────────────────────────

    /**
     * Activo si al menos uno de los contextos componentes está activo.
     * Short-circuit: retorna true en el primer contexto activo encontrado.
     */
    @Override
    public boolean isActive() {
        for (EntityContext ctx : contexts) {
            if (ctx.isActive()) return true;
        }
        return false;
    }
}
