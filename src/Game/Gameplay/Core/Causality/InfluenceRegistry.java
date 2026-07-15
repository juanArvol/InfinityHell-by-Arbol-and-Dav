package Game.Gameplay.Core.Causality;

import Game.Gameplay.Core.Modifiers.PropertyModifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Registro de influencias externas sobre modificadores con prioridad ordenada.
 *
 * ── POR QUÉ EXISTE (auditoría CFCC-002A) ────────────────────────────────
 * En CFCC-002, la ModifierInfluence de un PropertyModifier es declarada
 * por el propio modificador. Eso es correcto para influencias internas
 * ("cómo puedo ser transformado"), pero no cubre el caso de influencias
 * EXTERNAS:
 *
 *   "El hechizo A quiere amplificar todos los modificadores de fuego del jugador."
 *
 * Para que el hechizo aplique esa influencia tendría que modificar cada
 * PropertyModifier del contenedor del jugador directamente — acoplamiento directo,
 * violación del principio de desacoplamiento.
 *
 * InfluenceRegistry resuelve esto: es un registro donde cualquier sistema
 * puede añadir una influencia que se aplicará a TODOS los modificadores
 * que pasen por el PropertyResolver en el contexto de esa entidad.
 *
 * ── ANALOGÍA CON GameplayEventChannel ────────────────────────────────────
 * InfluenceRegistry es al pipeline de resolución lo que GameplayEventChannel
 * es al pipeline de eventos:
 *
 *   GameplayEventChannel   → intercepta eventos, puede modificarlos o cancelarlos
 *   InfluenceRegistry      → intercepta modificadores, puede transformarlos o cancelarlos
 *
 * Ambos usan prioridad ordenada y desacoplamiento completo.
 *
 * ── FLUJO DE RESOLUCIÓN ───────────────────────────────────────────────────
 * 1. PropertyResolver obtiene los modificadores activos del contenedor.
 * 2. Por cada modificador, aplica primero las influencias del InfluenceRegistry
 *    (externas, ordenadas por prioridad).
 * 3. Luego aplica la influencia propia del modificador (si la tiene).
 * 4. Luego evalúa el predicado.
 * 5. El resultado entra al pipeline ADDITIVE → MULTIPLICATIVE → OVERRIDE → CLAMP.
 *
 * ── PRIORIDAD ────────────────────────────────────────────────────────────
 * Menor valor = se ejecuta PRIMERO.
 *
 *   0–99    : influencias de amplificación máxima (habilidades definitivas)
 *   100–299 : influencias de amplificación normal (buffs, reliquias)
 *   300–499 : influencias de reducción (resistencias, escudos de modificador)
 *   500–699 : influencias de cancelación condicional (inmunidades)
 *   700+    : influencias de observación (logging, diagnóstico)
 *
 * Si una influencia retorna null (cancela el modificador), las siguientes
 * NO se ejecutan sobre ese modificador — el modificador queda excluido.
 *
 * ── INSTANCIABILIDAD ─────────────────────────────────────────────────────
 * InfluenceRegistry es instanciable. Cada entidad (o sistema) puede tener
 * su propio registro. No hay singleton global.
 *
 * Uso típico: un registro por entidad que puede recibir influencias externas,
 * almacenado como componente o como campo del sistema de combate.
 *
 * ── REGISTRO Y BAJA ──────────────────────────────────────────────────────
 * Las influencias se registran con un tag de identificación para poder
 * darlas de baja cuando el efecto termina:
 *
 *   registry.register(100, "fire_amplifier_spell", (mod, ctx) -> ...);
 *   // cuando el hechizo termina:
 *   registry.unregister("fire_amplifier_spell");
 *
 * ── THREAD SAFETY ────────────────────────────────────────────────────────
 * No es thread-safe. Usar desde el game loop thread.
 */
public final class InfluenceRegistry {

    /** Entrada interna: prioridad + tag + influencia. */
    private static final class Entry {
        final int               priority;
        final String            tag;
        final ModifierInfluence influence;

        Entry(int priority, String tag, ModifierInfluence influence) {
            this.priority  = priority;
            this.tag       = tag;
            this.influence = influence;
        }
    }

    private final List<Entry> entries = new ArrayList<>();
    private boolean sorted = true;

    // ── Registro ─────────────────────────────────────────────────────────

    /**
     * Registra una influencia externa con prioridad y tag de identificación.
     *
     * @param priority  orden de aplicación (menor = primero)
     * @param tag       identificador único para poder dar de baja la influencia
     * @param influence transformación a aplicar sobre modificadores
     * @throws IllegalArgumentException si tag o influence son null
     */
    public void register(int priority, String tag, ModifierInfluence influence) {
        if (tag == null || tag.isBlank())
            throw new IllegalArgumentException("tag no puede ser null o vacío.");
        if (influence == null)
            throw new IllegalArgumentException("influence no puede ser null.");
        entries.add(new Entry(priority, tag, influence));
        sorted = false;
    }

    /**
     * Registra una influencia con prioridad por defecto (300).
     *
     * @param tag       identificador único
     * @param influence transformación a aplicar
     */
    public void register(String tag, ModifierInfluence influence) {
        register(300, tag, influence);
    }

    /**
     * Da de baja todas las influencias con el tag indicado.
     * Si el tag no existe, la operación no tiene efecto.
     *
     * @param tag tag de la influencia a eliminar
     */
    public void unregister(String tag) {
        entries.removeIf(e -> e.tag.equals(tag));
    }

    /**
     * Elimina todas las influencias registradas.
     * Llamar al cambiar de escena o al destruir el sistema propietario.
     */
    public void clear() {
        entries.clear();
        sorted = true;
    }

    // ── Aplicación ───────────────────────────────────────────────────────

    /**
     * Aplica todas las influencias registradas sobre un modificador,
     * en orden de prioridad.
     *
     * Si alguna influencia retorna null, el modificador queda cancelado
     * y las siguientes influencias no se ejecutan.
     *
     * Si el registro está vacío, retorna el mismo modificador sin cambios.
     *
     * @param modifier modificador a transformar (nunca null)
     * @param context  contexto de resolución (nunca null)
     * @return el modificador transformado, o null si fue cancelado
     */
    public PropertyModifier apply(PropertyModifier modifier, ModifierContext context) {
        if (entries.isEmpty()) return modifier;
        ensureSorted();

        PropertyModifier current = modifier;
        for (Entry entry : entries) {
            current = entry.influence.apply(current, context);
            if (current == null) return null;   // cancelado — no continuar
        }
        return current;
    }

    /**
     * True si hay al menos una influencia registrada.
     */
    public boolean hasInfluences() {
        return !entries.isEmpty();
    }

    /**
     * Número de influencias registradas.
     */
    public int size() {
        return entries.size();
    }

    // ── Orden ─────────────────────────────────────────────────────────────

    private void ensureSorted() {
        if (!sorted) {
            entries.sort(Comparator.comparingInt(e -> e.priority));
            sorted = true;
        }
    }

    // ── Object ────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "InfluenceRegistry[" + entries.size() + " influences]";
    }
}
