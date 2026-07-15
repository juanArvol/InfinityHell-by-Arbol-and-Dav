package Sprites.Core;

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AssetRegistry — registro centralizado de SpriteHandles.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * Almacena y sirve SpriteHandles por ID. Es el único punto de acceso para
 * el Gameplay cuando necesita un recurso visual.
 *
 * El Gameplay nunca accede a AssetLoader ni a paths de archivos directamente.
 * Solo pide: AssetRegistry.get("player.idle").
 *
 * ── CICLO DE VIDA ─────────────────────────────────────────────────────────
 * 1. Assets.init() → los managers de assets registran SpriteHandles aquí.
 * 2. Gameplay → AssetRegistry.get(id) → SpriteHandle → RenderEngine.
 *
 * ── SINGLETON ─────────────────────────────────────────────────────────────
 * Una instancia global accesible vía AssetRegistry.getInstance().
 * No es un God Object: su única responsabilidad es el registro de handles.
 *
 * ── HANDLES FALTANTES ─────────────────────────────────────────────────────
 * get() nunca devuelve null. Si el ID no existe, devuelve SpriteHandle.EMPTY
 * y loguea un warning. El juego sigue funcionando.
 *
 * ── USO ───────────────────────────────────────────────────────────────────
 *
 *   // Registro (en Assets.init()):
 *   AssetRegistry.getInstance().register("player.idle", handle);
 *
 *   // Consulta (en Gameplay):
 *   SpriteHandle idle = AssetRegistry.getInstance().get("player.idle");
 *
 *   // Atajo estático para el Gameplay:
 *   SpriteHandle idle = AssetRegistry.get("player.idle");
 */
public final class AssetRegistry {

    // ── Singleton ─────────────────────────────────────────────────────────

    private static final AssetRegistry INSTANCE = new AssetRegistry();

    public static AssetRegistry getInstance() { return INSTANCE; }

    private AssetRegistry() {}

    // ── Estado ────────────────────────────────────────────────────────────

    /** Mapa ID → SpriteHandle. LinkedHashMap para orden de inserción. */
    private final Map<String, SpriteHandle> handles = new LinkedHashMap<>();

    // ── Registro ──────────────────────────────────────────────────────────

    /**
     * Registra un SpriteHandle con el ID dado.
     * Si ya existe un handle con ese ID, lo sobreescribe (útil para hot-reload).
     *
     * @param id     identificador lógico (ej: "player.idle")
     * @param handle handle a registrar
     */
    public void register(String id, SpriteHandle handle) {
        if (id == null || id.isBlank()) {
            System.err.println("[AssetRegistry] Intentando registrar handle con ID vacío");
            return;
        }
        if (handle == null) {
            System.err.println("[AssetRegistry] Intentando registrar handle null para ID: " + id);
            return;
        }
        handles.put(id, handle);
    }

    /**
     * Atajo de registro: crea el SpriteHandle internamente y lo registra.
     *
     * @param id         identificador lógico
     * @param definition definición del sprite
     */
    public void register(String id, SpriteDefinition definition) {
        register(id, new SpriteHandle(definition, id));
    }

    // ── Consulta ──────────────────────────────────────────────────────────

    /**
     * Obtiene un SpriteHandle por ID.
     * Nunca devuelve null — retorna SpriteHandle.EMPTY si el ID no existe.
     *
     * @param id identificador lógico
     * @return SpriteHandle registrado, o SpriteHandle.EMPTY
     */
    public SpriteHandle get(String id) {
        SpriteHandle handle = handles.get(id);
        if (handle == null) {
            System.err.println("[AssetRegistry] Handle no encontrado: '" + id + "'. Devolviendo EMPTY.");
            return SpriteHandle.EMPTY;
        }
        return handle;
    }

    /**
     * Atajo estático para el Gameplay. Equivalente a getInstance().get(id).
     * Reduce el boilerplate en los managers de assets.
     */
    public static SpriteHandle find(String id) {
        return INSTANCE.get(id);
    }

    /** true si existe un handle registrado con ese ID. */
    public boolean has(String id) {
        return handles.containsKey(id);
    }

    /**
     * Obtiene un SpriteHandle por ID o devuelve un fallback.
     * Útil cuando un ID puede no existir y se tiene alternativa.
     */
    public SpriteHandle getOrDefault(String id, SpriteHandle fallback) {
        SpriteHandle handle = handles.get(id);
        return handle != null ? handle : fallback;
    }

    // ── Utilidades ────────────────────────────────────────────────────────

    /**
     * Resuelve directamente la BufferedImage del frame por defecto de un handle.
     *
     * Uso: código legado o constructores que todavía necesiten BufferedImage
     * durante la migración. El Gameplay NO debería llamar esto directamente.
     *
     * @param id identificador del handle
     * @return BufferedImage del frame por defecto, o null si no hay imagen
     */
    public BufferedImage resolveImage(String id) {
        SpriteFrame frame = get(id).resolveDefault();
        return frame.getImage();
    }

    /** Limpia todos los handles registrados. Uso: tests. */
    public void clear() {
        handles.clear();
    }

    /** Cantidad de handles registrados. */
    public int size() { return handles.size(); }

    /** Mapa de handles (inmutable, para diagnóstico). */
    public Map<String, SpriteHandle> getAll() {
        return Collections.unmodifiableMap(handles);
    }

    @Override
    public String toString() {
        return "AssetRegistry[" + handles.size() + " handles]";
    }
}
