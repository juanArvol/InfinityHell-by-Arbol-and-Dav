package Game.World.Transition;

import Game.Engine.Entity.Components.Collisions.ColliderComponent;
import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.World.Core.World;
import java.awt.Rectangle;
import java.util.Optional;

/**
 * Resuelve conflictos de posición cuando una transición es inválida.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * Cuando TransitionValidator reporta que la posición destino está bloqueada,
 * TransitionResolver busca la posición libre más cercana.
 *
 * ── ESTRATEGIAS DE RESOLUCIÓN ─────────────────────────────────────────────
 * El resolver usa la ResolutionStrategy inyectada. Las estrategias incluidas:
 *
 *   PUSH_NEAREST  → busca la posición libre más cercana en espiral
 *   PUSH_CARDINAL → busca en los 4 ejes cardinales y toma el más cercano
 *   REJECT        → rechaza la transición (el objeto no transita)
 *   SNAP_TO_EDGE  → coloca el objeto en el borde del sector destino
 *
 * ── COMPOSICIÓN ───────────────────────────────────────────────────────────
 * TransitionResolver no tiene lógica específica del juego. Los sistemas de
 * gameplay que necesiten comportamiento especial (ej: "si no hay espacio,
 * dañar la entidad") deben implementar una ResolutionStrategy custom.
 */
public final class TransitionResolver {

    /**
     * Estrategia de resolución cuando la posición destino está bloqueada.
     */
    public interface ResolutionStrategy {
        /**
         * Intenta encontrar una posición válida alternativa.
         *
         * @param request     el request original con la posición bloqueada
         * @param targetWorld el mundo destino
         * @param validator   el validador para verificar candidatos
         * @return Optional con el request corregido, o empty para rechazar la transición
         */
        Optional<TransitionRequest> resolve(TransitionRequest request,
                                            World targetWorld,
                                            TransitionValidator validator);
    }

    // ── Estrategias incluidas ─────────────────────────────────────────────

    /**
     * Busca la posición libre más cercana en espiral de radio creciente.
     * Intenta hasta {@code maxAttempts} posiciones antes de rechazar.
     */
    public static ResolutionStrategy pushNearest(int maxAttempts, double stepSize) {
        return (request, world, validator) -> {
            Vector2D origin  = request.getTargetPosition();
            double   step    = stepSize;

            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                // Espiral: probar en los 4 cardinales, luego 8 diagonales, etc.
                double[][] offsets = spiralOffsets(attempt, step);
                for (double[] offset : offsets) {
                    Vector2D candidate = new Vector2D(
                        origin.getX() + offset[0],
                        origin.getY() + offset[1]
                    );
                    // Clamp a los bounds del mundo
                    candidate = clampToWorld(candidate, world, request.getSubject());

                    TransitionRequest revised = request.withTargetPosition(candidate);
                    if (validator.validate(revised, world).valid()) {
                        return Optional.of(revised);
                    }
                }
            }
            return Optional.empty(); // No se encontró posición válida
        };
    }

    /**
     * Busca en los 4 cardinales y retorna el más cercano que esté libre.
     */
    public static ResolutionStrategy pushCardinal(double distance) {
        return (request, world, validator) -> {
            Vector2D origin = request.getTargetPosition();
            double[][] directions = {{distance, 0}, {-distance, 0},
                                     {0, distance}, {0, -distance}};

            for (double[] dir : directions) {
                Vector2D candidate = new Vector2D(
                    origin.getX() + dir[0],
                    origin.getY() + dir[1]
                );
                TransitionRequest revised = request.withTargetPosition(candidate);
                if (validator.validate(revised, world).valid()) {
                    return Optional.of(revised);
                }
            }
            return Optional.empty();
        };
    }

    /**
     * Rechaza la transición. El objeto no se mueve al sector destino.
     */
    public static ResolutionStrategy reject() {
        return (request, world, validator) -> Optional.empty();
    }

    /**
     * Coloca el objeto en el borde del sector destino, en el lado más cercano
     * al punto de entrada.
     */
    @SuppressWarnings({"deprecation", "removal"})
    public static ResolutionStrategy snapToEdge(int edgeMargin) {
        return (request, world, validator) -> {
            Vector2D pos = request.getTargetPosition();
            double x = Math.max(edgeMargin, Math.min(pos.getX(),
                                                     world.getWidth()  - edgeMargin));
            double y = Math.max(edgeMargin, Math.min(pos.getY(),
                                                     world.getHeight() - edgeMargin));
            TransitionRequest revised = request.withTargetPosition(new Vector2D(x, y));
            return Optional.of(revised);
        };
    }

    // ── Instancia ─────────────────────────────────────────────────────────

    private final ResolutionStrategy strategy;

    public TransitionResolver() {
        this.strategy = pushNearest(16, 8.0);  // estrategia por defecto
    }

    public TransitionResolver(ResolutionStrategy strategy) {
        this.strategy = strategy;
    }

    /**
     * Intenta resolver la transición si la posición está bloqueada.
     *
     * @param request     el request con la posición posiblemente bloqueada
     * @param targetWorld el mundo destino
     * @param validator   el validador para verificar candidatos
     * @return Optional con el request (original o corregido), o empty para rechazar
     */
    public Optional<TransitionRequest> resolve(TransitionRequest request,
                                               World targetWorld,
                                               TransitionValidator validator) {
        // Si la posición original es válida, no hay nada que resolver
        if (validator.validate(request, targetWorld).valid()) {
            return Optional.of(request);
        }
        return strategy.resolve(request, targetWorld, validator);
    }

    // ── Helpers privados ──────────────────────────────────────────────────

    /**
     * Genera puntos en espiral alrededor del origen en el radio `attempt * step`.
     */
    private static double[][] spiralOffsets(int radius, double step) {
        double r = radius * step;
        // 8 direcciones en el radio dado: cardinales + diagonales
        double diag = r * 0.7071; // r / sqrt(2)
        return new double[][] {
            { r, 0}, {-r, 0}, {0,  r}, {0, -r},
            { diag,  diag}, {-diag,  diag},
            { diag, -diag}, {-diag, -diag}
        };
    }

    @SuppressWarnings({"deprecation", "removal"})
    private static Vector2D clampToWorld(Vector2D pos, World world,
                                          Game.Engine.GameObjects subject) {
        ColliderComponent col = subject.getComponent(ColliderComponent.class);
        Rectangle bounds = col != null ? col.getBounds() : new Rectangle(0, 0, 1, 1);
        double x = Math.max(0, Math.min(pos.getX(), world.getWidth()  - bounds.width));
        double y = Math.max(0, Math.min(pos.getY(), world.getHeight() - bounds.height));
        return new Vector2D(x, y);
    }
}
