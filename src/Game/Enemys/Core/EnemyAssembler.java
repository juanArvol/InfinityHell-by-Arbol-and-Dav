package Game.Enemys.Core;

import Game.Enemys.Core.Controllers.EnemyAIController;
import Game.Enemys.Core.Controllers.EnemyAttackController;
import Game.Enemys.Core.Controllers.EnemyComponentRegistry;
import Game.Enemys.Core.Controllers.EnemyMovementController;
import Game.Enemys.Core.Controllers.EnemyPhaseController;
import Game.Engine.Entity.Attributes.EntityAttributes;
import Game.Engine.Entity.Combat.AttackSources;
import Game.Engine.Entity.Components.Collisions.ColliderComponent;
import Game.Engine.Entity.Components.PhysicsComponent;
import Game.Engine.Entity.Flags.EntityFlags;
import Game.Engine.Entity.Stats.EntityStats;
import Game.Engine.GameEventBus;
import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Engine.Physics.Core.PhysicalState;

/**
 * Contrato base del ensamblador de Enemy.
 *
 * ── HRFC-007 — Living Entity Core ────────────────────────────────────────
 * EnemyAssembler ahora instancia los tipos genéricos del Living Entity Core:
 *
 *   EntityStats      (era EnemyStats)
 *   EntityFlags      (era EnemyFlags)
 *   EntityAttributes (era EnemyAttributes)
 *   AttackSources    → Game.Living.Combat
 *
 * Los sub-métodos de configuración (configureStats, configureFlags,
 * configureAttributes, configureAttackSources) reciben los tipos de Living
 * directamente. Game.Living.* es la única fuente de verdad — HRFC-008.
 *
 * ── Ciclo de ensamblado ───────────────────────────────────────────────────
 *   1.  definition()          — obtiene el prefab estático del enemy.
 *   2.  Instancia módulos Living (stats, flags, attributes, controllers).
 *   3.  configureStats()      — sobreescribir para asignar velocidad, daño, etc.
 *   4.  configureFlags()      — sobreescribir para asignar estado inicial.
 *   5.  configureAttributes() — sobreescribir para facción, elemento, clase.
 *   6.  configureAttackSources() — sobreescribir para declarar fuentes de ataque.
 *   7.  configureMovement()   — sobreescribir para asignar la MovementStrategy.
 *   8.  configureCombat()     — sobreescribir para añadir AttackPatterns.
 *   9.  configureComponents() — sobreescribir para añadir EnemyComponents.
 *   10. configureVisual()     — sobreescribir para añadir AnimationController, shadow, etc.
 *   11. configurePhases()     — sobreescribir para registrar fases y llamar start().
 *   12. Construye el Enemy inyectando todos los módulos.
 *   13. Ajusta el collider con las dimensiones de la definición.
 *   14. Retorna el Enemy completamente configurado.
 */
public abstract class EnemyAssembler {

    // ── Mini-HRFC — Declarative PhysicalState Ownership ───────────────────
    // PhysicsComponent es opt-in. No se crean valores físicos universales.
    // El assembler concreto declara explícitamente su participación en física.

    /**
     * Construye y retorna un Enemy completamente configurado en la posición dada.
     *
     * @param position posición inicial en el mundo.
     * @param eventBus bus de eventos para que el Enemy pueda emitir OnEnemyDeathEvent
     *                 y otros eventos de ciclo de vida.
     * @return Enemy completamente configurado, listo para añadir al mundo.
     */
    public final Enemy assemble(Vector2D position, GameEventBus eventBus) {
        EnemyDefinition def = definition();

        // ── 1. Crear módulos Living Entity Core ───────────────────────────
        EntityStats      stats      = new EntityStats();
        EntityFlags      flags      = new EntityFlags();
        EntityAttributes attributes = new EntityAttributes();

        EnemyAIController       aiController       = new EnemyAIController(null);
        EnemyMovementController movementController = new EnemyMovementController();
        EnemyAttackController   attackController   = new EnemyAttackController();
        EnemyPhaseController    phaseController    = new EnemyPhaseController();
        EnemyComponentRegistry  componentRegistry  = new EnemyComponentRegistry();
        AttackSources           attackSources      = new AttackSources();

        // ── 2. Aplicar defaults del prefab (si los define) ────────────────
        if (def.defaultStats      != null) def.defaultStats.configure(stats);
        if (def.defaultFlags      != null) def.defaultFlags.configure(flags);
        if (def.defaultAttributes != null) def.defaultAttributes.configure(attributes);

        // ── 3. Delegar configuración del assembler concreto ───────────────
        configureStats(stats);
        configureFlags(flags);
        configureAttributes(attributes);
        configureAttackSources(attackSources);

        // ── 4. Inicializar HP máximo ─────────────────────────────────────────
        // La fuente de verdad del HP es def.maxHealth (declarado en definition()).
        // Si configureStats() sobreescribió el HP mediante stats.setMaxHp(n),
        // ese valor ya está en stats — no se pisa. Si no lo sobreescribió,
        // el HP queda en el default de HealthStats (1) y aquí se inicializa
        // con el valor de la definición.
        // setMaxHp() inicializa también currentHp al máximo.
        if (stats.health().getMaxHp() <= 1) {
            stats.setMaxHp(def.maxHealth);
        }

        // ── 5. Construir Enemy con todos los módulos inyectados ───────────
        // maxHealth ya NO se pasa al constructor. HealthComponent lo lee desde
        // stats.health(), que fue inicializado en el paso 4.
        Enemy enemy = new Enemy(
            position,
            def.sprite,
            def.physics,
            aiController,
            movementController,
            attackController,
            phaseController,
            componentRegistry,
            stats,
            flags,
            attributes,
            attackSources,
            eventBus
        );

        // ── 6. Ajustar collider ───────────────────────────────────────────
        ColliderComponent col = enemy.getComponent(ColliderComponent.class);
        if (col != null) {
            col.setSize(def.colliderW, def.colliderH);
        }

        // ── 6b. PhysicsComponent — opt-in y declarativo ───────────────────
        // Mini-HRFC — Declarative PhysicalState Ownership
        //
        // El assembler concreto declara explícitamente su PhysicalState
        // mediante configurePhysics(). Si no declara nada, el Enemy no
        // participa en física universal.
        //
        // Esto elimina la imposición de valores físicos universales como
        // temperatura ambiente o humedad default a todos los enemigos.
        PhysicalState.Builder physicsBuilder = PhysicalState.builder();
        configurePhysics(physicsBuilder);
        PhysicalState physicalState = physicsBuilder.build();

        // Solo agregar PhysicsComponent si el assembler declaró propiedades
        if (!physicalState.isEmpty()) {
            enemy.addComponent(new PhysicsComponent(physicalState));
        }

        // ── 7. Configurar comportamiento (requiere enemy ya construido) ────
        configureMovement(enemy);
        configureCombat(enemy);
        configureComponents(enemy);
        configureVisual(enemy);
        configurePhases(enemy);

        return enemy;
    }

    // ── Template method obligatorio ───────────────────────────────────────

    /** Retorna la definición estática del enemy: sprite, HP, física, collider. */
    protected abstract EnemyDefinition definition();

    // ── Sub-métodos de configuración — sobreescribir los necesarios ───────

    /**
     * Configura las estadísticas numéricas: velocidad, daño, rangos, cooldowns.
     * Llamado antes de construir el Enemy.
     *
     * @param stats EntityStats a configurar (Living Entity Core).
     */
    protected void configureStats(EntityStats stats) {}

    /**
     * Configura los flags booleanos de estado inicial.
     * Llamado antes de construir el Enemy.
     *
     * @param flags EntityFlags a configurar (Living Entity Core).
     */
    protected void configureFlags(EntityFlags flags) {}

    /**
     * Configura los atributos de dominio: facción, elemento, clase, etc.
     * Llamado antes de construir el Enemy.
     *
     * @param attributes EntityAttributes a configurar (Living Entity Core).
     */
    protected void configureAttributes(EntityAttributes attributes) {}

    /**
     * Declara las fuentes de ataque: NATURAL, MAGIC, WEAPON, etc.
     * Llamado antes de construir el Enemy.
     *
     * @param sources AttackSources a configurar (Living Entity Core).
     */
    protected void configureAttackSources(AttackSources sources) {}

    /**
     * Asigna la estrategia de movimiento inicial al MovementController.
     * Llamado después de construir el Enemy.
     */
    protected void configureMovement(Enemy enemy) {}

    /**
     * Añade los AttackPatterns al AttackController.
     * Llamado después de construir el Enemy.
     */
    protected void configureCombat(Enemy enemy) {}

    /**
     * Registra EnemyComponents opcionales en el ComponentRegistry.
     * Llamado después de construir el Enemy.
     */
    protected void configureComponents(Enemy enemy) {}

    /**
     * Añade componentes visuales del engine: AnimationController, sombra, etc.
     * Llamado después de construir el Enemy.
     */
    protected void configureVisual(Enemy enemy) {}

    /**
     * Configura el estado físico del Enemy.
     * 
     * Mini-HRFC — Declarative PhysicalState Ownership
     * 
     * Hook opt-in para que el assembler concreto declare explícitamente
     * las propiedades físicas que este tipo de enemigo posee.
     * 
     * Si no se sobreescribe, el Enemy NO recibe PhysicsComponent y por
     * tanto no participa en los dominios físicos (thermal, electrical,
     * fluid, mechanical).
     * 
     * Ejemplo de uso en un assembler concreto:
     * 
     * <pre>{@code
     * @Override
     * protected void configurePhysics(PhysicalState.Builder physics) {
     *     physics
     *         .register(ThermalProperties.TEMPERATURE, 20.0)
     *         .register(FluidProperties.HUMIDITY, 0.5)
     *         .registerMaterial(materialComponent::registerInto);
     * }
     * }</pre>
     * 
     * @param physics Builder vacío para registrar propiedades físicas.
     */
    protected void configurePhysics(PhysicalState.Builder physics) {}

    /**
     * Registra las fases del Enemy y llama phaseController.start(enemy).
     * Llamado después de construir el Enemy y de configureComponents().
     */
    protected void configurePhases(Enemy enemy) {}
}
