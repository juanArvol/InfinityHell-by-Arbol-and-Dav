package Game.Engine.World.Fields;

import Game.Engine.GameObjects;
import Game.Engine.GameMath.Logic2D.Vector2D;

/**
 * Campo vectorial — aplica una fuerza (fx, fy) sobre los objetos en su radio.
 *
 * ── HRFC-015 — World Simulation Core ──────────────────────────────────────
 *
 * ── QUÉ ES VectorField ───────────────────────────────────────────────────
 * Un VectorField representa cualquier campo físico cuya influencia sobre un
 * objeto se expresa como un vector de fuerza aplicado a su física:
 *
 *   Campo gravitacional   → fuerza hacia abajo (o hacia un punto)
 *   Campo de viento       → fuerza lateral constante
 *   Campo magnético       → fuerza perpendicular a la velocidad del objeto
 *   Campo de impulso      → fuerza radial hacia afuera (explosión) o hacia adentro
 *
 * VectorField no sabe qué tipo de entidad recibe la fuerza. Lo único que hace
 * es calcular el vector de fuerza efectivo y delegar su aplicación al
 * VectorApplicator inyectado en construcción.
 *
 * ── DIRECCIÓN DEL CAMPO ──────────────────────────────────────────────────
 * Un VectorField puede configurarse en tres modos:
 *
 *   FIXED       → dirección fija (fx, fy) para todos los objetos en el campo.
 *                 Ejemplo: viento constante hacia la derecha, gravedad hacia abajo.
 *
 *   RADIAL_OUT  → fuerza radial desde el centro hacia el objeto (explosión, repulsión).
 *                 La dirección varía por objeto — siempre apunta hacia afuera.
 *
 *   RADIAL_IN   → fuerza radial desde el objeto hacia el centro (atracción, agujero negro).
 *                 La dirección varía por objeto — siempre apunta hacia adentro.
 *
 * ── VectorApplicator ─────────────────────────────────────────────────────
 * Como en ScalarField, el applicator desacopla el campo de los componentes concretos.
 * El applicator más común para VectorField usa Physics2D.accumulate():
 *
 *   // Campo gravitacional (WorldFieldPresets o código de gameplay):
 *   new VectorField.Builder()
 *       .direction(VectorFieldMode.RADIAL_IN)
 *       .intensity(9.8)
 *       .applicator((obj, fx, fy) -> {
 *           Physics2DComponent pc = obj.getComponent(Physics2DComponent.class);
 *           if (pc != null) pc.getPhysics().accumulate(fx, fy);
 *       })
 *       .build();
 *
 * CollisionsSystem FASE 0.5 ya llama flushAccumulatedForces() — las fuerzas
 * acumuladas por VectorField se integran en el step correcto sin ningún cambio
 * en la infraestructura existente.
 */
public final class VectorField extends WorldField<double[]> {

    // ── Modo de dirección ─────────────────────────────────────────────────

    /**
     * Determina cómo se calcula la dirección del campo para cada objeto destino.
     */
    public enum VectorFieldMode {
        /** Dirección fija para todos los objetos. */
        FIXED,
        /** Fuerza radial hacia afuera desde el centro (repulsión, explosión). */
        RADIAL_OUT,
        /** Fuerza radial hacia adentro hacia el centro (atracción, gravedad). */
        RADIAL_IN
    }

    /**
     * Función de aplicación del campo vectorial sobre un objeto.
     * Recibe el objeto destino y las componentes de fuerza (fx, fy).
     */
    @FunctionalInterface
    public interface VectorApplicator {
        /**
         * Aplica la fuerza al componente de física u otro componente del objeto.
         *
         * @param target objeto sobre el que se aplica el campo.
         * @param fx     componente horizontal de la fuerza efectiva.
         * @param fy     componente vertical de la fuerza efectiva.
         */
        void apply(GameObjects target, double fx, double fy);
    }

    private final VectorFieldMode   mode;
    private final double            baseFx;
    private final double            baseFy;
    private final VectorApplicator  applicator;

    private VectorField(Builder b) {
        super(b);
        this.mode       = b.mode;
        this.baseFx     = b.baseFx;
        this.baseFy     = b.baseFy;
        this.applicator = b.applicator;
    }

    // ── Aplicación ────────────────────────────────────────────────────────

    /**
     * Calcula el vector de fuerza efectivo para el objeto en su posición y
     * delega en el VectorApplicator.
     *
     * Para FIXED: escala la dirección base por la intensidad efectiva.
     * Para RADIAL_OUT/IN: calcula la dirección desde el centro al objeto y
     * la escala por la intensidad efectiva.
     *
     * @param target    objeto destino (ya en rango).
     * @param intensity intensidad efectiva ya atenuada por el falloff.
     */
    @Override
    protected void applyEffect(GameObjects target, double intensity) {
        double fx, fy;

        switch (mode) {
            case FIXED -> {
                // Normalizar la dirección base y escalar por intensidad
                double len = Math.hypot(baseFx, baseFy);
                if (len < 1e-9) return;
                fx = (baseFx / len) * intensity;
                fy = (baseFy / len) * intensity;
            }
            case RADIAL_OUT -> {
                Vector2D toTarget = directionFromCenter(target);
                fx = toTarget.getX() * intensity;
                fy = toTarget.getY() * intensity;
            }
            case RADIAL_IN -> {
                Vector2D toTarget = directionFromCenter(target);
                fx = -toTarget.getX() * intensity;
                fy = -toTarget.getY() * intensity;
            }
            default -> { return; }
        }

        applicator.apply(target, fx, fy);
    }

    /**
     * Calcula el vector normalizado desde el centro del campo hacia el objeto.
     * Si el objeto está exactamente en el centro, retorna (0, 0).
     */
    private Vector2D directionFromCenter(GameObjects target) {
        Vector2D center = getPosition();
        Vector2D pos    = target.getTransform().getPosition();
        double dx = pos.getX() - center.getX();
        double dy = pos.getY() - center.getY();
        double len = Math.hypot(dx, dy);
        if (len < 1e-9) return new Vector2D(0, 0);
        return new Vector2D(dx / len, dy / len);
    }

    // ── Accesores ─────────────────────────────────────────────────────────

    /** Modo de dirección configurado. */
    public VectorFieldMode getMode() { return mode; }

    /** Componente X de la dirección base (solo relevante en modo FIXED). */
    public double getBaseFx()        { return baseFx; }

    /** Componente Y de la dirección base (solo relevante en modo FIXED). */
    public double getBaseFy()        { return baseFy; }

    // ── Builder ───────────────────────────────────────────────────────────

    /** Crea un Builder de VectorField. */
    public static Builder builder() { return new Builder(); }

    /**
     * Builder de VectorField.
     * Extiende WorldField.Builder con parámetros de dirección y applicator.
     */
    public static final class Builder extends WorldField.Builder<Builder> {

        private VectorFieldMode  mode       = VectorFieldMode.FIXED;
        private double           baseFx     = 0.0;
        private double           baseFy     = 1.0;  // hacia abajo por defecto
        private VectorApplicator applicator;

        /**
         * Modo de dirección del campo.
         * Por defecto: FIXED con dirección (0, 1) — gravedad hacia abajo.
         */
        public Builder mode(VectorFieldMode m)     { this.mode = m; return this; }

        /**
         * Dirección base del campo en modo FIXED.
         * No necesita estar normalizada — se normaliza al aplicar.
         *
         * @param fx componente horizontal.
         * @param fy componente vertical.
         */
        public Builder direction(double fx, double fy) {
            this.baseFx = fx;
            this.baseFy = fy;
            return this;
        }

        /**
         * Define la función de aplicación del campo sobre el objeto destino.
         * Parámetro obligatorio.
         *
         * @param a función que recibe (GameObjects, double fx, double fy).
         */
        public Builder applicator(VectorApplicator a) {
            this.applicator = a;
            return this;
        }

        /**
         * Construye el VectorField.
         *
         * @throws IllegalStateException si applicator es null.
         */
        public VectorField build() {
            if (applicator == null) {
                throw new IllegalStateException(
                    "VectorField requiere un VectorApplicator. " +
                    "Usar .applicator((obj, fx, fy) -> { ... }) en el Builder.");
            }
            return new VectorField(this);
        }
    }
}
