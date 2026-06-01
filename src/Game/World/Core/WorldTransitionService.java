package Game.World.Core;

import Game.Engine.GameObjects;
import Game.World.Generator.WorldGenerator;

import java.util.ArrayList;
import java.util.List;

/**
 * Servicio de transición entre mundos.
 *
 * RAZÓN DE EXISTENCIA:
 *   La lógica de transferencia de objetos entre mundos era una responsabilidad
 *   mezclada dentro de WorldManager. Extraerla cumple SRP y facilita testing.
 *
 *   WorldTransitionService sabe:
 *   - Qué objetos salieron del mundo actual (pos fuera de bounds)
 *   - A qué mundo vecino van
 *   - Cómo ajustar sus coordenadas tras el cruce
 *   - Si el objeto rastreado (el "player") cruzó → qué WorldCoordinator es el nuevo actual
 *
 *   WorldTransitionService NO sabe:
 *   - Qué es un Player (usa un predicado inyectable en lugar de instanceof)
 *   - Cómo dibujar mundos
 *   - Cómo pre-generar vecinos (eso es WorldManager)
 *
 * ELIMINACIÓN DE instanceof Player:
 *   El código original usaba `if (obj instanceof Player)` para determinar
 *   si el objeto que cruzó era el jugador y actualizar currentCoord.
 *   Esto acoplaba WorldManager al tipo concreto Player.
 *
 *   Solución: WorldTransitionService recibe un `java.util.function.Predicate<GameObjects>`
 *   que identifica el objeto "controlador de mundo" (puede ser Player o cualquier otro).
 *   GameState lo configura: transitionService.setWorldControllerPredicate(obj -> obj instanceof Player)
 *   Este acoplamiento queda en la capa de composición (GameState), no en la lógica de dominio.
 */
public final class WorldTransitionService {

    private final WorldCache     cache;
    private final WorldGenerator generator;

    // Predicado que identifica cuál objeto controla el cambio de mundo activo.
    // Por defecto: cualquier objeto que salga cambia el mundo (comportamiento original).
    // En práctica: GameState lo configura como obj -> obj instanceof Player.
    private java.util.function.Predicate<GameObjects> worldControllerPredicate =
        obj -> true; // fallback: cualquier objeto que cruce activa el cambio

    public WorldTransitionService(WorldCache cache, WorldGenerator generator) {
        this.cache     = cache;
        this.generator = generator;
    }

    /**
     * Registra el predicado que identifica el "objeto controlador" de mundo.
     * Cuando ese objeto cruza un borde, el WorldCoordinator activo cambia.
     *
     * Uso desde GameState:
     *   transitionService.setWorldControllerPredicate(obj -> obj instanceof Player);
     *
     * Esto mantiene el conocimiento de "Player" en la capa de composición,
     * no en la lógica de transición.
     */
    public void setWorldControllerPredicate(java.util.function.Predicate<GameObjects> predicate) {
        this.worldControllerPredicate = predicate;
    }

    /**
     * Procesa todas las transferencias de objetos entre mundos.
     *
     * @param world         el mundo actual
     * @param currentCoord  coordenada del mundo actual
     * @param worldWidth    ancho lógico de cada mundo
     * @param worldHeight   alto lógico de cada mundo
     * @return              nueva WorldCoordinator si el objeto controlador cruzó,
     *                      null si el mundo activo no cambió
     */
    public WorldCoordinator processTransitions(World world,
                                               WorldCoordinator currentCoord,
                                               int worldWidth,
                                               int worldHeight) {
        List<GameObjects> toTransfer = findObjectsOutOfBounds(world, worldWidth, worldHeight);
        if (toTransfer.isEmpty()) return null;

        WorldCoordinator newCurrentCoord = null;

        for (GameObjects obj : toTransfer) {
            var pos = obj.getTransform().getPosition();

            int dx = 0, dy = 0;
            if (pos.getX() < 0)                dx = -1;
            else if (pos.getX() >= worldWidth)  dx =  1;
            if (pos.getY() < 0)                dy = -1;
            else if (pos.getY() >= worldHeight) dy =  1;

            WorldCoordinator nextCoord = new WorldCoordinator(
                currentCoord.x() + dx,
                currentCoord.y() + dy
            );

            ensureWorldExists(nextCoord, worldWidth, worldHeight);

            // Ajustar coordenadas del objeto al nuevo mundo
            double newX = pos.getX();
            double newY = pos.getY();
            if (dx != 0) newX = (dx > 0) ? newX - worldWidth  : newX + worldWidth;
            if (dy != 0) newY = (dy > 0) ? newY - worldHeight : newY + worldHeight;
            pos.setX(newX);
            pos.setY(newY);

            world.remove(obj);
            World nextWorld;
            synchronized (cache) {
                nextWorld = cache.get(nextCoord);
            }
            nextWorld.add(obj);

            // Si este objeto es el controlador de mundo, actualizar currentCoord
            if (worldControllerPredicate.test(obj)) {
                newCurrentCoord = nextCoord;
            }
        }

        // Flush del mundo actual y del nuevo mundo si hubo cambio
        world.getObjectsContainer().flush();
        if (newCurrentCoord != null) {
            synchronized (cache) {
                World nextWorld = cache.get(newCurrentCoord);
                if (nextWorld != null) nextWorld.getObjectsContainer().flush();
            }
        }

        return newCurrentCoord;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<GameObjects> findObjectsOutOfBounds(World world, int worldWidth, int worldHeight) {
        List<GameObjects> result = new ArrayList<>();
        for (var obj : world.getObjectsContainer().getObjects()) {
            var pos = obj.getTransform().getPosition();
            if (pos.getX() < 0 || pos.getX() >= worldWidth ||
                pos.getY() < 0 || pos.getY() >= worldHeight) {
                result.add(obj);
            }
        }
        return result;
    }

    private void ensureWorldExists(WorldCoordinator coord, int worldWidth, int worldHeight) {
        synchronized (cache) {
            if (!cache.contains(coord)) {
                cache.put(generator.generate(worldWidth, worldHeight, coord));
            }
        }
    }
}
