package Game.Items.Types.Bullets;

import Game.Engine.Colisions.Filter.CollisionProfile;
import Game.Engine.GameEventBus;
import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Gameplay.Events.SpawnProjectileEvent;
import Game.Items.Types.Bullets.BulletComport.BulletBehavior;
import Game.Items.Types.Bullets.Definition.Bullet;
import Game.Items.Types.Bullets.Definition.BulletType;
import Game.Items.Types.Bullets.Definition.ProjectilePool;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Sistema de spawning de proyectiles mediante BulletType.
 *
 * ── ACLARACIÓN ARQUITECTÓNICA ─────────────────────────────────────────────
 *
 * ProjectileRegistry NO ES un registry de items como BulletRegistry o WeaponRegistry.
 *
 * RESPONSABILIDADES:
 *   ✓ Escuchar SpawnProjectileEvent en el bus del mundo
 *   ✓ Convertir BulletType → ProjectileBlueprint → Bullet
 *   ✓ Gestionar ProjectilePool interno para reutilización
 *   ✓ Resolver dirección origin→target automáticamente
 *
 * NO HACE:
 *   ✗ Registrar BulletDefinition (eso es BulletRegistry)
 *   ✗ Gestionar rareza o loot pools (eso es BulletRegistry)
 *   ✗ Crear BulletBehavior directamente (eso es BulletType)
 *
 * ── LIFECYCLE DE LISTENER ─────────────────────────────────────────────────
 *
 * installListener(bus, bulletSpawner) registra exactamente UN listener en el
 * bus proporcionado y conserva la Subscription para poder desregistrarlo limpiamente.
 *
 * Si se llama installListener() nuevamente (ej: reinicio del mundo), el
 * listener anterior se cancela antes de instalar el nuevo — nunca se acumulan.
 *
 * uninstallListener() cancela el listener explícitamente. Debe llamarse cuando
 * el World/Scene que owns este registry se destruye.
 *
 * shutdown() combina uninstallListener() + reset del estado interno.
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
 *     bus.post(new SpawnProjectileEvent(BulletType.NORMAL_BULLET, origin, target, enemy));
 *
 *   ProjectileRegistry escucha SpawnProjectileEvent:
 *     → extrae BulletType del evento
 *     → busca la factory por BulletType
 *     → resuelve dirección origin→target
 *     → factory produce ProjectileBlueprint
 *     → pool.acquire(blueprint, origin, direction)  ← vía pool si es posible
 *     → bulletSpawner.accept(bullet) para añadirlo al mundo
 *
 * ── REGISTRO ──────────────────────────────────────────────────────────────
 *
 * Tres variantes de registro:
 *
 *   registerSimple(type, behaviorSupplier)
 *     Para proyectiles que solo necesitan behavior, con dirección origin→target.
 *     Usa CollisionProfile.ENEMY_BULLET por defecto (proyectiles de enemigos).
 *
 *   registerSimple(type, behaviorSupplier, baseSpeed)
 *     Igual, con velocidad base explícita.
 *
 *   register(type, factory)
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

    private final Map<BulletType, BlueprintFactory> factories = new LinkedHashMap<>();

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

    // ── HRFC — Consolidación y Limpieza de Legacy ────────────────────────
    // reset() fue eliminado. Usar shutdown() en su lugar para lifecycle correcto.

    /**
     * Cierra el registry limpiamente: cancela el listener del bus y destruye el singleton.
     *
     * Llamar cuando el World/Scene que owns este registry se destruye.
     * Esto garantiza que el bus no retiene referencias al registry
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
     * @param type    BulletType que identifica el tipo de proyectil
     * @param factory factory que construye el Blueprint
     */
    public ProjectileRegistry register(BulletType type, BlueprintFactory factory) {
        if (type == null) {
            throw new IllegalArgumentException("BulletType no puede ser null");
        }
        if (factories.containsKey(type)) {
            throw new IllegalStateException("BulletType duplicado: " + type);
        }
        factories.put(type, factory);
        return this;
    }

    /**
     * Registra un tipo de proyectil simple usando solo un BulletBehavior.
     *
     * La velocidad y datos vienen de getDefaultData() del behavior.
     * La dirección se calcula de origin→target.
     * CollisionProfile: ENEMY_BULLET (proyectiles de enemigos por defecto).
     *
     * @param type            BulletType que identifica el tipo de proyectil
     * @param behaviorFactory factory que produce el BulletBehavior
     */
    public ProjectileRegistry registerSimple(BulletType type,
                                             Supplier<BulletBehavior> behaviorFactory) {
        return register(type, (origin, target) -> {
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
     * @param type            BulletType que identifica el tipo de proyectil
     * @param behaviorFactory factory del BulletBehavior
     * @param baseSpeed       velocidad del proyectil en unidades/frame
     */
    public ProjectileRegistry registerSimple(BulletType type,
                                             Supplier<BulletBehavior> behaviorFactory,
                                             double baseSpeed) {
        return register(type, (origin, target) -> {
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
     * Crea un proyectil dado un BulletType, origen/target y propietario del disparo.
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
     * @param type   BulletType que identifica el tipo de proyectil
     * @param origin posición de spawn
     * @param target posición objetivo (puede ser null)
     * @param owner  el objeto que disparó el proyectil (puede ser null)
     * @return Bullet listo para añadir al mundo, o null si el tipo no está registrado
     */
    public Bullet resolve(BulletType type, Vector2D origin, Vector2D target, Object owner) {
        if (type == null) {
            System.err.println("[ProjectileRegistry] BulletType no puede ser null");
            return null;
        }
        
        BlueprintFactory factory = factories.get(type);
        if (factory == null) {
            System.err.println("[ProjectileRegistry] BulletType no registrado: " + type);
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
     * @param type   BulletType que identifica el tipo de proyectil
     * @param origin posición de spawn
     * @param target posición objetivo (puede ser null)
     * @return Bullet listo para añadir al mundo, o null si el tipo no está registrado
     */
    public Bullet resolve(BulletType type, Vector2D origin, Vector2D target) {
        return resolve(type, origin, target, null);
    }

    /** @return true si el BulletType está registrado */
    public boolean has(BulletType type) {
        return type != null && factories.containsKey(type);
    }

    /** Vista de solo lectura de los tipos registrados. */
    public Map<BulletType, BlueprintFactory> getAll() {
        return Collections.unmodifiableMap(factories);
    }

    // ── Pool context ──────────────────────────────────────────────────────

    /** Acceso de solo lectura al pool para estadísticas/diagnóstico. */
    public ProjectilePool getPool() {
        return pool;
    }

    // ── Listener de SpawnProjectileEvent ─────────────────────────────────

    /**
     * Instala el listener de SpawnProjectileEvent en el bus indicado.
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
     * @param bus          bus de eventos donde registrar el listener
     * @param bulletSpawner callback que añade el Bullet al mundo (world::add,
     *                      worldManager::addDynamic, etc.)
     */
    public void installListener(GameEventBus bus, Consumer<Bullet> bulletSpawner) {
        if (bus == null) throw new IllegalArgumentException("ProjectileRegistry.installListener: bus is required");
        // Cancelar listener anterior si existe — evita duplicados
        if (listenerSubscription != null && listenerSubscription.isActive()) {
            listenerSubscription.cancel();
        }

        listenerSubscription = bus.subscribe(
            SpawnProjectileEvent.class,
            event -> {
                BulletType type = event.projectileTypeId();
                Vector2D origin = event.origin();
                
                if (type == null || origin == null) return;

                // Propagar sourceEntity como owner del proyectil
                Bullet bullet = resolve(
                    type,
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
