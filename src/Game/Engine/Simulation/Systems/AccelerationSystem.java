package Game.Engine.Simulation.Systems;

import Game.Engine.Simulation.ComponentMask;
import Game.Engine.Simulation.ComponentType;
import Game.Engine.Simulation.EntityId;
import Game.Engine.Simulation.Storage.EntityRecord;
import Game.Engine.Simulation.Storage.EntityStore;
import Game.Engine.Simulation.Storage.PrimitiveStorage;

/**
 * Sistema que integra aceleración en velocidad para todas las entidades con física.
 *
 * ── HRFC — Game.Engine Unified Simulation Data Architecture / ECS-DOD ─────
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 *
 * AccelerationSystem aplica la ecuación básica de aceleración:
 *
 *   velocity += acceleration * deltaTime
 *
 * Opera sobre todas las entidades que tienen Velocity + Acceleration.
 *
 * ── ECUACIÓN ──────────────────────────────────────────────────────────────
 *
 *   vx(t + Δt) = vx(t) + ax(t) × Δt
 *   vy(t + Δt) = vy(t) + ay(t) × Δt
 *
 * Donde:
 *   vx, vy = velocidad actual
 *   ax, ay = aceleración actual
 *   Δt = deltaTime en segundos
 *
 * ── REQUISITOS DE COMPONENTES ────────────────────────────────────────────
 *
 * Requiere:
 *   - VELOCITY (vx, vy)
 *   - ACCELERATION (ax, ay)
 *
 * Ignora entidades que no tienen ambos componentes.
 *
 * ── ORDEN EN PIPELINE ────────────────────────────────────────────────────
 *
 * AccelerationSystem DEBE ejecutarse ANTES de MovementSystem:
 *
 *   1. AccelerationSystem → velocity += acceleration * dt
 *   2. MovementSystem     → position += velocity * dt
 *
 * Si se invierte el orden, la aceleración se aplicaría un frame tarde.
 *
 * ── USO TÍPICO ───────────────────────────────────────────────────────────
 *
 * Aceleración se usa para:
 *   - Gravedad (aceleración constante hacia abajo)
 *   - Fuerzas continuas (viento, corrientes)
 *   - Propulsión (cohetes, jetpacks)
 *   - Trayectorias curvas (proyectiles parabólicos)
 *
 * Nota: Para fuerzas instantáneas (explosiones, knockback), modificar
 * velocity directamente en lugar de usar acceleration.
 *
 * ── FÍSICA ───────────────────────────────────────────────────────────────
 *
 * Este sistema implementa integración de Euler explícita:
 *
 *   v(t + Δt) = v(t) + a(t) × Δt
 *
 * Es un integrador de primer orden:
 *   - Simple y rápido
 *   - Suficiente para gameplay
 *   - Error O(Δt²)
 *
 * Para simulación física más precisa, considerar integradores de orden
 * superior (Verlet, RK4), pero eso está fuera del alcance de este HRFC.
 *
 * ── PERFORMANCE ──────────────────────────────────────────────────────────
 *
 * Similar a MovementSystem:
 *   - Acceso secuencial a 4 arrays (velX, velY, accX, accY)
 *   - 4 lecturas + 2 escrituras por entidad
 *   - 4 operaciones aritméticas (2 multiplicaciones, 2 sumas)
 *   - Altamente vectorizable
 *   - Cache-friendly
 *
 * ── ALLOCATION-FREE ──────────────────────────────────────────────────────
 *
 * Sin allocations, sin boxing, sin indirecciones.
 *
 * ── EJEMPLO: GRAVEDAD ────────────────────────────────────────────────────
 *
 *   // Crear entidad con gravedad
 *   ComponentMask mask = ComponentMask.EMPTY
 *       .with(ComponentType.VELOCITY.id())
 *       .with(ComponentType.ACCELERATION.id());
 *   EntityId id = store.create(mask);
 *
 *   SimulationHandle h = store.getHandle(id);
 *   PrimitiveStorage s = store.getStorage();
 *
 *   // Gravedad: 980 px/s² hacia abajo (eje Y positivo = abajo)
 *   s.accelerationsX()[h.index()] = 0f;
 *   s.accelerationsY()[h.index()] = 980f;
 *
 *   // Velocidad inicial
 *   s.velocitiesX()[h.index()] = 100f;
 *   s.velocitiesY()[h.index()] = -500f; // lanzamiento hacia arriba
 *
 *   // Después de 1 segundo:
 *   // vy = -500 + 980 * 1.0 = 480 px/s (cayendo)
 */
public final class AccelerationSystem implements SimulationSystem {

    private final ComponentMask requirements;

    public AccelerationSystem() {
        this.requirements = ComponentMask.EMPTY
            .with(ComponentType.VELOCITY.id())
            .with(ComponentType.ACCELERATION.id());
    }

    @Override
    public void update(EntityStore store, double deltaTime) {
        PrimitiveStorage storage = store.getStorage();

        // Acceso directo a los arrays
        float[] velX = storage.velocitiesX();
        float[] velY = storage.velocitiesY();
        float[] accX = storage.accelerationsX();
        float[] accY = storage.accelerationsY();

        int count = store.count();

        // Loop denso allocation-free
        for (int i = 0; i < count; i++) {
            // Validar que entidad tiene los componentes requeridos
            EntityId entityId = store.getEntityAt(i);
            if (entityId == null) continue;

            EntityRecord record = store.getRecord(entityId);
            if (!record.mask().matches(requirements)) continue;

            // Integrar aceleración en velocidad
            // velocity += acceleration * deltaTime
            velX[i] += accX[i] * deltaTime;
            velY[i] += accY[i] * deltaTime;
        }
    }

    @Override
    public String name() {
        return "AccelerationSystem";
    }
}
