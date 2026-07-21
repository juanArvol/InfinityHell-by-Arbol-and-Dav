package Game.Engine.World.Physics;

import java.util.List;

/**
 * Vista del mundo que una ley física recibe para operar.
 *
 * ── HRFC-019 — Eliminación Definitiva del Modelo Orientado a Tipos de Ley ─
 *
 * ── FILOSOFÍA ─────────────────────────────────────────────────────────────
 * WorldContext es el único punto de contacto entre una PhysicsLaw y el mundo.
 *
 * El Solver construye un WorldContext una vez por frame y lo entrega a todas
 * las leyes sin distinción. Cada ley decide qué entidades consulta, cuántas
 * necesita, cómo las filtra y qué cambios acumula.
 *
 * El Solver no prepara contextos distintos según el tipo de ley.
 * El Solver no conoce qué hace la ley con el contexto.
 * El Solver no sabe si la ley opera sobre un objeto, dos, o cien.
 *
 * ── QUÉ PUEDE HACER UNA LEY CON ESTE CONTEXTO ────────────────────────────
 *
 *   Una ley de gravedad (un objeto):
 *     for (EntityView e : ctx.entities())
 *         if (e.has("velocity_y"))
 *             e.add("velocity_y", 9.8 * ctx.deltaTime());
 *
 *   Una ley de transferencia térmica (pares):
 *     List<EntityView> all = ctx.entities();
 *     for (int i = 0; i < all.size() - 1; i++)
 *         for (int j = i + 1; j < all.size(); j++)
 *             if (ctx.distance(all.get(i), all.get(j)) <= 32.0)
 *                 // transferir entre i y j
 *
 *   Una ley de campo gravitacional (N cuerpos):
 *     List<EntityView> all = ctx.entities();
 *     for (EntityView a : all)
 *         for (EntityView b : all)
 *             if (a != b)
 *                 // fuerza entre pares
 *
 * ── INVARIANTE ────────────────────────────────────────────────────────────
 *   ✗ El Solver nunca clasifica leyes para construir este contexto.
 *   ✗ Este contexto nunca expone clasificaciones de entidades.
 *   ✓ El Solver entrega exactamente el mismo WorldContext a todas las leyes.
 *   ✓ Cada ley es responsable de su propio patrón de acceso.
 */
public interface WorldContext {

    /**
     * Lista de todas las entidades activas con estado físico este frame.
     * Las entidades están en orden estable durante un frame completo.
     *
     * La ley puede iterar una, todas, o ninguna de ellas según su lógica.
     *
     * @return lista inmutable en orden de entidades activas. Nunca null.
     */
    List<EntityView> entities();

    /**
     * Tiempo transcurrido desde el último frame, en segundos.
     * Usado por leyes que necesitan integrar cantidades por unidad de tiempo.
     *
     * @return delta time en segundos. Siempre positivo.
     */
    double deltaTime();

    /**
     * Distancia euclídea entre dos entidades en unidades del mundo.
     * Útil para leyes de transferencia que operan sobre pares dentro de un radio.
     *
     * @param a primera entidad.
     * @param b segunda entidad.
     * @return distancia euclídea. Siempre >= 0.
     */
    double distance(EntityView a, EntityView b);

    // ─────────────────────────────────────────────────────────────────────
    // Vista de una entidad individual
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Vista de lectura y escritura diferida del estado físico de una entidad.
     *
     * Una ley lee el estado actual con {@link #get} y {@link #has}, y acumula
     * cambios con {@link #add}. Los cambios se aplican al PhysicalState real
     * al finalizar la ejecución de la ley en el Solver.
     *
     * ── ESCRITURA DIFERIDA ────────────────────────────────────────────────
     * {@link #add} acumula deltas en un buffer local de la entidad.
     * El buffer se vuelca al PhysicalState después de que la ley complete
     * su método solve(). Esto garantiza que todos los cálculos dentro de
     * solve() ven un estado consistente: el estado al inicio de esa ley.
     *
     * Dos leyes nunca se interfieren entre sí: los deltas de la ley A se
     * aplican antes de que la ley B comience a leer.
     *
     * ── PROPIEDAD vs MATERIAL ──────────────────────────────────────────────
     * No existe distinción entre propiedades de estado y propiedades de
     * material. Ambas se leen con {@link #get}. Las leyes no saben si
     * "thermal_conductivity" es una propiedad de material o de estado —
     * es simplemente un valor numérico identificado por un string.
     *
     * ── INVARIANTE ────────────────────────────────────────────────────────
     *   ✗ Nunca expone el GameObjects ni el PhysicalState directamente.
     *   ✗ Nunca expone tipos de dominio.
     *   ✓ Todo acceso es por identificador de texto.
     *   ✓ Escritura es siempre un delta acumulado, nunca un set absoluto.
     */
    interface EntityView {

        /**
         * True si la entidad tiene la propiedad registrada en su estado.
         *
         * @param propertyId identificador de la propiedad.
         * @return true si existe.
         */
        boolean has(String propertyId);

        /**
         * Valor numérico actual de la propiedad.
         * Retorna 0.0 si la entidad no tiene esa propiedad.
         *
         * @param propertyId identificador de la propiedad.
         * @return valor actual, o 0.0 si no existe.
         */
        double get(String propertyId);

        /**
         * Acumula un delta sobre la propiedad de la entidad.
         * El delta se aplica al PhysicalState real al finalizar la ley actual.
         * No hace nada si la entidad no tiene la propiedad.
         *
         * @param propertyId identificador de la propiedad.
         * @param delta      valor a añadir. Negativo para restar.
         */
        void add(String propertyId, double delta);
    }
}
