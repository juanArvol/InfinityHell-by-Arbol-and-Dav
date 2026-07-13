package Game.Engine.Colisions;

import java.awt.Rectangle;

/**
 * Detección continua de colisiones (Swept AABB).
 *
 * ── Qué hace ────────────────────────────────────────────────────────────
 * Dado un rectángulo en movimiento (moving) con velocidad (vx, vy) y un
 * rectángulo estático (target), calcula:
 *
 *   time     — fracción del movimiento [0..1] en que ocurre el primer contacto.
 *              0   = ya están en contacto exacto (límite con límite).
 *              1   = sin colisión este frame.
 *   normalX/Y — normal de la cara impactada. Indica qué eje frenar.
 *
 * ── Por qué se llama por eje (vx o vy = 0) ──────────────────────────────
 * CollisionsSystem resuelve X e Y por separado para evitar ambigüedad
 * en esquinas. Cuando se llama con vy=0, el eje Y se trata como "libre"
 * (tyEntry=-∞, tyExit=+∞) y solo importa el eje X, y viceversa.
 *
 * ── Edge case: penetración preexistente ─────────────────────────────────
 * Si el objeto ya se solapaba con el bloque en el eje perpendicular
 * (penetración de un frame anterior), la distancia de entrada en ese eje
 * es negativa. El código anterior descartaba entryTime < 0 completamente,
 * lo que causaba que colisiones reales en el eje activo se ignoraran
 * cuando había penetración mínima en el eje pasivo.
 *
 * Solución: solo el eje ACTIVO (el que tiene velocidad) decide la colisión.
 * Si txEntry < 0 pero el eje activo es X, hay penetración → reportar time=0
 * para que el sistema frene inmediatamente (no atraviesa más).
 */
public final class SweptAABB {

    private SweptAABB() {}

    public static final class Result {
        /** Fracción del movimiento hasta el contacto. 1.0 = sin colisión. */
        public final double time;
        /** Normal X: -1 (impacto desde izquierda), +1 (desde derecha), 0. */
        public final int normalX;
        /** Normal Y: -1 (impacto desde arriba / suelo), +1 (desde abajo / techo), 0. */
        public final int normalY;

        public Result(double time, int normalX, int normalY) {
            this.time    = time;
            this.normalX = normalX;
            this.normalY = normalY;
        }

        /** true si hay colisión este frame (time entre 0 inclusive y 1 exclusive). */
        public boolean hasCollision() {
            return time >= 0.0 && time < 1.0;
        }

        public static final Result NONE = new Result(1.0, 0, 0);
    }

    /**
     * Calcula la colisión continua entre un rect en movimiento y uno estático.
     *
     * Diseñado para llamarse con UN solo eje activo a la vez (el otro en 0).
     * CollisionsSystem resuelve primero en X (vy=0), luego en Y (vx=0).
     *
     * @param moving   bounds actuales del objeto (antes de moverse este frame)
     * @param target   bounds del obstáculo estático
     * @param vx       velocidad horizontal (0 si se está resolviendo solo Y)
     * @param vy       velocidad vertical   (0 si se está resolviendo solo X)
     */
    public static Result calculate(Rectangle moving, Rectangle target,
                                   double vx, double vy) {

        if (vx == 0.0 && vy == 0.0) return Result.NONE;

        // ── Distancias de entrada y salida en X ───────────────────────────
        double xEntry, xExit;
        if (vx > 0.0) {
            xEntry = target.x - (moving.x + moving.width);
            xExit  = (target.x + target.width) - moving.x;
        } else if (vx < 0.0) {
            xEntry = (target.x + target.width) - moving.x;
            xExit  = target.x - (moving.x + moving.width);
        } else {
            // Eje X inactivo: sin límite de tiempo en X
            xEntry = Double.NEGATIVE_INFINITY;
            xExit  = Double.POSITIVE_INFINITY;
        }

        // ── Distancias de entrada y salida en Y ───────────────────────────
        double yEntry, yExit;
        if (vy > 0.0) {
            yEntry = target.y - (moving.y + moving.height);
            yExit  = (target.y + target.height) - moving.y;
        } else if (vy < 0.0) {
            yEntry = (target.y + target.height) - moving.y;
            yExit  = target.y - (moving.y + moving.height);
        } else {
            // Eje Y inactivo: sin límite de tiempo en Y
            yEntry = Double.NEGATIVE_INFINITY;
            yExit  = Double.POSITIVE_INFINITY;
        }

        // ── Tiempos normalizados ──────────────────────────────────────────
        double txEntry = (vx != 0.0) ? xEntry / vx : Double.NEGATIVE_INFINITY;
        double txExit  = (vx != 0.0) ? xExit  / vx : Double.POSITIVE_INFINITY;
        double tyEntry = (vy != 0.0) ? yEntry / vy : Double.NEGATIVE_INFINITY;
        double tyExit  = (vy != 0.0) ? yExit  / vy : Double.POSITIVE_INFINITY;

        double entryTime = Math.max(txEntry, tyEntry);
        double exitTime  = Math.min(txExit,  tyExit);

        // Sin superposición de intervalos → sin colisión
        if (entryTime > exitTime || exitTime <= 0.0) return Result.NONE;

        // Colisión fuera del frame actual
        if (entryTime >= 1.0) return Result.NONE;

        // ── Edge case: penetración preexistente ───────────────────────────
        // Si entryTime < 0, el objeto ya estaba solapando el bloque al inicio
        // del frame (penetración de frames anteriores). En lugar de ignorarlo
        // (lo que causa que siga atravesando), lo reportamos con time=0 para
        // que CollisionsSystem frene la velocidad sin mover más en ese eje.
        if (entryTime < 0.0) {
            // Solo reportar si el eje activo realmente se solapa.
            // Verificar que hay superposición real en el eje activo:
            boolean overlapX = (moving.x < target.x + target.width) &&
                               (moving.x + moving.width > target.x);
            boolean overlapY = (moving.y < target.y + target.height) &&
                               (moving.y + moving.height > target.y);

            if (!overlapX || !overlapY) return Result.NONE;

            // Reportar colisión en tiempo 0: frenar sin mover
            int nx = 0, ny = 0;
            if (vx != 0.0) nx = (vx > 0) ? -1 : 1;
            if (vy != 0.0) ny = (vy > 0) ? -1 : 1;
            return new Result(0.0, nx, ny);
        }

        // ── Normal: el eje que entró ÚLTIMO determina la cara impactada ───
        int normalX = 0, normalY = 0;
        if (txEntry >= tyEntry) {
            normalX = (vx > 0.0) ? -1 : 1;
        } else {
            normalY = (vy > 0.0) ? -1 : 1;
        }

        return new Result(entryTime, normalX, normalY);
    }
}
