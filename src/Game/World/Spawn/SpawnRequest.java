package Game.World.Spawn;

import java.util.Objects;

/**
 * Solicitud concreta de ejecución de un spawn.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * SpawnRequest combina un SpawnDescriptor con la condición/trigger de cuándo
 * debe ejecutarse, y lleva el estado de ejecución en tiempo real.
 *
 * Es la unidad que SpawnRegistry gestiona y SpawnSystem evalúa cada tick.
 *
 * ── CICLO DE VIDA ─────────────────────────────────────────────────────────
 *   1. Se crea con un descriptor y una condición o trigger.
 *   2. Se registra en SpawnRegistry via SpawnSystem.register().
 *   3. SpawnSystem la evalúa cada tick:
 *        a. Verifica si la condición está activa O el trigger disparado.
 *        b. Verifica cooldown y maxInstances.
 *        c. Si todo pasa: llama descriptor.strategy.create(point.samplePosition())
 *                         y añade el resultado al mundo.
 *   4. Si isOneTime() es true, se marca como completada y SpawnRegistry la retira.
 *
 * ── MODOS DE ACTIVACIÓN ───────────────────────────────────────────────────
 * Un SpawnRequest puede usar:
 *   1. Solo condición (SpawnCondition): activa mientras la condición sea true.
 *   2. Solo trigger (SpawnTrigger): activa cuando el trigger dispara.
 *   3. Condición + trigger: requiere AMBOS (el trigger AND la condición).
 *
 * Si ni condición ni trigger están presentes, el request nunca se ejecuta.
 *
 * ── BUILDERS ──────────────────────────────────────────────────────────────
 *   SpawnRequest.withCondition(descriptor, condition)
 *   SpawnRequest.withTrigger(descriptor, trigger)
 *   SpawnRequest.immediate(descriptor)   → se ejecuta una vez en el próximo tick
 */
public final class SpawnRequest {

    private final SpawnDescriptor descriptor;
    private final SpawnCondition  condition;
    private final SpawnTrigger    trigger;
    private final boolean         oneTime;

    // ── Estado de ejecución ───────────────────────────────────────────────

    /**
     * ── HRFC — Unified DeltaTime Migration ───────────────────────────────
     *
     * MIGRACIÓN: cooldownRemaining ahora es double (segundos) en lugar de int (frames).
     */
    private double  cooldownRemaining = 0.0;
    private int     activeInstances   = 0;
    private boolean completed         = false;
    private int     totalSpawned      = 0;

    // ── Constructores ─────────────────────────────────────────────────────

    private SpawnRequest(SpawnDescriptor descriptor,
                         SpawnCondition condition,
                         SpawnTrigger trigger,
                         boolean oneTime) {
        Objects.requireNonNull(descriptor, "descriptor cannot be null");
        this.descriptor = descriptor;
        this.condition  = condition;
        this.trigger    = trigger;
        this.oneTime    = oneTime;
    }

    // ── Factories ─────────────────────────────────────────────────────────

    /**
     * Request continuo: activo mientras la condición sea verdadera.
     */
    public static SpawnRequest withCondition(SpawnDescriptor descriptor,
                                             SpawnCondition condition) {
        return new SpawnRequest(descriptor, condition, null, false);
    }

    /**
     * Request disparado: activo cuando el trigger dispara.
     * Si el trigger es oneShot, el request se desactivará solo.
     */
    public static SpawnRequest withTrigger(SpawnDescriptor descriptor,
                                           SpawnTrigger trigger) {
        return new SpawnRequest(descriptor, null, trigger, trigger.isOneShot());
    }

    /**
     * Request de un solo uso: se ejecuta en el próximo tick y se retira.
     */
    public static SpawnRequest immediate(SpawnDescriptor descriptor) {
        return new SpawnRequest(descriptor, SpawnCondition.always(),
                                null, true);
    }

    /**
     * Request con condición Y trigger: ambos deben estar activos simultáneamente.
     */
    public static SpawnRequest withConditionAndTrigger(SpawnDescriptor descriptor,
                                                       SpawnCondition condition,
                                                       SpawnTrigger trigger) {
        return new SpawnRequest(descriptor, condition, trigger, trigger.isOneShot());
    }

    // ── Evaluación por SpawnSystem ────────────────────────────────────────

    /**
     * Retorna true si este request debe intentar spawnear en el tick actual.
     * SpawnSystem llama este método; no llamar desde código de gameplay.
     *
     * @param world el mundo activo
     * @return true si las condiciones de spawn se cumplen
     */
    public boolean shouldSpawnNow(Game.World.Core.World world, double deltaTime) {
        if (completed)            return false;
        if (cooldownRemaining > 0) return false;

        int maxInst = descriptor.getMaxInstances();
        if (maxInst > 0 && activeInstances >= maxInst) return false;

        boolean conditionMet = (condition == null) || condition.isMet(world, deltaTime);
        boolean triggerMet   = (trigger   == null) || trigger.isTriggered(world);

        // Si solo hay condición: conditionMet debe ser true
        // Si solo hay trigger:   triggerMet debe ser true
        // Si hay ambos:          ambos deben ser true
        if (condition != null && trigger != null) return conditionMet && triggerMet;
        if (condition != null) return conditionMet;
        if (trigger   != null) return triggerMet;

        return false; // sin condición ni trigger → nunca spawnea
    }

    /**
     * Notifica que un objeto fue spawnado exitosamente.
     * SpawnSystem llama este método después de añadir el objeto al mundo.
     *
     * ── HRFC — Unified DeltaTime Migration ───────────────────────────────
     * ── Mini-HRFC — Final Temporal Normalization ──────────────────────────
     *
     * CAMBIO: cooldown ahora debe convertirse de ticks a segundos.
     * SpawnDescriptor.getCooldownTicks() debería migrar a getCooldownSeconds().
     * 
     * CORRECCIÓN CRÍTICA: El sistema legacy operaba a 30 FPS, no 60 FPS.
     * Por ahora, asumiendo getCooldownTicks() retorna frames → convertir a segundos @ 30 FPS.
     */
    public void onSpawned() {
        totalSpawned++;
        activeInstances++;

        // Reiniciar cooldown (MIGRACIÓN: convertir ticks → segundos @ 30 FPS)
        int cdTicks = descriptor.getCooldownTicks();
        if (cdTicks > 0) {
            cooldownRemaining = cdTicks / 30.0; // TODO: migrar SpawnDescriptor a getCooldownSeconds()
        }

        // Consumir trigger si existe
        if (trigger != null) trigger.onConsumed();

        // Completar si es de un solo uso
        if (oneTime) completed = true;
    }

    /**
     * Notifica que una instancia fue destruida/removida del mundo.
     * Llamar desde el listener de OnEnemyDeathEvent o equivalente.
     */
    public void onInstanceRemoved() {
        if (activeInstances > 0) activeInstances--;
    }

    /**
     * Avanza el cooldown. SpawnSystem llama esto cada tick.
     *
     * ── HRFC — Unified DeltaTime Migration ───────────────────────────────
     *
     * CAMBIO: Ahora recibe deltaTime y decrementa en segundos.
     *
     * @param deltaTime tiempo del simulation step en segundos
     */
    public void tickCooldown(double deltaTime) {
        if (cooldownRemaining > 0) {
            cooldownRemaining -= deltaTime;
            if (cooldownRemaining < 0) cooldownRemaining = 0;
        }
    }

    // ── Acceso ────────────────────────────────────────────────────────────

    public SpawnDescriptor getDescriptor()       { return descriptor;       }
    public SpawnCondition  getCondition()        { return condition;        }
    public SpawnTrigger    getTrigger()          { return trigger;          }
    public boolean         isOneTime()           { return oneTime;          }
    public boolean         isCompleted()         { return completed;        }
    public int             getActiveInstances()  { return activeInstances;  }
    public int             getTotalSpawned()     { return totalSpawned;     }
    
    /**
     * ── HRFC — Unified DeltaTime Migration ───────────────────────────────
     *
     * CAMBIO: getCooldownRemaining() ahora retorna double (segundos).
     */
    public double getCooldownRemaining() { return cooldownRemaining; }

    /** Fuerza la marcación como completada. Útil para cancelar un spawn activo. */
    public void cancel() { completed = true; }
}
