package Game.Engine.World.Physics;

/**
 * Catálogo de propiedades físicas fundamentales del World Simulation Core.
 *
 * ── HRFC-017 — Consolidación Definitiva del Modelo Declarativo ────────────
 *
 * ── FILOSOFÍA ─────────────────────────────────────────────────────────────
 * PhysicalProperties es donde reside el conocimiento del dominio físico
 * del juego. No en el Engine. No en el Solver.
 *
 * Añadir una nueva propiedad física = añadir una constante aquí.
 * El PhysicsSolver no necesita ningún cambio.
 *
 * ── RELACIÓN CON CoreDomains ──────────────────────────────────────────────
 * Cada PhysicalProperty está vinculada a un dominio de CoreDomains, que
 * sigue siendo el marcador de tipo en el sistema de genéricos.
 *
 * ── PROPIEDADES FUNDAMENTALES ─────────────────────────────────────────────
 *
 *   TEMPERATURE  — energía térmica almacenada. 0 = ambiente. Libre.
 *   CHARGE       — carga eléctrica neta. 0 = neutro. Libre.
 *   HUMIDITY     — contenido fluídico. Rango natural [0, 1].
 *   PRESSURE     — presión local. 0 = ambiente. Libre.
 *
 * ── EXTENSIBILIDAD ────────────────────────────────────────────────────────
 * El Gameplay puede definir sus propias propiedades en catálogos propios:
 *
 *   // En un módulo de magia:
 *   public interface MagicProperties {
 *       PhysicalProperty<MagicDomains.Mana> MANA = PhysicalProperty
 *           .builder(MagicDomains.Mana.class, "mana")
 *           .range(0.0, 100.0)
 *           .equilibrium(0.0)
 *           .build();
 *   }
 *
 * El Solver integrará esas propiedades automáticamente si se registran en
 * PhysicalState sin ningún cambio en el código del Engine.
 */
public final class PhysicalProperties {

    private PhysicalProperties() {}

    // ── Dominio térmico ───────────────────────────────────────────────────

    /**
     * Temperatura / energía térmica almacenada.
     * 0 = temperatura ambiente del mundo.
     * Positiva = más caliente que el ambiente.
     * Negativa = más fría que el ambiente.
     * Sin límites — la temperatura puede alcanzar cualquier valor.
     */
    public static final PhysicalProperty<CoreDomains.Thermal> TEMPERATURE =
        PhysicalProperty.builder(CoreDomains.Thermal.class, "temperature")
            .equilibrium(0.0)
            .description("Energía térmica almacenada relativa al ambiente")
            .build();

    // ── Dominio eléctrico ─────────────────────────────────────────────────

    /**
     * Carga eléctrica neta.
     * 0 = objeto neutro.
     * Positiva = carga positiva acumulada.
     * Negativa = carga negativa acumulada.
     * Sin límites estructurales en el descriptor — los objetos concretos
     * pueden configurar un límite en su PhysicalState si lo requieren.
     */
    public static final PhysicalProperty<CoreDomains.Electrical> CHARGE =
        PhysicalProperty.builder(CoreDomains.Electrical.class, "charge")
            .equilibrium(0.0)
            .description("Carga eléctrica neta acumulada")
            .build();

    // ── Dominio fluídico ──────────────────────────────────────────────────

    /**
     * Contenido fluídico (humedad).
     * Rango natural [0, 1]: 0 = completamente seco, 1 = saturado.
     * El clamp está integrado en la propiedad — ninguna ecuación puede
     * sacar el valor de este rango.
     */
    public static final PhysicalProperty<CoreDomains.Fluid> HUMIDITY =
        PhysicalProperty.builder(CoreDomains.Fluid.class, "humidity")
            .range(0.0, 1.0)
            .equilibrium(0.0)
            .description("Contenido fluídico relativo [0=seco, 1=saturado]")
            .build();

    // ── Dominio de presión ────────────────────────────────────────────────

    /**
     * Presión local relativa al equilibrio del mundo.
     * 0 = presión ambiente.
     * Positiva = sobrepresión.
     * Negativa = subpresión / vacío.
     * Sin límites en el descriptor — los objetos concretos configuran
     * su límite estructural en su PhysicalState.
     */
    public static final PhysicalProperty<CoreDomains.Pressure> PRESSURE =
        PhysicalProperty.builder(CoreDomains.Pressure.class, "pressure")
            .equilibrium(0.0)
            .description("Presión local relativa al equilibrio ambiental")
            .build();
}
