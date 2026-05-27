package Game.Ammulets;

import Game.Bullets.BulletComport.BulletBehavior;
import Game.Weapons.WeaponType.WeaponStats;

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
}
