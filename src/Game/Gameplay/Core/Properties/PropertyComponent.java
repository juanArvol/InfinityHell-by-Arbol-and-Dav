package Game.Gameplay.Core.Properties;

import Game.Engine.Component;

/**
 * Componente que otorga un conjunto de propiedades modificables a una entidad.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * PropertyComponent es el punto de acceso al PropertyMap de una entidad.
 * Expone el mapa de valores base y un mecanismo de acceso rápido para
 * consultas directas sin pasar por PropertyResolver.
 *
 * ── USO ──────────────────────────────────────────────────────────────────
 *
 *   // En el constructor de una entidad:
 *   PropertyComponent props = new PropertyComponent();
 *   props.set(PropertyKeys.DAMAGE, 30.0);
 *   props.set(PropertyKeys.SPEED, 2.5);
 *   props.set(PropertyKeys.LIFETIME, 90.0);
 *   addComponent(props);
 *
 *   // Acceso desde un sistema externo (sin saber el tipo concreto):
 *   PropertyComponent props = entity.getComponent(PropertyComponent.class);
 *   if (props != null) {
 *       double baseDmg = props.getBase(PropertyKeys.DAMAGE);
 *   }
 *
 *   // Resolución con modificadores (ej: desde un sistema de combate):
 *   double finalDmg = PropertyResolver.resolve(
 *       props.getMap(), PropertyKeys.DAMAGE, activeModifiers
 *   );
 *
 * ── POR QUÉ EXISTE COMO COMPONENTE ───────────────────────────────────────
 * Usar el sistema de componentes del Engine hace que PropertyComponent sea
 * consultable desde cualquier sistema que solo conozca GameObjects:
 *
 *   entity.getComponent(PropertyComponent.class)
 *
 * No hay que saber si es Player, Enemy, Bullet o Spell.
 */
public final class PropertyComponent extends Component {

    private final PropertyMap map;

    public PropertyComponent() {
        this.map = new PropertyMap();
    }

    /** Constructor con un PropertyMap preconfigurado. */
    public PropertyComponent(PropertyMap map) {
        this.map = map;
    }

    // ── Acceso al mapa ────────────────────────────────────────────────────

    /**
     * Retorna el PropertyMap subyacente para uso con PropertyResolver.
     */
    public PropertyMap getMap() {
        return map;
    }

    // ── Shortcuts ─────────────────────────────────────────────────────────

    /**
     * Establece el valor base de una propiedad.
     * Atajo para no tener que llamar getMap().setBase() en cada línea.
     */
    public void set(PropertyKey<?> key, double value) {
        map.setBase(key, value);
    }

    /**
     * Retorna el valor base de una propiedad, sin aplicar modificadores.
     * Para el valor resuelto usar PropertyResolver.resolve().
     */
    public double getBase(PropertyKey<?> key) {
        return map.getBase(key);
    }

    /**
     * True si esta entidad tiene la propiedad registrada explícitamente.
     */
    public boolean has(PropertyKey<?> key) {
        return map.has(key);
    }
}
