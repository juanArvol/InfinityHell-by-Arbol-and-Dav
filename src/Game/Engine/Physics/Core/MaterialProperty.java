package Game.Engine.Physics.Core;

import java.util.function.ToDoubleFunction;

/**
 * Descriptor genérico de una propiedad de material.
 *
 * ── HRFC-018 — Consolidación Definitiva del Modelo Declarativo ────────────
 * ── HRFC — Cierre del Refactor Arquitectónico ─────────────────────────────
 *
 * ── PROBLEMA QUE RESUELVE ─────────────────────────────────────────────────
 * Antes de HRFC-018, las leyes físicas accedían a las constantes del material
 * mediante getters específicos por fenómeno:
 *
 *   mat.getThermalConductivity()
 *   mat.getHeatCapacity()
 *   mat.getElectricalConductivity()
 *   ...
 *
 * Añadir una propiedad nueva requería modificar MaterialComponent, añadir un
 * getter y actualizar todas las leyes que la usaran.
 *
 * ── SOLUCIÓN ──────────────────────────────────────────────────────────────
 * MaterialProperty<V> encapsula el acceso a una propiedad del material
 * mediante una función extractora sobre MaterialData.
 *
 * Las relaciones físicas referencian propiedades mediante descriptores:
 *
 *   double conductivity = ThermalMaterialProperties.THERMAL_CONDUCTIVITY.from(mat);
 *   double capacity     = ThermalMaterialProperties.HEAT_CAPACITY.from(mat);
 *
 * La firma es idéntica para todas las propiedades. Los evaluadores nunca
 * necesitan conocer el nombre del getter subyacente.
 *
 * ── DESACOPLAMIENTO DE ENTITY (HRFC — Cierre) ─────────────────────────────
 * El extractor opera sobre MaterialData (interfaz de Physics.Core), no sobre
 * MaterialComponent (clase concreta de Entity.Components.Collisions).
 *
 * Esto elimina la dependencia estructural:
 *
 *   Antes:  Physics.Core  →  Entity.Components.Collisions.MaterialComponent
 *   Ahora:  Physics.Core  →  Physics.Core.MaterialData   (solo intra-módulo)
 *           Entity.Components.Collisions.MaterialComponent  →  Physics.Core.MaterialData
 *
 * Cualquier implementación de MaterialData puede ser usada como fuente de
 * datos de material — no solo MaterialComponent.
 *
 * ── DISEÑO ────────────────────────────────────────────────────────────────
 * MaterialProperty<V> es inmutable. Contiene:
 *
 *   id          → identificador único legible (para depuración y serialización)
 *   extractor   → ToDoubleFunction<MaterialData> que lee el valor
 *   description → descripción opcional legible
 *
 * El parámetro de tipo <V> está reservado para extensibilidad futura
 * (propiedades no escalares). En esta versión todas las propiedades del
 * material son escalares (double), por lo que V = Double en los catálogos.
 *
 * ── EXTENSIBILIDAD ────────────────────────────────────────────────────────
 * Para añadir una nueva propiedad física de material:
 *
 *   1. Añadir el getter en la interfaz MaterialData.
 *   2. Implementarlo en MaterialComponent (y cualquier otra implementación).
 *   3. Declarar una constante en el catálogo del dominio correspondiente:
 *
 *      public static final MaterialProperty<Double> MAGNETIC_PERMEABILITY =
 *          MaterialProperty.of("magnetic_permeability",
 *              MaterialData::getMagneticPermeability,
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
 *   ✗ Referencias a clases del módulo Entity
 *
 * @param <V> tipo del valor de la propiedad. Double para propiedades escalares.
 */
public final class MaterialProperty<V> {

    /** Identificador único de la propiedad. Nunca null ni vacío. */
    private final String id;

    /**
     * Función que extrae el valor escalar de esta propiedad desde un MaterialData.
     * Nunca null.
     */
    private final ToDoubleFunction<MaterialData> extractor;

    /** Descripción legible opcional. Puede ser null. */
    private final String description;

    // ── Constructor privado — usar factories ──────────────────────────────

    private MaterialProperty(String id,
                              ToDoubleFunction<MaterialData> extractor,
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
     * @param extractor   función que extrae el valor desde MaterialData.
     * @param description descripción legible (puede ser null).
     * @param <V>         tipo del valor.
     * @return descriptor configurado.
     */
    public static <V> MaterialProperty<V> of(
            String id,
            ToDoubleFunction<MaterialData> extractor,
            String description) {
        return new MaterialProperty<>(id, extractor, description);
    }

    /**
     * Crea un descriptor sin descripción.
     *
     * @param id        identificador único. No puede ser null ni vacío.
     * @param extractor función que extrae el valor desde MaterialData.
     * @param <V>       tipo del valor.
     * @return descriptor configurado.
     */
    public static <V> MaterialProperty<V> of(
            String id,
            ToDoubleFunction<MaterialData> extractor) {
        return new MaterialProperty<>(id, extractor, null);
    }

    // ── Acceso al valor ───────────────────────────────────────────────────

    /**
     * Extrae el valor de esta propiedad desde el material dado.
     *
     * Uso en leyes físicas:
     *
     *   double cond = ThermalMaterialProperties.THERMAL_CONDUCTIVITY.from(mat);
     *
     * @param material el material del que leer la propiedad. No puede ser null.
     * @return valor escalar de la propiedad.
     */
    public double from(MaterialData material) {
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
    public double fromOrDefault(MaterialData material, double defaultValue) {
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
