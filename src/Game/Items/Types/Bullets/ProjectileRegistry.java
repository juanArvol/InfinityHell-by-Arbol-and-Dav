package Game.Items.Types.Bullets;

import Game.Engine.Colisions.Filter.CollisionProfile;
import Game.Engine.Events.GameEventBus;
import Game.Engine.Events.SpawnProjectileEvent;
import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Items.Types.Bullets.BulletComport.BulletBehavior;
import Game.Items.Types.Bullets.Definition.Bullet;
import Game.Items.Types.Bullets.Definition.ProjectilePool;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Registro central de tipos de proyectil identificados por ID string.
 *
 * ── HRFC — Projectile Construction & Transformation Pipeline ─────────────
 *
 * ProjectileRegistry conecta un string ID ("sans.bone", "fireball") con una
 * factory concreta y escucha SpawnProjectileEvent para que cualquier emisor
 * (Enemy, Boss, Turret, Trap) pueda spawnear proyectiles sin conocer
 * BulletFactory ni BulletBehavior.
 *
 * ── LIFECYCLE DE LISTENER ─────────────────────────────────────────────────
 *
 * installListener() registra exactamente UN listener en GameEventBus.GLOBAL y
 * conserva la Subscription para poder desregistrarlo limpiamente.
 *
 * Si se llama installListener() nuevamente (ej: reinicio del mundo), el
 * listener anterior se cancela antes de instalar el nuevo — nunca se acumulan.
 *
 * uninstallListener() cancela el listener explícitamente. Debe llamarse cuando
 * el World/Scene que owns este registry se destruye.
 *
 * shutdown() combina uninstallListener() + reset del estado interno. Es el
 * punto correcto de limpieza cuando el registry tiene lifecycle de World.
 *
 * ── POOL INTEGRADO ────────────────────────────────────────────────────────
 *
 * ProjectileRegistry gestiona un ProjectilePool interno. resolve() usa el pool
 * automáticamente para tipos de proyectil que lo admiten (behavior stateless
 * o movement ResettableMovement).
 *
 * El pool recibe un ProjectileContext inyectado desde el mundo (via setContext)
 * para que behaviors puedan spawnear proyectiles secundarios en onExpire.
 *
 * ── PIPELINE ──────────────────────────────────────────────────────────────
 *
 *   Emisor (Enemy, Boss):
 *     GameEventBus.GLOBAL.post(new SpawnProjectileEvent(
 *         "sans.bone", origin, target, enemy));
 *
 *   ProjectileRegistry escucha SpawnProjectileEvent:
 *     → busca la factory por "sans.bone"
 *     → resuelve dirección origin→target
 *     → factory produce ProjectileBlueprint
 *     → pool.acquire(blueprint, origin, direction)  ← vía pool si es posible
 *     → bulletSpawner.accept(bullet) para añadirlo al mundo
 *
 * ── REGISTRO ──────────────────────────────────────────────────────────────
 *
 * Tres variantes de registro:
 *
 *   registerSimple(id, behaviorSupplier)
 *     Para proyectiles que solo necesitan behavior, con dirección origin→target.
 *     Usa CollisionProfile.ENEMY_BULLET por defecto (proyectiles de enemigos).
 *
 *   registerSimple(id, behaviorSupplier, baseSpeed)
 *     Igual, con velocidad base explícita.
 *
 *   register(id, factory)
 *     Para proyectiles con lógica compleja: factory recibe (origin, target)
 *     y retorna el Blueprint final. Permite HomingMovement, modifiers custom, etc.
 */
public final class ProjectileRegistry {

    private static ProjectileRegistry instance;

    /**
     * Factory tipada: recibe (origin, target) y retorna un ProjectileBlueprint.
     * target puede ser null si el emisor no tiene objetivo directo.
     */
    @FunctionalInterface
    public interface BlueprintFactory {
        ProjectileBlueprint create(Vector2D origin, Vector2D target);
    }

    private final Map<String, BlueprintFactory> factories = new LinkedHashMap<>();

    /**
     * Pool de proyectiles interno.
     * resolve() lo usa para reutilizar instancias cuando el blueprint lo permite.
     */
    private final ProjectilePool pool = new ProjectilePool();

    /**
     * Subscription activa del listener de SpawnProjectileEvent.
     * null = no hay listener instalado.
     * Conservada para permitir cancelación limpia en uninstallListener().
     */
    private GameEventBus.Subscription listenerSubscription = null;

    private ProjectileRegistry() {}

    // ── Ciclo de vida ─────────────────────────────────────────────────────

    public static ProjectileRegistry getInstance() {
        if (instance == null) instance = new ProjectileRegistry();
        return instance;
    }

    /**
     * Resetea el singleton.
     *
     * IMPORTANTE: llamar uninstallListener() o shutdown() ANTES de reset()
     * para cancelar el listener en GameEventBus.GLOBAL. Si reset() se llama
     * directamente sin cancelar el listener, el listener anterior queda
     * huérfano en GLOBAL reteniendo referencias al registry destruido.
     *
     * Preferir shutdown() que hace ambas cosas en orden correcto.
     *
     * @deprecated Usar {@link #shutdown()} para limpieza completa con lifecycle correcto.
     */
    @Deprecated
    public static void reset() {
        if (instance != null) {
            instance.uninstallListener();
        }
        instance = null;
    }

    /**
     * Cierra el registry limpiamente: cancela el listener del bus y destruye el singleton.
     *
     * Llamar cuando el World/Scene que owns este registry se destruye.
     * Esto garantiza que GameEventBus.GLOBAL no retiene referencias al registry
     * ni a los bulletSpawners capturados en el listener.
     */
    public static void shutdown() {
        if (instance != null) {
            instance.uninstallListener();
            instance.pool.clear();
        }
        instance = null;
    }

    // ── Registro ──────────────────────────────────────────────────────────

    /**
     * Registra un tipo de proyectil con su BlueprintFactory completa.
     *
     * La factory recibe (origin, target) donde target puede ser null.
     * Retorna un ProjectileBlueprint listo para pasar a ProjectilePool.acquire()
     * o BulletFactory.build().
     *
     * @param id      identificador único del tipo ("sans.bone", "fireball", etc.)
     * @param factory factory que construye el Blueprint
     */
    public ProjectileRegistry register(String id, BlueprintFactory factory) {
        if (factories.containsKey(id)) {
            throw new IllegalStateException("ProjectileType duplicado: '" + id + "'");
        }
        factories.put(id, factory);
        return this;
    }

    /**
     * Registra un tipo de proyectil simple usando solo un BulletBehavior.
     *
     * La velocidad y datos vienen de getDefaultData() del behavior.
     * La dirección se calcula de origin→target.
     * CollisionProfile: ENEMY_BULLET (proyectiles de enemigos por defecto).
     *
     * @param id              identificador único del tipo
     * @param behaviorFactory factory que produce el BulletBehavior
     */
    public ProjectileRegistry registerSimple(String id,
                                             Supplier<BulletBehavior> behaviorFactory) {
        return register(id, (origin, target) -> {
            BulletBehavior behavior = behaviorFactory.get();
            double speed  = behavior.getDefaultData().speedFactor() * 8.0;
            double damage = behavior.getDefaultData().damage();

            return ProjectileBlueprint.from(
                    behavior, speed, damage,
                    CollisionProfile.ENEMY_BULLET);
        });
    }

    /**
     * Registra un tipo de proyectil con velocidad base explícita.
     *
     * @param id              identificador único del tipo
     * @param behaviorFactory factory del BulletBehavior
     * @param baseSpeed       velocidad del proyectil en unidades/frame
     */
    public ProjectileRegistry registerSimple(String id,
                                             Supplier<BulletBehavior> behaviorFactory,
                                             double baseSpeed) {
        return register(id, (origin, target) -> {
            BulletBehavior behavior = behaviorFactory.get();
            double speed  = baseSpeed * behavior.getDefaultData().speedFactor();
            double damage = behavior.getDefaultData().damage();

            return ProjectileBlueprint.from(
                    behavior, speed, damage,
                    CollisionProfile.ENEMY_BULLET);
        });
    }

    // ── Resolución ────────────────────────────────────────────────────────

    /**
     * Crea un proyectil dado un ID, origen/target y propietario del disparo.
     *
     * ── RUTA ÚNICA DE CREACIÓN ────────────────────────────────────────────
     *
     * Usa el pool interno (pool.acquire) en lugar de BulletFactory.build()
     * directamente. El pool decide si reutiliza una instancia existente o
     * crea una nueva, según las propiedades del blueprint (isStateless,
     * ResettableMovement). El resultado es indistinguible para el llamador.
     *
     * Esto garantiza una sola ruta de creación a través del registry — no
     * existen dos caminos paralelos (pool vs. factory) para el mismo tipo.
     *
     * El owner se propaga al evento OnProjectileSpawn para tracking externo.
     *
     * @param id     tipo de proyectil registrado
     * @param origin posición de spawn
     * @param target posición objetivo (puede ser null)
     * @param owner  el objeto que disparó el proyectil (puede ser null)
     * @return Bullet listo para añadir al mundo, o null si el ID no está registrado
     */
    public Bullet resolve(String id, Vector2D origin, Vector2D target, Object owner) {
        BlueprintFactory factory = factories.get(id);
        if (factory == null) {
            System.err.println("[ProjectileRegistry] Tipo desconocido: '" + id + "'");
            return null;
        }

        ProjectileBlueprint blueprint = factory.create(origin, target);
        if (blueprint == null) return null;

        Vector2D direction = resolveDirection(origin, target);

        // Usar pool interno — ruta única de creación para proyectiles del registry.
        // El pool emite OnProjectileSpawn con el owner correcto.
        return pool.acquire(blueprint, origin, direction, owner);
    }

    /**
     * Crea un proyectil sin propietario conocido.
     *
     * @param id     tipo de proyectil registrado
     * @param origin posición de spawn
     * @param target posición objetivo (puede ser null)
     * @return Bullet listo para añadir al mundo, o null si el ID no está registrado
     */
    public Bullet resolve(String id, Vector2D origin, Vector2D target) {
        return resolve(id, origin, target, null);
    }

    /** @return true si el ID está registrado */
    public boolean has(String id) {
        return factories.containsKey(id);
    }

    /** Vista de solo lectura de los IDs registrados. */
    public Map<String, BlueprintFactory> getAll() {
        return Collections.unmodifiableMap(factories);
    }

    // ── Pool context ──────────────────────────────────────────────────────

    /**
     * Inyecta el ProjectileContext en el pool interno.
     *
     * Llamar desde GameWorldBootstrap después de crear el mundo:
     *   registry.setProjectileContext(new WorldProjectileContext(worldManager));
     *
     * @param context contexto de interacción con el mundo
     */
    public void setProjectileContext(Game.Items.Types.Bullets.Definition.ProjectileContext context) {
        pool.setContext(context);
    }

    /** Acceso de solo lectura al pool para estadísticas/diagnóstico. */
    public ProjectilePool getPool() {
        return pool;
    }

    // ── Listener de SpawnProjectileEvent ─────────────────────────────────

    /**
     * Instala el listener de SpawnProjectileEvent en GameEventBus.GLOBAL.
     *
     * ── LIFECYCLE EXPLÍCITO ───────────────────────────────────────────────
     *
     * Si ya hay un listener instalado, se cancela ANTES de instalar el nuevo.
     * Esto garantiza que no se acumulan listeners duplicados si installListener()
     * se llama múltiples veces (ej: reinicio del mundo, reconstrucción del bootstrap).
     *
     * La Subscription queda conservada en this.listenerSubscription.
     * Para cancelar el listener, llamar uninstallListener() o shutdown().
     *
     * @param bulletSpawner callback que añade el Bullet al mundo (world::add,
     *                      worldManager::addDynamic, etc.)
     */
    public void installListener(Consumer<Bullet> bulletSpawner) {
        // Cancelar listener anterior si existe — evita duplicados
        if (listenerSubscription != null && listenerSubscription.isActive()) {
            listenerSubscription.cancel();
        }

        listenerSubscription = GameEventBus.GLOBAL.subscribe(
            SpawnProjectileEvent.class,
            event -> {
                Vector2D origin = event.origin();
                if (origin == null) return;

                // Propagar sourceEntity como owner del proyectil
                Bullet bullet = resolve(
                    event.projectileTypeId(),
                    origin,
                    event.target(),
                    event.sourceEntity()
                );
                if (bullet != null) {
                    bulletSpawner.accept(bullet);
                }
            }
        );
    }

    /**
     * Cancela el listener de SpawnProjectileEvent si está instalado.
     *
     * Llamar cuando el World/Scene que gestiona este registry se destruye,
     * para liberar la referencia al bulletSpawner y al registry en GLOBAL.
     *
     * Idempotente — puede llamarse aunque no haya listener instalado.
     */
    public void uninstallListener() {
        if (listenerSubscription != null) {
            listenerSubscription.cancel();
            listenerSubscription = null;
        }
    }

    /**
     * @return true si hay un listener activo instalado en el bus.
     */
    public boolean isListenerInstalled() {
        return listenerSubscription != null && listenerSubscription.isActive();
    }

    // ── Utilidad interna ──────────────────────────────────────────────────

    /**
     * Calcula la dirección normalizada de origin hacia target.
     * Si target es null o coincide con origin, usa (1, 0) como default.
     */
    private static Vector2D resolveDirection(Vector2D origin, Vector2D target) {
        if (target == null) return new Vector2D(1, 0);

        double dx  = target.getX() - origin.getX();
        double dy  = target.getY() - origin.getY();
        double len = Math.hypot(dx, dy);
        return (len > 1e-6)
                ? new Vector2D(dx / len, dy / len)
                : new Vector2D(1, 0);
    }
}
