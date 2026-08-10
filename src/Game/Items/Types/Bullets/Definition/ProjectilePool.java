package Game.Items.Types.Bullets.Definition;

import Game.Engine.Events.GameEventBus;
import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Engine.Pooling.AbstractObjectPool;
import Game.Items.Types.Bullets.BulletFactory;
import Game.Items.Types.Bullets.Flyweight.BulletFlyweight;
import Game.Items.Types.Bullets.Flyweight.BulletFlyweightCache;
import Game.Items.Types.Bullets.ProjectileBlueprint;
import Game.Items.Types.Bullets.ResettableMovement;

/**
 * Pool dinámico de proyectiles — reutilización de instancias para reducir GC pressure.
 *
 * ── ARQUITECTURA ──────────────────────────────────────────────────────────
 *
 * ProjectilePool extiende AbstractObjectPool<Bullet> del Engine, consumiendo
 * la infraestructura general de pooling en lugar de reimplementarla.
 *
 * La infraestructura genérica aporta:
 *   - Cola de disponibles (Deque sin límite, LIFO)
 *   - Estadísticas: totalAcquires, poolHits, poolMisses, hitRate
 *   - Protocolo acquire/release con separación de creación y reset
 *
 * La lógica específica de Bullet que permanece aquí:
 *   - Compatibilidad de reutilización (mismo tipo behavior + movement)
 *   - Firma extendida acquire(blueprint, position, direction, owner)
 *   - Inyección de ProjectileContext y ownerPool en cada adquisición
 *   - Delegación de construcción a BulletFactory (única autoridad)
 *   - Emisión de OnProjectileSpawn (un único punto, para nuevas y reutilizadas)
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
public final class ProjectilePool extends AbstractObjectPool<Bullet> {

    /** Contexto inyectado en cada proyectil adquirido. NULL por defecto. */
    private ProjectileContext context = ProjectileContext.NULL;

    /**
     * Bus de eventos inyectado en cada proyectil adquirido.
     * null = los proyectiles no emiten eventos.
     */
    private GameEventBus eventBus = null;

    /**
     * Blueprint en uso durante la llamada actual a acquire().
     */
    private ProjectileBlueprint currentBlueprint = null;

    // ── Context ───────────────────────────────────────────────────────────

    /**
     * Configura el ProjectileContext inyectado en cada proyectil adquirido.
     */
    public void setContext(ProjectileContext ctx) {
        this.context = (ctx != null) ? ctx : ProjectileContext.NULL;
    }

    /**
     * Configura el bus de eventos inyectado en cada proyectil adquirido.
     * Llamar desde GameWorldBootstrap después de crear el bus.
     *
     * @param bus bus activo (null = proyectiles sin eventos)
     */
    public void setEventBus(GameEventBus bus) {
        this.eventBus = bus;
    }

    // ── Acquire con firma específica de Bullet ────────────────────────────

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

        double xSpeed = direction.getX() * blueprint.speed();
        double ySpeed = direction.getY() * blueprint.speed();

        // Exponer blueprint para createInstance() y canReuse() del base
        this.currentBlueprint = blueprint;

        // findReusable() del base llama canReuse(candidate) — que usa currentBlueprint
        Bullet bullet = findReusable();

        if (bullet != null) {
            // ── Reutilizar instancia compatible ───────────────────────────
            recordAcquireHit();

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
            recordAcquireMiss();
            bullet = BulletFactory.buildForPool(blueprint, position, direction);        }

        this.currentBlueprint = null; // limpiar referencia temporal

        // Inyectar referencia al pool — mecanismo de auto-devolución.
        // Cuando bullet.isPendingDestruction() detecte destrucción,
        // llamará this.release(bullet) automáticamente.
        bullet.assignPool(this);

        // Inyectar contexto actualizado para onExpire.
        bullet.setProjectileContext(context);

        // ── Único punto de emisión de OnProjectileSpawn ───────────────────
        // También inyecta el eventBus en el bullet para eventos de ciclo de vida.
        BulletFactory.emitSpawn(eventBus, bullet, owner);

        return bullet;
    }

    // ── Release (package-private para Bullet) ────────────────────────────

    /**
     * Devuelve una Bullet al pool para reutilización futura.
     *
     * Llamado automáticamente desde Bullet.isPendingDestruction() cuando
     * la instancia tiene ownerPool == this.
     *
     * onRelease() ya fue invocado por Bullet.emitDestroy() antes de llegar
     * aquí — el cleanup del behavior está garantizado.
     *
     * Aunque este método es public (requerido por el contrato ObjectPool<T>),
     * solo debe ser llamado desde Bullet.isPendingDestruction() dentro del
     * mismo paquete. No llamar desde código externo.
     */
    @Override
    public void release(Bullet bullet) {
        super.release(bullet);
    }

    // ── AbstractObjectPool<Bullet> — hooks requeridos ────────────────────

    /**
     * Creación de instancia nueva — delegada a BulletFactory.
     *
     * No se usa directamente porque acquire() con blueprint tiene su propio
     * flujo. Este método existe para satisfacer el contrato de AbstractObjectPool
     * y nunca se llama en el flujo normal de acquire(blueprint, ...).
     *
     * Si se llama (por ejemplo desde el acquire() genérico heredado del base),
     * lanzará UnsupportedOperationException: ProjectilePool require un
     * blueprint para construir una Bullet y acquire() genérico no lo provee.
     */
    @Override
    protected Bullet createInstance() {
        throw new UnsupportedOperationException(
            "ProjectilePool.createInstance() no soportado sin blueprint. " +
            "Usar acquire(blueprint, position, direction)."
        );
    }

    /**
     * Reset de instancia — no usado en el flujo normal.
     *
     * El reset real ocurre en bullet.resetState(...) con todos los parámetros
     * del blueprint. Este hook del base no tiene suficiente contexto para
     * resetear correctamente una Bullet.
     */
    @Override
    protected void resetInstance(Bullet bullet) {
        // No-op: el reset concreto ocurre en acquire() via bullet.resetState(...)
        // con los parámetros completos del blueprint.
    }

    /**
     * Compatibilidad de reutilización — lógica específica de Bullet.
     *
     * Una Bullet es reutilizable si:
     *   1. El blueprint actual permite reutilización (behavior stateless + movement
     *      stateless o ResettableMovement).
     *   2. El behavior de la Bullet disponible es del mismo tipo que el del blueprint.
     *   3. El movement de la Bullet disponible es del mismo tipo que el del blueprint.
     *
     * Si currentBlueprint es null (llamada fuera de acquire(blueprint,...)),
     * retorna false conservadoramente.
     */
    @Override
    protected boolean canReuse(Bullet candidate) {
        ProjectileBlueprint bp = this.currentBlueprint;
        if (bp == null) return false;
        if (!isBlueprintReusable(bp)) return false;

        return candidate.getBehavior().getClass() == bp.behavior().getClass()
            && candidate.getMovement().getClass() == bp.movement().getClass();
    }

    // ── acquire() genérico del base — bloqueado ───────────────────────────

    /**
     * Bloqueado: ProjectilePool requiere un blueprint para construir y resetear
     * una Bullet. Usar acquire(blueprint, position, direction) en su lugar.
     *
     * @throws UnsupportedOperationException siempre
     */
    @Override
    public Bullet acquire() {
        throw new UnsupportedOperationException(
            "Usar ProjectilePool.acquire(blueprint, position, direction)."
        );
    }

    // ── Helpers privados ──────────────────────────────────────────────────

    /**
     * Determina si el blueprint permite reutilizar una instancia del pool.
     *
     * Condiciones de NO reutilización:
     *   - Behavior stateful: no hay garantía de que su estado pueda resetearse.
     *   - Movement stateful + no ResettableMovement: no hay mecanismo de reset.
     */
    private static boolean isBlueprintReusable(ProjectileBlueprint blueprint) {
        if (!blueprint.behavior().isBehaviorStateless()) return false;
        if (blueprint.movement().isStateless()) return true;
        return blueprint.movement() instanceof ResettableMovement;
    }

    /** Vacía el pool. Llamar en shutdown del mundo para liberar referencias. */
    @Override
    public void clear() {
        super.clear();
    }
}
