package Game.Engine.Simulation.Systems;

import Game.Engine.Simulation.ComponentMask;
import Game.Engine.Simulation.ComponentType;
import Game.Engine.Simulation.Storage.EntityStore;
import Game.Engine.Simulation.Storage.PrimitiveStorage;

/**
 * Sistema que integra acceleration en velocity.
 *
 * ── HRFC — Projectile DOD Migration ──────────────────────────────────────
 *
 * ── RESPONSABILIDAD ──────────────────────────────────────────────────────
 *
 * Aplica la ecuación cinemática básica:
 *
 *   velocity += acceleration * deltaTime
 *
 * También aplica gravedad global escalada por gravityScale:
 *
 *   velocityY += (accelerationY + GRAVITY * gravityScale) * deltaTime
 *
 * ── COMPONENTES REQUERIDOS ───────────────────────────────────────────────
 *
 * Este sistema requiere:
 *   - VELOCITY (write)
 *   - ACCELERATION (read)
 *   - GRAVITY_SCALE (read)
 *
 * OPTIMIZACIÓN ACTUAL:
 *   Como todas las bullets tienen PROJECTILE_MASK (que incluye estos componentes),
 *   no filtramos en runtime. Procesamos todas las entidades del store.
 *
 * FUTURO:
 *   Si el EntityStore contiene tipos mixtos (bullets + particles + enemies),
 *   activar filtrado por ComponentMask para procesar solo entidades válidas.
 *
 * ── ORDEN DE EJECUCIÓN ───────────────────────────────────────────────────
 *
 * DEBE ejecutarse DESPUÉS de que ProjectileMovement configure acceleration,
 * pero ANTES de MovementSystem que integra position.
 *
 * Orden correcto:
 *   1. ProjectileMovementSystem (behaviors configuran acceleration)
 *   2. AccelerationSystem (ESTE)
 *   3. MovementSystem
 *
 * ── GRAVEDAD ─────────────────────────────────────────────────────────────
 *
 * GRAVITY es la aceleración gravitatoria global (ej: 0.4 = caída lenta, 9.8 = realista).
 * gravityScale[i] es el multiplicador por entidad:
 *   - 0.0 = sin gravedad (proyectiles normales)
 *   - 1.0 = gravedad estándar
 *   - 2.0 = doble gravedad (MetheorBullet)
 *
 * ── HOT PATH ─────────────────────────────────────────────────────────────
 *
 * Loop denso sobre arrays primitivos.
 * Sin indirecciones, sin allocations, cache-friendly.
 * Compilador puede vectorizar automáticamente.
 */
public final class AccelerationSystem implements SimulationSystem {

    /** Gravedad global en units/s² (ej: 0.4 = caída lenta, 9.8 = realista) */
    private static final float GRAVITY = 0.4f;
    
    /** Máscara de componentes requeridos por este sistema */
    private static final ComponentMask REQUIRED_COMPONENTS = ComponentMask.EMPTY
        .with(ComponentType.VELOCITY)
        .with(ComponentType.ACCELERATION)
        .with(ComponentType.GRAVITY_SCALE);

    @Override
    public void update(EntityStore entityStore, double deltaTime) {
        PrimitiveStorage storage = entityStore.getStorage();
        int count = entityStore.count();

        float[] velX = storage.velocitiesX();
        float[] velY = storage.velocitiesY();
        float[] accX = storage.accelerationsX();
        float[] accY = storage.accelerationsY();
        float[] gravityScale = storage.gravityScale();

        float dt = (float) deltaTime;

        // HOT LOOP — procesamiento denso
        // OPTIMIZACIÓN: No filtramos porque todas las bullets tienen estos componentes
        // Si en futuro el store contiene tipos mixtos, activar filtrado aquí
        for (int i = 0; i < count; i++) {
            velX[i] += accX[i] * dt;
            velY[i] += (accY[i] + GRAVITY * gravityScale[i]) * dt;
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
        return "AccelerationSystem";
    }
}
