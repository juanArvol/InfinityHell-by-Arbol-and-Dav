package Game.Engine.Entity.Properties.Modifier.Causality;

import Game.Engine.Entity.Properties.Modifier.PropertyModifier;
import Game.Engine.Entity.Properties.PropertyKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Nodo causal de alta fidelidad — registra el hecho de que una modificación
 * ocurrió, preservando toda la información necesaria para reconstruirlo.
 *
 * ── POR QUÉ EXISTE ────────────────────────────────────────────────────────
 * ModifierChain almacena únicamente strings de identificación.
 * No puede responder:
 *
 *   "¿Qué propiedad se modificó en este nodo?"
 *   "¿Cuánto cambió el valor?"
 *   "¿Bajo qué condiciones se activó?"
 *
 * CausalNode resuelve eso almacenando hechos, no nombres.
 *
 * ── MODELO: GRAFO DIRIGIDO, NO ÁRBOL ────────────────────────────────────
 * ModifierChain → árbol: un nodo tiene exactamente UN padre.
 * CausalNode    → grafo dirigido: un nodo puede tener MÚLTIPLES padres.
 *
 * Esto permite representar convergencia causal:
 *   StatusBurning ← [Explosion] ← Projectile ← Spell
 *   StatusBurning ← [AreaOfEffect] ← Environment
 *
 * ── DETECCIÓN DE CICLOS ──────────────────────────────────────────────────
 * Antes de añadir un hijo, CausalNode verifica que el hijo no es ancestro
 * del padre actual. Para ciclos de orden superior, MAX_DEPTH actúa como
 * salvaguarda.
 *
 * ── THREAD SAFETY ────────────────────────────────────────────────────────
 * No es thread-safe. Usar desde el game loop thread.
 */
public final class CausalNode {

    /** Profundidad máxima del grafo causal por trayectoria. Salvaguarda anti-ciclo. */
    public static final int MAX_DEPTH = 64;

    private final PropertyModifier modifier;
    private final PropertyKey<?>   property;
    private final double           valueBefore;
    private final double           valueAfter;
    private final ModifierContext  context;
    private final long             timestamp;

    private final List<CausalNode> parents  = new ArrayList<>();
    private final List<CausalNode> children = new ArrayList<>();

    private CausalNode(
            PropertyModifier modifier,
            PropertyKey<?>   property,
            double           valueBefore,
            double           valueAfter,
            ModifierContext  context,
            long             timestamp) {

        if (property == null)
            throw new IllegalArgumentException("CausalNode requiere un PropertyKey no null.");

        this.modifier    = modifier;
        this.property    = property;
        this.valueBefore = valueBefore;
        this.valueAfter  = valueAfter;
        this.context     = context;
        this.timestamp   = timestamp;
    }

    // ── Factory methods ───────────────────────────────────────────────────

    /**
     * Crea un nodo causal completo registrando un hecho de modificación.
     */
    public static CausalNode of(
            PropertyModifier modifier,
            PropertyKey<?>   property,
            double           valueBefore,
            double           valueAfter,
            ModifierContext  context,
            long             timestamp) {
        return new CausalNode(modifier, property, valueBefore, valueAfter, context, timestamp);
    }

    /**
     * Crea un nodo raíz sintético — representa el inicio de una cadena causal
     * cuando no hay un modificador específico como origen.
     */
    public static CausalNode syntheticRoot(String label, long timestamp) {
        PropertyKey<Void> syntheticKey = PropertyKey.of(label, Void.class, null);
        return new CausalNode(null, syntheticKey, 0.0, 0.0, null, timestamp);
    }    // ── Vinculación en el grafo ───────────────────────────────────────────

    /**
     * Añade un nodo hijo a este nodo, estableciendo la relación causal padre→hijo.
     *
     * @throws IllegalArgumentException si child es null
     * @throws IllegalStateException si añadir child crearía un ciclo directo
     * @throws IllegalStateException si la profundidad máxima sería superada
     */
    public void addChild(CausalNode child) {
        if (child == null) throw new IllegalArgumentException("child no puede ser null.");
        if (child == this) throw new IllegalStateException("Un nodo no puede ser su propio hijo.");

        if (isAncestor(child)) {
            throw new IllegalStateException(
                "Ciclo causal detectado: añadir '" + child.property.displayName()
                + "' como hijo de '" + this.property.displayName()
                + "' crearía un ciclo en el grafo causal."
            );
        }

        int maxParentDepth = this.maxDepthFromRoots();
        if (maxParentDepth + 1 > MAX_DEPTH) {
            throw new IllegalStateException(
                "El grafo causal excede la profundidad máxima (" + MAX_DEPTH
                + "). Posible propagación desbocada."
            );
        }

        children.add(child);
        child.parents.add(this);
    }

    /** Desconecta este nodo de todos sus padres e hijos. */
    public void detach() {
        for (CausalNode parent : parents)   parent.children.remove(this);
        parents.clear();
        for (CausalNode child : children)   child.parents.remove(this);
        children.clear();
    }

    // ── Consultas del grafo ───────────────────────────────────────────────

    public boolean isRoot()                        { return parents.isEmpty(); }
    public boolean isLeaf()                        { return children.isEmpty(); }
    public List<CausalNode> getParents()           { return Collections.unmodifiableList(parents); }
    public List<CausalNode> getChildren()          { return Collections.unmodifiableList(children); }

    /** True si {@code candidate} es ancestro (directo o transitivo) de este nodo. */
    public boolean isAncestor(CausalNode candidate) {
        if (candidate == null) return false;
        List<CausalNode> frontier = new ArrayList<>(parents);
        List<CausalNode> visited  = new ArrayList<>();
        while (!frontier.isEmpty()) {
            CausalNode current = frontier.remove(frontier.size() - 1);
            if (current == candidate) return true;
            if (!visited.contains(current)) {
                visited.add(current);
                frontier.addAll(current.parents);
            }
        }
        return false;
    }

    /** Retorna todas las raíces accesibles desde este nodo. */
    public List<CausalNode> getRoots() {
        List<CausalNode> roots   = new ArrayList<>();
        List<CausalNode> visited = new ArrayList<>();
        collectRoots(this, roots, visited);
        return Collections.unmodifiableList(roots);
    }

    private static void collectRoots(CausalNode node, List<CausalNode> roots, List<CausalNode> visited) {
        if (visited.contains(node)) return;
        visited.add(node);
        if (node.parents.isEmpty()) { roots.add(node); }
        else { for (CausalNode parent : node.parents) collectRoots(parent, roots, visited); }
    }

    /** Retorna la profundidad máxima desde cualquier raíz hasta este nodo. */
    public int maxDepthFromRoots() {
        return computeMaxDepth(this, new ArrayList<>());
    }

    private static int computeMaxDepth(CausalNode node, List<CausalNode> visited) {
        if (visited.contains(node)) return 0;
        visited.add(node);
        if (node.parents.isEmpty()) return 0;
        int max = 0;
        for (CausalNode parent : node.parents) {
            int d = computeMaxDepth(parent, new ArrayList<>(visited)) + 1;
            if (d > max) max = d;
        }
        return max;
    }

    /**
     * Retorna todos los nodos causales que contribuyeron a este nodo,
     * en orden BFS de raíces hacia este nodo.
     */
    public List<CausalNode> fullCausalHistory() {
        List<CausalNode> history = new ArrayList<>();
        List<CausalNode> visited = new ArrayList<>();
        collectHistory(this, history, visited);
        return Collections.unmodifiableList(history);
    }

    private static void collectHistory(CausalNode node, List<CausalNode> history, List<CausalNode> visited) {
        if (visited.contains(node)) return;
        for (CausalNode parent : node.parents) collectHistory(parent, history, visited);
        visited.add(node);
        history.add(node);
    }

    // ── Accesores de datos del hecho ─────────────────────────────────────

    public PropertyModifier getModifier()  { return modifier; }
    public PropertyKey<?> getProperty()    { return property; }
    public double getValueBefore()         { return valueBefore; }
    public double getValueAfter()          { return valueAfter; }
    public double getDelta()               { return valueAfter - valueBefore; }
    public ModifierContext getContext()    { return context; }
    public long getTimestamp()             { return timestamp; }

    @Override
    public String toString() {
        String modStr = modifier != null ? modifier.getSourceId() : "root";
        return "CausalNode["
            + "mod=" + modStr
            + ", prop=" + property.displayName()
            + ", " + valueBefore + "→" + valueAfter
            + " (Δ" + getDelta() + ")"
            + ", t=" + timestamp
            + ", parents=" + parents.size()
            + ", children=" + children.size()
            + "]";
    }
}
