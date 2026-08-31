package Game.Engine.Simulation.Systems;

import Game.Engine.Simulation.ComponentMask;
import Game.Engine.Simulation.ComponentType;
import Game.Engine.Simulation.Storage.EntityStore;
import Game.Engine.Simulation.Storage.PrimitiveStorage;

/**
 * Sistema que decrece lifetime y marca entidades expiradas.
 *
 * ── HRFC — Projectile DOD Migration ──────────────────────────────────────
 *
 * ── RESPONSABILIDAD ──────────────────────────────────────────────────────
 *
 * Decrece lifetime en deltaTime y marca FLAG_EXPIRED cuando agota el tiempo:
 *
 *   lifetime -= deltaTime
 *   if (lifetime <= 0) flags |= FLAG_EXPIRED
 *
 * ── COMPONENTES REQUERIDOS ───────────────────────────────────────────────
 *
 * Este sistema requiere:
 *   - LIFETIME (write)
 *   - FLAGS (write)
 *
 * OPTIMIZACIÓN ACTUAL:
 *   Como todas las bullets tienen PROJECTILE_MASK (que incluye estos componentes),
 *   no filtramos en runtime. Procesamos todas las entidades del store.
 *
 * FUTURO:
 *   Si el EntityStore contiene tipos mixtos (bullets + particles + enemies),
 *   activar filtrado por ComponentMask para procesar solo entidades válidas.
 *
 * ── ÚNICO WRITER DE LIFETIME ─────────────────────────────────────────────
 *
 * LifetimeSystem es el ÚNICO responsable de decrecer lifetime.
 *
 * PROHIBIDO:
 * - BulletLife.advance() ya NO decrece lifetime
 * - Bullet.update() NO toca lifetime
 * - BulletBehavior NO decrece lifetime
 *
 * PERMITIDO:
 * - BulletLife.kill() escribe 0 directamente (muerte manual)
 * - BulletLife.extend() añade tiempo (extensión manual)
 *
 * ── FLAGS ────────────────────────────────────────────────────────────────
 *
 * FLAG_EXPIRED (bit 1) se marca cuando lifetime <= 0.
 * ProjectileBehaviorSystem lee este flag y llama behavior.onExpire().
 *
 * FLAG_DEAD (bit 0) se marca por kill() manual (comportamiento, colisión).
 *
 * isAlive() retorna true si:
 *   - lifetime > 0
 *   - FLAG_DEAD no está activo
 *
 * ── ORDEN DE EJECUCIÓN ───────────────────────────────────────────────────
 *
 * Puede ejecutarse en cualquier momento. Típicamente después de Movement:
 *   1. ProjectileMovementSystem
 *   2. AccelerationSystem
 *   3. MovementSystem
 *   4. LifetimeSystem (ESTE)
 *   5. CollisionSystem
 *   6. ProjectileBehaviorSystem (lee FLAG_EXPIRED)
 *
 * ── HOT PATH ─────────────────────────────────────────────────────────────
 *
 * Loop denso sobre arrays primitivos.
 * Branch prediction favorable — mayoría de entidades siguen vivas.
 */
public final class LifetimeSystem implements SimulationSystem {

    private static final int FLAG_EXPIRED = 1 << 1;
    
    /** Máscara de componentes requeridos por este sistema */
    private static final ComponentMask REQUIRED_COMPONENTS = ComponentMask.EMPTY
        .with(ComponentType.LIFETIME)
        .with(ComponentType.FLAGS);

    @Override
    public void update(EntityStore entityStore, double deltaTime) {
        PrimitiveStorage storage = entityStore.getStorage();
        int count = entityStore.count();

        float[] lifetimes = storage.lifetimes();
        int[] flags = storage.flags();

        float dt = (float) deltaTime;

        // HOT LOOP — procesamiento denso
        // OPTIMIZACIÓN: No filtramos porque todas las bullets tienen estos componentes
        // Si en futuro el store contiene tipos mixtos, activar filtrado aquí
        for (int i = 0; i < count; i++) {
            if (lifetimes[i] > 0f) {
                lifetimes[i] -= dt;
                if (lifetimes[i] <= 0f) {
                    lifetimes[i] = 0f; // clampeo
                    flags[i] |= FLAG_EXPIRED; // marcar para callback
                }
            }
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
        return "LifetimeSystem";
    }
}
