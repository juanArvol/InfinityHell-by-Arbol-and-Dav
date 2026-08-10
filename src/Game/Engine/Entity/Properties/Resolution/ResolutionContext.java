package Game.Engine.Entity.Properties.Resolution;

import Game.Engine.Entity.Properties.Modifier.PropertyModifierComponent;
import Game.Engine.Entity.Properties.Modifier.PropertyModifierContainer;
import Game.Engine.Entity.Properties.PropertyComponent;
import Game.Engine.Entity.Properties.PropertyKey;
import Game.Engine.Entity.Properties.PropertyMap;
import Game.Engine.GameObjects;

/**
 * Contexto de resolución — agrupa entidad, mapa de propiedades y modificadores
 * para simplificar las llamadas al PropertyResolver en sistemas de combate.
 *
 * ── PROBLEMA QUE RESUELVE ─────────────────────────────────────────────────
 * Sin este contexto, resolver una propiedad requiere tres llamadas
 * independientes para obtener los datos necesarios.
 *
 * Con ResolutionContext:
 *   double dmg = ResolutionContext.of(entity).resolve(PropertyKeys.DAMAGE);
 *
 * ── CREACIÓN BARATA ───────────────────────────────────────────────────────
 * ResolutionContext se puede crear en cada frame sin overhead significativo:
 * solo obtiene dos referencias de componente. Los mísmos map y container son
 * reutilizados durante la vida del contexto.
 */
public final class ResolutionContext {

    private static final PropertyMap                EMPTY_MAP       = new PropertyMap();
    private static final PropertyModifierContainer  EMPTY_CONTAINER = new PropertyModifierContainer();

    private final PropertyMap               map;
    private final PropertyModifierContainer container;

    private ResolutionContext(PropertyMap map, PropertyModifierContainer container) {
        this.map       = map;
        this.container = container;
    }

    // ── Factory methods ───────────────────────────────────────────────────

    /**
     * Crea un contexto de resolución para una entidad.
     * Extrae PropertyComponent y PropertyModifierComponent de la entidad.
     * Si alguno no existe, usa implementaciones vacías (nunca null).
     */
    public static ResolutionContext of(GameObjects entity) {
        PropertyComponent pc = entity.getComponent(PropertyComponent.class);
        PropertyModifierComponent mc = entity.getComponent(PropertyModifierComponent.class);

        PropertyMap               map       = pc != null ? pc.getMap()       : EMPTY_MAP;
        PropertyModifierContainer container = mc != null ? mc.getContainer() : EMPTY_CONTAINER;

        return new ResolutionContext(map, container);
    }

    /** Crea un contexto con map y container explícitos. */
    public static ResolutionContext of(PropertyMap map, PropertyModifierContainer container) {
        return new ResolutionContext(
            map       != null ? map       : EMPTY_MAP,
            container != null ? container : EMPTY_CONTAINER
        );
    }

    // ── Resolución ────────────────────────────────────────────────────────

    /** Resuelve el valor final de una propiedad con el pipeline completo. Sin clamp. */
    public double resolve(PropertyKey<?> key) {
        return PropertyResolver.resolve(map, key, container);
    }

    /** Resuelve el valor final con clamp al rango [min, max]. */
    public double resolveWithClamp(PropertyKey<?> key, double min, double max) {
        return PropertyResolver.resolveWithClamp(map, key, container, min, max);
    }

    /** Resuelve la propiedad garantizando que el resultado es positivo (≥ 0). */
    public double resolvePositive(PropertyKey<?> key) {
        return PropertyResolver.resolveWithClamp(map, key, container, 0.0, Double.MAX_VALUE);
    }

    // ── Acceso directo ────────────────────────────────────────────────────

    public PropertyMap getMap()                       { return map; }
    public PropertyModifierContainer getContainer()   { return container; }
}
