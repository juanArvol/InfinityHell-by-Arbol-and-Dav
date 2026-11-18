package Game.Engine.Entity.Components.Collisions;

import Game.Engine.Colisions.Filter.CollisionProfile;
import Game.Engine.Colisions.Filter.Layer;
import Game.Engine.Component;
import Game.Engine.GameMath.Logic2D.RectangleH;

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

    private double width;
    private double height;
    private double offsetX;
    private double offsetY;

    private CollisionProfile profile;
    
    /**
     * Si true, este collider NO resuelve físicamente las colisiones (no aplica
     * resolución de velocidad ni ContactState), pero SÍ genera eventos de colisión.
     * Útil para sensores, zonas de daño, pickups, etc.
     * 
     * Los objetos con isSensor=true siguen usando la misma detección swept que
     * todos los demás — la única diferencia es que no modifican la velocidad al colisionar.
     */
    private boolean isSensor = false;

    // ── Constructores ──────────────────────────────────────────────────────

    /** Collider sin tamaño definido (se fija con setSize antes de usar). */
    public ColliderComponent() {
        this(0, 0, 0, 0, new CollisionProfile(Layer.WORLD, Layer.WORLD));
    }

    /** Collider con tamaño y perfil. Offset = (0,0). */
    public ColliderComponent(double width, double height, CollisionProfile profile) {
        this(width, height, 0, 0, profile);
    }

    /** Collider con tamaño, offset y perfil. */
    public ColliderComponent(double width, double height, double offsetX, double offsetY, CollisionProfile profile) {
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
     * Retorna RectangleH con precisión double para física exacta.
     * 
     * ── HRFC — DOD Integration ────────────────────────────────────────────
     * Lee posición directamente desde DOD storage cuando existe (Bullet, Enemy).
     * Esto evita sincronización constante DOD ↔ Transform.
     * 
     * PRIORIDAD DE LECTURA:
     *   1. DOD storage (Bullet/Enemy) - más eficiente
     *   2. Transform (objetos legacy) - fallback
     */
    public RectangleH getBounds() {
        double posX, posY;
        
        // Leer posición desde DOD si existe
        if (gameObject instanceof Game.Items.Types.Bullets.Definition.Bullet bullet) {
            // Bullet DOD: leer directamente de PrimitiveStorage
            var handle = bullet.getSimulationHandle();
            if (handle != null && handle.isValid()) {
                var storage = bullet.getEntityStore().getStorage();
                int idx = handle.index();
                posX = storage.positionsX()[idx];
                posY = storage.positionsY()[idx];
            } else {
                // Fallback si el handle no es válido
                var pos = gameObject.getTransform().getPosition();
                posX = pos.getX();
                posY = pos.getY();
            }
        } else if (gameObject instanceof Game.Enemys.Core.Enemy enemy) {
            // Enemy DOD: leer desde EntityStorage
            var handle = enemy.getSimulationHandle();
            if (handle != null && handle.isValid()) {
                var storage = enemy.getEntityStore().getStorage();
                int idx = handle.index();
                posX = storage.positionsX()[idx];
                posY = storage.positionsY()[idx];
            } else {
                // Fallback si el handle no es válido
                var pos = gameObject.getTransform().getPosition();
                posX = pos.getX();
                posY = pos.getY();
            }
        } else {
            // Objeto tradicional: leer desde Transform
            var pos = gameObject.getTransform().getPosition();
            posX = pos.getX();
            posY = pos.getY();
        }
        
        return new RectangleH(
                posX + offsetX,
                posY + offsetY,
                width,
                height
        );
    }

    // ── Perfil ────────────────────────────────────────────────────────────

    public CollisionProfile getProfile() { return profile; }
    public void setProfile(CollisionProfile profile) { this.profile = profile; }

    /** Shorthand para saber si este collider puede chocar con otro. 
     * 
     * ── HRFC FIX: Filtrado unificado ──────────────────────────────────────
     * Combina layer/mask Y CollisionMatrix en un solo check para evitar
     * redundancia y garantizar consistencia.
     */
    public boolean canCollideWith(ColliderComponent other) {
        // Check 1: layer/mask tradicional
        boolean layerCheck = this.profile.canCollideWith(other.profile);
        
        // Check 2: CollisionMatrix (si está habilitado)
        boolean matrixCheck = !Game.Engine.Colisions.Filter.CollisionMatrix.isEnabled()
            || Game.Engine.Colisions.Filter.CollisionMatrix.canCollide(
                this.profile.category, 
                other.profile.category
            );
        
        // Ambos deben pasar
        return layerCheck && matrixCheck;
    }

    // ── Tamaño y offset ───────────────────────────────────────────────────

    public void setSize(double w, double h)       { this.width = w; this.height = h; }
    public void setOffset(double ox, double oy)   { this.offsetX = ox; this.offsetY = oy; }

    public double getWidth()   { return width; }
    public double getHeight()  { return height; }
    public double getOffsetX() { return offsetX; }
    public double getOffsetY() { return offsetY; }

    // ── Sensor flag ───────────────────────────────────────────────────────

    public boolean isSensor()       { return isSensor; }
    public void setSensor(boolean s) { this.isSensor = s; }

    // ── Layer/Mask directo (compatibilidad) ───────────────────────────────

    public int getLayer() { return profile.layer; }
    public int getMask()  { return profile.mask; }
}