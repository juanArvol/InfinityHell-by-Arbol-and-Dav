package Game.Engine.Components.Visuals;

/**
 * FillMode — cómo escala y posiciona el sprite dentro del área de render.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * Define la política de escala visual. SpriteRenderer la consulta cada frame
 * para calcular la posición y dimensiones reales del drawImage().
 *
 * No requiere lógica externa al SpriteRenderer. La selección del modo es
 * solo datos: el código de render la interpreta directamente.
 *
 * ── MODOS ─────────────────────────────────────────────────────────────────
 *
 *   STRETCH   — estira el sprite hasta cubrir exactamente el área.
 *               No mantiene aspecto. Es el comportamiento original del engine.
 *
 *   FIT       — escala manteniendo el aspect ratio hasta que el sprite
 *               quepa completamente dentro del área. Nunca recorta.
 *               Puede dejar bandas vacías en uno de los ejes.
 *
 *   COVER     — escala manteniendo el aspect ratio hasta cubrir completamente
 *               el área. Puede recortar parte del sprite en uno de los ejes.
 *
 *   CENTER    — no escala. Centra el sprite en el área tal como es.
 *               Si el sprite es mayor que el área, se recorta por los bordes.
 *
 *   TILE      — repite el sprite en ambos ejes hasta llenar el área.
 *               Nunca deforma. Los restos se controlan con Alignment.
 *
 *   TILE_X    — repite solo en el eje horizontal. En vertical STRETCH.
 *
 *   TILE_Y    — repite solo en el eje vertical. En horizontal STRETCH.
 *
 * ── USO ───────────────────────────────────────────────────────────────────
 *
 *   SpriteRenderer r = getComponent(SpriteRenderer.class);
 *   r.setFillMode(FillMode.FIT);
 *   r.setFillMode(FillMode.TILE);
 */
public enum FillMode {

    /**
     * Estira el sprite hasta ocupar toda el área exactamente.
     * No mantiene aspect ratio.
     * Es el comportamiento por defecto del engine (compatible con HRFC-003.5/6).
     */
    STRETCH,

    /**
     * Escala manteniendo aspect ratio hasta que el sprite quepa en el área.
     * El lado más restrictivo define la escala. El otro eje puede tener bandas.
     * Nunca recorta el sprite.
     */
    FIT,

    /**
     * Escala manteniendo aspect ratio hasta cubrir completamente el área.
     * El lado menos restrictivo define la escala. El exceso se recorta.
     * Garantiza que no queden bandas vacías.
     */
    COVER,

    /**
     * No escala el sprite. Lo centra en el área tal como es.
     * Si el sprite supera el área en algún eje, se recorta visualmente.
     */
    CENTER,

    /**
     * Repite el sprite en ambos ejes (X e Y) hasta llenar completamente el área.
     * El sprite nunca se deforma. Los restos se controlan con Alignment.
     */
    TILE,

    /**
     * Repite el sprite solo en el eje horizontal (X).
     * En el eje vertical se estira (STRETCH).
     */
    TILE_X,

    /**
     * Repite el sprite solo en el eje vertical (Y).
     * En el eje horizontal se estira (STRETCH).
     */
    TILE_Y
}
