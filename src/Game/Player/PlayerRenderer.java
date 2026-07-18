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
 *   Parado        → play("idle"),  flipH = según última dirección conocida
 *   Moviendo dcha → play("walk_right"), flipH = false
 *   Moviendo izda → play("walk_right"), flipH = true   ← mismo anim, flip
 *
 * El flip se aplica sobre el SpriteRenderer vía setFlipH(). Este método
 * actualiza el TransformData internamente y propaga el cambio en el siguiente
 * render a través de SpriteDrawer → FlipStrategy.
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
    public void update() {
        if (animController == null || physicsComponent == null) return;

        double velocityX = physicsComponent
            .getPhysics()
            .getVelocity()
            .getX();

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
