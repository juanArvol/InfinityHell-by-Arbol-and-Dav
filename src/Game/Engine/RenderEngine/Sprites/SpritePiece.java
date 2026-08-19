package Game.Engine.RenderEngine.Sprites;

import Game.Engine.RenderEngine.Context.RenderCamera;
import Game.Engine.RenderEngine.Context.RenderContext;
import Game.Engine.RenderEngine.Contracts.Renderable;
import Game.Engine.RenderEngine.Culling.ViewportCuller;
import Game.Engine.RenderEngine.Strategies.ShadowStrategy;
import Game.Engine.RenderEngine.Strategies.SpriteDrawer;
import Game.Engine.RenderEngine.Transform.TransformData;
import Sprites.Core.Animation;
import Sprites.Core.SpriteFrame;
import Sprites.Core.SpriteHandle;

/**
 * SpriteComponent — pieza visual independiente del sistema "Lego".
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * Representa UNA parte visual de una entidad: cabeza, cuerpo, brazo, arma,
 * capa, sombra, etc. Tiene su propio SpriteHandle, TransformData, capa de
 * profundidad, visibilidad y estado de animación propio.
 *
 * ── INDEPENDENCIA ─────────────────────────────────────────────────────────
 * Cada SpriteComponent es completamente independiente:
 *   - Puede estar visible o invisible sin afectar los demás
 *   - Puede tener una animación diferente a la del cuerpo principal
 *   - Puede tener flip, tinte, escala propios
 *   - Puede ser intercambiado en runtime (cambiar arma, casco, ropa)
 *
 * ── USO DIRECTO ───────────────────────────────────────────────────────────
 * Como Component de un GameObjects directamente (render simple):
 *
 *   addComponent(new SpriteComponent(PlayerAssets.handle, "head")
 *       .withOffset(-2, -24)
 *       .withLayer(10)
 *       .withTransform(TransformData.builder().flipH(true).build()));
 *
 * ── USO DENTRO DE SpriteComposite ────────────────────────────────────────
 * SpriteComposite contiene una lista de SpriteComponent. Cada parte del
 * modelo se registra en el composite con addPart(). El composite propaga
 * la posición base del gameObject a cada SpriteComponent.
 *
 * ── ANIMACIÓN POR COMPONENTE ──────────────────────────────────────────────
 * Cada SpriteComponent puede reproducir su propia animación independiente:
 *   component.play("walk_right");
 *
 * El estado de reproducción (frameIndex, tick) vive en SpriteComponent.
 * La lógica de avance de frames es la misma que AnimationController.
 *
 * ── CULLING ───────────────────────────────────────────────────────────────
 * Antes de dibujar, se verifica si el componente es visible en el viewport.
 * Los componentes fuera de pantalla se omiten completamente.
 *
 * ── SOMBRA OPCIONAL ───────────────────────────────────────────────────────
 * Un ShadowStrategy puede adjuntarse al componente para dibujar una sombra
 * antes del sprite. Se activa con withShadow(strategy).
 */
public final class SpritePiece implements Renderable {

    // ── Identificación ────────────────────────────────────────────────────────

    /** ID legible para debug y lookup en SpriteComposite. */
    private final String partId;

    // ── Recurso visual ────────────────────────────────────────────────────────

    private SpriteHandle  handle;
    private TransformData transform = TransformData.IDENTITY;

    // ── Posición relativa al gameObject ───────────────────────────────────────

    /** Offset en píxeles respecto a la posición base del gameObject. */
    private int offsetX = 0;
    private int offsetY = 0;

    // ── Tamaño de render ──────────────────────────────────────────────────────

    /** 0 = usar el tamaño natural del frame. */
    private int renderWidth  = 0;
    private int renderHeight = 0;

    // ── Profundidad y visibilidad ─────────────────────────────────────────────

    /** Capa de render dentro del composite (mayor = encima). Default 0. */
    private int  layer   = 0;

    /** Si false, este componente no se dibuja. */
    private boolean visible = true;

    // ── Estado de animación local ─────────────────────────────────────────────

    private String    currentAnimKey  = null;
    private Animation currentAnim     = null;
    private int       frameIndex      = 0;
    private double    elapsed         = 0.0;  // ✅ HRFC: migrado a tiempo real

    /** Frame actual resuelto (puede venir de animación o del handle). */
    private SpriteFrame currentFrame  = null;

    // ── Sombra opcional ───────────────────────────────────────────────────────

    private ShadowStrategy shadowStrategy = null;

    // ── Contexto de render (inyectado por SpriteComposite o start()) ──────────

    /** Posición base del objeto al que pertenece este componente. */
    private double baseWorldX = 0;
    private double baseWorldY = 0;

    /** Dimensiones del framebuffer (para culling). Inyectadas por el sistema. */
    private int virtualWidth  = 1280;
    private int virtualHeight = 720;

    // ── Constructor ───────────────────────────────────────────────────────────

    /**
     * @param handle handle del sprite
     * @param partId identificador de la parte (ej: "head", "body", "weapon")
     */
    public SpritePiece(SpriteHandle handle, String partId) {
        this.handle = handle;
        this.partId = partId != null ? partId : "part";
        resolveDefaultFrame();
    }

    /**
     * Constructor sin ID explícito.
     */
    public SpritePiece(SpriteHandle handle) {
        this(handle, "sprite");
    }

    // ── Builder fluido ────────────────────────────────────────────────────────

    public SpritePiece withOffset(int ox, int oy) {
        this.offsetX = ox;
        this.offsetY = oy;
        return this;
    }

    public SpritePiece withLayer(int layer) {
        this.layer = layer;
        return this;
    }

    public SpritePiece withTransform(TransformData transform) {
        this.transform = transform != null ? transform : TransformData.IDENTITY;
        return this;
    }

    public SpritePiece withRenderSize(int w, int h) {
        this.renderWidth  = w;
        this.renderHeight = h;
        return this;
    }

    public SpritePiece withShadow(ShadowStrategy strategy) {
        this.shadowStrategy = strategy;
        return this;
    }

    public SpritePiece withVirtualSize(int vw, int vh) {
        this.virtualWidth  = vw;
        this.virtualHeight = vh;
        return this;
    }

    // ── API de animación ──────────────────────────────────────────────────────

    /**
     * Inicia o continúa una animación en este componente.
     * Idempotente: si ya se está reproduciendo, no reinicia.
     *
     * @param key clave de animación (ej: "idle", "walk_right")
     */
    public void play(String key) {
        if (key == null || handle == null) return;
        if (key.equals(currentAnimKey)) return; // ya reproduciéndose

        Animation anim = handle.getAnimation(key);
        if (anim == null) {
            System.err.println("[SpriteComponent:" + partId + "] Animación '" + key + "' no encontrada");
            return;
        }

        currentAnimKey = key;
        currentAnim    = anim;
        frameIndex     = 0;
        elapsed        = 0.0;
        currentFrame   = anim.getFirstFrame();
    }

    /**
     * Detiene la animación y vuelve al frame por defecto.
     */
    public void stopAnimation() {
        currentAnimKey = null;
        currentAnim    = null;
        frameIndex     = 0;
        elapsed        = 0.0;
        resolveDefaultFrame();
    }

    /**
     * Avanza el estado de animación usando tiempo real transcurrido.
     *
     * ── HRFC — Unified DeltaTime Migration & Temporal Model Completion ────
     *
     * Migrado desde frame-based (tick++) hacia time-based (elapsed += deltaTime).
     *
     * ANTES:
     *   tick++ cada frame
     *   if (tick >= frameTicks) → avanzar frame
     *   A 31 FPS: animación 93% más lenta
     *   A 120 FPS: animación 100% más rápida
     *
     * AHORA:
     *   elapsed += deltaTime
     *   if (elapsed >= frameSeconds) → avanzar frame
     *   A cualquier FPS: velocidad de animación consistente
     *
     * NOTA: Animation.ticksForFrame() aún retorna ticks (legado).
     * Convertimos ticks → segundos @ 60 FPS como valor esperado.
     * Migración futura: Animation debería aceptar duraciones en segundos.
     *
     * Llamado por SpriteSkeletonComponent.update(deltaTime).
     *
     * @param deltaTime tiempo real del simulation step en segundos
     */
    public void updateAnimation(double deltaTime) {
        if (currentAnim == null) return;

        // Duración del frame actual en ticks (legado)
        int frameTicks = currentAnim.ticksForFrame(frameIndex);
        
        // Convertir ticks → segundos (asumiendo 60 FPS como tick-base)
        // TODO HRFC: migrar Animation a duraciones en segundos
        double frameSeconds = frameTicks / 60.0;

        elapsed += deltaTime;
        
        if (elapsed >= frameSeconds) {
            elapsed -= frameSeconds;  // mantener excedente para precisión
            if (!currentAnim.isFinished(frameIndex)) {
                frameIndex = currentAnim.nextIndex(frameIndex);
            }
        }
        
        currentFrame = currentAnim.getFrame(frameIndex);
    }

    // ── Renderable ────────────────────────────────────────────────────────────

    @Override
    public void render(RenderContext ctx, RenderCamera camera) {
        if (!visible) return;

        SpriteFrame frame = resolveFrame();
        if (frame == null || !frame.isValid()) return;

        int rw = renderWidth  > 0 ? renderWidth  : frame.getWidth();
        int rh = renderHeight > 0 ? renderHeight : frame.getHeight();

        if (rw <= 0 || rh <= 0) return;

        // Posición en pantalla
        int screenX = (int)(baseWorldX - camera.getX()) + offsetX;
        int screenY = (int)(baseWorldY - camera.getY()) + offsetY;

        // Aplicar offset del TransformData
        if (transform.hasOffset()) {
            screenX += transform.offsetX;
            screenY += transform.offsetY;
        }

        // Culling: skip si completamente fuera del viewport
        if (!ViewportCuller.isVisibleOnScreen(screenX, screenY, rw, rh,
                virtualWidth, virtualHeight)) {
            return;
        }

        // Dibujar via SpriteDrawer (gestiona todas las estrategias)
        SpriteDrawer.INSTANCE.draw(
            ctx.getGraphics2D(),
            frame,
            screenX, screenY,
            rw, rh,
            transform,
            shadowStrategy
        );
    }

    // ── Posición base (inyectada por SpriteComposite) ─────────────────────────

    /**
     * Actualiza la posición base del gameObject al que pertenece este componente.
     * Llamado por SpriteComposite cada frame antes del render.
     */
    public void setBasePosition(double worldX, double worldY) {
        this.baseWorldX = worldX;
        this.baseWorldY = worldY;
    }

    // ── Intercambio en runtime ────────────────────────────────────────────────

    /**
     * Reemplaza el handle de este componente.
     * Permite cambiar arma, casco, ropa sin recrear la entidad.
     */
    public void setHandle(SpriteHandle handle) {
        this.handle       = handle;
        this.currentAnim  = null;
        this.currentAnimKey = null;
        this.frameIndex   = 0;
        this.elapsed      = 0.0;
        resolveDefaultFrame();
    }

    /**
     * Reemplaza el TransformData.
     */
    public void setTransform(TransformData transform) {
        this.transform = transform != null ? transform : TransformData.IDENTITY;
    }

    // ── Estado ────────────────────────────────────────────────────────────────

    public String    getPartId()        { return partId;        }
    public int       getLayer()         { return layer;         }
    public boolean   isVisible()        { return visible;       }
    public void      setVisible(boolean v) { this.visible = v;  }
    public TransformData getTransform() { return transform;     }
    public SpriteHandle  getHandle()    { return handle;        }
    public String    getCurrentAnimKey(){ return currentAnimKey;}

    // ── Privados ──────────────────────────────────────────────────────────────

    private void resolveDefaultFrame() {
        currentFrame = (handle != null && handle.isValid())
            ? handle.resolveDefault()
            : SpriteFrame.empty();
    }

    private SpriteFrame resolveFrame() {
        if (currentFrame != null && currentFrame.isValid()) return currentFrame;
        return (handle != null && handle.isValid()) ? handle.resolveDefault() : null;
    }
}
