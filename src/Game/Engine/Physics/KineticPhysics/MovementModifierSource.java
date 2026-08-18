package Game.Engine.Physics.KineticPhysics;

/**
 * Contrato del dominio para cualquier objeto que registra modificadores
 * de movimiento en el sistema de física.
 *
 * ── HRFC-FASE3 — Identidad type-safe para MovementModifier ───────────────
 *
 * MovementModifierSource es el equivalente de StatContributor para el
 * dominio de modificadores de movimiento. Todo objeto que aplica
 * MovementModifiers debe implementar esta interfaz.
 *
 * ── IDENTIDAD ─────────────────────────────────────────────────────────────
 * La identidad de un MovementModifierSource es su referencia de objeto (==).
 * ModifierStack usa identidad de referencia para localizar y eliminar
 * modificadores al llamar remove(source).
 *
 * El source es la fuente que registra el modificador. Cuando un StatusEffect
 * aplica un MovementModifier, pasa {@code this} como source. Cuando el efecto
 * expira, {@code remove(this)} elimina exactamente su modificador usando
 * identidad de referencia (==).
 *
 * ── USO TÍPICO ────────────────────────────────────────────────────────────
 *   public class PoisonEffect implements StatusEffect, MovementModifierSource {
 *       private final double slowFactor = 0.60;
 *       
 *       @Override
 *       public void onApply(GameObjects entity) {
 *           if (entity instanceof Living living) {
 *               Physics2D physics = living.getPhysics();
 *               physics.statusStack().add(this, ctx -> slowFactor);
 *           }
 *       }
 *       
 *       @Override
 *       public void onExpire(GameObjects entity) {
 *           if (entity instanceof Living living) {
 *               Physics2D physics = living.getPhysics();
 *               physics.statusStack().remove(this);  // ✅ Uses 'this', not a String
 *           }
 *       }
 *   }
 *
 * ── MODIFICADORES TEMPORALES ──────────────────────────────────────────────
 * Para modificadores temporales sin source rastreable persistente, crear
 * una instancia anónima local y conservar la referencia:
 *
 *   MovementModifierSource tempSource = new MovementModifierSource() {};
 *   stack.add(tempSource, ctx -> 0.5);
 *   // ... conservar tempSource para posterior eliminación
 *   stack.remove(tempSource);
 *
 * ── COMPATIBILIDAD CON LAMBDAS ────────────────────────────────────────────
 * Como MovementModifier es un FunctionalInterface, las lambdas siguen
 * siendo válidas. Solo cambia que ahora requieren un source asociado:
 *
 *   ANTES: stack.add("slow", ctx -> 0.5);
 *   AHORA: stack.add(source, ctx -> 0.5);
 *
 * El source identifica QUIÉN registró el modificador, no QUÉ modificador es.
 * El modificador mismo (la lambda) describe QUÉ hace (retorna 0.5).
 *
 * ── DIFERENCIA CON StatContributor ────────────────────────────────────────
 * StatContributor tiene un método {@code contribute(ModifierWriter)} porque
 * un contributor puede aportar múltiples StatModifiers a la vez.
 *
 * MovementModifierSource NO tiene ese método porque cada source típicamente
 * registra un solo MovementModifier en una stack específica (statusStack o
 * environmentStack). El source conserva la referencia al modifier (lambda)
 * internamente si necesita cambiar su comportamiento.
 */
public interface MovementModifierSource {
    
    /**
     * Nombre legible del source para debug/logging.
     * NO usar para lógica — solo para diagnóstico.
     * 
     * <p>Este método existe únicamente para facilitar debugging y logging.
     * La identidad del source es la referencia del objeto (==), NO el
     * string retornado por este método.
     * 
     * <p>Implementación por defecto retorna el nombre simple de la clase.
     * Sobreescribir si se necesita un nombre más descriptivo.
     * 
     * @return nombre descriptivo (ej: "PoisonEffect", "WindZone")
     */
    default String debugName() {
        return getClass().getSimpleName();
    }
}
