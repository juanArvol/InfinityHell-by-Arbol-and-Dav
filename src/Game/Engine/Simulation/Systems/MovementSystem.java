package Game.Engine.Simulation.Systems;

import Game.Engine.Simulation.ComponentMask;
import Game.Engine.Simulation.ComponentType;
import Game.Engine.Simulation.Storage.EntityStore;
import Game.Engine.Simulation.Storage.PrimitiveStorage;

/**
 * Sistema que integra velocity en position.
 *
 * ── HRFC — Projectile DOD Migration ──────────────────────────────────────
 *
 * ── RESPONSABILIDAD ──────────────────────────────────────────────────────
 *
 * Aplica la ecuación cinemática básica:
 *
 *   position += velocity * deltaTime
 *
 * ── COMPONENTES REQUERIDOS ───────────────────────────────────────────────
 *
 * Este sistema requiere:
 *   - POSITION (write)
 *   - VELOCITY (read)
 *
 * OPTIMIZACIÓN ACTUAL:
 *   Como todas las bullets tienen PROJECTILE_MASK (que incluye estos componentes),
 *   no filtramos en runtime. Procesamos todas las entidades del store.
 *
 * FUTURO:
 *   Si el EntityStore contiene tipos mixtos (bullets + particles + enemies),
 *   activar filtrado por ComponentMask para procesar solo entidades válidas.
 *
 * ── ÚNICO WRITER DE POSITION ─────────────────────────────────────────────
 *
 * MovementSystem es el ÚNICO responsable de modificar position.
 *
 * PROHIBIDO:
 * - Bullet.moveByPhysics() NO se llama
 * - ProjectileMovement NO modifica position
 * - BulletBehavior NO modifica position
 *
 * PERMITIDO:
 * - CollisionSystem puede ajustar position para resolver overlap
 *
 * ── ORDEN DE EJECUCIÓN ───────────────────────────────────────────────────
 *
 * DEBE ejecutarse DESPUÉS de AccelerationSystem que actualiza velocity.
 *
 * Orden correcto:
 *   1. ProjectileMovementSystem
 *   2. AccelerationSystem
 *   3. MovementSystem (ESTE)
 *   4. CollisionSystem
 *
 * ── HOT PATH ─────────────────────────────────────────────────────────────
 *
 * Loop denso sobre arrays primitivos.
 * Sin indirecciones, sin allocations, cache-friendly.
 * Compilador puede vectorizar automáticamente (SIMD).
 *
 * Para 3600 projectiles:
 *   ANTES: 3600 × Bullet.moveByPhysics() con indirecciones
 *   AHORA: un loop sobre arrays secuenciales
 */
public final class MovementSystem implements SimulationSystem {

    /** Máscara de componentes requeridos por este sistema */
    private static final ComponentMask REQUIRED_COMPONENTS = ComponentMask.EMPTY
        .with(ComponentType.POSITION)
        .with(ComponentType.VELOCITY);

    @Override
    public void update(EntityStore entityStore, double deltaTime) {
        PrimitiveStorage storage = entityStore.getStorage();
        int count = entityStore.count();

        float[] posX = storage.positionsX();
        float[] posY = storage.positionsY();
        float[] velX = storage.velocitiesX();
        float[] velY = storage.velocitiesY();

        float dt = (float) deltaTime;

        // HOT LOOP — procesamiento denso, SIMD-friendly
        // OPTIMIZACIÓN: No filtramos porque todas las bullets tienen estos componentes
        // Si en futuro el store contiene tipos mixtos, activar filtrado aquí
        for (int i = 0; i < count; i++) {
            posX[i] += velX[i] * dt;
            posY[i] += velY[i] * dt;
        }
    }
    
    /**
     * Retorna los componentes requeridos por este sistema.
     * Usado para validación y debugging.
     */
    public ComponentMask getRequiredComponents() {
        return REQUIRED_COMPONENTS;
    }

    @Override
    public String name() {
        return "MovementSystem";
    }
}
