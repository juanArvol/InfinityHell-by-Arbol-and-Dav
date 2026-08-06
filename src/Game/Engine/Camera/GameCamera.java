package Game.Engine.Camera;

import Game.Engine.Camera.Constraint.CameraConstraintList;
import Game.Engine.Camera.Constraint.WorldBoundsConstraint;
import Game.Engine.Camera.Modifier.CameraModifierStack;
import Game.Engine.Camera.Modifier.CameraState;
import Game.Engine.GameMath.Logic2D.Vector2D;
import java.awt.geom.AffineTransform;

/**
 * Cámara del Engine — entidad de primer nivel.
 *
 * ── HRFC: Integración de Modifiers y Constraints ─────────────────────────
 *
 * GameCamera ahora integra tres sistemas ortogonales:
 *
 *   CameraController     → QUIÉN sigue a quién y cómo (lerp, snap, cinemático)
 *   CameraModifierStack  → CÓMO se ve la imagen (shake, zoom, offset, rotation)
 *   CameraConstraintList → DÓNDE puede ir (bounds, región, maxFollow)
 *
 * ── PIPELINE DE CÁMARA POR FRAME ─────────────────────────────────────────
 *
 *   1. CameraController.update()     → calcula posición base via lerpCenterOn()
 *   2. modifiers.computeState()      → acumula efectos de todos los modificadores
 *   3. applyModifierState(state)     → aplica offsets, zoom delta, rotation delta
 *   4. constraints.apply()           → restringe la posición final
 *
 * El paso 2-4 ocurre implícitamente dentro de applyClamp() cuando los sistemas
 * están activos. El orden garantiza que constraints siempre tienen la
 * última palabra sobre la posición final.
 *
 * ── RETROCOMPATIBILIDAD TOTAL ─────────────────────────────────────────────
 * Toda la API existente (centerOn, lerpCenterOn, setZoom, setWorldBounds, etc.)
 * sigue funcionando sin cambios. Los nuevos sistemas son opcionalmente usados:
 * si modifierStack está vacío y constraintList está vacía, el comportamiento
 * es idéntico al anterior.
 *
 * ── CLAMP ANTERIOR REEMPLAZADO POR WorldBoundsConstraint ─────────────────
 * El clamp hardcodeado en applyClamp() fue reemplazado por WorldBoundsConstraint
 * (prioridad 1000 — siempre última en aplicarse).
 * setWorldBounds() ahora actualiza la constraint, no el estado interno del clamp.
 * La API setWorldBounds() sigue siendo la misma — sin cambios externos.
 *
 * ── NO ACEPTA COORDENADAS NEGATIVAS ──────────────────────────────────────
 * La constraint por defecto clamp a [minX, minY] que arranca en 0.
 * Para mundos con coords negativas: worldBoundsConstraint.update(minX, minY, maxX, maxY).
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
 * ── Coordenadas ──────────────────────────────────────────────────────────
 *
 * Exclusivamente coordenadas VIRTUALES (virtualWidth × virtualHeight).
 * La transformación virtual → pantalla real la realiza el pipeline de Display.
 * GameCamera nunca conoce resoluciones reales de monitor.
 */
public final class GameCamera {

    // ── Estado de posición/vista ──────────────────────────────────────────────

    /** Posición top-left de la vista en coordenadas de mundo virtual (base, sin modificadores). */
    private double x;
    private double y;

    /** Posición final tras aplicar modificadores y constraints (usada en render). */
    private double finalX;
    private double finalY;

    /** Factor de zoom base. 1.0 = sin zoom. >1.0 = ampliar. <1.0 = alejar. */
    private float zoom;

    /** Zoom efectivo = zoom * zoomDelta de los modificadores. */
    private float effectiveZoom;

    /**
     * Rotación base en radianes. 0.0 = sin rotación.
     * Positivo = sentido horario (convención AWT).
     */
    private float rotation;

    /** Rotación efectiva = rotation + rotationDelta de los modificadores. */
    private float effectiveRotation;

    // ── Dimensiones virtuales ─────────────────────────────────────────────────

    private int virtualWidth;
    private int virtualHeight;

    // ── Sistemas integrados ───────────────────────────────────────────────────

    /** Pila de modificadores temporales (shake, zoom, offset, rotation, letterbox). */
    private final CameraModifierStack  modifierStack;

    /** Lista de constraints de posición (WorldBounds, Region, MaxFollow, etc.). */
    private final CameraConstraintList constraintList;

    /** La constraint de WorldBounds es la más importante: acceso directo para setWorldBounds(). */
    private final WorldBoundsConstraint worldBoundsConstraint;

    // ── Estado del último CameraState (para acceso externo, ej: letterbox) ───

    private CameraState lastModifierState = null;

    // ── Construcción ─────────────────────────────────────────────────────────

    /**
     * Crea una cámara centrada en el origen, sin zoom ni rotación.
     *
     * @param virtualWidth  ancho virtual del área de juego (DisplaySettings.virtualWidth)
     * @param virtualHeight alto virtual del área de juego (DisplaySettings.virtualHeight)
     */
    public GameCamera(int virtualWidth, int virtualHeight) {
        this.x               = 0;
        this.y               = 0;
        this.finalX          = 0;
        this.finalY          = 0;
        this.zoom            = 1.0f;
        this.effectiveZoom   = 1.0f;
        this.rotation        = 0.0f;
        this.effectiveRotation = 0.0f;
        this.virtualWidth    = virtualWidth;
        this.virtualHeight   = virtualHeight;

        this.modifierStack         = new CameraModifierStack();
        this.constraintList        = new CameraConstraintList();
        this.worldBoundsConstraint = new WorldBoundsConstraint(); // deshabilitada por defecto
        this.constraintList.add(worldBoundsConstraint);
    }

    // ── Posicionamiento directo ───────────────────────────────────────────────

    /**
     * Centra la cámara en la posición de mundo dada (snap instantáneo).
     * Aplica constraints. Los modificadores se aplican en commitFrame().
     */
    public void centerOn(double worldX, double worldY) {
        double targetX = worldX - virtualWidth  / (2.0 * zoom);
        double targetY = worldY - virtualHeight / (2.0 * zoom);
        setBasePosition(targetX, targetY);
    }

    /**
     * Mueve la cámara directamente a la posición top-left indicada.
     */
    public void moveTo(double x, double y) {
        setBasePosition(x, y);
    }

    // ── Interpolación (lerp) ──────────────────────────────────────────────────

    /**
     * Suaviza la posición de la cámara hacia el centro del objetivo.
     *
     * Fórmula: pos = pos + (target - pos) * factor
     * Con factor=0.1 y 30fps: alcanza ~96% en 1 segundo.
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

        setBasePosition(newX, newY);
    }

    /**
     * Suaviza el zoom hacia el valor objetivo.
     *
     * @param targetZoom zoom deseado (1.0 = normal)
     * @param factor     velocidad de lerp [0.0, 1.0]
     */
    public void lerpZoom(float targetZoom, float factor) {
        zoom = zoom + (targetZoom - zoom) * factor;
        commitFrame();
    }

    /**
     * Suaviza la rotación hacia el ángulo objetivo.
     *
     * @param targetRotation ángulo en radianes
     * @param factor         velocidad de lerp [0.0, 1.0]
     */
    public void lerpRotation(float targetRotation, float factor) {
        rotation = rotation + (targetRotation - rotation) * factor;
        commitFrame();
    }

    // ── Mutación directa de zoom y rotación ──────────────────────────────────

    /**
     * Establece el zoom base inmediatamente.
     * @param zoom > 0; 1.0 = sin zoom.
     */
    public void setZoom(float zoom) {
        if (zoom > 0) {
            this.zoom = zoom;
            commitFrame();
        }
    }

    /**
     * Establece la rotación base inmediatamente.
     * @param rotation en radianes; 0.0 = sin rotación.
     */
    public void setRotation(float rotation) {
        this.rotation = rotation;
        commitFrame();
    }

    // ── Sistema de modificadores ──────────────────────────────────────────────

    /**
     * Acceso al modifier stack para añadir/eliminar modificadores temporales.
     *
     * Uso:
     *   camera.getModifiers().add(ShakeModifier.impact(6.0f, 12));
     *   camera.getModifiers().add(new ZoomModifier(1.5f, 30, 0.1f));
     */
    public CameraModifierStack getModifiers() {
        return modifierStack;
    }

    // ── Sistema de constraints ────────────────────────────────────────────────

    /**
     * Acceso a la lista de constraints para añadir/eliminar restricciones.
     *
     * Uso:
     *   camera.getConstraints().add(new RegionConstraint(200, 100, 1000, 600));
     *   camera.getConstraints().add(HardConstraint.horizontal(0, worldWidth));
     */
    public CameraConstraintList getConstraints() {
        return constraintList;
    }

    // ── Límites de mundo ──────────────────────────────────────────────────────

    /**
     * Configura los límites del mundo.
     *
     * Actualiza WorldBoundsConstraint (prioridad 1000 — siempre gana).
     * La cámara nunca mostrará área más allá de los bordes del mundo.
     * Llamar cada vez que el mundo cambie de tamaño o se cambie de sector.
     *
     * @param worldWidth  ancho total del mundo en píxeles virtuales
     * @param worldHeight alto total del mundo en píxeles virtuales
     */
    public void setWorldBounds(int worldWidth, int worldHeight) {
        worldBoundsConstraint.update(worldWidth, worldHeight);
        commitFrame(); // re-aplicar constraints con los nuevos límites
    }

    /** Elimina los límites de mundo. La cámara puede ir a cualquier posición. */
    public void clearWorldBounds() {
        worldBoundsConstraint.setEnabled(false);
        commitFrame();
    }

    /** True si los límites de mundo están activos. */
    public boolean hasBounds() {
        return worldBoundsConstraint.isActive();
    }

    // ── Actualización de resolución virtual ──────────────────────────────────

    /**
     * Actualiza las dimensiones virtuales.
     * Llamar cuando DisplayManager notifique un cambio de resolución virtual.
     */
    public void onVirtualResolutionChanged(int newVirtualWidth, int newVirtualHeight) {
        this.virtualWidth  = newVirtualWidth;
        this.virtualHeight = newVirtualHeight;
        commitFrame();
    }

    /**
     * Consolida el estado del frame: aplica modificadores y constraints.
     *
     * Debe llamarse al final de cada tick del game loop, después de que
     * CameraController haya actualizado la posición base.
     *
     * CameraSystem (WorldManager) llama este método automáticamente.
     * No es necesario llamarlo desde código de gameplay.
     */
    public void commitFrame() {
        // 1. Calcular estado acumulado de los modificadores
        CameraState state = modifierStack.computeState();
        lastModifierState = state;

        // 2. Aplicar offsets de modificadores a la posición base
        double candidateX = x + state.offsetX;
        double candidateY = y + state.offsetY;

        // 3. Aplicar zoom y rotación efectivos
        effectiveZoom     = zoom     * state.zoomDelta;
        effectiveRotation = rotation + state.rotationDelta;

        // 4. Aplicar constraints en cadena priorizada
        var constrained = constraintList.apply(
            candidateX, candidateY,
            virtualWidth, virtualHeight,
            effectiveZoom
        );

        finalX = constrained.getX();
        finalY = constrained.getY();
    }

    // ── Transformación de vista ───────────────────────────────────────────────

    /**
     * Devuelve la transformación de vista lista para aplicar al Graphics2D.
     *
     * Usa finalX/finalY (ya con modificadores y constraints aplicados).
     *
     * La transformación convierte coordenadas de mundo a coordenadas de
     * pantalla virtual aplicando en orden:
     *   1. Translación (offset de cámara efectivo).
     *   2. Zoom efectivo (base × zoomDelta de modificadores).
     *   3. Rotación efectiva (base + rotationDelta de modificadores).
     *
     * @return transformación de vista como AffineTransform (nueva instancia cada llamada).
     */
    public AffineTransform getViewTransform() {
        AffineTransform t = new AffineTransform();

        if (effectiveZoom != 1.0f || effectiveRotation != 0.0f) {
            double cx = virtualWidth  / 2.0;
            double cy = virtualHeight / 2.0;
            t.translate(cx, cy);
            if (effectiveRotation != 0.0f) t.rotate(effectiveRotation);
            if (effectiveZoom     != 1.0f) t.scale(effectiveZoom, effectiveZoom);
            t.translate(-cx, -cy);
        }

        t.translate(-finalX, -finalY);
        return t;
    }

    // ── Lectura de estado ─────────────────────────────────────────────────────

    /**
     * Offset X efectivo de la cámara (tras modificadores y constraints).
     * screenX = worldX - camera.getX()  (con zoom=1, rotation=0).
     */
    public double getX() { return finalX; }

    /**
     * Offset Y efectivo de la cámara (tras modificadores y constraints).
     */
    public double getY() { return finalY; }

    /**
     * Offset X base de la cámara (sin modificadores ni constraints).
     * Útil para CameraController que trabajan con la posición base.
     */
    public double getBaseX() { return x; }

    /**
     * Offset Y base de la cámara (sin modificadores ni constraints).
     */
    public double getBaseY() { return y; }

    /** Zoom base actual. 1.0 = sin zoom. */
    public float getZoom() { return zoom; }

    /** Zoom efectivo (base × modificadores). Usar en render. */
    public float getEffectiveZoom() { return effectiveZoom; }

    /** Rotación base en radianes. */
    public float getRotation() { return rotation; }

    /** Rotación efectiva (base + modificadores). Usar en render. */
    public float getEffectiveRotation() { return effectiveRotation; }

    /** Ancho virtual configurado. */
    public int getVirtualWidth()  { return virtualWidth;  }

    /** Alto virtual configurado. */
    public int getVirtualHeight() { return virtualHeight; }

    /**
     * Copia defensiva de la posición efectiva como Vector2D.
     * Preferir getX()/getY() cuando solo se necesita el valor escalar.
     */
    public Vector2D getPosition() {
        return new Vector2D(finalX, finalY);
    }

    /**
     * El último CameraState calculado por commitFrame().
     * Usado por el renderer para efectos como letterbox.
     * Puede ser null si commitFrame() no se ha llamado aún.
     */
    public CameraState getLastModifierState() {
        return lastModifierState;
    }

    // ── Privados ──────────────────────────────────────────────────────────────

    /**
     * Establece la posición base y consolida el frame.
     * Punto de convergencia de todas las escrituras de posición.
     */
    private void setBasePosition(double newX, double newY) {
        this.x = newX;
        this.y = newY;
        commitFrame();
    }
}
