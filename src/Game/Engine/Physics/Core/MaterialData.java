package Game.Engine.Physics.Core;

/**
 * Contrato de acceso a las propiedades intrínsecas de un material.
 *
 * ── HRFC — Cierre del Refactor Arquitectónico ─────────────────────────────
 *
 * ── PROBLEMA QUE RESUELVE ─────────────────────────────────────────────────
 * Antes de esta interfaz, MaterialProperty<V> dependía directamente de
 * Game.Engine.Entity.Components.Collisions.MaterialComponent para extraer
 * valores de material mediante un ToDoubleFunction<MaterialComponent>.
 *
 * Esa dependencia introducía un acoplamiento estructural incorrecto:
 *
 *   Physics.Core  →  Entity.Components.Collisions
 *
 * El Core de Physics no debe conocer ninguna clase del módulo Entity.
 * La dirección correcta del acoplamiento es:
 *
 *   Entity.Components.Collisions.MaterialComponent  →  Physics.Core.MaterialData
 *
 * ── SOLUCIÓN ──────────────────────────────────────────────────────────────
 * MaterialData define el contrato mínimo que Physics.Core necesita para
 * extraer propiedades escalares de un material. Cualquier clase que
 * implemente esta interfaz puede ser usada como fuente de datos de material
 * por MaterialProperty y por los evaluadores del Physics Core.
 *
 * MaterialComponent implementa MaterialData. Los extractores de MaterialProperty
 * reciben MaterialData en lugar de MaterialComponent, eliminando la dependencia
 * hacia Entity desde Core.
 *
 * ── PROPIEDADES DECLARADAS ────────────────────────────────────────────────
 * La interfaz expone exactamente las propiedades que el catálogo de
 * MaterialProperty (y los evaluadores del Core) necesitan leer. No más.
 *
 *   Térmicas:
 *     getThermalConductivity()      coeficiente de conducción [0, 1]
 *     getHeatCapacity()             inercia térmica (> 0)
 *     getThermalDiffusivity()       velocidad de disipación [0, 1]
 *     getMeltingPoint()             temperatura de fusión
 *     getBoilingPoint()             temperatura de ebullición
 *
 *   Eléctricas:
 *     getElectricalConductivity()   conductividad eléctrica [0, 1]
 *
 *   Mecánicas:
 *     getCompressibility()          facilidad de cambio de volumen [0, 1]
 *     getElasticity()               energía conservada en deformaciones [0, 1]
 *     getHardness()                 resistencia a deformación [0, 1]
 *     getDensity()                  masa por unidad de volumen (> 0)
 *
 *   Fluídicas:
 *     getHumidityAbsorption()       coeficiente de absorción [0, 1]
 *     getViscosity()                resistencia al flujo [0, +∞)
 *
 * ── EXTENSIBILIDAD ────────────────────────────────────────────────────────
 * Para añadir una nueva propiedad de material al Core:
 *   1. Añadir el getter en esta interfaz.
 *   2. Implementarlo en MaterialComponent (y en cualquier otra implementación).
 *   3. Declarar la constante en el catálogo correspondiente.
 *
 * Physics.Core no necesita conocer qué clase concreta implementa MaterialData.
 *
 * ── INVARIANTE ────────────────────────────────────────────────────────────
 *   ✗ No contiene lógica de simulación.
 *   ✗ No contiene referencias a clases del módulo Entity.
 *   ✓ Solo declara getters escalares de propiedades de material.
 */
public interface MaterialData {

    // ── Dominio térmico ───────────────────────────────────────────────────

    /** Conductividad térmica [0, 1]. 0 = aislante perfecto, 1 = conductor perfecto. */
    double getThermalConductivity();

    /** Capacidad calorífica específica (> 0). Mayor valor = más inercia térmica. */
    double getHeatCapacity();

    /** Difusividad térmica [0, 1]. Velocidad de disipación hacia el ambiente. */
    double getThermalDiffusivity();

    /** Temperatura de fusión en unidades del juego. */
    double getMeltingPoint();

    /** Temperatura de ebullición en unidades del juego. */
    double getBoilingPoint();

    // ── Dominio eléctrico ─────────────────────────────────────────────────

    /** Conductividad eléctrica efectiva [0, 1]. 0 = aislante, 1 = conductor perfecto. */
    double getElectricalConductivity();

    // ── Dominio mecánico ──────────────────────────────────────────────────

    /** Compresibilidad [0, 1]. 0 = incompresible, 1 = muy compresible. */
    double getCompressibility();

    /** Elasticidad [0, 1]. 0 = completamente inelástico, 1 = elástico perfecto. */
    double getElasticity();

    /** Dureza [0, 1]. 0 = muy blando, 1 = muy duro. */
    double getHardness();

    /** Densidad relativa del material (> 0). */
    double getDensity();

    // ── Dominio fluídico ──────────────────────────────────────────────────

    /** Coeficiente de absorción de humedad [0, 1]. */
    double getHumidityAbsorption();

    /** Viscosidad [0, +∞). 0 para sólidos rígidos. */
    double getViscosity();
}
