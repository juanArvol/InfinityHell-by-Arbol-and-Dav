package Game.Engine.Colisions;

import java.awt.Rectangle;

/**
 * Detección continua de colisiones (Swept AABB).
 *
 * ── Mini-HRFC 1.5 — Canonical Physics Units ──────────────────────────────
 *
 * CONTRATO DIMENSIONAL DE PARÁMETROS:
 *
 *   vx, vy — DESPLAZAMIENTO del frame (displacement), NO velocidad.
 *            Debe expresarse en unidades espaciales.
 *            Calculado por CollisionsSystem como: velocity × deltaTime
 *
 *   time   — Fracción del movimiento [0..1] donde ocurre la colisión.
 *            0 = contacto en posición actual (penetración preexistente)
 *            1 = sin colisión; movimiento completo permitido
 *            0.5 = colisión a la mitad del movimiento
 *
 *   INVARIANTE: El algoritmo NO conoce framerate ni tiempo absoluto.
 *               Opera únicamente sobre desplazamientos espaciales.
 *
 * ── Qué hace ────────────────────────────────────────────────────────────
 * Dado un rectángulo en movimiento (moving) con desplazamiento (vx, vy) y un
 * rectángulo estático (target), calcula:
 *
 *   time     — fracción del movimiento [0..1] en que ocurre el primer contacto.
 *   normalX/Y — normal de la cara impactada. Indica qué eje frenar.
 *
 * ── Dos modos de uso ────────────────────────────────────────────────────
 *
 * calculate(moving, target, vx, vy):
 *   Sweep de un solo eje activo (el otro debe ser 0).
 *   Usado por objetos SÓLIDOS — CollisionsSystem resuelve X e Y por separado
 *   para evitar ambigüedad en esquinas al aplicar resolución de movimiento.
 *   Cuando se llama con vy=0, solo importa el eje X, y viceversa.
 *
 * calculate2D(moving, target, vx, vy):
 *   Sweep simultáneo de ambos ejes. Usado por TRIGGERS (bullets, sensores).
 *   Los triggers no aplican resolución de movimiento eje-por-eje, por lo que
 *   el sweep 2D completo es correcto — produce la normal real de la cara
 *   impactada sin el sesgo de la resolución secuencial.
 *   Este es el método que detecta tunneling completo (diagonal incluido).
 *
 * ── Diseño B — política unificada con respuesta especializada ────────────
 *
 * Ambos métodos comparten la misma matemática fundamental. La diferencia
 * entre SÓLIDO y TRIGGER no está en la detección sino en qué ocurre después:
 *
 *   SÓLIDO  → calculate() por eje → resolución de velocidad + ContactState
 *   TRIGGER → calculate2D()       → setLastContactNormal() + dispatch
 *
 * ── Edge case: penetración preexistente ─────────────────────────────────
 * Si el objeto ya se solapaba con el bloque en el eje perpendicular
 * (penetración de un frame anterior), la distancia de entrada en ese eje
 * es negativa. El código anterior descartaba entryTime < 0 completamente,
 * lo que causaba que colisiones reales en el eje activo se ignoraran
 * cuando había penetración mínima en el eje pasivo.
 *
 * Solución (calculate): solo el eje ACTIVO decide la colisión.
 * Si txEntry < 0 pero el eje activo es X, hay penetración → reportar time=0.
 *
 * Solución (calculate2D): ambos ejes activos → si hay overlap en AMBOS,
 * reportar time=0 con la normal del eje de menor penetración.
 *
 * ── Corner normal tie-breaking ───────────────────────────────────────────
 * Cuando txEntry == tyEntry (esquina exacta), ambos ejes "entran" al mismo
 * tiempo y ninguno tiene precedencia geométrica absoluta. En ese caso se
 * usa el eje de mayor velocidad absoluta como desempate: el eje más rápido
 * es el que "alcanza" el borde con mayor certeza. Esto es determinista,
 * independiente del orden de los objetos, y produce una normal coherente
 * con la intención de movimiento del proyectil.
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
     * CollisionsSystem lo usa para objetos SÓLIDOS: primero en X (vy=0),
     * luego en Y (vx=0). Esto evita ambigüedad en esquinas al resolver movimiento.
     *
     * ── Mini-HRFC 1.5: Parámetros como displacement ──────────────────────
     *
     * @param moving   bounds actuales del objeto (antes de moverse este frame)
     * @param target   bounds del obstáculo estático
     * @param vx       DESPLAZAMIENTO horizontal del frame [units], NO velocidad
     * @param vy       DESPLAZAMIENTO vertical del frame [units], NO velocidad
     *
     * IMPORTANTE: vx y vy deben calcularse como velocity × deltaTime antes
     * de llamar a este método. Este algoritmo opera sobre desplazamientos
     * espaciales, no sobre velocidades temporales.
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

        if (entryTime > exitTime || exitTime <= 0.0) return Result.NONE;
        if (entryTime >= 1.0) return Result.NONE;

        // ── Edge case: penetración preexistente ───────────────────────────
        if (entryTime < 0.0) {
            boolean overlapX = (moving.x < target.x + target.width) &&
                               (moving.x + moving.width > target.x);
            boolean overlapY = (moving.y < target.y + target.height) &&
                               (moving.y + moving.height > target.y);

            if (!overlapX || !overlapY) return Result.NONE;

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

    /**
     * Calcula la colisión continua entre un rect en movimiento y uno estático,
     * con ambos ejes activos simultáneamente.
     *
     * ── Cuándo usar este método ────────────────────────────────────────────
     * Usar para TRIGGERS (bullets, sensores, proyectiles) que no necesitan
     * resolución de movimiento eje-por-eje. El sweep 2D completo garantiza:
     *   - Detección correcta de tunneling en movimiento diagonal.
     *   - Normal real de la cara impactada sin sesgo de resolución secuencial.
     *   - CCD completo: la posición puede estar al otro lado del obstáculo y
     *     aún así se detecta el impacto en time < 1.
     *
     * ── Eje estático con velocidad cero ───────────────────────────────────
     * Si vx=0 o vy=0, ese eje debe tener overlap real para que haya colisión.
     * El eje inactivo se trata como "ventana libre" solo si ya hay overlap;
     * si no hay overlap en ese eje, no puede haber colisión.
     * Esto evita falsos positivos cuando el objeto está muy separado en Y
     * pero tiene movimiento solo en X.
     *
     * ── Edge case: penetración preexistente ───────────────────────────────
     * Si entryTime < 0 (ya solapados), se reporta time=0 con la normal del
     * eje de menor penetración (más probable que sea el eje correcto).
     *
     * ── Corner normal tie-breaking ───────────────────────────────────────────
     * Cuando txEntry == tyEntry (esquina exacta o near-corner dentro de 1e-8),
     * el desempate se hace por el eje de mayor velocidad absoluta. Esto es
     * determinista e independiente del orden de los objetos.
     *
     * ── Mini-HRFC 1.5: Parámetros como displacement ──────────────────────
     *
     * @param moving   bounds actuales del objeto (antes de moverse este frame)
     * @param target   bounds del obstáculo estático
     * @param vx       DESPLAZAMIENTO horizontal del frame [units], NO velocidad
     * @param vy       DESPLAZAMIENTO vertical del frame [units], NO velocidad
     *
     * IMPORTANTE: vx y vy deben calcularse como velocity × deltaTime antes
     * de llamar a este método. Este algoritmo opera sobre desplazamientos
     * espaciales, no sobre velocidades temporales.
     */
    public static Result calculate2D(Rectangle moving, Rectangle target,
                                     double vx, double vy) {

        if (vx == 0.0 && vy == 0.0) return Result.NONE;

        // ── Verificación de overlap en ejes inactivos ─────────────────────
        // Si un eje tiene velocidad cero, el objeto no se acercará ni alejará
        // en ese eje este frame. Para que haya colisión, debe haber overlap
        // real en ese eje ya desde el inicio del frame.
        if (vx == 0.0) {
            boolean overlapX = (moving.x < target.x + target.width) &&
                               (moving.x + moving.width > target.x);
            if (!overlapX) return Result.NONE;
        }
        if (vy == 0.0) {
            boolean overlapY = (moving.y < target.y + target.height) &&
                               (moving.y + moving.height > target.y);
            if (!overlapY) return Result.NONE;
        }

        // ── Distancias de entrada y salida en X ───────────────────────────
        double txEntry, txExit;
        if (vx > 0.0) {
            txEntry = (target.x - (moving.x + moving.width)) / vx;
            txExit  = ((target.x + target.width) - moving.x) / vx;
        } else if (vx < 0.0) {
            txEntry = ((target.x + target.width) - moving.x) / vx;
            txExit  = (target.x - (moving.x + moving.width)) / vx;
        } else {
            // Eje X estático con overlap confirmado arriba
            txEntry = Double.NEGATIVE_INFINITY;
            txExit  = Double.POSITIVE_INFINITY;
        }

        // ── Distancias de entrada y salida en Y ───────────────────────────
        double tyEntry, tyExit;
        if (vy > 0.0) {
            tyEntry = (target.y - (moving.y + moving.height)) / vy;
            tyExit  = ((target.y + target.height) - moving.y) / vy;
        } else if (vy < 0.0) {
            tyEntry = ((target.y + target.height) - moving.y) / vy;
            tyExit  = (target.y - (moving.y + moving.height)) / vy;
        } else {
            // Eje Y estático con overlap confirmado arriba
            tyEntry = Double.NEGATIVE_INFINITY;
            tyExit  = Double.POSITIVE_INFINITY;
        }

        // ── Overlap de intervalos ─────────────────────────────────────────
        double entryTime = Math.max(txEntry, tyEntry);
        double exitTime  = Math.min(txExit,  tyExit);

        // Sin superposición de intervalos → sin colisión
        if (entryTime > exitTime || exitTime <= 0.0) return Result.NONE;

        // Colisión fuera del frame actual
        if (entryTime >= 1.0) return Result.NONE;

        // ── Edge case: penetración preexistente ───────────────────────────
        if (entryTime < 0.0) {
            boolean overlapX = (moving.x < target.x + target.width) &&
                               (moving.x + moving.width > target.x);
            boolean overlapY = (moving.y < target.y + target.height) &&
                               (moving.y + moving.height > target.y);

            if (!overlapX || !overlapY) return Result.NONE;

            double penX = vx != 0 ? Math.min(
                    (moving.x + moving.width) - target.x,
                    (target.x + target.width) - moving.x
            ) : Double.MAX_VALUE;
            double penY = vy != 0 ? Math.min(
                    (moving.y + moving.height) - target.y,
                    (target.y + target.height) - moving.y
            ) : Double.MAX_VALUE;

            int nx = 0, ny = 0;
            if (penX <= penY && vx != 0.0) {
                nx = (vx > 0) ? -1 : 1;
            } else if (vy != 0.0) {
                ny = (vy > 0) ? -1 : 1;
            } else if (vx != 0.0) {
                nx = (vx > 0) ? -1 : 1;
            }
            return new Result(0.0, nx, ny);
        }

        // ── Normal: el eje que entró ÚLTIMO determina la cara impactada ───
        //
        // Tie-breaking para esquina exacta (txEntry == tyEntry):
        //   Cuando ambos ejes alcanzan el borde al mismo instante (vértice de
        //   esquina), ninguno tiene precedencia geométrica absoluta. Se usa el
        //   eje de mayor velocidad absoluta como desempate: el eje más rápido
        //   contribuye más al movimiento y es el eje de contacto primario.
        //   Esto produce una normal determinista e independiente del orden de
        //   objetos, coherente con la dirección de movimiento del proyectil.
        //
        //   TOLERANCIA: se compara con un epsilon para capturar casos near-corner
        //   donde floating point produce txEntry ≈ tyEntry pero la geometría es
        //   una esquina. El epsilon es 1e-8 — suficientemente pequeño para no
        //   afectar casos de contacto de cara normal (~1/velocidad de diferencia).
        int normalX = 0, normalY = 0;
        double cornerEps = 1e-8;
        boolean txDominates;
        if (Math.abs(txEntry - tyEntry) <= cornerEps) {
            // Esquina exacta o near-corner: desempate por velocidad
            txDominates = Math.abs(vx) >= Math.abs(vy);
        } else {
            txDominates = txEntry > tyEntry;
        }

        if (txDominates) {
            normalX = (vx > 0.0) ? -1 : 1;
        } else {
            normalY = (vy > 0.0) ? -1 : 1;
        }

        return new Result(entryTime, normalX, normalY);
    }

    /**
     * Calcula el bounding box del swept path de un rect con velocidad (vx, vy).
     *
     * El swept AABB es el AABB que contiene todas las posiciones que el objeto
     * puede ocupar durante el movimiento completo de un frame. Se usa como
     * broad phase: cualquier obstáculo que NO intersecte con este bounds
     * es imposible que sea impactado por el objeto en este frame.
     *
     * ── Mini-HRFC 1.5: Parámetros como displacement ──────────────────────
     *
     * @param bounds  bounds actuales del objeto en movimiento
     * @param vx      DESPLAZAMIENTO horizontal del frame [units], NO velocidad
     * @param vy      DESPLAZAMIENTO vertical del frame [units], NO velocidad
     * @return Rectangle que envuelve el path completo del frame
     */
    public static Rectangle sweptBounds(Rectangle bounds, double vx, double vy) {
        int x = bounds.x, y = bounds.y;
        int w = bounds.width, h = bounds.height;

        int newX = (int)(vx < 0 ? x + vx : x);
        int newY = (int)(vy < 0 ? y + vy : y);
        int newW = (int)(w + Math.abs(vx)) + 1;
        int newH = (int)(h + Math.abs(vy)) + 1;

        return new Rectangle(newX, newY, newW, newH);
    }
}
