package Game.Engine.Entity.Properties.Modifier.Causality;

import Game.Engine.Entity.Properties.Modifier.PropertyModifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Registro de influencias externas sobre modificadores con prioridad ordenada.
 *
 * ── POR QUÉ EXISTE ────────────────────────────────────────────────────────
 * En el pipeline de resolución, la ModifierInfluence de un PropertyModifier
 * es declarada por el propio modificador. Eso no cubre el caso de influencias
 * EXTERNAS:
 *
 *   "El hechizo A quiere amplificar todos los modificadores de fuego del jugador."
 *
 * InfluenceRegistry resuelve esto: es un registro donde cualquier sistema
 * puede añadir una influencia que se aplicará a TODOS los modificadores
 * que pasen por el PropertyResolver en el contexto de esa entidad.
 *
 * ── ANALOGÍA CON GameplayEventChannel ────────────────────────────────────
 *   GameplayEventChannel → intercepta eventos, puede modificarlos o cancelarlos
 *   InfluenceRegistry    → intercepta modificadores, puede transformarlos o cancelarlos
 *
 * ── PRIORIDAD ────────────────────────────────────────────────────────────
 * Menor valor = se ejecuta PRIMERO.
 * Si una influencia retorna null (cancela el modificador), las siguientes
 * NO se ejecutan sobre ese modificador.
 *
 * ── REGISTRO Y BAJA ──────────────────────────────────────────────────────
 *   registry.register(100, "fire_amplifier_spell", (mod, ctx) -> ...);
 *   // cuando el hechizo termina:
 *   registry.unregister("fire_amplifier_spell");
 *
 * ── THREAD SAFETY ────────────────────────────────────────────────────────
 * No es thread-safe. Usar desde el game loop thread.
 */
public final class InfluenceRegistry {

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
     */
    public void register(int priority, String tag, ModifierInfluence influence) {
        if (tag == null || tag.isBlank())
            throw new IllegalArgumentException("tag no puede ser null o vacío.");
        if (influence == null)
            throw new IllegalArgumentException("influence no puede ser null.");
        entries.add(new Entry(priority, tag, influence));
        sorted = false;
    }

    /** Registra una influencia con prioridad por defecto (300). */
    public void register(String tag, ModifierInfluence influence) {
        register(300, tag, influence);
    }

    /** Da de baja todas las influencias con el tag indicado. */
    public void unregister(String tag) {
        entries.removeIf(e -> e.tag.equals(tag));
    }

    /** Elimina todas las influencias registradas. */
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
     * @return el modificador transformado, o null si fue cancelado
     */
    public PropertyModifier apply(PropertyModifier modifier, ModifierContext context) {
        if (entries.isEmpty()) return modifier;
        ensureSorted();

        PropertyModifier current = modifier;
        for (Entry entry : entries) {
            current = entry.influence.apply(current, context);
            if (current == null) return null;
        }
        return current;
    }

    public boolean hasInfluences() { return !entries.isEmpty(); }
    public int size()               { return entries.size(); }

    private void ensureSorted() {
        if (!sorted) {
            entries.sort(Comparator.comparingInt(e -> e.priority));
            sorted = true;
        }
    }

    @Override
    public String toString() {
        return "InfluenceRegistry[" + entries.size() + " influences]";
    }
}
