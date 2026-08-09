package Game.Engine.Entity.Components;

import Game.Engine.Component;

/**
 * Componente que declara qué tipo de interacción ofrece cada cara de una entidad.
 *
 * ── HRFC — World Objects extensibles ─────────────────────────────────────
 *
 * MOTIVACIÓN:
 *   El sistema de colisiones actual tiene un fuerte sesgo hacia la cara
 *   superior ("está encima del objeto"). No existe representación de qué
 *   puede hacer un objeto cuando se le contacta desde otros ángulos.
 *
 *   Este componente es PREPARATORIO para el próximo HRFC de Collision.
 *   Permite a un World Object declarar sus capacidades de interacción por
 *   cara sin contener la lógica geométrica de detección de dirección.
 *
 *   El sistema de colisiones futuro determinará desde qué cara ocurrió el
 *   contacto y consultará este componente para saber qué hacer.
 *
 * ── PRINCIPIO FUNDAMENTAL ─────────────────────────────────────────────────
 *
 *   El World Object RESPONDE a la información de contacto.
 *   El sistema de Collision DETERMINA la dirección del contacto.
 *
 *   El World Object NO calcula geométricamente desde qué dirección llegó
 *   la colisión. Solo declara qué puede hacer en cada cara.
 *
 * ── SideInteraction ───────────────────────────────────────────────────────
 *
 *   SURFACE    → cara sobre la que se puede caminar/aterrizar.
 *                CollisionsSystem la usa para establecer onGround.
 *
 *   WALL       → cara que bloquea el movimiento lateral.
 *                Soporte futuro: wall-jump, wall-slide.
 *
 *   CEILING    → cara inferior del objeto (techo para el jugador).
 *                Soporte futuro: rebotar al saltar.
 *
 *   CLIMBABLE  → cara que permite escalar/adherirse.
 *                Soporte futuro: trepar paredes, escaleras.
 *
 *   PASSTHROUGH → cara permeable: el objeto puede atravesarla desde
 *                 esa dirección (plataformas de un solo sentido).
 *
 *   NONE       → sin interacción especial. La cara simplemente bloquea.
 *
 * ── USO TÍPICO ────────────────────────────────────────────────────────────
 *
 *   // Bloque sólido estándar — todas las caras bloquean
 *   WorldObject block = new WorldObject(...)
 *       .addComponent(new InteractionSideComponent(
 *           SideInteraction.SURFACE,    // TOP — se puede pisar
 *           SideInteraction.WALL,       // RIGHT
 *           SideInteraction.WALL,       // LEFT
 *           SideInteraction.CEILING     // BOTTOM — rebota
 *       ));
 *
 *   // Plataforma unidireccional — sólo se puede aterrizar desde arriba
 *   WorldObject oneWay = new WorldObject(...)
 *       .addComponent(new InteractionSideComponent(
 *           SideInteraction.SURFACE,    // TOP — se puede pisar
 *           SideInteraction.PASSTHROUGH,// RIGHT — se puede atravesar
 *           SideInteraction.PASSTHROUGH,// LEFT
 *           SideInteraction.PASSTHROUGH // BOTTOM — se puede atravesar desde abajo
 *       ));
 *
 *   // Pared escalable
 *   WorldObject climbableWall = new WorldObject(...)
 *       .addComponent(new InteractionSideComponent(
 *           SideInteraction.SURFACE,
 *           SideInteraction.CLIMBABLE,  // RIGHT — se puede escalar
 *           SideInteraction.CLIMBABLE,  // LEFT
 *           SideInteraction.CEILING
 *       ));
 *
 *   // Default — igual que un bloque sólido estándar
 *   WorldObject standard = new WorldObject(...)
 *       .addComponent(new InteractionSideComponent());
 *
 * ── CAPACIDAD GENÉRICA ────────────────────────────────────────────────────
 *
 *   InteractionSideComponent no está acoplado a World Objects.
 *   Cualquier entidad puede declarar interacciones por cara:
 *
 *     // Escudo que solo bloquea desde la cara frontal
 *     ShieldEntity shield = new ShieldEntity(...)
 *         .addComponent(new InteractionSideComponent()
 *             .setRight(SideInteraction.WALL)
 *             .setLeft(SideInteraction.PASSTHROUGH)  // vulnerable por detrás
 *             .setTop(SideInteraction.NONE)
 *             .setBottom(SideInteraction.NONE)
 *         );
 *
 * ── RELACIÓN CON EL HRFC DE COLLISION ─────────────────────────────────────
 *
 *   Este componente es DECLARATIVO. No tiene efecto hasta que el sistema de
 *   colisiones lo consulte. El HRFC de Collision implementará:
 *
 *     1. Detección de la dirección del contacto (normal de impacto).
 *     2. Consulta a InteractionSideComponent para determinar qué interacción.
 *     3. Propagación al gameplay (SURFACE → setOnGround, CLIMBABLE → canClimb).
 *
 *   Mientras el HRFC de Collision no exista, este componente es inerte.
 *   Añadirlo hoy en los WorldObjects no rompe nada — simplemente no se consulta.
 */
public final class InteractionSideComponent extends Component {

    /**
     * Tipo de interacción ofrecida por una cara del objeto.
     */
    public enum SideInteraction {

        /**
         * Superficie sobre la que se puede caminar/aterrizar.
         * El sistema de colisiones establece onGround = true al contactar.
         * Cara TOP habitual en plataformas, suelos y obstáculos.
         */
        SURFACE,

        /**
         * Superficie lateral que bloquea el movimiento horizontal.
         * Soporte futuro: wall-jump, wall-slide, apoyarse.
         * Cara LEFT/RIGHT habitual en paredes y laterales de obstáculos.
         */
        WALL,

        /**
         * Superficie inferior que actúa como techo.
         * Soporte futuro: rebotar al saltar, colgar.
         * Cara BOTTOM de plataformas cuando el jugador salta desde abajo.
         */
        CEILING,

        /**
         * Superficie que permite escalar o adherirse.
         * Soporte futuro: trepar, wall-slide con control.
         * Cara LEFT/RIGHT de ladders, paredes especiales.
         */
        CLIMBABLE,

        /**
         * Cara permeable desde esa dirección.
         * El objeto puede atravesarla sin colisión.
         * Uso: plataformas de un solo sentido (caer desde arriba, bloquear desde abajo).
         */
        PASSTHROUGH,

        /**
         * Sin interacción especial desde esta cara.
         * La cara bloquea físicamente pero no ofrece ninguna capacidad adicional.
         * Es el comportamiento por defecto de todas las caras.
         */
        NONE
    }

    // ── Estado — interacción por cada cara ───────────────────────────────

    /** Cara superior del objeto (habitualmente la superficie pisable). */
    private SideInteraction top;

    /** Cara derecha del objeto (habitualmente pared o escalable). */
    private SideInteraction right;

    /** Cara izquierda del objeto (habitualmente pared o escalable). */
    private SideInteraction left;

    /** Cara inferior del objeto (habitualmente techo o pasante). */
    private SideInteraction bottom;

    // ── Constructores ─────────────────────────────────────────────────────

    /**
     * Configuración por defecto — bloque sólido estándar:
     *   TOP    → SURFACE    (se puede pisar)
     *   RIGHT  → WALL       (bloquea lateralmente)
     *   LEFT   → WALL       (bloquea lateralmente)
     *   BOTTOM → CEILING    (actúa como techo)
     */
    public InteractionSideComponent() {
        this(SideInteraction.SURFACE,
             SideInteraction.WALL,
             SideInteraction.WALL,
             SideInteraction.CEILING);
    }

    /**
     * Configuración explícita de todas las caras.
     *
     * @param top    interacción de la cara superior
     * @param right  interacción de la cara derecha
     * @param left   interacción de la cara izquierda
     * @param bottom interacción de la cara inferior
     */
    public InteractionSideComponent(SideInteraction top,
                                     SideInteraction right,
                                     SideInteraction left,
                                     SideInteraction bottom) {
        this.top    = (top    != null) ? top    : SideInteraction.NONE;
        this.right  = (right  != null) ? right  : SideInteraction.NONE;
        this.left   = (left   != null) ? left   : SideInteraction.NONE;
        this.bottom = (bottom != null) ? bottom : SideInteraction.NONE;
    }

    // ── Setters encadenables ───────────────────────────────────────────────

    /**
     * Establece la interacción de la cara superior.
     * Encadenable para uso en fluent API:
     *   new InteractionSideComponent().setTop(SURFACE).setRight(CLIMBABLE)
     */
    public InteractionSideComponent setTop(SideInteraction v) {
        this.top = (v != null) ? v : SideInteraction.NONE;
        return this;
    }

    public InteractionSideComponent setRight(SideInteraction v) {
        this.right = (v != null) ? v : SideInteraction.NONE;
        return this;
    }

    public InteractionSideComponent setLeft(SideInteraction v) {
        this.left = (v != null) ? v : SideInteraction.NONE;
        return this;
    }

    public InteractionSideComponent setBottom(SideInteraction v) {
        this.bottom = (v != null) ? v : SideInteraction.NONE;
        return this;
    }

    // ── Getters ───────────────────────────────────────────────────────────

    public SideInteraction getTop()    { return top;    }
    public SideInteraction getRight()  { return right;  }
    public SideInteraction getLeft()   { return left;   }
    public SideInteraction getBottom() { return bottom; }

    // ── Consultas de conveniencia ─────────────────────────────────────────

    /** True si la cara superior ofrece una superficie pisable. */
    public boolean isTopSurface()    { return top    == SideInteraction.SURFACE;    }

    /** True si alguna cara lateral es escalable. */
    public boolean hasClimbableSide() {
        return right == SideInteraction.CLIMBABLE || left == SideInteraction.CLIMBABLE;
    }

    /** True si la cara especificada es permeable (PASSTHROUGH). */
    public boolean isPassthrough(SideInteraction side) {
        return side == SideInteraction.PASSTHROUGH;
    }

    /**
     * Devuelve la interacción correspondiente a la normal de impacto proporcionada.
     * Convención: normalX/normalY en {-1, 0, +1} tal como los produce SweptAABB.
     *
     * El sistema de colisiones llama este método para saber qué interacción
     * aplica dado el ángulo de impacto detectado.
     *
     *   normalY = -1 → impacto desde abajo → el objeto impactado ofrece su TOP
     *   normalY = +1 → impacto desde arriba → el objeto impactado ofrece su BOTTOM
     *   normalX = -1 → impacto desde la derecha → el objeto impactado ofrece su RIGHT
     *   normalX = +1 → impacto desde la izquierda → el objeto impactado ofrece su LEFT
     *
     * @param normalX normal horizontal del impacto (-1, 0, +1)
     * @param normalY normal vertical del impacto   (-1, 0, +1)
     * @return SideInteraction que aplica a esta dirección de impacto
     */
    public SideInteraction forNormal(int normalX, int normalY) {
        // Eje Y tiene precedencia (igual que SweptAABB)
        if (normalY == -1) return top;     // viene de abajo → cara TOP del obstáculo
        if (normalY == +1) return bottom;  // viene de arriba → cara BOTTOM del obstáculo
        if (normalX == -1) return right;   // viene de la derecha → cara RIGHT
        if (normalX == +1) return left;    // viene de la izquierda → cara LEFT
        return SideInteraction.NONE;
    }
}
