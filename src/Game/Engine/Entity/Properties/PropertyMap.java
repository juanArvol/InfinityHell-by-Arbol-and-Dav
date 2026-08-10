package Game.Engine.Entity.Properties;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Mapa de valores base de propiedades modificables para una entidad.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * PropertyMap almacena los valores BASE de cada propiedad. Los modificadores
 * y la resolución final son responsabilidad de PropertyResolver.
 *
 * La separación base/modificadores es fundamental: permite que el mismo
 * PropertyMap responda de manera diferente según los modificadores activos,
 * sin que la mutación de un modificador altere el valor base de la entidad.
 *
 *   PropertyMap (base)       → inmutable después de la construcción del objeto
 *   PropertyResolver (calc)  → calcula el valor final frame a frame
 *
 * ── IDENTIDAD DE CLAVE ───────────────────────────────────────────────────
 * El mapa interno es un IdentityHashMap, lo que significa que la identidad
 * de una PropertyKey ES SU REFERENCIA DE OBJETO, no su displayName().
 *
 * Esto garantiza que:
 *   - PropertyKeys.DAMAGE y PropertyKeys.DAMAGE son la misma clave (misma instancia).
 *   - Un PropertyKey.of("Damage", ...) creado ad-hoc nunca colisiona con
 *     PropertyKeys.DAMAGE aunque tenga el mismo displayName.
 *   - Es imposible acceder a una propiedad con un string mágico.
 *
 * ── USO ──────────────────────────────────────────────────────────────────
 *
 *   // Durante la construcción de una entidad:
 *   PropertyMap props = new PropertyMap();
 *   props.setBase(PropertyKeys.DAMAGE, 25.0);
 *   props.setBase(PropertyKeys.SPEED,  3.5);
 *
 *   // Consulta directa del valor base (sin modificadores):
 *   double baseDamage = props.getBase(PropertyKeys.DAMAGE);
 *
 *   // Consulta con resolución completa (con modificadores):
 *   double finalDamage = PropertyResolver.resolve(props, PropertyKeys.DAMAGE, container);
 *
 * ── THREAD SAFETY ────────────────────────────────────────────────────────
 * PropertyMap no es thread-safe. Modificar desde el game loop thread únicamente.
 */
public final class PropertyMap {

    /**
     * Almacenamiento interno. La clave es la instancia de PropertyKey.
     * IdentityHashMap usa == para comparar claves, garantizando que la
     * identidad sea la referencia de objeto y no el displayName().
     */
    private final IdentityHashMap<PropertyKey<?>, Double> values = new IdentityHashMap<>();

    // ── Escritura ─────────────────────────────────────────────────────────

    /**
     * Establece el valor base de una propiedad.
     *
     * @param key   clave de la propiedad (debe ser una constante estática)
     * @param value valor base (sin modificadores)
     */
    public <T> void setBase(PropertyKey<T> key, double value) {
        values.put(key, value);
    }

    /**
     * Elimina una propiedad del mapa.
     * Tras la eliminación, getBase() retornará el valor por defecto de la clave.
     */
    public <T> void remove(PropertyKey<T> key) {
        values.remove(key);
    }

    // ── Lectura ───────────────────────────────────────────────────────────

    /**
     * Retorna el valor base de una propiedad.
     * Si la clave no está registrada, retorna el valor por defecto de la clave.
     *
     * La comparación es por referencia de instancia (IdentityHashMap),
     * no por displayName().
     *
     * @param key clave de la propiedad
     * @return valor base registrado, o key.defaultValue() si no existe
     */
    public <T> double getBase(PropertyKey<T> key) {
        Double v = values.get(key);
        Object def = key.defaultValue();
        return v != null ? v : (def instanceof Number n ? n.doubleValue() : 0.0);
    }

    /**
     * True si esta propiedad tiene un valor base explícitamente registrado.
     * La comparación es por referencia de instancia.
     */
    public <T> boolean has(PropertyKey<T> key) {
        return values.containsKey(key);
    }

    /**
     * Vista no modificable de todas las claves registradas en este mapa.
     * Las claves retornadas son las instancias de PropertyKey, no strings.
     */
    public Set<PropertyKey<?>> registeredKeys() {
        return Collections.unmodifiableSet(values.keySet());
    }

    /**
     * Número de propiedades registradas en este mapa.
     */
    public int size() {
        return values.size();
    }
}
