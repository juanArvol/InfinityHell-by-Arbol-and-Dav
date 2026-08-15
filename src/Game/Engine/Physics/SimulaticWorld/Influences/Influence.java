package Game.Engine.Physics.SimulaticWorld.Influences;

import Game.Engine.GameObjects;

/**
 * Influencia sobre una propiedad del mundo — abstracción raíz.
 *
 * ── HRFC-015 — World Simulation Core ──────────────────────────────────────
 * ── HRFC FASE 1 — Consolidación Universal del Estado Físico ───────────────
 *
 * ── QUÉ ES UNA INFLUENCE ─────────────────────────────────────────────────
 * Una Influence es cualquier agente que modifica el estado físico del mundo
 * sobre uno o varios objetos específicos, sin requerir un campo espacial.
 *
 * La distinción con WorldField es conceptual pero importante:
 *
 *   WorldField   → actúa sobre todos los objetos en un área del espacio.
 *                  La geometría (posición, radio, falloff) es la esencia del campo.
 *                  Ejemplo: un piromante emite un campo térmico en su radio.
 *
 *   Influence    → actúa sobre un target específico o sobre un conjunto
 *                  definido por el autor. No requiere geometría espacial.
 *                  Ejemplo: una maldición que extrae energía térmica de un objetivo
 *                  concreto, o un aura que amplifica la conductividad eléctrica
 *                  de todos los aliados independientemente de su posición.
 *
 * Ambos terminan modificando el estado físico (PhysicalState via PhysicsComponent).
 * El pipeline continúa igual aguas abajo:
 *
 *   Influence modifica PhysicalState
 *       ↓
 *   PhysicsSolver resuelve ecuaciones, pares y restricciones
 *       ↓
 *   Gameplay observa el estado físico resultante
 *
 * ── USOS TÍPICOS ─────────────────────────────────────────────────────────
 * - Magia: un hechizo que enfría directamente a un objetivo específico.
 * - Auras: un buff que mantiene elevada la carga eléctrica de aliados.
 * - Poderes pasivos: una habilidad que reduce la conductividad térmica propia.
 * - Maldiciones: una debuff que incrementa la presión interna de un objetivo.
 * - Armas: una espada que extrae calor del objetivo en cada golpe.
 * - Tecnología: un dispositivo que aumenta la humedad del área objetivo.
 *
 * ── DISEÑO ────────────────────────────────────────────────────────────────
 * Influence es una interfaz con un único método: apply(target).
 * Cada implementación concreta sabe qué propiedad modificar y cuánto.
 * El Engine no conoce ninguna implementación concreta — solo la abstracción.
 *
 * apply() es llamado por InfluenceSystem cada frame mientras la influencia
 * esté activa. InfluenceSystem gestiona el ciclo de vida; Influence
 * solo describe el efecto.
 *
 * ── CICLO DE VIDA ────────────────────────────────────────────────────────
 * Una Influence puede ser:
 *   - Instantánea: aplicada una sola vez y descartada.
 *   - Persistente: aplicada cada frame hasta que el InfluenceSystem la retire.
 *   - Temporal: aplicada durante N frames y luego expirada.
 *
 * El ciclo de vida lo gestiona InfluenceSystem según lo que retorna tick():
 *   true  → la influencia sigue activa para el próximo frame.
 *   false → la influencia ha terminado y debe ser eliminada.
 *
 * ── IMMUTABILIDAD DE LA INTENSIDAD ───────────────────────────────────────
 * Las implementaciones concretas de Influence pueden tener estado interno
 * (contador de duración, intensidad variable, etc.). El Engine no impone
 * inmutabilidad — la responsabilidad de correctitud es del implementador.
 */
public interface Influence {

    /**
     * Aplica el efecto de esta influencia sobre el objeto destino.
     *
     * La implementación modifica directamente el estado físico del objeto
     * (PhysicalState via PhysicsComponent) sin evaluar consecuencias de gameplay.
     *
     * Ejemplo:
     *   PhysicsComponent pc = target.getComponent(PhysicsComponent.class);
     *   if (pc != null) {
     *       pc.getState().add(ThermalProperties.TEMPERATURE, -5.0);  // enfriar
     *   }
     *
     * Invariante: apply() NO debe crear StatusEffects, disparar eventos de
     * gameplay ni invocar lógica de combate. Solo modifica propiedades físicas.
     * Las consecuencias emergen de la capa posterior (PhysicsSolver).
     *
     * @param target objeto sobre el que se aplica la influencia. Nunca null.
     */
    void apply(GameObjects target);

    /**
     * Procesa un frame de la influencia y determina si sigue activa.
     *
     * InfluenceSystem llama tick() ANTES de apply(). Si tick() retorna false,
     * la influencia expira y apply() no se llama en este frame.
     *
     * La implementación por defecto retorna siempre true (influencia permanente).
     * Sobreescribir para implementar duración finita o condiciones de expiración.
     *
     * @return true si la influencia sigue activa; false si ha expirado.
     */
    default boolean tick() {
        return true;
    }

    /**
     * Llamado por InfluenceSystem cuando la influencia expira o es eliminada.
     * Permite limpiar recursos o revertir cambios persistentes.
     *
     * La implementación por defecto no hace nada.
     *
     * @param target objeto sobre el que actuaba la influencia.
     */
    default void onExpire(GameObjects target) {}
}
