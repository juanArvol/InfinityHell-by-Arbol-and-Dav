package Game.Engine.Entity.Properties.Modifier;

/**
 * Contrato del dominio para cualquier objeto que aporta modificaciones
 * de propiedades a una entidad.
 *
 * ── HRFC-FASE3 — Identidad type-safe para PropertyModifier ───────────────
 *
 * PropertyModifierSource es el equivalente de StatContributor para el
 * dominio de propiedades. Todo objeto que aplica PropertyModifiers debe
 * implementar esta interfaz.
 *
 * ── IDENTIDAD ─────────────────────────────────────────────────────────────
 * La identidad de un PropertyModifierSource es su referencia de objeto (==).
 * PropertyModifierContainer usa identidad de referencia para localizar y
 * eliminar todos los modificadores de una fuente al llamar
 * removeBySource(source).
 *
 * El source es la fuente que aporta el modificador. Cuando un StatusEffect
 * o buff aplica un PropertyModifier, pasa {@code this} como source. Cuando
 * el efecto expira, {@code removeBySource(this)} elimina exactamente sus
 * modificadores usando identidad de referencia (==).
 *
 * ── USO TÍPICO ────────────────────────────────────────────────────────────
 *   public class HasteEffect implements StatusEffect, PropertyModifierSource {
 *       
 *       @Override
 *       public void onApply(GameObjects entity) {
 *           PropertyModifierComponent mc = entity.getComponent(PropertyModifierComponent.class);
 *           if (mc != null) {
 *               mc.add(PropertyModifier.multiplicative(PropertyKeys.SPEED, 1.3, this));
 *           }
 *       }
 *       
 *       @Override
 *       public void onExpire(GameObjects entity) {
 *           PropertyModifierComponent mc = entity.getComponent(PropertyModifierComponent.class);
 *           if (mc != null) {
 *               mc.removeBySource(this);  // ✅ Uses 'this', not a String
 *           }
 *       }
 *   }
 *
 * ── MODIFICADORES ANÓNIMOS ────────────────────────────────────────────────
 * Para modificadores permanentes que nunca se revocan por fuente, usar
 * PropertyModifier.NO_SOURCE como sentinel:
 *
 *   PropertyModifier.additive(key, 50.0, PropertyModifier.NO_SOURCE);
 *
 * ── COMPATIBILIDAD CON ModifierSource (Causality) ─────────────────────────
 * PropertyModifierSource es DIFERENTE de ModifierSource (capa de causalidad).
 * 
 *   PropertyModifierSource → identidad de LIFECYCLE (quién aplica el modifier)
 *   ModifierSource         → identidad de CAUSALIDAD (qué lo causó, para chains)
 *
 * Un PropertyModifier puede tener ambos:
 *   - source: PropertyModifierSource (para removeBySource)
 *   - causalSource: ModifierSource (para cadenas causales)
 *
 * Son ortogonales y cumplen propósitos diferentes.
 *
 * ── DIFERENCIA CON StatContributor ────────────────────────────────────────
 * StatContributor tiene un método {@code contribute(ModifierWriter)} porque
 * un contributor puede aportar múltiples StatModifiers a la vez.
 *
 * PropertyModifierSource NO tiene ese método porque PropertyModifiers se
 * crean y añaden individualmente según se necesiten. Un source puede crear
 * múltiples PropertyModifiers si lo requiere el diseño, pero no existe un
 * protocolo de contribución batch como en el sistema de stats.
 *
 * Si en el futuro se requiere un protocolo de contribución batch para
 * PropertyModifiers, se puede añadir sin romper esta interfaz (ya que
 * actualmente es un marker interface con solo debugName()).
 */
public interface PropertyModifierSource {
    
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
     * @return nombre descriptivo (ej: "HasteRune", "BurningEffect")
     */
    default String debugName() {
        return getClass().getSimpleName();
    }
}
