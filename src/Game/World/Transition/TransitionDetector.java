package Game.World.Transition;

import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Engine.GameObjects;
import Game.World.Core.World;
import Game.World.Core.WorldCoordinator;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Detecta automáticamente las transiciones por cruce de borde.
 *
 * ── RESPONSABILIDAD ÚNICA ─────────────────────────────────────────────────
 * TransitionDetector hace exactamente una cosa: detectar qué entidades
 * salieron de los bounds del sector actual y crear un TransitionRequest
 * para cada una.
 *
 * Esta responsabilidad fue extraída de WorldTransitionService, que antes
 * mezclaba detección + transferencia + generación de vecinos.
 *
 * ── LO QUE NO HACE ────────────────────────────────────────────────────────
 * - No transfiere entidades entre mundos (eso es TransitionSystem).
 * - No genera mundos vecinos (eso es WorldPrewarmService).
 * - No valida colisiones post-transición (eso es TransitionValidator).
 * - No ajusta coordenadas finales (eso es calcularlas y pasarlas en el request).
 *
 * ── PREDICADO DE CONTROLADOR ─────────────────────────────────────────────
 * Para determinar si una entidad es el "world controller" (cuya transición
 * cambia el sector activo), el detector usa un Predicate<GameObjects> inyectable.
 * Por defecto: ningún objeto es controller (requiere configuración explícita).
 */
public final class TransitionDetector {

    /** Predicado que identifica la entidad que controla el sector activo. */
    private Predicate<GameObjects> controllerPredicate = obj -> false;

    public TransitionDetector() {}

    /**
     * Configura el predicado que identifica la entidad controladora.
     * En la práctica: {@code obj -> obj instanceof Player}.
     */
    public void setControllerPredicate(Predicate<GameObjects> predicate) {
        this.controllerPredicate = predicate;
    }

    // ── Detección ─────────────────────────────────────────────────────────

    /**
     * Detecta todas las entidades fuera de los bounds del mundo actual
     * y retorna un TransitionRequest por cada una.
     *
     * @param world        el mundo actual
     * @param currentCoord coordenada del sector actual
     * @param worldWidth   ancho lógico de cada sector
     * @param worldHeight  alto lógico de cada sector
     * @return lista de TransitionRequests (una por entidad fuera de bounds)
     */
    public List<TransitionRequest> detect(World world,
                                           WorldCoordinator currentCoord,
                                           int worldWidth,
                                           int worldHeight) {
        List<TransitionRequest> requests = new ArrayList<>();

        for (GameObjects obj : world.getDynamicEntityRegistry().getAll()) {
            var pos = obj.getTransform().getPosition();
            double x = pos.getX();
            double y = pos.getY();

            // Determinar dirección de cruce
            int dx = 0, dy = 0;
            if      (x < 0)            dx = -1;
            else if (x >= worldWidth)  dx =  1;
            if      (y < 0)            dy = -1;
            else if (y >= worldHeight) dy =  1;

            // Si no cruzó ningún borde, skip
            if (dx == 0 && dy == 0) continue;

            // Calcular posición ajustada en el nuevo sector
            double newX = x;
            double newY = y;
            if (dx != 0) newX = (dx > 0) ? newX - worldWidth  : newX + worldWidth;
            if (dy != 0) newY = (dy > 0) ? newY - worldHeight : newY + worldHeight;

            boolean isController = controllerPredicate.test(obj);

            TransitionRequest request = TransitionRequest.borderCross(
                obj,
                currentCoord,
                dx, dy,
                new Vector2D(newX, newY),
                isController
            );

            requests.add(request);
        }

        return requests;
    }
}
