package Game.Engine.World.Solver;

import Game.Engine.World.Physics.PhysicsLaw;
import Game.Engine.World.Physics.PropertyDescriptor;
import Game.Engine.World.Physics.WorldContext;

import java.util.List;

/**
 * Verificación de extensibilidad del World Simulation Core — HRFC-019.
 *
 * ── PROPÓSITO ─────────────────────────────────────────────────────────────
 * Este archivo demuestra que el Core cumple el invariante final de HRFC-019:
 *
 *   Cualquier fenómeno físico nuevo se implementa registrando nuevas
 *   propiedades y nuevas leyes. Nunca modificando el Solver, el LawRegistry,
 *   el WorldSimulation, el PhysicalState ni ningún otro componente del Core.
 *
 * Cada ley en este catálogo es completamente funcional. No es pseudocódigo.
 * Puede registrarse en cualquier LawRegistry y el PhysicsSolver la resolverá
 * exactamente igual que las leyes de CoreLaws, sin ningún cambio.
 *
 * ── PROPIEDADES NUEVAS ────────────────────────────────────────────────────
 * Los fenómenos nuevos solo requieren PropertyDescriptors nuevos.
 * No requieren nuevos tipos. No requieren nuevas clases base.
 * No requieren modificar CoreProperties.
 */
public final class ExtensibilityLaws {

    private ExtensibilityLaws() {}

    // ══════════════════════════════════════════════════════════════════════
    // NUEVAS PROPIEDADES — declaradas aquí para los fenómenos nuevos.
    // En un proyecto real vivirían en un catálogo propio (GameplayProperties,
    // PlanetProperties, MagicProperties, etc.)
    // ══════════════════════════════════════════════════════════════════════

    /** Componente Y de velocidad. Afectada por gravedad. */
    static final PropertyDescriptor VELOCITY_Y =
        PropertyDescriptor.of("velocity_y", 0.0, "Velocidad vertical en unidades/s");

    /** Componente X de velocidad. */
    static final PropertyDescriptor VELOCITY_X =
        PropertyDescriptor.of("velocity_x", 0.0, "Velocidad horizontal en unidades/s");

    /** Masa del objeto en unidades del juego. */
    static final PropertyDescriptor MASS =
        PropertyDescriptor.ofPositive("mass", 1.0, "Masa del objeto en kg relativos");

    /** Intensidad de campo magnético local. */
    static final PropertyDescriptor MAGNETIC_FIELD =
        PropertyDescriptor.of("magnetic_field", 0.0, "Intensidad de campo magnético");

    /** Nivel de radiación acumulada. */
    static final PropertyDescriptor RADIATION_LEVEL =
        PropertyDescriptor.ofPositive("radiation_level", 0.0,
            "Nivel de radiación ionizante acumulada");

    /** Coeficiente de absorción de radiación del material [0,1]. */
    static final PropertyDescriptor RADIATION_ABSORPTION =
        PropertyDescriptor.ofBounded("radiation_absorption", 0.1, 0.0, 1.0,
            "Fracción de radiación que el material absorbe por frame");

    /** Temperatura crítica de superconductividad. */
    static final PropertyDescriptor SUPERCONDUCTIVITY_THRESHOLD =
        PropertyDescriptor.of("superconductivity_threshold", Double.POSITIVE_INFINITY,
            "Temperatura por debajo de la cual el material es superconductor");

    /** Concentración de cristales precipitados [0,1]. */
    static final PropertyDescriptor CRYSTAL_CONCENTRATION =
        PropertyDescriptor.ofBounded("crystal_concentration", 0.0, 0.0, 1.0,
            "Fracción de masa cristalizada");

    /** Tasa de cristalización del material [0,1]. */
    static final PropertyDescriptor CRYSTALLIZATION_RATE =
        PropertyDescriptor.ofBounded("crystallization_rate", 0.0, 0.0, 1.0,
            "Velocidad de precipitación de cristales del material");

    /** Estado de plasma: 0 = sólido/líquido/gas normal, 1 = plasma total. */
    static final PropertyDescriptor PLASMA_STATE =
        PropertyDescriptor.ofBounded("plasma_state", 0.0, 0.0, 1.0,
            "Fracción de ionización de plasma [0=normal, 1=plasma puro]");

    /** Temperatura de transición a plasma del material. */
    static final PropertyDescriptor PLASMA_THRESHOLD =
        PropertyDescriptor.of("plasma_threshold", Double.POSITIVE_INFINITY,
            "Temperatura en la que el material alcanza estado de plasma");

    /** Tensión superficial del material líquido. */
    static final PropertyDescriptor SURFACE_TENSION =
        PropertyDescriptor.ofPositive("surface_tension", 0.0,
            "Tensión superficial del material en estado líquido");

    /** Radio de Schwarzschild efectivo (en unidades del mundo). */
    static final PropertyDescriptor SCHWARZSCHILD_RADIUS =
        PropertyDescriptor.ofPositive("schwarzschild_radius", 0.0,
            "Radio de Schwarzschild derivado de la masa del objeto");

    /** Espín cuántico del objeto. Valores típicos: 0, 0.5, 1. */
    static final PropertyDescriptor QUANTUM_SPIN =
        PropertyDescriptor.of("quantum_spin", 0.0,
            "Número cuántico de espín del objeto");

    /** Función de onda: amplitud de probabilidad cuántica. */
    static final PropertyDescriptor WAVE_FUNCTION =
        PropertyDescriptor.ofBounded("wave_function", 1.0, 0.0, 1.0,
            "Amplitud de la función de onda cuántica [0=colapsada, 1=superposición]");

    // ══════════════════════════════════════════════════════════════════════
    // GRAVEDAD
    // Fenómeno: aceleración vertical constante sobre objetos con velocity_y.
    // Archivos del Core modificados: NINGUNO.
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Gravedad uniforme hacia abajo.
     * Itera todas las entidades con velocity_y y les acumula 9.8 * deltaTime.
     */
    public static final PhysicsLaw GRAVITY = PhysicsLaw.builder()
        .inputs(VELOCITY_Y.getId())
        .outputs(VELOCITY_Y.getId())
        .priority(1)
        .solve(ctx -> {
            double g = 9.8 * ctx.deltaTime();
            for (WorldContext.EntityView e : ctx.entities()) {
                if (e.has(VELOCITY_Y.getId()))
                    e.add(VELOCITY_Y.getId(), g);
            }
        })
        .build();

    // ══════════════════════════════════════════════════════════════════════
    // MAGNETISMO
    // Fenómeno: fuerza entre dipolos magnéticos dentro de un radio de acción.
    // Archivos del Core modificados: NINGUNO.
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Fuerza magnética entre pares de objetos con campo magnético.
     * Cargas del mismo signo se repelen; signos opuestos se atraen.
     * La fuerza decae con el cuadrado de la distancia.
     */
    public static final PhysicsLaw MAGNETISM = PhysicsLaw.builder()
        .inputs(MAGNETIC_FIELD.getId(), VELOCITY_X.getId(), VELOCITY_Y.getId())
        .outputs(VELOCITY_X.getId(), VELOCITY_Y.getId())
        .priority(80)
        .solve(ctx -> {
            List<WorldContext.EntityView> all = ctx.entities();
            int n = all.size();
            for (int i = 0; i < n - 1; i++) {
                WorldContext.EntityView a = all.get(i);
                if (!a.has(MAGNETIC_FIELD.getId())) continue;
                for (int j = i + 1; j < n; j++) {
                    WorldContext.EntityView b = all.get(j);
                    if (!b.has(MAGNETIC_FIELD.getId())) continue;
                    double dist = ctx.distance(a, b);
                    if (dist < 1.0 || dist > 128.0) continue;
                    double fieldA = a.get(MAGNETIC_FIELD.getId());
                    double fieldB = b.get(MAGNETIC_FIELD.getId());
                    // fuerza proporcional al producto de campos, inversa al cuadrado
                    double force = (fieldA * fieldB) / (dist * dist) * ctx.deltaTime();
                    // signo negativo = atracción (campos opuestos), positivo = repulsión
                    double sign = Math.signum(fieldA * fieldB);
                    a.add(VELOCITY_X.getId(),  sign * force * 0.1);
                    b.add(VELOCITY_X.getId(), -sign * force * 0.1);
                }
            }
        })
        .build();

    // ══════════════════════════════════════════════════════════════════════
    // RADIACIÓN
    // Fenómeno: emisión y absorción de radiación ionizante entre objetos.
    // Archivos del Core modificados: NINGUNO.
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Transferencia de radiación entre pares dentro del radio de emisión.
     * La radiación se difunde del objeto más radioactivo al menos.
     * También convierte parte de la radiación absorbida en calor.
     */
    public static final PhysicsLaw RADIATION = PhysicsLaw.builder()
        .inputs(RADIATION_LEVEL.getId(), RADIATION_ABSORPTION.getId())
        .outputs(RADIATION_LEVEL.getId(), "temperature")
        .priority(110)
        .solve(ctx -> {
            List<WorldContext.EntityView> all = ctx.entities();
            int n = all.size();
            for (int i = 0; i < n - 1; i++) {
                WorldContext.EntityView a = all.get(i);
                if (!a.has(RADIATION_LEVEL.getId())) continue;
                for (int j = i + 1; j < n; j++) {
                    WorldContext.EntityView b = all.get(j);
                    if (!b.has(RADIATION_LEVEL.getId())) continue;
                    if (ctx.distance(a, b) > 96.0) continue;
                    double rA  = a.get(RADIATION_LEVEL.getId());
                    double rB  = b.get(RADIATION_LEVEL.getId());
                    double diff = rA - rB;
                    if (Math.abs(diff) < 1e-6) continue;
                    double absA = a.get(RADIATION_ABSORPTION.getId());
                    double absB = b.get(RADIATION_ABSORPTION.getId());
                    double transferred = diff * Math.min(absA, absB) * 0.02;
                    a.add(RADIATION_LEVEL.getId(), -transferred);
                    b.add(RADIATION_LEVEL.getId(),  transferred);
                    // la radiación absorbida genera calor
                    if (b.has("temperature"))
                        b.add("temperature", Math.abs(transferred) * 0.5);
                }
            }
        })
        .build();

    // ══════════════════════════════════════════════════════════════════════
    // SUPERCONDUCTIVIDAD
    // Fenómeno: resistencia eléctrica cero por debajo de temperatura crítica.
    // Archivos del Core modificados: NINGUNO.
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Superconductividad: cuando la temperatura de un objeto cae por debajo
     * de su umbral de superconductividad, su carga eléctrica no se disipa.
     * Esta ley cancela la disipación eléctrica acumulada aplicando el delta
     * inverso cuando la condición se cumple.
     *
     * En la práctica: la ley de disipación eléctrica sigue ejecutándose, pero
     * esta ley restaura la carga a su valor previo, resultando en carga neta
     * sin cambios — efecto de resistencia cero.
     */
    public static final PhysicsLaw SUPERCONDUCTIVITY = PhysicsLaw.builder()
        .inputs("temperature", "charge", SUPERCONDUCTIVITY_THRESHOLD.getId())
        .outputs("charge")
        .priority(60) // prioridad mayor que ELECTRICAL_AMBIENT_DISSIPATION (50)
        .solve(ctx -> {
            for (WorldContext.EntityView e : ctx.entities()) {
                if (!e.has("charge")) continue;
                if (!e.has("temperature")) continue;
                if (!e.has(SUPERCONDUCTIVITY_THRESHOLD.getId())) continue;
                double temp      = e.get("temperature");
                double threshold = e.get(SUPERCONDUCTIVITY_THRESHOLD.getId());
                if (temp >= threshold) continue;
                // por debajo del umbral: la carga no se disipa
                // cancelamos el delta de disipación restaurando la carga actual
                double charge = e.get("charge");
                double rate   = e.get("electrical_conductivity");
                // recalculamos el delta que la disipación aplicaría y lo revertimos
                double dissipated = -charge * rate * 0.02;
                e.add("charge", -dissipated); // cancela la disipación
            }
        })
        .build();

    // ══════════════════════════════════════════════════════════════════════
    // CRISTALIZACIÓN
    // Fenómeno: precipitación de sólidos cuando temperatura baja y humedad alta.
    // Archivos del Core modificados: NINGUNO.
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Cristalización: cuando la temperatura es negativa y hay humedad suficiente,
     * la humedad se convierte en masa cristalizada. La cristalización libera
     * calor latente (exotérmica).
     */
    public static final PhysicsLaw CRYSTALLIZATION = PhysicsLaw.builder()
        .inputs("temperature", "humidity", CRYSTALLIZATION_RATE.getId())
        .outputs("humidity", CRYSTAL_CONCENTRATION.getId(), "temperature")
        .priority(40)
        .solve(ctx -> {
            for (WorldContext.EntityView e : ctx.entities()) {
                if (!e.has("temperature"))  continue;
                if (!e.has("humidity"))     continue;
                if (!e.has(CRYSTAL_CONCENTRATION.getId())) continue;
                double temp     = e.get("temperature");
                double humidity = e.get("humidity");
                if (temp >= 0 || humidity < 0.1) continue;
                double rate   = Math.max(0.01, e.get(CRYSTALLIZATION_RATE.getId()));
                // intensidad proporcional a qué tan fría está la temperatura
                double intensity = Math.min(1.0, Math.abs(temp) / 100.0) * rate * 0.1;
                double converted = humidity * intensity;
                e.add("humidity", -converted);
                e.add(CRYSTAL_CONCENTRATION.getId(), converted);
                // calor latente de cristalización (exotérmica)
                e.add("temperature", converted * 50.0);
            }
        })
        .build();

    // ══════════════════════════════════════════════════════════════════════
    // PLASMA
    // Fenómeno: ionización del material a temperaturas extremas.
    // Archivos del Core modificados: NINGUNO.
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Transición a plasma: cuando la temperatura supera el umbral del material,
     * el estado de plasma aumenta progresivamente. El plasma amplifica la
     * conductividad eléctrica y emite radiación.
     */
    public static final PhysicsLaw PLASMA_TRANSITION = PhysicsLaw.builder()
        .inputs("temperature", PLASMA_THRESHOLD.getId())
        .outputs(PLASMA_STATE.getId(), "electrical_conductivity", RADIATION_LEVEL.getId())
        .priority(35)
        .solve(ctx -> {
            for (WorldContext.EntityView e : ctx.entities()) {
                if (!e.has("temperature")) continue;
                if (!e.has(PLASMA_STATE.getId())) continue;
                if (!e.has(PLASMA_THRESHOLD.getId())) continue;
                double temp      = e.get("temperature");
                double threshold = e.get(PLASMA_THRESHOLD.getId());
                if (temp < threshold) {
                    // enfriamiento: salir del estado de plasma
                    double plasma = e.get(PLASMA_STATE.getId());
                    if (plasma > 0) e.add(PLASMA_STATE.getId(), -plasma * 0.05);
                    continue;
                }
                // calentamiento: aumentar ionización
                double excess     = (temp - threshold) / threshold;
                double ionization = Math.min(excess * 0.1, 0.05);
                e.add(PLASMA_STATE.getId(), ionization);
                // el plasma es altamente conductor y emite radiación
                double plasmaLevel = e.get(PLASMA_STATE.getId());
                if (e.has("electrical_conductivity"))
                    e.add("electrical_conductivity", plasmaLevel * 0.01);
                if (e.has(RADIATION_LEVEL.getId()))
                    e.add(RADIATION_LEVEL.getId(), plasmaLevel * temp * 0.0001);
            }
        })
        .build();

    // ══════════════════════════════════════════════════════════════════════
    // TENSIÓN SUPERFICIAL
    // Fenómeno: cohesión entre objetos líquidos adyacentes.
    // Archivos del Core modificados: NINGUNO.
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Tensión superficial: los objetos líquidos (viscosidad > 0) se atraen
     * mutuamente cuando están muy próximos, simulando cohesión superficial.
     * El efecto solo actúa a distancias muy cortas (≤ 8 unidades).
     */
    public static final PhysicsLaw SURFACE_TENSION_LAW = PhysicsLaw.builder()
        .inputs(SURFACE_TENSION.getId(), "viscosity", VELOCITY_X.getId(), VELOCITY_Y.getId())
        .outputs(VELOCITY_X.getId(), VELOCITY_Y.getId())
        .priority(90)
        .solve(ctx -> {
            List<WorldContext.EntityView> all = ctx.entities();
            int n = all.size();
            for (int i = 0; i < n - 1; i++) {
                WorldContext.EntityView a = all.get(i);
                if (!a.has(SURFACE_TENSION.getId())) continue;
                if (a.get("viscosity") <= 0) continue;
                for (int j = i + 1; j < n; j++) {
                    WorldContext.EntityView b = all.get(j);
                    if (!b.has(SURFACE_TENSION.getId())) continue;
                    if (b.get("viscosity") <= 0) continue;
                    double dist = ctx.distance(a, b);
                    if (dist < 0.1 || dist > 8.0) continue;
                    double tension  = Math.min(
                        a.get(SURFACE_TENSION.getId()),
                        b.get(SURFACE_TENSION.getId()));
                    double pull     = tension / (dist * dist) * ctx.deltaTime();
                    // fuerza de cohesión: cada objeto se "acerca" al otro
                    a.add(VELOCITY_X.getId(),  pull * 0.01);
                    b.add(VELOCITY_X.getId(), -pull * 0.01);
                }
            }
        })
        .build();

    // ══════════════════════════════════════════════════════════════════════
    // AGUJERO NEGRO — RADIO DE SCHWARZSCHILD
    // Fenómeno: atracción gravitacional extrema y horizonte de eventos.
    // Archivos del Core modificados: NINGUNO.
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Atracción gravitacional de cuerpos masivos con radio de Schwarzschild.
     *
     * Para cada par de objetos con masa:
     *   - Aplica atracción gravitacional newtoniana escalada.
     *   - Si la distancia cae por debajo del radio de Schwarzschild del objeto
     *     más masivo, el objeto menos masivo pierde toda su velocidad
     *     (absorción por el horizonte de eventos).
     *
     * El radio de Schwarzschild se calcula como propiedad del estado del objeto
     * (registrada como schwarzschild_radius). La ley lo lee directamente.
     */
    public static final PhysicsLaw BLACK_HOLE_GRAVITY = PhysicsLaw.builder()
        .inputs(MASS.getId(), SCHWARZSCHILD_RADIUS.getId(),
                VELOCITY_X.getId(), VELOCITY_Y.getId())
        .outputs(VELOCITY_X.getId(), VELOCITY_Y.getId())
        .priority(2)
        .solve(ctx -> {
            List<WorldContext.EntityView> all = ctx.entities();
            int n = all.size();
            for (int i = 0; i < n - 1; i++) {
                WorldContext.EntityView a = all.get(i);
                if (!a.has(MASS.getId())) continue;
                for (int j = i + 1; j < n; j++) {
                    WorldContext.EntityView b = all.get(j);
                    if (!b.has(MASS.getId())) continue;
                    double dist = ctx.distance(a, b);
                    if (dist < 0.1) continue;
                    double mA = a.get(MASS.getId());
                    double mB = b.get(MASS.getId());
                    double rsA = a.get(SCHWARZSCHILD_RADIUS.getId());
                    double rsB = b.get(SCHWARZSCHILD_RADIUS.getId());
                    // horizonte de eventos: absorción
                    if (dist <= rsA && mA > mB) {
                        if (b.has(VELOCITY_X.getId())) b.add(VELOCITY_X.getId(), -b.get(VELOCITY_X.getId()));
                        if (b.has(VELOCITY_Y.getId())) b.add(VELOCITY_Y.getId(), -b.get(VELOCITY_Y.getId()));
                        continue;
                    }
                    if (dist <= rsB && mB > mA) {
                        if (a.has(VELOCITY_X.getId())) a.add(VELOCITY_X.getId(), -a.get(VELOCITY_X.getId()));
                        if (a.has(VELOCITY_Y.getId())) a.add(VELOCITY_Y.getId(), -a.get(VELOCITY_Y.getId()));
                        continue;
                    }
                    // atracción gravitacional newtoniana (G escalada al juego)
                    double G      = 6.674e-4;
                    double force  = G * mA * mB / (dist * dist) * ctx.deltaTime();
                    double accelA = force / Math.max(0.01, mA);
                    double accelB = force / Math.max(0.01, mB);
                    // dirección: A atrae a B y viceversa (simplificado, sin vector unitario)
                    if (a.has(VELOCITY_X.getId())) a.add(VELOCITY_X.getId(),  accelA * 0.01);
                    if (b.has(VELOCITY_X.getId())) b.add(VELOCITY_X.getId(), -accelB * 0.01);
                }
            }
        })
        .build();

    // ══════════════════════════════════════════════════════════════════════
    // EFECTOS CUÁNTICOS — COLAPSO DE FUNCIÓN DE ONDA
    // Fenómeno: superposición cuántica y colapso por proximidad.
    // Archivos del Core modificados: NINGUNO.
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Colapso de función de onda: cuando dos objetos con función de onda
     * activa se acercan lo suficiente, la función de onda de ambos colapsa
     * progresivamente hacia 0 (estado determinista).
     *
     * Un objeto en superposición (wave_function ≈ 1) tiene posición indefinida.
     * La interpretación de ese estado es responsabilidad del Gameplay, no del Core.
     * El Core solo modela el valor numérico.
     */
    public static final PhysicsLaw QUANTUM_WAVE_COLLAPSE = PhysicsLaw.builder()
        .inputs(WAVE_FUNCTION.getId(), QUANTUM_SPIN.getId())
        .outputs(WAVE_FUNCTION.getId())
        .priority(200)
        .solve(ctx -> {
            List<WorldContext.EntityView> all = ctx.entities();
            int n = all.size();
            for (int i = 0; i < n - 1; i++) {
                WorldContext.EntityView a = all.get(i);
                if (!a.has(WAVE_FUNCTION.getId())) continue;
                if (a.get(WAVE_FUNCTION.getId()) < 0.01) continue;
                for (int j = i + 1; j < n; j++) {
                    WorldContext.EntityView b = all.get(j);
                    if (!b.has(WAVE_FUNCTION.getId())) continue;
                    if (ctx.distance(a, b) > 16.0) continue;
                    // espines opuestos causan colapso mutuo más rápido
                    double spinA   = a.get(QUANTUM_SPIN.getId());
                    double spinB   = b.get(QUANTUM_SPIN.getId());
                    double factor  = (spinA * spinB < 0) ? 0.2 : 0.05;
                    double wfA     = a.get(WAVE_FUNCTION.getId());
                    double wfB     = b.get(WAVE_FUNCTION.getId());
                    a.add(WAVE_FUNCTION.getId(), -wfA * factor * ctx.deltaTime());
                    b.add(WAVE_FUNCTION.getId(), -wfB * factor * ctx.deltaTime());
                }
            }
        })
        .build();

    // ══════════════════════════════════════════════════════════════════════
    // COLECCIÓN COMPLETA
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Todas las leyes de extensibilidad.
     *
     * Ejemplo de uso — añadir gravedad y magnetismo a un mundo existente
     * sin modificar ningún archivo del Core:
     *
     *   world.solver().addLaw(ExtensibilityLaws.GRAVITY);
     *   world.solver().addLaw(ExtensibilityLaws.MAGNETISM);
     *   world.solver().addLaw(ExtensibilityLaws.RADIATION);
     *
     * O registrar todos como catálogo adicional:
     *
     *   LawRegistry extended = new LawRegistry().registerAll(ExtensibilityLaws.all());
     *   WorldSimulation sim = WorldSimulation.builder()
     *       .registerAll(new LawRegistry().registerAll(CoreLaws.all()))
     *       .registerAll(extended)
     *       .build();
     *
     * @return array con todas las leyes de extensibilidad.
     */
    public static PhysicsLaw[] all() {
        return new PhysicsLaw[] {
            GRAVITY,
            MAGNETISM,
            RADIATION,
            SUPERCONDUCTIVITY,
            CRYSTALLIZATION,
            PLASMA_TRANSITION,
            SURFACE_TENSION_LAW,
            BLACK_HOLE_GRAVITY,
            QUANTUM_WAVE_COLLAPSE
        };
    }
}
