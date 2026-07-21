package Game.Engine.World.Physics;

/**
 * Marcador de dominio físico — identifica la naturaleza de una magnitud.
 *
 * ── HRFC-015 — World Simulation Core (iteración final) ────────────────────
 *
 * ── PROPÓSITO ─────────────────────────────────────────────────────────────
 * PhysicalDomain es una interfaz marcadora que actúa como parámetro de tipo
 * en {@link PhysicalQuantity}. Su función es exclusivamente semántica:
 * distingue en tiempo de compilación magnitudes de distintos dominios.
 *
 * Esto significa que el compilador rechaza operaciones entre dominios distintos:
 *
 *   PhysicalQuantity<CoreDomains.Thermal>    temperature = ...;
 *   PhysicalQuantity<CoreDomains.Electrical> charge      = ...;
 *
 *   temperature.add(charge);   // ERROR DE COMPILACIÓN — dominios incompatibles
 *   temperature.add(temperature); // OK
 *
 * ── POR QUÉ UN MARCADOR Y NO UNA CLASE ABSTRACTA ─────────────────────────
 * PhysicalDomain no necesita comportamiento. Su única responsabilidad es
 * ser un tipo distinto para cada dominio. Una interfaz marcadora vacía
 * cumple exactamente con ese contrato sin imponer overhead ni jerarquía
 * de implementación.
 *
 * ── CÓMO EXTENDER ─────────────────────────────────────────────────────────
 * Para definir un nuevo dominio físico en cualquier módulo del Engine o
 * del juego, simplemente declarar una clase o interfaz que implemente
 * PhysicalDomain:
 *
 *   // En un módulo de magia:
 *   public interface MagicDomains {
 *       final class Mana     implements PhysicalDomain {}
 *       final class Arcane   implements PhysicalDomain {}
 *       final class Ethereal implements PhysicalDomain {}
 *   }
 *
 *   PhysicalQuantity<MagicDomains.Mana> manaLevel = PhysicalQuantity.of(100.0);
 *
 * No es necesario registrar el dominio en ningún sistema central.
 * Un dominio existe en el momento en que se declara como tipo concreto.
 *
 * ── ENGINE vs GAMEPLAY ────────────────────────────────────────────────────
 * El Engine define los dominios físicos fundamentales en {@link CoreDomains}.
 * El Gameplay puede definir dominios adicionales (magia, química, radiación)
 * sin modificar el Engine. La cadena de simulación opera sobre
 * {@code PhysicalQuantity<?>} cuando necesita generalidad, y sobre tipos
 * específicos cuando necesita precisión de dominio.
 */
public interface PhysicalDomain {
    // Interfaz marcadora — sin métodos.
    // La identidad del dominio es el tipo Java que la implementa.
}
