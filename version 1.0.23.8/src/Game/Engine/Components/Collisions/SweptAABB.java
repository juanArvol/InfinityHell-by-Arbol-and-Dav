package Game.Engine.Components.Collisions;

import java.awt.Rectangle;

/**
 * Detección continua de colisiones (Swept AABB).
 *
 * Resuelve el problema del "bullet through paper": cuando un objeto
 * va muy rápido, el AABB estándar no detecta la colisión porque
 * el objeto "saltó" de un lado al otro en un solo frame.
 *
 * ── Qué hace ────────────────────────────────────────────────────────────
 * Dado un rectángulo en movimiento (moving) con una velocidad (vx, vy)
 * y un rectángulo estático (target), calcula:
 *
 *   - time: fracción del movimiento [0..1] en que ocurre el primer contacto.
 *           0 = ya están tocándose. 1 = no hay colisión este frame.
 *   - normalX/normalY: normal de la cara del target que se tocó primero.
 *                      Usada para saber si el impacto fue lateral (X) o vertical (Y).
 *
 * ── Cómo usar el resultado ───────────────────────────────────────────────
 *   SweptAABB.Result r = SweptAABB.calculate(myBounds, wallBounds, vx, vy);
 *   if (r.hasCollision()) {
 *       // Mover solo hasta el punto de contacto:
 *       pos.x += vx * r.time;
 *       pos.y += vy * r.time;
 *       // Cancelar el eje que chocó:
 *       if (r.normalX != 0) vx = 0;
 *       if (r.normalY != 0) vy = 0;
 *   }
 *
 * ── Limitación conocida ──────────────────────────────────────────────────
 * Solo funciona con rectangulos AABB (no rotados).
 * El target debe ser estático (velocidad = 0). Para colisión entre
 * dos objetos móviles, restar la velocidad relativa antes de llamar.
 */
public final class SweptAABB {

    private SweptAABB() {}

    public static final class Result {
        /** Fracción del movimiento hasta el contacto. 1.0 = sin colisión. */
        public final double time;
        /** Normal X de la cara impactada: -1 (impacto desde izquierda), +1 (desde derecha), 0. */
        public final int normalX;
        /** Normal Y de la cara impactada: -1 (impacto desde arriba), +1 (desde abajo), 0. */
        public final int normalY;

        public Result(double time, int normalX, int normalY) {
            this.time    = time;
            this.normalX = normalX;
            this.normalY = normalY;
        }

        /** @return true si hay colisión este frame (time está entre 0 y 1 exclusive). */
        public boolean hasCollision() {
            return time >= 0.0 && time < 1.0;
        }

        /** Resultado vacío: sin colisión. */
        public static final Result NONE = new Result(1.0, 0, 0);
    }

    /**
     * Calcula la colisión continua entre un rect en movimiento y uno estático.
     *
     * @param moving    bounds del objeto en movimiento (posición ACTUAL, antes de moverse)
     * @param target    bounds del objeto estático
     * @param velocityX velocidad horizontal del objeto en movimiento este frame
     * @param velocityY velocidad vertical del objeto en movimiento este frame
     * @return resultado con time y normal. Si time=1.0, no hay colisión.
     */
    public static Result calculate(Rectangle moving, Rectangle target,
                                   double velocityX, double velocityY) {

        // Si la velocidad es cero en ambos ejes, no hay movimiento → sin colisión
        if (velocityX == 0.0 && velocityY == 0.0) return Result.NONE;

        // Distancias de entrada y salida en X
        double xEntry, xExit;
        if (velocityX > 0.0) {
            xEntry = target.x - (moving.x + moving.width);
            xExit  = (target.x + target.width) - moving.x;
        } else if (velocityX < 0.0) {
            xEntry = (target.x + target.width) - moving.x;
            xExit  = target.x - (moving.x + moving.width);
        } else {
            xEntry = Double.NEGATIVE_INFINITY;
            xExit  = Double.POSITIVE_INFINITY;
        }

        // Distancias de entrada y salida en Y
        double yEntry, yExit;
        if (velocityY > 0.0) {
            yEntry = target.y - (moving.y + moving.height);
            yExit  = (target.y + target.height) - moving.y;
        } else if (velocityY < 0.0) {
            yEntry = (target.y + target.height) - moving.y;
            yExit  = target.y - (moving.y + moving.height);
        } else {
            yEntry = Double.NEGATIVE_INFINITY;
            yExit  = Double.POSITIVE_INFINITY;
        }

        // Tiempos normalizados [0..1]
        double txEntry = (velocityX != 0.0) ? xEntry / velocityX : Double.NEGATIVE_INFINITY;
        double txExit  = (velocityX != 0.0) ? xExit  / velocityX : Double.POSITIVE_INFINITY;
        double tyEntry = (velocityY != 0.0) ? yEntry / velocityY : Double.NEGATIVE_INFINITY;
        double tyExit  = (velocityY != 0.0) ? yExit  / velocityY : Double.POSITIVE_INFINITY;

        double entryTime = Math.max(txEntry, tyEntry);
        double exitTime  = Math.min(txExit,  tyExit);

        // Sin colisión: el objeto salió antes de entrar, o la entrada es en el futuro lejano
        if (entryTime > exitTime || entryTime >= 1.0 || exitTime <= 0.0) {
            return Result.NONE;
        }

        // Clamp: no reportar colisiones que ya ocurrieron antes de este frame
        if (entryTime < 0.0) return Result.NONE;

        // Normal: el eje que entró ÚLTIMO es el que chocó
        int normalX = 0, normalY = 0;
        if (txEntry > tyEntry) {
            normalX = (velocityX > 0.0) ? -1 : 1;
        } else {
            normalY = (velocityY > 0.0) ? -1 : 1;
        }

        return new Result(entryTime, normalX, normalY);
    }
}
