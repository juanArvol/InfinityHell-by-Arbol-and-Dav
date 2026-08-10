package Game.Engine.Entity.Properties.Modifier.Causality;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Cadena causal de modificadores — representa la propagación de una
 * modificación a través de múltiples entidades y sistemas.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * ModifierChain responde a las preguntas de causalidad:
 *
 *   "¿Qué modificador originó éste?"
 *   "¿Quién modificó al modificador?"
 *   "¿Cuántos niveles de propagación existen?"
 *
 * ── MODELO: ÁRBOL CAUSAL ─────────────────────────────────────────────────
 * ModifierChain es un árbol causal inmutable. Cada nodo conoce su padre
 * y su profundidad. Un nodo puede tener múltiples hijos.
 *
 * ── PROFUNDIDAD MÁXIMA ────────────────────────────────────────────────────
 * Para prevenir ciclos accidentales o cadenas infinitas, ModifierChain
 * impone un límite de profundidad de 32 niveles.
 */
public final class ModifierChain {

    /** Límite de profundidad máxima para prevenir cadenas desbocadas. */
    public static final int MAX_DEPTH = 32;

    private final String       chainId;
    private final ModifierChain parent;       // null si es raíz
    private final int          depth;
    private final List<ModifierChain> children = new ArrayList<>();
    private boolean sealed = false;

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

    /** Crea un nodo raíz — el inicio de una nueva cadena causal. */
    public static ModifierChain root(String chainId) {
        return new ModifierChain(chainId, null);
    }

    /**
     * Crea un nodo hijo de esta cadena.
     *
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

    public String getChainId()                        { return chainId; }
    public int getDepth()                             { return depth; }
    public boolean isRoot()                           { return parent == null; }
    public Optional<ModifierChain> getParent()        { return Optional.ofNullable(parent); }
    public List<ModifierChain> getChildren()          { return Collections.unmodifiableList(children); }
    public boolean hasChildren()                      { return !children.isEmpty(); }
    public boolean isSealed()                         { return sealed; }

    /** Sella este nodo, impidiendo la creación de más hijos. */
    public ModifierChain seal() {
        this.sealed = true;
        return this;
    }

    /** Retorna la raíz de toda la cadena causal. */
    public ModifierChain getRoot() {
        ModifierChain current = this;
        while (current.parent != null) current = current.parent;
        return current;
    }

    /**
     * Retorna la lista de chainIds desde la raíz hasta este nodo (inclusive),
     * en orden causal de origen a destino.
     */
    public List<String> ancestry() {
        List<String> path = new ArrayList<>(depth + 1);
        collectAncestry(this, path);
        return Collections.unmodifiableList(path);
    }

    private static void collectAncestry(ModifierChain node, List<String> acc) {
        if (node.parent != null) collectAncestry(node.parent, acc);
        acc.add(node.chainId);
    }

    /** True si esta cadena desciende de (o es) el nodo raíz indicado. */
    public boolean isDescendantOf(String ancestorId) {
        ModifierChain current = this;
        while (current != null) {
            if (current.chainId.equals(ancestorId)) return true;
            current = current.parent;
        }
        return false;
    }

    @Override
    public String toString() {
        return "ModifierChain[" + String.join(" → ", ancestry()) + "]";
    }
}
