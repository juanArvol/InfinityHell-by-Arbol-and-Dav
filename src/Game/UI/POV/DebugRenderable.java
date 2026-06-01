package Game.UI.POV;

/**
 * Interfaz para objetos que pueden renderizar información de debug.
 *
 * FIX M-03: la firma original era debugRender(RenderContext, Camera).
 * RenderContext ya encapsula la cámara a través de withCamera() /
 * withCameraIsolated() — pasar Camera como argumento adicional rompía esa
 * encapsulación y permitía que los implementadores accedieran a la cámara
 * directamente, saltándose el pipeline de transformación de RenderContext.
 *
 * El llamador es responsable de pasar un RenderContext ya configurado con
 * la transformación de cámara apropiada si el debug necesita coordenadas de mundo.
 *
 * Ejemplo de llamada desde el render loop:
 *
 *   RenderContext worldCtx = ctx.withCamera(camera);
 *   try {
 *       debugRenderable.debugRender(worldCtx);
 *   } finally {
 *       worldCtx.dispose();
 *   }
 */
public interface DebugRenderable {
    void debugRender(RenderContext ctx);
}
