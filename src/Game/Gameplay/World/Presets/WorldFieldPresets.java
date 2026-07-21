package Game.Gameplay.World.Presets;

import Game.Engine.Entity.Components.Physics2DComponent;
import Game.Engine.Entity.Components.ThermalComponent;
import Game.Engine.GameObjects;
import Game.Engine.World.Components.ElectricalComponent;
import Game.Engine.World.Components.FluidComponent;
import Game.Engine.World.Components.PressureComponent;
import Game.Engine.World.Fields.FieldFalloff;
import Game.Engine.World.Fields.ScalarField;
import Game.Engine.World.Fields.VectorField;
import Game.Engine.World.Fields.WorldField;

/**
 * Factories para WorldField concretos del universo de Infinity Hell.
 *
 * ── HRFC-015 — World Simulation Core ──────────────────────────────────────
 *
 * ── UBICACIÓN ─────────────────────────────────────────────────────────────
 * WorldFieldPresets vive en Game.Gameplay.World.Presets, no en el Engine.
 * Los campos concretos (thermal, gravity, rain, vacuum...) son contenido
 * del universo de Infinity Hell — no infraestructura del Engine.
 *
 * El Engine define las abstracciones (ScalarField, VectorField, FieldFalloff).
 * Este módulo de Gameplay define las instancias concretas que el juego usa.
 *
 * ── USO ───────────────────────────────────────────────────────────────────
 *
 *   // Un piromante genera un campo térmico que sigue su posición:
 *   sim.fields().add(WorldFieldPresets.thermal(x, y, 100, 20.0, pyromancer));
 *
 *   // Sans genera un campo gravitacional temporal (120 frames):
 *   sim.fields().add(WorldFieldPresets.gravity(x, y, 200, 15.0, sans, 120));
 *
 *   // Zona de lluvia permanente (sin source):
 *   sim.fields().add(WorldFieldPresets.humidity(x, y, 300, 0.05, null));
 *
 *   // Vacío de presión:
 *   sim.fields().add(WorldFieldPresets.pressure(x, y, 80, -60.0, null));
 */
public final class WorldFieldPresets {

    private WorldFieldPresets() {}

    // ── Campos escalares ──────────────────────────────────────────────────

    /**
     * Campo térmico permanente — añade/sustrae temperatura en radio.
     * Sigue la posición del source si no es null.
     *
     * @param intensity positivo = calor, negativo = frío (delta/frame en el centro).
     */
    public static ScalarField thermal(double x, double y, double radius,
                                       double intensity, GameObjects source) {
        return ScalarField.builder()
            .position(x, y).radius(radius).intensity(intensity)
            .falloff(FieldFalloff.LINEAR).source(source).applyToSource(false)
            .applicator((obj, delta) -> {
                ThermalComponent tc = obj.getComponent(ThermalComponent.class);
                if (tc != null) tc.addHeat(delta);
            })
            .build();
    }

    /**
     * Campo térmico con duración finita.
     * @param lifetime duración en frames. WorldField.PERMANENT (-1) = sin límite.
     */
    public static ScalarField thermal(double x, double y, double radius,
                                       double intensity, GameObjects source, int lifetime) {
        return ScalarField.builder()
            .position(x, y).radius(radius).intensity(intensity)
            .falloff(FieldFalloff.LINEAR).source(source).applyToSource(false)
            .lifetime(lifetime)
            .applicator((obj, delta) -> {
                ThermalComponent tc = obj.getComponent(ThermalComponent.class);
                if (tc != null) tc.addHeat(delta);
            })
            .build();
    }

    /**
     * Campo eléctrico — añade/sustrae carga eléctrica en radio.
     * Atenuación cuadrática: la carga decrece rápidamente con la distancia.
     *
     * @param intensity positivo = carga positiva, negativo = carga negativa (delta/frame).
     */
    public static ScalarField electric(double x, double y, double radius,
                                        double intensity, GameObjects source) {
        return ScalarField.builder()
            .position(x, y).radius(radius).intensity(intensity)
            .falloff(FieldFalloff.QUADRATIC).source(source).applyToSource(false)
            .applicator((obj, delta) -> {
                ElectricalComponent ec = obj.getComponent(ElectricalComponent.class);
                if (ec != null) ec.addCharge(delta);
            })
            .build();
    }

    /**
     * Campo de humedad — añade/sustrae humedad en radio.
     * Intensidad constante en todo el radio (lluvia uniforme).
     *
     * @param intensity positivo = humedece, negativo = seca (delta/frame).
     */
    public static ScalarField humidity(double x, double y, double radius,
                                        double intensity, GameObjects source) {
        return ScalarField.builder()
            .position(x, y).radius(radius).intensity(intensity)
            .falloff(FieldFalloff.CONSTANT).source(source).applyToSource(true)
            .applicator((obj, delta) -> {
                FluidComponent fc = obj.getComponent(FluidComponent.class);
                if (fc != null) fc.addHumidity(delta);
            })
            .build();
    }

    /**
     * Campo de presión — añade/sustrae presión local en radio.
     * Atenuación cuadrática: el efecto decrece con la distancia.
     *
     * @param intensity positivo = sobrepresión, negativo = subpresión/vacío (delta/frame).
     */
    public static ScalarField pressure(double x, double y, double radius,
                                        double intensity, GameObjects source) {
        return ScalarField.builder()
            .position(x, y).radius(radius).intensity(intensity)
            .falloff(FieldFalloff.QUADRATIC).source(source).applyToSource(false)
            .applicator((obj, delta) -> {
                PressureComponent pc = obj.getComponent(PressureComponent.class);
                if (pc != null) pc.addPressure(delta);
            })
            .build();
    }

    // ── Campos vectoriales ────────────────────────────────────────────────

    /**
     * Campo gravitacional personalizado — atrae objetos hacia el centro.
     * Usa ley del inverso del cuadrado (no llega a cero en los bordes).
     * Se integra con CollisionsSystem vía Physics2D.accumulate() en FASE 0.5.
     *
     * @param intensity magnitud de la fuerza de atracción por frame.
     */
    public static VectorField gravity(double x, double y, double radius,
                                       double intensity, GameObjects source) {
        return VectorField.builder()
            .position(x, y).radius(radius).intensity(intensity)
            .falloff(FieldFalloff.INVERSE_SQUARE).mode(VectorField.VectorFieldMode.RADIAL_IN)
            .source(source).applyToSource(false)
            .applicator((obj, fx, fy) -> {
                Physics2DComponent pc = obj.getComponent(Physics2DComponent.class);
                if (pc != null) pc.getPhysics().accumulate(fx, fy);
            })
            .build();
    }

    /** Campo gravitacional con duración finita. */
    public static VectorField gravity(double x, double y, double radius,
                                       double intensity, GameObjects source, int lifetime) {
        return VectorField.builder()
            .position(x, y).radius(radius).intensity(intensity)
            .falloff(FieldFalloff.INVERSE_SQUARE).mode(VectorField.VectorFieldMode.RADIAL_IN)
            .source(source).applyToSource(false).lifetime(lifetime)
            .applicator((obj, fx, fy) -> {
                Physics2DComponent pc = obj.getComponent(Physics2DComponent.class);
                if (pc != null) pc.getPhysics().accumulate(fx, fy);
            })
            .build();
    }

    /**
     * Campo de impulso radial — empuja objetos hacia afuera del centro.
     * Útil para explosiones, ondas de choque, campos repulsores.
     *
     * @param intensity magnitud del impulso por frame.
     */
    public static VectorField radialImpulse(double x, double y, double radius,
                                             double intensity, GameObjects source) {
        return VectorField.builder()
            .position(x, y).radius(radius).intensity(intensity)
            .falloff(FieldFalloff.LINEAR).mode(VectorField.VectorFieldMode.RADIAL_OUT)
            .source(source).applyToSource(false)
            .applicator((obj, fx, fy) -> {
                Physics2DComponent pc = obj.getComponent(Physics2DComponent.class);
                if (pc != null) pc.getPhysics().accumulate(fx, fy);
            })
            .build();
    }

    /**
     * Campo de viento — fuerza direccional constante en un área.
     * La dirección (fx, fy) no necesita estar normalizada.
     *
     * @param fx        componente X de la dirección del viento.
     * @param fy        componente Y de la dirección del viento.
     * @param intensity magnitud de la fuerza de viento por frame.
     */
    public static VectorField wind(double x, double y, double radius,
                                    double fx, double fy, double intensity,
                                    GameObjects source) {
        return VectorField.builder()
            .position(x, y).radius(radius).intensity(intensity)
            .falloff(FieldFalloff.CONSTANT).mode(VectorField.VectorFieldMode.FIXED)
            .direction(fx, fy).source(source).applyToSource(false)
            .applicator((obj, vx, vy) -> {
                Physics2DComponent pc = obj.getComponent(Physics2DComponent.class);
                if (pc != null) pc.getPhysics().accumulate(vx, vy);
            })
            .build();
    }
}
