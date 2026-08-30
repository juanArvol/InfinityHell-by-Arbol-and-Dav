package Game.Engine.Simulation;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * GUÍA DE MIGRACIÓN A ARQUITECTURA HÍBRIDA ECS/DOD
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * HRFC — Game.Engine Unified Simulation Data Architecture / ECS-DOD Foundation
 *
 * Esta guía explica cómo migrar dominios existentes (Bullets, Enemies, Player,
 * Bosses) a la nueva infraestructura híbrida ECS/DOD del Game.Engine.
 *
 * ═══════════════════════════════════════════════════════════════════════════
 * § 1. PRINCIPIO FUNDAMENTAL
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * La arquitectura híbrida separa:
 *
 *   IDENTIDAD / LÓGICA PROPIA        →  Objeto OO (dominio)
 *   DATOS DE SIMULACIÓN              →  Engine DOD (infraestructura)
 *
 * UNA ENTIDAD PUEDE SER SIMULTÁNEAMENTE:
 *   - Una instancia OO con lógica propia
 *   - Una entidad DOD con datos de simulación
 *
 * Ejemplo:
 *
 *   Player (instancia OO)
 *     ├── inventory          → OO
 *     ├── loadout            → OO
 *     ├── input logic        → OO
 *     └── EntityId           → handle a DOD
 *            ↓
 *   EntityStore (DOD)
 *     ├── position           → primitivo
 *     ├── velocity           → primitivo
 *     ├── health             → primitivo
 *     └── collision          → primitivo
 *
 * ═══════════════════════════════════════════════════════════════════════════
 * § 2. REGLA DE CLASIFICACIÓN: ¿QUÉ VA A DOD?
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * Usar esta regla para CADA CAMPO de una entidad:
 *
 *   ┌────────────────────────────────────────────────────────────────┐
 *   │ ¿Es procesado frecuentemente por simulación?                   │
 *   │ ¿Es primitivo o representable como primitivos?                 │
 *   │ ¿Es compartible entre múltiples tipos de entidades?            │
 *   │ ¿Puede procesarse en batch sin conocer la identidad?           │
 *   └────────────────────────────────────────────────────────────────┘
 *
 *   SI mayoría de respuestas = SÍ  →  CANDIDATO PARA DOD
 *   SI mayoría de respuestas = NO  →  MANTENER EN OO
 *
 * ───────────────────────────────────────────────────────────────────────────
 * § 2.1. EJEMPLOS DE DATOS DOD (típicamente migrar)
 * ───────────────────────────────────────────────────────────────────────────
 *
 *   ✓ position (x, y)              → float[], float[]
 *   ✓ velocity (vx, vy)            → float[], float[]
 *   ✓ acceleration (ax, ay)        → float[], float[]
 *   ✓ health                       → float[]
 *   ✓ lifetime                     → float[]
 *   ✓ rotation                     → float[]
 *   ✓ mass                         → float[]
 *   ✓ collision bounds             → float[] (minX, minY, maxX, maxY)
 *   ✓ flags (estado binario)       → int[] (bitfield)
 *   ✓ typeId (para dispatch)       → int[]
 *   ✓ behaviorId (para dispatch)   → int[]
 *
 * ───────────────────────────────────────────────────────────────────────────
 * § 2.2. EJEMPLOS DE DATOS OO (típicamente NO migrar)
 * ───────────────────────────────────────────────────────────────────────────
 *
 *   ✗ inventory (lista de items)      → OO
 *   ✗ loadout (configuración)         → OO
 *   ✗ attack patterns (comportamiento complejo) → OO
 *   ✗ boss phases (máquina de estados)  → OO
 *   ✗ AI decision trees               → OO
 *   ✗ weapon definitions              → OO
 *   ✗ bullet behavior objects         → OO
 *   ✗ animation controllers           → OO
 *   ✗ UI state                        → OO
 *   ✗ referencias a otros objetos     → OO
 *   ✗ listeners/callbacks             → OO
 *
 * ═══════════════════════════════════════════════════════════════════════════
 * § 3. PATRÓN DE MIGRACIÓN PARA UNA ENTIDAD
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * ANTES (solo OO):
 *
 *   class Bullet extends MovingObjects {
 *       private Vector2D position;   // field OO
 *       private Vector2D velocity;   // field OO
 *       private float lifetime;      // field OO
 *       private BulletBehavior behavior;  // lógica
 *
 *       public void update(double dt) {
 *           position.addLocal(velocity.scale(dt));
 *           lifetime -= dt;
 *           behavior.update(this, dt);
 *       }
 *   }
 *
 * DESPUÉS (híbrido OO + DOD):
 *
 *   class Bullet {
 *       private final EntityId entityId;        // handle a DOD
 *       private final BulletBehavior behavior;  // lógica (OO)
 *
 *       // Constructor
 *       public Bullet(EntityStore store, BulletBehavior behavior) {
 *           // Crear entidad en DOD
 *           ComponentMask mask = ComponentMask.EMPTY
 *               .with(ComponentType.POSITION.id())
 *               .with(ComponentType.VELOCITY.id())
 *               .with(ComponentType.LIFETIME.id());
 *
 *           this.entityId = store.create(mask);
 *           this.behavior = behavior;
 *       }
 *
 *       // Acceso a datos de simulación
 *       public Vector2D getPosition(EntityStore store) {
 *           SimulationHandle h = store.getHandle(entityId);
 *           if (!h.isValid()) return Vector2D.ZERO;
 *
 *           PrimitiveStorage s = store.getStorage();
 *           int idx = h.index();
 *           return new Vector2D(s.positionsX()[idx], s.positionsY()[idx]);
 *       }
 *
 *       // Lógica específica de dominio
 *       public void updateBehavior(EntityStore store, double dt) {
 *           behavior.update(this, store, dt);
 *       }
 *   }
 *
 *   // Los datos position, velocity, lifetime son procesados por:
 *   // AccelerationSystem, MovementSystem, LifetimeSystem
 *   // en el SimulationPipeline
 *
 * ═══════════════════════════════════════════════════════════════════════════
 * § 4. FLUJO TÍPICO DE CREACIÓN DE ENTIDAD
 * ═══════════════════════════════════════════════════════════════════════════
 *
 *   // 1. Determinar qué componentes necesita
 *   ComponentMask mask = ComponentMask.EMPTY
 *       .with(ComponentType.POSITION.id())
 *       .with(ComponentType.VELOCITY.id())
 *       .with(ComponentType.LIFETIME.id())
 *       .with(ComponentType.DAMAGE.id());
 *
 *   // 2. Crear en EntityStore
 *   EntityId id = entityStore.create(mask);
 *
 *   // 3. Obtener handle
 *   SimulationHandle handle = entityStore.getHandle(id);
 *   int idx = handle.index();
 *
 *   // 4. Inicializar datos de simulación
 *   PrimitiveStorage storage = entityStore.getStorage();
 *   storage.positionsX()[idx] = spawnX;
 *   storage.positionsY()[idx] = spawnY;
 *   storage.velocitiesX()[idx] = dirX * speed;
 *   storage.velocitiesY()[idx] = dirY * speed;
 *   storage.lifetimes()[idx] = 3.0f;  // 3 segundos
 *   storage.damage()[idx] = 50f;
 *
 *   // 5. Crear instancia OO que mantiene EntityId
 *   Bullet bullet = new Bullet(id, behavior, typeDefinition);
 *
 * ═══════════════════════════════════════════════════════════════════════════
 * § 5. ACCESO A DATOS DE SIMULACIÓN DESDE DOMINIO
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * Cuando el dominio necesita leer/escribir datos de simulación:
 *
 *   class Bullet {
 *       private final EntityId entityId;
 *
 *       public void applyKnockback(EntityStore store, float kx, float ky) {
 *           SimulationHandle h = store.getHandle(entityId);
 *           if (!h.isValid()) return;
 *
 *           PrimitiveStorage s = store.getStorage();
 *           int idx = h.index();
 *
 *           // Modificar velocidad directamente
 *           s.velocitiesX()[idx] += kx;
 *           s.velocitiesY()[idx] += ky;
 *       }
 *
 *       public float getX(EntityStore store) {
 *           SimulationHandle h = store.getHandle(entityId);
 *           if (!h.isValid()) return 0f;
 *           return store.getStorage().positionsX()[h.index()];
 *       }
 *   }
 *
 * IMPORTANTE: Siempre validar handle antes de acceder a los arrays.
 *
 * ═══════════════════════════════════════════════════════════════════════════
 * § 6. INTEGRACIÓN CON SIMULATIONPIPELINE
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * Setup del pipeline (una vez al inicio):
 *
 *   EntityStore entityStore = new EntityStore();
 *   SimulationPipeline pipeline = new SimulationPipeline(entityStore);
 *
 *   // Registrar sistemas base
 *   pipeline.register(new AccelerationSystem());
 *   pipeline.register(new MovementSystem());
 *   pipeline.register(new LifetimeSystem());
 *
 *   // Registrar sistemas específicos de dominio (futuro)
 *   pipeline.register(new ProjectileBehaviorSystem());
 *   pipeline.register(new EnemyAISystem());
 *
 *   // Opcional: auto-compactación
 *   pipeline.setAutoCompact(true);
 *
 * Game loop:
 *
 *   while (running) {
 *       double deltaTime = timer.getDeltaTime();
 *
 *       // 1. Lógica de dominio (OO)
 *       for (Bullet b : bullets) {
 *           b.updateBehavior(entityStore, deltaTime);
 *       }
 *
 *       // 2. Simulación física (DOD batch)
 *       pipeline.update(deltaTime);
 *
 *       // 3. Collision, render, etc.
 *       collisionSystem.update(objects);
 *       renderSystem.update(objects);
 *   }
 *
 * ═══════════════════════════════════════════════════════════════════════════
 * § 7. MIGRACIÓN DE PLAYER
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * Player es un caso especial:
 *   - Cardinalidad 1 (solo un Player)
 *   - Lógica compleja (input, combat, inventory)
 *   - Algunos datos son de simulación (position, velocity, health)
 *
 * ESTRATEGIA:
 *
 *   Player (OO)
 *     ├── inventory          → OO (no migrar)
 *     ├── loadout            → OO (no migrar)
 *     ├── controller         → OO (no migrar)
 *     ├── combat             → OO (no migrar)
 *     └── EntityId           → handle a simulación
 *            ↓
 *   EntityStore (DOD)
 *     ├── position           → migrar
 *     ├── velocity           → migrar
 *     ├── health             → migrar
 *     └── collision          → migrar
 *
 * NO ES NECESARIO que Player use DOD si la complejidad no lo justifica.
 * La arquitectura es OPCIONAL — solo migrar cuando el beneficio es claro.
 *
 * ═══════════════════════════════════════════════════════════════════════════
 * § 8. MIGRACIÓN DE BOSS
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * Boss similar a Player:
 *   - Lógica específica compleja (patterns, phases, AI)
 *   - Datos físicos simples (position, velocity, health)
 *
 * ESTRATEGIA:
 *
 *   Boss (OO)
 *     ├── attack patterns    → OO (no migrar)
 *     ├── phases             → OO (no migrar)
 *     ├── AI                 → OO (no migrar)
 *     └── EntityId           → handle a simulación
 *            ↓
 *   EntityStore (DOD)
 *     ├── position           → migrar
 *     ├── velocity           → migrar
 *     ├── health             → migrar
 *     └── collision          → migrar
 *
 * Mismo principio que Player: migrar solo datos de simulación, mantener
 * lógica específica en OO.
 *
 * ═══════════════════════════════════════════════════════════════════════════
 * § 9. MIGRACIÓN DE BULLETS (CASO IDEAL PARA DOD)
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * Bullets son el caso ideal para DOD:
 *   - Alta cardinalidad (3,600 - 10,000+)
 *   - Datos simples (position, velocity, lifetime, damage)
 *   - Procesamiento uniforme (movimiento, lifetime, behavior)
 *
 * ESTRATEGIA:
 *
 *   // Datos de simulación → 100% DOD
 *   EntityStore:
 *     position, velocity, lifetime, damage, typeId, behaviorId
 *
 *   // Lógica de comportamiento → puede permanecer OO si es complejo
 *   BulletBehavior (interface):
 *     void update(EntityId bullet, EntityStore store, double dt)
 *
 *   // Dispatching por typeId/behaviorId
 *   ProjectileBehaviorSystem:
 *     for (int i = 0; i < count; i++) {
 *         int behaviorId = storage.behaviorIds()[i];
 *         BulletBehavior behavior = behaviorRegistry.get(behaviorId);
 *         behavior.update(entityId, store, dt);
 *     }
 *
 * BENEFICIO ESPERADO: 3-5x speedup para 10K+ bullets
 *
 * ═══════════════════════════════════════════════════════════════════════════
 * § 10. MIGRACIÓN DE ENEMIES
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * Enemies: caso intermedio
 *   - Cardinalidad media-alta (100-1,000)
 *   - Datos físicos simples
 *   - IA variable (puede ser compleja o simple)
 *
 * ESTRATEGIA:
 *
 *   Enemy (OO — opcional)
 *     ├── AI state           → OO si complejo
 *     ├── EntityId           → handle a simulación
 *     └── logic              → OO
 *
 *   EntityStore (DOD)
 *     ├── position
 *     ├── velocity
 *     ├── health
 *     ├── typeId
 *     └── behaviorId
 *
 * Enemigos simples (walkers, drones) pueden ser 100% DOD.
 * Enemigos complejos (con state machines) mantienen lógica en OO.
 *
 * ═══════════════════════════════════════════════════════════════════════════
 * § 11. CREACIÓN DE SISTEMAS ESPECÍFICOS DE DOMINIO
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * Los dominios pueden crear sus propios sistemas:
 *
 *   public class ProjectileBehaviorSystem implements SimulationSystem {
 *
 *       private final ComponentMask requirements = ComponentMask.EMPTY
 *           .with(ComponentType.POSITION.id())
 *           .with(ComponentType.VELOCITY.id())
 *           .with(ComponentType.BEHAVIOR_ID.id());
 *
 *       private final BehaviorRegistry behaviorRegistry;
 *
 *       @Override
 *       public void update(EntityStore store, double deltaTime) {
 *           PrimitiveStorage s = store.getStorage();
 *           int[] behaviorIds = s.behaviorIds();
 *
 *           for (int i = 0; i < store.count(); i++) {
 *               EntityId id = store.getEntityAt(i);
 *               if (id == null) continue;
 *
 *               EntityRecord rec = store.getRecord(id);
 *               if (!rec.mask().matches(requirements)) continue;
 *
 *               int behaviorId = behaviorIds[i];
 *               BulletBehavior behavior = behaviorRegistry.get(behaviorId);
 *               behavior.update(id, store, deltaTime);
 *           }
 *       }
 *   }
 *
 * Registrar en pipeline:
 *
 *   pipeline.register(new ProjectileBehaviorSystem(behaviorRegistry));
 *
 * ═══════════════════════════════════════════════════════════════════════════
 * § 12. ORDEN DE MIGRACIÓN RECOMENDADO
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * Para migrar Infinity HELL progresivamente:
 *
 *   FASE 1 — Infraestructura (✓ COMPLETADO)
 *     ✓ EntityStore, PrimitiveStorage, SimulationPipeline
 *     ✓ Sistemas base: Movement, Acceleration, Lifetime
 *     ✓ Benchmark para validar arquitectura
 *
 *   FASE 2 — Bullets (próximo HRFC)
 *     → Mayor beneficio (alta cardinalidad)
 *     → Migrar datos de simulación a DOD
 *     → Conservar BulletBehavior en OO si es complejo
 *     → Medir performance real con 3,600+ bullets
 *
 *   FASE 3 — Enemies (HRFC posterior)
 *     → Migrar enemigos simples a DOD
 *     → Enemigos complejos mantienen lógica en OO
 *
 *   FASE 4 — Particles y efectos (HRFC posterior)
 *     → Alta cardinalidad, ideal para DOD
 *
 *   FASE 5 — Player/Boss (OPCIONAL)
 *     → Solo si el beneficio justifica la complejidad
 *     → Cardinalidad baja, beneficio limitado
 *
 * ═══════════════════════════════════════════════════════════════════════════
 * § 13. CHECKLIST DE MIGRACIÓN PARA UN DOMINIO
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * □ 1. Identificar datos de simulación (position, velocity, etc.)
 * □ 2. Identificar lógica de dominio (behavior, AI, patterns)
 * □ 3. Crear ComponentMask apropiada
 * □ 4. Modificar constructor para crear EntityId
 * □ 5. Modificar accessors para leer desde EntityStore
 * □ 6. Crear sistemas específicos si es necesario
 * □ 7. Registrar sistemas en SimulationPipeline
 * □ 8. Actualizar código que accede a datos migrados
 * □ 9. Ejecutar benchmark para validar mejora
 * □ 10. Ejecutar tests de gameplay para validar corrección
 *
 * ═══════════════════════════════════════════════════════════════════════════
 * § 14. ANTIPATRONES A EVITAR
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * ✗ NO duplicar datos entre OO y DOD
 *   Malo:
 *     class Bullet {
 *         Vector2D position;  // OO
 *         EntityId entityId;  // DOD también tiene position
 *     }
 *
 *   Bueno:
 *     class Bullet {
 *         EntityId entityId;  // única fuente: DOD
 *         Vector2D getPosition(EntityStore s) { ... }
 *     }
 *
 * ✗ NO cachear handles durante períodos largos
 *   Malo:
 *     class Bullet {
 *         SimulationHandle cachedHandle;  // puede invalidarse
 *     }
 *
 *   Bueno:
 *     SimulationHandle h = store.getHandle(entityId);
 *     // usar inmediatamente, descartar
 *
 * ✗ NO acceder a arrays sin validar handle
 *   Malo:
 *     int idx = handle.index();
 *     float x = storage.positionsX()[idx];  // puede ser índice inválido
 *
 *   Bueno:
 *     if (!handle.isValid()) return;
 *     int idx = handle.index();
 *     float x = storage.positionsX()[idx];
 *
 * ✗ NO migrar TODO a DOD "porque sí"
 *   Migrar solo cuando el beneficio es claro.
 *   Lógica compleja puede permanecer en OO.
 *
 * ═══════════════════════════════════════════════════════════════════════════
 * § 15. PREGUNTAS FRECUENTES
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * Q: ¿Debo migrar TODA una clase a DOD?
 * A: NO. Migrar solo los datos de simulación. La lógica específica puede
 *    permanecer en OO. La arquitectura es HÍBRIDA.
 *
 * Q: ¿Qué pasa con Vector2D?
 * A: Vector2D sigue siendo válido para APIs de alto nivel. Simplemente no
 *    es el storage primario de datos calientes. Los arrays primitivos son
 *    el storage, Vector2D es la interfaz.
 *
 * Q: ¿Cómo accedo a la posición de un Bullet desde fuera?
 * A: Mediante un getter que lee del EntityStore:
 *      bullet.getPosition(entityStore)
 *    O accediendo directamente si tienes el EntityId:
 *      store.getHandle(id) → arrays
 *
 * Q: ¿Qué pasa con las colisiones?
 * A: El sistema de colisiones existente puede seguir operando sobre
 *    GameObjects. Futuros HRFCs pueden integrar CollisionSystem con DOD.
 *
 * Q: ¿Cuándo veré beneficios de performance?
 * A: Para poblaciones grandes (3K+). Para entidades individuales o pequeñas
 *    poblaciones (<1K), el overhead puede ser similar o mayor que OO.
 *
 * Q: ¿Debo usar esto para el Player?
 * A: OPCIONAL. Player tiene cardinalidad 1, el beneficio es limitado.
 *    Solo migrar si quieres consistencia arquitectónica o si Player
 *    necesita integrarse con sistemas batch (ej: spatial queries).
 *
 * ═══════════════════════════════════════════════════════════════════════════
 * § 16. RECURSOS Y REFERENCIAS
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * Código de ejemplo:
 *   - Game.Engine.Simulation.Benchmark.SimulationBenchmark
 *   - Game.Engine.Simulation.Systems.MovementSystem
 *   - Game.Engine.Simulation.Systems.AccelerationSystem
 *   - Game.Engine.Simulation.Systems.LifetimeSystem
 *
 * Infraestructura principal:
 *   - Game.Engine.Simulation.Storage.EntityStore
 *   - Game.Engine.Simulation.Storage.PrimitiveStorage
 *   - Game.Engine.Simulation.Systems.SimulationPipeline
 *
 * Próximos HRFCs:
 *   - HRFC — Projectile DOD Migration
 *   - HRFC — Enemy DOD Migration
 *   - HRFC — Spatial System Integration
 *   - HRFC — Collision System DOD Integration
 *
 * ═══════════════════════════════════════════════════════════════════════════
 * FIN DE LA GUÍA DE MIGRACIÓN
 * ═══════════════════════════════════════════════════════════════════════════
 */
public final class MIGRATION_GUIDE {
    private MIGRATION_GUIDE() {}
}
