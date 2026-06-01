package Game.Engine.Components.Collisions;

import Game.Engine.Component;
import Game.Engine.Colisions.Filter.CollisionProfile;
import Game.Engine.Colisions.Filter.Layer;

import java.awt.Rectangle;

/**
 * Define el área de colisión de un objeto y su perfil (layer/mask).
 *
 * ── Diseño limpio ──────────────────────────────────────────────────────────
 * El ColliderComponent ya NO sincroniza con HitBoxComponent automáticamente
 * en cada update(). Esa sincronización causaba que el collider se recalculara
 * cada frame incluso en objetos estáticos, y generaba offsets incorrectos
 * cuando la HitBox tenía offsets propios.
 *
 * Ahora el tamaño se define explícitamente al crear el collider o al llamar
 * setSize(). Si querés sincronizar con la HitBox, hacelo una vez en el
 * constructor del objeto.
 *
 * ── Cómo usarlo ────────────────────────────────────────────────────────────
 *   // Objeto con tamaño explícito y perfil de jugador:
 *   ColliderComponent col = new ColliderComponent(15, 24, CollisionProfile.PLAYER);
 *   addComponent(col);
 *
 *   // Objeto estático con tamaño del bloque:
 *   ColliderComponent col = new ColliderComponent(width, height, CollisionProfile.WORLD);
 *   addComponent(col);
 */
public class ColliderComponent extends Component {

    public enum Type { SOLID, TRIGGER }

    private int width;
    private int height;
    private int offsetX;
    private int offsetY;

    private CollisionProfile profile;
    private Type type = Type.SOLID;

    // ── Constructores ──────────────────────────────────────────────────────

    /** Collider sin tamaño definido (se fija con setSize antes de usar). */
    public ColliderComponent() {
        this(0, 0, 0, 0, new CollisionProfile(Layer.WORLD, Layer.WORLD));
    }

    /** Collider con tamaño y perfil. Offset = (0,0). */
    public ColliderComponent(int width, int height, CollisionProfile profile) {
        this(width, height, 0, 0, profile);
    }

    /** Collider con tamaño, offset y perfil. */
    public ColliderComponent(int width, int height, int offsetX, int offsetY, CollisionProfile profile) {
        this.width   = width;
        this.height  = height;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.profile = profile;
    }

    // ── Bounds ────────────────────────────────────────────────────────────

    /**
     * Rectángulo de colisión en coordenadas de mundo.
     * Se recalcula cada vez que se llama (no se cachea — el objeto puede moverse).
     */
    public Rectangle getBounds() {
        var pos = gameObject.getTransform().getPosition();
        return new Rectangle(
                (int) pos.getX() + offsetX,
                (int) pos.getY() + offsetY,
                width,
                height
        );
    }

    // ── Perfil ────────────────────────────────────────────────────────────

    public CollisionProfile getProfile() { return profile; }
    public void setProfile(CollisionProfile profile) { this.profile = profile; }

    /** Shorthand para saber si este collider puede chocar con otro. */
    public boolean canCollideWith(ColliderComponent other) {
        return this.profile.canCollideWith(other.profile);
    }

    // ── Tamaño y offset ───────────────────────────────────────────────────

    public void setSize(int w, int h)       { this.width = w; this.height = h; }
    public void setOffset(int ox, int oy)   { this.offsetX = ox; this.offsetY = oy; }

    public int getWidth()   { return width; }
    public int getHeight()  { return height; }
    public int getOffsetX() { return offsetX; }
    public int getOffsetY() { return offsetY; }

    // ── Tipo ──────────────────────────────────────────────────────────────

    public Type getType()       { return type; }
    public void setType(Type t) { this.type = t; }
    public boolean isTrigger()  { return type == Type.TRIGGER; }
    public boolean isSolid()    { return type == Type.SOLID; }

    // ── Layer/Mask directo (compatibilidad) ───────────────────────────────

    public int getLayer() { return profile.layer; }
    public int getMask()  { return profile.mask; }
}
