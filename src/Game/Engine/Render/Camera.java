package Game.Engine.Render;

import Game.Engine.Camera.GameCamera;
import Game.Engine.GameMath.SpaceLogic.Logic2D.Vector2D;

/**
 * Vista de cámara para el sistema de render.
 *
 * ── HRFC-001: Camera como entidad de primer nivel del Engine ─────────────
 *
 * La cámara real del Engine es {@link GameCamera} (paquete Game.Engine.Camera).
 * Esta clase es un adaptador de solo lectura que expone los valores de
 * GameCamera con la API que los componentes de render existentes esperan
 * (getX(), getY()), sin exponer la API de escritura.
 *
 * ── Por qué este adaptador existe ────────────────────────────────────────
 *
 * Los componentes de render (SpriteRenderer, RectRenderer, ShadowComponent,
 * HitBoxComponent, etc.) reciben Camera en sus métodos render() y debugRender().
 * Cambiar esa firma en todos ellos sería un cambio de API masivo en este paso.
 *
 * El adaptador permite:
 *   1. Migrar GameCamera como entidad independiente sin romper nada.
 *   2. Los componentes existentes siguen funcionando sin modificación.
 *   3. Migración gradual: en el futuro, los componentes pueden recibir
 *      GameCamera directamente cuando la API esté estabilizada.
 *
 * ── Uso correcto ─────────────────────────────────────────────────────────
 *
 * WorldRenderer crea un Camera a partir de GameCamera antes de pasarlo
 * a los sistemas de render:
 *
 *   Camera renderCamera = new Camera(engineCamera);
 *   renderSystem.render(objects, ctx, renderCamera);
 *
 * Camera es un objeto barato (solo wrappea dos doubles). No almacenar
 * referencias de larga duración; crear uno por frame si es necesario.
 *
 * ── API de escritura ──────────────────────────────────────────────────────
 *
 * Los métodos centerOn() se conservan temporalmente por compatibilidad con
 * World.java, que aún los usa. Serán eliminados en el Paso 1b cuando World
 * deje de gestionar la cámara directamente.
 *
 * @deprecated Preferir {@link GameCamera} para nueva lógica de Engine.
 *             Este adaptador existe exclusivamente para compatibilidad de
 *             la API de render existente durante la migración HRFC-001.
 */
@Deprecated(since = "hrfc-001", forRemoval = false)
public class Camera {

    private final double x;
    private final double y;

    // ── Constructor desde GameCamera (nuevo flujo) ────────────────────────────

    /**
     * Crea un adaptador de solo lectura a partir del estado actual de GameCamera.
     *
     * Captura los valores en el momento de la construcción. Si GameCamera
     * cambia después, esta instancia NO se actualiza — usar una instancia
     * nueva por frame.
     */
    public Camera(GameCamera source) {
        this.x = source.getX();
        this.y = source.getY();
    }

    // ── Constructor por defecto (posición cero) ───────────────────────────────

    /**
     * Crea una cámara en la posición de origen (sin offset).
     * Usado internamente por World durante la transición HRFC-001.
     */
    public Camera() {
        this.x = 0;
        this.y = 0;
    }

    // ── Constructor directo por valores ──────────────────────────────────────

    /**
     * Crea una cámara con posición específica.
     * Usado por código de compatibilidad durante la transición.
     */
    Camera(double x, double y) {
        this.x = x;
        this.y = y;
    }

    // ── API de render (solo lectura) ──────────────────────────────────────────

    /** Offset X. screenX = worldX - camera.getX() */
    public double getX() { return x; }

    /** Offset Y. screenY = worldY - camera.getY() */
    public double getY() { return y; }

    /**
     * Copia defensiva de la posición.
     * Para acceso de solo lectura preferir getX()/getY().
     */
    public Vector2D getPosition() {
        return new Vector2D(x, y);
    }
}
