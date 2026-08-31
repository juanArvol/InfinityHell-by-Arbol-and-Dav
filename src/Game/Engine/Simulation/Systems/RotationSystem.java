package Game.Engine.Simulation.Systems;

import Game.Engine.Simulation.ComponentMask;
import Game.Engine.Simulation.ComponentType;
import Game.Engine.Simulation.Storage.EntityStore;
import Game.Engine.Simulation.Storage.PrimitiveStorage;

/**
 * Sistema que actualiza rotación visual de entidades.
 *
 * ── HRFC — Projectile DOD Migration ──────────────────────────────────────
 *
 * ── RESPONSABILIDAD ──────────────────────────────────────────────────────
 *
 * Incrementa rotation basándose en velocidad angular configurada externamente.
 * NO calcula velocidad angular — eso es responsabilidad del behavior.
 *
 * Para bullets que giran visualmente (drills, spinning projectiles, saws):
 *   - El behavior configura una velocidad angular constante o variable
 *   - Este sistema integra: rotation += angularVelocity * deltaTime
 *
 * ── COMPONENTES REQUERIDOS ───────────────────────────────────────────────
 *
 * Este sistema requiere:
 *   - ROTATION (write) — ángulo visual actual
 *   - ANGULAR_VELOCITY (read) — velocidad angular en rad/s
 *
 * OPTIMIZACIÓN ACTUAL:
 *   Como solo ALGUNAS bullets tienen rotación activa, filtramos entidades
 *   que tienen angularVelocity != 0.
 *
 * FUTURO:
 *   Considerar ComponentType.ROTATION flag para marcar entidades con
 *   rotación activa y evitar iterar sobre todas.
 *
 * ── USO TÍPICO ───────────────────────────────────────────────────────────
 *
 * Desde BulletBehavior.onAttached():
 *
 *   // DrillBullet — gira a 360° por segundo
 *   int index = bullet.getEntityIndex();
 *   storage.angularVelocities()[index] = (float) Math.toRadians(360.0);
 *
 * Desde ProjectileMovement.tick():
 *
 *   // Spinning saw — velocidad angular aumenta con tiempo
 *   double angularVel = Math.toRadians(180.0 + elapsed * 60.0);
 *   storage.angularVelocities()[index] = (float) angularVel;
 *
 * ── RENDERING ────────────────────────────────────────────────────────────
 *
 * El renderer debe usar storage.rotations()[index] para aplicar rotación visual:
 *
 *   float rotation = storage.rotations()[index];
 *   g2d.rotate(rotation, centerX, centerY);
 *   g2d.drawImage(sprite, x, y, null);
 *
 * ── NOTA: ROTACIÓN VISUAL vs DIRECCIÓN DE VELOCITY ──────────────────────
 *
 * rotation NO afecta la dirección de movimiento.
 * Es puramente visual — para efectos de renderizado.
 *
 * Para proyectiles que "apuntan" en dirección de movimiento, calcular
 * ángulo desde velocity en el renderer, NO usar rotation.
 */
public final class RotationSystem implements SimulationSystem {

    /** Máscara de componentes requeridos por este sistema */
    private static final ComponentMask REQUIRED_COMPONENTS = ComponentMask.EMPTY
        .with(ComponentType.ROTATION)
        .with(ComponentType.ANGULAR_VELOCITY);

    @Override
    public void update(EntityStore entityStore, double deltaTime) {
        PrimitiveStorage storage = entityStore.getStorage();
        int count = entityStore.count();

        float[] rotations = storage.rotations();
        float[] angularVelocities = storage.angularVelocities();

        float dt = (float) deltaTime;

        // Integrar rotation
        // OPTIMIZACIÓN: Solo procesar entidades con angularVelocity != 0
        for (int i = 0; i < count; i++) {
            float angularVel = angularVelocities[i];
            if (angularVel != 0f) {
                rotations[i] += angularVel * dt;
                
                // Normalizar a rango [0, 2π) para evitar overflow
                float rotation = rotations[i];
                if (rotation > Math.PI * 2.0f) {
                    rotations[i] = rotation - (float) (Math.PI * 2.0);
                } else if (rotation < 0f) {
                    rotations[i] = rotation + (float) (Math.PI * 2.0);
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
        return "RotationSystem";
    }
}
