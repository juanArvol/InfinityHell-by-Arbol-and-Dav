package Game.Engine.RenderEngine.Context;

import Game.Engine.Camera.GameCamera;
import Game.Engine.GameMath.Logic2D.Vector2D;

/**
 * Snapshot de posición de cámara para el pipeline de render.
 *
 * ── Qué es y por qué existe ────────────────────────────────────────────────
 *
 * Los componentes de render ({@link Game.Engine.RenderEngine.Contracts.Renderable},
 * {@link Game.Engine.RenderEngine.Contracts.DebugRenderable}) calculan la posición
 * en pantalla de cada objeto restando el offset de cámara:
 *
 *   screenX = worldX - camera.getX()
 *   screenY = worldY - camera.getY()
 *
 * Esta clase captura ese par de valores (x, y) en el momento en que
 * {@link Game.Engine.RenderEngine.Scene.SceneRenderer} inicia el frame. Es un
 * value object de solo lectura: no puede modificarse, no tiene identidad propia,
 * y no representa la cámara autoritativa del Engine.
 *
 * La cámara autoritativa es {@link GameCamera} (paquete Game.Engine.Camera).
 * RenderCamera es un adaptador que expone exclusivamente lo que el pipeline
 * de render necesita.
 *
 * ── Por qué no se elimina este adaptador ─────────────────────────────────
 *
 * Los componentes de render aplican el offset de cámara manualmente:
 *   int x = (int)(pos.getX() - camera.getX()) + offsetX;
 *
 * Migrar a un sistema donde {@link RenderContext#withCamera(GameCamera)}
 * aplica la transformación completa (translación + zoom + rotación) requiere
 * cambiar las firmas de {@link Game.Engine.RenderEngine.Contracts.Renderable}
 * y {@link Game.Engine.RenderEngine.Contracts.DebugRenderable} y actualizar todos
 * sus implementadores. Ese cambio pertenece a una refactorización posterior
 * del sistema de render.
 *
 * Mientras la migración no ocurra, este adaptador es necesario y correcto
 * para el caso zoom=1, rotation=0 que es el modo de operación actual.
 *
 * ── Limitación conocida: zoom y rotación ─────────────────────────────────
 *
 * RenderCamera solo captura (x, y). Si {@link GameCamera} tiene zoom ≠ 1
 * o rotation ≠ 0, los componentes de render que usen únicamente
 * {@code camera.getX()} y {@code camera.getY()} producirán resultados
 * incorrectos — solo compensan la traslación, no la escala ni la rotación.
 *
 * El path correcto para zoom y rotación es:
 *   {@link RenderContext#withCamera(GameCamera)} → aplica {@code getViewTransform()}.
 *
 * Esta limitación no afecta el gameplay actual (zoom=1, sin rotación).
 * Cuando se implemente zoom dinámico o shake con rotación, la migración
 * de Renderable / DebugRenderable debe completarse primero.
 *
 * ── Uso correcto ──────────────────────────────────────────────────────────
 *
 * {@link Game.Engine.RenderEngine.Scene.SceneRenderer} crea una instancia por frame:
 *
 *   RenderCamera renderCamera = new RenderCamera(engineCamera);
 *   renderSystem.render(objects, ctx, renderCamera);
 *
 * RenderCamera es barato (dos doubles). No almacenar referencias de larga
 * duración: crear una instancia nueva por frame a partir del estado
 * actual de GameCamera.
 *
 * ── Separación de conceptos ───────────────────────────────────────────────
 *
 *   GameCamera    → cámara del Engine: estado autoritativo (posición, zoom,
 *                   rotación, lerp, clamp). Vive en Game.Engine.Camera.
 *
 *   RenderCamera  → adaptador de render: snapshot (x, y) de solo lectura
 *                   para el pipeline de componentes. Vive en
 *                   Game.Engine.RenderEngine.Context.
 *
 *   [futuro]      → cámara de gameplay: entidad que participa en mecánicas,
 *                   bosses, cinemáticas, triggers. Vivirá en Game.Gameplay.Camera.
 *                   No representa ni a GameCamera ni a RenderCamera.
 *
 * ── Historial de paquete ─────────────────────────────────────────────────
 *
 * MIGRADO DESDE: Game.Engine.RenderEngine (raíz del módulo)
 * MOTIVO: reorganización RFC RenderEngine — los objetos de trabajo del frame
 * se agrupan en el subpaquete Context.
 */
public class RenderCamera {

    private final double x;
    private final double y;
    
    // ── HRFC: Reference to source GameCamera for optimized renderers ─────
    /**
     * Referencia a la GameCamera original (puede ser null si se construyó sin ella).
     * Expuesta para optimized renderers que necesitan acceso a la cámara completa.
     */
    private final GameCamera sourceCamera;

    // ── Constructor desde GameCamera (flujo normal) ───────────────────────────

    /**
     * Captura la posición actual de GameCamera como snapshot de solo lectura.
     *
     * Llamar una vez al inicio del frame. Si GameCamera cambia después,
     * esta instancia NO se actualiza — crear una nueva instancia por frame.
     */
    public RenderCamera(GameCamera source) {
        this.x = source.getX();
        this.y = source.getY();
        this.sourceCamera = source;
    }

    // ── Constructor en el origen ──────────────────────────────────────────────

    /**
     * Snapshot en la posición (0, 0): sin offset de cámara.
     * Útil en tests y en contextos donde no hay desplazamiento de vista.
     */
    public RenderCamera() {
        this.x = 0;
        this.y = 0;
        this.sourceCamera = null;
    }

    // ── Constructor directo por valores (package-private) ─────────────────────

    /**
     * Snapshot en la posición indicada.
     * Package-private: uso interno del Engine y tests unitarios.
     */
    RenderCamera(double x, double y) {
        this.x = x;
        this.y = y;
        this.sourceCamera = null;
    }

    // ── API de render (solo lectura) ──────────────────────────────────────────

    /**
     * Offset X de la vista. {@code screenX = worldX - camera.getX()}
     * Válido únicamente con zoom=1, rotation=0. Ver limitación en Javadoc de clase.
     */
    public double getX() { return x; }

    /**
     * Offset Y de la vista. {@code screenY = worldY - camera.getY()}
     * Válido únicamente con zoom=1, rotation=0. Ver limitación en Javadoc de clase.
     */
    public double getY() { return y; }

    /**
     * Copia defensiva de la posición como Vector2D.
     * Preferir {@link #getX()} / {@link #getY()} cuando solo se necesita el escalar.
     */
    public Vector2D getPosition() {
        return new Vector2D(x, y);
    }
    
    /**
     * Retorna la GameCamera original si está disponible.
     * 
     * ── HRFC: Optimized Renderers ─────────────────────────────────────────
     * Agregado para permitir que optimized renderers (BulletBatchRenderer)
     * accedan a la GameCamera completa cuando necesitan más que solo (x,y).
     * 
     * @return GameCamera original, o null si se construyó sin ella
     */
    public GameCamera getGameCamera() {
        return sourceCamera;
    }
}
