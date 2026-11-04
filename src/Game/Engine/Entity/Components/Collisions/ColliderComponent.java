package Game.Engine.Entity.Components.Collisions;

import Game.Engine.Colisions.Filter.CollisionProfile;
import Game.Engine.Colisions.Filter.Layer;
import Game.Engine.Component;
import Game.Engine.GameMath.Logic2D.RectangleH;

/**
 * Define el área de colisión de un objeto y su perfil (layer/mask).
 *
 * ══════════════════════════════════════════════════════════════════════════
 * REFACTORIZACIÓN ARQUITECTÓNICA — Eliminación de Type.SOLID vs Type.TRIGGER
 * ══════════════════════════════════════════════════════════════════════════
 *
 * ANTES:
 *   - Type.SOLID → procesado en FASE 1 (eje por eje)
 *   - Type.TRIGGER → procesado en FASE 1B (2D)
 *   - Complejidad innecesaria, código duplicado
 *   - Bullets como "triggers" → física inconsistente
 *
 * AHORA:
 *   - UNA SOLA FASE para TODOS los objetos dinámicos
 *   - blockMovement flag simple: ¿este objeto bloquea a otros?
 *   - Bullets, player, enemies → MISMA física, MISMA detección
 *   - Solo el COMPORTAMIENTO difiere (via behaviors/components)
 *
 * FILOSOFÍA:
 *   La física es UNIVERSAL. La diferencia está en la RESPUESTA a la colisión,
 *   no en cómo se detecta. Un muro bloquea movimiento. Una bullet no bloquea
 *   a otros pero SÍ es bloqueada por muros. Simple.
 *
 * ── blockMovement Flag ────────────────────────────────────────────────────
 *
 * ¿Este objeto impide que otros se muevan a través de él?
 *
 * TRUE  → Muros, plataformas, suelo (objetos estáticos del mundo)
 * FALSE → Player, enemies, bullets, items (entidades dinámicas)
 *
 * Objetos con blockMovement=true suelen ser estáticos (velocity=0).
 * Objetos con blockMovement=false son dinámicos y se mueven libremente.
 *
 * ── Cómo usarlo ────────────────────────────────────────────────────────────
 *
 * // Muro (bloquea, estático):
 * ColliderComponent col = new ColliderComponent(width, height, CollisionProfile.WORLD);
 * col.setBlockMovement(true);
 *
 * // Bullet (dinámico, no bloquea):
 * ColliderComponent col = new ColliderComponent(4, 4, CollisionProfile.BULLET);
 * col.setBlockMovement(false); // default
 *
 * // Player (dinámico, no bloquea):
 * ColliderComponent col = new ColliderComponent(15, 24, CollisionProfile.PLAYER);
 * col.setBlockMovement(false); // default
 */
public class ColliderComponent extends Component {

    // ══════════════════════════════════════════════════════════════════════
    // ELIMINADO: public enum Type { SOLID, TRIGGER }
    // Ya no es necesario. Reemplazado por blockMovement flag.
    // ══════════════════════════════════════════════════════════════════════

    private double width;
    private double height;
    private double offsetX;
    private double offsetY;

    private CollisionProfile profile;
    
    /**
     * ¿Este objeto bloquea el movimiento de otros?
     * 
     * TRUE  → Muros, mundo estático (impide paso)
     * FALSE → Entidades dinámicas (player, bullets, enemies)
     * 
     * DEFAULT: false (la mayoría de objetos son dinámicos)
     */
    private boolean blockMovement = false;

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
     */
    public RectangleH getBounds() {
        var pos = gameObject.getTransform().getPosition();
        return new RectangleH(
                pos.getX() + offsetX,
                pos.getY() + offsetY,
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

    // ── Block Movement ────────────────────────────────────────────────────

    /**
     * ¿Este objeto bloquea el movimiento de otros objetos?
     * 
     * TRUE  → Muros, suelo, obstáculos estáticos
     * FALSE → Entidades dinámicas (player, bullets, enemies, items)
     */
    public boolean blocksMovement() { return blockMovement; }
    
    /**
     * Configura si este objeto bloquea movimiento.
     * 
     * Típicamente:
     * - Mundo/muros: setBlockMovement(true)
     * - Entidades: setBlockMovement(false)
     */
    public void setBlockMovement(boolean blocks) { this.blockMovement = blocks; }

    // ── Layer/Mask directo (compatibilidad) ───────────────────────────────

    public int getLayer() { return profile.layer; }
    public int getMask()  { return profile.mask; }
}