package Game.Items.Types.Bullets;

import Game.Engine.Events.GameEventBus;
import Game.Engine.Events.SpawnProjectileEvent;
import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Items.Types.Bullets.BulletComport.BulletBehavior;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Registro central de tipos de proyectil identificados por ID string.
 *
 * ── HRFC — Weapon & Projectile System ────────────────────────────────────
 *
 * Resuelve el gap crítico: SpawnProjectileEvent existía con un campo
 * {@code projectileTypeId} ("sans.bone", "fireball", "arrow"), pero no había
 * ningún sistema que lo escuchara ni que supiera qué proyectil instanciar.
 *
 * ProjectileRegistry conecta el string ID con una factory concreta y registra
 * el listener en GameEventBus para que cualquier emisor (Enemy, Boss, Turret,
 * Trap) pueda spawnear proyectiles sin conocer BulletFactory ni BulletBehavior.
 *
 * ── ARQUITECTURA ──────────────────────────────────────────────────────────
 *
 *   Emisor (Enemy, Boss):
 *     GameEventBus.GLOBAL.post(new SpawnProjectileEvent(
 *         "sans.bone", origin, target, enemy));
 *
 *   ProjectileRegistry escucha SpawnProjectileEvent:
 *     → busca la factory por "sans.bone"
 *     → calcula dirección desde origin→target
 *     → crea el Bullet via BulletFactory
 *     → llama bulletSpawner.accept(bullet) para añadirlo al mundo
 *
 * ── REGISTRO ──────────────────────────────────────────────────────────────
 *
 * Dos variantes de factory:
 *
 *   registerSimple(id, behaviorSupplier)
 *     → para proyectiles que solo necesitan behavior (datos del getDefaultData())
 *
 *   register(id, factory)
 *     → para proyectiles con lógica compleja:
 *         factory recibe (origin, target) y retorna el Bullet final
 *         (puede usar HomingMovement, velocidad custom, etc.)
 *
 * ── CÓMO AÑADIR UN TIPO DE PROYECTIL DE ENEMIGO ──────────────────────────
 *
 *   // En GameWorldBootstrap o en la inicialización de la fase del jefe:
 *   ProjectileRegistry.getInstance().registerSimple(
 *       "sans.bone",
 *       SansBoneBehavior::new
 *   );
 *
 *   // Para proyectiles más complejos con movimiento homing:
 *   ProjectileRegistry.getInstance().register(
 *       "sans.targeted_bone",
 *       (origin, target) -> {
 *           HomingMovement homing = new HomingMovement(() -> playerRef, 90, 8.0);
 *           SansBoneBehavior behavior = new SansBoneBehavior();
 *           Vector2D dir = target.subtract(origin).normalize();
 *           return new Bullet(origin, null, behavior, homing, 4.0, 0.0, 120, 15, 12, 12);
 *       }
 *   );
 */
public final class ProjectileRegistry {

    private static ProjectileRegistry instance;

    /**
     * Factory tipada: recibe (origin, target) y retorna un Bullet listo para spawnar.
     * target puede ser null si el emisor no tiene objetivo directo.
     */
    @FunctionalInterface
    public interface ProjectileFactory {
        Bullet create(Vector2D origin, Vector2D target);
    }

    private final Map<String, ProjectileFactory> factories = new LinkedHashMap<>();

    private ProjectileRegistry() {}

    // ── Ciclo de vida ─────────────────────────────────────────────────────

    public static ProjectileRegistry getInstance() {
        if (instance == null) instance = new ProjectileRegistry();
        return instance;
    }

    /** Limpia el registro (útil entre sesiones de juego o tests). */
    public static void reset() {
        instance = null;
    }

    // ── Registro ──────────────────────────────────────────────────────────

    /**
     * Registra un tipo de proyectil con su factory completa.
     *
     * La factory recibe (origin, target) donde target puede ser null.
     * Es responsable de construir el Bullet completo.
     *
     * @param id      identificador único del tipo ("sans.bone", "fireball", etc.)
     * @param factory factory que construye el Bullet
     */
    public ProjectileRegistry register(String id, ProjectileFactory factory) {
        if (factories.containsKey(id)) {
            throw new IllegalStateException("ProjectileType duplicado: '" + id + "'");
        }
        factories.put(id, factory);
        return this; // fluent API para encadenar registros
    }

    /**
     * Registra un tipo de proyectil simple usando solo un BulletBehavior.
     *
     * La velocidad, datos y movimiento vienen de getDefaultData() y
     * getDefaultMovement() del behavior. La dirección se calcula de origin→target.
     * Si target es null, la dirección por defecto es hacia la derecha (1,0).
     *
     * Conveniente para la mayoría de proyectiles de enemigos que solo necesitan
     * un behavior concreto sin personalización adicional.
     *
     * @param id              identificador único del tipo
     * @param behaviorFactory factory que produce el BulletBehavior
     */
    public ProjectileRegistry registerSimple(String id,
                                             Supplier<BulletBehavior> behaviorFactory) {
        return register(id, (origin, target) -> {
            BulletBehavior behavior = behaviorFactory.get();
            ProjectileData data     = behavior.getDefaultData();

            Vector2D dir;
            if (target != null) {
                double dx = target.getX() - origin.getX();
                double dy = target.getY() - origin.getY();
                double len = Math.hypot(dx, dy);
                dir = (len > 1e-6)
                        ? new Vector2D(dx / len, dy / len)
                        : new Vector2D(1, 0);
            } else {
                dir = new Vector2D(1, 0);
            }

            double speed  = data.speedFactor() * 8.0; // velocidad base por defecto
            double damage = data.damage();

            return BulletFactory.createBulletWithBehavior(
                    origin.getX(), origin.getY(),
                    dir,
                    behavior,
                    speed,
                    damage
            );
        });
    }

    /**
     * Registra un tipo de proyectil con velocidad base explícita.
     * Usa getDefaultData() y getDefaultMovement() del behavior para todo lo demás.
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
            ProjectileData data     = behavior.getDefaultData();

            Vector2D dir;
            if (target != null) {
                double dx = target.getX() - origin.getX();
                double dy = target.getY() - origin.getY();
                double len = Math.hypot(dx, dy);
                dir = (len > 1e-6)
                        ? new Vector2D(dx / len, dy / len)
                        : new Vector2D(1, 0);
            } else {
                dir = new Vector2D(1, 0);
            }

            return BulletFactory.createBulletWithBehavior(
                    origin.getX(), origin.getY(),
                    dir,
                    behavior,
                    baseSpeed * data.speedFactor(),
                    data.damage()
            );
        });
    }

    // ── Resolución ────────────────────────────────────────────────────────

    /**
     * Crea un proyectil dado un ID y origen/target.
     *
     * @param id     tipo de proyectil registrado
     * @param origin posición de spawn
     * @param target posición objetivo (puede ser null)
     * @return Bullet listo para añadir al mundo, o null si el ID no está registrado
     */
    public Bullet resolve(String id, Vector2D origin, Vector2D target) {
        ProjectileFactory factory = factories.get(id);
        if (factory == null) {
            System.err.println("[ProjectileRegistry] Tipo desconocido: '" + id + "'");
            return null;
        }
        return factory.create(origin, target);
    }

    /** @return true si el ID está registrado */
    public boolean has(String id) {
        return factories.containsKey(id);
    }

    /** Vista de solo lectura de los IDs registrados. */
    public Map<String, ProjectileFactory> getAll() {
        return Collections.unmodifiableMap(factories);
    }

    // ── Listener de SpawnProjectileEvent ─────────────────────────────────

    /**
     * Registra el listener de SpawnProjectileEvent en el bus global.
     *
     * Cuando un Enemy, Boss o Turret emite un SpawnProjectileEvent, el listener
     * resuelve el proyectil y lo pasa al bulletSpawner para añadirlo al mundo.
     *
     * Llamar una vez desde GameWorldBootstrap después de que el world esté
     * configurado.
     *
     * @param bulletSpawner callback que añade el Bullet al mundo (world::add)
     */
    public void installListener(java.util.function.Consumer<Bullet> bulletSpawner) {
        GameEventBus.GLOBAL.subscribe(SpawnProjectileEvent.class, event -> {
            Vector2D origin = event.origin();
            Vector2D target = event.target();

            if (origin == null) return; // sin origen definido — ignorar

            Bullet bullet = resolve(event.projectileTypeId(), origin, target);
            if (bullet != null) {
                bulletSpawner.accept(bullet);
            }
        });
    }
}
