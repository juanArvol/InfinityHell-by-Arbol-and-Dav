package Game.Items.Types.Bullets.Definition;

import Game.Engine.GameEventBus;
import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Engine.Simulation.Storage.EntityStore;
import Game.Gameplay.Events.ProjectileEvents;
import Game.Items.Types.Bullets.BulletComport.BulletStats;
import Game.Items.Types.Bullets.Flyweight.BulletFlyweight;
import Game.Items.Types.Bullets.Flyweight.BulletFlyweightCache;
import Game.Items.Types.Bullets.Movement.CompositeMovement;
import Game.Items.Types.Bullets.Movement.GravityMovement;
import Game.Items.Types.Bullets.ProjectileBlueprint;
import Game.Items.Types.Bullets.ProjectileMovement;

/**
 * Factory de proyectiles — única autoridad de construcción de instancias Bullet.
 *
 * ── RESPONSABILIDADES ─────────────────────────────────────────────────────
 *
 *   1. Resolver el BulletFlyweight del cache (recursos compartidos del tipo).
 *   2. Calcular velocidad X/Y desde speed escalar y dirección normalizada.
 *   3. Construir la instancia Bullet con el Flyweight ya resuelto.
 *   4. Registrar la entidad en EntityStore (DOD).
 *   5. Emitir OnProjectileSpawn cuando corresponde.
 *
 * ── HRFC — Projectile DOD Migration ──────────────────────────────────────
 *
 * EntityStore es requerido para construcción. Debe ser inyectado via:
 *   - setEntityStore() durante bootstrap
 *   - O pasado explícitamente en cada llamada a build/buildForPool
 *
 * Sin EntityStore configurado, las llamadas fallarán con IllegalStateException.
 *
 * ── LO QUE NO HACE ────────────────────────────────────────────────────────
 *
 *   × No decide si una instancia debe reutilizarse (eso es ProjectilePool).
 *   × No gestiona disponibilidad ni reciclaje de instancias.
 *   × No resuelve assets directamente (delega a BulletFlyweightCache).
 *   × No conoce Player, Enemy ni ninguna entidad concreta.
 *   × No contiene if/switch por tipo de proyectil.
 *
 * ── DOS RUTAS DE CONSTRUCCIÓN ─────────────────────────────────────────────
 *
 * build()        — ruta pública. Construye y emite OnProjectileSpawn.
 *                  Usar desde ModifiedWeapon y cualquier sistema que construya
 *                  proyectiles sin pool.
 *
 * buildForPool() — ruta del pool (package-private). Construye SIN emitir
 *                  OnProjectileSpawn. El pool controla cuándo y con qué owner
 *                  se emite el evento, tanto para instancias nuevas como para
 *                  instancias reutilizadas. Un único punto de emisión en acquire().
 *
 * ── RELACIÓN FACTORY ↔ POOL ───────────────────────────────────────────────
 *
 *   Pool.acquire()
 *       ├── instancia compatible → resetState()
 *       └── no disponible → BulletFactory.buildForPool() → Bullet nueva
 *                               → pool.assignPool(bullet)
 *                               → pool.emit OnProjectileSpawn(bullet, owner)
 *
 *   La Factory no sabe si la Bullet irá a un pool. El Pool no sabe cómo
 *   construir una Bullet. Responsabilidades inequívocas.
 *
 * ── FLYWEIGHT ─────────────────────────────────────────────────────────────
 *
 *   BulletFlyweightCache.INSTANCE.get(blueprint) devuelve el Flyweight
 *   compartido para este tipo (assetKey + profile + dimensiones).
 *   La resolución del asset ocurre solo la primera vez para cada tipo.
 *   Con N Bullets del mismo tipo activas: 1 BulletFlyweight, no N.
 */
public final class BulletFactory {

    /**
     * EntityStore compartido para registrar proyectiles.
     * Debe ser inyectado durante bootstrap via setEntityStore().
     */
    private static EntityStore entityStore = null;

    private BulletFactory() {}

    /**
     * Configura el EntityStore usado para registrar proyectiles.
     * Llamar durante bootstrap del mundo antes de crear proyectiles.
     *
     * @param store EntityStore compartido (no null)
     * @throws IllegalArgumentException si store es null
     */
    public static void setEntityStore(EntityStore store) {
        if (store == null) {
            throw new IllegalArgumentException("EntityStore cannot be null");
        }
        BulletFactory.entityStore = store;
    }

    /**
     * Retorna el EntityStore configurado.
     *
     * @return EntityStore activo
     * @throws IllegalStateException si no se configuró via setEntityStore()
     */
    private static EntityStore requireEntityStore() {
        if (entityStore == null) {
            throw new IllegalStateException(
                "BulletFactory requires EntityStore to be configured. " +
                "Call BulletFactory.setEntityStore() during world bootstrap."
            );
        }
        return entityStore;
    }

    // ── Ruta pública — construcción con evento ────────────────────────────

    /**
     * Construye un Bullet y emite OnProjectileSpawn con owner = null.
     *
     * @param bus       bus de eventos para inyectar en el proyectil
     * @param blueprint definición completa y resuelta del proyectil
     * @param position  posición de spawn en coordenadas del mundo
     * @param direction dirección normalizada de vuelo
     * @return Bullet listo para añadir al mundo
     */
    public static Bullet build(GameEventBus bus,
                               ProjectileBlueprint blueprint,
                               Vector2D position,
                               Vector2D direction) {
        return build(bus, blueprint, position, direction, null);
    }

    /**
     * Construye un Bullet, emite OnProjectileSpawn propagando el owner.
     *
     * @param bus       bus de eventos para inyectar en el proyectil
     * @param blueprint definición completa y resuelta del proyectil
     * @param position  posición de spawn en coordenadas del mundo
     * @param direction dirección normalizada de vuelo
     * @param owner     el objeto que originó el disparo (puede ser null)
     * @return Bullet listo para añadir al mundo
     */
    public static Bullet build(GameEventBus bus,
                               ProjectileBlueprint blueprint,
                               Vector2D position,
                               Vector2D direction,
                               Object owner) {
        // FASE 4 — Delegar a la versión optimizada con primitivos
        Bullet bullet = construct(blueprint, 
            position.getX(), position.getY(),
            direction.getX(), direction.getY());
        emitSpawn(bus, bullet, owner);
        return bullet;
    }

    // ── Ruta del pool — construcción sin evento ───────────────────────────

    /**
     * Construye un Bullet SIN emitir OnProjectileSpawn.
     *
     * FASE 4 — Optimización: firma con primitivos para reducir allocations.
     *
     * Usar exclusivamente desde ProjectilePool.acquire() cuando no hay
     * instancia compatible disponible. El pool emite el evento después de
     * inyectar el ownerPool y el projectileContext, con el owner correcto
     * y en el mismo punto que las reutilizaciones — un único punto de emisión.
     *
     * El nombre "buildForPool" documenta explícitamente que esta ruta omite
     * el evento. No llamar desde código que no sea ProjectilePool.
     *
     * @param blueprint definición completa y resuelta del proyectil
     * @param posX      posición de spawn (coordenada X)
     * @param posY      posición de spawn (coordenada Y)
     * @param dirX      dirección normalizada de vuelo (componente X)
     * @param dirY      dirección normalizada de vuelo (componente Y)
     * @return Bullet nueva, sin evento emitido, sin ownerPool asignado
     */
    public static Bullet buildForPool(ProjectileBlueprint blueprint,
                                      double posX, double posY,
                                      double dirX, double dirY) {
        return construct(blueprint, posX, posY, dirX, dirY);
    }

    // ── Stats para UI (sin instanciar Bullet) ────────────────────────────

    /**
     * Calcula BulletStats desde un ProjectileBlueprint sin crear un Bullet.
     * Usado por CrossHairHUD y cualquier sistema de preview.
     *
     * hasGravity se infiere inspeccionando recursivamente el árbol de movimiento.
     *
     * @param blueprint definición del proyectil
     * @return BulletStats para presentación en UI
     */
    public static BulletStats statsFrom(ProjectileBlueprint blueprint) {
        boolean hasGravity = containsGravity(blueprint.movement());
        return new BulletStats(
                blueprint.speed(),
                blueprint.damage(),
                blueprint.lifeTime(),
                hasGravity
        );
    }

    // ── Construcción interna compartida ──────────────────────────────────

    /**
     * Núcleo de construcción compartido entre build() y buildForPool().
     *
     * FASE 4 — Optimización: firma con primitivos para reducir allocations.
     * Los componentes x/y se pasan directamente sin crear Vector2D temporales.
     *
     * ── HRFC — Projectile DOD Migration ──────────────────────────────────
     *
     * Pasa EntityStore al constructor de Bullet para registro DOD.
     * Resuelve el Flyweight, calcula la velocidad vectorial y construye
     * la instancia Bullet. No emite eventos — eso es responsabilidad del
     * caller según si usa la ruta pública o la ruta del pool.
     *
     * Mini-HRFC — pasa el PhysicalState del blueprint a Bullet.
     */
    private static Bullet construct(ProjectileBlueprint blueprint,
                                    double posX, double posY,
                                    double dirX, double dirY) {
        BulletFlyweight flyweight = BulletFlyweightCache.INSTANCE.get(blueprint);
        EntityStore store = requireEntityStore();

        double xSpeed = dirX * blueprint.speed();
        double ySpeed = dirY * blueprint.speed();

        return new Bullet(
                posX, posY,
                flyweight,
                blueprint.behavior(),
                blueprint.movement(),
                xSpeed,
                ySpeed,
                blueprint.lifeTime(),
                blueprint.damage(),
                blueprint.physicalState(),  // Mini-HRFC — null si no declarado
                store                        // DOD registration
        );
    }

    // ── Emisión de evento (separada para reutilizar desde el pool) ────────

    /**
     * Emite OnProjectileSpawn si hay suscriptores en el bus dado.
     *
     * Separado de construct() para que ProjectilePool pueda emitir el evento
     * una vez, en el mismo punto, tanto para instancias nuevas como para
     * instancias reutilizadas — garantizando que el owner es siempre el correcto.
     *
     * También inyecta el bus en el bullet Y establece el owner antes de emitir
     * el evento. Esto garantiza una única fuente de verdad para ownership:
     *   - bullet.getOwner() == owner
     *   - OnProjectileSpawn.owner == owner
     *
     * @param bus    bus de eventos activo. Null = no se emite nada.
     * @param bullet proyectil recién construido o reutilizado
     * @param owner  objeto que disparó el proyectil (puede ser null)
     */
    public static void emitSpawn(GameEventBus bus, Bullet bullet, Object owner) {
        bullet.setEventBus(bus);
        bullet.setOwner(owner);
        
        if (bus != null && bus.hasListeners(ProjectileEvents.OnProjectileSpawn.class)) {
            bus.post(new ProjectileEvents.OnProjectileSpawn(bullet, owner));
        }
    }

    // ── Utilidad interna ──────────────────────────────────────────────────

    /**
     * Determina si un ProjectileMovement incluye gravedad real.
     *
     * ── HRFC 1 — Consolidación de ProjectileResolver §13 ─────────────────
     *
     * Única fuente de verdad para la detección de gravedad en proyectiles.
     * Usado por:
     *   - ProjectileBlueprint.from() para evitar doble composición de gravedad
     *   - BulletFactory.statsFrom() para derivar BulletStats.hasGravity
     *
     * Inspección recursiva:
     *   - GravityMovement directo → true.
     *   - CompositeMovement → inspecciona cada componente recursivamente.
     *   - Cualquier otro movement → false.
     */
    public static boolean containsGravity(ProjectileMovement movement) {
        if (movement instanceof GravityMovement) return true;
        if (movement instanceof CompositeMovement composite) {
            for (ProjectileMovement component : composite.getComponents()) {
                if (containsGravity(component)) return true;
            }
        }
        return false;
    }
}
