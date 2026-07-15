package Game.Gameplay.Core.Modifiers;

import Game.Engine.Component;

/**
 * Componente que otorga a una entidad la capacidad de recibir y acumular
 * modificadores de propiedades activos.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * ModifierComponent es el punto de acceso al ModifierContainer de una entidad.
 * Cualquier sistema que quiera aplicar un modificador a una entidad lo hace
 * a través de este componente, sin necesidad de conocer el tipo de la entidad.
 *
 * ── USO ──────────────────────────────────────────────────────────────────
 *
 *   // En el constructor de una entidad que puede recibir modificadores:
 *   addComponent(new ModifierComponent());
 *
 *   // Desde un sistema de buffs (sin saber si es Player, Enemy, etc.):
 *   ModifierComponent mc = entity.getComponent(ModifierComponent.class);
 *   if (mc != null) {
 *       mc.add(PropertyModifier.multiplicative(PropertyKeys.SPEED, 1.3, "haste_rune"));
 *   }
 *
 *   // Cuando el buff expira:
 *   if (mc != null) {
 *       mc.removeBySource("haste_rune");
 *   }
 *
 *   // En el sistema de combate, para calcular el daño final:
 *   ModifierComponent mc = attacker.getComponent(ModifierComponent.class);
 *   ModifierContainer container = (mc != null) ? mc.getContainer() : EMPTY;
 *   double finalDamage = PropertyResolver.resolve(props, PropertyKeys.DAMAGE, container);
 *
 * ── RELACIÓN CON CoreCapabilities.CAN_RECEIVE_MODIFIERS ──────────────────
 * ModifierComponent puede añadirse a cualquier entidad. La capacidad
 * CAN_RECEIVE_MODIFIERS en CapabilityComponent indica INTENCIÓN: "este
 * sistema de entidades acepta modificadores externos". ModifierComponent
 * es la IMPLEMENTACIÓN de esa capacidad.
 *
 * Lo correcto es añadir ambos juntos:
 *   addComponent(new ModifierComponent());
 *   caps.add(CoreCapabilities.CAN_RECEIVE_MODIFIERS);
 */
public final class ModifierComponent extends Component {

    private final ModifierContainer container;

    /** Contenedor vacío compartido para entidades sin modificadores. Evita null checks. */
    private static final ModifierContainer EMPTY_CONTAINER = new ModifierContainer();

    public ModifierComponent() {
        this.container = new ModifierContainer();
    }

    // ── Shortcuts ─────────────────────────────────────────────────────────

    /**
     * Añade un modificador al contenedor de esta entidad.
     */
    public void add(PropertyModifier modifier) {
        container.add(modifier);
    }

    /**
     * Elimina todos los modificadores de la fuente indicada.
     */
    public void removeBySource(String sourceId) {
        container.removeBySource(sourceId);
    }

    // ── Acceso al contenedor ──────────────────────────────────────────────

    /**
     * Retorna el ModifierContainer de esta entidad para uso con PropertyResolver.
     */
    public ModifierContainer getContainer() {
        return container;
    }

    // ── Utilitario estático ───────────────────────────────────────────────

    /**
     * Retorna el ModifierContainer de una entidad, o un contenedor vacío
     * compartido si la entidad no tiene ModifierComponent.
     *
     * Permite llamar PropertyResolver.resolve() sin null-checks:
     *
     *   double dmg = PropertyResolver.resolve(
     *       props.getMap(),
     *       PropertyKeys.DAMAGE,
     *       ModifierComponent.containerOf(attacker)
     *   );
     *
     * @param entity entidad cuyo ModifierComponent se quiere obtener
     * @return el contenedor activo, o un contenedor vacío inmutable
     */
    public static ModifierContainer containerOf(Game.Engine.GameObjects entity) {
        ModifierComponent mc = entity.getComponent(ModifierComponent.class);
        return mc != null ? mc.getContainer() : EMPTY_CONTAINER;
    }
}
