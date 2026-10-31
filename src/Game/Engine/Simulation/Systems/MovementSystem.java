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
 * ── CRÍTICO: SOLO PROCESA BULLETS, NO ENEMIES ───────────────────────────
 *
 * MovementSystem procesa SOLO entidades con PROJECTILE_MASK.
 * Enemies (con ENEMY_MASK) NO deben procesarse aquí porque:
 *   - CollisionsSystem mueve enemies con swept AABB
 *   - Enemies necesitan resolución de colisiones física
 *   - Position de enemies vive en Transform OOP, no en DOD
 *
 * Si enemies se procesan aquí, se mueven en DOD storage pero CollisionsSystem
 * lee del Transform OOP, causando desincronización.
 *
 * ── ÚNICO WRITER DE POSITION (PARA BULLETS) ─────────────────────────────
 *
 * MovementSystem es el ÚNICO responsable de modificar position DE BULLETS.
 *
 * PROHIBIDO (para bullets):
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
    
    /** Máscara para EXCLUIR enemies - solo procesar bullets */
    private static final ComponentMask PROJECTILE_ONLY = ComponentType.PROJECTILE_MASK;

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
        // SOLO para bullets (PROJECTILE_MASK), NO enemies
        for (int i = 0; i < count; i++) {
            var entityId = entityStore.getEntityAt(i);
            if (entityId == null) continue;
            
            var record = entityStore.getRecord(entityId);
            if (record == null) continue;
            
            // FILTRAR: Skip enemies (ENEMY_MASK), solo procesar bullets (PROJECTILE_MASK)
            if (!record.mask().equals(PROJECTILE_ONLY)) {
                continue;
            }
            
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