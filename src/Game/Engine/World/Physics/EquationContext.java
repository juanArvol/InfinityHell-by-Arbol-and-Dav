package Game.Engine.World.Physics;

import Game.Engine.World.Components.MaterialComponent;

/**
 * Contexto de lectura de estado físico durante la evaluación de una ecuación.
 *
 * ── HRFC-017 — Consolidación Definitiva del Modelo Declarativo ────────────
 *
 * ── RESPONSABILIDAD ──────────────────────────────────────────────────────
 * EquationContext es la única ventana que una PhysicsEquation tiene sobre el
 * estado de un objeto. Proporciona acceso de solo lectura a los valores de
 * la propiedad fuente y destino, y al material del objeto.
 *
 * Las funciones Condition y Coefficient de PhysicsEquation reciben este
 * contexto. Así el PhysicsSolver puede exponer exactamente la información
 * que las ecuaciones necesitan sin exponer ni el GameObjects ni el PhysicalState
 * completo.
 *
 * ── SOLO LECTURA ──────────────────────────────────────────────────────────
 * EquationContext es estrictamente de lectura. Las ecuaciones nunca modifican
 * el estado directamente — solo calculan valores. El Solver aplica el delta
 * resultante sobre el PhysicalState.
 *
 * ── DISEÑO ────────────────────────────────────────────────────────────────
 * EquationContext es genérico sobre los dominios fuente y destino para
 * mantener el type-safety sin duplicar código. Cada PhysicsEquation<S,T>
 * recibe un EquationContext<S,T> tipado.
 *
 * @param <S> dominio de la propiedad fuente.
 * @param <T> dominio de la propiedad destino.
 */
public final class EquationContext<S extends PhysicalDomain, T extends PhysicalDomain> {

    private final PhysicalQuantity<S>  sourceQuantity;
    private final PhysicalQuantity<T>  targetQuantity;
    private final MaterialComponent    material;

    // ── Constructor ───────────────────────────────────────────────────────

    /**
     * Crea un contexto de ecuación.
     *
     * @param sourceQuantity magnitud fuente (puede ser null si el objeto no tiene esa propiedad).
     * @param targetQuantity magnitud destino (puede ser null si el objeto no tiene esa propiedad).
     * @param material       material del objeto. Nunca null — usar MaterialComponent.DEFAULT.
     */
    public EquationContext(PhysicalQuantity<S> sourceQuantity,
                           PhysicalQuantity<T> targetQuantity,
                           MaterialComponent material) {
        if (material == null) throw new IllegalArgumentException("material no puede ser null");
        this.sourceQuantity = sourceQuantity;
        this.targetQuantity = targetQuantity;
        this.material       = material;
    }

    // ── Acceso a la fuente ────────────────────────────────────────────────

    /**
     * True si el objeto tiene la propiedad fuente registrada en su PhysicalState.
     *
     * @return true si sourceQuantity no es null.
     */
    public boolean hasSource() { return sourceQuantity != null; }

    /**
     * Valor numérico actual de la propiedad fuente.
     * Retorna 0.0 si el objeto no tiene la propiedad fuente.
     *
     * @return valor de la fuente, o 0.0 si no existe.
     */
    public double source() {
        return sourceQuantity != null ? sourceQuantity.getValue() : 0.0;
    }

    /**
     * La PhysicalQuantity fuente completa.
     * Puede ser null si el objeto no tiene la propiedad fuente.
     *
     * @return magnitud fuente o null.
     */
    public PhysicalQuantity<S> sourceQuantity() { return sourceQuantity; }

    // ── Acceso al destino ─────────────────────────────────────────────────

    /**
     * True si el objeto tiene la propiedad destino registrada en su PhysicalState.
     *
     * @return true si targetQuantity no es null.
     */
    public boolean hasTarget() { return targetQuantity != null; }

    /**
     * Valor numérico actual de la propiedad destino.
     * Retorna 0.0 si el objeto no tiene la propiedad destino.
     *
     * @return valor del destino, o 0.0 si no existe.
     */
    public double target() {
        return targetQuantity != null ? targetQuantity.getValue() : 0.0;
    }

    /**
     * La PhysicalQuantity destino completa.
     * Puede ser null si el objeto no tiene la propiedad destino.
     *
     * @return magnitud destino o null.
     */
    public PhysicalQuantity<T> targetQuantity() { return targetQuantity; }

    // ── Acceso al material ────────────────────────────────────────────────

    /**
     * El material del objeto. Nunca null.
     * Usar para acceder a las constantes físicas del material
     * (conductividad, capacidad calorífica, compresibilidad, etc.)
     *
     * @return material del objeto.
     */
    public MaterialComponent getMaterial() { return material; }
}
