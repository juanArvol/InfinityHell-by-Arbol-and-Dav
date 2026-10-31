package Game.Gameplay.Events;

import Game.Engine.AbstractEntity;

/**
 * Evento de daño — publicado cuando una entidad está a punto de recibir daño.
 *
 * ══ ARQUITECTURA DE EVENTOS DE DAÑO ═══════════════════════════════════════
 *
 * Este evento se publica ANTES de aplicar el daño a la entidad, permitiendo
 * que sistemas externos (amuletos, efectos de estado, habilidades) puedan:
 *   • Cancelar el daño completamente
 *   • Modificar la cantidad de daño
 *   • Registrar/reaccionar al evento
 *
 * ══ FLUJO DE EJECUCIÓN ════════════════════════════════════════════════════
 *
 * 1. Entidad recibe ataque (ej: Player.receiveDamage(50))
 * 2. Se crea DamageEvent con daño=50, target=Player, source=null
 * 3. Se publica en GameEventBus
 * 4. Listeners procesan el evento:
 *    - Amuleto "Explosion Shield" detecta source instanceof Bullet
 *    - Cancela el evento con event.cancel()
 * 5. Después del bus.post():
 *    - Si event.isCancelled() → no aplicar daño, retornar
 *    - Si no → aplicar event.getAmount() (puede haber sido modificado)
 *
 * ══ INFORMACIÓN DE LA FUENTE ══════════════════════════════════════════════
 *
 * El campo "source" indica qué/quién causó el daño:
 *   • Bullet         → daño por proyectil
 *   • AbstractEntity → daño de enemigo (melee, contacto)
 *   • null           → daño ambiental (lava, poison tick, fall damage)
 *
 * Los listeners pueden usar instanceof para filtrar tipos específicos.
 *
 * ══ EJEMPLO DE USO ════════════════════════════════════════════════════════
 *
 * Listener de amuleto:
 * <pre>
 *   bus.subscribe(DamageEvent.class, event -> {
 *       if (event.getTarget() != player) return; // Solo proteger al player
 *       if (event.getSource() instanceof Bullet) {
 *           event.cancel(); // Anular todo daño de balas
 *       }
 *   });
 * </pre>
 *
 * En Player.receiveDamage():
 * <pre>
 *   public void receiveDamage(int amount, Object source) {
 *       if (playerStats.isInvulnerable()) return;
 *       
 *       DamageEvent event = new DamageEvent(this, amount, source);
 *       eventBus.post(event);
 *       
 *       if (event.isCancelled()) return;
 *       
 *       gotDamage(event.getAmount());
 *       playerStats.triggerInvulnerability();
 *   }
 * </pre>
 */
public final class DamageEvent {

    private final AbstractEntity target;
    private final Object source;
    private int amount;
    private boolean cancelled;

    /**
     * Crea un nuevo evento de daño.
     *
     * @param target   entidad que recibiría el daño
     * @param amount   cantidad de daño original
     * @param source   fuente del daño (Bullet, AbstractEntity, null para ambiental)
     */
    public DamageEvent(AbstractEntity target, int amount, Object source) {
        this.target = target;
        this.amount = amount;
        this.source = source;
        this.cancelled = false;
    }

    // ══ Getters ═══════════════════════════════════════════════════════════

    /**
     * @return la entidad que recibiría el daño
     */
    public AbstractEntity getTarget() {
        return target;
    }

    /**
     * @return la cantidad actual de daño (puede haber sido modificada por listeners)
     */
    public int getAmount() {
        return amount;
    }

    /**
     * @return la fuente del daño (Bullet, AbstractEntity, o null para ambiental)
     */
    public Object getSource() {
        return source;
    }

    /**
     * @return true si el evento ha sido cancelado (el daño no se aplicará)
     */
    public boolean isCancelled() {
        return cancelled;
    }

    // ══ Modificadores ═════════════════════════════════════════════════════

    /**
     * Modifica la cantidad de daño.
     * Permite a listeners reducir/aumentar el daño antes de aplicarlo.
     *
     * @param amount nueva cantidad de daño (debe ser >= 0)
     */
    public void setAmount(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Damage amount cannot be negative: " + amount);
        }
        this.amount = amount;
    }

    /**
     * Cancela el evento — el daño no se aplicará a la entidad.
     * Idempotente: puede llamarse múltiples veces sin efecto adverso.
     */
    public void cancel() {
        this.cancelled = true;
    }

    /**
     * Restaura el evento cancelado.
     * Útil si múltiples listeners procesan el evento y uno decide revertir
     * la cancelación de otro.
     */
    public void uncancel() {
        this.cancelled = false;
    }

    // ══ Utilidades ════════════════════════════════════════════════════════

    /**
     * Multiplica el daño por un factor.
     * Convenience method para modificar porcentualmente.
     *
     * @param factor multiplicador (ej: 0.5 = reducir 50%, 1.5 = aumentar 50%)
     */
    public void multiplyDamage(double factor) {
        this.amount = (int) (this.amount * factor);
    }

    /**
     * Reduce el daño en una cantidad fija.
     * El daño no puede ser menor que 0.
     *
     * @param reduction cantidad a reducir
     */
    public void reduceDamage(int reduction) {
        this.amount = Math.max(0, this.amount - reduction);
    }

    @Override
    public String toString() {
        return String.format(
            "DamageEvent[target=%s, amount=%d, source=%s, cancelled=%s]",
            target.getClass().getSimpleName(),
            amount,
            source != null ? source.getClass().getSimpleName() : "null",
            cancelled
        );
    }
}
