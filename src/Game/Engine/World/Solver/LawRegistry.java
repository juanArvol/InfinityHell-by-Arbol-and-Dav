package Game.Engine.World.Solver;

import Game.Engine.World.Physics.PhysicsLaw;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Registro de leyes físicas declarativas.
 *
 * ── HRFC-019 — Eliminación Definitiva del Modelo Orientado a Tipos de Ley ─
 *
 * ── RESPONSABILIDAD ÚNICA ────────────────────────────────────────────────
 * LawRegistry almacena instancias de PhysicsLaw.
 * No produce leyes. No tiene leyes por defecto. No conoce fenómenos.
 * No conoce dominios. No conoce categorías de leyes.
 * Únicamente almacena y expone la lista de leyes para que el PhysicsSolver
 * las recorra.
 *
 * ── POR QUÉ NO HAY defaults() ─────────────────────────────────────────────
 * En el modelo anterior, LawRegistry.defaults() era una factory que producía
 * las 10 leyes fundamentales del Engine. Esa responsabilidad acoplaba el
 * registro a un conjunto concreto de leyes, convirtiendo al Engine en el
 * productor oficial del conocimiento físico.
 *
 * El Engine no debe decidir qué leyes existen. El Engine solo debe resolverlas.
 *
 * El conocimiento físico concreto (las 10 leyes base) vive ahora en CoreLaws,
 * un catálogo externo al Core que cualquier mundo puede usar, ignorar o extender.
 *
 * ── ORÍGENES DE LAS LEYES ────────────────────────────────────────────────
 * Las leyes pueden provenir de cualquier fuente:
 *   - Engine:   CoreLaws.all()
 *   - Gameplay: leyes de mecánicas de juego
 *   - Mods:     leyes registradas por módulos externos
 *   - Mundos:   leyes específicas de una zona o bioma
 *   - Plugins:  leyes inyectadas en runtime
 *
 * El LawRegistry no distingue el origen. Todas las leyes son iguales.
 *
 * ── USO BÁSICO ────────────────────────────────────────────────────────────
 *
 *   // Registro con leyes del catálogo base
 *   LawRegistry registry = new LawRegistry()
 *       .registerAll(CoreLaws.all());
 *
 *   // Registro vacío con leyes personalizadas
 *   LawRegistry registry = new LawRegistry()
 *       .register(PhysicsLaw.builder()...build());
 *
 *   // Extender el catálogo base con leyes adicionales
 *   LawRegistry registry = new LawRegistry()
 *       .registerAll(CoreLaws.all())
 *       .register(magnetismLaw)
 *       .register(gravityLaw);
 *
 * ── INVARIANTE ────────────────────────────────────────────────────────────
 *   ✗ Nunca produce leyes propias.
 *   ✗ Nunca conoce fenómenos físicos concretos.
 *   ✗ Nunca tiene un conjunto "oficial" de leyes.
 *   ✓ Solo almacena instancias de PhysicsLaw y las expone.
 */
public final class LawRegistry {

    private final List<PhysicsLaw> laws = new ArrayList<>();

    // ── Constructor ───────────────────────────────────────────────────────

    /** Crea un registro vacío. */
    public LawRegistry() {}

    // ── Registro ──────────────────────────────────────────────────────────

    /**
     * Registra una ley física.
     *
     * @param law la ley a registrar. Ignorado si null.
     * @return this (para encadenado).
     */
    public LawRegistry register(PhysicsLaw law) {
        if (law != null) laws.add(law);
        return this;
    }

    /**
     * Registra múltiples leyes físicas.
     *
     * @param laws leyes a registrar.
     * @return this (para encadenado).
     */
    public LawRegistry registerAll(PhysicsLaw... laws) {
        if (laws == null) return this;
        for (PhysicsLaw law : laws) register(law);
        return this;
    }

    /**
     * Copia todas las leyes de otro LawRegistry en este registro.
     *
     * @param other registro cuyas leyes se añaden. Ignorado si null.
     * @return this (para encadenado).
     */
    public LawRegistry registerAll(LawRegistry other) {
        if (other != null) this.laws.addAll(other.laws);
        return this;
    }

    // ── Consultas ─────────────────────────────────────────────────────────

    /**
     * Vista inmutable de todas las leyes registradas, en orden de registro.
     *
     * @return lista inmutable de PhysicsLaw.
     */
    public List<PhysicsLaw> laws() {
        return Collections.unmodifiableList(laws);
    }

    /** Número de leyes registradas. */
    public int size() { return laws.size(); }

    /** True si no hay leyes registradas. */
    public boolean isEmpty() { return laws.isEmpty(); }

    // ── Object ────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "LawRegistry[" + laws.size() + " laws]";
    }
}
