package Game.Engine.Resources;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Cache genérico de recursos inmutables compartidos.
 *
 * ── MOTIVACIÓN ────────────────────────────────────────────────────────────
 *
 * Generaliza el patrón que estaba implementado exclusivamente en
 * BulletFlyweightCache para proyectiles. Ahora cualquier subsistema puede
 * compartir recursos inmutables sin crear un cache dedicado por tipo:
 *
 *   BulletFlyweightCache   → usa ResourceCache<FlyweightKey, BulletFlyweight>
 *   (futuro) TextureCache  → ResourceCache<String, BufferedImage>
 *   (futuro) SoundCache    → ResourceCache<String, AudioClip>
 *   (futuro) CollisionProfileCache → ResourceCache<ProfileKey, CollisionProfile>
 *
 * ── PATRÓN FLYWEIGHT ──────────────────────────────────────────────────────
 *
 * Por cada clave K única, existe exactamente UN valor V en el cache.
 * La primera solicitud de una clave resuelve y almacena el recurso.
 * Las solicitudes posteriores con la misma clave retornan la instancia
 * ya almacenada — sin resolver ni duplicar.
 *
 * Esto garantiza que N entidades del mismo tipo compartan los mismos
 * recursos inmutables en memoria, independientemente de cuántas instancias
 * existan activas.
 *
 * ── INMUTABILIDAD DE LOS VALORES ──────────────────────────────────────────
 *
 * Los valores almacenados DEBEN ser inmutables o tratarse como read-only.
 * ResourceCache no tiene mecanismo para invalidar o modificar un valor
 * ya almacenado. Si un recurso puede cambiar, usar otro patrón.
 *
 * ── THREAD SAFETY ─────────────────────────────────────────────────────────
 *
 * Implementado con ConcurrentHashMap + computeIfAbsent, seguro para acceso
 * desde múltiples threads aunque el game loop sea single-thread.
 *
 * ── CICLO DE VIDA ─────────────────────────────────────────────────────────
 *
 * clear() vacía el cache. Útil al recargar assets o entre tests.
 * En producción normal no es necesario llamar clear() — los recursos
 * son válidos durante toda la sesión del juego.
 *
 * @param <K> tipo de clave que identifica un recurso (debe implementar equals/hashCode)
 * @param <V> tipo del recurso almacenado (debe ser inmutable o tratarse como read-only)
 */
public final class ResourceCache<K, V> {

    private final Map<K, V> cache = new ConcurrentHashMap<>();

    /**
     * Obtiene el recurso para la clave dada, creándolo si no existe.
     *
     * Si ya existe un recurso para esta clave, lo retorna directamente.
     * Si no existe, invoca el resolver para crearlo, lo almacena y lo retorna.
     *
     * El resolver se invoca como máximo UNA VEZ por clave.
     *
     * @param key      clave que identifica el recurso
     * @param resolver función que crea el recurso cuando no está en cache
     * @return recurso inmutable asociado a la clave
     */
    public V getOrCreate(K key, Function<K, V> resolver) {
        return cache.computeIfAbsent(key, resolver);
    }

    /**
     * Retorna el recurso para la clave dada, o null si no está en cache.
     *
     * A diferencia de getOrCreate(), NO crea el recurso si no existe.
     * Útil para comprobar si un recurso ya fue cargado.
     *
     * @param key clave a consultar
     * @return recurso asociado, o null si no está en cache
     */
    public V get(K key) {
        return cache.get(key);
    }

    /**
     * Almacena explícitamente un recurso para una clave.
     *
     * Útil cuando el recurso se crea externamente (tests, precarga).
     * Si ya existía un recurso para esa clave, lo sobreescribe.
     *
     * @param key   clave del recurso
     * @param value recurso a almacenar
     */
    public void put(K key, V value) {
        cache.put(key, value);
    }

    /**
     * Comprueba si hay un recurso almacenado para la clave dada.
     *
     * @param key clave a comprobar
     * @return true si el recurso ya está en cache
     */
    public boolean contains(K key) {
        return cache.containsKey(key);
    }

    /**
     * Número de recursos distintos almacenados en el cache.
     *
     * @return tamaño del cache
     */
    public int size() {
        return cache.size();
    }

    /**
     * Vacía el cache.
     *
     * Llamar si los assets cambian (recarga en caliente, tests).
     * En uso normal de producción no es necesario.
     */
    public void clear() {
        cache.clear();
    }
}
