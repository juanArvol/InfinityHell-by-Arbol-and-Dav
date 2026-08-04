package Game.Engine.Physics.Core;

import Game.Engine.World.Components.MaterialComponent;
import java.util.function.ToDoubleFunction;

/**
 * Descriptor genérico de una propiedad de material.
 *
 * ── HRFC-018 — Consolidación Definitiva del Modelo Declarativo ────────────
 *
 * ── PROBLEMA QUE RESUELVE ─────────────────────────────────────────────────
 * Antes de HRFC-018, las leyes físicas (PhysicsEquation, PhysicsConstraint,
 * PairEquation) accedían a las constantes del material mediante getters
 * específicos por fenómeno:
 *
 *   mat.getThermalConductivity()
 *   mat.getHeatCapacity()
 *   mat.getElectricalConductivity()
 *   mat.getHumidityAbsorption()
 *   mat.getCompressibility()
 *   ...
 *
 * Añadir una propiedad nueva (p.ej. permeabilidad magnética) requería:
 *   1. Añadir un campo en MaterialComponent.
 *   2. Añadir un getter en MaterialComponent.
 *   3. Actualizar todas las leyes que usen esa propiedad.
 *
 * ── SOLUCIÓN ──────────────────────────────────────────────────────────────
 * MaterialProperty<V> es un descriptor que encapsula el acceso a una
 * propiedad del material mediante una función extractora.
 *
 * Las relaciones físicas (PhysicalRelation) referencian propiedades mediante
 * descriptores, no mediante getters específicos. El acceso uniforme es:
 *
 *   double conductivity = MaterialProperties.THERMAL_CONDUCTIVITY.from(mat);
 *   double capacity     = MaterialProperties.HEAT_CAPACITY.from(mat);
 *   double magnetic     = MaterialProperties.MAGNETIC_PERMEABILITY.from(mat);
 *
 * La firma es idéntica para todas las propiedades. El Solver nunca necesita
 * conocer el nombre del getter subyacente.
 *
 * ── DISEÑO ────────────────────────────────────────────────────────────────
 * MaterialProperty<V> es inmutable. Contiene:
 *
 *   id          → identificador único legible (para depuración y serialización)
 *   extractor   → ToDoubleFunction<MaterialComponent> que lee el valor
 *   description → descripción opcional legible
 *
 * El parámetro de tipo <V> está reservado para extensibilidad futura
 * (propiedades no escalares). En esta versión todas las propiedades del
 * material son escalares (double), por lo que V = Double en el catálogo.
 *
 * ── EXTENSIBILIDAD ────────────────────────────────────────────────────────
 * Para añadir una nueva propiedad física de material:
 *
 *   1. Añadir el campo en MaterialComponent (una sola línea).
 *   2. Añadir el getter en MaterialComponent (una sola línea).
 *   3. Declarar una constante en el catálogo (MaterialProperties o uno propio):
 *
 *      public static final MaterialProperty<Double> MAGNETIC_PERMEABILITY =
 *          MaterialProperty.of("magnetic_permeability",
 *              MaterialComponent::getMagneticPermeability,
 *              "Permeabilidad magnética del material [0,1]");
 *
  *   4. Usar el descriptor en las PhysicalRelation que la necesiten.
 *      → PhysicsSolver no cambia. RelationRegistry no cambia.
 *
 * ── QUÉ NO CONTIENE ──────────────────────────────────────────────────────
 *   ✗ Algoritmos
 *   ✗ Condiciones if/switch
 *   ✗ Lógica de simulación
 *   ✗ Nombres de fenómenos
 *   ✗ Referencias al Solver
 *
 * @param <V> tipo del valor de la propiedad. Double para propiedades escalares.
 */
public final class MaterialProperty<V> {

    /** Identificador único de la propiedad. Nunca null ni vacío. */
    private final String id;

    /**
     * Función que extrae el valor escalar de esta propiedad desde un MaterialComponent.
     * Nunca null.
     */
    private final ToDoubleFunction<MaterialComponent> extractor;

    /** Descripción legible opcional. Puede ser null. */
    private final String description;

    // ── Constructor privado — usar factories ──────────────────────────────

    private MaterialProperty(String id,
                              ToDoubleFunction<MaterialComponent> extractor,
                              String description) {
        if (id == null || id.isBlank())
            throw new IllegalArgumentException("id no puede ser null ni vacío");
        if (extractor == null)
            throw new IllegalArgumentException("extractor no puede ser null");
        this.id          = id;
        this.extractor   = extractor;
        this.description = description;
    }

    // ── Factories ─────────────────────────────────────────────────────────

    /**
     * Crea un descriptor con id, función extractora y descripción.
     *
     * @param id          identificador único. No puede ser null ni vacío.
     * @param extractor   función que extrae el valor desde MaterialComponent.
     * @param description descripción legible (puede ser null).
     * @param <V>         tipo del valor.
     * @return descriptor configurado.
     */
    public static <V> MaterialProperty<V> of(
            String id,
            ToDoubleFunction<MaterialComponent> extractor,
            String description) {
        return new MaterialProperty<>(id, extractor, description);
    }

    /**
     * Crea un descriptor sin descripción.
     *
     * @param id        identificador único. No puede ser null ni vacío.
     * @param extractor función que extrae el valor desde MaterialComponent.
     * @param <V>       tipo del valor.
     * @return descriptor configurado.
     */
    public static <V> MaterialProperty<V> of(
            String id,
            ToDoubleFunction<MaterialComponent> extractor) {
        return new MaterialProperty<>(id, extractor, null);
    }

    // ── Acceso al valor ───────────────────────────────────────────────────

    /**
     * Extrae el valor de esta propiedad desde el material dado.
     *
     * Uso en leyes físicas:
     *
     *   double cond = MaterialProperties.THERMAL_CONDUCTIVITY.from(mat);
     *
     * @param material el material del que leer la propiedad. No puede ser null.
     * @return valor escalar de la propiedad.
     */
    public double from(MaterialComponent material) {
        if (material == null)
            throw new IllegalArgumentException("material no puede ser null");
        return extractor.applyAsDouble(material);
    }

    /**
     * Extrae el valor de esta propiedad desde el material dado.
     * Retorna el valor por defecto si el material es null.
     *
     * @param material     el material del que leer la propiedad.
     * @param defaultValue valor retornado si material es null.
     * @return valor escalar de la propiedad, o defaultValue si material es null.
     */
    public double fromOrDefault(MaterialComponent material, double defaultValue) {
        if (material == null) return defaultValue;
        return extractor.applyAsDouble(material);
    }

    // ── Accesores de metadato ─────────────────────────────────────────────

    /** Identificador único de la propiedad. */
    public String getId()          { return id; }

    /** Descripción legible. Puede ser null. */
    public String getDescription() { return description; }

    // ── Object ────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "MaterialProperty[" + id + "]";
    }
}
