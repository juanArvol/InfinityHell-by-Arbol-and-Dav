package Game.Items.Types.Bullets;

import Game.Items.Types.Bullets.Definition.Bullet;

/**
 * Estrategia de movimiento de un proyectil.
 *
 * ── HRFC — Weapon & Projectile System ────────────────────────────────────
 *
 * Separa CÓMO SE MUEVE un proyectil de QUÉ HACE al impactar.
 *
 * ANTES:
 *   El movimiento de los proyectiles era fijo: velocidad constante en X/Y
 *   con gravedad opcional. Para hacer movimiento homing, sinusoidal u orbital
 *   habría que añadir lógica condicional dentro de BulletBehavior.update()
 *   o de Bullet.update(), llenando el código de if/switch.
 *
 * AHORA:
 *   ProjectileMovement es la única responsable de actualizar la velocidad
 *   del proyectil cada frame. Bullet.update() llama movement.tick(bullet)
 *   antes de PhysicsStepper.moveWith(). La física y la posición las gestiona
 *   siempre BulletPhysics — ProjectileMovement solo decide qué velocidad
 *   tiene el proyectil en cada frame.
 *
 * ── EXTENSIÓN SIN MODIFICAR EL NÚCLEO ────────────────────────────────────
 *
 * Añadir un nuevo tipo de movimiento = crear una nueva clase que implemente
 * ProjectileMovement. No se toca Bullet, BulletPhysics ni ningún behavior.
 *
 * ── IMPLEMENTACIONES INCLUIDAS ────────────────────────────────────────────
 *
 *   LinearMovement      — velocidad constante (comportamiento por defecto).
 *   GravityMovement     — aplica gravedad acumulativa.
 *   HomingMovement      — gira hacia un objetivo cada frame.
 *   SinusoidalMovement  — oscila perpendicularmente a la dirección inicial.
 *   AcceleratingMovement— acelera o desacelera linealmente.
 *   BoomerangMovement   — acelera, frena, invierte y regresa.
 *
 * Las implementaciones viven en Game.Items.Types.Bullets.Movement
 * para no saturar el paquete raíz de Bullets.
 *
 * ── COMPOSICIÓN ───────────────────────────────────────────────────────────
 *
 * Los movimientos se pueden componer con CompositeMovement:
 *
 *   ProjectileMovement m = new CompositeMovement(
 *       new HomingMovement(target, 180.0),
 *       new SinusoidalMovement(3.0, 0.15)   // zigzag sobre la curva de homing
 *   );
 *
 * ── CONTRATO ──────────────────────────────────────────────────────────────
 *
 * tick(bullet) se llama exactamente una vez por frame, antes de que
 * PhysicsStepper mueva el proyectil. Debe modificar la velocidad del
 * proyectil a través de bullet.getPhysics(). No debe llamar a moveByPhysics()
 * directamente — eso lo hace Bullet.update().
 */
@FunctionalInterface
public interface ProjectileMovement {

    /**
     * Actualiza la velocidad del proyectil para este frame.
     *
     * Implementaciones típicas:
     *   - Movimiento lineal: no hace nada (velocidad ya está fija).
     *   - Homing: calcula la dirección al objetivo y ajusta vx/vy.
     *   - Sinusoidal: añade un componente perpendicular oscilante.
     *   - Aceleración: incrementa la magnitud del vector de velocidad.
     *
     * @param bullet el proyectil cuya velocidad se va a actualizar
     */
    void tick(Bullet bullet);

    // ── Composición ───────────────────────────────────────────────────────

    /**
     * Indica si esta instancia de movimiento NO tiene estado interno mutable.
     *
     * ── HRFC — Projectile System Refactor ────────────────────────────────
     *
     * Las implementaciones con estado (SinusoidalMovement, BoomerangMovement)
     * tienen campos que se modifican en cada tick (tick++, angle, etc.).
     * Una instancia stateful NO puede compartirse entre múltiples proyectiles.
     *
     * Las implementaciones sin estado (LinearMovement, AcceleratingMovement)
     * pueden compartirse — LinearMovement.INSTANCE es reutilizable por todos.
     *
     * BulletFactory y ProjectilePool usan este flag para decidir si pueden
     * reutilizar la misma instancia o deben crear una nueva por proyectil.
     *
     * Regla de implementación:
     *   - Sin campos de instancia mutables → retornar true.
     *   - Con cualquier campo que cambie en tick() → retornar false (default).
     *
     * @return true si esta instancia es segura para compartir entre proyectiles.
     */
    default boolean isStateless() {
        return false;
    }

    /**
     * Compone este movimiento con otro, aplicándolos en secuencia.
     * Útil para combinar efectos sin crear una subclase nueva.
     *
     * Ejemplo: homing + sinusoidal = misil serpenteante
     *   movement = homing.andThen(sinusoidal)
     *
     * La composición siempre es stateful (isStateless() = false) para ser
     * conservadores — aunque los dos componentes sean stateless, la lambda
     * generada por andThen no puede garantizarlo.
     */
    default ProjectileMovement andThen(ProjectileMovement after) {
        return bullet -> {
            this.tick(bullet);
            after.tick(bullet);
        };
    }
}
