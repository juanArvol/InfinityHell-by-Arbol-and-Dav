package Game.Engine.World.Solver;

import Game.Engine.World.Physics.CoreProperties;
import Game.Engine.World.Physics.PhysicsLaw;
import Game.Engine.World.Physics.WorldContext;

import java.util.List;

/**
 * Catálogo de las leyes físicas fundamentales del World Simulation Core.
 *
 * ── HRFC-019 — Eliminación Definitiva del Modelo Orientado a Tipos de Ley ─
 *
 * ── RESPONSABILIDAD ──────────────────────────────────────────────────────
 * CoreLaws es un catálogo externo al Engine Core.
 *
 * El PhysicsSolver no lo conoce. El LawRegistry no lo produce.
 * Es simplemente un conjunto de instancias PhysicsLaw listas para registrar.
 *
 * ── LEYES INCLUIDAS ───────────────────────────────────────────────────────
 *
 *   [1]  Expansión volumétrica        temperatura → presión (un objeto)
 *   [2]  Transferencia térmica        temperatura entre pares (dos objetos)
 *   [3]  Transferencia eléctrica      carga entre pares (dos objetos)
 *   [4]  Difusión fluídica            humedad entre pares (dos objetos)
 *   [5]  Disipación térmica ambiental temperatura → equilibrio (un objeto)
 *   [6]  Disipación eléctrica         carga → equilibrio (un objeto)
 *   [7]  Disipación fluídica          humedad → equilibrio (un objeto)
 *   [8]  Disipación de exceso térmico corrección cuando energía > umbral
 *   [9]  Disipación de exceso eléctrico corrección cuando acumulación > umbral
 *   [10] Liberación en saturación     corrección fluídica en saturación
 *
 * ── CÓMO AÑADIR UNA NUEVA LEY SIN MODIFICAR NADA DEL CORE ───────────────
 *
 *   // Fuera de CoreLaws, en cualquier catálogo del juego o mod:
 *
 *   PhysicsLaw gravityLaw = PhysicsLaw.builder()
 *       .inputs("velocity_y")
 *       .outputs("velocity_y")
 *       .solve(ctx -> {
 *           for (WorldContext.EntityView e : ctx.entities())
 *               if (e.has("velocity_y"))
 *                   e.add("velocity_y", 9.8 * ctx.deltaTime());
 *       })
 *       .priority(1)
 *       .build();
 *
 *   registry.register(gravityLaw);
 *
 *   PhysicsLaw magnetismLaw = PhysicsLaw.builder()
 *       .inputs("magnetic_field", "velocity_x", "velocity_y")
 *       .outputs("velocity_x", "velocity_y")
 *       .solve(ctx -> {
 *           List<WorldContext.EntityView> all = ctx.entities();
 *           for (int i = 0; i < all.size() - 1; i++)
 *               for (int j = i + 1; j < all.size(); j++) {
 *                   if (!all.get(i).has("magnetic_field")) continue;
 *                   if (!all.get(j).has("magnetic_field")) continue;
 *                   if (ctx.distance(all.get(i), all.get(j)) > 64.0) continue;
 *                   // fuerza entre dipolos magnéticos
 *               }
 *       })
 *       .build();
 *
 *   PhysicsLaw blackHoleLaw = PhysicsLaw.builder()
 *       .inputs("mass", "velocity_x", "velocity_y")
 *       .outputs("velocity_x", "velocity_y")
 *       .solve(ctx -> {
 *           List<WorldContext.EntityView> all = ctx.entities();
 *           for (WorldContext.EntityView a : all)
 *               for (WorldContext.EntityView b : all) {
 *                   if (a == b) continue;
 *                   double dist = ctx.distance(a, b);
 *                   if (dist < 1e-6) continue;
 *                   double rs = 2.0 * 6.674e-11 * b.get("mass") / (3e8 * 3e8);
 *                   if (dist <= rs) { /* absorción * / }
 *                   // aceleración gravitacional
 *               }
 *       })
 *       .build();
 *
 * En ninguno de estos casos se modifica PhysicsSolver, LawRegistry,
 * WorldSimulation, PhysicalState, ni CoreLaws.
 */
public final class CoreLaws {

    private CoreLaws() {}

    // ── Ley 1: Expansión volumétrica ──────────────────────────────────────

    /**
     * La temperatura genera presión interna proporcional a la incompresibilidad.
     * ΔP = temperatura × (1 − compresibilidad) × 0.05
     */
    public static final PhysicsLaw VOLUMETRIC_EXPANSION = PhysicsLaw.builder()
        .inputs(
            CoreProperties.TEMPERATURE.getId(),
            CoreProperties.COMPRESSIBILITY.getId())
        .outputs(CoreProperties.PRESSURE.getId())
        .priority(5)
        .solve(ctx -> {
            for (WorldContext.EntityView e : ctx.entities()) {
                if (!e.has(CoreProperties.TEMPERATURE.getId())) continue;
                if (!e.has(CoreProperties.PRESSURE.getId()))    continue;
                double temp = e.get(CoreProperties.TEMPERATURE.getId());
                if (Math.abs(temp) < 1e-6) continue;
                double coeff = (1.0 - e.get(CoreProperties.COMPRESSIBILITY.getId())) * 0.05;
                e.add(CoreProperties.PRESSURE.getId(), temp * coeff);
            }
        })
        .build();

    // ── Ley 2: Transferencia térmica entre pares ───────────────────────────

    /**
     * El calor fluye del objeto más caliente al más frío.
     * La velocidad depende de la menor conductividad de los dos.
     * La inercia depende de la capacidad calorífica de cada receptor.
     */
    public static final PhysicsLaw THERMAL_TRANSFER = PhysicsLaw.builder()
        .inputs(
            CoreProperties.TEMPERATURE.getId(),
            CoreProperties.THERMAL_CONDUCTIVITY.getId(),
            CoreProperties.HEAT_CAPACITY.getId())
        .outputs(CoreProperties.TEMPERATURE.getId())
        .priority(100)
        .solve(ctx -> {
            List<WorldContext.EntityView> all = ctx.entities();
            int n = all.size();
            for (int i = 0; i < n - 1; i++) {
                WorldContext.EntityView a = all.get(i);
                if (!a.has(CoreProperties.TEMPERATURE.getId())) continue;
                for (int j = i + 1; j < n; j++) {
                    WorldContext.EntityView b = all.get(j);
                    if (!b.has(CoreProperties.TEMPERATURE.getId())) continue;
                    if (ctx.distance(a, b) > 32.0) continue;
                    double tA   = a.get(CoreProperties.TEMPERATURE.getId());
                    double tB   = b.get(CoreProperties.TEMPERATURE.getId());
                    double diff = tA - tB;
                    if (Math.abs(diff) < 1e-6) continue;
                    double cA = a.get(CoreProperties.THERMAL_CONDUCTIVITY.getId());
                    double cB = b.get(CoreProperties.THERMAL_CONDUCTIVITY.getId());
                    double conductivity = Math.min(cA, cB);
                    if (conductivity <= 0) continue;
                    double delta  = diff * conductivity * 0.05;
                    double scaleA = 1.0 / Math.max(0.01, a.get(CoreProperties.HEAT_CAPACITY.getId()));
                    double scaleB = 1.0 / Math.max(0.01, b.get(CoreProperties.HEAT_CAPACITY.getId()));
                    a.add(CoreProperties.TEMPERATURE.getId(), -delta * scaleA);
                    b.add(CoreProperties.TEMPERATURE.getId(),  delta * scaleB);
                }
            }
        })
        .build();

    // ── Ley 3: Transferencia eléctrica entre pares ────────────────────────

    /**
     * La carga eléctrica fluye entre conductores en contacto.
     * Si alguno es aislante (conductividad = 0), no hay transferencia.
     */
    public static final PhysicsLaw ELECTRICAL_TRANSFER = PhysicsLaw.builder()
        .inputs(
            CoreProperties.CHARGE.getId(),
            CoreProperties.ELECTRICAL_CONDUCTIVITY.getId())
        .outputs(CoreProperties.CHARGE.getId())
        .priority(100)
        .solve(ctx -> {
            List<WorldContext.EntityView> all = ctx.entities();
            int n = all.size();
            for (int i = 0; i < n - 1; i++) {
                WorldContext.EntityView a = all.get(i);
                if (!a.has(CoreProperties.CHARGE.getId())) continue;
                for (int j = i + 1; j < n; j++) {
                    WorldContext.EntityView b = all.get(j);
                    if (!b.has(CoreProperties.CHARGE.getId())) continue;
                    if (ctx.distance(a, b) > 32.0) continue;
                    double cA = a.get(CoreProperties.ELECTRICAL_CONDUCTIVITY.getId());
                    double cB = b.get(CoreProperties.ELECTRICAL_CONDUCTIVITY.getId());
                    if (cA <= 0 || cB <= 0) continue;
                    double diff = a.get(CoreProperties.CHARGE.getId())
                                - b.get(CoreProperties.CHARGE.getId());
                    if (Math.abs(diff) < 1e-9) continue;
                    double delta = diff * (cA * cB) * 0.05;
                    a.add(CoreProperties.CHARGE.getId(), -delta);
                    b.add(CoreProperties.CHARGE.getId(),  delta);
                }
            }
        })
        .build();

    // ── Ley 4: Difusión fluídica entre pares ──────────────────────────────

    /**
     * La humedad se difunde del objeto más húmedo al más seco.
     * Si alguno es impermeable (absorción = 0), no hay difusión.
     */
    public static final PhysicsLaw FLUID_DIFFUSION = PhysicsLaw.builder()
        .inputs(
            CoreProperties.HUMIDITY.getId(),
            CoreProperties.HUMIDITY_ABSORPTION.getId())
        .outputs(CoreProperties.HUMIDITY.getId())
        .priority(100)
        .solve(ctx -> {
            List<WorldContext.EntityView> all = ctx.entities();
            int n = all.size();
            for (int i = 0; i < n - 1; i++) {
                WorldContext.EntityView a = all.get(i);
                if (!a.has(CoreProperties.HUMIDITY.getId())) continue;
                for (int j = i + 1; j < n; j++) {
                    WorldContext.EntityView b = all.get(j);
                    if (!b.has(CoreProperties.HUMIDITY.getId())) continue;
                    if (ctx.distance(a, b) > 32.0) continue;
                    double absA = a.get(CoreProperties.HUMIDITY_ABSORPTION.getId());
                    double absB = b.get(CoreProperties.HUMIDITY_ABSORPTION.getId());
                    if (absA <= 0 || absB <= 0) continue;
                    double diff = a.get(CoreProperties.HUMIDITY.getId())
                                - b.get(CoreProperties.HUMIDITY.getId());
                    if (Math.abs(diff) < 1e-6) continue;
                    double delta = diff * Math.min(absA, absB) * 0.05;
                    a.add(CoreProperties.HUMIDITY.getId(), -delta);
                    b.add(CoreProperties.HUMIDITY.getId(),  delta);
                }
            }
        })
        .build();

    // ── Ley 5: Disipación térmica ambiental ───────────────────────────────

    /**
     * La temperatura de un objeto converge hacia el equilibrio (0)
     * a una tasa determinada por su difusividad térmica.
     */
    public static final PhysicsLaw THERMAL_AMBIENT_DISSIPATION = PhysicsLaw.builder()
        .inputs(
            CoreProperties.TEMPERATURE.getId(),
            CoreProperties.THERMAL_DIFFUSIVITY.getId())
        .outputs(CoreProperties.TEMPERATURE.getId())
        .priority(50)
        .solve(ctx -> {
            for (WorldContext.EntityView e : ctx.entities()) {
                if (!e.has(CoreProperties.TEMPERATURE.getId())) continue;
                double temp = e.get(CoreProperties.TEMPERATURE.getId());
                if (Math.abs(temp) < 1e-6) continue;
                double rate = e.get(CoreProperties.THERMAL_DIFFUSIVITY.getId());
                if (rate <= 0) continue;
                e.add(CoreProperties.TEMPERATURE.getId(), -temp * rate * 0.05);
            }
        })
        .build();

    // ── Ley 6: Disipación eléctrica ambiental ─────────────────────────────

    /**
     * La carga eléctrica se disipa hacia el ambiente
     * a una tasa determinada por la conductividad eléctrica del material.
     */
    public static final PhysicsLaw ELECTRICAL_AMBIENT_DISSIPATION = PhysicsLaw.builder()
        .inputs(
            CoreProperties.CHARGE.getId(),
            CoreProperties.ELECTRICAL_CONDUCTIVITY.getId())
        .outputs(CoreProperties.CHARGE.getId())
        .priority(50)
        .solve(ctx -> {
            for (WorldContext.EntityView e : ctx.entities()) {
                if (!e.has(CoreProperties.CHARGE.getId())) continue;
                double charge = e.get(CoreProperties.CHARGE.getId());
                if (Math.abs(charge) < 1e-9) continue;
                double rate = e.get(CoreProperties.ELECTRICAL_CONDUCTIVITY.getId());
                if (rate <= 0) continue;
                e.add(CoreProperties.CHARGE.getId(), -charge * rate * 0.02);
            }
        })
        .build();

    // ── Ley 7: Disipación fluídica ambiental ──────────────────────────────

    /**
     * La humedad de un objeto se evapora lentamente
     * a una tasa determinada por el coeficiente de absorción del material.
     */
    public static final PhysicsLaw FLUID_AMBIENT_DISSIPATION = PhysicsLaw.builder()
        .inputs(
            CoreProperties.HUMIDITY.getId(),
            CoreProperties.HUMIDITY_ABSORPTION.getId())
        .outputs(CoreProperties.HUMIDITY.getId())
        .priority(50)
        .solve(ctx -> {
            for (WorldContext.EntityView e : ctx.entities()) {
                if (!e.has(CoreProperties.HUMIDITY.getId())) continue;
                double hum = e.get(CoreProperties.HUMIDITY.getId());
                if (hum < 1e-6) continue;
                double rate = e.get(CoreProperties.HUMIDITY_ABSORPTION.getId());
                if (rate <= 0) continue;
                e.add(CoreProperties.HUMIDITY.getId(), -hum * rate * 0.005);
            }
        })
        .build();

    // ── Ley 8: Disipación de exceso de energía térmica ────────────────────

    /**
     * Cuando la energía térmica acumulada (temperatura × capacidad) supera 500,
     * el objeto disipa el 10% de su temperatura por frame.
     */
    public static final PhysicsLaw THERMAL_EXCESS_DISSIPATION = PhysicsLaw.builder()
        .inputs(
            CoreProperties.TEMPERATURE.getId(),
            CoreProperties.HEAT_CAPACITY.getId())
        .outputs(CoreProperties.TEMPERATURE.getId())
        .priority(10)
        .solve(ctx -> {
            for (WorldContext.EntityView e : ctx.entities()) {
                if (!e.has(CoreProperties.TEMPERATURE.getId())) continue;
                double temp = e.get(CoreProperties.TEMPERATURE.getId());
                if (Math.abs(temp) < 1e-6) continue;
                double energy = Math.abs(temp) * e.get(CoreProperties.HEAT_CAPACITY.getId());
                if (energy < 500.0) continue;
                e.add(CoreProperties.TEMPERATURE.getId(), -temp * 0.1);
            }
        })
        .build();

    // ── Ley 9: Disipación de exceso de carga eléctrica ────────────────────

    /**
     * Cuando la carga acumulada (|carga| × conductividad) supera 10,
     * el objeto disipa el 8% de su carga por frame.
     */
    public static final PhysicsLaw ELECTRICAL_EXCESS_DISSIPATION = PhysicsLaw.builder()
        .inputs(
            CoreProperties.CHARGE.getId(),
            CoreProperties.ELECTRICAL_CONDUCTIVITY.getId())
        .outputs(CoreProperties.CHARGE.getId())
        .priority(20)
        .solve(ctx -> {
            for (WorldContext.EntityView e : ctx.entities()) {
                if (!e.has(CoreProperties.CHARGE.getId())) continue;
                double charge = e.get(CoreProperties.CHARGE.getId());
                if (Math.abs(charge) < 1e-9) continue;
                double accumulated = Math.abs(charge)
                    * e.get(CoreProperties.ELECTRICAL_CONDUCTIVITY.getId());
                if (accumulated < 10.0) continue;
                e.add(CoreProperties.CHARGE.getId(), -charge * 0.08);
            }
        })
        .build();

    // ── Ley 10: Liberación en saturación fluídica ─────────────────────────

    /**
     * Cuando la humedad relativa (humedad / absorción) supera 0.6,
     * el objeto libera el 5% de su humedad por frame.
     */
    public static final PhysicsLaw FLUID_SATURATION_RELEASE = PhysicsLaw.builder()
        .inputs(
            CoreProperties.HUMIDITY.getId(),
            CoreProperties.HUMIDITY_ABSORPTION.getId())
        .outputs(CoreProperties.HUMIDITY.getId())
        .priority(30)
        .solve(ctx -> {
            for (WorldContext.EntityView e : ctx.entities()) {
                if (!e.has(CoreProperties.HUMIDITY.getId())) continue;
                double hum = e.get(CoreProperties.HUMIDITY.getId());
                if (hum < 1e-6) continue;
                double absorption = Math.max(0.01,
                    e.get(CoreProperties.HUMIDITY_ABSORPTION.getId()));
                if ((hum / absorption) < 0.6) continue;
                e.add(CoreProperties.HUMIDITY.getId(), -hum * 0.05);
            }
        })
        .build();

    // ── Colección completa ────────────────────────────────────────────────

    /**
     * Todas las leyes físicas fundamentales.
     *
     *   LawRegistry registry = new LawRegistry().registerAll(CoreLaws.all());
     *
     * @return array con las 10 leyes fundamentales.
     */
    public static PhysicsLaw[] all() {
        return new PhysicsLaw[] {
            VOLUMETRIC_EXPANSION,
            THERMAL_TRANSFER,
            ELECTRICAL_TRANSFER,
            FLUID_DIFFUSION,
            THERMAL_AMBIENT_DISSIPATION,
            ELECTRICAL_AMBIENT_DISSIPATION,
            FLUID_AMBIENT_DISSIPATION,
            THERMAL_EXCESS_DISSIPATION,
            ELECTRICAL_EXCESS_DISSIPATION,
            FLUID_SATURATION_RELEASE
        };
    }
}
