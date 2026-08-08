package Game.Items.Types.Bullets.Movement;

import Game.Engine.AbstractEntity;
import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Items.Types.Bullets.Definition.Bullet;
import Game.Items.Types.Bullets.ProjectileMovement;

/**
 * Movimiento de seguimiento (homing) — el proyectil gira hacia un objetivo.
 *
 * En cada frame, calcula la dirección al objetivo y rota el vector de
 * velocidad actual hasta ese ángulo, limitado por turnSpeed. Esto produce
 * una curva suave en lugar de un giro abrupto.
 *
 * Casos de uso:
 *   - Misiles teledirigidos
 *   - Orbes de seguimiento
 *   - Proyectiles mágicos que persiguen al jugador
 *   - Invocaciones que buscan enemigos
 *
 * ── Referencia directa al objetivo ───────────────────────────────────────
 *
 * HomingMovement mantiene una referencia directa a su AbstractEntity objetivo.
 * Esta referencia es intencional y correcta: la bala fue disparada contra
 * ese objetivo concreto y su ciclo de vida está ligado a él.
 *
 * Una referencia directa NO es un memory leak mientras la Bullet esté viva.
 * La Bullet mantiene el HomingMovement, que mantiene el target. Cuando la
 * Bullet muere y sale del pool o del mundo, la cadena de referencias deja
 * de ser alcanzable y el GC puede recuperarla normalmente.
 *
 * El objetivo es AbstractEntity (no GameObjects) porque:
 *   - Solo entidades con lifecycle de vida (HealthComponent) son objetivos válidos.
 *   - AbstractEntity.isDead() refleja si el objetivo sigue vivo.
 *   - GameObjects no tiene lifecycle de vida y no debe adquirirlo.
 *
 * Si el objetivo muere antes que la bala, tick() detecta isDead() y deja
 * de girar hacia él — la bala continúa en línea recta hasta expirar.
 *
 * ── Ownership de la referencia ────────────────────────────────────────────
 *
 * La referencia pertenece al ciclo de vida de esta instancia de HomingMovement.
 * Dado que cada disparo construye un nuevo HomingMovement con su propio target,
 * no existe posibilidad de que el target de un ciclo anterior contamine el nuevo.
 *
 * ── isStateless() ────────────────────────────────────────────────────────
 *
 * HomingMovement no tiene campos numéricos que muten frame a frame.
 * isStateless() == true permite al pool reutilizar la Bullet que lo contiene.
 * Cuando el pool reutiliza una Bullet, el nuevo blueprint provee una nueva
 * instancia de HomingMovement con el nuevo target — no se reutiliza
 * la instancia de HomingMovement entre disparos distintos.
 *
 * ── Dos proyectiles, dos instancias ───────────────────────────────────────
 *
 * Si dos proyectiles persiguen objetivos distintos, necesitan instancias
 * distintas de HomingMovement, cada una con su propio target:
 *
 *   new HomingMovement(enemyA, 90.0, 8.0)  // persigue a enemyA
 *   new HomingMovement(enemyB, 90.0, 8.0)  // persigue a enemyB
 *
 * Si persiguen el mismo objetivo, pueden compartir la instancia porque
 * HomingMovement no tiene estado mutable propio.
 */
public final class HomingMovement implements ProjectileMovement {

    /**
     * Objetivo al que este proyectil persigue.
     *
     * AbstractEntity porque solo entidades con lifecycle de vida son objetivos
     * válidos. null = sin objetivo activo (el proyectil continúa recto).
     */
    private final AbstractEntity target;

    /** Velocidad de giro en radianes por frame (convertido desde grados en constructor). */
    private final double turnSpeedRad;

    /** Velocidad de movimiento del proyectil (magnitud del vector). */
    private final double speed;

    /**
     * @param target       la entidad objetivo a perseguir. Puede ser null (sin objetivo).
     * @param turnSpeedDeg velocidad de giro máxima en grados/frame (1–360)
     * @param speed        velocidad de avance del proyectil en unidades/frame
     */
    public HomingMovement(AbstractEntity target, double turnSpeedDeg, double speed) {
        this.target       = target;
        this.turnSpeedRad = Math.toRadians(turnSpeedDeg);
        this.speed        = speed;
    }

    /**
     * HomingMovement no tiene campos de instancia que muten frame a frame.
     *
     * isStateless() == true significa que el pool puede reutilizar la Bullet
     * que contiene esta instancia entre ciclos de vida. El nuevo blueprint
     * provee una nueva instancia de HomingMovement con el nuevo target —
     * esta instancia no se reutiliza entre disparos distintos.
     */
    @Override
    public boolean isStateless() {
        return true;
    }

    @Override
    public void tick(Bullet bullet) {
        // Sin objetivo → movimiento recto
        if (target == null) return;

        // Si el objetivo murió, dejar de perseguirlo.
        // isDead() está en AbstractEntity vía HealthComponent.
        // Un objetivo muerto puede tener posición inválida o estar siendo reciclado.
        if (target.isDead()) return;

        Vector2D bulletPos = bullet.getTransform().getPosition();
        Vector2D targetPos = target.getTransform().getPosition();

        double dx   = targetPos.getX() - bulletPos.getX();
        double dy   = targetPos.getY() - bulletPos.getY();
        double dist = Math.hypot(dx, dy);
        if (dist < 1e-6) return; // proyectil ya sobre el target

        double targetAngle  = Math.atan2(dy, dx);
        double currentAngle = Math.atan2(
                bullet.getPhysics().getYspeed(),
                bullet.getPhysics().getXspeed()
        );

        // Diferencia angular más corta (entre -π y π)
        double diff = targetAngle - currentAngle;
        while (diff >  Math.PI) diff -= 2 * Math.PI;
        while (diff < -Math.PI) diff += 2 * Math.PI;

        // Limitar el giro por frame
        double rotation = Math.copySign(Math.min(Math.abs(diff), turnSpeedRad), diff);
        double newAngle  = currentAngle + rotation;

        bullet.getPhysics().setXspeed(Math.cos(newAngle) * speed);
        bullet.getPhysics().setYspeed(Math.sin(newAngle) * speed);
    }
}
