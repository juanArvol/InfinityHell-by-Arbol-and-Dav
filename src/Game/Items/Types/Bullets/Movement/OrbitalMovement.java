package Game.Items.Types.Bullets.Movement;

import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Engine.GameObjects;
import Game.Items.Types.Bullets.Bullet;
import Game.Items.Types.Bullets.ProjectileMovement;
import java.util.function.Supplier;

/**
 * Movimiento orbital — el proyectil orbita alrededor de un centro dinámico.
 *
 * El proyectil describe un círculo (o elipse) alrededor del objeto central,
 * a una distancia constante y con velocidad angular configurable.
 *
 * Casos de uso:
 *   - Escudos orbitales del jugador
 *   - Satélites de ataque de un jefe
 *   - Orbes que rodean a una invocación
 *   - Patrones de bullet-hell con anillos rotativos
 *
 * Uso:
 *   // 3 proyectiles orbitando al jugador a 60px, separados 120°:
 *   for (int i = 0; i < 3; i++) {
 *       double phase = Math.toRadians(i * 120.0);
 *       ProjectileMovement m = new OrbitalMovement(player::getTransform, 60.0, 3.0, phase);
 *       bullets.add(factory.create(m));
 *   }
 */
public final class OrbitalMovement implements ProjectileMovement {

    private final Supplier<GameObjects> centerSupplier;
    private final double radius;
    private final double angularSpeedDeg;
    private double angle; // ángulo actual en radianes

    /**
     * @param centerSupplier proveedor del objeto central a orbitar
     * @param radius         radio de la órbita en unidades
     * @param angularSpeedDeg velocidad angular en grados/frame (positivo = antihorario)
     * @param initialAngleDeg ángulo inicial en grados
     */
    public OrbitalMovement(Supplier<GameObjects> centerSupplier,
                           double radius,
                           double angularSpeedDeg,
                           double initialAngleDeg) {
        this.centerSupplier  = centerSupplier;
        this.radius          = radius;
        this.angularSpeedDeg = Math.toRadians(angularSpeedDeg);
        this.angle           = Math.toRadians(initialAngleDeg);
    }

    @Override
    public void tick(Bullet bullet) {
        GameObjects center = centerSupplier.get();
        if (center == null) return;

        angle += angularSpeedDeg;

        Vector2D centerPos = center.getTransform().getPosition();
        double targetX = centerPos.getX() + Math.cos(angle) * radius;
        double targetY = centerPos.getY() + Math.sin(angle) * radius;

        // Calcular velocidad necesaria para llegar a la posición orbital
        // (posicionamiento directo — el bullet se "teletransporta" suavemente
        // al punto de la órbita, lo que simula movimiento orbital preciso)
        Vector2D pos = bullet.getTransform().getPosition();
        bullet.getPhysics().setXspeed(targetX - pos.getX());
        bullet.getPhysics().setYspeed(targetY - pos.getY());
    }
}
