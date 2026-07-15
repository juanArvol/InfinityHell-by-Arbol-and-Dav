package Game.Gameplay.Core.Causality;

import Game.Gameplay.Core.Modifiers.PropertyModifier;
import Game.Gameplay.Core.Properties.PropertyKey;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Nodo causal de alta fidelidad — registra el hecho de que una modificación
 * ocurrió, preservando toda la información necesaria para reconstruirlo.
 *
 * ── POR QUÉ EXISTE (auditoría CFCC-002A) ────────────────────────────────
 * ModifierChain almacena únicamente strings de identificación.
 * No puede responder:
 *
 *   "¿Qué propiedad se modificó en este nodo?"
 *   "¿Cuánto cambió el valor?"
 *   "¿Bajo qué condiciones se activó?"
 *   "¿Qué modificador lo causó exactamente?"
 *   "¿Cuándo ocurrió?"
 *
 * CausalNode resuelve eso almacenando hechos, no nombres.
 *
 * ── RELACIÓN CON ModifierChain ────────────────────────────────────────────
 * ModifierChain no desaparece. Sigue siendo útil como representación
 * ligera de una trayectoria para uso en game loop y en sistemas que no
 * necesitan alta fidelidad.
 *
 * CausalNode es la capa de alta fidelidad:
 *
 *   ModifierChain  → "Spell → Projectile → Explosion"      (nombres)
 *   CausalNode     → todo lo anterior + propiedad + valores
 *                    + modificador + contexto + timestamp   (hechos)
 *
 * ── MODELO: GRAFO DIRIGIDO, NO ÁRBOL ────────────────────────────────────
 * La diferencia crítica con ModifierChain:
 *
 *   ModifierChain  → árbol: un nodo tiene exactamente UN padre.
 *   CausalNode     → grafo dirigido: un nodo puede tener MÚLTIPLES padres.
 *
 * Esto permite representar convergencia causal:
 *
 *   StatusBurning ← [Explosion] ← Projectile ← Spell
 *   StatusBurning ← [AreaOfEffect]             ← Environment
 *
 * El estado StatusBurning tiene DOS causas. Un árbol no puede modelar esto
 * sin duplicar el nodo, perdiendo la identidad del objeto. Un grafo sí.
 *
 * También permite detectar ciclos (amplificación de retroalimentación)
 * de forma estructural, no solo por límite de profundidad.
 *
 * ── CONTENIDO DE CADA NODO ───────────────────────────────────────────────
 * Cada CausalNode representa UN hecho: "el modificador M aplicó la fase P
 * sobre la propiedad K, cambiando el valor de V_before a V_after, en el
 * frame T, bajo el contexto C".
 *
 * ── CONSTRUCCIÓN ─────────────────────────────────────────────────────────
 * CausalNode se crea en PropertyResolver cuando un modificador es efectivamente
 * aplicado. El nodo se añade al ModifierContext para que los sistemas externos
 * puedan leerlo después de la resolución.
 *
 * Para vincular causalmente dos nodos (propagación):
 *
 *   CausalNode parent = ...; // nodo del hechizo
 *   CausalNode child  = CausalNode.of(modifier, property, before, after, ctx, frame);
 *   parent.addChild(child);
 *
 * ── DETECCIÓN DE CICLOS ──────────────────────────────────────────────────
 * Antes de añadir un hijo, CausalNode verifica que el hijo no es ancestro
 * del padre actual (ciclo directo). Para ciclos de orden superior (amplificación
 * recursiva), el límite configurable MAX_DEPTH actúa como salvaguarda.
 *
 * ── THREAD SAFETY ────────────────────────────────────────────────────────
 * No es thread-safe. Usar desde el game loop thread.
 */
public final class CausalNode {

    /** Profundidad máxima del grafo causal por trayectoria. Salvaguarda anti-ciclo. */
    public static final int MAX_DEPTH = 64;

    // ── Identidad del hecho ───────────────────────────────────────────────

    /**
     * El modificador que causó este hecho.
     * Referencia directa al PropertyModifier activo, no solo su sourceId.
     * Null si el nodo es una raíz sintética (inicio de cadena sin modificador).
     */
    private final PropertyModifier modifier;

    /**
     * La propiedad que fue modificada en este nodo.
     * Nunca null.
     */
    private final PropertyKey<?> property;

    /**
     * Valor de la propiedad ANTES de aplicar el modificador de este nodo.
     */
    private final double valueBefore;

    /**
     * Valor de la propiedad DESPUÉS de aplicar el modificador de este nodo.
     */
    private final double valueAfter;

    /**
     * El ModifierContext completo en el momento en que este modificador fue aplicado.
     * Contiene entidades, tags, capacidades, evento disparador, timestamp, etc.
     * Puede ser null si el resolver operó en modo CFCC-001 (sin contexto externo).
     */
    private final ModifierContext context;

    /**
     * Timestamp en frames o unidades de tiempo del juego en que ocurrió este hecho.
     */
    private final long timestamp;

    // ── Estructura del grafo ──────────────────────────────────────────────

    /**
     * Padres de este nodo en el grafo causal.
     * Un nodo puede tener múltiples padres (convergencia de causas).
     * Lista vacía si es nodo raíz.
     */
    private final List<CausalNode> parents = new ArrayList<>();

    /**
     * Hijos de este nodo en el grafo causal.
     * Representan efectos causados por este hecho.
     */
    private final List<CausalNode> children = new ArrayList<>();

    // ── Constructor privado ───────────────────────────────────────────────

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
     *
     * @param modifier    el PropertyModifier que causó el hecho (puede ser null para raíz sintética)
     * @param property    la propiedad modificada
     * @param valueBefore valor antes de aplicar el modificador
     * @param valueAfter  valor después de aplicar el modificador
     * @param context     contexto de resolución en el momento del hecho (puede ser null)
     * @param timestamp   frame o unidad de tiempo en que ocurrió
     * @return nodo causal listo para vincularse al grafo
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
     * cuando no hay un modificador específico como origen (ej: acción directa del jugador,
     * evento de entorno).
     *
     * @param label     identificador descriptivo de la causa raíz
     * @param timestamp frame o unidad de tiempo
     * @return nodo raíz sin padres ni modificador de referencia
     */
    public static CausalNode syntheticRoot(String label, long timestamp) {
        // Usamos la propiedad null-safe: la key sintética tiene id = label
        // y se construye inline como interfaz funcional.
        // No se usa PropertyKey.of() para no requerir importación de PropertyKeys de catálogo.
        PropertyKey<Void> syntheticKey = PropertyKey.of(label, Void.class, null);
        return new CausalNode(null, syntheticKey, 0.0, 0.0, null, timestamp);
    }

    // ── Vinculación en el grafo ───────────────────────────────────────────

    /**
     * Añade un nodo hijo a este nodo, estableciendo la relación causal padre→hijo.
     * Actualiza tanto la lista de hijos de este nodo como la lista de padres del hijo.
     *
     * Detecta ciclos directos: si {@code child} es ancestro de este nodo, lanza
     * excepción en lugar de crear un ciclo.
     *
     * @param child el nodo que representa un efecto causado por este hecho
     * @throws IllegalArgumentException si child es null
     * @throws IllegalStateException si añadir child crearía un ciclo directo
     * @throws IllegalStateException si la profundidad máxima sería superada
     */
    public void addChild(CausalNode child) {
        if (child == null) throw new IllegalArgumentException("child no puede ser null.");
        if (child == this) throw new IllegalStateException("Un nodo no puede ser su propio hijo.");

        // Detección de ciclo: verificar si child ya es ancestro de this
        if (isAncestor(child)) {
            throw new IllegalStateException(
                "Ciclo causal detectado: añadir '" + child.property.id()
                + "' como hijo de '" + this.property.id()
                + "' crearía un ciclo en el grafo causal."
            );
        }

        // Límite de profundidad: verificar por la trayectoria más larga desde la raíz
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

    /**
     * Desconecta este nodo de todos sus padres e hijos.
     * Útil para liberar nodos al finalizar el frame de resolución
     * cuando no se necesita persistencia del grafo completo.
     */
    public void detach() {
        for (CausalNode parent : parents) {
            parent.children.remove(this);
        }
        parents.clear();
        for (CausalNode child : children) {
            child.parents.remove(this);
        }
        children.clear();
    }

    // ── Consultas del grafo ───────────────────────────────────────────────

    /**
     * True si este nodo es raíz (no tiene padres).
     */
    public boolean isRoot() {
        return parents.isEmpty();
    }

    /**
     * True si este nodo es hoja (no tiene hijos).
     */
    public boolean isLeaf() {
        return children.isEmpty();
    }

    /**
     * Lista no modificable de padres de este nodo.
     */
    public List<CausalNode> getParents() {
        return Collections.unmodifiableList(parents);
    }

    /**
     * Lista no modificable de hijos de este nodo.
     */
    public List<CausalNode> getChildren() {
        return Collections.unmodifiableList(children);
    }

    /**
     * True si {@code candidate} es ancestro (directo o transitivo) de este nodo.
     * Usa BFS para manejar el grafo (no solo árbol).
     *
     * @param candidate nodo a buscar como ancestro
     * @return true si candidate es padre, abuelo, etc. de este nodo
     */
    public boolean isAncestor(CausalNode candidate) {
        if (candidate == null) return false;
        // BFS hacia arriba desde this
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

    /**
     * Retorna todas las raíces accesibles desde este nodo subiendo por los padres.
     * En un grafo con convergencia puede haber múltiples raíces.
     *
     * @return lista de nodos raíz (nodes without parents)
     */
    public List<CausalNode> getRoots() {
        List<CausalNode> roots   = new ArrayList<>();
        List<CausalNode> visited = new ArrayList<>();
        collectRoots(this, roots, visited);
        return Collections.unmodifiableList(roots);
    }

    private static void collectRoots(CausalNode node, List<CausalNode> roots, List<CausalNode> visited) {
        if (visited.contains(node)) return;
        visited.add(node);
        if (node.parents.isEmpty()) {
            roots.add(node);
        } else {
            for (CausalNode parent : node.parents) {
                collectRoots(parent, roots, visited);
            }
        }
    }

    /**
     * Retorna la profundidad máxima desde cualquier raíz hasta este nodo.
     * En un árbol esto es la profundidad única; en un grafo puede haber
     * múltiples caminos de diferente longitud.
     *
     * @return profundidad máxima (0 si es raíz)
     */
    public int maxDepthFromRoots() {
        return computeMaxDepth(this, new ArrayList<>());
    }

    private static int computeMaxDepth(CausalNode node, List<CausalNode> visited) {
        if (visited.contains(node)) return 0; // ciclo detectado — no subir más
        visited.add(node);
        if (node.parents.isEmpty()) return 0;
        int max = 0;
        for (CausalNode parent : node.parents) {
            int d = computeMaxDepth(parent, new ArrayList<>(visited)) + 1;
            if (d > max) max = d;
        }
        return max;
    }

    // ── Trazabilidad ──────────────────────────────────────────────────────

    /**
     * Retorna todos los nodos causales que contribuyeron a este nodo,
     * incluyendo nodos en ramas convergentes. El resultado está en orden BFS
     * de raíces hacia este nodo.
     *
     * Útil para responder: "¿qué cadena completa produjo este efecto?"
     *
     * @return lista ordenada de nodos ancestros incluyendo este nodo al final
     */
    public List<CausalNode> fullCausalHistory() {
        List<CausalNode> history = new ArrayList<>();
        List<CausalNode> visited = new ArrayList<>();
        collectHistory(this, history, visited);
        return Collections.unmodifiableList(history);
    }

    private static void collectHistory(CausalNode node, List<CausalNode> history, List<CausalNode> visited) {
        if (visited.contains(node)) return;
        for (CausalNode parent : node.parents) {
            collectHistory(parent, history, visited);
        }
        visited.add(node);
        history.add(node);
    }

    // ── Accesores de datos del hecho ─────────────────────────────────────

    /**
     * El PropertyModifier que causó este hecho, o null si es raíz sintética.
     */
    public PropertyModifier getModifier()  { return modifier; }

    /**
     * La propiedad modificada en este nodo.
     */
    public PropertyKey<?> getProperty()    { return property; }

    /**
     * Valor antes de aplicar el modificador.
     */
    public double getValueBefore()         { return valueBefore; }

    /**
     * Valor después de aplicar el modificador.
     */
    public double getValueAfter()          { return valueAfter; }

    /**
     * Diferencia neta producida por este hecho: valueAfter - valueBefore.
     */
    public double getDelta()               { return valueAfter - valueBefore; }

    /**
     * El ModifierContext en el momento del hecho, o null.
     */
    public ModifierContext getContext()    { return context; }

    /**
     * Timestamp del hecho en frames. 0 si no fue establecido.
     */
    public long getTimestamp()             { return timestamp; }

    // ── Object ────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        String modStr = modifier != null ? modifier.getSourceId() : "root";
        return "CausalNode["
            + "mod=" + modStr
            + ", prop=" + property.id()
            + ", " + valueBefore + "→" + valueAfter
            + " (Δ" + getDelta() + ")"
            + ", t=" + timestamp
            + ", parents=" + parents.size()
            + ", children=" + children.size()
            + "]";
    }
}
