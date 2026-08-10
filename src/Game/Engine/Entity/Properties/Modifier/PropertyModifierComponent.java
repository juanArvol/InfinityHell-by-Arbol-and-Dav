package Game.Engine.Entity.Properties.Modifier;

import Game.Engine.Component;
import Game.Engine.GameObjects;

/**
 * Componente que otorga a una entidad la capacidad de recibir y acumular
 * PropertyModifier activos.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * PropertyModifierComponent es el punto de acceso al PropertyModifierContainer
 * de una entidad. Cualquier sistema que quiera aplicar un modificador de
 * propiedad a una entidad lo hace a través de este componente.
 *
 * NOTA DE NAMING: la clase se llama PropertyModifierComponent para evitar
 * conflicto con el sistema de stats RPG del Engine (ModifierComponent en Stats).
 *
 * ── USO ──────────────────────────────────────────────────────────────────
 *
 *   // En el constructor de una entidad que puede recibir modificadores:
 *   addComponent(new PropertyModifierComponent());
 *
 *   // Desde un sistema de buffs:
 *   PropertyModifierComponent mc = entity.getComponent(PropertyModifierComponent.class);
 *   if (mc != null) {
 *       mc.add(PropertyModifier.multiplicative(PropertyKeys.SPEED, 1.3, "haste_rune"));
 *   }
 *
 *   // Cuando el buff expira:
 *   if (mc != null) mc.removeBySource("haste_rune");
 */
public final class PropertyModifierComponent extends Component {

    private final PropertyModifierContainer container;

    private static final PropertyModifierContainer EMPTY_CONTAINER = new PropertyModifierContainer();

    public PropertyModifierComponent() {
        this.container = new PropertyModifierContainer();
    }

    // ── Shortcuts ─────────────────────────────────────────────────────────

    public void add(PropertyModifier modifier) {
        container.add(modifier);
    }

    public void removeBySource(String sourceId) {
        container.removeBySource(sourceId);
    }

    public PropertyModifierContainer getContainer() {
        return container;
    }

    // ── Utilitario estático ───────────────────────────────────────────────

    /**
     * Retorna el PropertyModifierContainer de una entidad, o un contenedor
     * vacío compartido si la entidad no tiene PropertyModifierComponent.
     *
     * Permite llamar PropertyResolver.resolve() sin null-checks.
     */
    public static PropertyModifierContainer containerOf(GameObjects entity) {
        PropertyModifierComponent mc = entity.getComponent(PropertyModifierComponent.class);
        return mc != null ? mc.getContainer() : EMPTY_CONTAINER;
    }
}
