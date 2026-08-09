package Game.Engine.Entity.Components;

import Game.Engine.Component;
import Game.Engine.GameMath.Logic2D.Vector2D;

/**
 * Componente que declara que una entidad puede ser desplazada por contacto.
 *
 * ── HRFC — World Objects extensibles ─────────────────────────────────────
 *
 * RESPONSABILIDAD:
 *   PushableComponent es una declaración de capacidad. Indica que esta entidad
 *   acepta impulsos de desplazamiento generados por el contacto con otras.
 *   No implementa la mecánica de empuje — esa lógica pertenece al sistema
 *   de colisiones o al onCollisionWith() del objeto que empuja.
 *
 * ── CAPACIDAD GENÉRICA ────────────────────────────────────────────────────
 *
 *   PushableComponent no está acoplado a World Objects. Cualquier entidad
 *   puede declarar esta capacidad:
 *
 *     // Caja del mundo empujable
 *     WorldObject crate = new WorldObject(...);
 *     crate.addComponent(new Physics2DComponent(cratePhysics));
 *     crate.addComponent(new PushableComponent(0.8));
 *
 *     // Proyectil enemigo desplazable (mecánica futura)
 *     EnemyBullet bullet = new EnemyBullet(...);
 *     bullet.addComponent(new PushableComponent(1.0));
 *
 *     // Bala que arrastra otra bala (mecánica futura de GrapplingBullet)
 *     GrapplingBullet hook = new GrapplingBullet(...);
 *     // hook interactúa con EnemyBullet que tiene PushableComponent
 *
 * ── PARÁMETRO pushReceptivity ─────────────────────────────────────────────
 *
 *   pushReceptivity ∈ [0.0, 1.0]:
 *
 *     0.0 → declara la capacidad pero no transmite ninguna fuerza.
 *           Útil para objetos que "reaccionan" al empuje de otra forma
 *           (sonido, partículas) sin desplazarse físicamente.
 *
 *     1.0 → transmite el 100% de la fuerza de empuje.
 *           Objeto perfectamente desplazable (caja ligera).
 *
 *     0.5 → transmite el 50% de la fuerza.
 *           Objeto con resistencia media (barril).
 *
 *   El sistema de empuje calcula el impulso transmitido como:
 *     impulso_efectivo = impulso_fuente × pushReceptivity
 *
 *   El resultado se aplica vía Physics2DComponent.addForce() si está presente.
 *   Si no hay Physics2DComponent, el desplazamiento no tiene efecto físico —
 *   el objeto queda preparado para reaccionar a empujes sin participar en
 *   la simulación física completa (útil para objetos de desplazamiento discreto).
 *
 * ── RELACIÓN CON Physics2DComponent ──────────────────────────────────────
 *
 *   PushableComponent ≠ Physics2DComponent.
 *
 *   Physics2DComponent: "esta entidad participa en la simulación física del Engine".
 *   PushableComponent:  "esta entidad puede recibir impulsos de otras entidades".
 *
 *   Combinaciones posibles:
 *
 *     PushableComponent solo → declara receptividad, sin física completa.
 *     Physics2DComponent solo → física completa, sin desplazamiento por contacto.
 *     Ambos → física completa + desplazamiento por contacto. Caso típico de Crate.
 *
 * ── SISTEMA DE EMPUJE ─────────────────────────────────────────────────────
 *
 *   El sistema que aplica el empuje (CollisionsSystem futuro o
 *   onCollisionWith del objeto empujador) sigue este patrón:
 *
 *     PushableComponent pushable = other.getComponent(PushableComponent.class);
 *     if (pushable != null && pushable.isEnabled()) {
 *         Vector2D impulse = computePushImpulse(attacker, other);
 *         pushable.applyPush(other, impulse.getX(), impulse.getY());
 *     }
 *
 * ── ACUMULACIÓN DE IMPULSOS ───────────────────────────────────────────────
 *
 *   Si múltiples objetos empujan a la misma entidad en el mismo frame,
 *   los impulsos se acumulan antes de aplicarse. applyPush() delega en
 *   Physics2DComponent.addForce() que los integra directamente en velocity.
 *
 * ── ESTADO Y CICLO DE VIDA ────────────────────────────────────────────────
 *
 *   PushableComponent es stateless más allá de pushReceptivity y enabled.
 *   No acumula estado entre frames — cada empuje es un evento puntual.
 */
public final class PushableComponent extends Component {

    /**
     * Factor de receptividad al empuje ∈ [0.0, 1.0].
     * 1.0 = absorbe el 100% del impulso. 0.0 = no se desplaza.
     */
    private double pushReceptivity;

    /**
     * Si false, la entidad ignora los empujes aunque tenga este componente.
     * Permite desactivar temporalmente la receptividad (ej: caja anclada).
     */
    private boolean enabled;

    // ── Constructores ─────────────────────────────────────────────────────

    /**
     * Receptividad completa (1.0), habilitado.
     * Para objetos totalmente desplazables.
     */
    public PushableComponent() {
        this(1.0);
    }

    /**
     * Receptividad configurable, habilitado.
     *
     * @param pushReceptivity factor de receptividad en [0.0, 1.0].
     *                        Se clampea al rango válido.
     */
    public PushableComponent(double pushReceptivity) {
        this.pushReceptivity = Math.max(0.0, Math.min(1.0, pushReceptivity));
        this.enabled         = true;
    }

    // ── API de empuje ─────────────────────────────────────────────────────

    /**
     * Aplica un impulso de empuje a la entidad propietaria.
     *
     * Si la entidad tiene {@link Physics2DComponent}, el impulso se
     * transmite a la simulación física vía {@code addForce()}.
     * Si no tiene física, el empuje no produce efecto físico —
     * el componente solo registra que se recibió el impulso.
     *
     * El impulso efectivo se escala por {@code pushReceptivity}:
     *   fx_efectivo = fx × pushReceptivity
     *   fy_efectivo = fy × pushReceptivity
     *
     * @param fx componente X del impulso de empuje (en unidades de fuerza)
     * @param fy componente Y del impulso de empuje (en unidades de fuerza)
     */
    public void applyPush(double fx, double fy) {
        if (!enabled || gameObject == null) return;
        if (pushReceptivity <= 0.0) return;

        Physics2DComponent physComp =
            gameObject.getComponent(Physics2DComponent.class);

        if (physComp != null) {
            double effFx = fx * pushReceptivity;
            double effFy = fy * pushReceptivity;
            physComp.getPhysics().addForce(effFx, effFy);
        }
        // Si no hay Physics2DComponent, el push queda registrado pero sin efecto físico.
        // Las subclases pueden sobreescribir onPushReceived() para reaccionar.
        onPushReceived(fx * pushReceptivity, fy * pushReceptivity);
    }

    /**
     * Aplica un impulso de empuje expresado como Vector2D.
     *
     * @param impulse vector de impulso; no puede ser null.
     */
    public void applyPush(Vector2D impulse) {
        if (impulse == null) return;
        applyPush(impulse.getX(), impulse.getY());
    }

    // ── Hook de extensión ─────────────────────────────────────────────────

    /**
     * Llamado cada vez que se recibe un impulso de empuje efectivo.
     * El impulso ya tiene aplicado el factor pushReceptivity.
     *
     * Override en subclase o componente para reaccionar al empuje
     * sin física (sonido, partículas, animación de impacto):
     *
     *   {@literal @}Override
     *   protected void onPushReceived(double fx, double fy) {
     *       soundSystem.play("crate_bump");
     *       particles.emit(gameObject.getTransform().getPosition(), 5);
     *   }
     *
     * @param fx componente X del impulso efectivo
     * @param fy componente Y del impulso efectivo
     */
    protected void onPushReceived(double fx, double fy) {
        // Hook vacío — override si se necesita reacción sin física
    }

    // ── Consultas ─────────────────────────────────────────────────────────

    /**
     * Factor de receptividad al empuje [0.0, 1.0].
     */
    public double getPushReceptivity() { return pushReceptivity; }

    /**
     * Cambia el factor de receptividad.
     * Útil para modificar el comportamiento sin reemplazar el componente.
     *
     * @param v nuevo factor en [0.0, 1.0]. Se clampea al rango válido.
     */
    public void setPushReceptivity(double v) {
        this.pushReceptivity = Math.max(0.0, Math.min(1.0, v));
    }

    /** True si este componente acepta impulsos de empuje. */
    public boolean isEnabled() { return enabled; }

    /**
     * Habilita o deshabilita la receptividad al empuje.
     * Con false, applyPush() no tiene efecto aunque el componente esté presente.
     */
    public void setEnabled(boolean v) { this.enabled = v; }
}
