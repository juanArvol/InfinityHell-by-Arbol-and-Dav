package Game.World.Core;

import Game.World.Generator.WorldGenerator;
import Game.Engine.GameObjects;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Gestiona los mundos del juego, su caché y las transiciones entre ellos.
 *
 * ─── FIX LAG AL CAMBIAR DE MUNDO ────────────────────────────────────────────
 *
 * PROBLEMA ORIGINAL:
 *   handleTransfers() generaba el mundo destino de forma SÍNCRONA en el hilo
 *   del game loop, bloqueando el render completo durante la generación.
 *   Esto causaba el cuelgue/lag visible al cruzar el borde del mapa.
 *
 * SOLUCIÓN — PRE-GENERACIÓN ASÍNCRONA:
 *   1. prewarmNeighbors() se llama cada frame para detectar si el jugador
 *      está cerca de algún borde (dentro del umbral PREWARM_THRESHOLD).
 *   2. Si el vecino en esa dirección aún no está en caché, se genera en un
 *      hilo background (ExecutorService de un solo hilo).
 *   3. Cuando el jugador realmente cruza el borde, el mundo ya está listo
 *      → handleTransfers() simplemente lo recupera del caché (sin bloquear).
 *
 *   El acceso al caché desde el background thread está sincronizado con
 *   `synchronized (cache)` para evitar condiciones de carrera.
 *
 * OTRAS CORRECCIONES:
 *   - double flush() eliminado: solo se llama flush() en el mundo actual
 *     tras las transferencias, ya que el mundo destino aún no es el
 *     currentWorld cuando se añaden objetos.
 *   - El ExecutorService se cierra correctamente con shutdown().
 *
 * ─── FIX BUG-007 (mantenido) ─────────────────────────────────────────────────
 *   update() ya NO escala posiciones de objetos. El mundo tiene coordenadas
 *   lógicas fijas. La cámara y el render se adaptan al tamaño de pantalla.
 */
public class WorldManager {

    // Umbral en píxeles lógicos: si el jugador está a menos de esta distancia
    // de un borde, se pre-genera el mundo vecino en background.
    private static final int PREWARM_THRESHOLD = 300;

    private static WorldManager instance;

    private final WorldCache       cache     = new WorldCache();
    private final WorldGenerator   generator = new WorldGenerator();

    // Hilo único de background para generación asíncrona de vecinos.
    // Un solo hilo es suficiente y evita saturar la CPU con generaciones paralelas.
    private final ExecutorService bgExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "WorldPrewarm");
        t.setDaemon(true);   // no bloquea el cierre de la JVM
        t.setPriority(Thread.MIN_PRIORITY);
        return t;
    });

    private WorldCoordinator currentCoord;

    private int logicalWidth;
    private int logicalHeight;

    private WorldManager(int width, int height) {
        this.logicalWidth  = width;
        this.logicalHeight = height;
        currentCoord = new WorldCoordinator(0, 0);
        regenerateAll();
    }

    public static void init(int width, int height) {
        if (instance == null) {
            instance = new WorldManager(width, height);
        }
    }

    public static WorldManager getInstance() {
        return instance;
    }

    public World getCurrentWorld() {
        synchronized (cache) {
            if (!cache.contains(currentCoord)) {
                World world = generator.generate(logicalWidth, logicalHeight, currentCoord);
                cache.put(world);
            }
            return cache.get(currentCoord);
        }
    }

    /**
     * FIX BUG-007: ya NO escala posiciones de objetos al actualizar.
     * Ahora también dispara la pre-generación de vecinos.
     */
    public void update(int screenWidth, int screenHeight) {
        World world = getCurrentWorld();
        world.update();

        // Pre-generar vecinos ANTES de que el jugador llegue al borde
        prewarmNeighbors(world);

        handleTransfers(world);
    }

    /**
     * FIX REFACTOR DISPLAY: recibe Graphics2D en lugar de Graphics.
     */
    public void draw(Graphics2D g) {
        getCurrentWorld().draw(g);
    }

    /**
     * Resize del mundo lógico (solo para regenerar mundos futuros).
     * NO mueve objetos existentes.
     */
    public void resize(int newWidth, int newHeight) {
        if (newWidth <= 0 || newHeight <= 0) return;
        this.logicalWidth  = newWidth;
        this.logicalHeight = newHeight;
    }

    /**
     * Cierra el executor de background. Llamar al cerrar la aplicación.
     */
    public void shutdown() {
        bgExecutor.shutdown();
    }

    // ─── Pre-calentamiento de vecinos ─────────────────────────────────────────

    /**
     * Detecta si el jugador está cerca de algún borde y pre-genera el mundo
     * vecino correspondiente en un hilo background.
     *
     * La generación es asíncrona: si el jugador nunca llega a cruzar ese borde,
     * el mundo generado queda en caché sin costo adicional (es pequeño en memoria).
     * Si llega a cruzarlo, el mundo ya está listo y handleTransfers() no bloquea.
     */
    private void prewarmNeighbors(World world) {
        // Buscar el jugador en el mundo actual
        GameObjects player = findPlayer(world);
        if (player == null) return;

        var pos = player.getTransform().getPosition();
        double px = pos.getX();
        double py = pos.getY();

        // Revisar los 4 vecinos cardinales
        if (px < PREWARM_THRESHOLD)                      scheduleNeighbor(-1,  0);
        if (px > logicalWidth  - PREWARM_THRESHOLD)      scheduleNeighbor( 1,  0);
        if (py < PREWARM_THRESHOLD)                      scheduleNeighbor( 0, -1);
        if (py > logicalHeight - PREWARM_THRESHOLD)      scheduleNeighbor( 0,  1);
    }

    /**
     * Envía la generación del vecino (dx,dy) al hilo background si aún no
     * está en caché. Sincronizado para evitar condición de carrera entre el
     * game loop y el hilo de generación.
     */
    private void scheduleNeighbor(int dx, int dy) {
        WorldCoordinator neighborCoord = new WorldCoordinator(
            currentCoord.x() + dx,
            currentCoord.y() + dy
        );

        synchronized (cache) {
            if (cache.contains(neighborCoord)) return; // ya está listo
        }

        // Capturar valores para lambda (deben ser effectively final)
        final int w = logicalWidth;
        final int h = logicalHeight;

        bgExecutor.submit(() -> {
            World generated = generator.generate(w, h, neighborCoord);
            synchronized (cache) {
                // Verificar de nuevo dentro del lock (doble check)
                if (!cache.contains(neighborCoord)) {
                    cache.put(generated);
                }
            }
        });
    }

    // ─── Transferencia de objetos entre mundos ────────────────────────────────

    private void handleTransfers(World world) {
        List<GameObjects> toTransfer = new ArrayList<>();

        for (var obj : world.getObjectsContainer().getObjects()) {
            var pos = obj.getTransform().getPosition();
            if (pos.getX() < 0 || pos.getX() > logicalWidth ||
                pos.getY() < 0 || pos.getY() > logicalHeight) {
                toTransfer.add(obj);
            }
        }

        for (var obj : toTransfer) {
            var pos = obj.getTransform().getPosition();

            int dx = 0, dy = 0;
            if (pos.getX() < 0)                  dx = -1;
            else if (pos.getX() >= logicalWidth)  dx =  1;
            if (pos.getY() < 0)                  dy = -1;
            else if (pos.getY() >= logicalHeight) dy =  1;

            WorldCoordinator nextCoord = new WorldCoordinator(
                currentCoord.x() + dx,
                currentCoord.y() + dy
            );

            // Garantía: si el vecino no estaba listo (llegó antes del prewarm),
            // se genera aquí de forma síncrona como fallback.
            synchronized (cache) {
                if (!cache.contains(nextCoord)) {
                    cache.put(generator.generate(logicalWidth, logicalHeight, nextCoord));
                }
            }

            World nextWorld;
            synchronized (cache) {
                nextWorld = cache.get(nextCoord);
            }

            double newX = pos.getX();
            double newY = pos.getY();
            if (dx != 0) newX = (dx > 0) ? newX - logicalWidth  : newX + logicalWidth;
            if (dy != 0) newY = (dy > 0) ? newY - logicalHeight : newY + logicalHeight;

            pos.setX(newX);
            pos.setY(newY);

            world.remove(obj);
            nextWorld.add(obj);

            if (obj instanceof Game.Player.Player) {
                currentCoord = nextCoord;
            }
        }

        // FIX: solo flush() del mundo actual. El mundo destino no necesita flush
        // aquí porque su cola de pendientes fue modificada con add(), no remove().
        world.getObjectsContainer().flush();

        // Si hubo cambio de mundo, flush del nuevo mundo actual también
        if (!toTransfer.isEmpty()) {
            getCurrentWorld().getObjectsContainer().flush();
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /** Busca al jugador (Player) entre los objetos del mundo. */
    private GameObjects findPlayer(World world) {
        for (var obj : world.getObjectsContainer().getObjects()) {
            if (obj instanceof Game.Player.Player) return obj;
        }
        return null;
    }

    private void regenerateAll() {
        synchronized (cache) {
            cache.clear();
            cache.put(generator.generate(logicalWidth, logicalHeight, currentCoord));
        }
    }
}
