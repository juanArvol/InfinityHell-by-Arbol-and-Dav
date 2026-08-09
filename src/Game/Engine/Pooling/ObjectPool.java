package Game.Engine.Pooling;

/**
 * Contrato general de un pool de objetos reutilizables.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 *
 * ObjectPool gestiona instancias de tipo T, permitiendo su adquisición y
 * devolución. La decisión de crear una instancia nueva o reutilizar una
 * existente es responsabilidad del pool.
 *
 * ── MOTIVACIÓN ────────────────────────────────────────────────────────────
 *
 * Esta abstracción generaliza el patrón de pooling que estaba implementado
 * exclusivamente en ProjectilePool para Bullets. Ahora cualquier subsistema
 * del Engine o del Game puede implementar su propio pool reutilizando esta
 * infraestructura:
 *
 *   ProjectilePool   → pool de Bullet
 *   (futuro) ParticlePool  → pool de Particle
 *   (futuro) EnemyPool     → pool de Enemy de alta frecuencia
 *
 * ── CONTRATO DE REUTILIZACIÓN ─────────────────────────────────────────────
 *
 * Una instancia devuelta al pool via release() NO debe ser usada externamente
 * hasta que sea adquirida nuevamente via acquire().
 *
 * El pool garantiza que la instancia ha sido reseteada al estado correcto
 * antes de entregarse al caller de acquire().
 *
 * ── THREAD SAFETY ─────────────────────────────────────────────────────────
 *
 * No se garantiza thread-safety a menos que la implementación lo especifique.
 * Para el uso típico de game loop (single-thread), no es necesario.
 *
 * @param <T> tipo de objeto que gestiona este pool
 */
public interface ObjectPool<T> {

    /**
     * Adquiere una instancia del pool, lista para usar.
     *
     * Si hay instancias disponibles, se resetea y reutiliza.
     * Si no hay instancias disponibles, se crea una nueva.
     *
     * La instancia devuelta tiene un estado equivalente a una instancia
     * recién construida — sin estado residual del ciclo anterior.
     *
     * @return instancia lista para usar
     */
    T acquire();

    /**
     * Devuelve una instancia al pool para su reutilización futura.
     *
     * Pre-condición: la instancia fue adquirida de este pool (no externas).
     * Post-condición: la instancia NO debe ser usada externamente hasta el
     * próximo acquire().
     *
     * @param instance la instancia a devolver
     */
    void release(T instance);

    /**
     * Número de instancias actualmente disponibles para reutilización.
     *
     * @return tamaño de la cola de disponibles
     */
    int getPoolSize();

    /**
     * Vacía el pool, liberando todas las instancias disponibles.
     *
     * Llamar al destruir el sistema que owns este pool para liberar
     * referencias y permitir la recolección de basura.
     */
    void clear();
}
