package Game.Gameplay.Core.Capabilities;

/**
 * Contrato de una capacidad de gameplay.
 *
 * ── QUÉ ES UNA CAPACIDAD ─────────────────────────────────────────────────
 * Una capacidad expresa QUÉ PUEDE HACER una entidad, o QUÉ PUEDE
 * RECIBIRLE. Es la respuesta a preguntas como:
 *
 *   "¿Puede esta entidad rebotar?"
 *   "¿Puede recibir modificadores mágicos?"
 *   "¿Puede congelarse?"
 *   "¿Puede dividirse en múltiples entidades?"
 *
 * ── DIFERENCIA CON TAG ────────────────────────────────────────────────────
 * - Tag     → "¿qué ES?"  → identidad, clasificación
 * - Capability → "¿qué PUEDE?" → comportamiento aceptado, interacciones
 *
 * Un tag ORGANIC describe qué es la entidad.
 * Una capability CAN_BE_FROZEN describe con qué puede interactuar.
 *
 * ── POR QUÉ REEMPLAZA AL instanceof ──────────────────────────────────────
 * Con instanceof:
 *   if (other instanceof Enemy e) { applyPoison(e); }
 *
 * El sistema de hielo no puede aplicarse a un Boss que implementa
 * "Enemy" pero no debería congelarse — no hay forma de expresar esa
 * excepción sin if adicionales o flags en la clase.
 *
 * Con capacidades:
 *   CapabilityComponent caps = other.getComponent(CapabilityComponent.class);
 *   if (caps != null && caps.has(CoreCapabilities.CAN_BE_FROZEN)) {
 *       applyFreeze(other);
 *   }
 *
 * El Boss simplemente no registra CAN_BE_FROZEN. No hace falta un flag ni
 * un if especial. La ausencia de la capacidad ya expresa la excepción.
 *
 * ── DISEÑO: INTERFAZ MARCADORA ────────────────────────────────────────────
 * GameplayCapability es una interfaz marcadora. Las capacidades concretas
 * en CoreCapabilities son instancias anónimas (lambdas o singletons), una
 * por cada comportamiento. El tipo de la clase de la instancia es la
 * identidad de la capacidad.
 *
 * Alternativamente, una capacidad puede ser una clase con datos:
 *   new BounceCapability(maxBounces) — capacidad de rebotar hasta N veces.
 * En ese caso, CapabilityComponent guarda la instancia concreta y
 * el sistema puede extraerla con get(BounceCapability.class).
 */
public interface GameplayCapability {
    // Interfaz marcadora intencionalmente vacía.
    // La identidad de la capacidad es su clase/tipo, no un método.
}
