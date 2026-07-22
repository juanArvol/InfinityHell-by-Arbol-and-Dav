package Game.Engine.World.Solver;

import Game.Engine.World.Physics.PhysicalRelation;
import Game.Engine.World.Physics.PropertyDescriptor;
import java.util.List;

/**
 * Contrato del sistema de resolución para evaluar una familia de relaciones físicas.
 *
 * ── HRFC-022 — Eliminación del Paradigma de Ley Ejecutable ───────────────
 *
 * ── PRINCIPIO FUNDAMENTAL ─────────────────────────────────────────────────
 * RelationEvaluator pertenece exclusivamente al sistema de resolución.
 *
 * Un evaluador conoce el procedimiento matemático necesario para resolver
 * una familia concreta de relaciones físicas cuando el sistema de resolución
 * lo requiera.
 *
 * RelationEvaluator no forma parte de PhysicalRelation.
 * RelationEvaluator no forma parte de las entidades.
 * RelationEvaluator no forma parte de los materiales.
 * RelationEvaluator no forma parte de las propiedades.
 *
 * ── RESPONSABILIDADES SEPARADAS ───────────────────────────────────────────
 *
 *   PhysicalRelation    → describe conocimiento físico (QUÉ)
 *   RelationEvaluator   → implementa la matemática correspondiente (CÓMO)
 *   PhysicsSolver       → coordina el proceso completo de simulación
 *
 * ── FLUJO DEL SISTEMA DE RESOLUCIÓN ──────────────────────────────────────
 *
 *   PhysicsSolver recibe PhysicalRelation
 *       ↓
 *   lee RelationType
 *       ↓
 *   EvaluatorRegistry.get(relationType)  →  RelationEvaluator
 *       ↓
 *   evaluator.evaluate(relation, views, deltaTime)
 *       ↓
 *   los cambios quedan acumulados en los EvaluationView de cada entidad
 *       ↓
 *   PhysicsSolver hace commit de todos los cambios al PhysicalState definitivo
 *
 * ── CONTRATO DE evaluate() ────────────────────────────────────────────────
 * El evaluador recibe:
 *   relation  → la PhysicalRelation declarativa que describe el fenómeno.
 *               El evaluador la consulta para conocer las propiedades
 *               participantes y las restricciones.
 *   views     → lista de vistas de entidades activas con estado físico.
 *               El evaluador itera sobre ellas, aplica las restricciones
 *               de la relación (distancia, umbral, propiedad presente...)
 *               y acumula deltas mediante view.add().
 *   deltaTime → tiempo transcurrido desde el último frame, en segundos.
 *
 * El evaluador NUNCA:
 *   ✗ modifica PhysicalState directamente.
 *   ✗ lee o escribe fuera de los EvaluationView que recibe.
 *   ✗ accede a entidades fuera de la lista recibida.
 *   ✗ contiene estado mutable propio.
 *   ✗ conoce materiales ni tipos de entidad.
 *
 * ── EXTENSIBILIDAD ────────────────────────────────────────────────────────
 * Añadir un nuevo fenómeno:
 *   1. Añadir constante en RelationType.
 *   2. Implementar RelationEvaluator para ese tipo.
 *   3. Registrar en EvaluatorRegistry.
 *
 * No se modifica ningún evaluador existente ni ningún componente del Core.
 */
public interface RelationEvaluator {

    /**
     * Evalúa la relación física sobre las entidades activas.
     *
     * El evaluador itera las vistas, aplica las restricciones declaradas
     * en la relación y acumula deltas sobre las propiedades participantes.
     *
     * Los cambios acumulados mediante {@link EvaluationView#add} son
     * aplicados al PhysicalState definitivo por el PhysicsSolver al finalizar
     * la evaluación de todas las relaciones del frame.
     *
     * @param relation  la relación física a evaluar. Nunca null.
     * @param views     vistas de las entidades activas este frame. Nunca null.
     * @param deltaTime tiempo del frame en segundos. Siempre positivo.
     */
    void evaluate(PhysicalRelation       relation,
                  List<EvaluationView>   views,
                  double                 deltaTime);

    // ── Vista de entidad para evaluadores ─────────────────────────────────

    /**
     * Vista de lectura y escritura diferida del estado físico de una entidad,
     * expuesta exclusivamente al sistema de resolución.
     *
     * Un evaluador lee el estado actual con {@link #has} y {@link #get},
     * y acumula cambios con {@link #add}. Los cambios se aplican al
     * PhysicalState definitivo únicamente durante la fase de Commit
     * al finalizar todos los evaluadores.
     *
     * ── IDENTIDAD FUERTE ──────────────────────────────────────────────────
     * Todo acceso es exclusivamente por PropertyDescriptor.
     * No existe ningún método que acepte String como identificador.
     *
     * ── ESCRITURA DIFERIDA ────────────────────────────────────────────────
     * add() acumula deltas. Los deltas no son visibles en las lecturas
     * de get() durante el mismo frame — todas las lecturas reflejan el
     * snapshot tomado al inicio del frame.
     *
     * ── POSICIÓN ─────────────────────────────────────────────────────────
     * x() e y() exponen la posición de la entidad en el mundo.
     * Los evaluadores que operan sobre pares de entidades (FOURIER, OHM,
     * FICK...) las usan para calcular distancias y verificar la restricción
     * MAX_DISTANCE.
     */
    interface EvaluationView {

        /**
         * True si la entidad tiene la propiedad registrada en su estado.
         *
         * @param descriptor descriptor de la propiedad.
         * @return true si existe.
         */
        boolean has(PropertyDescriptor descriptor);

        /**
         * Valor del snapshot de la propiedad para este frame.
         * Retorna 0.0 si la entidad no tiene esa propiedad.
         *
         * @param descriptor descriptor de la propiedad.
         * @return valor del snapshot, o 0.0 si no existe.
         */
        double get(PropertyDescriptor descriptor);

        /**
         * Acumula un delta sobre la propiedad de la entidad.
         * El delta se aplica al PhysicalState definitivo al finalizar
         * todos los evaluadores. No hace nada si la entidad no tiene
         * la propiedad.
         *
         * @param descriptor descriptor de la propiedad.
         * @param delta      valor a añadir. Negativo para restar.
         */
        void add(PropertyDescriptor descriptor, double delta);

        /**
         * Posición X de la entidad en el mundo.
         * Usada por evaluadores de pares para calcular distancias.
         *
         * @return coordenada X en unidades del mundo.
         */
        double x();

        /**
         * Posición Y de la entidad en el mundo.
         * Usada por evaluadores de pares para calcular distancias.
         *
         * @return coordenada Y en unidades del mundo.
         */
        double y();

        /**
         * Estado transitorio de magnitudes derivadas de esta entidad para el frame actual.
         *
         * Un evaluador escribe aquí los resultados matemáticos intermedios que
         * otro evaluador posterior necesita leer — sin contaminar PhysicalState
         * ni introducir propiedades puente persistentes.
         *
         * Ejemplos de uso:
         *   // OhmEvaluator escribe la corriente calculada:
         *   view.frameState().add(FrameMagnitudes.CURRENT, Math.abs(transferred));
         *
         *   // JouleEvaluator la lee en la misma pasada del frame:
         *   double current = view.frameState().get(FrameMagnitudes.CURRENT);
         *
         * El FrameState de cada entidad es destruido al finalizar el frame.
         * Nunca persiste. Nunca se escribe en PhysicalState.
         *
         * @return el FrameState de esta entidad para este frame. Nunca null.
         */
        FrameState frameState();
    }
}
