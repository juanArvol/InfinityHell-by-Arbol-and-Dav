package Game.World.Core;

import Game.Engine.GameObjects;
import Game.World.Transition.TransitionSystem;
import java.util.function.Predicate;

/**
 * Adaptador de compatibilidad para el sistema de transición.
 *
 * ── EVOLUCIÓN ─────────────────────────────────────────────────────────────
 * WorldTransitionService ANTES mezclaba cuatro responsabilidades distintas:
 *   1. Detección de objetos fuera de bounds
 *   2. Transferencia de objetos entre mundos
 *   3. Generación de mundos vecinos
 *   4. Validación implícita (ninguna)
 *
 * AHORA WorldTransitionService es un adaptador delgado sobre TransitionSystem,
 * que implementa esas responsabilidades por separado y con mayor extensibilidad:
 *   1. TransitionDetector   → detección
 *   2. TransitionSystem.executeTransfer → transferencia
 *   3. TransitionSystem.ensureWorldExists → generación (solo cuando se necesita)
 *   4. TransitionValidator + TransitionResolver → validación y resolución
 *
 * ── RETROCOMPATIBILIDAD ───────────────────────────────────────────────────
 * La API pública de WorldTransitionService se mantiene para que WorldManager
 * no necesite cambios bruscos. WorldManager usará gradualmente la nueva
 * API de TransitionSystem directamente.
 *
 * ── DELEGACIÓN ────────────────────────────────────────────────────────────
 * Todos los métodos públicos de esta clase delegan en TransitionSystem.
 * No hay lógica duplicada.
 */
public final class WorldTransitionService {

    private final TransitionSystem transitionSystem;

    public WorldTransitionService(WorldCache cache,
                                   Game.World.Generator.WorldGenerator generator,
                                   Game.Engine.Events.GameEventBus eventBus,
                                   java.util.function.Supplier<World> currentWorldSupplier) {
        this.transitionSystem = new TransitionSystem(
            currentWorldSupplier, cache, generator, eventBus
        );
    }

    /**
     * Constructor de compatibilidad mínimo (usa GameEventBus.GLOBAL).
     * Preferir el constructor completo para mayor control.
     */
    public WorldTransitionService(WorldCache cache,
                                   Game.World.Generator.WorldGenerator generator) {
        this(cache, generator,
             Game.Engine.Events.GameEventBus.GLOBAL,
             () -> null);   // supplier temporal — WorldManager lo reconfigura
    }

    // ── API de configuración ──────────────────────────────────────────────

    /**
     * Registra el predicado que identifica el "world controller".
     * Delega en TransitionSystem.setControllerPredicate().
     */
    public void setWorldControllerPredicate(Predicate<GameObjects> predicate) {
        transitionSystem.setControllerPredicate(predicate);
    }

    // ── API de procesamiento ──────────────────────────────────────────────

    /**
     * Procesa todas las transferencias del tick actual.
     *
     * Delega completamente en TransitionSystem.update().
     *
     * @param world         el mundo actual
     * @param currentCoord  coordenada del sector actual
     * @param worldWidth    ancho lógico de cada sector
     * @param worldHeight   alto lógico de cada sector
     * @return nueva WorldCoordinator si el world controller cambió de sector,
     *         null si el sector activo no cambió
     */
    public WorldCoordinator processTransitions(World world,
                                               WorldCoordinator currentCoord,
                                               int worldWidth,
                                               int worldHeight) {
        return transitionSystem.update(world, currentCoord, worldWidth, worldHeight);
    }

    /**
     * Acceso directo al TransitionSystem para código que necesita la API completa.
     * Usar para registrar gates, solicitar teleportes, etc.
     */
    public TransitionSystem getTransitionSystem() {
        return transitionSystem;
    }
}
