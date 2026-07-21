package Game.Engine.World.Influences;

import Game.Engine.GameObjects;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Factories y combinadores de Influence — utilidades de composición.
 *
 * ── HRFC-015 — World Simulation Core (iteración final) ────────────────────
 *
 * ── PROPÓSITO ─────────────────────────────────────────────────────────────
 * Influences provee los patrones de construcción y composición más frecuentes
 * para implementaciones de {@link Influence}, sin que el código de gameplay
 * repita boilerplate.
 *
 * ── PATRONES DISPONIBLES ──────────────────────────────────────────────────
 *
 *   of(consumer)                 — influencia sin duración (permanente)
 *   timed(frames, consumer)      — influencia con duración finita
 *   once(consumer)               — influencia que se aplica exactamente una vez
 *   compose(a, b, ...)           — compone N influencias como una sola
 *   conditional(predicate, base) — envuelve una influencia con una condición de activación
 *   decaying(initial, decay, consumer) — influencia cuya intensidad decae por frame
 *
 * ── INVARIANTE (mantenido en todas las factories) ─────────────────────────
 * Ninguna influencia creada aquí crea StatusEffects, dispara eventos de gameplay
 * ni invoca lógica de combate. El Consumer<GameObjects> inyectado es responsabilidad
 * del caller — el Engine solo gestiona el ciclo de vida.
 *
 * ── USO ───────────────────────────────────────────────────────────────────
 *
 *   // Influencia permanente que reduce temperatura:
 *   Influence cold = Influences.of(obj -> {
 *       ThermalComponent tc = obj.getComponent(ThermalComponent.class);
 *       if (tc != null) tc.addHeat(-5.0);
 *   });
 *
 *   // Influencia que dura 60 frames:
 *   Influence brief = Influences.timed(60, obj -> { ... });
 *
 *   // Dos influencias como una:
 *   Influence combo = Influences.compose(coldInfluence, chargeInfluence);
 *
 *   // Influencia que solo aplica si la temperatura ya es positiva:
 *   Influence conditional = Influences.conditional(
 *       target -> { ThermalComponent tc = target.getComponent(...); return tc != null && tc.isHot(); },
 *       heatAmplifier
 *   );
 *
 *   // Registrar en el sistema:
 *   sim.influences().add(cold, target);
 */
public final class Influences {

    private Influences() {}

    // ── Factories básicas ─────────────────────────────────────────────────

    /**
     * Crea una influencia permanente que aplica el efecto dado cada frame.
     * Nunca expira — el caller es responsable de removerla con
     * {@code influenceSystem.remove(binding)}.
     *
     * @param effect función de modificación sobre el objeto destino.
     * @return influencia permanente.
     */
    public static Influence of(Consumer<GameObjects> effect) {
        return target -> effect.accept(target);
    }

    /**
     * Crea una influencia con duración finita en frames.
     * Se auto-expira cuando el contador llega a cero.
     *
     * @param frames  duración en frames (> 0).
     * @param effect  función de modificación sobre el objeto destino.
     * @return influencia temporal.
     */
    public static Influence timed(int frames, Consumer<GameObjects> effect) {
        if (frames <= 0) throw new IllegalArgumentException("frames debe ser > 0");
        return new Influence() {
            private int remaining = frames;

            @Override
            public void apply(GameObjects target) { effect.accept(target); }

            @Override
            public boolean tick() {
                return --remaining > 0;
            }
        };
    }

    /**
     * Crea una influencia que se aplica exactamente una vez y luego expira.
     * Útil para modificaciones instantáneas registradas en el sistema
     * (p.ej. un impacto que aplica un delta de temperatura en el frame actual).
     *
     * @param effect función de modificación sobre el objeto destino.
     * @return influencia de un único frame.
     */
    public static Influence once(Consumer<GameObjects> effect) {
        return new Influence() {
            private boolean applied = false;

            @Override
            public void apply(GameObjects target) {
                effect.accept(target);
                applied = true;
            }

            @Override
            public boolean tick() {
                // El primer tick activa apply(); el segundo retorna false
                return !applied;
            }
        };
    }

    /**
     * Crea una influencia cuya intensidad decae por frame.
     * En cada frame el efecto se aplica con una intensidad multiplicada por el
     * factor de decaimiento respecto al frame anterior.
     *
     * La influencia expira cuando la intensidad cae por debajo de un epsilon.
     *
     * Útil para efectos de golpe que se disipan gradualmente (impulso de calor,
     * descarga eléctrica que se disipa, etc.).
     *
     * @param initialIntensity intensidad inicial.
     * @param decayFactor      factor multiplicativo por frame (0 < decay < 1).
     *                         Ejemplo: 0.95 = pierde 5% de intensidad por frame.
     * @param effect           función que recibe el objeto y la intensidad actual.
     * @return influencia con decaimiento exponencial.
     */
    public static Influence decaying(double initialIntensity, double decayFactor,
                                      java.util.function.BiConsumer<GameObjects, Double> effect) {
        if (decayFactor <= 0 || decayFactor >= 1.0)
            throw new IllegalArgumentException("decayFactor debe estar en (0, 1)");

        return new Influence() {
            private double intensity = initialIntensity;
            private static final double EPSILON = 1e-4;

            @Override
            public void apply(GameObjects target) {
                effect.accept(target, intensity);
                intensity *= decayFactor;
            }

            @Override
            public boolean tick() { return Math.abs(intensity) > EPSILON; }
        };
    }

    // ── Composición ───────────────────────────────────────────────────────

    /**
     * Compone dos o más influencias en una sola.
     * La influencia compuesta aplica todas en orden.
     * tick() retorna true mientras AL MENOS UNA siga activa.
     * onExpire() se propaga a todas.
     *
     * Útil para poderes que modifican múltiples dominios físicos
     * (p.ej. una tormenta que añade calor Y aumenta carga eléctrica Y humedece).
     *
     * @param influences influencias a componer. No puede ser vacía.
     * @return influencia compuesta.
     */
    public static Influence compose(Influence... influences) {
        if (influences == null || influences.length == 0)
            throw new IllegalArgumentException("compose requiere al menos una influencia");

        final List<Influence> parts = Arrays.asList(influences);

        return new Influence() {

            @Override
            public void apply(GameObjects target) {
                for (Influence part : parts) {
                    part.apply(target);
                }
            }

            @Override
            public boolean tick() {
                boolean anyActive = false;
                for (Influence part : parts) {
                    if (part.tick()) anyActive = true;
                }
                return anyActive;
            }

            @Override
            public void onExpire(GameObjects target) {
                for (Influence part : parts) {
                    part.onExpire(target);
                }
            }
        };
    }

    /**
     * Envuelve una influencia base con una condición de activación.
     * La influencia base solo se aplica si el predicado retorna true para
     * el objeto destino en ese frame.
     *
     * tick() y onExpire() siempre se propagan a la influencia base.
     *
     * Útil para influencias que solo afectan a objetos con ciertas propiedades
     * (p.ej. solo si ya están húmedos, solo si tienen ThermalComponent, etc.).
     *
     * @param condition predicado evaluado antes de apply(). Debe ser puro (sin efectos secundarios).
     * @param base      influencia base a aplicar cuando la condición es true.
     * @return influencia condicional.
     */
    public static Influence conditional(java.util.function.Predicate<GameObjects> condition,
                                         Influence base) {
        if (condition == null) throw new IllegalArgumentException("condition no puede ser null");
        if (base      == null) throw new IllegalArgumentException("base no puede ser null");

        return new Influence() {

            @Override
            public void apply(GameObjects target) {
                if (condition.test(target)) {
                    base.apply(target);
                }
            }

            @Override
            public boolean tick()                      { return base.tick(); }

            @Override
            public void onExpire(GameObjects target)   { base.onExpire(target); }
        };
    }
}
