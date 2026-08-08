package Game.Items.Types.Bullets.Definition;

import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Items.Types.Bullets.BulletFactory;
import Game.Items.Types.Bullets.ProjectileBlueprint;
import Game.Items.Types.Bullets.ResettableMovement;
import Game.Items.Types.Bullets.Flyweight.BulletFlyweight;
import Game.Items.Types.Bullets.Flyweight.BulletFlyweightCache;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Pool dinámico de proyectiles — reutilización de instancias para reducir GC pressure.
 *
 * ── RESPONSABILIDADES ─────────────────────────────────────────────────────
 *
 *   - Mantener instancias de Bullet liberadas y disponibles para reutilización.
 *   - Determinar si una instancia disponible es compatible con el blueprint solicitado.
 *   - Reutilizar instancias compatibles mediante resetState().
 *   - Delegar la construcción de instancias NUEVAS a BulletFactory (única autoridad).
 *   - Gestionar el lifecycle: acquire → active → release → recycle.
 *
 * ── LO QUE NO HACE ────────────────────────────────────────────────────────
 *
 *   × No construye Bullets — eso es exclusivamente BulletFactory.
 *   × No resuelve assets ni profiles — eso es BulletFlyweightCache.
 *   × No decide qué hace un proyectil al impactar — eso es BulletBehavior.
 *
 * ── POOL DINÁMICO SIN TAMAÑO FIJO ─────────────────────────────────────────
 *
 * El pool crece bajo demanda: cuando la demanda concurrente supera las
 * instancias disponibles, BulletFactory crea nuevas instancias. Esas
 * instancias quedan registradas en el pool (via assignPool) y son
 * reutilizadas en los ciclos siguientes al liberarse.
 *
 * No hay un maxSize artificial. El número máximo de instancias activas es
 * el número de proyectiles simultáneos que el juego haya necesitado alguna
 * vez. El pool alcanza su estado estable naturalmente con el tiempo.
 *
 * ── COMPATIBILIDAD DE REUTILIZACIÓN ──────────────────────────────────────
 *
 * Una instancia disponible es compatible con un blueprint si:
 *   1. La clase del behavior almacenado == clase del behavior del blueprint.
 *   2. La clase del movement almacenado == clase del movement del blueprint.
 *   3. El blueprint permite reutilización: behavior stateless Y (movement
 *      stateless O movement ResettableMovement).
 *
 * Dos blueprints con BulletNormal + LinearMovement son compatibles aunque
 * difieran en damage, speed o lifetime: esos valores se sobreescriben en
 * resetState(). Lo que no puede sobreescribirse sin residuo es el tipo de
 * behavior y el tipo de movement.
 *
 * ── RELACIÓN FACTORY ↔ POOL ───────────────────────────────────────────────
 *
 *   acquire(blueprint, position, direction, owner)
 *       │
 *       ├── instancia compatible disponible
 *       │       ↓
 *       │   resetState(...)        ← limpia todo el estado anterior
 *       │   assignPool(this)       ← reregistra para el nuevo ciclo
 *       │   setProjectileContext() ← inyecta contexto actualizado
 *       │   BulletFactory.emitSpawn(bullet, owner)
 *       │
 *       └── no disponible
 *               ↓
 *           BulletFactory.buildForPool(blueprint, pos, dir)
 *               ↓  ← única autoridad de construcción
 *           bullet.assignPool(this)
 *           bullet.setProjectileContext(context)
 *           BulletFactory.emitSpawn(bullet, owner)
 *
 * ── MECANISMO DE RETORNO AL POOL ──────────────────────────────────────────
 *
 * Bullet.isPendingDestruction() detecta cuando el proyectil muere. Si tiene
 * ownerPool != null, llama ownerPool.release(this) automáticamente. No hay
 * subclase PooledBullet ni constructor duplicado: la diferencia entre una
 * Bullet "simple" y una "pooled" es únicamente ese campo ownerPool.
 *
 * ── GARANTÍAS ANTE CONTAMINACIÓN DE ESTADO ────────────────────────────────
 *
 * resetState() garantiza antes de reutilizar:
 *   - posición, velocidad, vida, damage, destroyEventFired: sobreescritos.
 *   - behavior.onRelease() ya fue invocado (en emitDestroy, antes del release).
 *   - movement.reset() invocado si ResettableMovement.
 *   - behavior.onAttached() invocado al final para el nuevo ciclo.
 *   - ownerPool y projectileContext reinyectados después de resetState().
 *   - Flyweight actualizado in-place si el tipo de proyectil cambió.
 */
public final class ProjectilePool {

    /**
     * Cola de instancias disponibles para reutilización.
     *
     * Deque sin límite superior. El pool crece hasta el pico de demanda
     * concurrente y luego reutiliza esas instancias indefinidamente.
     *
     * La búsqueda de compatibilidad es O(n) sobre las disponibles.
     * En práctica n es pequeño: los proyectiles activos se van liberando
     * antes del siguiente disparo, y los tipos distintos en el pool son pocos.
     */
    private final Deque<Bullet> available = new ArrayDeque<>();

    /** Contexto inyectado en cada proyectil adquirido. NULL por defecto. */
    private ProjectileContext context = ProjectileContext.NULL;

    // ── Estadísticas ──────────────────────────────────────────────────────

    private int totalAcquires = 0;
    private int poolHits      = 0;
    private int poolMisses    = 0;

    // ── Context ───────────────────────────────────────────────────────────

    /**
     * Configura el ProjectileContext inyectado en cada proyectil adquirido.
     *
     * Llamar una vez al inicializar el pool:
     *   pool.setContext(new WorldProjectileContext(world));
     *
     * @param ctx contexto a inyectar (null = ProjectileContext.NULL)
     */
    public void setContext(ProjectileContext ctx) {
        this.context = (ctx != null) ? ctx : ProjectileContext.NULL;
    }

    // ── Acquire ───────────────────────────────────────────────────────────

    /**
     * Obtiene un proyectil listo para usar. Emite OnProjectileSpawn con owner = null.
     */
    public Bullet acquire(ProjectileBlueprint blueprint,
                          Vector2D position,
                          Vector2D direction) {
        return acquire(blueprint, position, direction, null);
    }

    /**
     * Obtiene un proyectil listo para usar, propagando el owner al evento de spawn.
     *
     * Lógica de adquisición:
     *   1. Buscar instancia compatible (mismo tipo de behavior + movement).
     *   2. Si existe: resetState() + reinyectar pool + contexto.
     *   3. Si no: BulletFactory.buildForPool() + inyectar pool + contexto.
     *   4. Emitir OnProjectileSpawn en ambos casos (un único punto).
     *
     * @param blueprint definición del proyectil
     * @param position  posición de spawn
     * @param direction dirección normalizada de vuelo
     * @param owner     el objeto que originó el disparo (puede ser null)
     * @return Bullet lista para añadir al mundo
     */
    public Bullet acquire(ProjectileBlueprint blueprint,
                          Vector2D position,
                          Vector2D direction,
                          Object owner) {
        totalAcquires++;

        double xSpeed = direction.getX() * blueprint.speed();
        double ySpeed = direction.getY() * blueprint.speed();

        Bullet bullet = findCompatible(blueprint);

        if (bullet != null) {
            // ── Reutilizar instancia compatible ───────────────────────────
            poolHits++;

            BulletFlyweight newFlyweight = BulletFlyweightCache.INSTANCE.get(blueprint);

            // Reset completo: posición, velocidad, vida, damage, behavior,
            // movement (con reset si ResettableMovement), flyweight y flags.
            // PRE: onRelease() ya fue invocado por emitDestroy() antes de
            // que isPendingDestruction() llamara release().
            bullet.resetState(
                    position.getX(), position.getY(),
                    xSpeed, ySpeed,
                    blueprint.lifeTime(),
                    blueprint.damage(),
                    blueprint.behavior(),
                    blueprint.movement(),
                    newFlyweight
            );

        } else {
            // ── No hay instancia compatible — construir una nueva ─────────
            // BulletFactory es la única autoridad de construcción.
            // buildForPool() construye sin emitir el evento de spawn:
            // el pool lo emitirá a continuación con el owner correcto,
            // en el mismo punto que las instancias reutilizadas.
            poolMisses++;
            bullet = BulletFactory.buildForPool(blueprint, position, direction);
        }

        // Inyectar referencia al pool — mecanismo de auto-devolución.
        // Cuando bullet.isPendingDestruction() detecte destrucción,
        // llamará this.release(bullet) automáticamente.
        bullet.assignPool(this);

        // Inyectar contexto actualizado para onExpire.
        bullet.setProjectileContext(context);

        // ── Único punto de emisión de OnProjectileSpawn ───────────────────
        // Tanto instancias nuevas como reutilizadas emiten aquí, con el
        // mismo owner. Las instancias nuevas no emitieron en buildForPool().
        BulletFactory.emitSpawn(bullet, owner);

        return bullet;
    }

    // ── Release ───────────────────────────────────────────────────────────

    /**
     * Devuelve una Bullet al pool para reutilización futura.
     *
     * Llamado automáticamente desde Bullet.isPendingDestruction() cuando
     * la instancia tiene ownerPool == this.
     *
     * onRelease() ya fue invocado por Bullet.emitDestroy() antes de llegar
     * aquí — el cleanup del behavior está garantizado.
     *
     * package-private: solo accesible desde Bullet (mismo paquete).
     */
    void release(Bullet bullet) {
        available.addFirst(bullet);
    }

    // ── Estadísticas ──────────────────────────────────────────────────────

    public int    getPoolSize()      { return available.size(); }
    public int    getTotalAcquires() { return totalAcquires; }
    public int    getPoolHits()      { return poolHits; }
    public int    getPoolMisses()    { return poolMisses; }
    public double getHitRate() {
        return (totalAcquires == 0) ? 0.0 : (double) poolHits / totalAcquires;
    }

    /** Vacía el pool. Llamar en shutdown del mundo para liberar referencias. */
    public void clear() { available.clear(); }

    // ── Búsqueda de instancia compatible ─────────────────────────────────

    /**
     * Busca y extrae la primera instancia disponible compatible con el blueprint.
     *
     * Compatible = misma clase de behavior + misma clase de movement +
     *              blueprint permite reutilización (stateless o ResettableMovement).
     *
     * Retorna null si no hay ninguna instancia compatible.
     * Complejidad O(n) sobre las instancias disponibles — aceptable en práctica.
     */
    private Bullet findCompatible(ProjectileBlueprint blueprint) {
        if (!canReuse(blueprint)) return null;

        Class<?> behaviorClass = blueprint.behavior().getClass();
        Class<?> movementClass = blueprint.movement().getClass();

        var it = available.iterator();
        while (it.hasNext()) {
            Bullet candidate = it.next();
            if (candidate.getBehavior().getClass() == behaviorClass
                    && candidate.getMovement().getClass() == movementClass) {
                it.remove();
                return candidate;
            }
        }
        return null;
    }

    /**
     * Determina si el blueprint permite reutilizar una instancia del pool.
     *
     * Condiciones de NO reutilización:
     *   - Behavior stateful: no hay garantía de que su estado pueda resetearse.
     *   - Movement stateful + no ResettableMovement: no hay mecanismo de reset.
     */
    private static boolean canReuse(ProjectileBlueprint blueprint) {
        if (!blueprint.behavior().isBehaviorStateless()) return false;
        if (blueprint.movement().isStateless()) return true;
        return blueprint.movement() instanceof ResettableMovement;
    }
}
