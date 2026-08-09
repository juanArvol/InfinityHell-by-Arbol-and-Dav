package Game.Engine.Pooling;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Implementación base de un pool de objetos reutilizables.
 *
 * ── ESTRUCTURA ────────────────────────────────────────────────────────────
 *
 * AbstractObjectPool<T> implementa la infraestructura compartida de pooling:
 *
 *   - Cola de instancias disponibles (Deque sin tamaño fijo).
 *   - Estadísticas de uso: acquires, hits, misses.
 *   - Protocolo acquire/release con separación de creación y reset.
 *
 * Las subclases solo deben implementar:
 *
 *   createInstance()  — cómo crear una instancia nueva cuando el pool está vacío.
 *   resetInstance(T)  — cómo resetear una instancia existente antes de reutilizarla.
 *   canReuse(T)       — si una instancia disponible es compatible con la solicitud.
 *                       Por defecto: siempre compatible.
 *
 * ── POOL DINÁMICO SIN TAMAÑO FIJO ─────────────────────────────────────────
 *
 * El pool crece bajo demanda: cuando la demanda supera las instancias
 * disponibles, createInstance() genera nuevas. Esas instancias quedan en el
 * pool tras el release y son reutilizadas en ciclos siguientes.
 *
 * El número máximo de instancias activas es el pico de demanda concurrente
 * que el sistema haya necesitado. El pool alcanza su estado estable
 * naturalmente.
 *
 * ── SUBCLASES TÍPICAS ─────────────────────────────────────────────────────
 *
 *   // Pool simple donde todas las instancias son equivalentes:
 *   public class ParticlePool extends AbstractObjectPool<Particle> {
 *       protected Particle createInstance() { return new Particle(); }
 *       protected void resetInstance(Particle p) { p.reset(); }
 *   }
 *
 *   // Pool con compatibilidad de tipo (ProjectilePool):
 *   public class ProjectilePool extends AbstractObjectPool<Bullet> {
 *       public Bullet acquire(ProjectileBlueprint blueprint, ...) { ... }
 *       protected boolean canReuse(Bullet b) { ... }
 *   }
 *
 * @param <T> tipo de objeto gestionado por este pool
 */
public abstract class AbstractObjectPool<T> implements ObjectPool<T> {

    /**
     * Cola de instancias disponibles para reutilización.
     *
     * Deque sin límite superior. addFirst() al liberar, pollFirst() al adquirir.
     * No hay fair-ordering: el pool prioriza reutilización LIFO (instancias
     * recientes primero) para mejorar la localidad de cache.
     */
    private final Deque<T> available = new ArrayDeque<>();

    // ── Estadísticas ──────────────────────────────────────────────────────

    /**
     * Contadores de uso del pool.
     *
     * Son private pero accesibles para subclases a través de los métodos
     * recordAcquireHit() y recordAcquireMiss(). Esto permite que subclases
     * que sobrescriban acquire() con una firma diferente (como ProjectilePool)
     * actualicen los mismos contadores del base, eliminando la necesidad de
     * contadores locales duplicados.
     */
    private int totalAcquires = 0;
    private int poolHits      = 0;
    private int poolMisses    = 0;

    /**
     * Registra un acquire resuelto reutilizando una instancia existente (hit).
     * Llamar desde acquire() de subclases cuando findReusable() tiene éxito.
     */
    protected final void recordAcquireHit() {
        totalAcquires++;
        poolHits++;
    }

    /**
     * Registra un acquire que requirió crear una instancia nueva (miss).
     * Llamar desde acquire() de subclases cuando findReusable() retorna null.
     */
    protected final void recordAcquireMiss() {
        totalAcquires++;
        poolMisses++;
    }

    /**
     * Adquiere una instancia, reutilizando una disponible si es posible.
     *
     * Busca la primera instancia disponible que canReuse() acepte.
     * Si no hay ninguna, crea una nueva con createInstance().
     *
     * El estado de la instancia devuelta es equivalente a una recién creada:
     * resetInstance() garantiza que no hay estado residual del ciclo anterior.
     */
    @Override
    public T acquire() {
        totalAcquires++;

        T instance = findReusable();
        if (instance != null) {
            poolHits++;
            resetInstance(instance);
        } else {
            poolMisses++;
            instance = createInstance();
        }

        return instance;
    }

    /**
     * Devuelve una instancia al pool.
     *
     * La instancia se añade al frente de la cola para reutilización LIFO.
     * Pre-condición: la instancia fue adquirida de este pool.
     */
    @Override
    public void release(T instance) {
        if (instance != null) {
            available.addFirst(instance);
        }
    }

    @Override
    public int getPoolSize() {
        return available.size();
    }

    @Override
    public void clear() {
        available.clear();
        totalAcquires = 0;
        poolHits      = 0;
        poolMisses    = 0;
    }

    // ── API para subclases ────────────────────────────────────────────────

    /**
     * Crea una nueva instancia del tipo T.
     *
     * Llamado cuando no hay instancias disponibles en el pool.
     * La instancia debe estar en un estado inicial correcto.
     *
     * @return nueva instancia lista para usar
     */
    protected abstract T createInstance();

    /**
     * Resetea una instancia existente al estado inicial para reutilización.
     *
     * Llamado justo antes de entregar una instancia reutilizada al caller.
     * Debe garantizar que la instancia no tiene estado residual del ciclo anterior.
     *
     * @param instance instancia a resetear (no null)
     */
    protected abstract void resetInstance(T instance);

    /**
     * Determina si una instancia disponible puede reutilizarse para la
     * solicitud actual.
     *
     * ── USO AVANZADO ─────────────────────────────────────────────────────
     *
     * Las subclases que gestionan instancias de subtipos (como ProjectilePool,
     * que tiene Bullets con diferentes behaviors/movements) pueden sobreescribir
     * este método para filtrar compatibilidad.
     *
     * La implementación por defecto acepta siempre → todas las instancias son
     * equivalentes (caso más común: ParticlePool, SimpleEnemyPool, etc.).
     *
     * @param instance instancia candidata del pool
     * @return true si esta instancia es aceptable para la solicitud actual
     */
    protected boolean canReuse(T instance) {
        return true;
    }

    // ── Búsqueda de reutilizable ──────────────────────────────────────────

    /**
     * Busca y extrae la primera instancia disponible que canReuse() acepte.
     *
     * Complejidad O(n) sobre las disponibles — aceptable cuando n es pequeño.
     * En práctica, para pools donde canReuse() siempre es true, la búsqueda
     * es O(1) (primera instancia siempre aceptable).
     *
     * @return instancia compatible, o null si no hay ninguna disponible
     */
    protected T findReusable() {
        var it = available.iterator();
        while (it.hasNext()) {
            T candidate = it.next();
            if (canReuse(candidate)) {
                it.remove();
                return candidate;
            }
        }
        return null;
    }

    /**
     * Acceso de solo lectura a la cola de disponibles.
     * Útil para que subclases implementen búsquedas personalizadas
     * sin reimplementar el manejo de la cola.
     *
     * @return cola de instancias disponibles (no modificar externamente)
     */
    protected Deque<T> getAvailable() {
        return available;
    }

    // ── Estadísticas de uso ───────────────────────────────────────────────

    /** @return número total de llamadas a acquire() desde la creación del pool */
    public int getTotalAcquires() { return totalAcquires; }

    /** @return número de acquire() resueltos reutilizando una instancia */
    public int getPoolHits()      { return poolHits; }

    /** @return número de acquire() que requirieron crear una instancia nueva */
    public int getPoolMisses()    { return poolMisses; }

    /** @return tasa de reutilización en [0.0, 1.0] */
    public double getHitRate() {
        return totalAcquires == 0 ? 0.0 : (double) poolHits / totalAcquires;
    }
}
