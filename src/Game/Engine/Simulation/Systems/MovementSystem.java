package Game.Engine.Simulation.Systems;

import Game.Engine.Simulation.ComponentMask;
import Game.Engine.Simulation.ComponentType;
import Game.Engine.Simulation.EntityId;
import Game.Engine.Simulation.Storage.EntityRecord;
import Game.Engine.Simulation.Storage.EntityStore;
import Game.Engine.Simulation.Storage.PrimitiveStorage;

/**
 * Sistema que integra velocidad en posición para todas las entidades con movimiento.
 *
 * ── HRFC — Game.Engine Unified Simulation Data Architecture / ECS-DOD ─────
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 *
 * MovementSystem aplica la ecuación básica de movimiento:
 *
 *   position += velocity * deltaTime
 *
 * Opera sobre todas las entidades que tienen Position + Velocity.
 *
 * ── ECUACIÓN ──────────────────────────────────────────────────────────────
 *
 *   x(t + Δt) = x(t) + vx(t) × Δt
 *   y(t + Δt) = y(t) + vy(t) × Δt
 *
 * Donde:
 *   x, y  = posición actual
 *   vx, vy = velocidad actual
 *   Δt = deltaTime en segundos
 *
 * ── REQUISITOS DE COMPONENTES ────────────────────────────────────────────
 *
 * Requiere:
 *   - POSITION (x, y)
 *   - VELOCITY (vx, vy)
 *
 * Ignora entidades que no tienen ambos componentes.
 *
 * ── ORDEN EN PIPELINE ────────────────────────────────────────────────────
 *
 * MovementSystem típicamente se ejecuta DESPUÉS de:
 *   - AccelerationSystem (velocity += acceleration * dt)
 *   - PhysicsSystem (aplicar fuerzas externas)
 *
 * Y ANTES de:
 *   - CollisionSystem (necesita posiciones actualizadas)
 *   - SpatialSystem (actualizar spatial hash)
 *
 * ── PERFORMANCE ──────────────────────────────────────────────────────────
 *
 * Este sistema es extremadamente simple y rápido:
 *   - Acceso secuencial a 4 arrays (posX, posY, velX, velY)
 *   - 4 lecturas + 2 escrituras por entidad
 *   - 4 operaciones aritméticas (2 multiplicaciones, 2 sumas)
 *   - Altamente vectorizable (compilador/JIT puede usar SIMD)
 *   - Cache-friendly (acceso secuencial, sin indirecciones)
 *
 * Esperado: ~1-2 nanosegundos por entidad en hardware moderno.
 * 10,000 entidades: ~10-20 microsegundos total.
 *
 * ── ALLOCATION-FREE ──────────────────────────────────────────────────────
 *
 * Este sistema no genera ninguna allocation:
 *   - No crea objetos temporales
 *   - No usa boxing
 *   - No usa streams ni lambdas
 *   - Solo primitives y referencias a arrays
 *
 * ── DETERMINISMO ─────────────────────────────────────────────────────────
 *
 * El sistema es completamente determinista:
 *   - Mismo input (posición, velocidad, deltaTime) → mismo output
 *   - No hay randomización
 *   - No hay dependencia de estado global
 *   - Procesamiento independiente por entidad
 *
 * ── EJEMPLO ──────────────────────────────────────────────────────────────
 *
 *   // Setup
 *   EntityStore store = new EntityStore();
 *   SimulationPipeline pipeline = new SimulationPipeline(store);
 *   pipeline.register(new MovementSystem());
 *
 *   // Crear entidad con posición y velocidad
 *   ComponentMask mask = ComponentMask.EMPTY
 *       .with(ComponentType.POSITION.id())
 *       .with(ComponentType.VELOCITY.id());
 *   EntityId id = store.create(mask);
 *
 *   SimulationHandle h = store.getHandle(id);
 *   PrimitiveStorage s = store.getStorage();
 *   s.positionsX()[h.index()] = 100f;
 *   s.positionsY()[h.index()] = 200f;
 *   s.velocitiesX()[h.index()] = 50f;  // 50 px/s
 *   s.velocitiesY()[h.index()] = -30f; // -30 px/s
 *
 *   // Update (deltaTime = 1/60 segundo)
 *   pipeline.update(1.0 / 60.0);
 *
 *   // Resultado:
 *   // posX = 100 + 50 * (1/60) = 100.833
 *   // posY = 200 - 30 * (1/60) = 199.5
 */
public final class MovementSystem implements SimulationSystem {

    private final ComponentMask requirements;

    public MovementSystem() {
        this.requirements = ComponentMask.EMPTY
            .with(ComponentType.POSITION.id())
            .with(ComponentType.VELOCITY.id());
    }

    @Override
    public void update(EntityStore store, double deltaTime) {
        PrimitiveStorage storage = store.getStorage();

        // Acceso directo a los arrays
        float[] posX = storage.positionsX();
        float[] posY = storage.positionsY();
        float[] velX = storage.velocitiesX();
        float[] velY = storage.velocitiesY();

        int count = store.count();

        // Loop denso allocation-free
        for (int i = 0; i < count; i++) {
            // Validar que entidad tiene los componentes requeridos
            EntityId entityId = store.getEntityAt(i);
            if (entityId == null) continue;

            EntityRecord record = store.getRecord(entityId);
            if (!record.mask().matches(requirements)) continue;

            // Integrar velocidad en posición
            // position += velocity * deltaTime
            posX[i] += velX[i] * deltaTime;
            posY[i] += velY[i] * deltaTime;
        }
    }

    @Override
    public String name() {
        return "MovementSystem";
    }
}
