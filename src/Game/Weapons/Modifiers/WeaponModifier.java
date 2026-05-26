package Game.Weapons.Modifiers;

import Game.Bullets.BulletComport.BulletBehavior;
import Game.Weapons.WeaponType.WeaponStats;

/**
 * Modificador de arma — altera el comportamiento de WeaponStats o de las balas
 * que el arma produce, sin tocar el WeaponComport ni la lógica de disparo.
 *
 * ── DISEÑO ───────────────────────────────────────────────────────────────
 * Patrón: Decorator / Pipeline
 *   Weapon base → WeaponModifier A → WeaponModifier B → resultado final
 *
 * Cada modificador puede:
 *   1. Alterar WeaponStats (cooldown, spread, damage, speed, pellets).
 *   2. Wrappear el BulletBehavior que sale del arma (añadir efectos on-hit).
 *
 * NO existe hardcode de tipo. Los modificadores concretos extienden esta clase
 * y sobreescriben solo lo que necesitan.
 *
 * ── EJEMPLO ──────────────────────────────────────────────────────────────
 *   WeaponModifier explosive = new ExplosiveModifier();
 *   WeaponStats final = explosive.applyToStats(weapon.getComport().getStats());
 *   BulletBehavior finalBehavior = explosive.wrapBehavior(baseBehavior);
 */
public abstract class WeaponModifier {

    /** ID único del modificador. Sirve para deduplicar y serializar. */
    public abstract String getId();

    /**
     * Aplica el modificador sobre un WeaponStats.
     * El input es una COPIA mutable — el original no se toca.
     * Override solo si el modificador cambia propiedades del arma.
     *
     * @param stats stats base del arma (copia)
     */
    public void applyToStats(WeaponStats stats) {
        // No-op por defecto — modificadores que no tocan stats no hacen nada.
    }

    /**
     * Envuelve el BulletBehavior original con comportamiento adicional.
     * Override si el modificador afecta qué hace la bala al impactar.
     *
     * @param base comportamiento base de la bala
     * @return comportamiento wrapeado (o base si no aplica)
     */
    public BulletBehavior wrapBehavior(BulletBehavior base) {
        return base; // Por defecto no modifica el behavior
    }

    /**
     * Indica si este modificador es compatible con otro (para evitar stacks inválidos).
     * Por defecto todos son compatibles entre sí.
     */
    public boolean isCompatibleWith(WeaponModifier other) {
        return true;
    }

    /**
     * Prioridad de aplicación en el pipeline.
     * Menor = se aplica primero. Útil si el orden importa (ej: damage buff antes de multiplicador).
     */
    public int priority() {
        return 100;
    }

    @Override
    public String toString() {
        return "WeaponModifier[" + getId() + "]";
    }
}
