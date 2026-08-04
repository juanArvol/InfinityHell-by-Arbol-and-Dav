package Game.Engine.Camera;

import Game.Engine.GameMath.Logic2D.Vector2D;
import java.awt.geom.AffineTransform;

/**
 * Cámara del Engine — entidad de primer nivel.
 *
 * ── Qué es ───────────────────────────────────────────────────────────────
 *
 * GameCamera representa la vista del Engine: posición en el mundo, zoom y
 * rotación. Es un servicio del Engine que cualquier subsistema puede consultar.
 *
 * NO pertenece al dominio del gameplay (World no la posee).
 * NO pertenece al renderer (SceneRenderer solo la lee para construir la vista).
 * PERTENECE al Engine como entidad independiente con identidad propia.
 *
 * ── Responsabilidades ────────────────────────────────────────────────────
 *
 *   ESTADO:     mantiene posición, zoom y rotación de la vista.
 *   TRANSFORM:  expone getViewTransform() para que el renderer la aplique.
 *   LERP:       soporta interpolación suave de posición y zoom.
 *   CLAMP:      clamp opcional a los límites de un mundo.
 *
 * ── Responsabilidades EXCLUIDAS ──────────────────────────────────────────
 *
 *   COMPORTAMIENTO: quién sigue a quién, cómo se mueve → CameraController.
 *   RENDER:         aplicar la transformación a Graphics2D → RenderContext.
 *   GAMEPLAY:       disparadores de eventos, visibilidad → sistemas futuros.
 *
 * ── Por qué getViewTransform() en lugar de getX()/getY() para el render ──
 *
 *   Cuando hay solo translación, ambas formas son equivalentes. Con zoom y
 *   rotación, el renderer necesita una transformación compuesta. Exponer un
 *   AffineTransform encapsula esa composición y permite añadir shear,
 *   perspectiva o cualquier otra transformación futura sin cambiar la firma
 *   de ningún método del renderer.
 *
 *   getX() y getY() se conservan para casos donde solo se necesita el offset
 *   de translación (componentes existentes como SpriteRenderer).
 *
 * ── Threading ────────────────────────────────────────────────────────────
 *
 *   GameCamera es mutable. Todas las escrituras deben ocurrir en el
 *   game loop thread (CameraController.update). getViewTransform() puede
 *   ser llamado desde el render thread si la política de sincronización
 *   del game loop lo garantiza (single-threaded loop: sin riesgo).
 *
 * ── Coordenadas ──────────────────────────────────────────────────────────
 *
 *   Exclusivamente coordenadas VIRTUALES (virtualWidth × virtualHeight).
 *   La transformación virtual → pantalla real la realiza el pipeline de Display.
 *   GameCamera nunca conoce resoluciones reales de monitor.
 */
public final class GameCamera {

    // ── Estado ───────────────────────────────────────────────────────────────

    /** Posición top-left de la vista en coordenadas de mundo virtual. */
    private double x;
    private double y;

    /** Factor de zoom. 1.0 = sin zoom. >1.0 = ampliar. <1.0 = alejar. */
    private float zoom;

    /**
     * Rotación en radianes. 0.0 = sin rotación.
     * Positivo = sentido horario (convención AWT).
     */
    private float rotation;

    // ── Límites de mundo (opcionales) ────────────────────────────────────────

    private int worldWidth  = 0;
    private int worldHeight = 0;
    private int virtualWidth;
    private int virtualHeight;
    private boolean hasBounds = false;

    // ── Construcción ─────────────────────────────────────────────────────────

    /**
     * Crea una cámara centrada en el origen, sin zoom ni rotación.
     *
     * @param virtualWidth  ancho virtual del área de juego (DisplaySettings.virtualWidth)
     * @param virtualHeight alto virtual del área de juego (DisplaySettings.virtualHeight)
     */
    public GameCamera(int virtualWidth, int virtualHeight) {
        this.x             = 0;
        this.y             = 0;
        this.zoom          = 1.0f;
        this.rotation      = 0.0f;
        this.virtualWidth  = virtualWidth;
        this.virtualHeight = virtualHeight;
    }

    // ── Posicionamiento directo ───────────────────────────────────────────────

    /**
     * Centra la cámara en la posición de mundo dada (snap instantáneo).
     *
     * Aplica clamp a los límites del mundo si están configurados.
     * Sin límites: clamp a >= 0 para evitar posiciones negativas.
     */
    public void centerOn(double worldX, double worldY) {
        double targetX = worldX - virtualWidth  / (2.0 * zoom);
        double targetY = worldY - virtualHeight / (2.0 * zoom);
        applyClamp(targetX, targetY);
    }

    /**
     * Mueve la cámara directamente a la posición top-left indicada (snap).
     * No aplica centrado — coloca el corner superior izquierdo en (x, y).
     */
    public void moveTo(double x, double y) {
        applyClamp(x, y);
    }

    // ── Interpolación (lerp) ──────────────────────────────────────────────────

    /**
     * Suaviza la posición de la cámara hacia el centro del objetivo.
     *
     * No llega instantáneamente: se aproxima al objetivo en cada tick,
     * produciendo el efecto de seguimiento suave característico de los
     * juegos de calidad. A mayor factor, más rápido (1.0 = snap).
     *
     * Fórmula: pos = pos + (target - pos) * factor
     * Con factor=0.1 y 30fps: alcanza ~96% en 1 segundo.
     * Con factor=0.08 y 30fps: alcanza ~92% en 1 segundo (más suave).
     *
     * @param worldX  posición X en mundo hacia la que centrar
     * @param worldY  posición Y en mundo hacia la que centrar
     * @param factor  velocidad de lerp [0.0, 1.0]; típicamente 0.05–0.15
     */
    public void lerpCenterOn(double worldX, double worldY, float factor) {
        double targetX = worldX - virtualWidth  / (2.0 * zoom);
        double targetY = worldY - virtualHeight / (2.0 * zoom);

        double newX = x + (targetX - x) * factor;
        double newY = y + (targetY - y) * factor;

        applyClamp(newX, newY);
    }

    /**
     * Suaviza el zoom hacia el valor objetivo.
     *
     * @param targetZoom zoom deseado (1.0 = normal)
     * @param factor     velocidad de lerp [0.0, 1.0]
     */
    public void lerpZoom(float targetZoom, float factor) {
        zoom = zoom + (targetZoom - zoom) * factor;
    }

    /**
     * Suaviza la rotación hacia el ángulo objetivo.
     *
     * @param targetRotation ángulo en radianes
     * @param factor         velocidad de lerp [0.0, 1.0]
     */
    public void lerpRotation(float targetRotation, float factor) {
        rotation = rotation + (targetRotation - rotation) * factor;
    }

    // ── Mutación directa de zoom y rotación ─────────────────────────────────

    /**
     * Establece el zoom inmediatamente.
     * @param zoom > 0; 1.0 = sin zoom.
     */
    public void setZoom(float zoom) {
        if (zoom > 0) this.zoom = zoom;
    }

    /**
     * Establece la rotación inmediatamente.
     * @param rotation en radianes; 0.0 = sin rotación.
     */
    public void setRotation(float rotation) {
        this.rotation = rotation;
    }

    // ── Límites de mundo ─────────────────────────────────────────────────────

    /**
     * Configura el clamp a los límites del mundo.
     *
     * Cuando está activo, la cámara nunca mostrará área más allá de los
     * bordes del mundo. Llamar cada vez que el mundo cambie de tamaño.
     *
     * @param worldWidth  ancho total del mundo en píxeles virtuales
     * @param worldHeight alto total del mundo en píxeles virtuales
     */
    public void setWorldBounds(int worldWidth, int worldHeight) {
        this.worldWidth  = worldWidth;
        this.worldHeight = worldHeight;
        this.hasBounds   = (worldWidth > 0 && worldHeight > 0);
        // Re-aplicar clamp con los nuevos límites.
        applyClamp(this.x, this.y);
    }

    /** Elimina los límites de mundo. La cámara puede ir a cualquier posición. */
    public void clearWorldBounds() {
        this.hasBounds = false;
    }

    // ── Actualización de resolución virtual ──────────────────────────────────

    /**
     * Actualiza las dimensiones virtuales.
     * Llamar cuando DisplayManager notifique un cambio de resolución virtual.
     */
    public void onVirtualResolutionChanged(int newVirtualWidth, int newVirtualHeight) {
        this.virtualWidth  = newVirtualWidth;
        this.virtualHeight = newVirtualHeight;
        applyClamp(this.x, this.y);
    }

    // ── Transformación de vista ───────────────────────────────────────────────

    /**
     * Devuelve la transformación de vista lista para aplicar al Graphics2D.
     *
     * La transformación convierte coordenadas de mundo a coordenadas de
     * pantalla virtual aplicando en orden:
     *   1. Translación (offset de cámara).
     *   2. Zoom (escala uniforme desde el centro de la pantalla).
     *   3. Rotación (alrededor del centro de la pantalla).
     *
     * El renderer aplica esta transformación con:
     *   Graphics2D g = ...;
     *   AffineTransform saved = g.getTransform();
     *   g.transform(camera.getViewTransform());
     *   // dibujar la escena
     *   g.setTransform(saved);
     *
     * Para el caso más común (solo translación, zoom=1, rotation=0), la
     * transformación es equivalente a g.translate(-x, -y).
     *
     * @return transformación de vista como AffineTransform (nueva instancia cada llamada).
     */
    public AffineTransform getViewTransform() {
        AffineTransform t = new AffineTransform();

        if (zoom != 1.0f || rotation != 0.0f) {
            // Zoom y/o rotación: componer desde el centro de la pantalla virtual.
            double cx = virtualWidth  / 2.0;
            double cy = virtualHeight / 2.0;

            // Trasladar al centro de pantalla, aplicar zoom+rotación, descentrar y aplicar offset.
            t.translate(cx, cy);
            if (rotation != 0.0f) t.rotate(rotation);
            if (zoom != 1.0f)     t.scale(zoom, zoom);
            t.translate(-cx, -cy);
        }

        // Translación de cámara (siempre presente).
        t.translate(-x, -y);

        return t;
    }

    // ── Lectura de estado ─────────────────────────────────────────────────────

    /**
     * Offset X de la cámara en coordenadas de mundo.
     * screenX = worldX - camera.getX()   (con zoom=1, rotation=0).
     * Conservado para compatibilidad con componentes existentes.
     */
    public double getX() { return x; }

    /**
     * Offset Y de la cámara en coordenadas de mundo.
     * screenY = worldY - camera.getY()   (con zoom=1, rotation=0).
     */
    public double getY() { return y; }

    /** Zoom actual. 1.0 = sin zoom. */
    public float getZoom() { return zoom; }

    /** Rotación actual en radianes. */
    public float getRotation() { return rotation; }

    /** Ancho virtual configurado. */
    public int getVirtualWidth()  { return virtualWidth;  }

    /** Alto virtual configurado. */
    public int getVirtualHeight() { return virtualHeight; }

    /**
     * Copia defensiva de la posición como Vector2D.
     * Preferir getX()/getY() cuando solo se necesita el valor escalar.
     */
    public Vector2D getPosition() {
        return new Vector2D(x, y);
    }

    /**
     * True si los límites de mundo están activos.
     */
    public boolean hasBounds() { return hasBounds; }

    // ── Privados ──────────────────────────────────────────────────────────────

    /**
     * Aplica la posición con clamp a los límites configurados.
     *
     * Sin límites: clamp a >= 0 para evitar posiciones negativas.
     * Con límites: clamp a [0, worldDim - virtualDim / zoom].
     *
     * La división por zoom corrige el visible area: con zoom=2.0 la vista
     * virtual cubre la mitad del espacio, por lo que el límite superior es
     * (worldWidth - virtualWidth/zoom), no (worldWidth - virtualWidth).
     */
    private void applyClamp(double newX, double newY) {
        if (hasBounds) {
            // Area visible = virtualDim / zoom
            double visW = virtualWidth  / (double) zoom;
            double visH = virtualHeight / (double) zoom;
            // Math.max(0, ...) protege el caso worldDim < virtualDim/zoom.
            this.x = Math.max(0, Math.min(newX, Math.max(0, worldWidth  - visW)));
            this.y = Math.max(0, Math.min(newY, Math.max(0, worldHeight - visH)));
        } else {
            // Sin límites: solo clamp a >= 0.
            this.x = Math.max(0, newX);
            this.y = Math.max(0, newY);
        }
    }
}
