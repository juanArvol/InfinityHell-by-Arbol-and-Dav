package Game.Engine.Entity.Components;

import Game.Engine.Component;
import Game.Engine.GameMath.Logic3D.Transform3D;

/**
 * Componente de física con eje Z — soporte para saltos en vista top-down 2.5D.
 *
 * ── FILOSOFÍA ────────────────────────────────────────────────────────────
 * Este componente es ADITIVO y OPCIONAL. No reemplaza PhysicsComponent ni
 * CollisionsSystem. Se añade a un objeto que ya tiene Physics normal y
 * "encima" gestiona el eje Z (altura sobre el suelo).
 *
 * El eje Z representa altura visual:
 *   Z = 0  → en el suelo (comportamiento 2D idéntico al original)
 *   Z > 0  → en el aire (jugador saltando, proyectil en arco, enemigo volador)
 *
 * Las colisiones en XY siguen siendo responsabilidad del CollisionsSystem existente.
 * La colisión "con el suelo" (Z → 0) la maneja este componente internamente.
 *
 * ── RENDER ───────────────────────────────────────────────────────────────
 * Para que el salto se vea bien, el objeto DEBE tener un Transform3D.
 * ShadowComponent3D puede leer getZ() para dibujar una sombra escalada en el suelo.
 *
 * Para activar Transform3D en un objeto: en el constructor de la subclase,
 * llamar al constructor protegido de GameObjects con new Transform3D():
 *
 *   // En EnemyFlying o cualquier subclase de MovingObjects:
 *   public EnemyFlying(Vector2D pos, BufferedImage tex, EnemyPhysics physics) {
 *       // El constructor de MovingObjects pasa el transform a GameObjects
 *       // a través de la cadena super(). Ver GameObjects(Transform2D).
 *   }
 *
 * Mientras el objeto no tenga Transform3D, syncTransform3D() es un no-op
 * silencioso — el objeto simplemente no tiene offset visual de altura.
 *
 * ── USO BÁSICO ───────────────────────────────────────────────────────────
 *   // En el constructor del Player o Enemy:
 *   Physics3DComponent jump3d = new Physics3DComponent();
 *   addComponent(jump3d);
 *
 *   // Con configuración explícita:
 *   addComponent(new Physics3DComponent(HeightPhysicsConfig.defaults()));
 *
 *   // En el input del Player (al presionar espacio):
 *   jump3d.jump(8.0);   // velocidad inicial hacia arriba
 *
 *   // En update() del Player:
 *   jump3d.update();    // se llama automáticamente via GameObjects.update()
 *
 * ── CONSOLIDACIÓN ────────────────────────────────────────────────────────
 * Se eliminó la copia duplicada en Game.Engine.GameMath.Physics.Types.Physics3D.
 * Esa copia añadía HeightPhysicsConfig y un tercer constructor; ambos fueron
 * integrados aquí. ShadowComponent3D ya apuntaba a esta clase — sin cambios.
 *
 * ── NO INCLUIR COMO PRINCIPAL AÚN ────────────────────────────────────────
 * Según la intención del proyecto: este sistema se añade sin romper lo existente.
 * Para activarlo en el Player, añadir el componente. Para desactivarlo, no añadirlo.
 * El CollisionsSystem y PhysicsComponent funcionan igual con o sin este componente.
 */
public class Physics3DComponent extends Component {

    // ── Parámetros físicos ────────────────────────────────────────────────

    /** Gravedad que actúa sobre Z cada tick. Valor positivo = cae hacia Z=0. */
    private double gravity = 0.5;

    /** Velocidad vertical en Z (positivo = sube, negativo = baja). */
    private double velocityZ = 0.0;

    /** Altura actual sobre el suelo en unidades lógicas. */
    private double z = 0.0;

    /** Si true, aterrizar dispara onLand(). Flag para evitar disparar múltiples veces. */
    private boolean wasInAir = false;

    // ── Constructores ─────────────────────────────────────────────────────

    /** Gravedad por defecto (0.5). */
    public Physics3DComponent() {}

    /** Gravedad personalizada. */
    public Physics3DComponent(double gravity) {
        this.gravity = gravity;
    }

    /**
     * Constructor con configuración completa.
     *
     * @param config configuración de física 3D (gravedad, velocidad terminal)
     */
    public Physics3DComponent(HeightPhysicsConfig config) {
        this.gravity = config.gravity();
    }

    // ── Ciclo de vida ─────────────────────────────────────────────────────

    @Override
    public void update(double dt) {
        if (z <= 0 && velocityZ <= 0) {
            // En el suelo — sin movimiento vertical
            if (wasInAir) {
                wasInAir = false;
                onLand();
            }
            z = 0;
            velocityZ = 0;
            syncTransform3D();
            return;
        }

        wasInAir = true;

        // Aplicar gravedad
        velocityZ -= gravity;

        // Mover en Z
        z += velocityZ;

        // Clamp al suelo
        if (z < 0) {
            z = 0;
            velocityZ = 0;
        }

        syncTransform3D();
    }

    // ── API pública ───────────────────────────────────────────────────────

    /**
     * Inicia un salto si el objeto está en el suelo.
     *
     * @param impulse velocidad inicial hacia arriba (unidades/tick).
     *                Valores típicos: 6–12 dependiendo de la gravedad.
     * @return true si el salto fue aceptado (estaba en el suelo), false si no.
     */
    public boolean jump(double impulse) {
        if (!isOnGround()) return false;
        velocityZ = impulse;
        wasInAir  = true;
        return true;
    }

    /**
     * Teleporta el objeto a una altura Z específica.
     * Útil para spawnear enemigos voladores a cierta altura.
     */
    public void setZ(double z) {
        this.z = Math.max(0, z);
        syncTransform3D();
    }

    /**
     * Aplica un impulso vertical adicional (útil para doble salto o dash vertical).
     * No verifica si está en el suelo.
     */
    public void addImpulse(double impulse) {
        velocityZ += impulse;
    }

    // ── Consultas ─────────────────────────────────────────────────────────

    public double getZ()         { return z; }
    public double getVelocityZ() { return velocityZ; }
    public boolean isOnGround()  { return z <= 0 && velocityZ <= 0; }
    public boolean isInAir()     { return !isOnGround(); }

    // ── Parámetros ────────────────────────────────────────────────────────

    public double getGravity()        { return gravity; }
    public void setGravity(double g)  { this.gravity = g; }

    // ── Hooks ─────────────────────────────────────────────────────────────

    /**
     * Llamado cuando el objeto aterriza (Z llega a 0 desde arriba).
     * Override en subclase o GameObjects para reaccionar al aterrizaje.
     * Ejemplo: reproducir sonido de aterrizaje, generar partículas de polvo.
     */
    protected void onLand() {
        // Hook vacío — override en subclase si se necesita
    }

    // ── Sincronización con Transform3D ────────────────────────────────────

    /**
     * Sincroniza la Z con el Transform3D del gameObject si existe.
     * Esto es lo que hace que el render y el depth sort sean correctos.
     *
     * Si el objeto solo tiene Transform (no Transform3D), no hace nada —
     * el objeto simplemente no tendrá offset visual de altura.
     */
    private void syncTransform3D() {
        if (gameObject == null) return;
        if (gameObject.getTransform() instanceof Transform3D t3d) {
            t3d.setZ(z);
        }
    }

    // ── Configuración inmutable ───────────────────────────────────────────

    /**
     * Configuración de física 3D.
     * Permite ajustar gravedad y velocidad terminal sin tocar la clase.
     *
     * Uso: new Physics3DComponent(new HeightPhysicsConfig(0.4, 20.0))
     *
     * Nota: terminalVelocity está disponible para que sistemas externos puedan
     * leerla y aplicarla si lo desean; Physics3DComponent no la usa internamente
     * por ahora (el clamp al suelo (z=0) actúa como límite natural).
     */
    public record HeightPhysicsConfig(double gravity, double terminalVelocity) {
        public static HeightPhysicsConfig defaults() {
            return new HeightPhysicsConfig(0.5, 15.0);
        }
    }
}
