package Game.Engine.World.Fields;

import Game.Engine.GameObjects;
import Game.Engine.GameMath.SpaceLogic.Logic2D.Vector2D;

/**
 * Campo físico del mundo — abstracción raíz de todos los campos.
 *
 * ── HRFC-015 — World Simulation Core ──────────────────────────────────────
 *
 * ── QUÉ ES UN WorldField ─────────────────────────────────────────────────
 * Un campo representa una propiedad del mundo que existe en un área del espacio
 * y que influye sobre los objetos dentro de su radio de acción.
 *
 * Los campos son la herramienta mediante la cual capacidades extraordinarias
 * modifican el mundo sin que el Engine conozca al autor:
 *
 *   Sans genera un GravityField   → El Engine solo ve un campo vectorial.
 *   Un piromante emite un ThermalField  → El Engine solo ve un campo escalar.
 *   Un rayo genera un ElectricField     → El Engine solo ve un campo escalar.
 *
 * El Engine no sabe quién creó el campo. Solo sabe que existe, dónde está,
 * qué radio tiene y qué intensidad produce en cada punto del espacio.
 *
 * ── JERARQUÍA ────────────────────────────────────────────────────────────
 *
 *   WorldField<T>    ← abstracción raíz (posición, radio, intensidad, falloff, vida)
 *       │
 *       ├── ScalarField   ← campo que produce un valor double en el destino
 *       │                   Ejemplos: temperatura, humedad, presión, carga
 *       │
 *       └── VectorField   ← campo que produce un vector (fx, fy) en el destino
 *                           Ejemplos: gravedad, viento, fuerza magnética, impulso
 *
 * Los campos concretos (ThermalField, GravityField, etc.) no son subclases —
 * son instancias configuradas de ScalarField o VectorField con distintos
 * parámetros. Los MaterialPresets equivalentes para campos viven en la Fase I.
 *
 * ── CICLO DE VIDA ────────────────────────────────────────────────────────
 * Un WorldField puede ser:
 *   - Permanente: lifetime = PERMANENT (−1). Existe hasta ser removido.
 *   - Temporal: se destruye cuando remaining llega a cero.
 *
 * WorldFieldSystem gestiona el ciclo de vida: lo crea, lo aplica cada frame
 * y lo elimina cuando expira.
 *
 * ── ORIGEN ────────────────────────────────────────────────────────────────
 * Un campo puede estar anclado a un objeto (origen dinámico) o a una posición
 * fija del mundo (origen estático). El origen dinámico se actualiza cada frame
 * desde la posición del objeto fuente.
 *
 * ── PARÁMETROS COMUNES ────────────────────────────────────────────────────
 *   position   → centro del campo en el espacio del mundo.
 *   radius     → radio de influencia (en unidades del mundo). Fuera = sin efecto.
 *   intensity  → magnitud base del campo (positivo = amplificación/calor/empuje).
 *   falloff    → función de atenuación con la distancia (ver FieldFalloff).
 *   lifetime   → duración en frames. PERMANENT (−1) = sin expiración.
 *   source     → objeto que generó el campo (puede ser null para campos ambientales).
 *
 * @param <T> tipo del valor que produce el campo al aplicarse sobre un objeto.
 *            ScalarField usa Double. VectorField usa double[2] (fx, fy).
 *            Este parámetro documenta la intención; el mecanismo de aplicación
 *            se implementa en cada subclase.
 */
public abstract class WorldField<T> {

    /** Valor especial de lifetime: el campo nunca expira. */
    public static final int PERMANENT = -1;

    // ── Estado del campo ──────────────────────────────────────────────────

    /** Centro del campo en el espacio del mundo. Mutable — sigue al source. */
    private final Vector2D position;

    /** Radio de influencia en unidades del mundo. */
    private final double radius;

    /**
     * Intensidad base del campo.
     * El signo tiene significado semántico dependiendo del tipo de campo:
     *   positivo → calor, sobrepresión, carga positiva, fuerza hacia afuera...
     *   negativo → frío, subpresión, carga negativa, fuerza hacia adentro...
     */
    private double intensity;

    /** Función de atenuación con la distancia. */
    private final FieldFalloff falloff;

    /**
     * Parámetro de corte para FieldFalloff.STEP.
     * Para otros tipos de falloff, este valor se ignora.
     * Rango [0, 1]: fracción del radio hasta donde el campo está activo.
     */
    private final double stepCutoff;

    /** Frames de vida restantes. −1 = permanente. */
    private int remaining;

    /**
     * Objeto que generó este campo. Puede ser null (campo ambiental sin autor).
     * WorldFieldSystem usa el source para:
     *   - Actualizar la posición del campo si el source se mueve.
     *   - Evitar que el campo afecte al propio source (si applyToSource = false).
     */
    private final GameObjects source;

    /**
     * Si false, el campo no se aplica sobre el objeto que lo generó.
     * Típicamente false para campos de personajes (Sans no se afecta a sí mismo).
     * true para campos ambientales (lava, viento, zonas).
     */
    private final boolean applyToSource;

    // ── Constructor ───────────────────────────────────────────────────────

    protected WorldField(Builder<?> b) {
        this.position      = new Vector2D(b.posX, b.posY);
        this.radius        = Math.max(0.0, b.radius);
        this.intensity     = b.intensity;
        this.falloff       = b.falloff;
        this.stepCutoff    = Math.max(0.0, Math.min(1.0, b.stepCutoff));
        this.remaining     = b.lifetime;
        this.source        = b.source;
        this.applyToSource = b.applyToSource;
    }

    // ── Ciclo de vida ─────────────────────────────────────────────────────

    /**
     * Actualiza el campo un frame.
     * Si el campo tiene source y debe seguirlo, actualiza la posición.
     * Decrementa el contador de vida restante.
     *
     * @return true si el campo sigue activo; false si ha expirado.
     */
    public boolean tick() {
        if (source != null) {
            Vector2D srcPos = source.getTransform().getPosition();
            position.setX(srcPos.getX());
            position.setY(srcPos.getY());
        }
        if (remaining == PERMANENT) return true;
        return --remaining > 0;
    }

    /** True si el campo está activo (no ha expirado). */
    public boolean isAlive() {
        return remaining == PERMANENT || remaining > 0;
    }

    // ── Geometría ─────────────────────────────────────────────────────────

    /**
     * Calcula la intensidad efectiva del campo sobre un objeto en la posición dada.
     * Si el objeto está fuera del radio, retorna 0.
     *
     * @param targetPos posición del objeto destino.
     * @return intensidad efectiva (base × factor de falloff) o 0 si fuera del radio.
     */
    public double effectiveIntensityAt(Vector2D targetPos) {
        if (radius <= 0) return intensity; // campo puntual: intensidad completa
        double dist = position.distance(targetPos);
        if (dist > radius) return 0.0;
        double normalized = dist / radius;
        double factor = falloff.compute(normalized, stepCutoff);
        return intensity * factor;
    }

    /**
     * True si la posición dada está dentro del radio de influencia del campo.
     *
     * @param targetPos posición a verificar.
     */
    public boolean isInRange(Vector2D targetPos) {
        if (radius <= 0) return true; // campo puntual: afecta a cualquiera en su posición
        return position.distance(targetPos) <= radius;
    }

    // ── Aplicación ────────────────────────────────────────────────────────

    /**
     * Aplica el efecto del campo sobre el objeto dado.
     * Implementado por subclases (ScalarField, VectorField).
     *
     * Pasos internos:
     *   1. Verificar que el objeto está en rango.
     *   2. Si source != null y !applyToSource, no afectar al source.
     *   3. Calcular la intensidad efectiva en la posición del objeto.
     *   4. Delegar en applyEffect() con la intensidad calculada.
     *
     * @param target objeto sobre el que se aplica el campo.
     */
    public final void applyTo(GameObjects target) {
        if (target == null) return;
        if (!applyToSource && target == source) return;

        Vector2D targetPos = target.getTransform().getPosition();
        if (!isInRange(targetPos)) return;

        double effective = effectiveIntensityAt(targetPos);
        if (effective == 0.0) return;

        applyEffect(target, effective);
    }

    /**
     * Aplica el efecto concreto del campo sobre el objeto.
     * Cada subclase implementa cómo la intensidad se traduce en un cambio
     * sobre los componentes del objeto.
     *
     * @param target    objeto destino (nunca null, siempre en rango).
     * @param intensity intensidad efectiva ya atenuada por el falloff.
     */
    protected abstract void applyEffect(GameObjects target, double intensity);

    // ── Accesores ─────────────────────────────────────────────────────────

    /** Centro del campo en coordenadas del mundo. */
    public Vector2D getPosition()     { return position; }

    /** Radio de influencia. */
    public double getRadius()         { return radius; }

    /** Intensidad base del campo. */
    public double getIntensity()      { return intensity; }

    /** Modifica la intensidad del campo en runtime (para campos que pulsan o decaen). */
    public void setIntensity(double v){ this.intensity = v; }

    /** Función de atenuación configurada. */
    public FieldFalloff getFalloff()  { return falloff; }

    /** Objeto fuente del campo, o null si es ambiental. */
    public GameObjects getSource()    { return source; }

    /** Frames de vida restantes. −1 = permanente. */
    public int getRemaining()         { return remaining; }

    /** True si el campo no se aplica sobre su propio source. */
    public boolean isApplyToSource()  { return applyToSource; }

    // ═════════════════════════════════════════════════════════════════════
    // Builder base — compartido por ScalarField.Builder y VectorField.Builder
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Builder base con todos los parámetros comunes de WorldField.
     * ScalarField.Builder y VectorField.Builder extienden este Builder.
     *
     * @param <B> tipo concreto del Builder (para retornar el tipo correcto en los setters).
     */
    @SuppressWarnings("unchecked")
    public abstract static class Builder<B extends Builder<B>> {

        double       posX          = 0.0;
        double       posY          = 0.0;
        double       radius        = 0.0;
        double       intensity     = 1.0;
        FieldFalloff falloff       = FieldFalloff.LINEAR;
        double       stepCutoff    = 1.0;
        int          lifetime      = PERMANENT;
        GameObjects  source        = null;
        boolean      applyToSource = false;

        /** Posición del centro del campo. */
        public B position(double x, double y) { this.posX = x; this.posY = y; return (B) this; }

        /** Radio de influencia. */
        public B radius(double r)             { this.radius = r; return (B) this; }

        /** Intensidad base del campo. */
        public B intensity(double i)          { this.intensity = i; return (B) this; }

        /** Función de atenuación. */
        public B falloff(FieldFalloff f)      { this.falloff = f; return (B) this; }

        /**
         * Parámetro de corte para FieldFalloff.STEP.
         * @param cutoff fracción del radio [0,1] hasta donde el campo está activo.
         */
        public B stepCutoff(double cutoff)    { this.stepCutoff = cutoff; return (B) this; }

        /** Duración en frames. Usar WorldField.PERMANENT (−1) para campos sin expiración. */
        public B lifetime(int frames)         { this.lifetime = frames; return (B) this; }

        /**
         * Objeto que genera el campo.
         * Si se establece, el campo sigue la posición del source y no lo afecta
         * a él mismo (salvo que se llame applyToSource(true) explícitamente).
         */
        public B source(GameObjects src)      { this.source = src; return (B) this; }

        /**
         * Si true, el campo también afecta al objeto que lo generó.
         * Por defecto false.
         */
        public B applyToSource(boolean v)     { this.applyToSource = v; return (B) this; }
    }
}
