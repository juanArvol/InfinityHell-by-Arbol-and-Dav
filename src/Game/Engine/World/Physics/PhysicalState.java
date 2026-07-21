package Game.Engine.World.Physics;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fuente de verdad del estado físico de un objeto.
 *
 * ── HRFC-019 — Eliminación Definitiva del Modelo Orientado a Tipos de Ley ─
 *
 * ── FILOSOFÍA ─────────────────────────────────────────────────────────────
 * PhysicalState es un mapa plano de identificador de propiedad → valor numérico.
 *
 * No hay genéricos de dominio. No hay PhysicalQuantity<D>. No hay CoreDomains.
 * No hay distinción entre "propiedad física" y "propiedad de material".
 * Todo es un double identificado por un string.
 *
 * El PhysicsSolver lee y escribe valores a través de este mapa.
 * LawContext es la vista que las leyes reciben sobre este estado.
 * PhysicalState nunca es accedido directamente por las leyes.
 *
 * ── ESTRUCTURA ────────────────────────────────────────────────────────────
 * PhysicalState contiene dos mapas paralelos:
 *
 *   values      → Map<String, Double> — los valores actuales de las propiedades.
 *   descriptors → Map<String, PropertyDescriptor> — los metadatos (rango, default).
 *
 * El Solver solo trabaja con el mapa de valores.
 * Los descriptores se usan para:
 *   - Clampear valores al aplicar deltas (si la propiedad está acotada).
 *   - Proveer el valor por defecto al crear el estado.
 *   - Introspección y depuración.
 *
 * ── PROPIEDADES DE MATERIAL ───────────────────────────────────────────────
 * Las propiedades de material (conductividad, capacidad calorífica, etc.)
 * también se almacenan en PhysicalState bajo sus descriptores.
 *
 * Esto elimina la distinción entre MaterialComponent y PhysicalState:
 * ambos son simplemente conjuntos de propiedades con valores numéricos.
 * Un objeto puede tener temperature, thermal_conductivity, humidity, y
 * hardness registrados en el mismo PhysicalState, y el Solver los trata
 * exactamente igual — son valores en un mapa.
 *
 * ── API DE ESCRITURA ──────────────────────────────────────────────────────
 * El único punto de escritura es:
 *
 *   add(id, delta)   → suma delta al valor actual, clampea si la propiedad
 *                      tiene límites.
 *   set(id, value)   → establece el valor directamente (sin delta).
 *
 * Los sistemas externos (WorldFieldSystem, InfluenceSystem) usan set() o add()
 * directamente. El PhysicsSolver usa add() exclusivamente a través de LawContext.
 *
 * ── COMPATIBILIDAD CON EL MODELO ANTERIOR ────────────────────────────────
 * Los componentes legacy (ElectricalComponent, FluidComponent, PressureComponent)
 * que subsisten durante la transición se mantienen como facades sobre las
 * propiedades de PhysicalState del objeto, sin duplicar el dato.
 *
 * ── INVARIANTE ────────────────────────────────────────────────────────────
 *   ✗ Ningún parámetro de tipo de dominio.
 *   ✗ Ninguna referencia a CoreDomains ni PhysicalDomain.
 *   ✗ Ninguna distinción entre propiedad física y propiedad de material.
 *   ✓ Toda propiedad es un string → double.
 *   ✓ El Solver escribe exclusivamente mediante add().
 *
 * ── THREAD SAFETY ────────────────────────────────────────────────────────
 * No es thread-safe. Usar exclusivamente desde el game loop thread.
 */
public final class PhysicalState {

    /** Valores actuales por id de propiedad. */
    private final Map<String, Double>             values;

    /** Descriptores por id de propiedad (metadatos: rango, default). */
    private final Map<String, PropertyDescriptor> descriptors;

    // ── Constructor privado — usar Builder ────────────────────────────────

    private PhysicalState(Builder b) {
        this.values      = new LinkedHashMap<>(b.values);
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
     * @param propertyId identificador de la propiedad.
     * @return true si existe un valor para este id.
     */
    public boolean has(String propertyId) {
        return propertyId != null && values.containsKey(propertyId);
    }

    /**
     * Valor numérico actual de la propiedad.
     * Retorna 0.0 si el objeto no tiene esa propiedad.
     *
     * @param propertyId identificador de la propiedad.
     * @return valor actual, o 0.0 si no existe.
     */
    public double get(String propertyId) {
        if (propertyId == null) return 0.0;
        Double v = values.get(propertyId);
        return v != null ? v : 0.0;
    }

    /**
     * Descriptor de la propiedad dado su id.
     *
     * @param propertyId identificador de la propiedad.
     * @return descriptor, o null si no está registrado.
     */
    public PropertyDescriptor getDescriptor(String propertyId) {
        if (propertyId == null) return null;
        return descriptors.get(propertyId);
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

    /**
     * Conjunto de todos los ids de propiedades registradas.
     * Vista inmutable.
     *
     * @return conjunto de ids.
     */
    public Collection<String> registeredIds() {
        return descriptors.keySet();
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
     * @param propertyId identificador de la propiedad.
     * @param delta      valor a añadir. Negativo = restar.
     */
    public void add(String propertyId, double delta) {
        if (propertyId == null || !values.containsKey(propertyId)) return;
        double current = values.get(propertyId);
        double next    = current + delta;
        PropertyDescriptor desc = descriptors.get(propertyId);
        if (desc != null && desc.isBounded()) {
            next = desc.clamp(next);
        }
        values.put(propertyId, next);
    }

    /**
     * Establece el valor de la propiedad directamente.
     * Si la propiedad tiene límites, el valor se clampea.
     * No hace nada si la propiedad no existe.
     *
     * @param propertyId identificador de la propiedad.
     * @param value      nuevo valor.
     */
    public void set(String propertyId, double value) {
        if (propertyId == null || !values.containsKey(propertyId)) return;
        PropertyDescriptor desc = descriptors.get(propertyId);
        values.put(propertyId, desc != null && desc.isBounded() ? desc.clamp(value) : value);
    }

    /**
     * Establece el valor de una propiedad aunque no esté registrada en los
     * descriptores. Útil para actualizar propiedades dinámicas en runtime.
     * No aplica clamp — el llamador es responsable de los rangos.
     *
     * @param propertyId identificador.
     * @param value      nuevo valor.
     */
    public void setRaw(String propertyId, double value) {
        if (propertyId != null) values.put(propertyId, value);
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

        private final Map<String, Double>             values      = new LinkedHashMap<>();
        private final Map<String, PropertyDescriptor> descriptors = new LinkedHashMap<>();

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
            values.put(descriptor.getId(), v);
            descriptors.put(descriptor.getId(), descriptor);
            return this;
        }

        /**
         * Registra una propiedad por id y valor sin descriptor.
         * Útil para propiedades dinámicas o de acceso rápido sin metadatos.
         * El valor no se clampea.
         *
         * @param propertyId   identificador.
         * @param initialValue valor inicial.
         * @return this.
         */
        public Builder registerRaw(String propertyId, double initialValue) {
            if (propertyId == null) return this;
            values.put(propertyId, initialValue);
            return this;
        }

        /**
         * Registra todas las propiedades de material de un MaterialComponent.
         * Equivale a llamar registerInto(this) sobre el material.
         *
         * Uso:
         *   PhysicalState.builder()
         *       .register(CoreProperties.TEMPERATURE, 20.0)
         *       .registerMaterial(mat)
         *       .build();
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
