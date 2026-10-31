package Game.Items.Types.Ammulets;

import Game.Engine.GameEventBus;
import Game.Items.Types.Bullets.BulletComport.BulletBehavior;
import Game.Items.Types.Weapons.WeaponType.WeaponStats;

/**
 * Efecto de un amuleto — qué modifica sobre el arma o las balas.
 *
 * ── DISEÑO ───────────────────────────────────────────────────────────────
 * Mismo contrato que WeaponModifier, pero para amuletos acumulables.
 * Los métodos son idénticos a propósito: el AmuletSystem puede aplicar
 * una lista de AmuletEffects en el mismo pipeline que antes usaba
 * WeaponModifier, sin cambiar la firma de ModifiedWeapon.
 *
 * ── ACUMULACIÓN ───────────────────────────────────────────────────────────
 * Cuando el jugador tiene N copias del mismo amuleto, el AmuletSystem
 * llama applyToStats() / wrapBehavior() N veces consecutivas.
 * Los efectos deben ser diseñados para que esto tenga sentido.
 *
 * Ejemplo correcto:   +5 de daño por llamada → x3 copias = +15 daño ✓
 * Ejemplo incorrecto: "setear piercing a true" → x3 copias = igual que x1 ✗
 *                     (usa incremento, no asignación)
 *
 * ── LIFECYCLE HOOKS (EVENT-BASED EFFECTS) ─────────────────────────────────
 *
 * Desde la adición del sistema de eventos (DamageEvent, etc.), los amuletos
 * pueden suscribirse a eventos del GameEventBus para implementar efectos
 * reactivos (ej: cancelar daño de explosiones, triggers al recibir golpes).
 *
 * onEquip(bus)   → Llamado cuando el amuleto se equipa
 *                  Aquí se registran listeners y se guarda la Subscription
 *
 * onUnequip()    → Llamado cuando el amuleto se desequipa o el stack llega a 0
 *                  Aquí se cancela la Subscription para limpiar el listener
 *
 * IMPORTANTE: onEquip/onUnequip se llaman POR CADA COPIA del amuleto.
 * Si el jugador tiene 3x del mismo amuleto:
 *   - onEquip() se llama 3 veces
 *   - onUnequip() se llama 3 veces (cuando se pierde todo el stack)
 *
 * El efecto debe guardar la Subscription internamente para cancelarla después.
 */
public interface AmuletEffect {

    /**
     * Modifica una copia de WeaponStats.
     * El input ya es una copia mutable — el WeaponComport base no se altera.
     * Override si el amuleto cambia propiedades del arma (daño, cadencia, spread).
     */
    default void applyToStats(WeaponStats stats) {
        // No-op por defecto
    }

    /**
     * Envuelve el BulletBehavior con comportamiento adicional.
     * Override si el amuleto afecta qué hace la bala al impactar
     * (perforación, rebote, área, etc.).
     *
     * @param base behavior actual (puede ser un wrapper de amuletos previos)
     * @return behavior con el efecto del amuleto añadido
     */
    default BulletBehavior wrapBehavior(BulletBehavior base) {
        return base; // No-op por defecto
    }

    /**
     * Llamado cuando el amuleto es equipado por el jugador.
     * Override para suscribirse a eventos del bus (DamageEvent, etc.).
     *
     * IMPORTANTE: Guardar la Subscription retornada para cancelarla en onUnequip().
     *
     * Ejemplo:
     * <pre>
     *   private Subscription damageSub;
     *
     *   {@literal @}Override
     *   public void onEquip(GameEventBus bus) {
     *       damageSub = bus.subscribe(DamageEvent.class, this::onDamage);
     *   }
     *
     *   {@literal @}Override
     *   public void onUnequip() {
     *       if (damageSub != null) damageSub.cancel();
     *   }
     * </pre>
     *
     * @param bus el GameEventBus del mundo actual
     */
    default void onEquip(GameEventBus bus) {
        // No-op por defecto
    }

    /**
     * Llamado cuando el amuleto es desequipado o el stack llega a 0.
     * Override para cancelar subscripciones al bus de eventos.
     */
    default void onUnequip() {
        // No-op por defecto
    }
}
