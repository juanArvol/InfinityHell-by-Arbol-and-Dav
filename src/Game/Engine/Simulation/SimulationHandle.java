package Game.Engine.Simulation;

/**
 * Handle de acceso eficiente a los datos de simulación de una entidad.
 *
 * ── HRFC — Game.Engine Unified Simulation Data Architecture / ECS-DOD ─────
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 *
 * SimulationHandle es un intermediario ligero que:
 * 1. Resuelve EntityId → dense array index
 * 2. Valida que la entidad sigue existiendo (generation checking)
 * 3. Permite acceso directo al storage sin búsquedas repetidas
 *
 * ── POR QUÉ EXISTE ───────────────────────────────────────────────────────
 *
 * Problema sin handle:
 *
 *   float x = entityStore.getPositionX(entityId);  // búsqueda en map
 *   float y = entityStore.getPositionY(entityId);  // búsqueda en map (otra vez)
 *   float vx = entityStore.getVelocityX(entityId); // búsqueda en map (otra vez)
 *
 * Tres accesos → tres búsquedas en HashMap.
 *
 * Solución con handle:
 *
 *   SimulationHandle h = entityStore.getHandle(entityId);  // una búsqueda
 *   float x = positionsX[h.index()];  // acceso directo
 *   float y = positionsY[h.index()];  // acceso directo
 *   float vx = velocitiesX[h.index()]; // acceso directo
 *
 * Una búsqueda inicial, luego acceso directo por índice.
 *
 * ── VALIDACIÓN CON GENERATION ────────────────────────────────────────────
 *
 * El handle almacena un "generation counter" que detecta si el índice
 * fue reutilizado para otra entidad:
 *
 *   1. Entidad A se crea en slot 5, generation = 1
 *   2. Handle guarda: index=5, generation=1
 *   3. Entidad A se destruye
 *   4. Slot 5 se reutiliza para entidad B, generation = 2
 *   5. Handle antiguo: index=5, generation=1
 *      EntityStore: slot 5 tiene generation=2
 *      → Handle.isValid() retorna false
 *
 * Esto previene el bug de "use-after-free" donde un handle apunta
 * accidentalmente a una entidad diferente.
 *
 * ── DISEÑO DELIBERADAMENTE SIMPLE ────────────────────────────────────────
 *
 * Este handle NO es un "smart pointer". No tiene ownership.
 * No previene modificaciones concurrentes. No hace reference counting.
 *
 * Es un índice validado. Punto.
 *
 * La responsabilidad de sincronización y ownership es del código que
 * usa el handle, no del handle mismo.
 *
 * ── INVALIDACIÓN ─────────────────────────────────────────────────────────
 *
 * Un handle se vuelve inválido cuando:
 * - La entidad es destruida (generation mismatch)
 * - El índice fue compactado (generation mismatch)
 *
 * Un handle inválido NO debe usarse para acceder a los arrays.
 * Siempre validar con isValid() antes de usar index().
 *
 * ── EJEMPLO ──────────────────────────────────────────────────────────────
 *
 *   class Player {
 *       private final EntityId entityId;
 *       private SimulationHandle cachedHandle;
 *
 *       public void update(EntityStore store, float[] posX, float[] posY) {
 *           // Refrescar handle si es necesario
 *           if (cachedHandle == null || !cachedHandle.isValid()) {
 *               cachedHandle = store.getHandle(entityId);
 *           }
 *
 *           int idx = cachedHandle.index();
 *           // Usar idx para acceder a los arrays
 *           posX[idx] += velocityX * dt;
 *           posY[idx] += velocityY * dt;
 *       }
 *   }
 *
 * ── NO ABUSAR ────────────────────────────────────────────────────────────
 *
 * No cachear handles durante períodos largos. Si una entidad puede
 * ser destruida o compactada entre frames, revalidar el handle.
 *
 * Para acceso puntual, obtener el handle, usarlo, descartarlo.
 * Para acceso repetido en el mismo frame, cachear temporalmente.
 */
public final class SimulationHandle {

    /** Handle inválido — representa acceso a ninguna entidad. */
    public static final SimulationHandle INVALID = new SimulationHandle(-1, 0);

    private final int index;       // índice en los arrays densos
    private final int generation;  // generation counter para validación

    /**
     * Constructor público para EntityStore.
     *
     * @param index índice denso en los arrays de componentes
     * @param generation generation counter para validación
     */
    public SimulationHandle(int index, int generation) {
        this.index = index;
        this.generation = generation;
    }

    /**
     * Retorna el índice denso para acceder a los arrays de componentes.
     *
     * ADVERTENCIA: Solo usar después de validar isValid().
     * Usar un índice de un handle inválido puede acceder a datos
     * de otra entidad o causar IndexOutOfBoundsException.
     *
     * @return índice en los arrays densos
     */
    public int index() {
        return index;
    }

    /**
     * Retorna el generation counter de este handle.
     * Usado por EntityStore para validación.
     */
    public int generation() {
        return generation;
    }

    /**
     * Retorna true si este handle apunta a una entidad válida.
     *
     * Un handle se vuelve inválido cuando:
     * - La entidad fue destruida
     * - El slot fue reutilizado para otra entidad
     * - El storage fue compactado
     *
     * IMPORTANTE: Este método solo valida la estructura del handle.
     * Para validación completa contra el EntityStore actual, usar
     * EntityStore.validateHandle(handle).
     */
    public boolean isValid() {
        return index >= 0;
    }

    @Override
    public String toString() {
        return isValid()
            ? "SimulationHandle[index=" + index + ", gen=" + generation + "]"
            : "SimulationHandle[INVALID]";
    }
}
