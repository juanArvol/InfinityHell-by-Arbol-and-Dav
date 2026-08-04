package Game.Engine.Physics.Core;

/**
 * Interfaz marcadora raíz de todos los Domain States del simulador.
 *
 * ── HRFC-032 — Evolución del SimulationContext hacia un registro extensible ─
 *
 * ── RESPONSABILIDAD ──────────────────────────────────────────────────────
 * DomainState es el contrato mínimo que toda clase de estado de dominio debe
 * cumplir para poder registrarse en el DomainStateRegistry y ser accesible
 * mediante el mecanismo genérico de SimulationContext.
 *
 * No impone métodos. No contiene lógica. Es una interfaz de marcado que
 * habilita el acceso tipado en tiempo de compilación:
 *
 *   context.state(KinematicState.class)
 *   context.state(MaterialState.class)
 *   context.state(ChemicalState.class)
 *   context.state(AcousticState.class)
 *
 * ── PRINCIPIO FUNDAMENTAL ─────────────────────────────────────────────────
 * Un DomainState:
 *   ✓ describe un área de conocimiento del simulador.
 *   ✓ es inmutable o de mutabilidad controlada (solo sistemas autorizados).
 *   ✓ puede almacenarse en DomainStateRegistry y recuperarse por tipo.
 *   ✗ no interpreta fenómenos.
 *   ✗ no ejecuta simulaciones.
 *   ✗ no contiene reglas de comportamiento.
 *   ✗ no modifica otros estados.
 *
 * ── EXTENSIBILIDAD ────────────────────────────────────────────────────────
 * Para añadir un nuevo dominio al simulador:
 *   1. Crear la clase del nuevo estado en su package de dominio.
 *   2. Implementar DomainState.
 *   3. Registrarlo en SimulationContext.Builder (o en runtime via
 *      context.register(newState)).
 *
 * El núcleo del Engine no necesita conocer el nuevo dominio.
 * SimulationContext no necesita un nuevo getter.
 * PhysicsSolver no cambia.
 *
 * ── DOMINOS ACTUALES ──────────────────────────────────────────────────────
 *   PhysicalState        — propiedades físicas del objeto (T, P, carga…)
 *   KinematicState       — estado de movimiento (v, KE, momentum…)
 *   MaterialState        — propiedades del material (conductividad, dureza…)
 *   ContactState         — interacción con otros cuerpos (onGround, normal…)
 *   EnvironmentState     — condiciones del entorno (gravedad, viento, T°…)
 *   ChemicalState        — estado químico (oxidación, pH, reactividad…)
 *   OpticalState         — propiedades ópticas (reflectividad, refracción…)
 *   AcousticState        — propiedades acústicas (resonancia, absorción…)
 *   NuclearState         — propiedades nucleares (radioactividad, isótopos…)
 *   BiomechanicalState   — estado biomecánico de entidades vivas (fatiga…)
 */
public interface DomainState {
    // Interfaz marcadora — sin métodos obligatorios.
    // La semántica viene del tipo concreto, no de esta interfaz.
}
