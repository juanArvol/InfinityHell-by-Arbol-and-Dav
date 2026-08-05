package Game.Engine.Physics.Core;

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
 *   3. Una propiedad registrada con ThermalProperties.TEMPERATURE es accesible
 *      ÚNICAMENTE con ThermalProperties.TEMPERATURE — no con ningún otro objeto,
 *      aunque su id textual coincida.
 *
 * El contrato es: quien registra el descriptor es quien debe conservar la
 * referencia para acceder al estado. Los catálogos de propiedades físicas
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
public final class PhysicalState implements DomainState {

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
     *
     * ── HRFC-031 ──────────────────────────────────────────────────────────
     * A partir de HRFC-031, PhysicalState describe exclusivamente propiedades
     * del dominio físico del objeto (temperatura, presión, carga eléctrica,
     * humedad, energía interna...).
     *
     * Las propiedades cinemáticas (velocidad, energía cinética, fricción) ya
     * no se registran aquí: pertenecen al KinematicState del SimulationContext.
     * Los evaluadores acceden a ellas mediante SimulationContext.kinematic().
     *
     * Para registrar propiedades de material usar:
     *   .registerMaterial(mat::registerInto)
     * donde mat es cualquier clase con un método registerInto(Builder).
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
         * Registra las propiedades de un material en este PhysicalState.
         *
         * El parámetro es un Consumer que recibe este Builder y lo puebla con
         * los PropertyDescriptors del material. Esto desacopla PhysicalState.Builder
         * de cualquier clase concreta de material:
         *
         *   PhysicalState.builder()
         *       .register(ThermalProperties.TEMPERATURE, 20.0)
         *       .registerMaterial(mat::registerInto)   // mat es MaterialComponent
         *       .build();
         *
         * Donde mat::registerInto es el método de MaterialComponent que conoce
         * qué PropertyDescriptors registrar y con qué valores iniciales.
         *
         * @param materialRegistrar función que puebla este Builder con las
         *                          propiedades del material. Ignorada si null.
         * @return this (para encadenado).
         */
        public Builder registerMaterial(java.util.function.Consumer<Builder> materialRegistrar) {
            if (materialRegistrar != null) materialRegistrar.accept(this);
            return this;
        }

        /** Construye el PhysicalState. */
        public PhysicalState build() {
            return new PhysicalState(this);
        }
    }
}
