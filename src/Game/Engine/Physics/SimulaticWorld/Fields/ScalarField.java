package Game.Engine.Physics.SimulaticWorld.Fields;

import Game.Engine.GameObjects;

/**
 * Campo escalar — modifica un valor double en un componente del objeto destino.
 *
 * ── HRFC-015 — World Simulation Core ──────────────────────────────────────
 *
 * ── QUÉ ES ScalarField ───────────────────────────────────────────────────
 * Un ScalarField representa cualquier campo físico cuya influencia sobre
 * un objeto se expresa como un delta numérico aplicado a una propiedad escalar:
 *
 *   Campo térmico      → Δtemperatura al ThermalComponent del destino
 *   Campo de humedad   → Δhumedad al FluidComponent del destino
 *   Campo de presión   → Δpresión al PressureComponent del destino
 *   Campo de carga     → Δcarga al ElectricalComponent del destino
 *
 * ScalarField no sabe a qué componente aplica. Eso lo define el ScalarApplicator
 * inyectado en construcción — una función que recibe el objeto y el delta, y
 * realiza la modificación sobre el componente correcto.
 *
 * ── POR QUÉ ScalarApplicator EN VEZ DE SUBCLASES ─────────────────────────
 * Si ScalarField conociera ThermalComponent o ElectricalComponent directamente,
 * necesitaría importarlos — creando dependencias cruzadas dentro del Engine.
 * Peor aún: habría que crear ThermalScalarField, ElectricalScalarField,
 * FluidScalarField, PressureScalarField... duplicando toda la lógica de campo.
 *
 * Con ScalarApplicator, ScalarField es completamente genérico. El código que
 * crea el campo inyecta la lógica de aplicación:
 *
 *   // Campo térmico (WorldFieldPresets o código de gameplay):
 *   new ScalarField.Builder()
 *       .position(x, y)
 *       .radius(120)
 *       .intensity(15.0)
 *       .falloff(FieldFalloff.LINEAR)
 *       .applicator((obj, delta) -> {
 *           ThermalComponent tc = obj.getComponent(ThermalComponent.class);
 *           if (tc != null) tc.addHeat(delta);
 *       })
 *       .build();
 *
 *   // Campo de presión:
 *   new ScalarField.Builder()
 *       .intensity(-50.0)     // subpresión (vacío)
 *       .applicator((obj, delta) -> {
 *           PressureComponent pc = obj.getComponent(PressureComponent.class);
 *           if (pc != null) pc.addPressure(delta);
 *       })
 *       .build();
 *
 * ScalarField permanece libre de importaciones de componentes concretos.
 *
 * ── PRESETS ───────────────────────────────────────────────────────────────
 * Los constructores de campos más comunes (ThermalField, ElectricField, etc.)
 * viven en WorldFieldPresets (Fase I). Son factories que crean ScalarField
 * con el applicator correcto preconfigurado.
 */
public final class ScalarField extends WorldField<Double> {

    /**
     * Función de aplicación del campo escalar sobre un objeto.
     * Recibe el objeto destino y el delta calculado (intensidad efectiva × dt).
     * Se encarga de encontrar el componente correcto y modificarlo.
     */
    @FunctionalInterface
    public interface ScalarApplicator {
        /**
         * Aplica el delta al componente de estado correspondiente del objeto.
         *
         * @param target objeto sobre el que se aplica el campo.
         * @param delta  valor a añadir al estado. Puede ser positivo o negativo.
         */
        void apply(GameObjects target, double delta);
    }

    private final ScalarApplicator applicator;

    private ScalarField(Builder b) {
        super(b);
        this.applicator = b.applicator;
    }

    // ── Aplicación ────────────────────────────────────────────────────────

    /**
     * Aplica la intensidad efectiva del campo sobre el componente de estado
     * del objeto mediante el ScalarApplicator configurado.
     *
     * @param target    objeto destino (ya verificado en rango por WorldField.applyTo).
     * @param intensity intensidad efectiva ya atenuada por el falloff.
     */
    @Override
    protected void applyEffect(GameObjects target, double intensity) {
        applicator.apply(target, intensity);
    }

    // ── Builder ───────────────────────────────────────────────────────────

    /** Crea un Builder de ScalarField. */
    public static Builder builder() { return new Builder(); }

    /**
     * Builder de ScalarField.
     * Extiende WorldField.Builder con el parámetro obligatorio: applicator.
     */
    public static final class Builder extends WorldField.Builder<Builder> {

        private ScalarApplicator applicator;

        /**
         * Define la función de aplicación del campo sobre el objeto destino.
         * Parámetro obligatorio — sin applicator el campo no tiene efecto.
         *
         * @param a función que recibe (GameObjects, double) y modifica el componente.
         */
        public Builder applicator(ScalarApplicator a) {
            this.applicator = a;
            return this;
        }

        /**
         * Construye el ScalarField.
         *
         * @throws IllegalStateException si applicator es null.
         */
        public ScalarField build() {
            if (applicator == null) {
                throw new IllegalStateException(
                    "ScalarField requiere un ScalarApplicator. " +
                    "Usar .applicator((obj, delta) -> { ... }) en el Builder.");
            }
            return new ScalarField(this);
        }
    }
}
