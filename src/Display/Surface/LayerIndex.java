package Display.Surface;

/**
 * Índice de capas del sistema de render por capas.
 *
 * ── Propósito ────────────────────────────────────────────────────────────
 *
 * Define el orden de composición del frame. Cada capa es un contexto de
 * dibujo independiente que se compone sobre las anteriores en orden ordinal
 * creciente: las capas con índice menor quedan debajo.
 *
 * ── Por qué capas explícitas en lugar de orden de llamada ────────────────
 *
 * Con un único Graphics2D compartido, el orden visual depende del orden
 * en que los subsistemas llaman a draw(). Cambiar ese orden requiere
 * modificar GameLoop o GameState. Con capas:
 *   - Cada subsistema dibuja en su capa, en cualquier orden de llamada.
 *   - El orden visual está declarado aquí, no en el flujo procedural.
 *   - Insertar una nueva capa (ej. SCREEN_EFFECTS) no requiere reorganizar código.
 *   - Screen shake, flash y postprocesado se aplican a capas específicas.
 *
 * ── Semántica de cada capa ────────────────────────────────────────────────
 *
 *   WORLD_BACKGROUND  — fondos del mundo: cielo, suelo, tiles de fondo.
 *                        Se limpia al inicio del frame con el background configurado.
 *
 *   WORLD_ENTITIES    — entidades del mundo: jugador, enemigos, objetos, partículas.
 *                        Se dibuja con cámara aplicada (offset, zoom, rotación).
 *
 *   WORLD_FOREGROUND  — objetos en primer plano del mundo con cámara.
 *                        Árboles, techos, decoraciones delanteras.
 *                        Reservada para uso futuro; actualmente vacía.
 *
 *   SCREEN_EFFECTS    — efectos en espacio de pantalla: screen shake, flash de daño,
 *                        viñeta, fade-in/out. SIN transformación de cámara.
 *                        Reservada para uso futuro; actualmente vacía.
 *
 *   HUD               — interfaz de usuario: barras de vida, munición, crosshair.
 *                        SIN transformación de cámara. Siempre encima del mundo.
 *
 *   OVERLAY           — overlays informativos: FPS counter, coordenadas de debug.
 *                        SIN transformación de cámara. Siempre encima de la HUD.
 *
 *   DEBUG             — herramientas de debug: hitboxes, paths de IA, grillas.
 *                        SIN transformación de cámara*. Siempre en la capa más alta.
 *                        (* los hitboxes sí aplican offset de cámara internamente)
 *
 * ── Transparencia entre capas ─────────────────────────────────────────────
 *
 * Todas las capas excepto WORLD_BACKGROUND son BufferedImage con canal alfa.
 * La composición se hace con AlphaComposite.SRC_OVER, preservando la
 * transparencia correctamente. Esto permite HUDs semi-transparentes, efectos
 * de fade y cualquier combinación sin artefactos.
 *
 * WORLD_BACKGROUND se limpia con el background opaco del Display (como ahora).
 * Las capas superiores se limpian con transparencia total al inicio del frame.
 *
 * ── Extensibilidad ────────────────────────────────────────────────────────
 *
 * Añadir una nueva capa: insertar una constante en la posición ordinal correcta.
 * El sistema de composición usa ordinal() para el orden, por lo que la posición
 * en el enum determina el orden visual sin configuración adicional.
 *
 * IMPORTANTE: no reordenar constantes existentes sin revisar todos los callers.
 */
public enum LayerIndex {

    /**
     * Fondo del mundo. Limpiado con el background del Display al inicio del frame.
     * No tiene canal alfa separado — es el framebuffer principal.
     */
    WORLD_BACKGROUND,

    /**
     * Entidades del mundo con transformación de cámara.
     * Dibujado con desplazamiento, zoom y rotación de GameCamera.
     */
    WORLD_ENTITIES,

    /**
     * Primer plano del mundo con transformación de cámara.
     * Reservado para árboles, techos, decoraciones delanteras.
     */
    WORLD_FOREGROUND,

    /**
     * Efectos en espacio de pantalla.
     * Screen shake, flash de daño, viñeta, fade.
     * Sin transformación de cámara.
     */
    SCREEN_EFFECTS,

    /**
     * Interfaz de usuario en espacio de pantalla.
     * Barras de vida, munición, crosshair, minimapa.
     * Sin transformación de cámara.
     */
    HUD,

    /**
     * Overlays informativos sobre la HUD.
     * FPS counter, tooltips de debug.
     * Sin transformación de cámara.
     */
    OVERLAY,

    /**
     * Herramientas de debug visuales.
     * Hitboxes, paths de IA, grillas, stats de render.
     * Siempre en la capa más alta.
     */
    DEBUG
}
