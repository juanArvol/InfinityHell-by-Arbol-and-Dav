package Game.Items.Types.Ammulets.Effects;

import Game.Engine.GameEventBus;
import Game.Gameplay.Events.DamageEvent;
import Game.Items.Types.Ammulets.AmuletEffect;
import Game.Items.Types.Bullets.Definition.Bullet;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * Explosion Shield — Amuleto que anula daño de explosiones propias
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * DESCRIPCIÓN:
 *   Protege al portador del daño causado por sus propias balas explosivas.
 *   Intercepta eventos de daño donde la fuente es un Bullet y cancela el daño.
 *
 * USO TÍPICO:
 *   • Armas con MetheorBullet u otras balas que causan daño en área
 *   • Permite jugar agresivamente sin riesgo de auto-daño
 *   • Útil en espacios cerrados donde las explosiones son inevitables
 *
 * MECÁNICA:
 *   • Se suscribe a DamageEvent en onEquip()
 *   • Si la fuente del daño es instanceof Bullet → cancela el evento
 *   • El empuje/knockback de explosiones se mantiene (solo anula daño)
 *   • Se desuscribe en onUnequip() para limpiar el listener
 *
 * ARQUITECTURA:
 *   Este efecto usa el sistema de eventos (DamageEvent) en lugar de modificar
 *   directamente WeaponStats o BulletBehavior, siguiendo el patrón event-driven
 *   para efectos reactivos.
 *
 * LIMITACIONES:
 *   • Solo protege del daño, no del empuje (design intencional)
 *   • No protege de daño de enemigos o ambiental
 *   • No stackeable: múltiples copias no aumentan el efecto
 *
 * EJEMPLO DE FLUJO:
 *   1. Player dispara MetheorBullet
 *   2. Bullet explota cerca del Player
 *   3. MetheorBullet llama Player.receiveDamage(5000, bullet)
 *   4. Player publica DamageEvent(target=Player, amount=5000, source=bullet)
 *   5. ExplosionShieldEffect.onDamage() detecta source instanceof Bullet
 *   6. Cancela el evento con event.cancel()
 *   7. Player no recibe daño (pero sí recibe empuje de la explosión)
 */
public final class ExplosionShieldEffect implements AmuletEffect {

    private GameEventBus.Subscription damageSubscription;

    // ══ Lifecycle Hooks ═══════════════════════════════════════════════════

    /**
     * Suscribirse a DamageEvent cuando el amuleto se equipa.
     *
     * @param bus el GameEventBus del mundo actual
     */
    @Override
    public void onEquip(GameEventBus bus) {
        // Guardar la suscripción para cancelarla después
        this.damageSubscription = bus.subscribe(DamageEvent.class, this::onDamageEvent);
    }

    /**
     * Cancelar la suscripción cuando el amuleto se desequipa.
     */
    @Override
    public void onUnequip() {
        if (damageSubscription != null) {
            damageSubscription.cancel();
            damageSubscription = null;
        }
    }

    // ══ Event Handlers ════════════════════════════════════════════════════

    /**
     * Intercepta eventos de daño y cancela los causados por Bullets.
     *
     * @param event el evento de daño a procesar
     */
    private void onDamageEvent(DamageEvent event) {
        // Solo nos interesa proteger de daño de balas (explosiones propias)
        Object source = event.getSource();
        
        if (source instanceof Bullet) {
            // Es una bala propia → cancelar el daño
            event.cancel();
        }
    }

    // ══ No modifica stats ni behavior ═════════════════════════════════════
    // Este efecto es puramente reactivo (event-based), no modifica
    // propiedades del arma ni del proyectil.
}
