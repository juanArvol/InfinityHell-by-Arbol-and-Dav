package Game.Enemys.EnemyTypes.Flying;

import Game.Enemys.Core.AI.Behaviors.FlyingBehavior;
import Game.Enemys.Core.Enemy;
import Game.Enemys.Core.EnemyAssembler;
import Game.Enemys.Core.EnemyDefinition;
import Game.Enemys.Core.Movement.FlyingMovement;
import Game.Enemys.EnemyPhysics;
import Game.Engine.Entity.Attributes.EntityAttributes;
import Game.Engine.Entity.Combat.AttackSource;
import Game.Engine.Entity.Combat.AttackSources;
import Game.Engine.Entity.Components.Visuals.AnimationControllerComponent;
import Game.Engine.Entity.Components.Visuals.ShadowComponent;
import Game.Engine.Entity.Flags.EntityFlags;
import Game.Engine.Entity.Stats.EntityStats;
import Sprites.Entity.Enemys.noBoss.Zombie.EnemyAssets;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * FlyingEnemyAssembler — Enemigo Volador
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * DESCRIPCIÓN:
 *   Enemigo aéreo que ignora terreno. Vuela libremente controlado por AI.
 *   Usa steering force para movimiento ágil sin física gravitacional.
 *
 * COMPORTAMIENTO:
 *   • Movimiento: Aéreo (FlyingMovement con steering)
 *   • AI: Flying Behavior (persigue en el aire)
 *   • Ataque: Embestida aérea
 *   • Facción: MONSTER (hostil a todos)
 *
 * FÍSICA:
 *   - Gravedad mínima (evita flotar infinito si AI falla)
 *   - Sin drag (aerodinámica perfecta)
 *   - Movimiento controlado 100% por AI
 *   - No usa parámetros terrestres
 *
 * USO TÍPICO:
 *   Enemigos aéreos, jefes voladores, unidades de soporte
 */
public final class FlyingEnemyAssembler extends EnemyAssembler {

    // ═════════════════════════════════════════════════════════════════════
    // SECCIÓN 1: PROPIEDADES FÍSICAS
    // ═════════════════════════════════════════════════════════════════════
    
    /** Gravedad mínima en units/s² (evita flotar infinito) */
    private static final double GRAVITY = 0.1;
    
    /** Masa del enemigo (ligero para vuelo ágil) */
    private static final double MASS = 40.0;
    
    /** Área efectiva (0 = sin drag, aerodinámica perfecta) */
    private static final double EFFECTIVE_AREA = 0.0;
    
    /** Coeficiente de resistencia del aire (0 = sin resistencia) */
    private static final double DRAG_COEFFICIENT = 0.0;
    
    /** Factor de deslizamiento (1.0 = sin fricción, siempre en aire) */
    private static final double SLIDE = 1.0;

    // ═════════════════════════════════════════════════════════════════════
    // SECCIÓN 2: PROPIEDADES DE MOVIMIENTO AÉREO
    // ═════════════════════════════════════════════════════════════════════
    
    /** Fuerza de steering para control AI en units/s² */
    private static final double STEERING_FORCE = 0.9;

    // ═════════════════════════════════════════════════════════════════════
    // SECCIÓN 3: PROPIEDADES DE GAMEPLAY
    // ═════════════════════════════════════════════════════════════════════
    
    /** Vida máxima */
    private static final int MAX_HEALTH = 80;
    
    /** Velocidad máxima en units/s */
    private static final double MAX_SPEED = 380.0;

    // ═════════════════════════════════════════════════════════════════════
    // SECCIÓN 4: ASSEMBLER IMPLEMENTATION
    // ═════════════════════════════════════════════════════════════════════

    @Override
    protected EnemyDefinition definition() {
        EnemyPhysics physics = createPhysics();
        
        return EnemyDefinition.builder()
            .sprite(EnemyAssets.flyingHandle)
            .health(MAX_HEALTH)
            .physics(physics)
            .collider(24, 30)
            .shadow(18, 7)
            .animation("idle")
            .build();
    }

    @Override
    protected void configureStats(EntityStats stats) {
        stats.setSpeed(MAX_SPEED);
    }

    @Override
    protected void configureFlags(EntityFlags flags) {
        flags.setFlying(true);
    }

    @Override
    protected void configureAttributes(EntityAttributes attributes) {
        attributes.setFaction(EntityAttributes.Faction.MONSTER)
                  .setEntityClass(EntityAttributes.EntityClass.COMMON);
    }

    @Override
    protected void configureAttackSources(AttackSources sources) {
        sources.add(AttackSource.NATURAL);
    }

    @Override
    protected void configureMovement(Enemy enemy) {
        enemy.getMovementController().setStrategy(new FlyingMovement());
        enemy.getAIController().setBehavior(
            new FlyingBehavior(STEERING_FORCE)
        );
    }

    @Override
    protected void configureVisual(Enemy enemy) {
        enemy.addComponent(new ShadowComponent(18, 7));
        enemy.addComponent(new AnimationControllerComponent(EnemyAssets.flyingHandle, "idle"));
    }

    // ═════════════════════════════════════════════════════════════════════
    // SECCIÓN 5: CONFIGURACIÓN INTERNA
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Crea configuración física del enemigo volador.
     * 
     * Física aérea minimalista:
     *   - Gravedad casi nula (0.1 u/s²)
     *   - Sin drag (aerodinámica perfecta)
     *   - Sin fricción (siempre en el aire)
     *   - NO necesita parámetros terrestres (nunca toca suelo)
     *   - Movimiento controlado 100% por FlyingMovement AI
     */
    private EnemyPhysics createPhysics() {
        return new EnemyPhysics(
            GRAVITY,
            MASS,
            EFFECTIVE_AREA,
            DRAG_COEFFICIENT,
            SLIDE
        );
    }
}
