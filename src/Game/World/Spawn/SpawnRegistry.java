package Game.World.Spawn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registro de SpawnRequests activos.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * SpawnRegistry mantiene la colección de SpawnRequests registrados y
 * permite acceder a ellos por ID. SpawnSystem los evalúa cada tick.
 *
 * ── CICLO DE VIDA ─────────────────────────────────────────────────────────
 * Los requests completados (oneTime ejecutado, o cancelados) se eliminan
 * automáticamente en el siguiente tick de limpieza.
 *
 * ── THREAD SAFETY ─────────────────────────────────────────────────────────
 * No thread-safe por diseño. Todas las operaciones deben ocurrir en el
 * game loop thread. Si en el futuro se necesita acceso multi-thread,
 * añadir sincronización explícita en SpawnSystem.
 */
public final class SpawnRegistry {

    /** Requests indexados por el ID del descriptor para acceso rápido. */
    private final Map<String, SpawnRequest> byId = new LinkedHashMap<>();

    /** Lista de iteración — copia viva sincronizada con byId. */
    private final List<SpawnRequest> all  = new ArrayList<>();

    // ── Registro ──────────────────────────────────────────────────────────

    /**
     * Registra un SpawnRequest.
     *
     * Si ya existe un request con el mismo descriptor ID, lanza excepción.
     * Para reemplazar, llamar unregister() primero.
     *
     * @param request request a registrar.
     * @throws IllegalStateException si el ID del descriptor ya está registrado.
     */
    public void register(SpawnRequest request) {
        String id = request.getDescriptor().getId();
        if (byId.containsKey(id)) {
            throw new IllegalStateException(
                "SpawnRequest duplicado para descriptor ID: '" + id + "'. " +
                "Llamar unregister() antes de registrar de nuevo."
            );
        }
        byId.put(id, request);
        all.add(request);
    }

    /**
     * Elimina el request con el ID de descriptor indicado.
     *
     * @param descriptorId ID del SpawnDescriptor del request a eliminar.
     * @return true si existía y fue eliminado.
     */
    public boolean unregister(String descriptorId) {
        SpawnRequest removed = byId.remove(descriptorId);
        if (removed != null) {
            all.remove(removed);
            return true;
        }
        return false;
    }

    /**
     * Obtiene el SpawnRequest activo con el ID dado.
     *
     * @param descriptorId ID del descriptor.
     * @return el request, o null si no está registrado.
     */
    public SpawnRequest get(String descriptorId) {
        return byId.get(descriptorId);
    }

    /** True si hay un request registrado con ese ID. */
    public boolean has(String descriptorId) {
        return byId.containsKey(descriptorId);
    }

    /**
     * Elimina todos los requests completados o cancelados.
     * SpawnSystem llama esto al final de cada tick.
     */
    public void purgeCompleted() {
        all.removeIf(r -> {
            if (r.isCompleted()) {
                byId.remove(r.getDescriptor().getId());
                return true;
            }
            return false;
        });
    }

    /**
     * Vista no modificable de todos los requests activos.
     * Solo para lectura en SpawnSystem — no modificar la lista retornada.
     */
    public List<SpawnRequest> getAll() {
        return Collections.unmodifiableList(all);
    }

    /** Limpia todos los requests. Llamar al cambiar de mundo/escena. */
    public void clear() {
        byId.clear();
        all.clear();
    }

    /** Cantidad de requests registrados. */
    public int size() { return all.size(); }
}
