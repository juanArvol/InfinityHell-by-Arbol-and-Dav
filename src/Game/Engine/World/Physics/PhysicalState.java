package Game.Engine.World.Physics;

import Game.Engine.World.Components.MaterialComponent;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fuente de verdad del estado físico de un objeto.
 *
 * ── HRFC-017 — Consolidación Definitiva del Modelo Declarativo ────────────
 *
 * ── FILOSOFÍA ─────────────────────────────────────────────────────────────
 * PhysicalState unifica en un único punto todo el estado físico de un objeto.
 *
 * En el modelo anterior, el estado estaba disperso en componentes separados:
 *   ThermalComponent    → temperatura
 *   ElectricalComponent → carga
 *   FluidComponent      → humedad
 *   PressureComponent   → presión
 *
 * Cada componente era un silo independiente. El Engine necesitaba conocer
 * cada silo por nombre para acceder al estado. Añadir una nueva propiedad
 * requería añadir un nuevo componente y enseñar al Engine a conocerlo.
 *
 * PhysicalState reemplaza ese modelo: es un mapa de dominio → PhysicalQuantity
 * completamente genérico. El Engine no necesita conocer los dominios por nombre.
 * El PhysicsSolver recorre todas las propiedades registradas de forma uniforme.
 *
 * ── DISEÑO ────────────────────────────────────────────────────────────────
 * PhysicalState es un mapa de identificador de propiedad → PhysicalQuantity.
 * La clave es el id textual de la PhysicalProperty, no la clase del dominio,
 * para soportar múltiples propiedades del mismo dominio en el mismo objeto
 * si el diseño del juego lo requiere.
 *
 * El acceso por dominio (getByDomain) retorna todas las cantidades registradas
 * bajo ese dominio, en orden de inserción.
 *
 * ── INVARIANTE FUNDAMENTAL ───────────────────────────────────────────────
 * Toda modificación al estado físico de un objeto debe producirse
 * exclusivamente a través de:
 *
 *   1. PhysicsSolver.solve()  — resolución de ecuaciones y restricciones.
 *   2. InfluenceSystem        — modificaciones directas externas (magia, auras).
 *   3. WorldFieldSystem       — campos espaciales.
 *
 * Ningún sistema de Gameplay puede modificar directamente PhysicalState.
 * Gameplay únicamente observa el estado resultante.
 *
 * ── COMPATIBILIDAD CON LOS COMPONENTES EXISTENTES ─────────────────────────
 * PhysicalState no elimina ThermalComponent, ElectricalComponent, etc.
 * durante la transición. Los componentes existentes se mantienen como
 * adaptadores hasta que se complete la migración completa.
 *
 * El método registerFromComponent() permite registrar la PhysicalQuantity
 * de un componente existente en el PhysicalState, de forma que el Solver
 * opere sobre la misma instancia que el componente.
 *
 * ── USO ───────────────────────────────────────────────────────────────────
 *
 *   // Construir el estado de un objeto
 *   PhysicalState state = PhysicalState.builder()
 *       .register(PhysicalProperties.TEMPERATURE, 20.0)
 *       .register(PhysicalProperties.CHARGE, 0.0)
 *       .register(PhysicalProperties.HUMIDITY, 0.0)
 *       .register(PhysicalProperties.PRESSURE, 0.0)
 *       .material(new MaterialComponent.Builder().thermalConductivity(0.5).build())
 *       .build();
 *
 *   // Leer el estado (Gameplay)
 *   double temp = state.get(PhysicalProperties.TEMPERATURE);
 *   boolean hasCharge = state.has(PhysicalProperties.CHARGE);
 *
 * ── THREAD SAFETY ────────────────────────────────────────────────────────
 * No es thread-safe. Usar exclusivamente desde el game loop thread.
 */
public final class PhysicalState {

    /**
     * Mapa de id de propiedad → PhysicalQuantity<?>.
     * LinkedHashMap para preservar el orden de inserción (determinismo).
     */
    private final Map<String, PhysicalQuantity<?>> quantities;

    /**
     * Mapa secundario de id de propiedad → PhysicalProperty<?>.
     * Permite al Solver conocer los metadatos de cada propiedad registrada
     * (dominio, equilibrio, límites) sin necesidad de un registro global.
     */
    private final Map<String, PhysicalProperty<?>> properties;

    /** Material del objeto. Nunca null. */
    private MaterialComponent material;

    // ── Constructor privado — usar Builder ────────────────────────────────

    private PhysicalState(Builder b) {
        this.quantities = Collections.unmodifiableMap(
            new LinkedHashMap<>(b.quantities));
        this.properties = Collections.unmodifiableMap(
            new LinkedHashMap<>(b.properties));
        this.material   = b.material != null ? b.material : MaterialComponent.DEFAULT;
    }

    // ── Factory ───────────────────────────────────────────────────────────

    /** Punto de entrada del Builder. */
    public static Builder builder() { return new Builder(); }

    /** Estado vacío sin propiedades. Útil para objetos sin simulación física. */
    public static PhysicalState empty() { return builder().build(); }

    // ── Acceso a cantidades ───────────────────────────────────────────────

    /**
     * True si el objeto tiene la propiedad registrada.
     *
     * @param property descriptor de la propiedad.
     * @return true si existe una PhysicalQuantity para esta propiedad.
     */
    public boolean has(PhysicalProperty<?> property) {
        return property != null && quantities.containsKey(property.getId());
    }

    /**
     * Retorna el valor numérico actual de la propiedad, o 0.0 si no existe.
     *
     * @param property descriptor de la propiedad.
     * @return valor actual, o 0.0 si el objeto no tiene esa propiedad.
     */
    public double get(PhysicalProperty<?> property) {
        if (property == null) return 0.0;
        PhysicalQuantity<?> q = quantities.get(property.getId());
        return q != null ? q.getValue() : 0.0;
    }

    /**
     * Retorna la PhysicalQuantity mutable de la propiedad.
     * El PhysicsSolver usa este método para aplicar deltas.
     * Retorna null si el objeto no tiene la propiedad.
     *
     * @param property descriptor de la propiedad.
     * @param <D>      dominio físico.
     * @return PhysicalQuantity mutable, o null.
     */
    @SuppressWarnings("unchecked")
    public <D extends PhysicalDomain> PhysicalQuantity<D> getQuantity(
            PhysicalProperty<D> property) {
        if (property == null) return null;
        return (PhysicalQuantity<D>) quantities.get(property.getId());
    }

    /**
     * Retorna la PhysicalQuantity mutable por id de propiedad.
     * Usar cuando no se dispone del descriptor pero sí del id.
     *
     * @param propertyId id de la propiedad.
     * @return PhysicalQuantity sin tipo específico, o null.
     */
    public PhysicalQuantity<?> getQuantityById(String propertyId) {
        if (propertyId == null) return null;
        return quantities.get(propertyId);
    }

    /**
     * Retorna el descriptor de la propiedad dado su id.
     *
     * @param propertyId id de la propiedad.
     * @return descriptor, o null si no existe.
     */
    public PhysicalProperty<?> getProperty(String propertyId) {
        if (propertyId == null) return null;
        return properties.get(propertyId);
    }

    /**
     * Colección de todos los descriptores de propiedades registradas.
     * En orden de inserción. Vista inmutable.
     *
     * El PhysicsSolver itera sobre esta colección para recorrer todas las
     * propiedades del objeto sin conocer sus nombres.
     *
     * @return colección inmutable de PhysicalProperty<?>.
     */
    public Collection<PhysicalProperty<?>> registeredProperties() {
        return properties.values();
    }

    /**
     * Colección de todos los pares (id → cantidad) registrados.
     * Vista inmutable del mapa interno.
     *
     * @return colección inmutable de entradas.
     */
    public Collection<Map.Entry<String, PhysicalQuantity<?>>> entries() {
        return quantities.entrySet();
    }

    /** Número de propiedades registradas en este estado. */
    public int size() { return quantities.size(); }

    /** True si no hay propiedades registradas. */
    public boolean isEmpty() { return quantities.isEmpty(); }

    // ── Acceso al material ────────────────────────────────────────────────

    /**
     * Material del objeto. Nunca null.
     *
     * @return material del objeto.
     */
    public MaterialComponent getMaterial() { return material; }

    /**
     * Actualiza el material del objeto.
     * Solo llamar desde sistemas que gestionan cambios de material en runtime
     * (p.ej. cambio de fase: sólido → líquido).
     *
     * @param material nuevo material. Si null, se usa MaterialComponent.DEFAULT.
     */
    public void setMaterial(MaterialComponent material) {
        this.material = material != null ? material : MaterialComponent.DEFAULT;
    }

    // ── Object ────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "PhysicalState[" + quantities.size() + " properties]";
    }

    // ── Builder ───────────────────────────────────────────────────────────

    /**
     * Builder de PhysicalState.
     * Permite registrar propiedades con sus valores iniciales y el material.
     */
    public static final class Builder {

        private final Map<String, PhysicalQuantity<?>> quantities = new LinkedHashMap<>();
        private final Map<String, PhysicalProperty<?>> properties = new LinkedHashMap<>();
        private MaterialComponent material;

        private Builder() {}

        /**
         * Registra una propiedad con el valor de equilibrio de la propia propiedad
         * como valor inicial.
         *
         * @param property descriptor de la propiedad.
         * @param <D>      dominio físico.
         * @return this (para encadenado).
         */
        public <D extends PhysicalDomain> Builder register(PhysicalProperty<D> property) {
            return register(property, property.getEquilibrium());
        }

        /**
         * Registra una propiedad con un valor inicial explícito.
         *
         * @param property     descriptor de la propiedad.
         * @param initialValue valor inicial.
         * @param <D>          dominio físico.
         * @return this (para encadenado).
         */
        public <D extends PhysicalDomain> Builder register(
                PhysicalProperty<D> property,
                double initialValue) {
            if (property == null) return this;
            quantities.put(property.getId(), property.createQuantity(initialValue));
            properties.put(property.getId(), property);
            return this;
        }

        /**
         * Registra la PhysicalQuantity de un componente existente en el estado.
         * Usa la misma instancia que el componente — las modificaciones del Solver
         * se reflejan automáticamente en el componente.
         *
         * Usar durante la transición para mantener compatibilidad con los
         * componentes de estado existentes (ThermalComponent, etc.).
         *
         * @param property descriptor de la propiedad.
         * @param quantity instancia de PhysicalQuantity del componente.
         * @param <D>      dominio físico.
         * @return this (para encadenado).
         */
        public <D extends PhysicalDomain> Builder registerExisting(
                PhysicalProperty<D> property,
                PhysicalQuantity<D> quantity) {
            if (property == null || quantity == null) return this;
            quantities.put(property.getId(), quantity);
            properties.put(property.getId(), property);
            return this;
        }

        /**
         * Define el material del objeto.
         *
         * @param material material del objeto.
         * @return this (para encadenado).
         */
        public Builder material(MaterialComponent material) {
            this.material = material;
            return this;
        }

        /** Construye el PhysicalState. */
        public PhysicalState build() {
            return new PhysicalState(this);
        }
    }
}
