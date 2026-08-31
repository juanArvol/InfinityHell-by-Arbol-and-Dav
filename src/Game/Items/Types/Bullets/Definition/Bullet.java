package Game.Items.Types.Bullets.Definition;

import Game.Engine.Colisions.Filter.CollisionProfile;
import Game.Engine.Entity.Components.Collisions.ColliderComponent;
import Game.Engine.Entity.Components.Physics2DComponent;
import Game.Engine.Entity.Components.PhysicsComponent;
import Game.Engine.Entity.Components.Visuals.HitBoxComponent;
import Game.Engine.Entity.Components.Visuals.SpriteRendererComponent;
import Game.Engine.GameEventBus;
import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Engine.GameObjects;
import Game.Engine.Lifecycle.EntityContext;
import Game.Engine.Lifecycle.SimulationLifecycle;
import Game.Engine.Physics.Core.PhysicalState;
import Game.Engine.Simulation.ComponentType;
import Game.Engine.Simulation.EntityId;
import Game.Engine.Simulation.SimulationHandle;
import Game.Engine.Simulation.Storage.EntityStore;
import Game.Gameplay.Events.ProjectileEvents;
import Game.Items.Creation.ItemRarity;
import Game.Items.Types.Bullets.BulletComport.BulletBehavior;
import Game.Items.Types.Bullets.BulletComport.BulletDamageDOD;
import Game.Items.Types.Bullets.BulletComport.BulletLifeDOD;
import Game.Items.Types.Bullets.BulletComport.BulletPhysics;
import Game.Items.Types.Bullets.BulletComport.BulletPhysicsDOD;
import Game.Items.Types.Bullets.BulletID;
import Game.Items.Types.Bullets.Flyweight.BulletFlyweight;
import Game.Items.Types.Bullets.Movement.LinearMovement;
import Game.Items.Types.Bullets.OffScreenTracker;
import Game.Items.Types.Bullets.ProjectileMovement;
import Game.Items.Types.Bullets.ProjectileTransformer;
import Game.Items.Types.Bullets.ResettableMovement;
import java.awt.Color;

/**
 * Proyectil del juego — entidad en el mundo con ciclo de vida finito.
 *
 * ── SEPARACIÓN DE RESPONSABILIDADES ──────────────────────────────────────
 *
 *   Bullet             → entidad en el mundo (posición, colisiones, ciclo de vida)
 *   BulletBehavior     → QUÉ HACE al impactar, en cada frame, al adjuntarse y expirar
 *   ProjectileMovement → CÓMO SE MUEVE cada frame
 *   BulletFlyweight    → recursos COMPARTIDOS e INMUTABLES (texture, profile, tamaño)
 *   BulletPhysics      → velocidad individual por frame
 *   BulletLife         → estado de vida individual
 *
 * ── FLYWEIGHT ─────────────────────────────────────────────────────────────
 *
 * Bullet recibe un BulletFlyweight que contiene los recursos inmutables
 * compartidos con todas las Bullets del mismo tipo:
 *
 *   - texture          → BufferedImage del sprite (no se duplica por instancia)
 *   - collisionProfile → perfil de colisión (no se recalcula por instancia)
 *   - width / height   → dimensiones del collider (no se repiten por instancia)
 *
 * Los recursos individuales (velocidad, vida, daño, behavior, movement) siguen
 * siendo propiedad exclusiva de cada Bullet.
 *
 * ── ESTADO INDIVIDUAL ─────────────────────────────────────────────────────
 *
 *   - BulletLife         → remaining, dead: estado de vida individual
 *   - BulletPhysics      → xSpeed, ySpeed: velocidad individual por frame
 *   - double damage      → calculado por ModifiedWeapon, individual
 *   - BulletBehavior     → puede tener wrappers con estado (PiercingWrapper, etc.)
 *   - ProjectileMovement → HomingMovement tiene target; Sinusoidal/Boomerang tienen estado
 *   - destroyEventFired  → flag de lifecycle individual
 *   - projectileContext  → inyectado por el pool
 *
 * ── CICLO DE VIDA ─────────────────────────────────────────────────────────
 *
 *   Spawn  → BulletFactory.build() construye la instancia con Flyweight
 *              → behavior.onAttached(this) es llamado en el constructor
 *   Alive  → update() se llama cada frame:
 *              1. BulletLife.advance()      → ¿sigue vivo?
 *              2. movement.tick(this)        → actualizar velocidad
 *              3. behavior.onUpdate(this)    → lógica del behavior
 *              4. moveByPhysics()            → mover la posición
 *              5. super.update()             → actualizar Components
 *   Expire → BulletLife.advance() retorna false
 *              → behavior.onExpire(this, ctx) → hook de expiración
 *              → emitir OnProjectileExpire al bus
 *              → behavior.onRelease(this)    → cleanup garantizado
 *              → emitir OnProjectileDestroy al bus
 *   Impact → onCollisionWith(other) → behavior.onCollision(this, other)
 *            → si muere: behavior.onRelease(this) + OnProjectileDestroy
 *   Destroy→ WorldObjectsContainer.flush() lo elimina del mundo
 *   Pool   → ownerPool.release(this) → vuelve al pool
 *
 *   GARANTÍA: onRelease() se invoca SIEMPRE exactamente una vez por ciclo
 *   de vida, independientemente de la causa de muerte.
 *
 * ── POOL RESET (resetState) ───────────────────────────────────────────────
 *
 *   resetState() acepta behavior, movement y flyweight del nuevo blueprint.
 *   Si el flyweight cambió (tipo diferente), se actualiza el sprite y el
 *   collisionProfile del collider existente — sin recrear componentes.
 *   Los comportamientos se restauran al estado del nuevo blueprint.
 *
 * ── CONSUMO DE INFRAESTRUCTURA DEL ENGINE ────────────────────────────────
 *
 *   Bullet consume las siguientes capacidades del Engine:
 *
 *   Engine.Lifecycle (SimulationLifecycle)
 *       → getSimulationContext() / shouldSimulate() delegados a BulletLife.
 *         El Engine puede determinar si simular este proyectil sin acoplarse
 *         a la lógica específica de BulletLife.
 *
 *   Engine.Spatial (radio declarado vía getInteractionRadius())
 *       → Bullet declara su radio de interacción. El Engine realiza la
 *         búsqueda espacial; Bullet no implementa la búsqueda.
 *
 *   Engine.Pooling (via ProjectilePool → AbstractObjectPool<Bullet>)
 *       → El pooling usa infraestructura general. Bullet no gestiona
 *         el pool directamente — solo se auto-devuelve vía ownerPool.
 *
 *   Engine.Resources (via BulletFlyweightCache → ResourceCache<K,V>)
 *       → Los recursos compartidos (textura, profile) usan el cache
 *         general del Engine.
 */
public class Bullet extends GameObjects implements Game.Engine.Destroyable, SimulationLifecycle {

    // ── Mini-HRFC — Declarative PhysicalState Ownership ───────────────────
    // El estado físico NO se impone universalmente. Cada tipo de proyectil
    // declara explícitamente su PhysicalState en su comportamiento o blueprint.

    // ── HRFC — Projectile DOD Migration ───────────────────────────────────
    // DOD Bridge — identidad y handle en EntityStore
    private EntityId entityId;
    private SimulationHandle simulationHandle;
    private EntityStore entityStore; // referencia al store para revalidación

    private final BulletLifeDOD      bulletLife;  // DOD adapter
    private final Physics2DComponent physicsComponent;
    private final BulletPhysicsDOD   physicsAdapter; // DOD adapter
    private final BulletDamageDOD    damageAdapter;  // DOD adapter
    
    /**
     * EntityContext cacheado — evita crear una nueva lambda en cada llamada
     * a getSimulationContext(). Se inicializa justo después de bulletLife
     * en el constructor para capturar la referencia correcta.
     */
    private EntityContext simulationContext;

    /**
     * Flyweight compartido — recursos inmutables del tipo de proyectil.
     *
     * No es final porque el pool puede reutilizar una instancia con un
     * blueprint de tipo diferente, en cuyo caso actualizamos el flyweight
     * (y con él, textura y collisionProfile).
     */
    private BulletFlyweight flyweight;

    /**
     * Behavior del proyectil. No final — permite transformaciones runtime.
     * Acceso de escritura solo via getView().changeBehavior() o setBehaviorRaw().
     */
    private BulletBehavior behavior;

    /**
     * Movement del proyectil. No final — permite transformaciones runtime.
     */
    private ProjectileMovement movement;

    /**
     * Flag para emitir OnProjectileDestroy exactamente una vez por ciclo de vida.
     * Se resetea en resetState() para el pool.
     */
    private boolean destroyEventFired = false;

    /**
     * Pool al que esta instancia pertenece, o null si no está gestionada por un pool.
     */
    private ProjectilePool ownerPool = null;

    /**
     * Bus de eventos para emitir eventos de ciclo de vida del proyectil.
     * Inyectado desde ProjectilePool.acquire() o BulletFactory.build().
     * Null = no se emiten eventos (proyectil sin suscriptores registrados).
     * package-private: acceso desde ProjectilePool y BulletFactory (mismo paquete).
     */
    GameEventBus eventBus = null;

    /**
     * Contexto de interacción con el mundo para el hook onExpire.
     * Inyectado por el pool (setProjectileContext) o NULL por defecto.
     * Package-private para acceso desde ProjectilePool (mismo paquete).
     */
    ProjectileContext projectileContext = ProjectileContext.NULL;

    // ── Metadata del Proyectil ────────────────────────────────────────────
    
    /**
     * Entidad que disparó este proyectil (Player, Enemy, Turret, etc.).
     * null = origen desconocido o proyectil del mundo.
     * 
     * Metadata intrínseca del proyectil, NO forma parte del ProjectileContext
     * (que representa servicios externos). Owner es información propia que
     * permite: faction queries, allegiance checks, scoring, damage attribution.
     */
    private Object owner = null;
    
    /**
     * Posición donde este proyectil fue spawneado originalmente.
     * Útil para: efectos visuales (trails desde origen), cálculos de distancia
     * recorrida, mecánicas basadas en trayectoria.
     * 
     * Metadata intrínseca del proyectil, NO forma parte del ProjectileContext.
     */
    private Vector2D spawnOrigin = null;

    // ── HRFC — Off-Screen Lifetime Tracking ───────────────────────────────

    /**
     * Rastreador de tiempo fuera de cámara.
     * Configurable por tipo de bullet via ProjectileBlueprint.
     * NULL = no hay tracking (never destroy off-screen).
     */
    private OffScreenTracker offScreenTracker = null;

    // ── Constructor principal ─────────────────────────────────────────────

    /**
     * Constructor con Flyweight — única ruta de construcción desde BulletFactory.
     *
     * FASE 4 — Optimización: constructor con primitivos de posición para reducir
     * allocations en el hot path de disparo.
     *
     * Los recursos inmutables (texture, profile, dimensiones) vienen ya
     * resueltos en el Flyweight — no se recalculan por instancia.
     *
     * ── Mini-HRFC — Declarative PhysicalState Ownership ───────────────────
     * PhysicalState se recibe como parámetro. Si es null o isEmpty(), el
     * proyectil NO recibe PhysicsComponent. La responsabilidad de declarar
     * el estado físico es del tipo concreto (BulletBehavior o Blueprint),
     * no de Bullet.
     *
     * @param posX          posición inicial de spawn (coordenada X)
     * @param posY          posición inicial de spawn (coordenada Y)
     * @param flyweight     recursos compartidos del tipo de proyectil
     * @param behavior      comportamiento de impacto y update
     * @param movement      estrategia de movimiento por frame
     * @param xSpeed        velocidad inicial en X (unidades/frame)
     * @param ySpeed        velocidad inicial en Y (unidades/frame)
     * @param lifeTime      ticks de vida máximos
     * @param damage        daño que aplica al impactar
     * @param physicalState estado físico declarado (null = sin física)
     * @param entityStore   EntityStore para registro DOD
     */
    public Bullet(
            double             posX,
            double             posY,
            BulletFlyweight    flyweight,
            BulletBehavior     behavior,
            ProjectileMovement movement,
            double             xSpeed,
            double             ySpeed,
            int                lifeTime,
            double             damage,
            PhysicalState      physicalState,
            EntityStore        entityStore
    ) {
        // NO tocar Transform — será eliminado en futura refactorización
        // Position SOLO en PrimitiveStorage

        this.flyweight  = flyweight;
        this.behavior   = behavior;
        this.movement   = (movement != null) ? movement : LinearMovement.INSTANCE;
        
        // ── HRFC — Projectile DOD Migration ───────────────────────────────
        // 1. Registrar en EntityStore con PROJECTILE_MASK
        this.entityStore = entityStore;
        this.entityId = entityStore.create(ComponentType.PROJECTILE_MASK);
        this.simulationHandle = entityStore.getHandle(entityId);
        
        // 2. Inicializar datos en PrimitiveStorage
        int idx = simulationHandle.index();
        var storage = entityStore.getStorage();
        
        storage.positionsX()[idx] = (float) posX;
        storage.positionsY()[idx] = (float) posY;
        storage.velocitiesX()[idx] = (float) xSpeed;
        storage.velocitiesY()[idx] = (float) ySpeed;
        storage.accelerationsX()[idx] = 0f;
        storage.accelerationsY()[idx] = 0f;
        storage.lifetimes()[idx] = (float) lifeTime;
        storage.damage()[idx] = (float) damage;
        storage.flags()[idx] = 0;
        storage.gravityScale()[idx] = 0f; // default: sin gravedad
        storage.mass()[idx] = 1f;
        storage.drag()[idx] = 0f;
        storage.rotations()[idx] = 0f; // default: sin rotación visual
        storage.ownerEntityIds()[idx] = 0L; // sin owner por defecto
        
        // 3. Crear adapters DOD apuntando al storage
        this.bulletLife = new BulletLifeDOD(storage, simulationHandle);
        this.physicsAdapter = new BulletPhysicsDOD(storage, simulationHandle);
        this.damageAdapter = new BulletDamageDOD(storage, simulationHandle);
        
        // EntityContext cacheado: envuelve BulletLife sin duplicar su estado.
        this.simulationContext = bulletLife::isAlive;

        // ── Render ────────────────────────────────────────────────────────
        // La texture viene del Flyweight — compartida con todas las Bullets
        // del mismo tipo. No se resuelve por instancia.
        if (flyweight.hasTexture()) {
            addComponent(new SpriteRendererComponent(flyweight.texture()));
        }

        // ── Collider (TRIGGER) — perfil del Flyweight ─────────────────────
        // effectiveProfile() retorna PLAYER_BULLET si collisionProfile es null.
        ColliderComponent collider = new ColliderComponent(
                flyweight.width(),
                flyweight.height(),
                flyweight.effectiveProfile());
        collider.setType(ColliderComponent.Type.TRIGGER);
        addComponent(collider);

        addComponent(new HitBoxComponent(Color.YELLOW));

        // ── Physics (cinemático) ──────────────────────────────────────────
        // BulletPhysics legacy — mantener para compatibilidad con Physics2DComponent
        // pero los datos reales están en PrimitiveStorage
        BulletPhysics physics = new BulletPhysics(xSpeed, ySpeed);
        physicsComponent = new Physics2DComponent(physics);
        addComponent(physicsComponent);

        // ── PhysicsComponent (estado físico opt-in) ───────────────────────
        // Mini-HRFC — Declarative PhysicalState Ownership
        if (physicalState != null && !physicalState.isEmpty()) {
            addComponent(new PhysicsComponent(physicalState));
        }

        // ── Lifecycle: onAttached ─────────────────────────────────────────
        this.behavior.onAttached(this);
    }

    // ── Update ────────────────────────────────────────────────────────────

    /**
     * Update del proyectil.
     *
     * ── HRFC — Unified DeltaTime Migration ───────────────────────────────
     *
     * CAMBIO: Ahora recibe deltaTime y lo propaga a BulletLife.advance()
     * y ProjectileMovement.tick() para integración temporal correcta.
     *
     * ── HRFC — Projectile DOD Migration ──────────────────────────────────
     *
     * IMPORTANTE: Este método YA NO ejecuta:
     * - lifetime decremento (LifetimeSystem lo hace)
     * - movement.tick() (ProjectileMovementSystem lo hace)
     * - moveByPhysics() (MovementSystem lo hace)
     *
     * SimulationPipeline coordina los sistemas DOD:
     *   1. ProjectileMovementSystem → configura acceleration
     *   2. AccelerationSystem → integra velocity
     *   3. MovementSystem → integra position
     *   4. LifetimeSystem → decrementa lifetime
     *   5. ProjectileBehaviorSystem → llama behavior.onUpdate()
     *
     * Este método se mantiene solo para compatibilidad de Components
     * legacy (super.update). La lógica real migró a sistemas.
     *
     * @param deltaTime tiempo del simulation step en segundos
     */
    @Override
    public void update(double deltaTime) {
        // Los sistemas DOD YA ejecutaron:
        // - LifetimeSystem decrementó lifetime
        // - ProjectileMovementSystem ejecutó movement.tick()
        // - AccelerationSystem integró acceleration → velocity
        // - MovementSystem integró velocity → position
        
        // Solo actualizar Components legacy si quedan
        super.update(deltaTime);
    }

    // ── Colisión ──────────────────────────────────────────────────────────

    @Override
    public void onCollisionWith(GameObjects other) {
        if (eventBus != null && eventBus.hasListeners(ProjectileEvents.OnProjectileHit.class)) {
            eventBus.post(new ProjectileEvents.OnProjectileHit(this, other));
        }

        behavior.onCollision(this, other);

        if (!bulletLife.isAlive()) {
            emitDestroy();
        }
    }

    // ── Destroyable ────────────────────────────────────────────────────────

    /**
     * Retorna true cuando el proyectil debe ser eliminado del mundo.
     *
     * Si esta instancia pertenece a un pool (ownerPool != null), la primera
     * vez que retorna true se auto-devuelve al pool invocando ownerPool.release(this).
     * onRelease() ya fue invocado antes en emitDestroy() — el cleanup del
     * behavior ocurre siempre antes del retorno al pool.
     *
     * Una vez que retorna true, no vuelve a retornar false en la misma instancia
     * (contrato de Destroyable). La bandera se resetea en resetState() cuando el
     * pool reutiliza la instancia para un nuevo ciclo.
     */
    @Override
    public boolean isPendingDestruction() {
        if (!bulletLife.isAlive()) {
            if (ownerPool != null) {
                ownerPool.release(this);
                ownerPool = null; // evitar double-release si se consulta más de una vez
            }
            return true;
        }
        return false;
    }

    // ── API pública ────────────────────────────────────────────────────────

    public BulletLifeDOD      getBulletLife() { return bulletLife; }
    public double             getBulletDamage()     { return damageAdapter.getDamage(); }
    public BulletBehavior     getBehavior()   { return behavior; }
    public ProjectileMovement getMovement()   { return movement; }
    public BulletFlyweight    getFlyweight()  { return flyweight; }

    /** Bus de eventos activo de este proyectil. Null si no fue inyectado. */
    public GameEventBus getEventBus() { return eventBus; }
    
    /**
     * Retorna el adapter DOD de física.
     * Los datos reales están en PrimitiveStorage.
     */
    public BulletPhysicsDOD getPhysics() {
        return physicsAdapter;
    }
    
    /**
     * Retorna X position directamente (sin allocation).
     * ÚNICA FUENTE DE VERDAD: PrimitiveStorage.
     */
    public double getPositionX() {
        return entityStore.getStorage().positionsX()[simulationHandle.index()];
    }
    
    /**
     * Retorna Y position directamente (sin allocation).
     * ÚNICA FUENTE DE VERDAD: PrimitiveStorage.
     */
    public double getPositionY() {
        return entityStore.getStorage().positionsY()[simulationHandle.index()];
    }
    
    /**
     * Retorna la posición actual desde PrimitiveStorage.
     * ALLOCATION: Crea nuevo Vector2D — evitar en hot paths.
     * 
     * Usar getPositionX() y getPositionY() cuando sea posible para evitar allocation.
     */
    public Vector2D getPosition() {
        return new Vector2D(getPositionX(), getPositionY());
    }
    
    /**
     * Escribe X/Y position directamente a PrimitiveStorage.
     * ÚNICA FUENTE DE VERDAD: PrimitiveStorage.
     * 
     * NO modifica Transform — Transform es legacy y será eliminado.
     */
    public void setPosition(double x, double y) {
        int idx = simulationHandle.index();
        var storage = entityStore.getStorage();
        storage.positionsX()[idx] = (float) x;
        storage.positionsY()[idx] = (float) y;
    }
    
    /**
     * Retorna rotation visual actual en radianes.
     * Usado para rendering de bullets giratorias (drills, spinning saws, etc.)
     */
    public float getRotation() {
        return entityStore.getStorage().rotations()[simulationHandle.index()];
    }
    
    /**
     * Establece rotation visual en radianes.
     * Usado por behaviors que necesitan rotar visualmente (DrillBullet, etc.)
     */
    public void setRotation(float radians) {
        entityStore.getStorage().rotations()[simulationHandle.index()] = radians;
    }
    
    /**
     * Retorna el EntityId del owner (quien disparó esta bullet).
     * 0 = sin owner.
     * 
     * Usado para:
     * - Faction checks (friendly fire)
     * - Scoring/attribution
     * - Achievement tracking
     */
    public long getOwnerEntityId() {
        return entityStore.getStorage().ownerEntityIds()[simulationHandle.index()];
    }
    
    /**
     * Establece el EntityId del owner.
     * Debe llamarse en spawn para tracking correcto.
     */
    public void setOwnerEntityId(long ownerEntityId) {
        entityStore.getStorage().ownerEntityIds()[simulationHandle.index()] = ownerEntityId;
    }
    
    // ── HRFC — Projectile DOD Migration ───────────────────────────────────
    
    /**
     * Retorna el handle de simulación para acceso directo a PrimitiveStorage.
     * Usado por ProjectileBehaviorSystem y otros sistemas.
     */
    public SimulationHandle getSimulationHandle() {
        return simulationHandle;
    }
    
    /**
     * Retorna el EntityId único de esta bullet.
     * Usado para consultas y mapeos estables.
     */
    public EntityId getEntityId() {
        return entityId;
    }

    /**
     * Contexto de interacción con el mundo.
     * Inyectado por el pool o ProjectileContext.NULL por defecto.
     *
     * ── HRFC — MetheorBullet Migration ───────────────────────────────────
     *
     * Añadido para permitir que behaviors con explosiones en onCollision()
     * puedan acceder a ProjectileContext.findEntitiesInRadius().
     *
     * Anteriormente, ProjectileContext solo estaba disponible en onExpire(),
     * lo que impedía implementar correctamente explosiones al impactar.
     */
    public ProjectileContext getProjectileContext() {
        return projectileContext;
    }

    /**
     * Inyecta el bus de eventos en este proyectil.
     * Llamado por BulletFactory.emitSpawn() y ProjectilePool.acquire().
     * package-accessible: visible en el paquete Definition y desde BulletFactory
     * via acceso explícito.
     */
    public void setEventBus(GameEventBus bus) { this.eventBus = bus; }

    // ── HRFC — Off-Screen Lifetime Tracking ───────────────────────────────

    /**
     * Configura el off-screen tracker para este bullet.
     * Debe llamarse con el tamaño del sprite configurado.
     *
     * @param maxOffScreenTime segundos máximos fuera de cámara (OffScreenTracker.NEVER_DESTROY para infinito)
     */
    public void setOffScreenTracking(double maxOffScreenTime) {
        if (maxOffScreenTime == OffScreenTracker.NEVER_DESTROY) {
            this.offScreenTracker = null;  // No tracking
            return;
        }
        
        this.offScreenTracker = new OffScreenTracker(maxOffScreenTime);
        
        // Configurar tamaño del sprite desde el flyweight
        if (flyweight != null) {
            offScreenTracker.setSpriteSize(flyweight.width(), flyweight.height());
        }
    }

    /**
     * Retorna el off-screen tracker activo, o null si no hay tracking configurado.
     */
    public OffScreenTracker getOffScreenTracker() {
        return offScreenTracker;
    }

    /**
     * Actualiza el off-screen tracking con la cámara actual.
     * Este método debería ser llamado por WorldManager después del update regular.
     *
     * @param camera cámara activa del juego
     * @param deltaTime tiempo transcurrido en segundos
     */
    public void updateOffScreenTracking(Game.Engine.Camera.GameCamera camera, double deltaTime) {
        if (offScreenTracker != null) {
            // Sin allocation — construir Vector2D solo cuando es necesario
            Vector2D position = new Vector2D(getPositionX(), getPositionY());
            offScreenTracker.update(position, camera, deltaTime);
            if (offScreenTracker.shouldDestroy()) {
                bulletLife.kill();  // Marcar para destrucción
            }
        }
    }

    // ── CEEM Support ──────────────────────────────────────────────────────

    /**
     * Retorna la rareza de este proyectil desde su BulletID.
     * 
     * ARQUITECTURA:
     *   La rareza se obtiene desde BulletRegistry usando el BulletID expuesto
     *   por el BulletBehavior concreto. Esto respeta la arquitectura existente
     *   donde la rareza está declarada en BulletRegistry por tipo.
     * 
     *   Mismo patrón usado por LootSpawnLayer con ItemRegistry.
     * 
     * @return ItemRarity del tipo, o COMMON si no tiene ID registrado
     */
    public ItemRarity getRarity() {
        BulletID bulletId = getBehavior().getBulletID();
        if (bulletId == null) {
            return ItemRarity.COMMON; // fallback para bullets custom sin ID
        }
        return BulletRegistry.getRarity(bulletId);
    }

    // ── Engine.Lifecycle — SimulationLifecycle ─────────────────────────────

    /**
     * Contexto de simulación del proyectil.
     *
     * Retorna el EntityContext cacheado que envuelve BulletLife.isAlive().
     * El proyectil tiene razón para seguir simulándose mientras tenga tiempo
     * de vida activo — su lifetime finito ES su contexto de simulación.
     *
     * No se usa LifetimeContext (del Engine) porque BulletLife es el estado
     * canónico de vida del proyectil, con semántica específica para piercing
     * y bounce (kill/revive/extend). Duplicarlo en un LifetimeContext paralelo
     * introduciría dos fuentes de verdad sobre la misma información.
     *
     * El campo simulationContext se inicializa una vez en el constructor —
     * sin allocations en el hot path de getSimulationContext().
     */
    @Override
    public EntityContext getSimulationContext() {
        return simulationContext;
    }

    /**
     * Retorna true mientras el proyectil tenga vida restante.
     *
     * Delegación directa a BulletLife.isAlive() — sin lógica adicional.
     * El valor es idéntico al que usa isPendingDestruction() internamente.
     */
    @Override
    public boolean shouldSimulate() {
        return bulletLife.isAlive();
    }

    // ── Engine.Spatial — radio de interacción ─────────────────────────────

    /**
     * Radio de interacción espacial de este proyectil.
     *
     * Delega a {@link BulletBehavior#getInteractionRadius(Bullet)} para que
     * cada tipo de proyectil pueda declarar su propio radio. El default en
     * BulletBehavior conserva el comportamiento original: la dimensión mayor
     * del collider dividida por 2.
     *
     * Uso típico desde BulletBehavior:
     *   ctx.findEntitiesInRadius(
     *       bullet.getTransform().getPosition(),
     *       bullet.getInteractionRadius()
     *   );
     *
     * @return radio de interacción en unidades del mundo (px)
     */
    public double getInteractionRadius() {
        return behavior.getInteractionRadius(this);
    }

    /**
     * Cambia el perfil de colisión de este proyectil.
     * Llamado por resetState() cuando el Flyweight cambia, o por ProjectileView.
     */
    public void setCollisionProfile(CollisionProfile profile) {
        ColliderComponent col = getComponent(ColliderComponent.class);
        if (col != null) col.setProfile(profile);
    }

    // ── API de transformación runtime (ProjectileTransformer) ─────────────

    public ProjectileTransformer.ProjectileView getView() {
        return new BulletProjectileView(this);
    }

    public void applyTransformer(ProjectileTransformer transformer) {
        transformer.apply(getView());
    }

    // ── Reset para pool (package-private) ────────────────────────────────

    /**
     * Resetea el estado mutable del proyectil para reutilización por el pool.
     *
     * ── GARANTÍA DE PARIDAD ───────────────────────────────────────────────
     *
     * Tras este método, la instancia tiene exactamente la misma configuración
     * que produciría BulletFactory.build() con el mismo blueprint.
     *
     * ── HRFC — Projectile DOD Migration ──────────────────────────────────
     *
     * IMPORTANTE: resetState() NO crea una nueva entidad DOD.
     * Reutiliza el EntityId/SimulationHandle existente y resetea los datos
     * en PrimitiveStorage in-place.
     *
     * Esto garantiza:
     * - EntityId permanece estable (identidad lógica)
     * - SimulationHandle permanece válido (misma ubicación física)
     * - No hay fugas de slots DOD
     * - No hay costo de create/destroy en el hot path
     *
     * Si el Flyweight cambió (diferente tipo de proyectil), se actualiza:
     *   - El sprite del SpriteRendererComponent
     *   - El perfil de colisión del ColliderComponent
     *
     * Los componentes NO se recrean — solo se actualizan sus campos mutables.
     * Esto evita el coste de construcción de Component mientras garantiza
     * que la instancia tiene la configuración correcta.
     *
     * ── Mini-HRFC — Declarative PhysicalState Ownership ───────────────────
     * PhysicalState se resetea completamente: se elimina el componente
     * existente y se agrega el nuevo estado declarado por el blueprint.
     * Esto garantiza que FireBullet → NormalBullet no conserva temperatura,
     * y que NormalBullet → FireBullet recibe la temperatura correcta.
     *
     * PRE-CONDICIÓN: behavior.onRelease(this) ya fue llamado antes de llegar
     * aquí (via emitDestroy() protegido por destroyEventFired).
     *
     * El behavior recibe onAttached(this) al final para el nuevo ciclo.
     *
     * package-private: solo accesible desde ProjectilePool (mismo paquete).
     */
    void resetState(double x, double y,
                    double xSpeed, double ySpeed,
                    int lifeTime, double damage,
                    BulletBehavior behavior,
                    ProjectileMovement movement,
                    BulletFlyweight newFlyweight,
                    PhysicalState newPhysicalState) {

        // Escribir position directamente a PrimitiveStorage
        setPosition(x, y);
        
        // ── HRFC — Projectile DOD Migration ───────────────────────────────
        // Resetear datos en PrimitiveStorage usando el handle existente
        int idx = simulationHandle.index();
        var storage = entityStore.getStorage();
        
        // Position (ya actualizado en Transform, sincronizar con storage)
        storage.positionsX()[idx] = (float) x;
        storage.positionsY()[idx] = (float) y;
        
        // Velocity
        storage.velocitiesX()[idx] = (float) xSpeed;
        storage.velocitiesY()[idx] = (float) ySpeed;
        
        // Acceleration (reset a cero — ProjectileMovement lo configurará)
        storage.accelerationsX()[idx] = 0f;
        storage.accelerationsY()[idx] = 0f;
        
        // Lifetime
        storage.lifetimes()[idx] = (float) lifeTime;
        
        // Damage
        storage.damage()[idx] = (float) damage;
        
        // Flags (limpiar todos)
        storage.flags()[idx] = 0;
        
        // Physics properties (reset a defaults)
        storage.gravityScale()[idx] = 0f;
        storage.mass()[idx] = 1f;
        storage.drag()[idx] = 0f;
        storage.rotations()[idx] = 0f;
        storage.ownerEntityIds()[idx] = 0L;
        
        // Metadata (opcional — puede ser útil para debugging)
        // storage.typeIds()[idx] = behavior.getBulletID().ordinal();
        // storage.behaviorIds()[idx] = ...;
        
        // ── Legacy fields ──────────────────────────────────────────────────
        // damage ya está en DOD storage — se escribió arriba
        this.destroyEventFired = false;
        // ownerPool y eventBus se limpian aquí; el pool los reinyecta justo después
        this.ownerPool = null;
        this.eventBus  = null;
        // owner y spawnOrigin se limpian aquí; el pool los reinyecta justo después
        this.owner = null;
        this.spawnOrigin = null;
        // Reset off-screen tracker if present
        if (this.offScreenTracker != null) {
            this.offScreenTracker.reset();
        }

        // ── HRFC — Reset Physical Properties (cinemático) ────────────────
        // Resetear masa, área efectiva y coeficiente de drag a valores por
        // defecto de BulletPhysics legacy (mantener por compatibilidad)
        BulletPhysics legacyPhysics = (BulletPhysics) physicsComponent.getPhysics();
        if (legacyPhysics != null) {
            legacyPhysics.setMass(1.0);
            legacyPhysics.setEffectiveArea(0.3);
            legacyPhysics.setDragCoefficient(0.0001);
        }
        
        // ── REGRESIÓN FIX — Limpiar estado de colisión residual ──────────────
        // Limpiar contactNormal en adapter DOD (mantiene estado local)
        physicsAdapter.clearLastContactNormal();
        
        // ── REGRESIÓN FIX — Limpiar estado físico acumulado ──────────────────
        physicsComponent.getPhysics().clearPooledState();

        // ── Mini-HRFC — Reset PhysicalState (declarativo) ─────────────────
        // Actualizar o remover el PhysicsComponent según el nuevo estado.
        PhysicsComponent existingPhysics = getComponent(PhysicsComponent.class);
        
        if (newPhysicalState == null || newPhysicalState.isEmpty()) {
            // El nuevo bullet no tiene física — mantener componente si existe
            // (no podemos remover componentes — limitación arquitectónica)
            if (existingPhysics != null) {
                // Component existe pero no debería. Por ahora, este escenario es raro.
            }
        } else {
            // El nuevo bullet tiene física declarada
            if (existingPhysics != null) {
                // Ya existe PhysicsComponent — reemplazar su contenido
                PhysicalState existingState = existingPhysics.getState();
                
                // Limpiar propiedades existentes que no estén en el nuevo estado
                for (var descriptor : existingState.registeredDescriptors()) {
                    if (!newPhysicalState.has(descriptor)) {
                        existingState.set(descriptor, 0.0);
                    }
                }
                
                // Copiar todas las propiedades del nuevo estado
                for (var descriptor : newPhysicalState.registeredDescriptors()) {
                    existingState.set(descriptor, newPhysicalState.get(descriptor));
                }
            } else {
                // No existe — agregarlo
                addComponent(new PhysicsComponent(newPhysicalState));
            }
        }

        // Actualizar Flyweight si cambió — sin recrear componentes
        if (this.flyweight != newFlyweight) {
            this.flyweight = newFlyweight;

            // Actualizar sprite
            SpriteRendererComponent renderer = getComponent(SpriteRendererComponent.class);
            if (renderer != null) {
                renderer.setSprite(newFlyweight.texture());
            }

            // Actualizar collisionProfile
            setCollisionProfile(newFlyweight.effectiveProfile());

            // Actualizar dimensiones del collider si cambiaron
            ColliderComponent col = getComponent(ColliderComponent.class);
            if (col != null) {
                col.setSize(newFlyweight.width(), newFlyweight.height());
            }
        }

        // Restaurar behavior y movement del nuevo blueprint
        this.behavior = behavior;
        this.movement = (movement != null) ? movement : LinearMovement.INSTANCE;

        // Si el movement tiene estado, resetearlo
        if (this.movement instanceof ResettableMovement rm) {
            rm.reset();
        }

        // Iniciar el ciclo de vida del behavior para este nuevo disparo
        this.behavior.onAttached(this);
    }

    /**
     * Modifica el daño del proyectil. Package-private para el pool y ProjectileView.
     */
    void resetDamage(double newDamage) {
        damageAdapter.setDamage(newDamage);
    }

    /**
     * Modifica el behavior con semántica de lifecycle definida.
     *
     * Secuencia:
     *   1. behavior.onDetached(this) sobre el behavior saliente.
     *   2. Asignación del nuevo behavior.
     *   3. newBehavior.onAttached(this) sobre el behavior entrante.
     */
    void changeBehaviorWithLifecycle(BulletBehavior newBehavior) {
        if (newBehavior == null) return;
        BulletBehavior old = this.behavior;
        this.behavior = newBehavior;
        old.onDetached(this);
        newBehavior.onAttached(this);
    }

    /**
     * Modifica el movement con semántica de reset definida.
     *
     * Si el nuevo movement implementa ResettableMovement, se llama reset().
     */
    void changeMovementWithReset(ProjectileMovement newMovement) {
        this.movement = (newMovement != null) ? newMovement : LinearMovement.INSTANCE;
        if (this.movement instanceof ResettableMovement rm) {
            rm.reset();
        }
    }

    /**
     * Inyecta el ProjectileContext para onExpire.
     * Package-private: llamado por ProjectilePool al adquirir.
     */
    void setProjectileContext(ProjectileContext ctx) {
        this.projectileContext = (ctx != null) ? ctx : ProjectileContext.NULL;
    }
    
    /**
     * Configura el owner de este proyectil.
     * Package-private: llamado por ProjectilePool/BulletFactory durante construcción.
     */
    void setOwner(Object owner) {
        this.owner = owner;
    }
    
    /**
     * Configura el origen de spawn de este proyectil.
     * FASE 4 — Optimización: firma con primitivos para reducir allocations.
     * Package-private: llamado por ProjectilePool/BulletFactory durante construcción.
     */
    void setSpawnOrigin(double originX, double originY) {
        if (this.spawnOrigin == null) {
            this.spawnOrigin = new Vector2D(originX, originY);
        } else {
            this.spawnOrigin.setX(originX);
            this.spawnOrigin.setY(originY);
        }
    }
    
    /**
     * Retorna el owner de este proyectil (puede ser null).
     * 
     * @return entidad que disparó el proyectil, o null si desconocido
     */
    public Object getOwner() {
        return owner;
    }
    
    /**
     * Retorna la posición de spawn original de este proyectil.
     * 
     * @return posición de origen, o null si no fue establecida
     */
    public Vector2D getSpawnOrigin() {
        return spawnOrigin;
    }

    /**
     * Registra el pool al que pertenece esta instancia.
     *
     * Cuando isPendingDestruction() detecte destrucción, llamará
     * ownerPool.release(this) para devolver la instancia al pool.
     *
     * null = sin pool — la instancia será recolectada por el GC normalmente.
     *
     * package-private: solo accesible desde ProjectilePool (mismo paquete).
     */
    void assignPool(ProjectilePool pool) {
        this.ownerPool = pool;
    }

    // ── Eventos de ciclo de vida ──────────────────────────────────────────

    private void emitExpireAndDestroy() {
        behavior.onExpire(this, projectileContext);

        if (eventBus != null && eventBus.hasListeners(ProjectileEvents.OnProjectileExpire.class)) {
            eventBus.post(new ProjectileEvents.OnProjectileExpire(this));
        }
        emitDestroy();
    }

    /**
     * Emite OnProjectileDestroy y llama onRelease exactamente una vez.
     *
     * Protegido por destroyEventFired para garantizar que onRelease() no se
     * invoca dos veces aunque emitDestroy() sea llamado desde múltiples paths
     * (expire + collision concurrente en el mismo frame).
     *
     * ── HRFC — Projectile DOD Migration ──────────────────────────────────
     *
     * IMPORTANTE: emitDestroy() marca para destrucción pero NO destruye
     * la entidad DOD inmediatamente. El EntityStore.compact() se encargará
     * de limpiar slots muertos al final del frame.
     *
     * Esto evita invalidar handles durante el processing de comportamientos.
     */
    void emitDestroy() {
        if (!destroyEventFired) {
            destroyEventFired = true;
            behavior.onRelease(this);
            if (eventBus != null && eventBus.hasListeners(ProjectileEvents.OnProjectileDestroy.class)) {
                eventBus.post(new ProjectileEvents.OnProjectileDestroy(this));
            }
            
            // Marcar entidad DOD para destrucción (compact() limpiará después)
            if (entityStore != null && entityId != null) {
                entityStore.destroy(entityId);
            }
        }
    }

    // ── Implementación de ProjectileView ─────────────────────────────────

    private static final class BulletProjectileView
            implements ProjectileTransformer.ProjectileView {

        private final Bullet bullet;

        BulletProjectileView(Bullet bullet) {
            this.bullet = bullet;
        }

        @Override public double getXSpeed() { return bullet.getPhysics().getXspeed(); }
        @Override public double getYSpeed() { return bullet.getPhysics().getYspeed(); }

        @Override
        public void redirect(double xSpeed, double ySpeed) {
            bullet.getPhysics().setXspeed(xSpeed);
            bullet.getPhysics().setYspeed(ySpeed);
        }

        @Override
        public CollisionProfile getCollisionProfile() {
            ColliderComponent col = bullet.getComponent(ColliderComponent.class);
            return col != null ? col.getProfile() : null;
        }

        @Override
        public void changeCollisionProfile(CollisionProfile profile) {
            bullet.setCollisionProfile(profile);
        }

        @Override public double getDamage()               { return bullet.getBulletDamage(); }
        @Override public void   changeDamage(double d)    { bullet.resetDamage(d); }
        @Override public BulletBehavior getBehavior()     { return bullet.getBehavior(); }
        @Override public void changeBehavior(BulletBehavior b) {
            bullet.changeBehaviorWithLifecycle(b);
        }
        @Override public ProjectileMovement getMovement() { return bullet.getMovement(); }
        @Override public void changeMovement(ProjectileMovement m) {
            bullet.changeMovementWithReset(m);
        }
        @Override public void kill()                      { bullet.getBulletLife().kill(); }
        @Override public void extendLifetime(int extra)   { bullet.getBulletLife().extend(extra); }
        @Override public Bullet bullet()                  { return bullet; }
    }
}
