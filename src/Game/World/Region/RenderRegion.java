package Game.World.Region;

import Game.Engine.Camera.GameCamera;
import Game.Engine.GameObjects;
import Game.Engine.Entity.Components.Collisions.ColliderComponent;
import Game.Engine.RenderEngine.Culling.RenderBounds;
import Game.Engine.RenderEngine.Culling.RenderBoundsStrategy;
import Game.World.Chunk.Chunk;
import Game.World.Chunk.ChunkStorage;
import Game.World.Chunk.GlobalChunkResolver;
import Game.World.Core.WorldCoordinator;
import Game.World.Entity.DynamicEntityRegistry;
import Game.World.Index.WorldSpatialIndex;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Región de render — determina qué objetos deben dibujarse este frame.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * RenderRegion filtra los objetos que son visibles dentro del viewport
 * de la cámara. Es la fuente de verdad sobre qué se dibuja.
 *
 * ── DESACOPLAMIENTO DE SimulationRegion ───────────────────────────────────
 * RenderRegion es INDEPENDIENTE de SimulationRegion. Esto permite:
 *
 *   - Renderizar objetos decorativos que no se simulan (parallax, fondos,
 *     efectos estáticos lejanos).
 *   - Simular objetos fuera de cámara (IA de enemigos cercanos pero no visibles).
 *   - No asumir que RenderRegion ⊆ SimulationRegion.
 *
 * ── FUENTES DE OBJETOS ────────────────────────────────────────────────────
 * RenderRegion puede obtener objetos de dos fuentes:
 *
 *   1. WorldSpatialIndex.query(renderBounds)  → objetos estáticos de chunks
 *   2. DynamicEntityRegistry.getAll()         → entidades dinámicas
 *
 * Los objetos son filtrados por ViewportCuller (via SpriteRendererComponent)
 * durante el render — RenderRegion provee candidatos, el renderer descarta
 * los completamente fuera del viewport.
 *
 * ── CÁMARA Y COORDENADAS GLOBALES ────────────────────────────────────────
 * GameCamera opera en coordenadas globales. RenderRegion calcula los bounds
 * del viewport en coordenadas globales y los usa para consultar el índice.
 * No hay conversión local/global en runtime — todo es global.
 *
 * ── USO EN WorldManager ───────────────────────────────────────────────────
 *   // Al inicio del frame de render:
 *   renderRegion.update(camera, RenderBoundsStrategy.SYMMETRIC);
 *
 *   // Obtener objetos para el renderer:
 *   List<GameObjects> visible = renderRegion.getVisibleObjects(spatialIndex, dynamics);
 *   sceneRenderer.draw(visible, camera, g, vw, vh);
 */
public final class RenderRegion {

    // ── Estado del frame actual ───────────────────────────────────────────

    /** Bounds del viewport en coordenadas globales para el frame actual. */
    private RenderBounds currentBounds;

    /** La estrategia de cálculo de bounds (inyectable). */
    private RenderBoundsStrategy boundsStrategy;

    // ── Constructor ───────────────────────────────────────────────────────

    /**
     * Crea una RenderRegion con estrategia simétrica por defecto.
     * (Equivalente al comportamiento anterior de SceneRenderer.)
     */
    public RenderRegion() {
        this.boundsStrategy = RenderBoundsStrategy.SYMMETRIC;
        // Bounds inválidos hasta la primera llamada a update()
        this.currentBounds  = null;
    }

    /**
     * Crea una RenderRegion con una estrategia personalizada.
     *
     * @param strategy estrategia de cálculo de RenderBounds
     */
    public RenderRegion(RenderBoundsStrategy strategy) {
        this.boundsStrategy = (strategy != null) ? strategy : RenderBoundsStrategy.SYMMETRIC;
        this.currentBounds  = null;
    }

    // ── Configuración ─────────────────────────────────────────────────────

    /**
     * Cambia la estrategia de cálculo de bounds en runtime.
     * null restaura SYMMETRIC.
     *
     * @param strategy la nueva estrategia
     */
    public void setBoundsStrategy(RenderBoundsStrategy strategy) {
        this.boundsStrategy = (strategy != null) ? strategy : RenderBoundsStrategy.SYMMETRIC;
    }

    public RenderBoundsStrategy getBoundsStrategy() {
        return boundsStrategy;
    }

    // ── Actualización por frame ───────────────────────────────────────────

    /**
     * Recalcula los bounds del viewport según la cámara y la estrategia activa.
     * Debe llamarse una vez por frame antes de consultar getVisibleObjects().
     *
     * @param camera la GameCamera activa del Engine
     */
    public void update(GameCamera camera) {
        currentBounds = boundsStrategy.compute(camera);
    }

    // ── Consultas ─────────────────────────────────────────────────────────

    /**
     * Obtiene la lista de objetos visibles para el frame actual usando
     * el WorldSpatialIndex y el DynamicEntityRegistry.
     *
     * Los objetos estáticos se obtienen consultando el índice espacial
     * con los bounds actuales. Las entidades dinámicas se incluyen todas
     * (el culling fino las descarta durante el render en SpriteRendererComponent).
     *
     * @param spatialIndex índice espacial del mundo
     * @param dynamics     registro de entidades dinámicas
     * @return lista de candidatos visibles (sin garantía de precisión pixel-perfect;
     *         SpriteRendererComponent hace el culling final)
     */
    public List<GameObjects> getVisibleObjects(WorldSpatialIndex spatialIndex,
                                                DynamicEntityRegistry dynamics) {
        if (currentBounds == null) return List.of();

        List<GameObjects> result = new ArrayList<>();

        // Objetos estáticos de los chunks visibles
        java.awt.Rectangle queryRect = new java.awt.Rectangle(
            (int) currentBounds.left,
            (int) currentBounds.top,
            (int) currentBounds.getWidth(),
            (int) currentBounds.getHeight()
        );
        result.addAll(spatialIndex.query(queryRect));

        // Entidades dinámicas (culling por SpriteRenderer)
        result.addAll(dynamics.getAll());

        return result;
    }

    /**
     * Versión alternativa que lee directamente de ChunkStorage
     * (sin WorldSpatialIndex). Para uso durante la fase de transición.
     *
     * @param chunkWidth  ancho de cada chunk
     * @param chunkHeight alto de cada chunk
     * @param storage     almacén de chunks
     * @param dynamics    registro de entidades dinámicas
     * @return lista de candidatos visibles
     */
    public List<GameObjects> getVisibleObjectsFromStorage(int chunkWidth, int chunkHeight,
                                                           ChunkStorage storage,
                                                           DynamicEntityRegistry dynamics) {
        if (currentBounds == null) return List.of();

        List<GameObjects> result = new ArrayList<>();

        int cx0 = GlobalChunkResolver.firstChunkIndex(currentBounds.left,  chunkWidth);
        int cy0 = GlobalChunkResolver.firstChunkIndex(currentBounds.top,   chunkHeight);
        int cx1 = GlobalChunkResolver.lastChunkIndex(currentBounds.right,  chunkWidth);
        int cy1 = GlobalChunkResolver.lastChunkIndex(currentBounds.bottom, chunkHeight);

        for (int cx = cx0; cx <= cx1; cx++) {
            for (int cy = cy0; cy <= cy1; cy++) {
                Chunk chunk = storage.get(new WorldCoordinator(cx, cy));
                if (chunk != null && chunk.isLoaded()) {
                    result.addAll(chunk.getObjects());
                }
            }
        }

        result.addAll(dynamics.getAll());

        return Collections.unmodifiableList(result);
    }

    /**
     * True si un objeto es (potencialmente) visible según sus bounds.
     * Test rápido basado en ColliderComponent; sin ColliderComponent,
     * asume visible (conservador).
     *
     * @param obj el objeto a comprobar
     * @return true si puede ser visible
     */
    public boolean isVisible(GameObjects obj) {
        if (currentBounds == null) return false;

        ColliderComponent col = obj.getComponent(ColliderComponent.class);
        if (col == null) return true; // sin bounds conocidos → asumir visible

        var pos = obj.getTransform().getPosition();
        return currentBounds.isVisible(
            pos.getX(), pos.getY(),
            col.getWidth(), col.getHeight()
        );
    }

    // ── Acceso ────────────────────────────────────────────────────────────

    /**
     * Los RenderBounds actuales en coordenadas globales.
     * null si update() no fue llamado aún.
     *
     * @return bounds del viewport actual
     */
    public RenderBounds getCurrentBounds() {
        return currentBounds;
    }
}
