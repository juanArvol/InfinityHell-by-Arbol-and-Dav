package Game.Items.Types.Bullets.Movement;

import Game.Engine.AbstractEntity;
import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Items.Types.Bullets.Definition.Bullet;
import Game.Items.Types.Bullets.ResettableMovement;

/**
 * Movimiento orbital — el proyectil orbita alrededor de un centro dinámico.
 *
 * ── HRFC — Unified DeltaTime Migration ───────────────────────────────────
 * ── Mini-HRFC — Final Temporal Normalization ──────────────────────────────
 *
 * El proyectil describe un círculo alrededor del objeto central a una distancia
 * constante y con velocidad angular configurable.
 *
 * MIGRACIÓN TEMPORAL:
 *   La velocidad angular ahora se expresa en grados/segundo (deg/s) en lugar
 *   de grados/frame, garantizando independencia del framerate.
 *
 *   ANTES (frame-based @ 30 FPS):
 *     angularSpeed = 3.0 deg/frame → 90 deg/s @ 30 FPS, 180 deg/s @ 60 FPS
 *
 *   AHORA (time-based):
 *     angularSpeed = 90.0 deg/s → 90 deg/s independientemente del FPS
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
 *       double phase = i * 120.0;
 *       // 90 deg/s de velocidad angular
 *       ProjectileMovement m = new OrbitalMovement(player, 60.0, 90.0, phase);
 *       bullets.add(factory.create(m));
 *   }
 *
 * ── Referencia directa al centro ─────────────────────────────────────────
 *
 * OrbitalMovement mantiene una referencia directa a su AbstractEntity central.
 * Esta referencia es intencional: el proyectil fue creado para orbitar ese
 * objeto concreto, y su ciclo de vida está ligado a él.
 *
 * El tipo es AbstractEntity (no GameObjects) porque:
 *   - Solo entidades con lifecycle de vida son centros válidos de órbita.
 *   - AbstractEntity.isDead() permite detectar que el centro fue destruido.
 *   - GameObjects no posee lifecycle de vida y no debe adquirirlo.
 *
 * Si el centro muere, tick() lo detecta con isDead() y el proyectil congela
 * su posición (deja de actualizar velocidad) hasta expirar por lifeTime.
 * Esto evita orbitar alrededor de una posición inválida o reciclada.
 *
 * ── Ownership y ausencia de residuo ──────────────────────────────────────
 *
 * Cada disparo construye una nueva instancia de OrbitalMovement con su
 * propio center. Cuando ProjectilePool reutiliza una Bullet, resetState()
 * reemplaza this.movement con la nueva instancia del blueprint — la instancia
 * anterior (con el center del ciclo viejo) queda sin referencias y puede
 * ser recolectada normalmente.
 *
 * reset() solo resetea el ángulo al valor inicial. No necesita nullificar
 * center porque se llama sobre la instancia ya correcta del nuevo disparo,
 * inmediatamente después de que resetState() la asignó.
 *
 * ── Pool ──────────────────────────────────────────────────────────────────
 *
 * Implementa ResettableMovement: el ángulo puede resetearse al valor inicial.
 * isStateless() == false porque angle es un campo mutable por frame.
 */
public final class OrbitalMovement implements ResettableMovement {

    /**
     * Centro de la órbita.
     *
     * AbstractEntity porque solo entidades con lifecycle de vida son centros
     * válidos. null = sin centro (el proyectil no actualiza su velocidad).
     */
    private final AbstractEntity center;

    private final double radius;
    private final double angularSpeedRad;  // radianes/segundo (no por frame)
    private final double initialAngleRad;   // guardado para reset()
    private double angle;                   // ángulo actual en radianes — estado mutable

    /**
     * ── HRFC — Unified DeltaTime Migration ───────────────────────────────
     * ── Mini-HRFC — Final Temporal Normalization ──────────────────────────
     *
     * @param center           la entidad alrededor de la cual orbitar. Puede ser null.
     * @param radius           radio de la órbita en unidades
     * @param angularSpeedDeg  velocidad angular en grados/segundo (deg/s)
     * @param initialAngleDeg  ángulo inicial en grados
     */
    public OrbitalMovement(AbstractEntity center,
                           double radius,
                           double angularSpeedDeg,
                           double initialAngleDeg) {
        this.center          = center;
        this.radius          = radius;
        this.angularSpeedRad = Math.toRadians(angularSpeedDeg);
        this.initialAngleRad = Math.toRadians(initialAngleDeg);
        this.angle           = this.initialAngleRad;
    }

    /**
     * Factory method para compatibilidad con código legacy que usaba deg/frame.
     *
     * @param center                la entidad alrededor de la cual orbitar
     * @param radius                radio de la órbita en unidades
     * @param angularSpeedDegFrame  velocidad angular en grados/frame @ 30 FPS
     * @param initialAngleDeg       ángulo inicial en grados
     * @return OrbitalMovement configurado con velocidad angular en deg/s
     *
     * @deprecated Usar constructor con deg/s directamente
     */
    @Deprecated
    public static OrbitalMovement fromFrames(AbstractEntity center,
                                             double radius,
                                             double angularSpeedDegFrame,
                                             double initialAngleDeg) {
        // Convertir deg/frame @ 30 FPS → deg/s
        return new OrbitalMovement(center, radius, angularSpeedDegFrame * 30.0, initialAngleDeg);
    }

    @Override
    public void tick(Bullet bullet, double dt) {
        // Sin centro → no actualizar velocidad
        if (center == null) return;

        // Si el centro fue destruido, no orbitar alrededor de él.
        // isDead() está en AbstractEntity vía HealthComponent.
        // El proyectil mantiene su última velocidad y expirará por lifeTime.
        if (center.isDead()) return;

        // ── HRFC + Mini-HRFC: Integración temporal ───────────────────────
        // angle += angularSpeedRad × dt
        // angularSpeedRad está en rad/s, dt en segundos
        angle += angularSpeedRad * dt;

        Vector2D centerPos = center.getTransform().getPosition();
        double targetX = centerPos.getX() + Math.cos(angle) * radius;
        double targetY = centerPos.getY() + Math.sin(angle) * radius;

        // ── CORRECCIÓN HRFC-DT-007 + Orbital Velocity Semantics ──────────
        // PROBLEMA IDENTIFICADO:
        //   OrbitalMovement calculaba velocity como desplazamiento objetivo
        //   (targetX - pos.getX()), que es un desplazamiento en pixels/frame.
        //   Cuando CollisionsSystem multiplica por deltaTime, el desplazamiento
        //   resultante es mucho menor del esperado.
        //
        // SOLUCIÓN:
        //   Calcular velocity como desplazamiento por frame × 30 para convertir
        //   a units/s, coherente con el sistema temporal.
        //
        // INVARIANTE:
        //   velocity [units/s] = desplazamiento [pixels/frame @ 30 FPS] × 30
        //
        // SEMÁNTICA DE ÓRBITA:
        //   La bullet debe moverse desde su posición actual hasta targetX/Y
        //   en UN frame @ 30 FPS. Entonces:
        //     displacement_per_frame = targetX - pos.getX()
        //     velocity = displacement_per_frame × 30
        //
        Vector2D pos = bullet.getTransform().getPosition();
        double displacementX = targetX - pos.getX();
        double displacementY = targetY - pos.getY();
        
        bullet.getPhysics().setXspeed(displacementX * 30.0);
        bullet.getPhysics().setYspeed(displacementY * 30.0);
    }

    /**
     * OrbitalMovement tiene estado interno (angle mutable por frame).
     * No puede compartirse como singleton entre proyectiles en fases distintas.
     */
    @Override
    public boolean isStateless() {
        return false;
    }

    /**
     * Resetea el ángulo al valor inicial de construcción.
     *
     * Se llama desde Bullet.resetState() sobre la instancia ya asignada del
     * nuevo blueprint — en ese momento center ya corresponde al nuevo ciclo.
     * Solo es necesario resetear angle para que la órbita empiece desde
     * initialAngleDeg, igual que una instancia recién construida.
     */
    @Override
    public void reset() {
        this.angle = initialAngleRad;
    }
}
