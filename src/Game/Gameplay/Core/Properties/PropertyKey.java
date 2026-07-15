package Game.Gameplay.Core.Properties;

/**
 * Clave tipada que identifica una propiedad de gameplay.
 *
 * ── PROBLEMA QUE RESUELVE ─────────────────────────────────────────────────
 * En el código actual, cada clase expone sus propiedades como campos o métodos
 * específicos: Bullet.damage, PlayerPhysics.speedMaxPiso, WeaponStats.cooldown.
 * No hay forma de preguntar genéricamente "¿cuál es el daño de esta entidad?"
 * sin saber su tipo concreto.
 *
 * PropertyKey permite identificar propiedades sin conocer el tipo portador:
 *
 *   PropertyMap props = entity.getComponent(PropertyComponent.class).getMap();
 *   double damage = props.get(PropertyKeys.DAMAGE);
 *
 * ── TIPADO ────────────────────────────────────────────────────────────────
 * El parámetro de tipo T documenta la semántica de la propiedad. En la práctica
 * actual todas las propiedades son double (magnitudes numéricas). T = Double
 * en PropertyKeys. La flexibilidad de T está preparada para propiedades no
 * numéricas futuras (Color, String, booleano, etc.).
 *
 * ── IDENTIDAD ────────────────────────────────────────────────────────────
 * Dos PropertyKey son iguales si tienen el mismo id(). Usar constantes estáticas
 * en PropertyKeys en lugar de instanciar nuevas claves en cada uso.
 *
 * ── EXTENSIBILIDAD ────────────────────────────────────────────────────────
 * Nuevos sistemas declaran sus propias claves sin modificar PropertyKeys base:
 *
 *   // En un módulo de magia:
 *   public static final PropertyKey<Double> MANA_COST =
 *       PropertyKey.of("SpellManaCost", Double.class, 0.0);
 *
 * @param <T> tipo del valor de la propiedad.
 */
public final class PropertyKey<T> {

    private final String id;
    private final Class<T> type;
    private final T defaultValue;

    private PropertyKey(String id, Class<T> type, T defaultValue) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El ID de una propiedad no puede ser nulo o vacío.");
        }
        this.id           = id;
        this.type         = type;
        this.defaultValue = defaultValue;
    }

    /**
     * Crea una nueva clave de propiedad.
     *
     * @param id           identificador único, por ejemplo "Damage"
     * @param type         clase del tipo de valor, por ejemplo Double.class
     * @param defaultValue valor que retorna PropertyMap cuando la clave no está registrada
     */
    public static <T> PropertyKey<T> of(String id, Class<T> type, T defaultValue) {
        return new PropertyKey<>(id, type, defaultValue);
    }

    // ── Acceso ────────────────────────────────────────────────────────────

    /** Identificador único de esta propiedad. */
    public String id() { return id; }

    /** Clase del tipo del valor. */
    public Class<T> type() { return type; }

    /**
     * Valor por defecto cuando la propiedad no está definida en un PropertyMap.
     * Permite usar get() sin null-checks en la mayoría de los casos.
     */
    public T defaultValue() { return defaultValue; }

    // ── Object ────────────────────────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PropertyKey<?> k)) return false;
        return id.equals(k.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "PropertyKey[" + id + ":" + type.getSimpleName() + "]";
    }
}
