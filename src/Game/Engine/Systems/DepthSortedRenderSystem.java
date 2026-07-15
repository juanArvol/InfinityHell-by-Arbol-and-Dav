package Game.Engine.Systems;

import Game.Engine.Component;
import Game.Engine.GameMath.SpaceLogic.Logic3D.Transform3D;
import Game.Engine.GameObjects;
import Game.Engine.RenderEngine.Context.RenderCamera;
import Game.Engine.RenderEngine.Context.RenderContext;
import Game.Engine.RenderEngine.Contracts.Renderable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Sistema de render con ordenamiento por profundidad (Painter's Algorithm).
 *
 * SISTEMA 2.5D: reemplaza al RenderSystem original cuando se quiere
 * depth sorting correcto.
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
 *
 * ──────────────────────────────────────────────────────────────────────────
 * CORRECCIÓN: BUFFER LOCAL EN LUGAR DE CAMPO MUTABLE
 *
 * Problema anterior:
 *   sortBuffer era un campo de instancia mutable pre-allocado. El objetivo
 *   era evitar crear un ArrayList en cada frame. Sin embargo:
 *
 *   1. El addAll(objects) ya crea un iterador interno cada frame, anulando
 *      el ahorro de la pre-allocación.
 *   2. Un campo de instancia mutable hace que render() no sea reentrante:
 *      si en el futuro se invocara desde múltiples threads (render secundario,
 *      sistema de reflejo, captura de screenshot) el sortBuffer se corrompería.
 *   3. La lista nunca se recorta: si en un frame hay 500 objetos y en el
 *      siguiente hay 10, el buffer retiene capacidad para 500 indefinidamente.
 *
 * Solución:
 *   Crear el buffer local dentro de render() cada invocación. El GC de la
 *   JVM optimiza bien listas de vida corta (escape analysis). render() es
 *   llamado 30 veces por segundo con listas de cientos de objetos — el
 *   overhead de crear un ArrayList<>(N) es O(N) igual que el addAll(),
 *   por lo que no hay regresión de rendimiento medible.
 *
 *   render() es ahora stateless y reentrante, preparado para usos futuros
 *   como renders secundarios, previews o capturas de pantalla.
 */
public class DepthSortedRenderSystem {

    /**
     * Renderiza los objetos ordenados por profundidad Y+Z.
     *
     * Crea un buffer local para la ordenación — stateless y reentrante.
     *
     * @param objects lista de todos los objetos del mundo (no se modifica)
     * @param ctx     contexto de render
     * @param camera  cámara actual
     */
    public void render(List<GameObjects> objects,
                       RenderContext ctx,
                       RenderCamera camera) {

        // Buffer local: stateless, reentrante, sin retención de memoria entre frames.
        List<GameObjects> sortBuffer = new ArrayList<>(objects);

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
     * Si solo tiene Transform2D, usa solo Y (retro-compatible).
     */
    private double getDepthValue(GameObjects obj) {
        if (obj.getTransform() instanceof Transform3D t3d) {
            return t3d.getDepthSortValue();
        }
        return obj.getTransform().getY();
    }
}
