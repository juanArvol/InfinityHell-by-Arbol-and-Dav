package Game.Player;

import Game.Engine.Component;
import Game.Engine.Components.Physics2DComponent;
import Game.Engine.Components.Visuals.SpriteRenderer;
import Graficos.Player.PlayerAssets;

import java.awt.image.BufferedImage;

/**
 * Renderizador del jugador — animación de movimiento e idle.
 *
 * ── REFACTOR: CACHEAR REFERENCIAS DE COMPONENTES ─────────────────────────
 *
 * PROBLEMA ORIGINAL:
 *   update() llamaba getComponent(PhysicsComponent.class) cada frame:
 *
 *     double velocityX =
 *         gameObject
 *             .getComponent(PhysicsComponent.class)  // ← búsqueda lineal O(n)
 *             .getPhysics()
 *             .getVelocity()
 *             .getX();
 *
 *   getComponent() itera la lista de componentes en cada llamada. Para
 *   un componente que no cambia de frame a frame, esta búsqueda es trabajo
 *   innecesario que se ejecuta 60 veces por segundo.
 *
 * SOLUCIÓN:
 *   Cachear PhysicsComponent en start(), que se llama una sola vez al
 *   agregarse el componente al objeto. Es seguro cachear aquí porque el
 *   ciclo de vida garantiza que los componentes no se añaden ni eliminan
 *   durante el update.
 *
 *   Si physicsComponent es null en start() (orden de addComponent incorrecto),
 *   update() lo gestiona con un null-check.
 *
 * BENEFICIO:
 *   - update() es O(1) en lugar de O(n) para el acceso a física.
 *   - Código más limpio: la cadena de llamadas queda encapsulada en un campo.
 *   - Patrón correcto: start() para inicializar, update() para ejecutar.
 */
public class PlayerRenderer extends Component {

    private final PlayerState state;

    private int frame;
    private int animTick;

    // Cacheados en start() — no cambiarán durante la vida del componente
    private SpriteRenderer   baseRenderer;
    private Physics2DComponent physicsComponent;

    public PlayerRenderer(PlayerState state) {
        this.state = state;
    }

    @Override
    public void start() {
        baseRenderer     = gameObject.getComponent(SpriteRenderer.class);
        physicsComponent = gameObject.getComponent(Physics2DComponent.class);
    }

    @Override
    public void update() {
        if (baseRenderer == null || physicsComponent == null) return;

        double velocityX = physicsComponent
            .getPhysics()
            .getVelocity()
            .getX();

        boolean isMoving = Math.abs(velocityX) > 0.5;

        BufferedImage spriteToUse;

        if (isMoving) {
            animTick++;
            if (animTick >= 10) {
                frame++;
                animTick = 0;
            }

            BufferedImage[] frames = state.isDer()
                ? PlayerAssets.walkDere.getFrames()
                : PlayerAssets.walkHiz.getFrames();

            frame %= frames.length;
            spriteToUse = frames[frame];

        } else {
            frame       = 0;
            spriteToUse = PlayerAssets.idle.getSprite();
        }

        baseRenderer.setSprite(spriteToUse);
    }
}
