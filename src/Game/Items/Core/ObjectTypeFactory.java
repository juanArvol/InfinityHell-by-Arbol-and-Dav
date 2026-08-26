package Game.Items.Core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import Game.Items.Creation.ItemID;

/**
 * Registry genérico para ObjectTypes.
 *
 * ── ARCHITECTURE — Items Module ──────────────────────────────────────────
 *
 * RESPONSABILIDAD ÚNICA:
 *
 *   Registrar y resolver ObjectTypes por familia.
 *
 * No crea instancias.
 * No contiene lógica de loot.
 *
 * Ejemplo:
 *
 *   ObjectTypeFactory.register(
 *       BulletType.class,
 *       meteorType
 *   );
 *
 * Familias:
 *
 *   BulletType
 *   WeaponType
 *   AmuletType
 *   ...
 */
public final class ObjectTypeFactory {

    private static final Map<
            Class<? extends ObjectType<?>>,
            Map<ItemID, ObjectType<?>>
            > FAMILY_REGISTRIES =
            new ConcurrentHashMap<>();

    private ObjectTypeFactory() {
    }

    // ── Registration ──────────────────────────────────────────────────────

    public static <T extends ObjectType<?>> T register(
            Class<T> family,
            T type
    ) {

        validateFamily(family);

        if (type == null) {
            throw new IllegalArgumentException(
                    "type no puede ser null"
            );
        }

        Map<ItemID, ObjectType<?>> registry =
                FAMILY_REGISTRIES.computeIfAbsent(
                        family,
                        key -> new LinkedHashMap<>()
                );

        synchronized (registry) {

            ItemID id = type.getItemId();

            if (registry.containsKey(id)) {
                throw new IllegalStateException(
                        family.getSimpleName() +
                        " duplicado: '" +
                        id +
                        "'"
                );
            }

            registry.put(id, type);
        }

        return type;
    }

    // ── Lookup ─────────────────────────────────────────────────────────────

    public static <T extends ObjectType<?>> T get(
            Class<T> family,
            ItemID id
    ) {

        T type = find(family, id);

        if (type == null) {
            throw new IllegalArgumentException(
                    family.getSimpleName() +
                    " no encontrado: '" +
                    id +
                    "'"
            );
        }

        return type;
    }

    @SuppressWarnings("unchecked")
    public static <T extends ObjectType<?>> T find(
            Class<T> family,
            ItemID id
    ) {

        validateFamily(family);

        if (id == null) {
            return null;
        }

        Map<ItemID, ObjectType<?>> registry =
                FAMILY_REGISTRIES.get(family);

        if (registry == null) {
            return null;
        }

        synchronized (registry) {
            return (T) registry.get(id);
        }
    }

    public static boolean has(
            Class<? extends ObjectType<?>> family,
            ItemID id
    ) {

        validateFamily(family);

        if (id == null) {
            return false;
        }

        Map<ItemID, ObjectType<?>> registry =
                FAMILY_REGISTRIES.get(family);

        if (registry == null) {
            return false;
        }

        synchronized (registry) {
            return registry.containsKey(id);
        }
    }

    // ── Values ─────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public static <T extends ObjectType<?>> Collection<T> values(
            Class<T> family
    ) {

        validateFamily(family);

        Map<ItemID, ObjectType<?>> registry =
                FAMILY_REGISTRIES.get(family);

        if (registry == null) {
            return Collections.emptyList();
        }

        synchronized (registry) {

            List<T> result =
                    new ArrayList<>(
                            registry.size()
                    );

            for (ObjectType<?> type :
                    registry.values()) {

                result.add(
                        (T) type
                );
            }

            return Collections.unmodifiableList(
                    result
            );
        }
    }

    // ── Testing ────────────────────────────────────────────────────────────

    /**
     * Limpia todos los registros.
     *
     * SOLO PARA TESTING.
     */
    public static void clearAll() {
        FAMILY_REGISTRIES.clear();
    }

    // ── Validation ─────────────────────────────────────────────────────────

    private static void validateFamily(
            Class<? extends ObjectType<?>> family
    ) {

        if (family == null) {
            throw new IllegalArgumentException(
                    "family no puede ser null"
            );
        }
    }
}