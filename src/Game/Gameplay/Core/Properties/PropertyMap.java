package Game.Gameplay.Core.Properties;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Mapa de valores base de propiedades de gameplay para una entidad.
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
 * ── USO ──────────────────────────────────────────────────────────────────
 *
 *   // Durante la construcción de una entidad:
 *   PropertyMap props = new PropertyMap();
 *   props.setBase(PropertyKeys.DAMAGE, 25.0);
 *   props.setBase(PropertyKeys.SPEED,  3.5);
 *   props.setBase(PropertyKeys.LIFETIME, 120.0);
 *
 *   // Consulta directa del valor base (sin modificadores):
 *   double baseDamage = props.getBase(PropertyKeys.DAMAGE);
 *
 *   // Consulta con resolución completa (con modificadores):
 *   double finalDamage = PropertyResolver.resolve(props, PropertyKeys.DAMAGE, modifiers);
 *
 * ── THREAD SAFETY ────────────────────────────────────────────────────────
 * PropertyMap no es thread-safe. Modificar desde el game loop thread únicamente.
 */
public final class PropertyMap {

    /**
     * Almacenamiento interno. La clave es el id() de PropertyKey para
     * evitar problemas de tipo genérico en el mapa.
     */
    private final Map<String, Double> values = new HashMap<>();

    // ── Escritura ─────────────────────────────────────────────────────────

    /**
     * Establece el valor base de una propiedad.
     *
     * @param key   clave de la propiedad
     * @param value valor base (sin modificadores)
     */
    public <T> void setBase(PropertyKey<T> key, double value) {
        values.put(key.id(), value);
    }

    /**
     * Elimina una propiedad del mapa.
     * Tras la eliminación, get() retornará el valor por defecto de la clave.
     */
    public <T> void remove(PropertyKey<T> key) {
        values.remove(key.id());
    }

    // ── Lectura ───────────────────────────────────────────────────────────

    /**
     * Retorna el valor base de una propiedad.
     * Si la clave no está registrada, retorna el valor por defecto de la clave.
     *
     * @param key clave de la propiedad
     * @return valor base registrado, o key.defaultValue() si no existe
     */
    public <T> double getBase(PropertyKey<T> key) {
        Double v = values.get(key.id());
        Object def = key.defaultValue();
        return v != null ? v : (def instanceof Number n ? n.doubleValue() : 0.0);
    }

    /**
     * True si esta propiedad tiene un valor base explícitamente registrado.
     */
    public <T> boolean has(PropertyKey<T> key) {
        return values.containsKey(key.id());
    }

    /**
     * Vista no modificable de todas las claves registradas en este mapa.
     * Retorna los IDs de las claves presentes.
     */
    public Set<String> registeredKeys() {
        return Collections.unmodifiableSet(values.keySet());
    }

    /**
     * Número de propiedades registradas en este mapa.
     */
    public int size() {
        return values.size();
    }
}
