package Game.Engine.Entity.Components.Collisions;

import Game.Engine.Colisions.Filter.CollisionProfile;
import Game.Engine.Colisions.Filter.Layer;
import Game.Engine.Component;
import Game.Engine.GameMath.Logic2D.RectangleH;
import Game.Engine.RenderEngine.Sprites.SizeSyncMode;

/**
 * Define el área de colisión de un objeto y su perfil (layer/mask).
 *
 * ── REFACTOR: Sistema de Sincronización Robusto ───────────────────────────
 *
 * PROBLEMA ANTERIOR:
 *   El SizeSyncMode vivía en SpriteRendererComponent y se ejecutaba una vez
 *   en start(). Luego cualquier código podía llamar a collider.setSize() y
 *   romper la sincronización. Era frágil y dependía del orden de llamadas.
 *
 * SOLUCIÓN NUEVA:
 *   ColliderComponent ahora conoce su SizeSyncMode y lo respeta de forma
 *   inmutable. Si está en modo COLLIDER_TO_SPRITE, el collider BLOQUEA
 *   llamadas a setSize() y obtiene su tamaño del SpriteRenderer cuando lo
 *   necesita.
 *
 * MODOS DE SINCRONIZACIÓN:
 *
 *   NONE:
 *     Tamaño manual. Llamar a setSize() libremente.
 *     El sprite y el collider son independientes.
 *
 *   COLLIDER_TO_SPRITE:
 *     El collider obtiene su tamaño del sprite automáticamente.
 *     setSize() lanza error si se intenta llamar.
 *     El offset sí se puede configurar.
 *
 *   SPRITE_TO_COLLIDER:
 *     El sprite se escala al tamaño del collider.
 *     setSize() funciona normalmente, el sprite se adapta.
 *
 * BENEFICIOS:
 *   ✅ No se puede romper la sincronización accidentalmente
 *   ✅ El modo es explícito y validado
 *   ✅ No depende del orden de llamadas
 *   ✅ Mensajes de error claros si intentas usar mal la API
 *
 * ── Cómo usarlo ────────────────────────────────────────────────────────────
 *
 *   // Tamaño manual (modo tradicional):
 *   ColliderComponent col = new ColliderComponent(15, 24, CollisionProfile.PLAYER);
 *   col.setSize(20, 30);  // ✅ Funciona
 *
 *   // Tamaño desde sprite (modo automático):
 *   ColliderComponent col = new ColliderComponent(SizeSyncMode.COLLIDER_TO_SPRITE, 
 *                                                   CollisionProfile.PLAYER);
 *   col.setSize(20, 30);  // ❌ Lanza IllegalStateException
 *   col.setOffset(4, 0);  // ✅ El offset sí funciona
 */
public class ColliderComponent extends Component {

    private double width;
    private double height;
    private double offsetX;
    private double offsetY;

    private CollisionProfile profile;
    
    /**
     * Modo de sincronización de tamaño con el SpriteRenderer.
     * Define si el tamaño es manual o se obtiene automáticamente del sprite.
     */
    private final SizeSyncMode syncMode;
    
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

    /** 
     * Collider sin tamaño definido (se fija con setSize antes de usar). 
     * Modo NONE (tamaño manual).
     */
    public ColliderComponent() {
        this(0, 0, 0, 0, new CollisionProfile(Layer.WORLD, Layer.WORLD));
    }

    /** 
     * Collider con tamaño y perfil. Offset = (0,0). 
     * Modo NONE (tamaño manual).
     */
    public ColliderComponent(double width, double height, CollisionProfile profile) {
        this(width, height, 0, 0, profile);
    }

    /** 
     * Collider con tamaño, offset y perfil. 
     * Modo NONE (tamaño manual).
     */
    public ColliderComponent(double width, double height, double offsetX, double offsetY, CollisionProfile profile) {
        this.width    = width;
        this.height   = height;
        this.offsetX  = offsetX;
        this.offsetY  = offsetY;
        this.profile  = profile;
        this.syncMode = SizeSyncMode.NONE;
    }

    // ── Constructores con SizeSyncMode (NUEVO) ────────────────────────────

    /**
     * Collider con modo de sincronización automática.
     * 
     * COLLIDER_TO_SPRITE:
     *   El collider obtiene su tamaño del sprite automáticamente.
     *   NO se puede llamar a setSize() después.
     *   
     * SPRITE_TO_COLLIDER:
     *   El sprite se escala al tamaño del collider.
     *   setSize() funciona normalmente.
     * 
     * @param syncMode modo de sincronización
     * @param profile perfil de colisión
     */
    public ColliderComponent(SizeSyncMode syncMode, CollisionProfile profile) {
        this(syncMode, 0, 0, profile);
    }

    /**
     * Collider con modo de sincronización y offset inicial.
     * 
     * @param syncMode modo de sincronización
     * @param offsetX offset horizontal inicial
     * @param offsetY offset vertical inicial
     * @param profile perfil de colisión
     */
    public ColliderComponent(SizeSyncMode syncMode, double offsetX, double offsetY, CollisionProfile profile) {
        if (syncMode == SizeSyncMode.COLLIDER_TO_SPRITE || 
            syncMode == SizeSyncMode.SPRITE_TO_COLLIDER_WITH_OFFSET) {
            // El tamaño se obtiene del sprite, inicializar en 0
            this.width   = 0;
            this.height  = 0;
        } else if (syncMode == SizeSyncMode.SPRITE_TO_COLLIDER) {
            // El tamaño se define manualmente, el sprite se adapta
            // Inicializar en 0, el usuario debe llamar a setSize()
            this.width   = 0;
            this.height  = 0;
        } else {
            // NONE - inicializar en 0, el usuario debe llamar a setSize()
            this.width   = 0;
            this.height  = 0;
        }
        
        this.offsetX  = offsetX;
        this.offsetY  = offsetY;
        this.profile  = profile;
        this.syncMode = syncMode;
    }

    // ── Bounds ────────────────────────────────────────────────────────────

    /**
     * Rectángulo de colisión en coordenadas de mundo.
     * Se recalcula cada vez que se llama (no se cachea — el objeto puede moverse).
     * Retorna RectangleH con precisión double para física exacta.
     * 
     * ── REFACTOR: Sincronización Automática ──────────────────────────────
     * 
     * Si syncMode == COLLIDER_TO_SPRITE, el tamaño se obtiene dinámicamente
     * del SpriteRendererComponent. Esto garantiza que el collider siempre
     * refleja el tamaño real del sprite, incluso si el sprite cambia.
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
        
        // Obtener tamaño según el modo de sincronización
        double effectiveWidth = width;
        double effectiveHeight = height;
        
        if (syncMode == SizeSyncMode.COLLIDER_TO_SPRITE || 
            syncMode == SizeSyncMode.SPRITE_TO_COLLIDER_WITH_OFFSET) {
            // Obtener tamaño desde el SpriteRenderer
            var spriteRenderer = gameObject.getComponent(
                Game.Engine.Entity.Components.Visuals.SpriteRendererComponent.class
            );
            if (spriteRenderer != null) {
                effectiveWidth = spriteRenderer.getRenderWidth();
                effectiveHeight = spriteRenderer.getRenderHeight();
            } else {
                // Fallback: usar el tamaño configurado manualmente (probablemente 0)
                // Advertir en consola si esto pasa
                if (effectiveWidth == 0 || effectiveHeight == 0) {
                    System.err.println("[ColliderComponent] ADVERTENCIA: SizeSyncMode es " 
                        + syncMode + " pero no hay SpriteRenderer. Usando tamaño 0×0.");
                }
            }
        }
        
        return new RectangleH(
                posX + offsetX,
                posY + offsetY,
                effectiveWidth,
                effectiveHeight
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

    /**
     * Configura el tamaño del collider manualmente.
     * 
     * VALIDACIÓN: Solo funciona si syncMode == NONE o SPRITE_TO_COLLIDER.
     * Si syncMode == COLLIDER_TO_SPRITE, lanza IllegalStateException porque
     * el tamaño debe venir del sprite, no configurarse manualmente.
     * 
     * @param w ancho del collider
     * @param h alto del collider
     * @throws IllegalStateException si syncMode no permite setSize manual
     */
    public void setSize(double w, double h) {
        if (syncMode == SizeSyncMode.COLLIDER_TO_SPRITE || 
            syncMode == SizeSyncMode.SPRITE_TO_COLLIDER_WITH_OFFSET) {
            throw new IllegalStateException(
                "ColliderComponent.setSize() no se puede llamar cuando syncMode=" + syncMode + ". " +
                "El tamaño se obtiene automáticamente del sprite. " +
                "Si necesitas control manual, usa SizeSyncMode.NONE en el constructor."
            );
        }
        this.width = w;
        this.height = h;
    }
    
    public void setOffset(double ox, double oy) { this.offsetX = ox; this.offsetY = oy; }

    /**
     * Obtiene el ancho del collider.
     * Si syncMode == COLLIDER_TO_SPRITE, retorna el tamaño actual del sprite.
     */
    public double getWidth() {
        if (syncMode == SizeSyncMode.COLLIDER_TO_SPRITE || 
            syncMode == SizeSyncMode.SPRITE_TO_COLLIDER_WITH_OFFSET) {
            var spriteRenderer = gameObject.getComponent(
                Game.Engine.Entity.Components.Visuals.SpriteRendererComponent.class
            );
            if (spriteRenderer != null) {
                return spriteRenderer.getRenderWidth();
            }
        }
        return width;
    }
    
    /**
     * Obtiene el alto del collider.
     * Si syncMode == COLLIDER_TO_SPRITE, retorna el tamaño actual del sprite.
     */
    public double getHeight() {
        if (syncMode == SizeSyncMode.COLLIDER_TO_SPRITE || 
            syncMode == SizeSyncMode.SPRITE_TO_COLLIDER_WITH_OFFSET) {
            var spriteRenderer = gameObject.getComponent(
                Game.Engine.Entity.Components.Visuals.SpriteRendererComponent.class
            );
            if (spriteRenderer != null) {
                return spriteRenderer.getRenderHeight();
            }
        }
        return height;
    }
    
    public double getOffsetX() { return offsetX; }
    public double getOffsetY() { return offsetY; }
    
    /** Retorna el modo de sincronización de este collider. */
    public SizeSyncMode getSyncMode() { return syncMode; }

    // ── Sensor flag ───────────────────────────────────────────────────────

    public boolean isSensor()       { return isSensor; }
    public void setSensor(boolean s) { this.isSensor = s; }

    // ── Layer/Mask directo (compatibilidad) ───────────────────────────────

    public int getLayer() { return profile.layer; }
    public int getMask()  { return profile.mask; }
}