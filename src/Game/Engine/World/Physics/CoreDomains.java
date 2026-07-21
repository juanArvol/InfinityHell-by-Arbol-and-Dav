package Game.Engine.World.Physics;

/**
 * Marcadores de dominio físico fundamentales del World Simulation Core.
 *
 * ── HRFC-017 — Consolidación Definitiva del Modelo Declarativo ────────────
 *
 * ── RESPONSABILIDAD ÚNICA ─────────────────────────────────────────────────
 * CoreDomains únicamente define marcadores de tipo para el sistema de genéricos.
 *
 * Cada clase interna implementa PhysicalDomain y actúa como parámetro de tipo
 * en PhysicalQuantity<D>, garantizando en tiempo de compilación que no se
 * mezclan magnitudes de dominios distintos:
 *
 *   PhysicalQuantity<CoreDomains.Thermal>    temperature = PhysicalQuantity.of(0.0);
 *   PhysicalQuantity<CoreDomains.Electrical> charge      = PhysicalQuantity.of(0.0);
 *   temperature.add(charge);   // ERROR de compilación — dominios distintos
 *
 * ── QUÉ NO SON ────────────────────────────────────────────────────────────
 * Los dominios de CoreDomains NO representan fenómenos físicos concretos.
 * No representan calor, fuego, electricidad, agua ni ningún otro concepto
 * de gameplay. Son únicamente marcadores de clasificación de magnitudes.
 *
 *   Thermal    → magnitud de energía térmica almacenada (positiva = más caliente,
 *                negativa = más fría que el ambiente).
 *   Electrical → magnitud de carga eléctrica neta.
 *   Fluid      → magnitud de contenido fluídico. Rango natural [0, 1].
 *   Pressure   → magnitud de presión relativa al equilibrio ambiental.
 *   Mechanical → magnitudes de propiedades mecánicas (masa, dureza, elasticidad).
 *
 * ── QUÉ NO CONTIENEN ──────────────────────────────────────────────────────
 *   ✗ Algoritmos
 *   ✗ Reglas físicas
 *   ✗ Referencias al Solver
 *   ✗ Fenómenos físicos concretos
 *   ✗ Semántica de gameplay
 *
 * El comportamiento físico emerge de las PhysicsEquation, PairEquation y
 * PhysicsConstraint registradas en PhysicsSolver. CoreDomains no participa
 * en ninguna resolución.
 *
 * ── EXTENSIBILIDAD ────────────────────────────────────────────────────────
 * El Gameplay define sus propios dominios sin modificar este catálogo:
 *
 *   // En un módulo de magia:
 *   public interface MagicDomains {
 *       final class Mana     implements PhysicalDomain {}
 *       final class Arcane   implements PhysicalDomain {}
 *   }
 *
 *   // El PhysicsSolver integra esos dominios automáticamente
 *   // cuando se registran PhysicsEquation o PhysicsConstraint que los referencian.
 *
 * ── RELACIÓN CON PhysicalProperties ──────────────────────────────────────
 * PhysicalProperties vincula cada dominio con un descriptor PhysicalProperty
 * que añade la capa semántica (id, equilibrium, range, description).
 * CoreDomains provee el marcador de tipo; PhysicalProperties provee el metadato.
 */
public final class CoreDomains {

    private CoreDomains() {}

    // ── Dominio térmico ───────────────────────────────────────────────────

    /**
     * Marcador de dominio para magnitudes de energía térmica.
     * Temperatura, calor, frío — cualquier magnitud que representa
     * energía térmica almacenada.
     */
    public static final class Thermal     implements PhysicalDomain {}

    // ── Dominio eléctrico ─────────────────────────────────────────────────

    /**
     * Marcador de dominio para magnitudes de carga eléctrica.
     * Carga neta, potencial eléctrico — cualquier magnitud de naturaleza eléctrica.
     */
    public static final class Electrical  implements PhysicalDomain {}

    // ── Dominio fluídico ──────────────────────────────────────────────────

    /**
     * Marcador de dominio para magnitudes fluídicas.
     * Contenido de fluido, humedad, concentración de líquido.
     * Rango natural de las magnitudes de este dominio: [0, 1].
     */
    public static final class Fluid       implements PhysicalDomain {}

    // ── Dominio de presión ────────────────────────────────────────────────

    /**
     * Marcador de dominio para magnitudes de presión.
     * Presión mecánica o de fluido, relativa al equilibrio ambiental.
     */
    public static final class Pressure    implements PhysicalDomain {}

    // ── Dominio mecánico ──────────────────────────────────────────────────

    /**
     * Marcador de dominio para magnitudes mecánicas.
     * Masa, dureza, elasticidad, densidad — propiedades mecánicas de la materia.
     * Rango de las magnitudes de este dominio: positivo, nunca negativo.
     */
    public static final class Mechanical  implements PhysicalDomain {}
}
