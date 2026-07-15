package Sprites.Core;

/**
 * AssetKey — clave tipada e inmutable para identificar un recurso gráfico.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * Desacopla el identificador lógico de un asset (su nombre en el juego)
 * de su ruta física en el classpath.
 *
 * El Gameplay y los Assets managers trabajan con AssetKeys. Solo AssetLoader
 * y AssetRegistry conocen los paths reales.
 *
 * ── CONVENCIÓN DE IDs ─────────────────────────────────────────────────────
 * Los IDs siguen el patrón "categoria.nombre":
 *
 *   "player.idle"
 *   "player.walk_right"
 *   "enemy.normal_0"
 *   "bullet.bala"
 *   "world.suelo"
 *   "obstacle.mondongo"
 *
 * ── TYPE ──────────────────────────────────────────────────────────────────
 * El tipo permite al AssetRegistry elegir la estrategia de carga correcta:
 *
 *   SINGLE_IMAGE  → un archivo PNG = un frame
 *   SPRITE_SHEET  → un archivo PNG = múltiples frames en cuadrícula
 *
 * En el futuro: ATLAS, ANIMATED_GIF, etc.
 */
public final class AssetKey {

    public enum Type {
        /** Un archivo de imagen = un solo frame. */
        SINGLE_IMAGE,
        /** Un archivo de imagen = cuadrícula de frames (SpriteSheet). */
        SPRITE_SHEET
    }

    private final String id;
    private final String path;
    private final Type   type;

    // Parámetros opcionales para SPRITE_SHEET
    private final int frameWidth;
    private final int frameHeight;

    // ── Constructores ────────────────────────────────────────────────────

    /**
     * Clave para imagen individual.
     *
     * @param id   identificador lógico (ej: "player.idle")
     * @param path path en classpath    (ej: "/Sprites/Source/player/eee.png")
     */
    public AssetKey(String id, String path) {
        this(id, path, Type.SINGLE_IMAGE, 0, 0);
    }

    /**
     * Clave para SpriteSheet.
     *
     * @param id          identificador lógico
     * @param path        path en classpath
     * @param frameWidth  ancho de cada frame
     * @param frameHeight alto de cada frame
     */
    public AssetKey(String id, String path, int frameWidth, int frameHeight) {
        this(id, path, Type.SPRITE_SHEET, frameWidth, frameHeight);
    }

    private AssetKey(String id, String path, Type type, int frameWidth, int frameHeight) {
        if (id   == null || id.isBlank())   throw new IllegalArgumentException("AssetKey: id no puede ser vacío");
        if (path == null || path.isBlank()) throw new IllegalArgumentException("AssetKey: path no puede ser vacío");
        this.id          = id;
        this.path        = path;
        this.type        = type;
        this.frameWidth  = frameWidth;
        this.frameHeight = frameHeight;
    }

    // ── Fábricas de conveniencia ─────────────────────────────────────────

    /** Atajo para imagen individual. */
    public static AssetKey single(String id, String path) {
        return new AssetKey(id, path);
    }

    /** Atajo para SpriteSheet. */
    public static AssetKey sheet(String id, String path, int fw, int fh) {
        return new AssetKey(id, path, fw, fh);
    }

    // ── Getters ───────────────────────────────────────────────────────────

    public String getId()          { return id;          }
    public String getPath()        { return path;        }
    public Type   getType()        { return type;        }
    public int    getFrameWidth()  { return frameWidth;  }
    public int    getFrameHeight() { return frameHeight; }

    public boolean isSingleImage() { return type == Type.SINGLE_IMAGE; }
    public boolean isSpriteSheet() { return type == Type.SPRITE_SHEET; }

    // ── Identidad ────────────────────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AssetKey other)) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() { return id.hashCode(); }

    @Override
    public String toString() {
        return "AssetKey[" + id + " → " + path + " (" + type + ")]";
    }
}
