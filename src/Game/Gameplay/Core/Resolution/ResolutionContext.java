package Game.Gameplay.Core.Resolution;

import Game.Engine.GameObjects;
import Game.Gameplay.Core.Modifiers.ModifierContainer;
import Game.Gameplay.Core.Modifiers.ModifierComponent;
import Game.Gameplay.Core.Properties.PropertyComponent;
import Game.Gameplay.Core.Properties.PropertyKey;
import Game.Gameplay.Core.Properties.PropertyMap;

/**
 * Contexto de resolución — agrupa entidad, mapa de propiedades y modificadores
 * para simplificar las llamadas al PropertyResolver en sistemas de combate.
 *
 * ── PROBLEMA QUE RESUELVE ─────────────────────────────────────────────────
 * Sin este contexto, resolver una propiedad requiere tres llamadas
 * independientes para obtener los datos necesarios:
 *
 *   PropertyComponent pc = entity.getComponent(PropertyComponent.class);
 *   ModifierComponent mc = entity.getComponent(ModifierComponent.class);
 *   double dmg = PropertyResolver.resolve(
 *       pc != null ? pc.getMap() : new PropertyMap(),
 *       PropertyKeys.DAMAGE,
 *       mc != null ? mc.getContainer() : EMPTY
 *   );
 *
 * Con ResolutionContext:
 *   double dmg = ResolutionContext.of(entity).resolve(PropertyKeys.DAMAGE);
 *
 * ── USO TÍPICO EN SISTEMAS DE COMBATE ────────────────────────────────────
 *
 *   ResolutionContext ctx = ResolutionContext.of(attacker);
 *
 *   double damage  = ctx.resolve(PropertyKeys.DAMAGE);
 *   double speed   = ctx.resolve(PropertyKeys.SPEED);
 *   double critChance = ctx.resolve(PropertyKeys.CRITICAL_CHANCE);
 *
 * ── CREACIÓN BARATA ───────────────────────────────────────────────────────
 * ResolutionContext se puede crear en cada frame sin overhead significativo:
 * solo obtiene dos referencias de componente (O(n) lineal sobre componentes,
 * en práctica < 10 componentes por entidad). Los mísmos map y container son
 * reutilizados durante la vida del contexto.
 *
 * Para uso intensivo por frame (sistemas críticos de performance), cachear
 * el PropertyComponent y ModifierComponent en start() en lugar de crear el
 * contexto cada frame.
 */
public final class ResolutionContext {

    private static final PropertyMap        EMPTY_MAP       = new PropertyMap();
    private static final ModifierContainer  EMPTY_CONTAINER = new ModifierContainer();

    private final PropertyMap       map;
    private final ModifierContainer container;

    private ResolutionContext(PropertyMap map, ModifierContainer container) {
        this.map       = map;
        this.container = container;
    }

    // ── Factory methods ───────────────────────────────────────────────────

    /**
     * Crea un contexto de resolución para una entidad.
     *
     * Extrae PropertyComponent y ModifierComponent de la entidad.
     * Si alguno no existe, usa implementaciones vacías (nunca null).
     *
     * @param entity entidad sobre la que se resolverán propiedades
     * @return contexto listo para usar
     */
    public static ResolutionContext of(GameObjects entity) {
        PropertyComponent pc = entity.getComponent(PropertyComponent.class);
        ModifierComponent mc = entity.getComponent(ModifierComponent.class);

        PropertyMap       map       = pc != null ? pc.getMap()       : EMPTY_MAP;
        ModifierContainer container = mc != null ? mc.getContainer() : EMPTY_CONTAINER;

        return new ResolutionContext(map, container);
    }

    /**
     * Crea un contexto con map y container explícitos.
     * Útil en tests o cuando los componentes ya están extraídos.
     */
    public static ResolutionContext of(PropertyMap map, ModifierContainer container) {
        return new ResolutionContext(
            map       != null ? map       : EMPTY_MAP,
            container != null ? container : EMPTY_CONTAINER
        );
    }

    // ── Resolución ────────────────────────────────────────────────────────

    /**
     * Resuelve el valor final de una propiedad con el pipeline completo.
     * Sin clamp.
     *
     * @param key propiedad a resolver
     * @return valor final (base + aditivos) × multiplicativos, override si aplica
     */
    public double resolve(PropertyKey<?> key) {
        return PropertyResolver.resolve(map, key, container);
    }

    /**
     * Resuelve el valor final con clamp al rango [min, max].
     *
     * @param key propiedad a resolver
     * @param min valor mínimo del resultado
     * @param max valor máximo del resultado
     */
    public double resolveWithClamp(PropertyKey<?> key, double min, double max) {
        return PropertyResolver.resolveWithClamp(map, key, container, min, max);
    }

    /**
     * Resuelve la propiedad garantizando que el resultado es positivo (≥ 0).
     * Atajo habitual para propiedades de magnitud (daño, velocidad, cooldown).
     */
    public double resolvePositive(PropertyKey<?> key) {
        return PropertyResolver.resolveWithClamp(map, key, container, 0.0, Double.MAX_VALUE);
    }

    // ── Acceso directo ────────────────────────────────────────────────────

    /** Acceso al mapa de valores base por si se necesita leer el base puro. */
    public PropertyMap getMap()           { return map; }

    /** Acceso al contenedor de modificadores activos. */
    public ModifierContainer getContainer() { return container; }
}
