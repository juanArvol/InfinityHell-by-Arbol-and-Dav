package Game.Enemys.Core;

import Game.Enemys.Core.AI.EnemyContext;
import Game.Enemys.Core.Controllers.EnemyAIController;
import Game.Enemys.Core.Controllers.EnemyAttackController;
import Game.Enemys.Core.Controllers.EnemyComponentRegistry;
import Game.Enemys.Core.Controllers.EnemyMovementController;
import Game.Enemys.Core.Controllers.EnemyPhaseController;
import Game.Enemys.Core.Variables.EnemyVariables;
import Game.Engine.Colisions.Filter.CollisionProfile;
import Game.Engine.ContextualUpdatable;
import Game.Engine.Entity.Attributes.EntityAttributes;
import Game.Engine.Entity.Combat.AttackSources;
import Game.Engine.Entity.Components.Collisions.ColliderComponent;
import Game.Engine.Entity.Components.HealthComponent;
import Game.Engine.Entity.Components.Physics2DComponent;
import Game.Engine.Entity.Components.StatusEffectComponent;
import Game.Engine.Entity.EntityInfoProvider;
import Game.Engine.Entity.Flags.EntityFlags;
import Game.Engine.Entity.Stats.EntityStats;
import Game.Engine.Entity.Stats.RuntimeStats;
import Game.Engine.GameEventBus;
import Game.Engine.GameMath.Logic2D.Transform2D;
import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Engine.MovingObjects;
import Game.Engine.RenderEngine.Sprites.SizeSyncMode;
import Game.Engine.Simulation.ComponentType;
import Game.Engine.Simulation.EntityId;
import Game.Engine.Simulation.SimulationHandle;
import Game.Engine.Simulation.Storage.EntityStore;
import Game.Gameplay.Events.OnEnemyDeathEvent;
import Sprites.Core.SpriteHandle;

/**
 * Núcleo único de todos los enemigos del juego.
 *
 * ── HRFC-007 — Living Entity Core ────────────────────────────────────────
 * Enemy ya no posee EnemyStats, EnemyFlags ni EnemyAttributes.
 * Ahora consume los tipos genéricos del Living Entity Core:
 *
 *   EntityStats      (era EnemyStats)
 *   EntityFlags      (era EnemyFlags)
 *   EntityAttributes (era EnemyAttributes)
 *   AttackSources    → Game.Living.Combat
 *   RuntimeStats     → Game.Living.Stats
 *
 * Enemy sigue siendo un esqueleto completamente agnóstico. No instancia
 * ningún módulo — todos llegan ya construidos desde EnemyAssembler.
 *
 * ── HRFC-013 — Consolidación Definitiva del Dominio Entity ──────────────
 * - Enemy implementa EntityInfoProvider: el contrato Living del Engine.
 * - EnemyVariables reemplazada por clase abstracta tipada. El campo
 *   variables es null por defecto; los Assemblers inyectan la implementación
 *   concreta mediante setVariables() para enemigos con mecánicas únicas.
 * - HealthComponent construido con EntityStats — ya no posee estado propio.
 * - maxHealth eliminado del constructor: vive en stats.health().
 *
 * ── Módulos inyectados ────────────────────────────────────────────────────
 *   EnemyAIController         — qué decide hacer cada frame
 *   EnemyMovementController   — cómo se mueve
 *   EnemyAttackController     — qué puede atacar
 *   EnemyPhaseController      — transiciones de estado
 *   EnemyComponentRegistry    — capacidades opcionales
 *   EntityStats               — estadísticas base (BaseStats)
 *   RuntimeStats              — estadísticas efectivas (base + modificadores)
 *   EntityFlags               — capabilities + states booleanos
 *   EntityAttributes          — clasificación de dominio
 *   AttackSources             — fuentes de ataque disponibles
 *
 * ── Ciclo de update() ────────────────────────────────────────────────────
 *   1. Reset de flags volátiles de EnemyState.
 *   2. PhaseController evalúa transiciones y actualiza la fase activa.
 *   3. AIController decide la acción (solo si isAbleToMove()).
 *   4. MovementController aplica la estrategia (solo si isAbleToMove()).
 *   5. AttackController actualiza patrones y dispara (solo si isAbleToAttack()).
 *   6. ComponentRegistry actualiza todos los EnemyComponents opcionales.
 *   7. super.update() — Components del engine (health, physics, renderer…).
 */
public final class Enemy extends MovingObjects implements EntityInfoProvider, Game.Engine.Destroyable, ContextualUpdatable {

    // ── Controladores — inyectados por EnemyAssembler ─────────────────────
    private final EnemyAIController         aiController;
    private final EnemyMovementController   movementController;
    private final EnemyAttackController     attackController;
    private final EnemyPhaseController      phaseController;
    private final EnemyComponentRegistry    componentRegistry;

    // ── Módulos de estado — Living Entity Core ────────────────────────────
    private final EntityStats        stats;
    private final RuntimeStats       runtimeStats;
    private final EntityFlags        flags;
    private final EntityAttributes   attributes;
    private final AttackSources      attackSources;

    // ── Estado de animación/física (interno al engine) ────────────────────
    private final EnemyState state;

    // ── Variables específicas del enemigo concreto ────────────────────────
    // Null para enemigos sin variables propias. El Assembler inyecta la
    // implementación concreta mediante setVariables() después de construir.
    private EnemyVariables variables = null;

    // ── Components del engine ─────────────────────────────────────────────
    private final HealthComponent       health;
    private final StatusEffectComponent effects;

    // ── Bus de eventos ────────────────────────────────────────────────────
    private final GameEventBus eventBus;

    // ── DOD Storage ───────────────────────────────────────────────────────
    private final EntityStore entityStore;
    private final EntityId entityId;
    private final SimulationHandle simulationHandle;

    // ── Ciclo de vida ─────────────────────────────────────────────────────
    private boolean pendingRemoval = false;
    // ── Constructor — todos los módulos son inyectados ────────────────────

    /**
     * Constructor completo de Enemy.
     * Llamado exclusivamente por EnemyAssembler.assemble().
     * Enemy no instancia ningún módulo.
     *
     * ── HRFC-013 ──────────────────────────────────────────────────────────
     * maxHealth eliminado del constructor. La vida máxima ya vive en
     * stats.health() (configurada por el Assembler antes de llamar aquí).
     * HealthComponent recibe EntityStats en lugar de un int.
     *
     * ── DOD Migration ─────────────────────────────────────────────────────
     * EntityStore requerido para registrar entidad en arquitectura DOD.
     */
    public Enemy(Vector2D position,
                 SpriteHandle handle,
                 Game.Enemys.EnemyPhysics physics,
                 EnemyAIController aiController,
                 EnemyMovementController movementController,
                 EnemyAttackController attackController,
                 EnemyPhaseController phaseController,
                 EnemyComponentRegistry componentRegistry,
                 EntityStats stats,
                 EntityFlags flags,
                 EntityAttributes attributes,
                 AttackSources attackSources,
                 GameEventBus eventBus,
                 EntityStore entityStore) {

        super(position, handle, physics, SizeSyncMode.NONE);

        if (aiController       == null) throw new IllegalArgumentException("Enemy: aiController is required");
        if (movementController == null) throw new IllegalArgumentException("Enemy: movementController is required");
        if (attackController   == null) throw new IllegalArgumentException("Enemy: attackController is required");
        if (phaseController    == null) throw new IllegalArgumentException("Enemy: phaseController is required");
        if (componentRegistry  == null) throw new IllegalArgumentException("Enemy: componentRegistry is required");
        if (stats              == null) throw new IllegalArgumentException("Enemy: stats is required");
        if (flags              == null) throw new IllegalArgumentException("Enemy: flags is required");
        if (attributes         == null) throw new IllegalArgumentException("Enemy: attributes is required");
        if (attackSources      == null) throw new IllegalArgumentException("Enemy: attackSources is required");
        if (eventBus           == null) throw new IllegalArgumentException("Enemy: eventBus is required");
        if (entityStore        == null) throw new IllegalArgumentException("Enemy: entityStore is required");

        this.aiController       = aiController;
        this.movementController = movementController;
        this.attackController   = attackController;
        this.phaseController    = phaseController;
        this.componentRegistry  = componentRegistry;

        this.stats         = stats;
        this.runtimeStats  = new RuntimeStats(stats);
        this.flags         = flags;
        this.attributes    = attributes;
        this.attackSources = attackSources;
        this.eventBus      = eventBus;

        this.state = new EnemyState();

        this.health  = new HealthComponent(stats);
        this.effects = new StatusEffectComponent();

        addComponent(health);
        addComponent(effects);

        ColliderComponent collider = getComponent(ColliderComponent.class);
        if (collider != null) {
            collider.setProfile(CollisionProfile.ENEMY);
            collider.setSize(24, 30);
        }

        // ── DOD Registration ──────────────────────────────────────────────
        this.entityStore = entityStore;
        this.entityId = entityStore.create(ComponentType.ENEMY_MASK);
        this.simulationHandle = entityStore.getHandle(entityId);
        
        // Initialize position in DOD storage
        float px = (float)position.getX();
        float py = (float)position.getY();
        int idx = simulationHandle.index();
        entityStore.getStorage().positionsX()[idx] = px;
        entityStore.getStorage().positionsY()[idx] = py;
        
        // Initialize velocity in DOD storage (zero by default)
        entityStore.getStorage().velocitiesX()[idx] = 0.0f;
        entityStore.getStorage().velocitiesY()[idx] = 0.0f;
        
        // Initialize health in DOD storage
        int maxHealth = stats.health().getMaxHp();
        entityStore.getStorage().health()[idx] = (float)maxHealth;
        entityStore.getStorage().maxHealth()[idx] = (float)maxHealth;
        
        // Connect EnemyPhysics to DOD storage
        Game.Enemys.EnemyPhysics enemyPhysics = getPhysics();
        if (enemyPhysics != null) {
            enemyPhysics.connectDOD(entityStore.getStorage(), simulationHandle);
        }
    }

    // ── Update ────────────────────────────────────────────────────────────

    /**
     * Actualiza el Enemy con contexto de IA y tiempo real.
     *
     * ── HRFC — Real DeltaTime Authority ──────────────────────────────────
     *
     * CORRECCIÓN TEMPORAL:
     * Enemy ahora recibe deltaTime y lo propaga a todos sus controladores:
     *   - AttackController: cooldowns independientes del framerate
     *   - MovementController: estrategias con temporalidad correcta
     *   - AIController: comportamientos con timing real
     *   - PhaseController: transiciones basadas en tiempo, no en frames
     *
     * El deltaTime fluye desde GameLoop → WorldManager → WorldEnemyUpdater
     * → Enemy (aquí) → Controllers.
     *
     * ── HRFC — Enemy DOD Migration ───────────────────────────────────────
     *
     * Enemy usa arquitectura híbrida OOP + DOD:
     * - Lógica de IA/movimiento/ataques en OOP (controllers)
     * - Datos de posición/velocity synced a DOD storage (para batch queries futuras)
     * - CollisionsSystem mueve position con swept AABB
     * - PhysicsStepper sincroniza position OOP → DOD
     * - EnemyPhysics sincroniza velocity OOP ↔ DOD bidireccional
     *
     * @param ctx contexto del objetivo (player u otro Enemy); puede ser null
     * @param deltaTime tiempo real del simulation step en segundos
     */
    public void update(EnemyContext ctx, double deltaTime) {
        // ── DOD SYNC: Sincronizar DOD → Transform ANTES de CollisionsSystem ──
        // ColliderComponent.getBounds() lee Transform.getPosition(), NO storage DOD.
        // Sin esta sync, el Enemy "se mueve en DOD" pero CollisionsSystem ve la
        // posición vieja del Transform, haciendo que el Enemy actúe como bloque sólido.
        syncFromDOD();
        
        if (health.isDead()) {
            onDeath();
            return;
        }

        // 1. Reset de flags volátiles para el frame
        state.resetFrameFlags();

        // 2. Fases — evalúa transiciones y actualiza fase activa
        phaseController.update(this, deltaTime);

        // 3 + 4. IA y movimiento
        if (flags.isAbleToMove()) {
            aiController.update(this, ctx, deltaTime);
            movementController.update(this, ctx, deltaTime);
        }

        // 5. Ataques — solo si las capabilities y states lo permiten
        if (flags.isAbleToAttack()) {
            boolean attacked = attackController.update(this, ctx, deltaTime);
            if (attacked) state.setAttacking(true);
        }

        // 6. EnemyComponents opcionales
        componentRegistry.update(this, deltaTime);

        // 7. Engine components
        super.update(deltaTime);
    }

    /**
     * Variante sin deltaTime — fallback para compatibilidad legacy.
     * Usa 1/30s como deltaTime de emergencia (legacy @ 30 FPS).
     *
     * ── Mini-HRFC — Final Temporal Normalization ──────────────────────────
     * CORRECCIÓN: El sistema legacy operaba a 30 FPS, no 60 FPS.
     *
     * @deprecated Usar update(ctx, deltaTime) con tiempo real
     */
    @Deprecated
    public void update(EnemyContext ctx) {
        update(ctx, 1.0 / 30.0);
    }

    @Override
    public void update(double deltaTime) { update(null, deltaTime); }

    /**
     * Fallback sin deltaTime - compatibilidad con código legacy.
     * Usa 1/30s como deltaTime de emergencia (legacy @ 30 FPS).
     *
     * ── Mini-HRFC — Final Temporal Normalization ──────────────────────────
     * CORRECCIÓN: El sistema legacy operaba a 30 FPS, no 60 FPS.
     *
     * @deprecated Usar update(double) con deltaTime real
     */
    @Deprecated
    public void update() { update(null, 1.0 / 30.0); }

    /**
     * Implementa ContextualUpdatable — el contexto esperado es EnemyContext.
     * Propaga deltaTime correctamente a la cadena de actualización.
     *
     * ── HRFC — Real DeltaTime Authority ──────────────────────────────────
     * El deltaTime ya no se descarta — se propaga a update(ctx, deltaTime).
     */
    @Override
    public void updateWithContext(Object context, double dt) {
        update((context instanceof EnemyContext ctx) ? ctx : null, dt);
    }

    // ── Muerte ────────────────────────────────────────────────────────────

    private void onDeath() {
        eventBus.post(new OnEnemyDeathEvent(this, getTransform().getPosition()));
        markForRemoval();
    }

    // ── Daño ──────────────────────────────────────────────────────────────

    /**
     * Aplica daño al enemy.
     * Ignorado si el enemy está en estado invencible.
     */
    public void damage(int amount) {
        if (flags.isInvincible()) return;
        health.damage(amount);
    }

    // ── StatusEffects ─────────────────────────────────────────────────────

    /**
     * Añade un efecto de estado al componente de efectos del enemy.
     *
     * @param effect efecto a añadir.
     */
    public void addEffect(StatusEffectComponent.StatusEffect effect) {
        effects.add(effect);
    }

    /**
     * True si hay algún efecto activo del tipo dado.
     * Consulta completamente tipada — sin Strings como clave lógica.
     *
     * @param type clase del tipo de efecto.
     */
    public <T extends StatusEffectComponent.StatusEffect> boolean hasEffect(Class<T> type) {
        return effects.hasEffect(type);
    }

    /**
     * Elimina todos los efectos activos del tipo dado, llamando onExpire() en cada uno.
     *
     * @param type clase del tipo de efecto.
     */
    public <T extends StatusEffectComponent.StatusEffect> void removeEffects(Class<T> type) {
        effects.removeAll(type);
    }

    /**
     * Devuelve el componente de efectos de estado para acceso directo avanzado.
     * Preferir los métodos de conveniencia addEffect/hasEffect/removeEffects
     * para el código de combate normal.
     */
    public StatusEffectComponent getEffectsComponent() {
        return effects;
    }

    // ── Ciclo de vida ─────────────────────────────────────────────────────

    public void markForRemoval() {
        pendingRemoval = true;
        // Release entity from DOD storage
        if (entityStore != null && entityId != null) {
            entityStore.destroy(entityId);
        }
    }
    
    public boolean isPendingRemoval() { return pendingRemoval; }

    @Override
    public boolean isPendingDestruction() { return pendingRemoval; }

    // ── Getters de controladores ──────────────────────────────────────────

    public EnemyAIController       getAIController()        { return aiController; }
    public EnemyMovementController getMovementController()  { return movementController; }
    public EnemyAttackController   getAttackController()    { return attackController; }
    public EnemyPhaseController    getPhaseController()     { return phaseController; }
    public EnemyComponentRegistry  getComponentRegistry()   { return componentRegistry; }

    // ── Getters de módulos — Living Entity Core ───────────────────────────

    /**
     * Estadísticas base de la entidad (BaseStats).
     * Usadas por Assemblers y fases para configuración inicial.
     * En combate, leer desde getRuntimeStats() para obtener valores efectivos.
     */
    public EntityStats      getStats()         { return stats; }

    /**
     * Estadísticas efectivas en tiempo de ejecución.
     * Todo el código de combate (patrones, behaviors, projectiles) debe leer de aquí.
     * Los modificadores activos (buffs, debuffs, fases) se aplican automáticamente.
     */
    public RuntimeStats     getRuntimeStats()  { return runtimeStats; }

    public EntityFlags      getFlags()         { return flags; }
    public EntityAttributes getAttributes()    { return attributes; }
    public AttackSources    getAttackSources() { return attackSources; }

    // ── Estado de animación/física ────────────────────────────────────────

    public EnemyState getState() { return state; }

    /**
     * Variables exclusivas del tipo de enemigo concreto.
     *
     * Retorna null si este enemigo no tiene variables específicas.
     * Los sistemas que necesiten variables concretas hacen cast al subtipo:
     *
     *   if (enemy.getVariables() instanceof SansVariables sv) {
     *       int frames = sv.getTeleportFrames();
     *   }
     *
     * Los Assemblers inyectan la implementación mediante setVariables().
     */
    public EnemyVariables getVariables() { return variables; }

    /**
     * Inyecta las variables específicas del enemigo concreto.
     * Llamar desde el Assembler después de construir el Enemy.
     *
     * @param variables implementación concreta. Puede ser null para enemigos
     *                  sin variables propias.
     */
    public void setVariables(EnemyVariables variables) { this.variables = variables; }

    // ── Components del engine ─────────────────────────────────────────────

    public HealthComponent getHealthComponent() { return health; }

    public Game.Enemys.EnemyPhysics getPhysics() {
        Physics2DComponent pc = getComponent(Physics2DComponent.class);
        return pc != null ? (Game.Enemys.EnemyPhysics) pc.getPhysics() : null;
    }

    public Physics2DComponent getPhysicsComponent() {
        return getComponent(Physics2DComponent.class);
    }

    // ── Bus de eventos ────────────────────────────────────────────────────

    /**
     * Bus de eventos de esta instancia.
     * Los patrones de ataque y componentes que necesitan emitir eventos
     * deben obtenerlo desde aquí en lugar de acceder a cualquier instancia global.
     */
    public GameEventBus getEventBus() { return eventBus; }

    // ── DOD Integration ───────────────────────────────────────────────────

    /**
     * Override de getTransform() para sincronizar con almacenamiento DOD.
     * 
     * ── CRITICAL FIX — Transform Sync for ColliderComponent ──────────────
     * ColliderComponent.getBounds() lee Transform.getPosition(), NO el storage DOD.
     * Si CollisionSystem actualizó la posición en DOD pero Transform está
     * desincronizado, las colisiones fallan.
     * 
     * Solución: antes de retornar Transform, sincronizarlo con DOD storage.
     * 
     * ── CONSTRUCTOR SAFETY ────────────────────────────────────────────────
     * Durante construcción (antes de que simulationHandle esté inicializado),
     * retorna el Transform sin sincronizar. Esto es seguro porque:
     * 1. super() ya inicializó Transform con la posición correcta
     * 2. El DOD storage aún no ha modificado nada (no hay drift)
     * 3. Una vez inicializado, siempre sincroniza
     */
    @Override
    public Transform2D getTransform() {
        Transform2D t = super.getTransform();
        
        // Durante construcción, simulationHandle es null - retornar sin sync
        if (simulationHandle == null) {
            return t;
        }
        
        // Después de construcción, siempre sincronizar con DOD storage
        int idx = simulationHandle.index();
        float dodX = entityStore.getStorage().positionsX()[idx];
        float dodY = entityStore.getStorage().positionsY()[idx];
        t.setPosition(dodX, dodY);
        return t;
    }

    /**
     * Posición X desde storage DOD.
     * Usado por renderers y sistemas que leen directamente DOD.
     */
    public float getPositionX() {
        return entityStore.getStorage().positionsX()[simulationHandle.index()];
    }

    /**
     * Posición Y desde storage DOD.
     * Usado por renderers y sistemas que leen directamente DOD.
     */
    public float getPositionY() {
        return entityStore.getStorage().positionsY()[simulationHandle.index()];
    }

    /**
     * EntityId DOD para acceso directo desde sistemas.
     */
    public EntityId getEntityId() {
        return entityId;
    }

    /**
     * SimulationHandle DOD para acceso directo desde sistemas.
     */
    public SimulationHandle getSimulationHandle() {
        return simulationHandle;
    }
    
    /**
     * Sincroniza position desde Transform OOP al DOD storage.
     * Llamado por PhysicsStepper después de que CollisionsSystem mueve el objeto.
     *
     * ── HRFC — Enemy DOD Migration ───────────────────────────────────────
     * 
     * Este método garantiza que DOD storage siempre tenga la position actualizada
     * después de que CollisionsSystem aplica swept AABB y resuelve colisiones.
     */
    /**
     * Sincroniza Transform OOP → DOD storage (después de PhysicsStepper).
     */
    public void syncPositionToDOD() {
        if (simulationHandle == null || !simulationHandle.isValid()) return;
        int idx = simulationHandle.index();
        float px = (float) getTransform().getX();
        float py = (float) getTransform().getY();
        entityStore.getStorage().positionsX()[idx] = px;
        entityStore.getStorage().positionsY()[idx] = py;
    }
    
    /**
     * Sincroniza DOD storage → Transform OOP (antes de CollisionsSystem).
     * 
     * CRÍTICO: ColliderComponent.getBounds() lee Transform.getPosition().
     * Si no sincronizamos, el collider usa posición vieja y el Enemy se comporta
     * como bloque sólido inmóvil aunque la física DOD lo esté moviendo.
     */
    public void syncFromDOD() {
        if (simulationHandle == null || !simulationHandle.isValid()) return;
        int idx = simulationHandle.index();
        float px = entityStore.getStorage().positionsX()[idx];
        float py = entityStore.getStorage().positionsY()[idx];
        getTransform().getPosition().setX(px);
        getTransform().getPosition().setY(py);
    }
    
    /**
     * Mueve el Enemy en DOD storage directamente (usado por PhysicsStepper).
     * NO modifica el Transform OOP - syncFromDOD() lo actualizará en el siguiente frame.
     */
    public void moveDOD(double dx, double dy) {
        if (simulationHandle == null || !simulationHandle.isValid()) return;
        int idx = simulationHandle.index();
        entityStore.getStorage().positionsX()[idx] += (float)dx;
        entityStore.getStorage().positionsY()[idx] += (float)dy;
    }
}
