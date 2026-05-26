package Game.Engine.Systems;

import Game.Engine.Component;
import Game.Engine.GameObjects;
import Game.Engine.Transform3D;
import Game.Render.Camera;
import Game.Render.RenderContext;
import Graficos.Renderable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Sistema de render con ordenamiento por profundidad (Painter's Algorithm).
 *
 * NUEVO SISTEMA para 2.5D: reemplaza al RenderSystem original cuando se
 * quiere depth sorting correcto.
 *
 * Funcionamiento:
 * 1. Ordena todos los objetos por su valor de profundidad antes de dibujar.
 * 2. El valor de profundidad es Y + Z*0.5 (Y de mundo + altura visual).
 * 3. Objetos con mayor valor se dibujan DESPUÉS → aparecen "encima".
 *
 * Ejemplo visual (vista top-down 2.5D):
 *   Jugador en Y=300, Z=0   → depthValue = 300   → se dibuja antes (detrás)
 *   Árbol  en Y=350, Z=0    → depthValue = 350   → se dibuja después (delante)
 *   Pájaro en Y=200, Z=100  → depthValue = 250   → se dibuja entre los dos
 *
 * Retro-compatible: si los objetos NO tienen Transform3D, usa solo Y
 * (Z=0 implícito), comportamiento idéntico al RenderSystem original.
 *
 * LIMITACIÓN (conocida): el Painter's Algorithm tiene artifacts cuando dos
 * objetos se intersecan en profundidad. Para el 90% de casos de juego
 * top-down esto no es problema. Si se necesita exactitud, se requiere
 * BSP tree o render por capas separadas.
 */
public class DepthSortedRenderSystem {

    // Lista de trabajo pre-allocada para evitar crear objetos cada frame
    private final List<GameObjects> sortBuffer = new ArrayList<>(256);

    /**
     * Renderiza los objetos ordenados por profundidad Y+Z.
     *
     * @param objects lista de todos los objetos del mundo
     * @param ctx     contexto de render
     * @param camera  cámara actual
     */
    public void render(List<GameObjects> objects,
                       RenderContext ctx,
                       Camera camera) {

        // Copiar a buffer de trabajo para no modificar la lista original
        sortBuffer.clear();
        sortBuffer.addAll(objects);

        // Ordenar por profundidad: menor valor primero (se dibuja primero = detrás)
        sortBuffer.sort(Comparator.comparingDouble(this::getDepthValue));

        // Renderizar en orden
        for (GameObjects obj : sortBuffer) {
            for (Component c : obj.getComponents()) {
                if (c instanceof Renderable renderable) {
                    renderable.render(ctx, camera);
                }
            }
        }
    }

    /**
     * Calcula el valor de profundidad de un objeto.
     * Si tiene Transform3D, usa Y + Z*0.5.
     * Si solo tiene Transform, usa solo Y (retro-compatible).
     */
    private double getDepthValue(GameObjects obj) {
        if (obj.getTransform() instanceof Transform3D t3d) {
            return t3d.getDepthSortValue();
        }
        return obj.getTransform().getY();
    }
}
