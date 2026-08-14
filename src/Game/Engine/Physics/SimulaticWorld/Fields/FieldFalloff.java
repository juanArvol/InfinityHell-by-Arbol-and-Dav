package Game.Engine.Physics.SimulaticWorld.Fields;

/**
 * Función de atenuación de la intensidad de un campo con la distancia.
 *
 * ── HRFC-015 — World Simulation Core ──────────────────────────────────────
 *
 * Determina cómo decrece la intensidad de un WorldField entre su origen y el
 * borde de su radio de influencia. La simulación multiplica la intensidad base
 * del campo por el factor que retorna esta función antes de aplicarla.
 *
 * Modelos disponibles:
 *
 *   CONSTANT   — intensidad uniforme en todo el radio.
 *                factor(d) = 1.0
 *                Usar para campos uniformes (zonas de temperatura, niebla tóxica).
 *
 *   LINEAR     — intensidad decrece linealmente desde el centro al borde.
 *                factor(d) = 1 − (d / radius)
 *                Usar para la mayoría de campos físicos (calor, electricidad, presión).
 *
 *   QUADRATIC  — intensidad decrece con el cuadrado de la distancia.
 *                factor(d) = (1 − d/radius)²
 *                Simula atenuación natural de campos que se propagan en área.
 *                Usar para gravedad, campos magnéticos, explosiones.
 *
 *   INVERSE_SQUARE — ley del inverso del cuadrado (física clásica).
 *                factor(d) = 1 / (1 + (d/radius)²)
 *                Nunca llega a cero, lo que evita divisiones y discontinuidades.
 *                Usar para campos que modelan fuerzas a larga distancia.
 *
 *   STEP       — intensidad completa hasta cierta fracción del radio, luego cero.
 *                factor(d) = d <= cutoff * radius ? 1.0 : 0.0
 *                Usar para campos de borde duro (escudos, barreras, zonas mágicas).
 *                El parámetro de corte se configura en WorldField.
 *
 * La elección del falloff no tiene lógica de gameplay — solo describe la
 * forma matemática del campo. Los módulos de simulación y el InteractionRegistry
 * deciden qué hacer con la intensidad resultante.
 */
public enum FieldFalloff {

    /** Intensidad uniforme en todo el radio. */
    CONSTANT {
        @Override
        public double compute(double normalizedDistance, double cutoff) {
            return 1.0;
        }
    },

    /** Atenuación lineal de centro a borde. */
    LINEAR {
        @Override
        public double compute(double normalizedDistance, double cutoff) {
            return Math.max(0.0, 1.0 - normalizedDistance);
        }
    },

    /** Atenuación cuadrática de centro a borde. */
    QUADRATIC {
        @Override
        public double compute(double normalizedDistance, double cutoff) {
            double linear = Math.max(0.0, 1.0 - normalizedDistance);
            return linear * linear;
        }
    },

    /** Ley del inverso del cuadrado. Nunca llega a cero. */
    INVERSE_SQUARE {
        @Override
        public double compute(double normalizedDistance, double cutoff) {
            double d = normalizedDistance;
            return 1.0 / (1.0 + d * d);
        }
    },

    /**
     * Intensidad completa hasta {@code cutoff × radius}, luego cero.
     * Si cutoff es 0.8, el campo está a plena intensidad en el 80%
     * interior del radio y cae a cero abruptamente en el 20% exterior.
     */
    STEP {
        @Override
        public double compute(double normalizedDistance, double cutoff) {
            return normalizedDistance <= cutoff ? 1.0 : 0.0;
        }
    };

    /**
     * Calcula el factor de atenuación para una distancia normalizada.
     *
     * @param normalizedDistance distancia al centro dividida por el radio del campo,
     *                           en [0, 1]. 0 = en el centro, 1 = en el borde.
     * @param cutoff             parámetro de corte para STEP. Ignorado por el resto.
     * @return factor de atenuación en [0, 1].
     */
    public abstract double compute(double normalizedDistance, double cutoff);

    /**
     * Variante sin parámetro de corte (equivale a cutoff = 1.0).
     * Conveniente para todos los falloffs excepto STEP.
     */
    public double compute(double normalizedDistance) {
        return compute(normalizedDistance, 1.0);
    }
}
