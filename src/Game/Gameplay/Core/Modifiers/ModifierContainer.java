package Game.Gameplay.Core.Modifiers;

import Game.Gameplay.Core.Properties.PropertyKey;
import java.util.*;

/**
 * Contenedor de modificadores de propiedades activos sobre una entidad.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * ModifierContainer acumula todos los PropertyModifier que están activos
 * sobre una entidad en un momento dado. PropertyResolver los consulta para
 * calcular el valor final de cada propiedad.
 *
 * ── CICLO DE VIDA DE UN MODIFICADOR ──────────────────────────────────────
 * Los modificadores temporales (buffs, debuffs, efectos de estado) se añaden
 * con una sourceId única. Cuando el efecto termina, se eliminan por sourceId:
 *
 *   // Al aplicar un buff de velocidad (efecto temporal):
 *   container.add(PropertyModifier.multiplicative(PropertyKeys.SPEED, 1.5, "speed_buff"));
 *
 *   // Cuando el buff expira (en StatusEffectComponent.tick()):
 *   container.removeBySource("speed_buff");
 *
 * ── MODIFICADORES PERMANENTES ─────────────────────────────────────────────
 * Los modificadores de equipamiento permanente tienen sourceIds fijos:
 *
 *   container.add(PropertyModifier.additive(PropertyKeys.DAMAGE, 8.0, "bone_tip_amulet"));
 *
 * Al desequipar:
 *   container.removeBySource("bone_tip_amulet");
 *
 * ── MÚLTIPLES MODIFICADORES POR FUENTE ───────────────────────────────────
 * Una misma sourceId puede tener múltiples modificadores (sobre propiedades
 * distintas). removeBySource() elimina TODOS los de esa fuente:
 *
 *   container.add(PropertyModifier.additive(PropertyKeys.DAMAGE, 5.0, "relic_X"));
 *   container.add(PropertyModifier.multiplicative(PropertyKeys.SPEED, 0.8, "relic_X"));
 *   container.removeBySource("relic_X");  // elimina ambos
 *
 * ── THREAD SAFETY ────────────────────────────────────────────────────────
 * No es thread-safe. Usar desde el game loop thread.
 */
public final class ModifierContainer {

    private final List<PropertyModifier> modifiers = new ArrayList<>();

    // ── Mutación ──────────────────────────────────────────────────────────

    /**
     * Añade un modificador al contenedor.
     */
    public void add(PropertyModifier modifier) {
        if (modifier == null) throw new IllegalArgumentException("modifier no puede ser null.");
        modifiers.add(modifier);
    }

    /**
     * Elimina todos los modificadores cuyo sourceId coincida con el dado.
     *
     * @param sourceId identificador de la fuente a eliminar
     */
    public void removeBySource(String sourceId) {
        modifiers.removeIf(m -> m.getSourceId().equals(sourceId));
    }

    /**
     * Elimina un modificador específico por referencia de objeto.
     */
    public void remove(PropertyModifier modifier) {
        modifiers.remove(modifier);
    }

    /**
     * Elimina todos los modificadores.
     */
    public void clear() {
        modifiers.clear();
    }

    // ── Consultas ─────────────────────────────────────────────────────────

    /**
     * Retorna todos los modificadores activos sobre una propiedad específica.
     * Retorna una lista no modificable; no crea una copia completa (eficiente).
     *
     * @param key propiedad consultada
     * @return lista de modificadores que afectan esa propiedad (puede estar vacía)
     */
    public List<PropertyModifier> getFor(PropertyKey<?> key) {
        // Filtro inline — evita allocar lista si no hay modificadores de esa clave
        List<PropertyModifier> result = null;
        for (PropertyModifier m : modifiers) {
            if (m.getKey().id().equals(key.id())) {
                if (result == null) result = new ArrayList<>();
                result.add(m);
            }
        }
        return result != null ? result : Collections.emptyList();
    }

    /**
     * Vista no modificable de todos los modificadores en el contenedor.
     */
    public List<PropertyModifier> getAll() {
        return Collections.unmodifiableList(modifiers);
    }

    /**
     * True si hay al menos un modificador activo sobre la propiedad dada.
     */
    public boolean hasModifiersFor(PropertyKey<?> key) {
        for (PropertyModifier m : modifiers) {
            if (m.getKey().id().equals(key.id())) return true;
        }
        return false;
    }

    /**
     * True si el contenedor no tiene ningún modificador activo.
     */
    public boolean isEmpty() {
        return modifiers.isEmpty();
    }

    /**
     * Número total de modificadores activos.
     */
    public int size() {
        return modifiers.size();
    }
}
