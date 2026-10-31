package Game.Enemys.EnemyTypes.Zombie;

import Game.Enemys.Core.AI.Behaviors.AggressiveBehavior;
import Game.Enemys.Core.Enemy;
import Game.Enemys.Core.EnemyAssembler;
import Game.Enemys.Core.EnemyDefinition;
import Game.Enemys.Core.Movement.GroundMovement;
import Game.Enemys.EnemyPhysics;
import Game.Engine.Entity.Attributes.EntityAttributes;
import Game.Engine.Entity.Combat.AttackSource;
import Game.Engine.Entity.Combat.AttackSources;
import Game.Engine.Entity.Components.Visuals.AnimationControllerComponent;
import Game.Engine.Entity.Stats.EntityStats;
import Sprites.Entity.Enemys.noBoss.Zombie.EnemyAssets;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * ZombieAssembler — Enemigo Terrestre Estándar
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * DESCRIPCIÓN:
 *   Enemigo terrestre básico con movimiento por gravedad. Camina sobre el suelo,
 *   persigue al jugador cuando lo detecta, ataca cuerpo a cuerpo.
 *
 * COMPORTAMIENTO:
 *   • Movimiento: Terrestre (GroundMovement)
 *   • AI: Agresiva (persigue cuando detecta)
 *   • Ataque: Cuerpo a cuerpo al entrar en rango
 *   • Facción: UNDEAD (no ataca a otros undead)
 *
 * FÍSICA:
 *   - Gravedad normal (igual que Player)
 *   - Más pesado que Player (más difícil de empujar)
 *   - Movimiento terrestre con fricción estándar
 *
 * USO TÍPICO:
 *   Enemigo común, spawns masivos, hordas
 */
public final class ZombieAssembler extends EnemyAssembler {

    // ═════════════════════════════════════════════════════════════════════
    // SECCIÓN 1: PROPIEDADES FÍSICAS
    // ═════════════════════════════════════════════════════════════════════
    
    /** Gravedad en units/s² (igual que Player para consistencia) */
    private static final double GRAVITY = 23.4;
    
    /** Masa del enemigo (2× Player, más difícil de empujar) */
    private static final double MASS = 80.0;
    
    /** Área efectiva expuesta al aire */
    private static final double EFFECTIVE_AREA = 1.0;
    
    /** Coeficiente de resistencia del aire */
    private static final double DRAG_COEFFICIENT = 0.00001;
    
    /** Factor de deslizamiento en tierra (fricción) */
    private static final double SLIDE = 0.8;
    
    /** Aceleración en tierra en units/s² */
    private static final double ACCEL_GROUND = 2190.0;
    
    /** Aceleración en aire en units/s² (poco control aéreo) */
    private static final double ACCEL_AIR = 300.0;
    
    /** Velocidad máxima en tierra en units/s */
    private static final double SPEED_MAX_GROUND = 900.0;
    
    /** Velocidad máxima en aire in units/s */
    private static final double SPEED_MAX_AIR = 600.0;

    // ═════════════════════════════════════════════════════════════════════
    // SECCIÓN 2: PROPIEDADES DE GAMEPLAY
    // ═════════════════════════════════════════════════════════════════════
    
    /** Vida máxima */
    private static final int MAX_HEALTH = 100;
    
    /** Rango de detección del jugador en units */
    private static final double DETECTION_RANGE = 9000.0;
    
    /** Rango de ataque cuerpo a cuerpo in units */
    private static final double ATTACK_RANGE = 50.0;
    
    /** Velocidad de movimiento AI in units/s */
    private static final double MOVE_SPEED = 250.0;
    
    /** Daño por ataque */
    private static final int DAMAGE = 10;

    // ═════════════════════════════════════════════════════════════════════
    // SECCIÓN 3: ASSEMBLER IMPLEMENTATION
    // ═════════════════════════════════════════════════════════════════════

    @Override
    protected EnemyDefinition definition() {
        EnemyPhysics physics = createPhysics();
        
        return EnemyDefinition.builder()
            .sprite(EnemyAssets.normalHandle)
            .health(MAX_HEALTH)
            .physics(physics)
            .collider(24, 30)
            .animation("idle")
            .animation("walk")
            .lootTable("zombie_drops")
            .build();
    }

    @Override
    protected void configureStats(EntityStats stats) {
        stats.setSpeed(MOVE_SPEED)
             .setDamage(DAMAGE)
             .setVisionRange(DETECTION_RANGE)
             .setAttackRange(ATTACK_RANGE);
    }

    @Override
    protected void configureAttributes(EntityAttributes attributes) {
        attributes.setFaction(EntityAttributes.Faction.UNDEAD)
                  .setEntityClass(EntityAttributes.EntityClass.COMMON);
    }

    @Override
    protected void configureAttackSources(AttackSources sources) {
        sources.add(AttackSource.NATURAL);
    }

    @Override
    protected void configureMovement(Enemy enemy) {
        enemy.getMovementController().setStrategy(new GroundMovement());
        enemy.getAIController().setBehavior(
            new AggressiveBehavior(DETECTION_RANGE, ATTACK_RANGE)
        );
    }

    @Override
    protected void configureVisual(Enemy enemy) {
        enemy.addComponent(new AnimationControllerComponent(EnemyAssets.normalHandle, "idle"));
    }

    // ═════════════════════════════════════════════════════════════════════
    // SECCIÓN 4: CONFIGURACIÓN INTERNA
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Crea configuración física del Zombie.
     * 
     * Física terrestre completa con:
     *   - Gravedad normal (como Player)
     *   - Mayor masa (más resistente a knockback)
     *   - Parámetros terrestres (accel, velocidad máxima)
     */
    private EnemyPhysics createPhysics() {
        return new EnemyPhysics(
            GRAVITY,
            MASS,
            EFFECTIVE_AREA,
            DRAG_COEFFICIENT,
            SLIDE,
            ACCEL_GROUND,
            ACCEL_AIR,
            SPEED_MAX_GROUND,
            SPEED_MAX_AIR
        );
    }
}
