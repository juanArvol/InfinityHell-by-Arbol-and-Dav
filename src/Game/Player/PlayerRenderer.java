package Game.Player;

import Game.Engine.Component;
import Game.Engine.Entity.Components.Physics2DComponent;
import Game.Engine.Entity.Components.Visuals.AnimationControllerComponent;
import Game.Engine.Entity.Components.Visuals.SpriteRendererComponent;

/**
 * PlayerRenderer — decide qué animación reproducir y gestiona el flip.
 *
 * ── HRFC-003.6: MIGRACIÓN A SPRITESHEET ──────────────────────────────────
 * El jugador ahora usa un único SpriteSheet. No existe animación "walk_left":
 * el movimiento hacia la izquierda reutiliza los frames de "walk_right"
 * aplicando flipH=true en el SpriteRenderer vía TransformData.
 *
 * ── SEPARACIÓN DE RESPONSABILIDADES ──────────────────────────────────────
 *   PlayerRenderer   → DECIDE qué animación reproducir y en qué dirección
 *   AnimationController → AVANZA los frames y notifica al SpriteRenderer
 *   SpriteRenderer   → DIBUJA el frame con el TransformData activo
 *
 * ── LÓGICA DE ANIMACIÓN ───────────────────────────────────────────────────
 *   En el aire cayendo (sin input X)   → play("falling")
 *   En el aire cayendo (con input X)   → play("fall_to_walk")
 *   En el aire subiendo (sin input X)  → play("jumping")
 *   En el aire subiendo (con input X)  → play("jump_to_walk")
 *   En el suelo parado                 → play("idle"), flipH = según última dirección
 *   En el suelo moviendose dcha        → play("walk_right"), flipH = false
 *   En el suelo moviendose izda        → play("walk_right"), flipH = true
 *
 * El flip se aplica sobre el SpriteRenderer vía setFlipH(). Este método
 * actualiza el TransformData internamente y propaga el cambio en el siguiente
 * render a través de SpriteDrawer → FlipStrategy.
 *
 * ── TRANSICIONES SUAVES ───────────────────────────────────────────────────
 * Las animaciones *_to_walk reproducen los primeros frames de jumping/falling
 * una vez, luego transicionan a los frames de loop de walking. Esto crea una
 * transición visual fluida cuando el jugador tiene input horizontal en el aire.
 *
 * ── PRIORIDAD DE ANIMACIONES ─────────────────────────────────────────────
 * La prioridad de las animaciones es:
 *   1. Falling (cayendo: velocityY > 0.5 y no en el suelo)
 *      - Sin input X: "falling" (loop puro)
 *      - Con input X: "fall_to_walk" (transición suave)
 *   2. Jumping (saltando: velocityY < -0.5 y no en el suelo)
 *      - Sin input X: "jumping" (loop puro)
 *      - Con input X: "jump_to_walk" (transición suave)
 *   3. Walk (moviéndose en el suelo: |velocityX| > 0.5 y en el suelo)
 *   4. Idle (parado: ninguna de las anteriores)
 *
 * ── CICLO DE VIDA ─────────────────────────────────────────────────────────
 * start() cachea AnimationController, SpriteRenderer y Physics2DComponent.
 * update() lee la velocidad y delega la decisión de animación + flip.
 */
public class PlayerRenderer extends Component {

    private final PlayerState state;

    private AnimationControllerComponent animController;
    private SpriteRendererComponent      spriteRenderer;
    private Physics2DComponent  physicsComponent;

    public PlayerRenderer(PlayerState state) {
        this.state = state;
    }

    @Override
    public void start() {
        animController   = gameObject.getComponent(AnimationControllerComponent.class);
        spriteRenderer   = gameObject.getComponent(SpriteRendererComponent.class);
        physicsComponent = gameObject.getComponent(Physics2DComponent.class);

        if (animController == null) {
            System.err.println("[PlayerRenderer] AnimationController no encontrado. "
                + "Asegurarse de añadir addComponent(new AnimationController(...)) "
                + "antes de PlayerRenderer en el constructor de Player.");
        }
        if (spriteRenderer == null) {
            System.err.println("[PlayerRenderer] SpriteRenderer no encontrado. "
                + "El flip horizontal no tendrá efecto.");
        }
    }

    @Override
    public void update(double dt) {
        if (animController == null || physicsComponent == null) return;

        var physics = physicsComponent.getPhysics();
        double velocityX = physics.getVelocity().getX();
        double velocityY = physics.getVelocity().getY();
        boolean onGround = physics.getOnGround();

        // ── PRIORIDAD 1: Animaciones aéreas ──────────────────────────────
        // Detectar si el jugador está en el aire y qué dirección vertical tiene
        
        if (!onGround) {
            // Detectar si hay input/movimiento horizontal
            boolean hasHorizontalMovement = Math.abs(velocityX) > 0.5;
            
            // Subiendo (velocidad Y negativa = hacia arriba en coordenadas de pantalla)
            if (velocityY < -0.5) {
                // Con movimiento horizontal: usar transición suave a walking
                if (hasHorizontalMovement) {
                    animController.play("jump_to_walk");
                } else {
                    // Sin movimiento horizontal: animación de salto pura
                    animController.play("jumping");
                }
            } 
            // Cayendo (velocidad Y positiva = hacia abajo en coordenadas de pantalla)
            else if (velocityY > 0.5) {
                // Con movimiento horizontal: usar transición suave a walking
                if (hasHorizontalMovement) {
                    animController.play("fall_to_walk");
                } else {
                    // Sin movimiento horizontal: animación de caída pura
                    animController.play("falling");
                }
            }
            // Punto exacto del apex (velocidad Y ≈ 0): mantener última animación
            // para evitar flickering entre jumping/falling
            
            // Actualizar flip horizontal basado en dirección de movimiento horizontal
            if (hasHorizontalMovement && spriteRenderer != null) {
                boolean goingRight = velocityX > 0;
                spriteRenderer.setFlipH(!goingRight);
            }
            
            return; // No procesar animaciones de suelo
        }

        // ── PRIORIDAD 2: Animaciones de suelo ────────────────────────────
        boolean isMoving = Math.abs(velocityX) > 0.5;

        if (isMoving) {
            boolean goingRight = velocityX > 0;

            // Ambas direcciones usan la misma animación "walk_right".
            // El flip horizontal produce el efecto visual de ir a la izquierda.
            animController.play("walk_right");

            if (spriteRenderer != null) {
                // flipH=false → frames originales (derecha)
                // flipH=true  → frames reflejados (izquierda)
                spriteRenderer.setFlipH(!goingRight);
            }
        } else {
            // Parado: idle. El flip mantiene el valor anterior (última dirección
            // conocida) para que el jugador mire hacia donde caminó por última vez.
            animController.play("idle");
        }
    }
}
