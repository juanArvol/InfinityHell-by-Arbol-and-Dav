package Game.Engine.Camera.Constraint;

import Game.Engine.GameMath.Logic2D.Vector2D;

/**
 * Restricción que impide que la cámara muestre área fuera del mundo.
 *
 * ── REEMPLAZA EL CLAMP HARDCODEADO EN GameCamera ──────────────────────────
 * GameCamera.applyClamp() tenía un clamp fijo que siempre limitaba a >= 0
 * y no podía ser desactivado por constraint individual. Ahora el clamp
 * es una constraint como cualquier otra, con prioridad máxima (1000).
 *
 * ── LIMITACIÓN DEL CLAMP ANTERIOR ────────────────────────────────────────
 * El clamp anterior nunca permitía valores negativos, lo que impedía
 * representar mundos con coordenadas negativas. WorldBoundsConstraint
 * puede configurarse con minX/minY negativos para mundos extendidos.
 *
 * ── PRIORIDAD ─────────────────────────────────────────────────────────────
 * Prioridad 1000 → siempre la última en aplicarse, siempre gana.
 * Es el límite definitivo que ninguna otra constraint puede sobrepasar.
 */
public final class WorldBoundsConstraint implements CameraConstraint {

    private double minX;
    private double minY;
    private double maxX;
    private double maxY;
    private boolean enabled;

    /**
     * Crea la restricción con los límites del mundo.
     *
     * @param worldWidth  ancho del mundo
     * @param worldHeight alto del mundo
     */
    public WorldBoundsConstraint(int worldWidth, int worldHeight) {
        update(worldWidth, worldHeight);
        this.enabled = (worldWidth > 0 && worldHeight > 0);
    }

    /**
     * Crea la restricción deshabilitada (cámara libre).
     */
    public WorldBoundsConstraint() {
        this.minX    = 0;
        this.minY    = 0;
        this.maxX    = Double.MAX_VALUE;
        this.maxY    = Double.MAX_VALUE;
        this.enabled = false;
    }

    /**
     * Actualiza los límites del mundo.
     * Llamar cuando el mundo cambie de tamaño o se cambie de sector.
     */
    public void update(int worldWidth, int worldHeight) {
        this.minX    = 0;
        this.minY    = 0;
        this.maxX    = worldWidth;
        this.maxY    = worldHeight;
        this.enabled = (worldWidth > 0 && worldHeight > 0);
    }

    /**
     * Actualiza con límites explícitos (para mundos con coordenadas negativas).
     */
    public void update(double minX, double minY, double maxX, double maxY) {
        this.minX    = minX;
        this.minY    = minY;
        this.maxX    = maxX;
        this.maxY    = maxY;
        this.enabled = true;
    }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    @Override
    public Vector2D constrain(double desiredX, double desiredY,
                               int virtualWidth, int virtualHeight, float zoom) {
        if (!enabled) return new Vector2D(desiredX, desiredY);

        // Área visible = virtualDim / zoom
        double visW = virtualWidth  / (double) zoom;
        double visH = virtualHeight / (double) zoom;

        // El max de la posición top-left es (worldDim - visibleArea)
        double clampMaxX = Math.max(minX, maxX - visW);
        double clampMaxY = Math.max(minY, maxY - visH);

        double x = Math.max(minX, Math.min(desiredX, clampMaxX));
        double y = Math.max(minY, Math.min(desiredY, clampMaxY));

        return new Vector2D(x, y);
    }

    @Override
    public boolean isActive() { return enabled; }

    @Override
    public int getPriority() { return 1000; } // Siempre la última en aplicarse
}
