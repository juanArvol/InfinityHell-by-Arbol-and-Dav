package Game.Items.Types.Bullets.Definition;

import Game.Engine.Colisions.Filter.CollisionProfile;
import Game.Engine.Entity.Components.Collisions.ColliderComponent;
import Game.Engine.Entity.Components.Physics2DComponent;
import Game.Engine.Entity.Components.Visuals.HitBoxComponent;
import Game.Engine.Entity.Components.Visuals.SpriteRendererComponent;
import Game.Engine.Events.GameEventBus;
import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Engine.GameObjects;
import Game.Engine.Lifecycle.EntityContext;
import Game.Engine.Lifecycle.SimulationLifecycle;
import Game.Engine.Physics.KineticPhysics.PhysicsStepper;
import Game.Items.Types.Bullets.BulletComport.BulletBehavior;
import Game.Items.Types.Bullets.BulletComport.BulletLife;
import Game.Items.Types.Bullets.BulletComport.BulletPhysics;
import Game.Items.Types.Bullets.Flyweight.BulletFlyweight;
import Game.Items.Types.Bullets.Movement.LinearMovement;
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

    private final BulletLife         bulletLife;
    private final Physics2DComponent physicsComponent;
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
     * Daño del proyectil. No final para el pool.
     */
    private double damage;

    /**
     * Flag para emitir OnProjectileDestroy exactamente una vez por ciclo de vida.
     * Se resetea en resetState() para el pool.
     */
    private boolean destroyEventFired = false;

    /**
     * Pool al que esta instancia pertenece, o null si no está gestionada por un pool.
     *
     * Inyectado por ProjectilePool.acquire() vía assignPool() después de construir.
     * Cuando la Bullet es destruida (isPendingDestruction() == true) y ownerPool != null,
     * se auto-devuelve al pool invocando ownerPool.release(this).
     *
     * Este campo es la única diferencia entre una Bullet "simple" y una "pooled":
     * no hay subclase, no hay constructor alternativo, no hay lógica duplicada.
     * La Factory construye siempre una Bullet plana. El Pool inyecta la referencia.
     *
     * Se limpia a null en resetState() y se reinyecta en cada acquire(), garantizando
     * que al resetear el pool también está actualizado si cambia la instancia de pool.
     */
    private ProjectilePool ownerPool = null;

    /**
     * Contexto de interacción con el mundo para el hook onExpire.
     * Inyectado por el pool (setProjectileContext) o NULL por defecto.
     * Package-private para acceso desde ProjectilePool (mismo paquete).
     */
    ProjectileContext projectileContext = ProjectileContext.NULL;

    // ── Constructor principal ─────────────────────────────────────────────

    /**
     * Constructor con Flyweight — única ruta de construcción desde BulletFactory.
     *
     * Los recursos inmutables (texture, profile, dimensiones) vienen ya
     * resueltos en el Flyweight — no se recalculan por instancia.
     *
     * @param position   posición inicial de spawn
     * @param flyweight  recursos compartidos del tipo de proyectil
     * @param behavior   comportamiento de impacto y update
     * @param movement   estrategia de movimiento por frame
     * @param xSpeed     velocidad inicial en X (unidades/frame)
     * @param ySpeed     velocidad inicial en Y (unidades/frame)
     * @param lifeTime   ticks de vida máximos
     * @param damage     daño que aplica al impactar
     */
    public Bullet(
            Vector2D           position,
            BulletFlyweight    flyweight,
            BulletBehavior     behavior,
            ProjectileMovement movement,
            double             xSpeed,
            double             ySpeed,
            int                lifeTime,
            double             damage
    ) {
        getTransform().setPosition(position);

        this.flyweight  = flyweight;
        this.behavior   = behavior;
        this.movement   = (movement != null) ? movement : LinearMovement.INSTANCE;
        this.damage     = damage;
        this.bulletLife = new BulletLife(lifeTime);
        // EntityContext cacheado: envuelve BulletLife sin duplicar su estado.
        // bulletLife::isAlive expresa "este proyectil tiene razón para existir
        // mientras tenga tiempo de vida activo" — que es el contexto de simulación
        // correcto para un proyectil (su contexto ES su lifetime finito).
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

        // ── Physics ───────────────────────────────────────────────────────
        BulletPhysics physics = new BulletPhysics(xSpeed, ySpeed);
        physicsComponent = new Physics2DComponent(physics);
        addComponent(physicsComponent);

        // ── Lifecycle: onAttached ─────────────────────────────────────────
        this.behavior.onAttached(this);
    }

    // ── Update ────────────────────────────────────────────────────────────

    @Override
    public void update() {
        if (!bulletLife.advance()) {
            emitExpireAndDestroy();
            return;
        }

        movement.tick(this);       // actualiza velocity (ej: GravityMovement, Homing)
        behavior.onUpdate(this);   // lógica de frame del behavior

        // moveByPhysics() ya NO se llama aquí.
        // CollisionsSystem FASE 1B integra la posición del proyectil con
        // SweptAABB 2D completo (CCD), garantizando detección correcta de
        // colisiones y normal del impacto disponible en onCollision().
        // Llamarlo aquí además de en CollisionsSystem produciría doble movimiento.

        super.update();            // actualiza Components registrados
    }

    // ── Colisión ──────────────────────────────────────────────────────────

    @Override
    public void onCollisionWith(GameObjects other) {
        if (GameEventBus.GLOBAL.hasListeners(ProjectileEvents.OnProjectileHit.class)) {
            GameEventBus.GLOBAL.post(new ProjectileEvents.OnProjectileHit(this, other));
        }

        behavior.onCollision(this, other);

        if (!bulletLife.isAlive()) {
            emitDestroy();
        }
    }

    // ── Movimiento ────────────────────────────────────────────────────────

    public void moveByPhysics() {
        var vel = getPhysics().getVelocity();
        PhysicsStepper.moveWith(this, vel.getX(), vel.getY());
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

    public BulletLife         getBulletLife() { return bulletLife; }
    public double             getDamage()     { return damage; }
    public BulletBehavior     getBehavior()   { return behavior; }
    public ProjectileMovement getMovement()   { return movement; }
    public BulletFlyweight    getFlyweight()  { return flyweight; }

    public BulletPhysics getPhysics() {
        return (BulletPhysics) physicsComponent.getPhysics();
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
     * Si el Flyweight cambió (diferente tipo de proyectil), se actualiza:
     *   - El sprite del SpriteRendererComponent
     *   - El perfil de colisión del ColliderComponent
     *
     * Los componentes NO se recrean — solo se actualizan sus campos mutables.
     * Esto evita el coste de construcción de Component mientras garantiza
     * que la instancia tiene la configuración correcta.
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
                    BulletFlyweight newFlyweight) {

        getTransform().setPosition(new Vector2D(x, y));
        getPhysics().setXspeed(xSpeed);
        getPhysics().setYspeed(ySpeed);
        bulletLife.resetTo(lifeTime);
        this.damage = damage;
        this.destroyEventFired = false;
        // ownerPool se limpia aquí; el pool lo reinyecta justo después via assignPool()
        this.ownerPool = null;

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
        this.damage = newDamage;
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

        if (GameEventBus.GLOBAL.hasListeners(ProjectileEvents.OnProjectileExpire.class)) {
            GameEventBus.GLOBAL.post(new ProjectileEvents.OnProjectileExpire(this));
        }
        emitDestroy();
    }

    /**
     * Emite OnProjectileDestroy y llama onRelease exactamente una vez.
     *
     * Protegido por destroyEventFired para garantizar que onRelease() no se
     * invoca dos veces aunque emitDestroy() sea llamado desde múltiples paths
     * (expire + collision concurrente en el mismo frame).
     */
    void emitDestroy() {
        if (!destroyEventFired) {
            destroyEventFired = true;
            behavior.onRelease(this);
            if (GameEventBus.GLOBAL.hasListeners(ProjectileEvents.OnProjectileDestroy.class)) {
                GameEventBus.GLOBAL.post(new ProjectileEvents.OnProjectileDestroy(this));
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

        @Override public double getDamage()               { return bullet.getDamage(); }
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
