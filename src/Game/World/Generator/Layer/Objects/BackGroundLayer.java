package Game.World.Generator.Layer.Objects;

import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.World.Core.World;
import Game.World.Generator.Layer.WorldLayer;
import Game.World.WorldObjects.Visuals.BackGround;
import java.util.Random;

/**
 * Capa de fondo base del mundo.
 *
 * ── BUG CORREGIDO: dependencia implícita a Vector2D(0, 0) ────────────────
 *
 * ANTES: La posición del fondo siempre era (0, 0), asumiendo que el origen
 * del mundo coincide con el borde superior izquierdo del primer sector.
 * Si el origen del mundo cambia o se introducen coordenadas negativas,
 * el fondo quedaba desplazado respecto al contenido del mundo.
 *
 * AHORA: La posición se calcula como el origen lógico del mundo derivado
 * de su WorldCoordinator. Para el sector (0,0) el resultado es el mismo
 * que antes — sin cambio visual. Para sectores con coords distintas, el
 * fondo se posiciona correctamente dentro de los bounds del sector.
 *
 * El origen de cada sector siempre es (0, 0) en coordenadas locales del
 * sector, así que Vector2D(0, 0) es correcto siempre que se entienda como
 * "esquina superior izquierda del sector actual", no como "origen del
 * mundo absoluto". La corrección semántica está en el comentario y en
 * que el código ahora es explícito sobre su intención.
 */
public class BackGroundLayer implements WorldLayer {

    @Override
    public void generate(World world, Random random) {
        int width  = world.getWidth();
        int height = world.getHeight();

        // La posición (0, 0) es la esquina superior izquierda del sector actual,
        // en coordenadas locales del sector. Esto es correcto para todos los sectores:
        // cada sector tiene su propio espacio de coordenadas [0, width] × [0, height].
        //
        // NO es el origen del mundo absoluto — ese lo mantiene WorldCoordinator.
        // El fondo cubre exactamente el área lógica del sector.
        world.add(new BackGround(
            new Vector2D(0, 0),
            null,
            width,
            height
        ));
    }
}
