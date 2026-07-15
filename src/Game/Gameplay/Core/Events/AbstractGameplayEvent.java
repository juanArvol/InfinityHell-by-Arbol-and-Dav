package Game.Gameplay.Core.Events;

/**
 * Implementación base de GameplayEvent con cancelación incorporada.
 *
 * ── PROPÓSITO ─────────────────────────────────────────────────────────────
 * Evitar que cada evento concreto duplique el boilerplate de cancelled/cancel().
 * Los eventos concretos extienden esta clase y solo declaran sus campos
 * específicos.
 *
 * ── USO ──────────────────────────────────────────────────────────────────
 *
 *   public class OnDamageEvent extends AbstractGameplayEvent {
 *       private final GameObjects source;   // quién daña (inmutable)
 *       private final GameObjects target;   // quién recibe (inmutable)
 *       private double damage;              // cuánto daña (modificable)
 *
 *       public OnDamageEvent(GameObjects source, GameObjects target, double damage) {
 *           this.source = source;
 *           this.target = target;
 *           this.damage = damage;
 *       }
 *
 *       public double getDamage()            { return damage; }
 *       public void   setDamage(double v)    { this.damage = v; }
 *       public GameObjects getSource()       { return source; }
 *       public GameObjects getTarget()       { return target; }
 *   }
 *
 *   // Disparo con interceptación:
 *   OnDamageEvent event = new OnDamageEvent(shooter, enemy, 50.0);
 *   channel.fire(event);
 *   if (!event.isCancelled()) {
 *       enemy.damage((int) event.getDamage());
 *   }
 */
public abstract class AbstractGameplayEvent implements GameplayEvent {

    private boolean cancelled = false;

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void cancel() {
        this.cancelled = true;
    }
}
