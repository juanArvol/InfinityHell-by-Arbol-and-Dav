package Game.Engine.Entity.Properties.Modifier;

import Game.Engine.Entity.Properties.PropertyKey;
import java.util.*;

/**
 * Contenedor de PropertyModifier activos sobre una entidad.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * PropertyModifierContainer acumula todos los PropertyModifier que están
 * activos sobre una entidad en un momento dado. PropertyResolver los consulta
 * para calcular el valor final de cada propiedad.
 *
 * ── HRFC-FASE3.5 — Eliminación de String Identity ────────────────────────
 * Los modificadores se identifican por PropertyModifierSource (referencia),
 * NO por String. String solo se permite para debugName() (diagnóstico).
 *
 * NOTA DE NAMING: la clase se llama PropertyModifierContainer para evitar
 * conflicto de nombre con Game.Engine.Entity.Stats.Modifier.ModifierContainer,
 * que es el contenedor del sistema de stats RPG (StatModifier/StatTarget).
 *
 * ── IDENTIDAD DE CLAVE ───────────────────────────────────────────────────
 * Las búsquedas por propiedad usan comparación por referencia de instancia
 * de PropertyKey (==), no por displayName(). Solo se considera que un
 * modificador afecta a una propiedad si su key ES la misma instancia que
 * la key consultada.
 *
 * ── CICLO DE VIDA DE UN MODIFICADOR ──────────────────────────────────────
 *   // Al aplicar un buff de velocidad (usando PropertyModifierSource):
 *   container.add(PropertyModifier.multiplicative(PropertyKeys.SPEED, 1.5, this));
 *
 *   // Cuando el buff expira:
 *   container.removeBySource(this);  // this implementa PropertyModifierSource
 *
 * ── MIGRACIÓN DESDE String ───────────────────────────────────────────────
 *
 *   ANTES (String Identity - prohibido):
 *     container.removeBySource("poison");
 *
 *   AHORA (Identidad tipada - correcto):
 *     container.removeBySource(poisonEffect);  // poisonEffect es PropertyModifierSource
 *
 * ── THREAD SAFETY ────────────────────────────────────────────────────────
 * No es thread-safe. Usar desde el game loop thread.
 */
public final class PropertyModifierContainer {

    private final List<PropertyModifier> modifiers = new ArrayList<>();

    // ── Mutación ──────────────────────────────────────────────────────────

    public void add(PropertyModifier modifier) {
        if (modifier == null) throw new IllegalArgumentException("modifier no puede ser null.");
        modifiers.add(modifier);
    }

    /** 
     * Elimina todos los modificadores cuyo source coincida con el dado por identidad de referencia (==).
     * 
     * @param source Fuente del modificador (identidad tipada, NOT String)
     */
    public void removeBySource(PropertyModifierSource source) {
        if (source == null) return;
        modifiers.removeIf(m -> m.getSource() == source);
    }

    public void remove(PropertyModifier modifier) {
        modifiers.remove(modifier);
    }

    public void clear() {
        modifiers.clear();
    }

    // ── Consultas ─────────────────────────────────────────────────────────

    /**
     * Retorna todos los modificadores activos sobre una propiedad específica.
     *
     * La comparación usa == (identidad de referencia), garantizando que solo
     * coinciden modificadores registrados contra la misma instancia de PropertyKey.
     */
    public List<PropertyModifier> getFor(PropertyKey<?> key) {
        List<PropertyModifier> result = null;
        for (PropertyModifier m : modifiers) {
            if (m.getKey() == key) {
                if (result == null) result = new ArrayList<>();
                result.add(m);
            }
        }
        return result != null ? result : Collections.emptyList();
    }

    public List<PropertyModifier> getAll() {
        return Collections.unmodifiableList(modifiers);
    }

    /**
     * True si existe al menos un modificador activo sobre la propiedad indicada.
     * La comparación usa == (identidad de referencia).
     */
    public boolean hasModifiersFor(PropertyKey<?> key) {
        for (PropertyModifier m : modifiers) {
            if (m.getKey() == key) return true;
        }
        return false;
    }

    public boolean isEmpty()  { return modifiers.isEmpty(); }
    public int size()         { return modifiers.size(); }
}
