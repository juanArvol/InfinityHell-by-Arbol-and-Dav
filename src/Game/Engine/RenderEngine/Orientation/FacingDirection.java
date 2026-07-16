package Game.Engine.RenderEngine.Orientation;

/**
 * FacingDirection — dirección cardinal en la que mira una entidad.
 *
 * ── PROPÓSITO ─────────────────────────────────────────────────────────────
 * En un sistema pseudo-3D, el sprite que se muestra de una entidad depende
 * del ángulo relativo entre la cámara y la entidad. FacingDirection encapsula
 * ese resultado de forma discreta.
 *
 * ── CONVENCIÓN DE EJES ────────────────────────────────────────────────────
 * Coordenadas de mundo típicas de vista top-down:
 *   NORTH → la entidad mira hacia arriba en pantalla (hacia la cámara)
 *   SOUTH → la entidad mira hacia abajo en pantalla (alejándose de la cámara)
 *   EAST  → la entidad mira a la derecha
 *   WEST  → la entidad mira a la izquierda
 *
 * ── ESCALABILIDAD ─────────────────────────────────────────────────────────
 * El enum soporta 4 y 8 direcciones actualmente.
 * Para 16 direcciones se añaden los intermedios (NNE, ENE, etc.).
 * Para orientaciones continuas se usa el ángulo en radianes directamente.
 * El OrientationResolver devuelve FacingDirection; el SpriteDefinition
 * puede indexar las animaciones por dirección.
 *
 * ── CLAVES DE ANIMACIÓN POR CONVENCIÓN ────────────────────────────────────
 * Las animaciones de SpriteDefinition siguen el patrón:
 *   animationKey + "_" + FacingDirection.animationSuffix()
 *
 *   "walk" + "_south" → "walk_south"
 *   "idle" + "_east"  → "idle_east"
 *
 * OrientationResolver.animationKey(base, direction) construye la clave.
 */
public enum FacingDirection {

    // ── 4 direcciones principales ─────────────────────────────────────────────

    /** Mira hacia abajo en pantalla (alejándose de la cámara, ángulo ~270°). */
    SOUTH("south"),

    /** Mira hacia arriba en pantalla (hacia la cámara, ángulo ~90°). */
    NORTH("north"),

    /** Mira a la derecha (ángulo ~0°). */
    EAST("east"),

    /** Mira a la izquierda (ángulo ~180°). */
    WEST("west"),

    // ── 8 direcciones (diagonales) ────────────────────────────────────────────

    /** Diagonal: abajo-derecha. */
    SOUTH_EAST("south_east"),

    /** Diagonal: abajo-izquierda. */
    SOUTH_WEST("south_west"),

    /** Diagonal: arriba-derecha. */
    NORTH_EAST("north_east"),

    /** Diagonal: arriba-izquierda. */
    NORTH_WEST("north_west");

    // ── ─────────────────────────────────────────────────────────────────────

    /** Sufijo de animación. Se usa para construir claves como "walk_south". */
    private final String animationSuffix;

    FacingDirection(String animationSuffix) {
        this.animationSuffix = animationSuffix;
    }

    /** Sufijo de la dirección para construir claves de animación. */
    public String animationSuffix() { return animationSuffix; }

    /**
     * Construye la clave de animación para esta dirección.
     *
     * @param baseKey clave base (ej: "walk", "idle", "attack")
     * @return clave compuesta (ej: "walk_south", "idle_east")
     */
    public String toAnimationKey(String baseKey) {
        return baseKey + "_" + animationSuffix;
    }

    /**
     * true si esta dirección forma parte del conjunto de 4 direcciones.
     */
    public boolean isFourDir() {
        return this == SOUTH || this == NORTH || this == EAST || this == WEST;
    }

    /**
     * true si esta dirección es una diagonal (parte del conjunto de 8).
     */
    public boolean isDiagonal() {
        return this == SOUTH_EAST || this == SOUTH_WEST
            || this == NORTH_EAST || this == NORTH_WEST;
    }

    /**
     * Para flip automático: si el sistema solo tiene sprites EAST, esta
     * propiedad indica que la dirección WEST puede representarse con flip
     * horizontal del sprite EAST.
     *
     * De forma análoga, NORTH_WEST puede representarse con flip de NORTH_EAST,
     * y SOUTH_WEST con flip de SOUTH_EAST.
     *
     * @return la dirección "espejo" de esta, o null si no aplica flip.
     */
    public FacingDirection flippedCounterpart() {
        return switch (this) {
            case WEST       -> EAST;
            case NORTH_WEST -> NORTH_EAST;
            case SOUTH_WEST -> SOUTH_EAST;
            default         -> null;
        };
    }

    /**
     * true si este FacingDirection puede representarse con flip de otro sprite.
     */
    public boolean isMirrorable() { return flippedCounterpart() != null; }
}
