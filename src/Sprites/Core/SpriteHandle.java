package Sprites.Core;

/**
 * SpriteHandle — referencia desacoplada a un recurso visual.
 *
 * ── PROPÓSITO ─────────────────────────────────────────────────────────────
 * El Gameplay nunca debe conocer BufferedImage, Loader, rutas de archivos ni
 * ningún detalle de implementación del sistema gráfico.
 *
 * SpriteHandle es el "ticket" que el Gameplay entrega al RenderEngine.
 * Internamente apunta a una SpriteDefinition (con sus frames y animaciones),
 * pero el Gameplay solo ve un handle opaco.
 *
 * ── CICLO DE VIDA ─────────────────────────────────────────────────────────
 * 1. Assets.init() crea SpriteDefinitions y registra SpriteHandles.
 * 2. El Gameplay obtiene un SpriteHandle del registro de assets.
 * 3. El SpriteRenderer / AnimationController resuelven el SpriteFrame real
 *    cuando el RenderEngine lo necesita.
 *
 * ── RESOLUCIÓN ────────────────────────────────────────────────────────────
 * SpriteHandle.resolve() devuelve el frame actual para el RenderEngine.
 * El Gameplay no llama resolve() directamente — eso es trabajo del
 * SpriteRenderer / AnimationController.
 *
 * ── HANDLE VACÍO ─────────────────────────────────────────────────────────
 * SpriteHandle.EMPTY es un handle null-safe. El RenderEngine lo omite
 * sin lanzar excepciones. Úsalo en lugar de null.
 */
public final class SpriteHandle {

    /** Handle vacío null-safe. El RenderEngine lo ignora silenciosamente. */
    public static final SpriteHandle EMPTY = new SpriteHandle(null, "<empty>");

    private final SpriteDefinition definition;
    private final String           id;

    // ── Constructor ──────────────────────────────────────────────────────

    /**
     * @param definition definición del sprite (puede ser null para EMPTY)
     * @param id         identificador legible (para logs y debug)
     */
    public SpriteHandle(SpriteDefinition definition, String id) {
        this.definition = definition;
        this.id         = id != null ? id : "<unknown>";
    }

    // ── Resolución ────────────────────────────────────────────────────────

    /**
     * Resuelve el frame por defecto del sprite (primer frame, sin animación).
     * Usado por SpriteRenderer cuando no hay AnimationController activo.
     *
     * @return SpriteFrame actual o SpriteFrame.empty() si el handle es vacío
     */
    public SpriteFrame resolveDefault() {
        if (definition == null) return SpriteFrame.empty();
        return definition.getDefaultFrame();
    }

    /**
     * Resuelve un frame de animación específico por nombre de animación e índice.
     * Usado por AnimationController para obtener el frame correcto cada tick.
     *
     * @param animationKey clave de la animación (ej: "walkDere", "idle")
     * @param frameIndex   índice del frame dentro de la animación
     * @return SpriteFrame o SpriteFrame.empty() si no existe la animación
     */
    public SpriteFrame resolveFrame(String animationKey, int frameIndex) {
        if (definition == null) return SpriteFrame.empty();
        Animation anim = definition.getAnimation(animationKey);
        if (anim == null) return resolveDefault();
        return anim.getFrame(frameIndex);
    }

    /**
     * Obtiene la Animation por clave. Devuelve null si no existe.
     * El AnimationController usa esto para manejar su propio estado de tick.
     */
    public Animation getAnimation(String key) {
        if (definition == null) return null;
        return definition.getAnimation(key);
    }

    // ── Estado ────────────────────────────────────────────────────────────

    /** true si el handle apunta a una definición válida con al menos un frame. */
    public boolean isValid() {
        return definition != null && definition.hasFrames();
    }

    public SpriteDefinition getDefinition() { return definition; }
    public String           getId()         { return id;         }

    @Override
    public String toString() {
        return "SpriteHandle[" + id + ", valid=" + isValid() + "]";
    }
}
