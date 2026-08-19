package Game.World.Transition;

import Game.Engine.GameEventBus;
import Game.Engine.GameObjects;
import Game.Gameplay.Events.TransitionEvent;
import Game.World.Core.World;
import Game.World.Core.WorldCache;
import Game.World.Core.WorldCoordinator;
import Game.World.Entity.DynamicEntityRegistry;
import Game.World.Generator.WorldGenerator;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Sistema central de transiciones entre sectores.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * TransitionSystem orquesta todo el ciclo de vida de una transición:
 *
 *   1. DETECCIÓN (TransitionDetector)
 *      Encuentra entidades que cruzaron un borde automáticamente.
 *
 *   2. GATES (TransitionGate[])
 *      Evalúa gates registrados (portales, puertas, triggers).
 *
 *   3. VALIDACIÓN (TransitionValidator)
 *      Verifica que la posición destino no esté bloqueada.
 *
 *   4. RESOLUCIÓN (TransitionResolver)
 *      Si está bloqueada, busca posición alternativa o rechaza.
 *
 *   5. ESTILO (TransitionStyle)
 *      Gestiona el timing visual de la transición.
 *
 *   6. TRANSFERENCIA
 *      Mueve la entidad del mundo origen al mundo destino.
 *
 *   7. EVENTOS (GameEventBus)
 *      Publica eventos de inicio/fin/rechazo de transición.
 *
 * ── GENERACIÓN DE VECINOS ─────────────────────────────────────────────────
 * TransitionSystem genera automáticamente el sector destino si no existe
 * en el cache. Esta responsabilidad fue extraída de WorldTransitionService
 * (donde estaba mezclada con la transferencia).
 *
 * ── LO QUE NO HACE ────────────────────────────────────────────────────────
 * - No hace prewarming predictivo de vecinos (eso es WorldPrewarmService).
 * - No controla la cámara (eso es CameraSystem).
 * - No actualiza el sector activo de WorldManager directamente
 *   (publica OnWorldControllerSectorChanged para que WorldManager lo escuche).
 *
 * ── ACOPLAMIENTO MÍNIMO ───────────────────────────────────────────────────
 * TransitionSystem no conoce Player, Enemy ni ningún tipo concreto del Game.
 * El predicado de "world controller" se inyecta desde la capa de composición.
 *
 * ── TRANSICIONES PENDIENTES ───────────────────────────────────────────────
 * Cuando un TransitionStyle necesita tiempo antes de transferir (FADE, SCROLL),
 * el request queda "pendiente" hasta que el style dice que está listo.
 * Durante este tiempo la entidad permanece en el sector origen.
 */
public final class TransitionSystem {

    // ── Colaboradores ─────────────────────────────────────────────────────

    private final TransitionDetector  detector;
    private final TransitionValidator validator;
    private final TransitionResolver  resolver;
    private final WorldCache          cache;
    private final WorldGenerator      generator;
    private final Supplier<World>     currentWorldSupplier;

    /**
     * Registry global del universo.
     * Necesario para crear Worlds nuevos con el registry correcto.
     *
     * HRFC — World Lifecycle Integrity:
     * TransitionSystem necesita crear Worlds vecinos cuando detecta transiciones.
     * Esos Worlds deben nacer con el registry global correcto, no con uno temporal.
     */
    private final DynamicEntityRegistry globalDynamicRegistry;

    /** Gates registrados: portales, puertas, triggers explícitos. */
    private final List<TransitionGate> gates = new ArrayList<>();

    /** Transiciones en curso (con style animado pendiente de completar). */
    private final List<PendingTransition> pendingTransitions = new ArrayList<>();

    /** GameEventBus para publicar eventos de transición. */

    /**
     * Dimensiones del chunk activo, actualizadas en cada llamada a update().
     * Usadas por request() para acceder a las dimensiones sin pasar parámetros.
     */
    private int lastWorldWidth  = 1280;
    private int lastWorldHeight = 720;
    private final GameEventBus eventBus;

    // ── Constructor ───────────────────────────────────────────────────────

    /**
     * @param currentWorldSupplier proveedor del mundo activo (siempre dinámico)
     * @param cache                caché de mundos
     * @param generator            generador de mundos para crear sectores nuevos
     * @param globalDynamicRegistry registry global del universo (para crear Worlds correctos)
     * @param eventBus             bus de eventos para publicar TransitionEvent
     */
    public TransitionSystem(Supplier<World>  currentWorldSupplier,
                             WorldCache       cache,
                             WorldGenerator   generator,
                             DynamicEntityRegistry globalDynamicRegistry,
                             GameEventBus     eventBus) {
        this.currentWorldSupplier  = currentWorldSupplier;
        this.cache                 = cache;
        this.generator             = generator;
        this.globalDynamicRegistry = globalDynamicRegistry;
        this.eventBus              = eventBus;
        this.detector              = new TransitionDetector();
        this.validator             = new TransitionValidator();
        this.resolver              = new TransitionResolver();
    }

    // ── Configuración ─────────────────────────────────────────────────────

    /**
     * Configura el predicado que identifica el "world controller".
     * Cuando ese objeto transita, cambia el sector activo del motor.
     *
     * Llamar desde la capa de composición:
     *   transitionSystem.setControllerPredicate(obj -> obj instanceof Player);
     */
    public void setControllerPredicate(Predicate<GameObjects> predicate) {
        detector.setControllerPredicate(predicate);
    }

    /**
     * Registra un TransitionGate (portal, puerta, teleporter, trigger de zona).
     */
    public void addGate(TransitionGate gate) {
        gates.add(gate);
    }

    /**
     * Elimina un gate registrado.
     */
    public void removeGate(TransitionGate gate) {
        gates.remove(gate);
    }

    // ── Transición explícita ──────────────────────────────────────────────

    /**
     * Solicita una transición explícita (teleporte, habilidad, script, IA).
     * El request se procesa inmediatamente en el próximo update().
     *
     * @param request el request de transición a ejecutar.
     */
    public void request(TransitionRequest request) {
        processRequest(request, currentWorldSupplier.get(), lastWorldWidth, lastWorldHeight);
    }

    // ── Update ────────────────────────────────────────────────────────────

    /**
     * Evalúa y procesa todas las transiciones del tick actual.
     * Llamado por WorldManager una vez por tick.
     *
     * ── HRFC — Unified DeltaTime Migration ───────────────────────────────
     *
     * @param world        el mundo activo
     * @param currentCoord el sector activo
     * @param worldWidth   ancho lógico de cada sector
     * @param worldHeight  alto lógico de cada sector
     * @param deltaTime    tiempo del simulation step en segundos
     * @return nueva WorldCoordinator si el world controller cambió de sector, null si no
     */
    public WorldCoordinator update(World world,
                                    WorldCoordinator currentCoord,
                                    int worldWidth,
                                    int worldHeight,
                                    double deltaTime) {
        // Guardar dimensiones para request() que no recibe estos parámetros
        this.lastWorldWidth  = worldWidth;
        this.lastWorldHeight = worldHeight;
        WorldCoordinator result = null;

        // ── 1. Actualizar transiciones pendientes (con style animado) ─────
        result = updatePending(deltaTime, result);

        // ── 2. Detección automática por borde ─────────────────────────────
        List<TransitionRequest> detected = detector.detect(
            world, currentCoord, worldWidth, worldHeight
        );
        for (TransitionRequest req : detected) {
            WorldCoordinator changed = processRequest(req, world, worldWidth, worldHeight);
            if (changed != null) result = changed;
        }

        // ── 3. Evaluar gates registrados ──────────────────────────────────
        List<TransitionGate> activeGates = List.copyOf(gates);
        for (TransitionGate gate : activeGates) {
            if (!gate.isActive()) {
                gates.remove(gate);
                continue;
            }
            for (GameObjects obj : world.getDynamicEntityRegistry().getAll()) {
                TransitionRequest req = gate.evaluate(obj, currentCoord);
                if (req != null) {
                    WorldCoordinator changed = processRequest(req, world, worldWidth, worldHeight);
                    if (changed != null) result = changed;
                    gate.onTransitionExecuted(req);
                }
            }
        }

        return result;
    }

    // ── Estado de transición en curso ─────────────────────────────────────

    /**
     * Retorna el estilo activo de la transición en curso (para que el renderer
     * pueda aplicar el efecto visual correspondiente).
     *
     * @return el TransitionStyle activo, o null si no hay transición en curso.
     */
    public TransitionStyle getCurrentStyle() {
        return pendingTransitions.isEmpty()
               ? null
               : pendingTransitions.get(0).request.getStyle();
    }

    // ── Privado ───────────────────────────────────────────────────────────

    /**
     * Procesa un TransitionRequest: valida, resuelve, aplica estilo y transfiere.
     *
     * @return nueva WorldCoordinator si el world controller cambió de sector, null si no.
     */
    private WorldCoordinator processRequest(TransitionRequest request, World sourceWorld,
                                              int worldWidth, int worldHeight) {
        WorldCoordinator toSector = request.getToSector();
        if (toSector == null) return null;

        // Asegurar que el mundo destino existe — usa dimensiones pasadas como parámetro
        ensureWorldExists(toSector, worldWidth, worldHeight);

        World targetWorld;
        synchronized (cache) {
            targetWorld = cache.get(toSector);
        }
        if (targetWorld == null) return null;

        // Validar y resolver posición
        Optional<TransitionRequest> resolved = resolver.resolve(request, targetWorld, validator);

        if (resolved.isEmpty()) {
            // Rechazado por el resolver
            eventBus.post(new TransitionEvent.OnTransitionRejected(
                request.getSubject(),
                request.getFromSector(),
                toSector,
                "No se encontró posición libre en el sector destino."
            ));
            return null;
        }

        TransitionRequest finalRequest = resolved.get();
        TransitionStyle style = finalRequest.getStyle();

        // Publicar inicio de transición
        eventBus.post(new TransitionEvent.OnTransitionStarted(
            finalRequest.getSubject(),
            finalRequest.getFromSector(),
            toSector,
            style
        ));

        // Iniciar el style
        style.begin(finalRequest);

        if (style.readyToTransfer()) {
            // Transición instantánea: transferir directamente
            return executeTransfer(finalRequest, sourceWorld, targetWorld);
        } else {
            // Transición animada: encolar como pendiente
            pendingTransitions.add(new PendingTransition(finalRequest, sourceWorld));
            return null;
        }
    }

    /**
     * Actualiza las transiciones pendientes (con style animado).
     *
     * @param deltaTime tiempo del simulation step en segundos
     * @param current coordenada actual (puede ser modificada si algún controller transita)
     * @return nueva WorldCoordinator si algún world controller completó su transición.
     */
    private WorldCoordinator updatePending(double deltaTime, WorldCoordinator current) {
        WorldCoordinator result = current;
        List<PendingTransition> toRemove = new ArrayList<>();

        for (PendingTransition pending : pendingTransitions) {
            // HRFC — Unified DeltaTime Migration
            // Propagar deltaTime a estilos animados que lo requieran
            if (pending.request.getStyle() instanceof TransitionStyle.FadeTransitionStyle fade) {
                fade.update(deltaTime);
            } else {
                pending.request.getStyle().update();
            }

            if (pending.request.getStyle().readyToTransfer() && !pending.transferred) {
                // Es el momento de transferir
                World targetWorld;
                WorldCoordinator toSector = pending.request.getToSector();
                synchronized (cache) {
                    targetWorld = cache.get(toSector);
                }
                if (targetWorld != null) {
                    WorldCoordinator changed = executeTransfer(
                        pending.request, pending.sourceWorld, targetWorld
                    );
                    if (changed != null) result = changed;
                }
                pending.transferred = true;

                if (pending.request.getStyle() instanceof TransitionStyle.FadeTransitionStyle fade) {
                    fade.markTransferDone();
                }
            }

            if (pending.request.getStyle().isComplete()) {
                toRemove.add(pending);
            }
        }

        pendingTransitions.removeAll(toRemove);
        return result;
    }

    /**
     * Bookkeeping de cambio de chunk — ya NO transfiere entidades entre mundos.
     *
     * ── ETAPA 8 ────────────────────────────────────────────────────────────
     * Las entidades dinámicas viven en DynamicEntityRegistry, independiente
     * de cualquier World/chunk. No existe "mover una entidad de mundo A a B".
     * La posición de la entidad ya fue ajustada antes de llegar aquí (si aplica).
     *
     * Lo único que hace este método:
     *   1. Notifica OnTransitionCompleted (para efectos de audio/visual).
     *   2. Si es el world controller, notifica OnWorldControllerSectorChanged
     *      para que WorldManager actualice currentCoord (legacy).
     *
     * El CollisionsSystem.clearContactHistory() ya se llama en WorldManager
     * al detectar el cambio de sector, sin necesidad de acceder a containers.
     *
     * @return nueva WorldCoordinator si el world controller cambió, null si no.
     */
    private WorldCoordinator executeTransfer(TransitionRequest request,
                                              World sourceWorld,
                                              World targetWorld) {
        GameObjects subject = request.getSubject();
        WorldCoordinator toSector = request.getToSector();

        // ── ELIMINADO: sourceWorld.remove(subject) / targetWorld.add(subject) ──
        // Las entidades dinámicas viven en DynamicEntityRegistry.
        // No pertenecen estructuralmente a ningún world/chunk.
        // Cruzar un límite de chunk es solo un cambio de afiliación — no una transferencia.

        // ── ELIMINADO: getObjectsContainer().flush() ──
        // El flush ocurre en DynamicEntityRegistry.flush() al inicio del tick.

        // ── ELIMINADO: clearCollisionContactHistory() ──
        // WorldManager llama collisionsSystem.clearContactHistory() directamente.

        // Publicar evento de completitud (para audio, efectos, UI)
        eventBus.post(new TransitionEvent.OnTransitionCompleted(
            subject,
            request.getFromSector(),
            toSector
        ));

        // Notificar cambio de sector activo si es el world controller
        if (request.isWorldController()) {
            eventBus.post(new TransitionEvent.OnWorldControllerSectorChanged(
                subject,
                request.getFromSector(),
                toSector
            ));
            return toSector;
        }

        return null;
    }

    @SuppressWarnings({"deprecation", "removal"})
    private void ensureWorldExists(WorldCoordinator coord, int worldWidth, int worldHeight) {
        synchronized (cache) {
            if (!cache.contains(coord)) {
                // HRFC — World Lifecycle Integrity:
                // WorldGenerator.generate() crea un World con registry temporal.
                // Extraemos el chunk generado y creamos un World nuevo con el
                // registry global correcto.
                World tempWorld = generator.generate(worldWidth, worldHeight, coord);
                
                // Extraer el chunk generado
                Game.World.Chunk.Chunk generatedChunk = null;
                for (Game.World.Chunk.Chunk chunk : tempWorld.getChunkStorage().allChunks()) {
                    generatedChunk = chunk;
                    break; // Solo hay un chunk por World generado
                }
                
                // Crear World con el registry global correcto
                World properWorld = new World(worldWidth, worldHeight, coord, globalDynamicRegistry);
                if (generatedChunk != null) {
                    properWorld.addChunk(generatedChunk);
                }
                
                cache.put(properWorld);
            }
        }
    }

    // ── Clases internas ───────────────────────────────────────────────────

    private static final class PendingTransition {
        final TransitionRequest request;
        final World             sourceWorld;
        boolean                 transferred = false;

        PendingTransition(TransitionRequest request, World sourceWorld) {
            this.request     = request;
            this.sourceWorld = sourceWorld;
        }
    }

}
