package Game.Engine.Entity.Stats;

import Game.Engine.Entity.Stats.Modifier.ModifierContainer;
import Game.Engine.Entity.Stats.Modifier.ModifierWriter;

/**
 * Estadísticas efectivas de una entidad viva en tiempo de ejecución.
 *
 * ── HRFC-012 — API declarativa mediante ModifierWriter ───────────────────
 *
 * CAMBIOS RESPECTO A HRFC-011A:
 *
 *   INTERNAMENTE MODIFICADO:
 *     apply(StatContributor) — ya no llama getContributions() ni itera
 *                              un array. Crea un ModifierWriter, llama
 *                              contributor.contribute(writer) y añade los
 *                              modificadores escritos al container.
 *
 *   SIN CAMBIOS EN LA API PÚBLICA:
 *     apply(StatContributor) / revoke(StatContributor) — contrato idéntico.
 *     addModifier(StatModifier) — modificadores permanentes sin fuente.
 *     clearModifiers() / onBaseStatsChanged() — sin cambios.
 *     getMovement/getCombat/getPerception/getResistance — sin cambios.
 *
 * ── Flujo único ───────────────────────────────────────────────────────────
 *   Todo objeto del dominio que modifica estadísticas implementa StatContributor.
 *   El motor trabaja con ese objeto directamente:
 *
 *       runtimeStats.apply(contributor);   // al activarse
 *       runtimeStats.revoke(contributor);  // al expirar
 *
 *   No existen escapes hacia Object ni APIs de revocación alternativas.
 *   Un único contrato, expresado en términos del dominio.
 *
 * ── Responsabilidad única ─────────────────────────────────────────────────
 *   RuntimeStats:
 *     ✓ Coordina la lectura de valores base (EntityStats).
 *     ✓ Delega el cálculo al ModifierContainer → ModifierBucket → evaluate().
 *     ✓ Mantiene las 4 vistas pre-alocadas (zero-alloc en steady-state).
 *     ✓ Gestiona el dirty flag de las vistas de categoría.
 *     ✓ Expone una API pública que habla únicamente en términos del dominio.
 *
 *   RuntimeStats NO:
 *     ✗ No almacena modificadores directamente.
 *     ✗ No busca modificadores por ningún identificador.
 *     ✗ No contiene ningún switch/if sobre tipos de operación.
 *     ✗ No conoce la existencia de FLAT, MULTIPLIER ni OVERRIDE.
 *     ✗ No conoce ModifierId — ese concepto no existe en este módulo.
 *     ✗ No expone ModifierBundle — es un detalle interno del motor.
 *     ✗ No expone Object como tipo arquitectónico en ningún método público.
 *
 * ── Arquitectura base / runtime ──────────────────────────────────────────
 *   EntityStats         → valores permanentes. Configurados por Assemblers y fases.
 *   StatContributor     → contrato del dominio para declarar contribuciones.
 *   ModifierContainer   → administra todos los modificadores activos (interno).
 *   RuntimeStats        → coordina EntityStats + ModifierContainer → vistas efectivas.
 *
 * ── Rendimiento ───────────────────────────────────────────────────────────
 *   - Cero allocations en steady-state (sin cambios de modificadores).
 *   - getMovement/getCombat/getPerception/getResistance: O(1) si dirty=false.
 *   - Cuando dirty=true: O(T) donde T = número de StatTargets (constante ~20).
 *   - apply/revoke: O(n) + O(T×k) — solo ocurren al activar/expirar una fuente.
 *
 * ── Uso típico ────────────────────────────────────────────────────────────
 *   // Un StatusEffect que implementa StatContributor:
 *   public class RageEffect implements StatusEffectComponent.StatusEffect, StatContributor {
 *       private final double damage = 50.0;
 *       private final double speed  = 20.0;
 *
 *       @Override
 *       public void contribute(ModifierWriter writer) {
 *           writer.add(StatTarget.COMBAT_DAMAGE,  ModifierOperations.FLAT, damage);
 *           writer.add(StatTarget.MOVEMENT_SPEED, ModifierOperations.FLAT, speed);
 *       }
 *
 *       @Override public boolean tick(GameObjects entity) { ... }
 *
 *       @Override public void onExpire(GameObjects entity) {
 *           if (entity instanceof Living living)
 *               living.getRuntimeStats().revoke(this);
 *       }
 *   }
 *
 *   // Uso desde código de gameplay — sin ningún concepto de infraestructura:
 *   entity.getRuntimeStats().apply(rageEffect);
 *   entity.getRuntimeStats().revoke(rageEffect);
 */
public class RuntimeStats {

    private final EntityStats       base;
    private final ModifierContainer container;

    // ── Vistas pre-alocadas — nunca se vuelven a crear ───────────────────
    private final MovementStats   movementView   = new MovementStats();
    private final CombatStats     combatView     = new CombatStats();
    private final PerceptionStats perceptionView = new PerceptionStats();
    private final ResistanceStats resistanceView = new ResistanceStats();

    // ── Writer reutilizable — nunca se vuelve a crear ────────────────────
    private final ModifierWriter writer = new ModifierWriter();

    /**
     * Dirty flag de las vistas de categoría.
     * true cuando el ModifierContainer fue modificado y las vistas
     * necesitan recomputarse.
     */
    private boolean dirty = true;

    // ── Constructor ───────────────────────────────────────────────────────

    public RuntimeStats(EntityStats base) {
        this.base      = base;
        this.container = new ModifierContainer();
    }

    // ── API del dominio — el contrato principal ───────────────────────────

    /**
     * Aplica las contribuciones de un objeto del dominio al sistema de stats.
     *
     * <p>Reutiliza el {@link ModifierWriter} pre-alocado: invoca
     * {@link ModifierWriter#reset(StatContributor)} para asociarlo al contributor,
     * llama {@link StatContributor#contribute(ModifierWriter)}, drena el buffer
     * al container y libera las referencias con {@link ModifierWriter#reset(StatContributor) reset}.
     * Cero allocations en esta llamada.
     *
     * <p>Llamar una sola vez al activar la fuente (StatusEffect, fase,
     * habilidad...). Llamadas múltiples acumularían los modificadores.
     *
     * @param contributor objeto del dominio cuyas contribuciones se aplican.
     *                    No hace nada si null.
     */
    public void apply(StatContributor contributor) {
        if (contributor == null) return;
        writer.reset(contributor);
        contributor.contribute(writer);
        int count = writer.count();
        for (int i = 0; i < count; i++) {
            container.addModifier(writer.get(i));
        }
        writer.reset(StatModifier.NO_SOURCE); // libera referencias al contributor
        if (count > 0) dirty = true;
    }

    /**
     * Revoca todas las contribuciones de un objeto del dominio.
     *
     * <p>Elimina del container todos los StatModifiers cuya fuente coincida
     * con {@code contributor} por identidad de referencia (==). Esto funciona
     * porque ModifierWriter construyó cada StatModifier con el contributor
     * como source durante la llamada a {@link #apply(StatContributor)}.
     *
     * <p>Llamar al expirar o desactivar la fuente. Seguro llamar múltiples
     * veces — si no quedan modificadores de esa fuente, no ocurre nada.
     *
     * @param contributor objeto del dominio cuyas contribuciones se revocan.
     *                    No hace nada si null.
     */
    public void revoke(StatContributor contributor) {
        if (contributor == null) return;
        container.removeBySource(contributor);
        dirty = true;
    }

    // ── Modificadores permanentes sueltos ─────────────────────────────────

    /**
     * Añade un modificador permanente sin fuente rastreable.
     *
     * <p>Usar para modificadores que se aplican una sola vez y nunca
     * necesitan revocarse (p.ej. un bonus de equipo permanente gestionado
     * por el propio sistema de equipamiento). Para modificadores revocables,
     * preferir {@link #apply(StatContributor)}.
     *
     * @param modifier modificador a añadir.
     */
    public void addModifier(StatModifier modifier) {
        container.addModifier(modifier);
        dirty = true;
    }

    /** Elimina todos los modificadores activos. */
    public void clearModifiers() {
        if (!container.isEmpty()) {
            container.clearAll();
            dirty = true;
        }
    }

    /**
     * Notifica a RuntimeStats que los valores base de EntityStats cambiaron.
     * Invalida la caché de todos los buckets para que recomputen en la siguiente lectura.
     * Llamar tras mutaciones de EntityStats fuera del constructor (p.ej. en fases).
     */
    public void onBaseStatsChanged() {
        container.invalidateAll();
        dirty = true;
    }

    // ── Vistas de categorías — CERO ALLOCATIONS ───────────────────────────

    /**
     * Vista de MovementStats con modificadores aplicados.
     * Devuelve siempre la misma instancia pre-alocada.
     * O(1) si no hubo cambios; O(T) en el primer acceso tras un cambio.
     */
    public MovementStats getMovement() {
        if (dirty) recompute();
        return movementView;
    }

    /** Vista de CombatStats con modificadores aplicados. */
    public CombatStats getCombat() {
        if (dirty) recompute();
        return combatView;
    }

    /** Vista de PerceptionStats con modificadores aplicados. */
    public PerceptionStats getPerception() {
        if (dirty) recompute();
        return perceptionView;
    }

    /** Vista de ResistanceStats con modificadores aplicados. */
    public ResistanceStats getResistance() {
        if (dirty) recompute();
        return resistanceView;
    }

    // ── Accesos directos de conveniencia ──────────────────────────────────

    /** Velocidad efectiva. Equivalente a getMovement().getSpeed(). */
    public double getSpeed()             { return getMovement().getSpeed(); }

    /** Daño efectivo (double). */
    public double getDamage()            { return getCombat().getDamage(); }

    /** Daño efectivo (int). */
    public int    getDamageInt()         { return getCombat().getDamageInt(); }

    /** Rango de ataque efectivo. */
    public double getAttackRange()       { return getCombat().getAttackRange(); }

    /** Cooldown de ataque efectivo (int). */
    public int    getAttackCooldownInt() { return getCombat().getAttackCooldownInt(); }

    /** Rango de teletransporte efectivo. */
    public double getTeleportRange()     { return getCombat().getTeleportRange(); }

    /** Rango de visión efectivo. */
    public double getVisionRange()       { return getPerception().getVisionRange(); }

    // ── Acceso al ModifierContainer (para callers avanzados) ──────────────

    /**
     * Expone el ModifierContainer subyacente para operaciones avanzadas.
     * Llamar {@link #markDirty()} después de cualquier mutación directa
     * para garantizar que las vistas se recomputen.
     *
     * <p>Uso normal: preferir apply/revoke/addModifier sobre este método.
     */
    public ModifierContainer getContainer() {
        return container;
    }

    /**
     * Marca las vistas de categoría como inválidas.
     * Llamar manualmente solo si se muta el ModifierContainer directamente
     * via {@link #getContainer()}.
     */
    public void markDirty() {
        dirty = true;
    }

    // ── Recompute interno — NO contiene lógica matemática ─────────────────

    /**
     * Rellena las 4 vistas pre-alocadas consultando el valor efectivo de
     * cada estadística al ModifierContainer.
     *
     * RuntimeStats NO sabe cómo se calcula cada valor. Únicamente solicita:
     *   container.getBucket(target).evaluate(base)
     *
     * Toda la matemática reside en las implementaciones de ModifierOperation.
     */
    private void recompute() {
        recomputeMovement();
        recomputeCombat();
        recomputePerception();
        recomputeResistance();
        dirty = false;
    }

    private void recomputeMovement() {
        MovementStats b = base.movement();
        movementView.setSpeed(        eval(b.getSpeed(),         StatTarget.MOVEMENT_SPEED));
        movementView.setAcceleration( eval(b.getAcceleration(),  StatTarget.MOVEMENT_ACCELERATION));
        movementView.setFriction(     eval(b.getFriction(),      StatTarget.MOVEMENT_FRICTION));
        movementView.setJumpHeight(   eval(b.getJumpHeight(),    StatTarget.MOVEMENT_JUMP_HEIGHT));
        movementView.setDashDistance( eval(b.getDashDistance(),  StatTarget.MOVEMENT_DASH_DISTANCE));
    }

    private void recomputeCombat() {
        CombatStats b = base.combat();
        combatView.setDamage(         eval(b.getDamage(),         StatTarget.COMBAT_DAMAGE));
        combatView.setDefense(        eval(b.getDefense(),        StatTarget.COMBAT_DEFENSE));
        combatView.setAttackRange(    eval(b.getAttackRange(),    StatTarget.COMBAT_ATTACK_RANGE));
        combatView.setAttackCooldown( eval(b.getAttackCooldown(), StatTarget.COMBAT_ATTACK_COOLDOWN));
        combatView.setCriticalChance( eval(b.getCriticalChance(), StatTarget.COMBAT_CRITICAL_CHANCE));
        combatView.setTeleportRange(  eval(b.getTeleportRange(),  StatTarget.COMBAT_TELEPORT_RANGE));
    }

    private void recomputePerception() {
        PerceptionStats b = base.perception();
        perceptionView.setVisionRange(   eval(b.getVisionRange(),    StatTarget.PERCEPTION_VISION_RANGE));
        perceptionView.setHearingRange(  eval(b.getHearingRange(),   StatTarget.PERCEPTION_HEARING_RANGE));
        perceptionView.setDetectionAngle(eval(b.getDetectionAngle(), StatTarget.PERCEPTION_DETECTION_ANGLE));
    }

    private void recomputeResistance() {
        ResistanceStats b = base.resistance();
        resistanceView.setFireResistance(    eval(b.getFireResistance(),     StatTarget.RESISTANCE_FIRE));
        resistanceView.setIceResistance(     eval(b.getIceResistance(),      StatTarget.RESISTANCE_ICE));
        resistanceView.setElectricResistance(eval(b.getElectricResistance(), StatTarget.RESISTANCE_ELECTRIC));
        resistanceView.setPoisonResistance(  eval(b.getPoisonResistance(),   StatTarget.RESISTANCE_POISON));
        resistanceView.setCurseResistance(   eval(b.getCurseResistance(),    StatTarget.RESISTANCE_CURSE));
    }

    /** Consulta el valor efectivo de un target al container. */
    private double eval(double base, StatTarget target) {
        return container.getBucket(target).evaluate(base);
    }
}
