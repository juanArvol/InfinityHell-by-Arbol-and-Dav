package Game.Engine.World.Physics.Core;

/**
 * Descriptor tipado de una magnitud física transitoria del frame.
 *
 * ── HRFC-022 — Identidad fuerte en FrameState ────────────────────────────
 *
 * ── PRINCIPIO FUNDAMENTAL ─────────────────────────────────────────────────
 * FrameMagnitude es al FrameState lo que PropertyDescriptor es a PhysicalState.
 *
 * Elimina completamente el uso de String como identificador de magnitudes
 * transitorias. Toda escritura y lectura en FrameState ocurre mediante
 * una referencia de descriptor, nunca mediante una clave literal.
 *
 * ── DISEÑO ────────────────────────────────────────────────────────────────
 * FrameMagnitude es inmutable. Contiene:
 *
 *   id          → identificador único legible. Solo para toString() y depuración.
 *   description → descripción física de la magnitud. Puede ser null.
 *
 * La identidad es por referencia de objeto (identity-based equals/hashCode),
 * igual que PropertyDescriptor. Dos descriptores con el mismo id son objetos
 * distintos y representan magnitudes distintas. El compilador garantiza
 * consistencia porque los evaluadores referencian directamente las constantes
 * de FrameMagnitudes.
 *
 * ── QUÉ NO CONTIENE ──────────────────────────────────────────────────────
 *   ✗ Ningún valor por defecto (las magnitudes transitorias comienzan siempre en 0).
 *   ✗ Ningún límite min/max (no son propiedades del mundo).
 *   ✗ Ninguna referencia al evaluador que la produce.
 *   ✗ Ninguna lógica de simulación.
 *
 * ── USO ───────────────────────────────────────────────────────────────────
 *
 *   // En un evaluador que produce corriente:
 *   view.frameState().add(FrameMagnitudes.CURRENT, Math.abs(transferred));
 *
 *   // En un evaluador que consume corriente:
 *   double current = view.frameState().get(FrameMagnitudes.CURRENT);
 *
 * ── INSTANCIACIÓN RESTRINGIDA AL PAQUETE ─────────────────────────────────
 * El constructor es package-private. Solo FrameMagnitudes, que reside en el
 * mismo paquete (Game.Engine.World.Solver), puede crear instancias.
 * Ningún código externo al paquete puede construir un FrameMagnitude.
 *
 * FrameMagnitudes es la única fuente autorizada de descriptores.
 * Del mismo modo que CoreProperties es la única fuente de PropertyDescriptor
 * en el Core.
 *
 * ── EXTENSIBILIDAD ────────────────────────────────────────────────────────
 * Añadir una nueva magnitud transitoria:
 *   1. Añadir una constante en FrameMagnitudes (mismo paquete).
 *   2. No modificar FrameState. No modificar ningún evaluador existente.
 */
public final class FrameMagnitude {

    /** Identificador legible. Solo para depuración. */
    private final String id;

    /** Descripción física de la magnitud. Puede ser null. */
    private final String description;

    // ── Constructor package-private — solo FrameMagnitudes puede instanciar ─

    FrameMagnitude(String id, String description) {
        if (id == null || id.isBlank())
            throw new IllegalArgumentException("id no puede ser null ni vacío");
        this.id          = id;
        this.description = description;
    }

    // ── Accesores ─────────────────────────────────────────────────────────

    /** Identificador legible. Solo para depuración. */
    public String getId() { return id; }

    /** Descripción física de la magnitud. Puede ser null. */
    public String getDescription() { return description; }

    // ── Object — identidad por referencia ────────────────────────────────

    /**
     * Identidad basada en referencia de objeto.
     * Dos descriptores son iguales si y solo si son el mismo objeto.
     * Igual que PropertyDescriptor.
     */
    @Override
    public boolean equals(Object o) { return this == o; }

    @Override
    public int hashCode() { return System.identityHashCode(this); }

    @Override
    public String toString() {
        return "FrameMagnitude[" + id + "]";
    }
}
