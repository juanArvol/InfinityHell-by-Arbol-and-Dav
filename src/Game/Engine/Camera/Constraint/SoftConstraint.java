package Game.Engine.Camera.Constraint;

import Game.Engine.GameMath.Logic2D.Vector2D;

/**
 * Restricción suave: resiste el movimiento cuando la cámara se acerca al límite.
 *
 * ── DIFERENCIA CON HardConstraint ────────────────────────────────────────
 * HardConstraint bloquea absolutamente: la cámara no puede pasar del límite.
 * SoftConstraint aplica resistencia: la cámara puede acercarse pero se
 * desacelera, nunca bloqueándose del todo (útil para mundos muy grandes
 * donde el bloqueo brusco se siente antinatural).
 *
 * ── FUNCIONAMIENTO ────────────────────────────────────────────────────────
 * Cuando la posición deseada supera softZone píxeles del límite,
 * la posición resultante se resiste linealmente: por cada píxel extra
 * más allá de softZone, el resultado se mueve solo resistanceFactor píxeles.
 *
 * Con resistanceFactor = 0.3:
 *   10px más allá del softZone → solo 3px de movimiento real
 *   50px más allá → 15px reales
 *   El límite real nunca se alcanza (convergencia asintótica)
 */
public final class SoftConstraint implements CameraConstraint {

    private final double minX;
    private final double minY;
    private final double maxX;
    private final double maxY;
    private final double softZone;       // píxeles antes del límite donde empieza la resistencia
    private final float  resistanceFactor; // [0.0, 1.0]; 0 = bloqueo total, 1 = sin resistencia
    private final int    priority;

    /**
     * @param minX, minY       límites mínimos del mundo (en coords de mundo)
     * @param maxX, maxY       límites máximos del mundo
     * @param softZone         píxeles de margen donde empieza la resistencia
     * @param resistanceFactor factor de resistencia [0.0, 1.0]
     */
    public SoftConstraint(double minX, double minY, double maxX, double maxY,
                          double softZone, float resistanceFactor) {
        this(minX, minY, maxX, maxY, softZone, resistanceFactor, 800);
    }

    public SoftConstraint(double minX, double minY, double maxX, double maxY,
                          double softZone, float resistanceFactor, int priority) {
        this.minX              = minX;
        this.minY              = minY;
        this.maxX              = maxX;
        this.maxY              = maxY;
        this.softZone          = softZone;
        this.resistanceFactor  = Math.max(0.0f, Math.min(1.0f, resistanceFactor));
        this.priority          = priority;
    }

    @Override
    public Vector2D constrain(double desiredX, double desiredY,
                               int virtualWidth, int virtualHeight, float zoom) {
        double visW = virtualWidth  / (double) zoom;
        double visH = virtualHeight / (double) zoom;

        double x = applyResistance(desiredX, minX, maxX - visW);
        double y = applyResistance(desiredY, minY, maxY - visH);

        return new Vector2D(x, y);
    }

    private double applyResistance(double desired, double min, double max) {
        double softMin = min + softZone;
        double softMax = max - softZone;

        if (desired < softMin) {
            double excess = softMin - desired;
            return softMin - excess * resistanceFactor;
        }
        if (desired > softMax) {
            double excess = desired - softMax;
            return softMax + excess * resistanceFactor;
        }
        return desired;
    }

    @Override
    public int getPriority() { return priority; }
}
