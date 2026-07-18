package Game.Engine.RenderEngine.Sprites;

/**
 * Alignment — alineación del sprite dentro del área de render.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * Controla dónde queda el resto visual cuando el área de render no es
 * múltiplo exacto del tamaño del sprite. Aplica principalmente a los modos
 * TILE, TILE_X y TILE_Y, pero también a CENTER cuando el sprite no llena
 * el área, y a FIT cuando quedan bandas vacías.
 *
 * Se usa por separado para el eje horizontal y el eje vertical, permitiendo
 * combinaciones como: alineado a la izquierda horizontalmente y centrado
 * verticalmente.
 *
 * ── VALORES ───────────────────────────────────────────────────────────────
 *
 *   START   — alinea al inicio del eje.
 *             Horizontal: borde izquierdo.
 *             Vertical:   borde superior.
 *
 *   CENTER  — centra en el eje.
 *
 *   END     — alinea al final del eje.
 *             Horizontal: borde derecho.
 *             Vertical:   borde inferior.
 *
 * ── USO ───────────────────────────────────────────────────────────────────
 *
 *   SpriteRenderer r = getComponent(SpriteRenderer.class);
 *   r.setFillMode(FillMode.TILE);
 *   r.setAlignH(Alignment.CENTER);
 *   r.setAlignV(Alignment.START);
 *
 * ── SEMÁNTICA EN TILE ─────────────────────────────────────────────────────
 *
 *   Si el área = 100px y el sprite = 32px → caben 3 repeticiones (96px).
 *   Quedan 4px de resto.
 *
 *   START  → el resto queda al final (derecha o abajo)
 *   CENTER → la tira de repeticiones se centra; 2px de resto a cada lado
 *   END    → el resto queda al inicio (izquierda o arriba)
 */
public enum Alignment {

    /**
     * Alinea al inicio del eje (izquierda para horizontal, arriba para vertical).
     * El exceso queda al final.
     */
    START,

    /**
     * Centra el contenido en el eje.
     * El exceso se distribuye equitativamente en ambos lados.
     */
    CENTER,

    /**
     * Alinea al final del eje (derecha para horizontal, abajo para vertical).
     * El exceso queda al inicio.
     */
    END
}
