package Game.Items.Types.Bullets.Definition;

import Game.Engine.GameEventBus;
import Game.Engine.Pooling.AbstractObjectPool;
import Game.Items.Types.Bullets.Capability.ProjectileContextResolver;
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

    /** 
     * Resolver that composes ProjectileContext from registered capability providers.
     * null = no resolver, all bullets get ProjectileContext.NULL
     */
    private ProjectileContextResolver contextResolver = null;

    /**
     * Bus de eventos inyectado en cada proyectil adquirido.
     * null = los proyectiles no emiten eventos.
     */
    private GameEventBus eventBus = null;

    /**
     * Blueprint en uso durante la llamada actual a acquire().
     */
    private ProjectileBlueprint currentBlueprint = null;

    // ── Instrumentación adicional ─────────────────────────────────────────

    /**
     * Número total de instancias Bullet creadas por este pool desde su inicio.
     * Incrementa solo cuando BulletFactory.buildForPool() es llamado.
     */
    private int instancesCreated = 0;

    /**
     * Número de instancias actualmente activas (adquiridas pero no liberadas).
     */
    private int activeInstances = 0;

    /**
     * Máximo número de instancias activas simultáneamente durante la vida del pool.
     */
    private int peakActiveInstances = 0;

    // ── Context ───────────────────────────────────────────────────────────

    /**
     * Configura el ProjectileContextResolver usado para componer contextos
     * según las capacidades requeridas por cada blueprint.
     */
    public void setContextResolver(ProjectileContextResolver resolver) {
        this.contextResolver = resolver;
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
     * 
     * FASE 4 — Optimización: firma con primitivos para reducir allocations.
     */
    public Bullet acquire(ProjectileBlueprint blueprint,
                          double posX, double posY,
                          double dirX, double dirY) {
        return acquire(blueprint, posX, posY, dirX, dirY, null);
    }

    /**
     * Obtiene un proyectil listo para usar, propagando el owner al evento de spawn.
     *
     * FASE 4 — Optimización: firma con primitivos para reducir allocations.
     * Los componentes x/y de position y direction se pasan directamente sin
     * crear objetos Vector2D temporales en el hot path de disparo.
     *
     * Lógica de adquisición:
     *   1. Buscar instancia compatible (mismo tipo de behavior + movement).
     *   2. Si existe: resetState() + reinyectar pool + contexto.
     *   3. Si no: BulletFactory.buildForPool() + inyectar pool + contexto.
     *   4. Emitir OnProjectileSpawn en ambos casos (un único punto).
     *
     * @param blueprint definición del proyectil
     * @param posX      posición de spawn (coordenada X)
     * @param posY      posición de spawn (coordenada Y)
     * @param dirX      dirección normalizada de vuelo (componente X)
     * @param dirY      dirección normalizada de vuelo (componente Y)
     * @param owner     el objeto que originó el disparo (puede ser null)
     * @return Bullet lista para añadir al mundo
     * @throws IllegalStateException si el blueprint requiere capacidades pero no hay resolver configurado
     */
    public Bullet acquire(ProjectileBlueprint blueprint,
                          double posX, double posY,
                          double dirX, double dirY,
                          Object owner) {

        double xSpeed = dirX * blueprint.speed();
        double ySpeed = dirY * blueprint.speed();

        // Exponer blueprint para createInstance() y canReuse() del base
        this.currentBlueprint = blueprint;
        
        try {
            // findReusable() del base llama canReuse(candidate) — que usa currentBlueprint
            Bullet bullet = findReusable();

            if (bullet != null) {
                // ── Reutilizar instancia compatible ───────────────────────────
                recordAcquireHit();

                BulletFlyweight newFlyweight = BulletFlyweightCache.INSTANCE.get(blueprint);

                // Reset completo con primitivos (FASE 4)
                bullet.resetState(
                        posX, posY,
                        xSpeed, ySpeed,
                        blueprint.lifeTime(),
                        blueprint.damage(),
                        blueprint.behavior(),
                        blueprint.movement(),
                        newFlyweight,
                        blueprint.physicalState()  // Mini-HRFC
                );

            } else {
                // ── No hay instancia compatible — construir una nueva ─────────
                // BulletFactory es la única autoridad de construcción.
                // buildForPool() construye sin emitir el evento de spawn:
                // el pool lo emitirá a continuación con el owner correcto,
                // en el mismo punto que las instancias reutilizadas.
                recordAcquireMiss();
                bullet = BulletFactory.buildForPool(blueprint, posX, posY, dirX, dirY);
                instancesCreated++; // FASE 2 — Instrumentación
            }

            // Inyectar referencia al pool — mecanismo de auto-devolución.
            // Cuando bullet.isPendingDestruction() detecte destrucción,
            // llamará this.release(bullet) automáticamente.
            bullet.assignPool(this);

            // ── Resolver contexto según capacidades requeridas ────────────────
            ProjectileContext resolvedContext = resolveContext(blueprint);
            bullet.setProjectileContext(resolvedContext);

            // ── Configurar spawn origin ───────────────────────────────────────
            bullet.setSpawnOrigin(posX, posY); // FASE 4 — primitivos

            // ── Único punto de emisión de OnProjectileSpawn ───────────────────
            // emitSpawn() establece el eventBus Y el owner en el bullet antes
            // de emitir el evento — única fuente de verdad para ownership.
            BulletFactory.emitSpawn(eventBus, bullet, owner);

            // FASE 2 — Actualizar contadores de instancias activas
            activeInstances++;
            if (activeInstances > peakActiveInstances) {
                peakActiveInstances = activeInstances;
            }

            return bullet;
            
        } finally {
            // Limpiar referencia temporal incluso si ocurre excepción
            this.currentBlueprint = null;
        }
    }
    
    /**
     * Resuelve el ProjectileContext según las capacidades requeridas.
     * 
     * Casos:
     *   A. Sin capacidades requeridas → ProjectileContext.NULL válido
     *   B. Con capacidades + resolver configurado → resolver context
     *   C. Con capacidades SIN resolver → IllegalStateException
     * 
     * @param blueprint blueprint del proyectil
     * @return ProjectileContext válido, nunca null
     * @throws IllegalStateException si requiere capacidades sin resolver configurado
     */
    private ProjectileContext resolveContext(ProjectileBlueprint blueprint) {
        java.util.Set<Class<?>> requiredCapabilities = blueprint.getRequiredCapabilities();
        
        // Caso A — Sin capacidades requeridas
        if (requiredCapabilities.isEmpty()) {
            return ProjectileContext.NULL;
        }
        
        // Caso C — Requiere capacidades pero no hay resolver
        if (contextResolver == null) {
            throw new IllegalStateException(
                "ProjectilePool cannot acquire projectile because the blueprint " +
                "requires projectile capabilities but no ProjectileContextResolver " +
                "has been configured.\n" +
                "Behavior: " + blueprint.behavior().getClass().getSimpleName() + "\n" +
                "Required capabilities: " + requiredCapabilities
            );
        }
        
        // Caso B — Resolver capacidades
        ProjectileContext resolvedContext = contextResolver.resolve(requiredCapabilities);
        
        // Validar que el resolver no retornó null
        if (resolvedContext == null) {
            throw new IllegalStateException(
                "ProjectileContextResolver.resolve() returned null for required capabilities.\n" +
                "Behavior: " + blueprint.behavior().getClass().getSimpleName() + "\n" +
                "Required capabilities: " + requiredCapabilities
            );
        }
        
        // Validar que todas las capacidades requeridas estén presentes
        for (Class<?> capabilityType : requiredCapabilities) {
            if (!resolvedContext.hasCapability(capabilityType)) {
                throw new IllegalStateException(
                    "ProjectileContext is missing required capability.\n" +
                    "Behavior: " + blueprint.behavior().getClass().getSimpleName() + "\n" +
                    "Missing capability: " + capabilityType.getSimpleName() + "\n" +
                    "Required capabilities: " + requiredCapabilities + "\n" +
                    "Resolved context: " + resolvedContext
                );
            }
        }
        
        return resolvedContext;
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
        
        // FASE 2 — Decrementar contador de instancias activas
        // Proteger contra doble release (aunque Bullet ya lo previene)
        if (activeInstances > 0) {
            activeInstances--;
        }
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
        // FASE 2 — Resetear contadores adicionales
        instancesCreated = 0;
        activeInstances = 0;
        peakActiveInstances = 0;
    }

    // ── FASE 2 — Getters de instrumentación adicional ────────────────────

    /**
     * @return número total de instancias Bullet creadas por este pool desde su inicio
     */
    public int getInstancesCreated() {
        return instancesCreated;
    }

    /**
     * @return número de instancias actualmente activas (adquiridas pero no liberadas)
     */
    public int getActiveInstances() {
        return activeInstances;
    }

    /**
     * @return máximo número de instancias activas simultáneamente durante la vida del pool
     */
    public int getPeakActiveInstances() {
        return peakActiveInstances;
    }
}
