package Game.Enemys.Core.Variables;

import java.util.HashMap;
import java.util.Map;

/**
 * Contenedor de variables numéricas del enemigo.
 *
 * Enemy nunca define valores concretos hardcodeados.
 * Enemy únicamente declara que dichos conceptos existen.
 * Las implementaciones concretas (assemblers, fases) asignan los valores.
 *
 * ── Qué se almacena aquí ─────────────────────────────────────────────────
 *   "speed"          — velocidad base de movimiento.
 *   "damage"         — daño base de ataques.
 *   "hp"             — vida máxima (usada por assembler al construir).
 *   "detection_range"— rango de detección del jugador.
 *   "attack_range"   — rango de ataque.
 *   "defense"        — reducción de daño recibido (%).
 *   etc.
 *
 * ── Por qué un Map en lugar de campos ────────────────────────────────────
 * Los campos fijos no pueden ser modificados por fases o efectos sin
 * conocer la clase concreta. Un Map permite que cualquier fase, componente
 * o efecto lea/escriba variables sin acoplamiento.
 *
 * Los valores por defecto garantizan que siempre hay un valor seguro.
 *
 * ── Acceso tipado ────────────────────────────────────────────────────────
 *   variables.getDouble("speed")          — lectura con default 0.0
 *   variables.getDouble("speed", 2.0)     — lectura con default personalizado
 *   variables.set("speed", 3.0)           — escritura
 *   variables.modify("speed", v -> v * 2) — modificación funcional
 */
public final class EnemyVariables {

    private final Map<String, Double> values = new HashMap<>();

    // ── Lectura ───────────────────────────────────────────────────────────

    public double getDouble(String key) {
        return values.getOrDefault(key, 0.0);
    }

    public double getDouble(String key, double defaultValue) {
        return values.getOrDefault(key, defaultValue);
    }

    public int getInt(String key) {
        return (int) getDouble(key);
    }

    public int getInt(String key, int defaultValue) {
        return (int) getDouble(key, defaultValue);
    }

    public boolean getBoolean(String key) {
        return getDouble(key) != 0.0;
    }

    public boolean has(String key) {
        return values.containsKey(key);
    }

    // ── Escritura ─────────────────────────────────────────────────────────

    public EnemyVariables set(String key, double value) {
        values.put(key, value);
        return this;
    }

    public EnemyVariables set(String key, int value) {
        values.put(key, (double) value);
        return this;
    }

    public EnemyVariables set(String key, boolean value) {
        values.put(key, value ? 1.0 : 0.0);
        return this;
    }

    /**
     * Modifica el valor existente aplicando una función.
     * Si la clave no existe, la función recibe el valor por defecto 0.0.
     *
     * Ejemplo: variables.modify("speed", v -> v * 1.5)
     */
    public EnemyVariables modify(String key, java.util.function.DoubleUnaryOperator fn) {
        values.put(key, fn.applyAsDouble(getDouble(key)));
        return this;
    }

    /**
     * Establece el valor solo si la clave no existe todavía.
     * Útil para configurar defaults sin sobreescribir configuraciones previas.
     */
    public EnemyVariables setIfAbsent(String key, double value) {
        values.putIfAbsent(key, value);
        return this;
    }

    // ── Claves estándar ───────────────────────────────────────────────────

    /**
     * Constantes para las claves más comunes.
     * Usar estas constantes en lugar de strings literales para evitar typos.
     */
    public static final class Keys {
        public static final String SPEED           = "speed";
        public static final String DAMAGE          = "damage";
        public static final String HP              = "hp";
        public static final String DEFENSE         = "defense";
        public static final String DETECTION_RANGE = "detection_range";
        public static final String ATTACK_RANGE    = "attack_range";

        private Keys() {}
    }
}
