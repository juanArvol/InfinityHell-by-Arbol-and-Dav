package Game.Engine.Systems;

import Game.Engine.Entity.Components.StatusEffectComponent;
import Game.Engine.Entity.EntityInfoProvider;
import Game.Engine.GameObjects;
import java.util.List;

/**
 * Sistema de sincronización de efectos de estado.
 *
 * ── HRFC-014 — GAP-11: Sincronizador canónico ───────────────────────────
 *
 * Responsabilidad única: para cada entidad que tenga un StatusEffectComponent,
 * llamar a entity.getFlags().synchronize(sec) para proyectar el estado actual
 * de los efectos sobre los flags derivados (ImpairmentFlags, DamageFlags).
 *
 * StatusEffectSystem NO gestiona el ciclo de vida de los efectos.
 * Eso lo hace StatusEffectComponent.update() internamente (tick + onExpire)
 * durante la fase de update de componentes del objeto.
 *
 * ── Orden de ejecución en el game loop ───────────────────────────────────
 *
 *   1. object.update() para cada objeto
 *      → StatusEffectComponent.update() se llama aquí (es un Component)
 *      → tick() + onExpire() de cada efecto activo
 *   2. StatusEffectSystem.update(objects)   ← este sistema
 *      → entity.getFlags().synchronize(sec)
 *      → ImpairmentFlags y DamageFlags quedan actualizados
 *   3. CollisionsSystem.update(objects)
 *      → usa flags actualizados para lógica de movimiento
 *
 * ── Declaración de fenómenos en StatusEffects ─────────────────────────────
 *
 * Para que un StatusEffect proyecte un fenómeno, debe implementar
 * una de las interfaces marcadoras de este sistema:
 *
 *   HasDamagePhenomenon     → proyecta en DamageFlags
 *   HasImpairmentPhenomenon → proyecta en ImpairmentFlags
 *
 * Ejemplo — efecto que produce el fenómeno de quemadura:
 *
 *   public class BurningEffect
 *           implements StatusEffectComponent.StatusEffect,
 *                      StatusEffectSystem.HasDamagePhenomenon {
 *
 *       {@literal @}Override
 *       public StatusEffectSystem.DamagePhenomenon getDamagePhenomenon() {
 *           return StatusEffectSystem.DamagePhenomenon.BURNING;
 *       }
 *
 *       {@literal @}Override
 *       public boolean tick(GameObjects entity) { ... }
 *   }
 *
 * Ejemplo — efecto que produce el fenómeno de aturdimiento:
 *
 *   public class StunEffect
 *           implements StatusEffectComponent.StatusEffect,
 *                      StatusEffectSystem.HasImpairmentPhenomenon {
 *
 *       {@literal @}Override
 *       public ImpairmentPhenomenon getImpairmentPhenomenon() {
 *           return StatusEffectSystem.ImpairmentPhenomenon.STUNNED;
 *       }
 *   }
 *
 * ── Extensión ─────────────────────────────────────────────────────────────
 *
 * Para añadir un nuevo fenómeno (p.ej. PARALYZED):
 *   1. Añadir PARALYZED a ImpairmentPhenomenon.
 *   2. Añadir el campo y consulta en ImpairmentFlags.
 *   3. Añadir el case en EntityFlags.applyImpairmentPhenomenon().
 *   4. Los StatusEffects que produzcan parálisis implementan HasImpairmentPhenomenon.
 *   Ningún otro archivo necesita cambiar.
 */
public final class StatusEffectSystem implements EngineSystem {

    // ── Interfaces marcadoras para declarar fenómenos ─────────────────────

    /**
     * Interfaz que un StatusEffect implementa para declarar que produce
     * el fenómeno de daño periódico indicado.
     *
     * Un efecto puede producir múltiples fenómenos si es necesario (p.ej.
     * un efecto de lava que quema y corroe simultáneamente) implementando
     * la interfaz dos veces — aunque en ese caso es preferible modelarlos
     * como dos efectos independientes.
     */
    public interface HasDamagePhenomenon {
        DamagePhenomenon getDamagePhenomenon();
    }

    /**
     * Interfaz que un StatusEffect implementa para declarar que produce
     * el fenómeno de incapacitación indicado.
     */
    public interface HasImpairmentPhenomenon {
        ImpairmentPhenomenon getImpairmentPhenomenon();
    }

    // ── Catálogos de fenómenos ─────────────────────────────────────────────

    /** Fenómenos de daño periódico reconocidos por el Engine. */
    public enum DamagePhenomenon {
        BURNING, POISONED, BLEEDING, ELECTRIFIED, CORRODED, CURSED, INFECTED
    }

    /** Fenómenos de incapacitación reconocidos por el Engine. */
    public enum ImpairmentPhenomenon {
        STUNNED, ROOTED, FROZEN, SILENCED, CONFUSED, SLEEPING, FEARED, DISARMED
    }

    // ── Update ────────────────────────────────────────────────────────────

    /**
     * Sincroniza los flags derivados de todas las entidades.
     *
     * Para cada objeto que sea EntityInfoProvider, delega la proyección
     * a entity.getFlags().synchronize(sec). Los setters de los flags
     * derivados son package-private — solo EntityFlags puede llamarlos.
     *
     * @param objects lista de objetos del mundo activos en este frame
     */
    @Override
    public void update(List<GameObjects> objects) {
        for (GameObjects obj : objects) {
            if (!(obj instanceof EntityInfoProvider living)) continue;

            // Obtener el StatusEffectComponent (puede ser null si la entidad
            // no tiene efectos de estado configurados)
            StatusEffectComponent sec = obj.getComponent(StatusEffectComponent.class);

            // Delegar la proyección a EntityFlags — único punto de escritura
            // de flags derivados. EntityFlags accede a los setters
            // package-private de ImpairmentFlags y DamageFlags.
            living.getFlags().synchronize(sec);
        }
    }
}
