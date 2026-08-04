package Game.Engine.Physics.Core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Registro de relaciones físicas declarativas.
 *
 * ── HRFC-022 — Eliminación del Paradigma de Ley Ejecutable ───────────────
 *
 * ── RESPONSABILIDAD ÚNICA ────────────────────────────────────────────────
 * RelationRegistry almacena instancias de PhysicalRelation.
 * No produce relaciones. No tiene relaciones por defecto.
 * No conoce fenómenos físicos. No conoce dominios. No conoce categorías.
 * Únicamente almacena y expone la lista de relaciones para que el
 * PhysicsSolver las evalúe mediante los evaluadores especializados.
 *
 * ── REEMPLAZA A LawRegistry ────────────────────────────────────────────────
 * LawRegistry almacenaba PhysicsLaw — objetos con comportamiento ejecutable.
 * RelationRegistry almacena PhysicalRelation — descriptores declarativos.
 * El cambio no es solo de nombre: es un cambio fundamental de paradigma.
 *
 * ── ORÍGENES DE LAS RELACIONES ───────────────────────────────────────────
 * Las relaciones pueden provenir de cualquier fuente:
 *   - Engine:   ThermalRelations.all(), ElectricalRelations.all(), FluidRelations.all()
 *   - Gameplay: relaciones de mecánicas de juego
 *   - Mods:     relaciones registradas por módulos externos
 *   - Mundos:   relaciones específicas de una zona o bioma
 *
 * RelationRegistry no distingue el origen. Todas son iguales.
 *
 * ── USO ───────────────────────────────────────────────────────────────────
 *
 *   // Registro con las relaciones del catálogo base
 *   RelationRegistry registry = new RelationRegistry()
 *       .registerAll(CoreRelations.all());
 *
 *   // Registro con relaciones personalizadas
 *   RelationRegistry registry = new RelationRegistry()
 *       .register(myCustomRelation);
 *
 *   // Extender el catálogo base con relaciones adicionales
 *   RelationRegistry registry = new RelationRegistry()
 *       .registerAll(CoreRelations.all())
 *       .register(gravityRelation)
 *       .register(magnetismRelation);
 *
 * ── INVARIANTE ────────────────────────────────────────────────────────────
 *   ✗ Nunca produce relaciones propias.
 *   ✗ Nunca conoce fenómenos físicos concretos.
 *   ✗ Nunca tiene un conjunto "oficial" de relaciones.
 *   ✓ Solo almacena instancias de PhysicalRelation y las expone.
 */
public final class RelationRegistry {

    private final List<PhysicalRelation> relations = new ArrayList<>();

    /** Crea un registro vacío. */
    public RelationRegistry() {}

    // ── Registro ──────────────────────────────────────────────────────────

    /**
     * Registra una relación física.
     *
     * @param relation la relación a registrar. Ignorada si null.
     * @return this (para encadenado).
     */
    public RelationRegistry register(PhysicalRelation relation) {
        if (relation != null) relations.add(relation);
        return this;
    }

    /**
     * Registra múltiples relaciones físicas.
     *
     * @param relations relaciones a registrar.
     * @return this (para encadenado).
     */
    public RelationRegistry registerAll(PhysicalRelation... relations) {
        if (relations == null) return this;
        for (PhysicalRelation r : relations) register(r);
        return this;
    }

    /**
     * Copia todas las relaciones de otro RelationRegistry en este registro.
     *
     * @param other registro cuyas relaciones se añaden. Ignorado si null.
     * @return this (para encadenado).
     */
    public RelationRegistry registerAll(RelationRegistry other) {
        if (other != null) this.relations.addAll(other.relations);
        return this;
    }

    // ── Consultas ─────────────────────────────────────────────────────────

    /**
     * Vista inmutable de todas las relaciones registradas, en orden de registro.
     *
     * @return lista inmutable de PhysicalRelation.
     */
    public List<PhysicalRelation> relations() {
        return Collections.unmodifiableList(relations);
    }

    /** Número de relaciones registradas. */
    public int size() { return relations.size(); }

    /** True si no hay relaciones registradas. */
    public boolean isEmpty() { return relations.isEmpty(); }

    @Override
    public String toString() {
        return "RelationRegistry[" + relations.size() + " relations]";
    }
}
