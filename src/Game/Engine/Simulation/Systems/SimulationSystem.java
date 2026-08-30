package Game.Engine.Simulation.Systems;

import Game.Engine.Simulation.Storage.EntityStore;

/**
 * Contrato base de todos los sistemas de simulación DOD.
 *
 * ── HRFC — Game.Engine Unified Simulation Data Architecture / ECS-DOD ─────
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 *
 * SimulationSystem define el contrato para sistemas que procesan datos
 * de simulación mediante operaciones batch sobre arrays primitivos densos.
 *
 * ── DIFERENCIA CON EngineSystem ──────────────────────────────────────────
 *
 *   EngineSystem (existente):
 *     void update(List<GameObjects> objects)
 *     → Opera sobre lista de objetos OO
 *     → Llama methods virtuales (object.update())
 *     → Indirección, allocations, pointer chasing
 *
 *   SimulationSystem (nuevo):
 *     void update(EntityStore store, double deltaTime)
 *     → Opera sobre EntityStore (acceso directo a arrays SoA)
 *     → Procesa datos mediante loops densos
 *     → Cache-friendly, allocation-free, vectorizable
 *
 * ── SEPARACIÓN DE RESPONSABILIDADES ──────────────────────────────────────
 *
 * SimulationSystem procesa DATOS DE SIMULACIÓN (position, velocity, etc.)
 * EngineSystem procesa OBJETOS DE DOMINIO (GameObjects y sus Components)
 *
 * Ambos pueden coexistir. Un frame típico puede ejecutar:
 *   1. object.update() para cada GameObject (lógica de dominio)
 *   2. SimulationPipeline.update() (datos de simulación DOD)
 *   3. EngineSystem.update() (render, audio, etc.)
 *
 * ── ACCESO A DATOS ───────────────────────────────────────────────────────
 *
 * Los sistemas acceden directamente a los arrays del EntityStore:
 *
 *   @Override
 *   public void update(EntityStore store, double deltaTime) {
 *       PrimitiveStorage s = store.getStorage();
 *       float[] posX = s.positionsX();
 *       float[] posY = s.positionsY();
 *       float[] velX = s.velocitiesX();
 *       float[] velY = s.velocitiesY();
 *
 *       int count = store.count();
 *
 *       for (int i = 0; i < count; i++) {
 *           // Validar que entidad tiene los componentes requeridos
 *           EntityId id = store.getEntityAt(i);
 *           if (id == null) continue;
 *           EntityRecord rec = store.getRecord(id);
 *           if (!rec.mask().matches(requirements)) continue;
 *
 *           // Procesar datos
 *           posX[i] += velX[i] * deltaTime;
 *           posY[i] += velY[i] * deltaTime;
 *       }
 *   }
 *
 * ── ALLOCATION-FREE HOT PATH ─────────────────────────────────────────────
 *
 * Los sistemas deben evitar allocations durante update():
 *
 *   ✗ NO crear: new Vector2D(), new ArrayList<>(), Stream<>, lambda
 *   ✓ SÍ usar: arrays primitivos, variables locales, primitives
 *
 * ── DETERMINISMO ─────────────────────────────────────────────────────────
 *
 * Los sistemas deben ser deterministas — mismo input → mismo output.
 * No depender de:
 *   - System.currentTimeMillis()
 *   - Random sin seed
 *   - Orden no garantizado (HashSet, HashMap iteration sin orden)
 *   - Estado global mutable compartido
 *
 * ── STATELESS ────────────────────────────────────────────────────────────
 *
 * Los sistemas idealmente no tienen estado mutable entre frames.
 * Todo el estado vive en el EntityStore.
 *
 * Si un sistema necesita estado temporal (ej: spatial hash, buffers),
 * debe documentarlo claramente y manejarlo de forma explícita.
 *
 * ── EJEMPLO: MovementSystem ──────────────────────────────────────────────
 *
 *   public class MovementSystem implements SimulationSystem {
 *
 *       private final ComponentMask requirements = ComponentMask.EMPTY
 *           .with(ComponentType.POSITION.id())
 *           .with(ComponentType.VELOCITY.id());
 *
 *       @Override
 *       public void update(EntityStore store, double deltaTime) {
 *           PrimitiveStorage s = store.getStorage();
 *           float[] posX = s.positionsX();
 *           float[] posY = s.positionsY();
 *           float[] velX = s.velocitiesX();
 *           float[] velY = s.velocitiesY();
 *
 *           for (int i = 0; i < store.count(); i++) {
 *               EntityId id = store.getEntityAt(i);
 *               if (id == null) continue;
 *
 *               EntityRecord rec = store.getRecord(id);
 *               if (!rec.mask().matches(requirements)) continue;
 *
 *               posX[i] += velX[i] * deltaTime;
 *               posY[i] += velY[i] * deltaTime;
 *           }
 *       }
 *   }
 *
 * ── EXTENSIBILIDAD ───────────────────────────────────────────────────────
 *
 * Nuevos sistemas se registran en el SimulationPipeline:
 *
 *   pipeline.register(new MovementSystem());
 *   pipeline.register(new AccelerationSystem());
 *   pipeline.register(new LifetimeSystem());
 *
 * El orden de registro determina el orden de ejecución.
 */
public interface SimulationSystem {

    /**
     * Actualiza este sistema para el frame actual.
     *
     * El sistema procesa los datos de simulación del EntityStore
     * y aplica su lógica sobre los arrays primitivos.
     *
     * ── CONTRATO TEMPORAL ─────────────────────────────────────────────────
     *
     * deltaTime representa los SEGUNDOS REALES transcurridos desde el último
     * simulation step. No es un valor fijo (1/60), sino variable según el
     * framerate real.
     *
     * Todos los cálculos temporales deben usar deltaTime:
     *   position += velocity * deltaTime
     *   velocity += acceleration * deltaTime
     *   lifetime -= deltaTime
     *
     * PROHIBIDO:
     *   - Recalcular deltaTime con System.nanoTime()
     *   - Usar constantes hardcoded (1/60, 0.016, etc.)
     *   - Asumir 60 FPS
     *
     * ── MUTABILIDAD ───────────────────────────────────────────────────────
     *
     * Los sistemas PUEDEN mutar los arrays del EntityStore directamente.
     * Esa es su responsabilidad — aplicar transformaciones sobre los datos.
     *
     * Los sistemas NO DEBEN:
     *   - Crear/destruir entidades durante update (hacer en batch después)
     *   - Modificar el EntityStore structure (count, capacity, records)
     *   - Llamar a otros sistemas directamente
     *
     * ── VALIDACIÓN DE COMPONENTES ────────────────────────────────────────
     *
     * Cada sistema debe validar que las entidades tienen los componentes
     * requeridos antes de acceder a sus datos:
     *
     *   if (!record.mask().matches(requirements)) continue;
     *
     * Acceder a un componente que la entidad no tiene resulta en datos
     * basura (el array existe pero el valor no está inicializado).
     *
     * @param store EntityStore que contiene los datos de simulación
     * @param deltaTime tiempo del simulation step en segundos
     */
    void update(EntityStore store, double deltaTime);

    /**
     * Retorna el nombre descriptivo de este sistema.
     * Usado para profiling y debugging.
     *
     * @return nombre del sistema
     */
    default String name() {
        return getClass().getSimpleName();
    }
}
