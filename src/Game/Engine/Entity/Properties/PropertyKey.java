package Game.Engine.Entity.Properties;

/**
 * Clave tipada que identifica una propiedad modificable de una entidad.
 *
 * ── IDENTIDAD ────────────────────────────────────────────────────────────
 * La identidad de un PropertyKey ES EL OBJETO MISMO, no su nombre textual.
 *
 * Dos PropertyKey son idénticos si y solo si son la misma instancia Java.
 * Esto se garantiza declarando todas las claves como constantes estáticas
 * en catálogos (PropertyKeys, SpellPropertyKeys, etc.) y nunca instanciando
 * nuevas claves en el punto de uso.
 *
 * La comparación correcta es:
 *
 *   key == PropertyKeys.DAMAGE          // identidad de instancia
 *   map.has(PropertyKeys.DAMAGE)        // delega a IdentityHashMap
 *
 * NUNCA:
 *   key.displayName().equals("Damage")  // eso es comparar representación textual
 *
 * ── REPRESENTACIÓN TEXTUAL ───────────────────────────────────────────────
 * displayName() existe únicamente para:
 *   - logging y debug
 *   - serialización externa
 *   - herramientas de inspección
 *
 * No participa en equals, hashCode ni en ninguna colección interna.
 *
 * ── TIPADO ────────────────────────────────────────────────────────────────
 * El parámetro de tipo T documenta el tipo del valor de la propiedad.
 * En la práctica actual todas las propiedades numéricas usan T = Double.
 * La flexibilidad de T está preparada para propiedades no numéricas futuras.
 *
 * ── EXTENSIBILIDAD ────────────────────────────────────────────────────────
 * Nuevos sistemas declaran sus propias claves sin modificar PropertyKeys:
 *
 *   // En un módulo de magia:
 *   public static final PropertyKey<Double> MANA_COST =
 *       PropertyKey.of("SpellManaCost", Double.class, 0.0);
 *
 * @param <T> tipo del valor de la propiedad.
 */
public final class PropertyKey<T> {

    // ── Metadata de representación (no es identidad) ──────────────────────

    private final String  displayName;
    private final Class<T> type;
    private final T        defaultValue;

    private PropertyKey(String displayName, Class<T> type, T defaultValue) {
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException(
                "El displayName de una PropertyKey no puede ser nulo o vacío.");
        }
        this.displayName  = displayName;
        this.type         = type;
        this.defaultValue = defaultValue;
    }

    /**
     * Crea una nueva clave de propiedad.
     *
     * Cada llamada a este método produce una instancia distinta con identidad
     * propia. Usar exclusivamente para declarar constantes estáticas.
     *
     * @param displayName  nombre legible para logs/debug, por ejemplo "Damage"
     * @param type         clase del tipo de valor, por ejemplo Double.class
     * @param defaultValue valor que retorna PropertyMap cuando la clave no está registrada
     */
    public static <T> PropertyKey<T> of(String displayName, Class<T> type, T defaultValue) {
        return new PropertyKey<>(displayName, type, defaultValue);
    }

    // ── Metadata de representación ────────────────────────────────────────

    /**
     * Nombre legible de esta propiedad, para logging, debug y serialización.
     * NO es la identidad de la clave. No usar como clave en colecciones.
     */
    public String displayName() { return displayName; }

    /**
     * Clase del tipo del valor.
     */
    public Class<T> type() { return type; }

    /**
     * Valor por defecto cuando la propiedad no está definida en un PropertyMap.
     * Permite usar getBase() sin null-checks en la mayoría de los casos.
     */
    public T defaultValue() { return defaultValue; }

    // ── Identidad: basada en instancia, no en String ──────────────────────
    //
    // equals y hashCode NO se sobreescriben.
    // La JVM usa identidad de objeto (referencia) por defecto, que es
    // exactamente lo que queremos: dos claves son iguales si y solo si
    // son la misma instancia.
    //
    // IdentityHashMap en PropertyMap aprovecha esto directamente.

    @Override
    public String toString() {
        return "PropertyKey[" + displayName + ":" + type.getSimpleName() + "]";
    }
}
