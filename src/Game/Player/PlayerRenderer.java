package Game.Player;

import Game.Engine.Component;
import Game.Engine.Components.Physics2DComponent;
import Game.Engine.Components.Visuals.AnimationController;

/**
 * PlayerRenderer — controla qué animación se reproduce según el estado del jugador.
 *
 * ── HRFC-002: MIGRACIÓN A ANIMACIONES ORIENTADAS A DATOS ─────────────────
 *
 * ANTES:
 *   - Accedía directamente a PlayerAssets.walkDere.getFrames() cada frame.
 *   - Llamaba SpriteRenderer.setSprite(BufferedImage) directamente.
 *   - Mantenía frame y animTick como estado local.
 *   - El Gameplay conocía BufferedImage.
 *
 * AHORA:
 *   - Obtiene AnimationController (el que gestiona el estado de frames).
 *   - Llama animController.play("walk_right") / play("walk_left") / play("idle").
 *   - El tick y el frameIndex los gestiona AnimationController internamente.
 *   - El Gameplay no conoce ni BufferedImage ni SpriteFrame.
 *
 * ── SEPARACIÓN DE RESPONSABILIDADES ──────────────────────────────────────
 *   PlayerRenderer   → DECIDE qué animación reproducir (lógica de gameplay)
 *   AnimationController → AVANZA los frames y notifica a SpriteRenderer
 *   SpriteRenderer   → DIBUJA el frame actual
 *
 * ── CICLO DE VIDA ─────────────────────────────────────────────────────────
 * start() cachea las referencias a Physics2DComponent y AnimationController.
 * update() lee la velocidad y delega la decisión de animación.
 */
public class PlayerRenderer extends Component {

    private final PlayerState state;

    // Cacheados en start() — no cambian durante la vida del componente
    private AnimationController  animController;
    private Physics2DComponent   physicsComponent;

    public PlayerRenderer(PlayerState state) {
        this.state = state;
    }

    @Override
    public void start() {
        animController   = gameObject.getComponent(AnimationController.class);
        physicsComponent = gameObject.getComponent(Physics2DComponent.class);

        if (animController == null) {
            System.err.println("[PlayerRenderer] AnimationController no encontrado. "
                + "Asegurarse de añadir addComponent(new AnimationController(PlayerAssets.handle)) "
                + "antes de PlayerRenderer en el constructor de Player.");
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
            // play() es idempotente — no reinicia si ya está reproduciéndose
            if (state.isDer()) {
                animController.play("walk_right");
            } else {
                animController.play("walk_left");
            }
        } else {
            animController.play("idle");
        }
    }
}
