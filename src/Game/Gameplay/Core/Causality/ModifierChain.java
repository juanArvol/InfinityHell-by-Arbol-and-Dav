package Game.Gameplay.Core.Causality;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Cadena causal de modificadores — representa la propagación de una modificación
 * a través de múltiples entidades y sistemas.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * ModifierChain responde a las preguntas de causalidad:
 *
 *   "¿Qué modificador originó éste?"
 *   "¿Quién modificó al modificador?"
 *   "¿Qué modificaciones nacieron de otra?"
 *   "¿Cuántos niveles de propagación existen?"
 *   "¿Quién fue el padre?"
 *   "¿Quiénes son los hijos en la cadena?"
 *
 * ── MODELO: ÁRBOL CAUSAL ─────────────────────────────────────────────────
 * ModifierChain NO es una lista — es un árbol causal inmutable.
 * Cada nodo conoce su padre y su profundidad.
 * Un nodo puede tener múltiples hijos (una modificación puede propagarse
 * a múltiples destinos simultáneamente).
 *
 * Ejemplo de cadena conceptual:
 *
 *   Spell                             (depth 0 — raíz)
 *     └── Projectile                  (depth 1)
 *           └── Explosion             (depth 2)
 *                 ├── Fire Area A     (depth 3)
 *                 │     └── Enemy A Burning  (depth 4)
 *                 │           └── Damage Over Time  (depth 5)
 *                 └── Fire Area B     (depth 3)
 *                       └── Enemy B Burning  (depth 4)
 *
 * Esta cadena puede reconstruirse completamente desde cualquier nodo:
 *   - Subiendo via getParent() hasta llegar a la raíz.
 *   - Bajando via getChildren() desde cualquier nodo.
 *
 * ── DISEÑO: INMUTABILIDAD ESTRUCTURAL ────────────────────────────────────
 * Los vínculos padre→hijo son INMUTABLES una vez establecidos.
 * Sin embargo, los hijos se añaden en construcción — un nodo padre puede
 * adquirir hijos después de crearse (cuando la propagación ocurre).
 *
 * Para preservar la invariante de que una cadena es "finalmente inmutable",
 * usar {@link #seal()} para cerrar el nodo y prevenir más hijos.
 *
 * ── FACTORY: RAÍZ Y DERIVACIÓN ───────────────────────────────────────────
 *
 *   // Crear la raíz de una nueva cadena (el hechizo original):
 *   ModifierChain spellChain = ModifierChain.root("spell_fireball");
 *
 *   // Derivar un hijo (el proyectil que el hechizo crea):
 *   ModifierChain projectileChain = spellChain.derive("projectile_001");
 *
 *   // Derivar nietos (la explosión del proyectil):
 *   ModifierChain explosionChain = projectileChain.derive("explosion_001");
 *
 *   // Reconstruir la cadena desde el nodo más profundo:
 *   List<String> path = explosionChain.ancestry();
 *   // → ["spell_fireball", "projectile_001", "explosion_001"]
 *
 * ── PROFUNDIDAD MÁXIMA ────────────────────────────────────────────────────
 * Para prevenir ciclos accidentales o cadenas infinitas, ModifierChain
 * impone un límite de profundidad configurable. Por defecto 32 niveles,
 * suficiente para cualquier cadena causal realista en Infinity Hell.
 *
 * ── IDENTIFICADORES ───────────────────────────────────────────────────────
 * Cada nodo tiene un ID de cadena (chainId) que describe conceptualmente
 * qué produjo ese modificador. No tiene que ser globalmente único — es
 * descriptivo para debug y logging. Puede ser el sourceId del modificador
 * que lo originó, o cualquier string significativo.
 *
 * ── COMPATIBILIDAD CON CFCC-001 ───────────────────────────────────────────
 * Los PropertyModifier sin ModifierChain asignada funcionan exactamente
 * igual que antes. La cadena es completamente opcional.
 */
public final class ModifierChain {

    /** Límite de profundidad máxima para prevenir cadenas desbocadas. */
    public static final int MAX_DEPTH = 32;

    private final String       chainId;
    private final ModifierChain parent;       // null si es raíz
    private final int          depth;
    private final List<ModifierChain> children = new ArrayList<>();
    private boolean sealed = false;

    // ── Constructor privado ───────────────────────────────────────────────

    private ModifierChain(String chainId, ModifierChain parent) {
        if (chainId == null || chainId.isBlank()) {
            throw new IllegalArgumentException("chainId no puede ser null o vacío.");
        }
        this.chainId = chainId;
        this.parent  = parent;
        this.depth   = (parent == null) ? 0 : parent.depth + 1;

        if (this.depth > MAX_DEPTH) {
            throw new IllegalStateException(
                "ModifierChain excede la profundidad máxima (" + MAX_DEPTH
                + "). Posible ciclo en la cadena causal. chainId='" + chainId + "'"
            );
        }
    }

    // ── Factory methods ───────────────────────────────────────────────────

    /**
     * Crea un nodo raíz — el inicio de una nueva cadena causal.
     *
     * @param chainId identificador descriptivo del origen (ej: "spell_fireball", "weapon_sword")
     * @return nodo raíz de la cadena
     */
    public static ModifierChain root(String chainId) {
        return new ModifierChain(chainId, null);
    }

    /**
     * Crea un nodo hijo de esta cadena — representa la propagación de la
     * modificación a un nuevo nivel.
     *
     * @param chainId identificador del nuevo nodo en la cadena
     * @return nodo hijo con esta cadena como padre
     * @throws IllegalStateException si esta cadena está sellada o si se supera MAX_DEPTH
     */
    public ModifierChain derive(String chainId) {
        if (sealed) {
            throw new IllegalStateException(
                "No se puede derivar de una cadena sellada. chainId='" + this.chainId + "'"
            );
        }
        ModifierChain child = new ModifierChain(chainId, this);
        children.add(child);
        return child;
    }

    // ── Consultas ─────────────────────────────────────────────────────────

    /**
     * Identificador descriptivo de este nodo en la cadena.
     */
    public String getChainId() {
        return chainId;
    }

    /**
     * Profundidad de este nodo. 0 = raíz.
     */
    public int getDepth() {
        return depth;
    }

    /**
     * True si este nodo es la raíz de la cadena (no tiene padre).
     */
    public boolean isRoot() {
        return parent == null;
    }

    /**
     * El nodo padre en la cadena causal, o null si es raíz.
     */
    public Optional<ModifierChain> getParent() {
        return Optional.ofNullable(parent);
    }

    /**
     * Lista no modificable de los nodos hijos directos de esta cadena.
     */
    public List<ModifierChain> getChildren() {
        return Collections.unmodifiableList(children);
    }

    /**
     * True si este nodo tiene al menos un hijo.
     */
    public boolean hasChildren() {
        return !children.isEmpty();
    }

    /**
     * True si esta cadena está sellada (no acepta más hijos).
     */
    public boolean isSealed() {
        return sealed;
    }

    /**
     * Sella este nodo, impidiendo la creación de más hijos.
     * Retorna this para permitir encadenamiento: {@code chain.derive("x").seal()}.
     */
    public ModifierChain seal() {
        this.sealed = true;
        return this;
    }

    // ── Reconstrucción de la cadena ───────────────────────────────────────

    /**
     * Retorna la raíz de toda la cadena causal subiendo por los padres.
     *
     * @return nodo raíz (depth == 0)
     */
    public ModifierChain getRoot() {
        ModifierChain current = this;
        while (current.parent != null) {
            current = current.parent;
        }
        return current;
    }

    /**
     * Retorna la lista de chainIds desde la raíz hasta este nodo (inclusive),
     * en orden causal de origen a destino.
     *
     *   explosionChain.ancestry()
     *   → ["spell_fireball", "projectile_001", "explosion_001"]
     *
     * @return lista ordenada de IDs de la cadena ancestral
     */
    public List<String> ancestry() {
        List<String> path = new ArrayList<>(depth + 1);
        collectAncestry(this, path);
        return Collections.unmodifiableList(path);
    }

    private static void collectAncestry(ModifierChain node, List<String> accumulator) {
        if (node.parent != null) {
            collectAncestry(node.parent, accumulator);
        }
        accumulator.add(node.chainId);
    }

    /**
     * True si esta cadena desciende de (o es) el nodo raíz indicado.
     *
     * @param ancestorId chainId del posible ancestro
     * @return true si el ancestorId aparece en la cadena de padres de este nodo
     */
    public boolean isDescendantOf(String ancestorId) {
        ModifierChain current = this;
        while (current != null) {
            if (current.chainId.equals(ancestorId)) return true;
            current = current.parent;
        }
        return false;
    }

    // ── Object ────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "ModifierChain[" + String.join(" → ", ancestry()) + "]";
    }
}
