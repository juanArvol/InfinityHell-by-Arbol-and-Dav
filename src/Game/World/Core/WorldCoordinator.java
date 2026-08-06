package Game.World.Core;

import java.util.Objects;

/**
 * Coordenada inmutable que identifica un sector del mundo.
 *
 * ── HRFC: Preparación para dimensiones futuras ────────────────────────────
 *
 * ANTES: WorldCoordinator solo tenía x e y (2D). Para implementar:
 *   - Múltiples dimensiones (el mundo de la muerte, otra dimensión)
 *   - Capas de profundidad (superficie, cavernas, subsuelo)
 *   - Planos alternativos
 * ...sería necesario romper la API o añadir un campo nuevo incompatible.
 *
 * AHORA: WorldCoordinator añade un campo 'dimension' (String) que identifica
 * la dimensión/plano al que pertenece el sector. Valor por defecto: "overworld".
 *
 * Este campo no rompe ningún código existente:
 *   - new WorldCoordinator(x, y) → dimension = "overworld" (igual que antes)
 *   - Los constructores de conveniencia (right/left/up/down) preservan dimension
 *   - equals() y hashCode() incluyen dimension
 *
 * Para crear sectores en otra dimensión:
 *   WorldCoordinator underworld = new WorldCoordinator(0, 0, "underworld");
 *
 * ── CONSTANTES DE DIMENSIÓN ───────────────────────────────────────────────
 * Las dimensiones se identifican con Strings en lugar de enum para permitir
 * dimensiones definidas por mods o scripts sin modificar el Engine.
 *
 * Las constantes OVERWORLD, UNDERWORLD, etc. son puntos de inicio, no un
 * conjunto cerrado. Cualquier String es una dimensión válida.
 */
public final class WorldCoordinator {

    // ── Constantes de dimensión ───────────────────────────────────────────

    /** Dimensión principal del juego. Valor por defecto. */
    public static final String OVERWORLD   = "overworld";

    /** Dimensión subterránea / cavernas. */
    public static final String UNDERWORLD  = "underworld";

    /** Dimensión del boss / arena especial. */
    public static final String BOSS_REALM  = "boss_realm";

    /** Dimensión de menú / título (para escenas no gameplay). */
    public static final String MENU        = "menu";

    // ── Estado ────────────────────────────────────────────────────────────

    private final int    x;
    private final int    y;
    private final String dimension;

    // ── Constructores ─────────────────────────────────────────────────────

    /**
     * Coordenada en la dimensión por defecto (OVERWORLD).
     * Retrocompatible con todo el código existente.
     */
    public WorldCoordinator(int x, int y) {
        this(x, y, OVERWORLD);
    }

    /**
     * Coordenada en una dimensión específica.
     *
     * @param x         columna del sector
     * @param y         fila del sector
     * @param dimension identificador de la dimensión (usar constantes si aplica)
     */
    public WorldCoordinator(int x, int y, String dimension) {
        this.x         = x;
        this.y         = y;
        this.dimension = (dimension != null && !dimension.isBlank()) ? dimension : OVERWORLD;
    }

    // ── Navegación cardinal ───────────────────────────────────────────────

    /** Sector a la derecha en la misma dimensión. */
    public WorldCoordinator right() { return new WorldCoordinator(x + 1, y, dimension); }

    /** Sector a la izquierda en la misma dimensión. */
    public WorldCoordinator left()  { return new WorldCoordinator(x - 1, y, dimension); }

    /** Sector arriba en la misma dimensión. */
    public WorldCoordinator up()    { return new WorldCoordinator(x, y - 1, dimension); }

    /** Sector abajo en la misma dimensión. */
    public WorldCoordinator down()  { return new WorldCoordinator(x, y + 1, dimension); }

    /**
     * Sector en la dimensión indicada con las mismas coordenadas x/y.
     * Útil para portales entre dimensiones.
     *
     * @param targetDimension dimensión destino
     */
    public WorldCoordinator inDimension(String targetDimension) {
        return new WorldCoordinator(x, y, targetDimension);
    }

    /**
     * Desplazamiento arbitrario de coordenadas en la misma dimensión.
     */
    public WorldCoordinator offset(int dx, int dy) {
        return new WorldCoordinator(x + dx, y + dy, dimension);
    }

    // ── Acceso ────────────────────────────────────────────────────────────

    public int    x()          { return x;         }
    public int    y()          { return y;         }
    public String dimension()  { return dimension; }

    /** True si esta coordenada pertenece a la dimensión por defecto (OVERWORLD). */
    public boolean isOverworld() { return OVERWORLD.equals(dimension); }

    // ── Identidad ─────────────────────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WorldCoordinator that)) return false;
        return x == that.x && y == that.y && Objects.equals(dimension, that.dimension);
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, dimension);
    }

    @Override
    public String toString() {
        return "WorldCoordinator[" + x + "," + y +
               (OVERWORLD.equals(dimension) ? "" : "," + dimension) + "]";
    }
}
