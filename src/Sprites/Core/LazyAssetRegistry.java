package Sprites.Core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * LazyAssetRegistry — registro de assets con carga diferida (lazy loading).
 *
 * ── OPTIMIZACIÓN: CARGA BAJO DEMANDA ──────────────────────────────────────
 * A diferencia de AssetRegistry que carga todos los sprites al inicio,
 * LazyAssetRegistry permite registrar proveedores (Supplier) que solo
 * cargan el asset cuando se accede por primera vez.
 *
 * ── CUÁNDO USAR ───────────────────────────────────────────────────────────
 * - Assets de bosses que solo aparecen en niveles específicos
 * - Assets de menús que no se usan durante el gameplay
 * - Assets de cutscenes o eventos especiales
 * - Assets grandes que no se necesitan inmediatamente
 *
 * ── EJEMPLO DE USO ────────────────────────────────────────────────────────
 *
 *   // En lugar de cargar todo al inicio:
 *   LazyAssetRegistry.register("boss.sans", () -> {
 *       // Este código solo se ejecuta cuando alguien llame get("boss.sans")
 *       var sheet = AssetLoader.loadSheet(...);
 *       return new SpriteHandle(...);
 *   });
 *
 *   // Primera vez: carga y cachea
 *   SpriteHandle sans = LazyAssetRegistry.get("boss.sans");
 *
 *   // Segunda vez: retorna cacheado
 *   SpriteHandle sans2 = LazyAssetRegistry.get("boss.sans");
 *
 * ── BENEFICIOS ─────────────────────────────────────────────────────────────
 * 1. Tiempo de carga inicial más rápido
 * 2. Menor uso de memoria si no se acceden todos los assets
 * 3. Posibilidad de descargar assets no usados
 * 4. Compatible con AssetRegistry (misma interfaz de get())
 *
 * ── INTEGRACIÓN ───────────────────────────────────────────────────────────
 * LazyAssetRegistry es opcional y complementa a AssetRegistry.
 * Assets críticos (jugador, balas básicas) siguen en AssetRegistry.
 * Assets opcionales (bosses, efectos especiales) van en LazyAssetRegistry.
 */
public final class LazyAssetRegistry {

    // ── Singleton ─────────────────────────────────────────────────────────

    private static final LazyAssetRegistry INSTANCE = new LazyAssetRegistry();

    public static LazyAssetRegistry getInstance() { return INSTANCE; }

    private LazyAssetRegistry() {}

    // ── Estado ────────────────────────────────────────────────────────────

    /** Proveedores de assets que aún no se han cargado. */
    private final Map<String, Supplier<SpriteHandle>> providers = new LinkedHashMap<>();

    /** Assets que ya fueron cargados (caché). */
    private final Map<String, SpriteHandle> loaded = new LinkedHashMap<>();

    // ── Registro ──────────────────────────────────────────────────────────

    /**
     * Registra un proveedor de asset que se ejecutará solo cuando se necesite.
     *
     * @param id       identificador del asset (ej: "boss.sans")
     * @param provider función que carga y retorna el SpriteHandle
     */
    public void register(String id, Supplier<SpriteHandle> provider) {
        if (id == null || id.isBlank()) {
            System.err.println("[LazyAssetRegistry] ID vacío ignorado");
            return;
        }
        if (provider == null) {
            System.err.println("[LazyAssetRegistry] Provider null para ID: " + id);
            return;
        }
        
        // Si ya estaba cargado, limpiar la caché para forzar recarga
        loaded.remove(id);
        providers.put(id, provider);
    }

    // ── Consulta ──────────────────────────────────────────────────────────

    /**
     * Obtiene un asset. Si no está cargado, lo carga ahora.
     * Si no existe el provider, retorna SpriteHandle.EMPTY.
     *
     * @param id identificador del asset
     * @return SpriteHandle cargado o EMPTY
     */
    public SpriteHandle get(String id) {
        // ¿Ya está en caché?
        SpriteHandle cached = loaded.get(id);
        if (cached != null) return cached;

        // ¿Existe el provider?
        Supplier<SpriteHandle> provider = providers.get(id);
        if (provider == null) {
            System.err.println("[LazyAssetRegistry] Asset no registrado: '" + id + "'");
            return SpriteHandle.EMPTY;
        }

        // Cargar ahora
        try {
            SpriteHandle handle = provider.get();
            if (handle == null) {
                System.err.println("[LazyAssetRegistry] Provider retornó null para: '" + id + "'");
                handle = SpriteHandle.EMPTY;
            }
            loaded.put(id, handle);
            System.out.println("[LazyAssetRegistry] Cargado bajo demanda: " + id);
            return handle;
            
        } catch (Exception e) {
            System.err.println("[LazyAssetRegistry] Error cargando '" + id + "': " + e.getMessage());
            e.printStackTrace();
            return SpriteHandle.EMPTY;
        }
    }

    /**
     * Atajo estático para acceso directo (igual que AssetRegistry).
     */
    public static SpriteHandle find(String id) {
        return INSTANCE.get(id);
    }

    // ── Gestión de caché ──────────────────────────────────────────────────

    /**
     * Pre-carga un asset sin esperar a que sea solicitado.
     * Útil para cargar assets en paralelo o durante pantallas de carga.
     */
    public void preload(String id) {
        get(id); // Simplemente acceder lo carga
    }

    /**
     * Pre-carga múltiples assets.
     */
    public void preloadAll(String... ids) {
        for (String id : ids) {
            preload(id);
        }
    }

    /**
     * Libera un asset específico de la memoria.
     * El provider queda registrado, puede volver a cargarse.
     *
     * @param id identificador del asset a descargar
     * @return true si se descargó algo
     */
    public boolean unload(String id) {
        return loaded.remove(id) != null;
    }

    /**
     * Libera todos los assets cargados pero mantiene los providers.
     * Los assets pueden recargarse si se solicitan de nuevo.
     */
    public void unloadAll() {
        int count = loaded.size();
        loaded.clear();
        if (count > 0) {
            System.out.println("[LazyAssetRegistry] " + count + " assets descargados");
        }
    }

    /**
     * Limpia completamente el registro (providers y cargados).
     * Uso: tests o reinicio completo del sistema.
     */
    public void clear() {
        providers.clear();
        loaded.clear();
    }

    // ── Estadísticas ──────────────────────────────────────────────────────

    /** Cantidad de providers registrados. */
    public int getProviderCount() { return providers.size(); }

    /** Cantidad de assets ya cargados. */
    public int getLoadedCount() { return loaded.size(); }

    /** true si el asset está cargado en memoria. */
    public boolean isLoaded(String id) { return loaded.containsKey(id); }

    /** true si existe un provider para este ID. */
    public boolean isRegistered(String id) { return providers.containsKey(id); }

    /** Map de todos los assets cargados (inmutable, para diagnóstico). */
    public Map<String, SpriteHandle> getAllLoaded() {
        return Collections.unmodifiableMap(loaded);
    }

    /**
     * Estadísticas de uso de memoria.
     */
    public String getStats() {
        return String.format("LazyAssetRegistry[providers=%d, loaded=%d, pending=%d]",
            providers.size(), loaded.size(), providers.size() - loaded.size());
    }

    @Override
    public String toString() {
        return getStats();
    }
}
