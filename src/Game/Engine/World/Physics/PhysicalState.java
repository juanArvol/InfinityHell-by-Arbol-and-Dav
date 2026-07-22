package Game.Engine.World.Physics;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fuente de verdad del estado físico de un objeto.
 *
 * ── HRFC-020 — Consolidación Definitiva del Modelo Declarativo ────────────
 *
 * ── FILOSOFÍA ─────────────────────────────────────────────────────────────
 * PhysicalState es la única fuente de verdad del estado físico de un objeto.
 *
 * Toda propiedad es identificada exclusivamente mediante un PropertyDescriptor.
 * No existe ningún mecanismo de acceso por String.
 * No pueden existir duplicados semánticos: la misma magnitud física no puede
 * aparecer bajo dos nombres distintos porque el identificador ES el descriptor,
 * no el texto que contiene.
 *
 * ── ESTRUCTURA ────────────────────────────────────────────────────────────
 * PhysicalState utiliza dos mapas indexados por identidad de descriptor:
 *
 *   values      → IdentityHashMap<PropertyDescriptor, Double>
 *                 Los valores actuales de cada propiedad.
 *                 Indexado por identidad (referencia) del descriptor,
 *                 no por el contenido del id.
 *
 *   descriptors → Map<PropertyDescriptor, PropertyDescriptor> (ordered set)
 *                 Registro canónico de todas las propiedades registradas.
 *                 Usado para introspección y para el Builder.
 *
 * ── POR QUÉ IdentityHashMap ───────────────────────────────────────────────
 * El uso de identidad de referencia (==) como clave garantiza que:
 *
 *   1. Dos descriptores distintos con el mismo id textual NO colisionan.
 *      (Esto previene accidentes de autor en catálogos de mods o extensiones.)
 *
 *   2. El acceso es O(1) sin cálculo de hashCode sobre strings.
 *
 *   3. Una propiedad registrada con CoreProperties.TEMPERATURE es accesible
 *      ÚNICAMENTE con CoreProperties.TEMPERATURE — no con ningún otro objeto,
 *      aunque su id textual coincida.
 *
 * El contrato es: quien registra el descriptor es quien debe conservar la
 * referencia para acceder al estado. Los catálogos (CoreProperties, etc.)
 * son constantes estáticas que cumplen ese contrato de forma natural.
 *
 * ── API DE ESCRITURA ──────────────────────────────────────────────────────
 *
 *   add(descriptor, delta)   → suma delta al valor actual, clampea si acotado.
 *   set(descriptor, value)   → establece valor directamente, clampea si acotado.
 *
 * Nunca:
 *   add("temperature", delta)   ← PROHIBIDO — no existe este método.
 *   set("temperature", value)   ← PROHIBIDO — no existe este método.
 *
 * ── INVARIANTE ────────────────────────────────────────────────────────────
 *   ✗ Ningún método acepta String como identificador de propiedad.
 *   ✗ Ningún Map<String, ...> interno.
 *   ✓ Todo acceso es exclusivamente mediante PropertyDescriptor.
 *   ✓ IdentityHashMap garantiza identidad fuerte de la clave.
 *   ✓ Imposible representar la misma magnitud bajo dos nombres distintos.
 *
 * ── THREAD SAFETY ─────────────────────────────────────────────────────────
 * No es thread-safe. Usar exclusivamente desde el game loop thread.
 */
public final class PhysicalState {

    /**
     * Valores actuales por descriptor.
     * Indexado por identidad de referencia (IdentityHashMap).
     */
    private final IdentityHashMap<PropertyDescriptor, Double> values;

    /**
     * Descriptores registrados en orden de inserción.
     * LinkedHashMap usado como conjunto ordenado (clave == valor).
     */
    private final Map<PropertyDescriptor, PropertyDescriptor> descriptors;

    // ── Constructor privado — usar Builder ────────────────────────────────

    private PhysicalState(Builder b) {
        this.values      = new IdentityHashMap<>(b.values);
        this.descriptors = Collections.unmodifiableMap(new LinkedHashMap<>(b.descriptors));
    }

    // ── Factory ───────────────────────────────────────────────────────────

    /** Punto de entrada del Builder. */
    public static Builder builder() { return new Builder(); }

    /** Estado vacío sin propiedades. */
    public static PhysicalState empty() { return builder().build(); }

    // ── Acceso a valores ──────────────────────────────────────────────────

    /**
     * True si el objeto tiene la propiedad registrada.
     *
     * @param descriptor descriptor de la propiedad.
     * @return true si existe un valor para este descriptor.
     */
    public boolean has(PropertyDescriptor descriptor) {
        return descriptor != null && values.containsKey(descriptor);
    }

    /**
     * Valor numérico actual de la propiedad.
     * Retorna 0.0 si el objeto no tiene esa propiedad.
     *
     * @param descriptor descriptor de la propiedad.
     * @return valor actual, o 0.0 si no existe.
     */
    public double get(PropertyDescriptor descriptor) {
        if (descriptor == null) return 0.0;
        Double v = values.get(descriptor);
        return v != null ? v : 0.0;
    }

    /**
     * Colección de todos los descriptores de propiedades registradas.
     * Vista inmutable en orden de inserción.
     *
     * @return colección de descriptores.
     */
    public Collection<PropertyDescriptor> registeredDescriptors() {
        return descriptors.values();
    }

    /** Número de propiedades registradas. */
    public int size() { return values.size(); }

    /** True si no hay propiedades registradas. */
    public boolean isEmpty() { return values.isEmpty(); }

    // ── Escritura ─────────────────────────────────────────────────────────

    /**
     * Suma un delta al valor actual de la propiedad.
     * Si la propiedad tiene límites (descriptor acotado), el resultado
     * se clampea automáticamente.
     * No hace nada si la propiedad no existe.
     *
     * @param descriptor descriptor de la propiedad.
     * @param delta      valor a añadir. Negativo = restar.
     */
    public void add(PropertyDescriptor descriptor, double delta) {
        if (descriptor == null || !values.containsKey(descriptor)) return;
        double current = values.get(descriptor);
        double next    = current + delta;
        values.put(descriptor, descriptor.isBounded() ? descriptor.clamp(next) : next);
    }

    /**
     * Establece el valor de la propiedad directamente.
     * Si la propiedad tiene límites, el valor se clampea.
     * No hace nada si la propiedad no existe.
     *
     * @param descriptor descriptor de la propiedad.
     * @param value      nuevo valor.
     */
    public void set(PropertyDescriptor descriptor, double value) {
        if (descriptor == null || !values.containsKey(descriptor)) return;
        values.put(descriptor, descriptor.isBounded() ? descriptor.clamp(value) : value);
    }

    // ── Object ────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "PhysicalState[" + values.size() + " properties]";
    }

    // ── Builder ───────────────────────────────────────────────────────────

    /**
     * Builder de PhysicalState.
     */
    public static final class Builder {

        private final IdentityHashMap<PropertyDescriptor, Double>              values      = new IdentityHashMap<>();
        private final LinkedHashMap<PropertyDescriptor, PropertyDescriptor>    descriptors = new LinkedHashMap<>();

        private Builder() {}

        /**
         * Registra una propiedad con el valor por defecto del descriptor.
         *
         * @param descriptor descriptor de la propiedad.
         * @return this.
         */
        public Builder register(PropertyDescriptor descriptor) {
            if (descriptor == null) return this;
            return register(descriptor, descriptor.getDefaultValue());
        }

        /**
         * Registra una propiedad con un valor inicial explícito.
         * Si el descriptor está acotado, el valor se clampea al rango.
         *
         * @param descriptor   descriptor de la propiedad.
         * @param initialValue valor inicial.
         * @return this.
         */
        public Builder register(PropertyDescriptor descriptor, double initialValue) {
            if (descriptor == null) return this;
            double v = descriptor.isBounded() ? descriptor.clamp(initialValue) : initialValue;
            values.put(descriptor, v);
            descriptors.put(descriptor, descriptor);
            return this;
        }

        /**
         * Registra todas las propiedades de material de un MaterialComponent.
         * Equivale a llamar registerInto(this) sobre el material.
         *
         * @param material el material cuyas propiedades se registran.
         *                 Ignorado si null.
         * @return this (para encadenado).
         */
        public Builder registerMaterial(Game.Engine.World.Components.MaterialComponent material) {
            if (material != null) material.registerInto(this);
            return this;
        }

        /** Construye el PhysicalState. */
        public PhysicalState build() {
            return new PhysicalState(this);
        }
    }
}
