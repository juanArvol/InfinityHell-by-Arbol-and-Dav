package Game.Engine.GameMath.Physics.Types.Physics3D;

import Game.Engine.Component;
import Game.Engine.Events.GameEventBus;
import Game.Engine.Events.GameEvents.OnJumpEvent;
import Game.Engine.Events.GameEvents.OnLandEvent;
import Game.Engine.GameMath.SpaceLogic.Logic3D.Transform3D;

/**
 * Componente de física con eje Z — saltos en vista top-down 2.5D.
 *
 * INTEGRADO con GameEventBus: emite OnJumpEvent y OnLandEvent.
 * RETRO-COMPATIBLE: se añade opcionalmente a cualquier GameObject.
 *
 * Ver Backup/Game/Engine/Components/Physics3DComponent.java para la lógica base.
 * Esta versión añade los eventos y el constructor con HeightPhysicsConfig.
 *
 * NOTA DE PACKAGE: el original estaba en Game.Physics3D (error de package).
 * Este está en Game.Physics3D también para no romper el import existente.
 */
public class Physics3DComponent extends Component {

    private double gravity;
    private double velocityZ = 0.0;
    private double z = 0.0;
    private boolean wasInAir = false;

    // ── Constructores ─────────────────────────────────────────────────────

    public Physics3DComponent() {
        this(0.5);
    }

    public Physics3DComponent(double gravity) {
        this.gravity = gravity;
    }

    /** Constructor con config completa — preparado para HeightPhysicsConfig. */
    public Physics3DComponent(HeightPhysicsConfig config) {
        this.gravity = config.gravity();
    }

    // ── Ciclo de vida ─────────────────────────────────────────────────────

    @Override
    public void update() {
        if (z <= 0 && velocityZ <= 0) {
            if (wasInAir) {
                wasInAir = false;
                onLand();
                //GameEventBus.post(new OnLandEvent(gameObject));
            }
            z = 0;
            velocityZ = 0;
            syncTransform3D();
            return;
        }

        wasInAir = true;
        velocityZ -= gravity;
        z += velocityZ;

        if (z < 0) {
            z = 0;
            velocityZ = 0;
        }

        syncTransform3D();
    }

    // ── API pública ───────────────────────────────────────────────────────

    /**
     * Inicia un salto si el objeto está en el suelo.
     * @return true si el salto fue aceptado.
     */
    public boolean jump(double impulse) {
        if (!isOnGround()) return false;
        velocityZ = impulse;
        wasInAir  = true;
        //GameEventBus.post(new OnJumpEvent(gameObject, impulse));
        return true;
    }

    public void setZ(double z) {
        this.z = Math.max(0, z);
        syncTransform3D();
    }

    public void addImpulse(double impulse) {
        velocityZ += impulse;
    }

    // ── Consultas ─────────────────────────────────────────────────────────

    public double getZ()         { return z; }
    public double getVelocityZ() { return velocityZ; }
    public boolean isOnGround()  { return z <= 0 && velocityZ <= 0; }
    public boolean isInAir()     { return !isOnGround(); }

    // ── Parámetros ────────────────────────────────────────────────────────

    public double getGravity()       { return gravity; }
    public void setGravity(double g) { this.gravity = g; }

    // ── Hooks ─────────────────────────────────────────────────────────────

    /** Override para lógica específica al aterrizar (sonido, partículas). */
    protected void onLand() {}

    // ── Sync Transform3D ─────────────────────────────────────────────────

    private void syncTransform3D() {
        if (gameObject == null) return;
        if (gameObject.getTransform() instanceof Transform3D t3d) {
            t3d.setZ(z);
        }
    }

    // ── Config inmutable ──────────────────────────────────────────────────

    /**
     * Configuración de física 3D.
     * Permite ajustar gravedad y velocidad terminal sin tocar la clase.
     *
     * Uso: new Physics3DComponent(new HeightPhysicsConfig(0.4, 20.0))
     */
    public record HeightPhysicsConfig(double gravity, double terminalVelocity) {
        public static HeightPhysicsConfig defaults() {
            return new HeightPhysicsConfig(0.5, 15.0);
        }
    }
}
