package Game.Items.Creation;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry base para ItemDefinitions.
 *
 * ── ARQUITECTURA — Items Module ──────────────────────────────────────────
 *
 * PROPÓSITO:
 * Proporcionar la infraestructura común para los registries
 * especializados de cada familia de ItemDefinition.
 *
 * Cada registry concreto define el tipo de ItemDefinition que administra
 * mediante el parámetro genérico T.
 *
 * Ejemplo:
 *
 *     WeaponRegistry extends ItemRegistry<WeaponDefinition>
 *     BulletRegistry extends ItemRegistry<BulletDefinition>
 *     AmuletRegistry extends ItemRegistry<AmuletDefinition>
 *
 * ── IDENTIDAD ────────────────────────────────────────────────────────────
 *
 * ItemID constituye la identidad de una ItemDefinition.
 *
 * El registry utiliza ItemID directamente como clave del mapa.
 * No existen accesos alternativos mediante String.
 *
 * ── RESPONSABILIDADES ────────────────────────────────────────────────────
 *
 * - Registrar definiciones.
 * - Evitar IDs duplicados.
 * - Obtener definiciones mediante ItemID.
 * - Buscar definiciones mediante ItemID.
 * - Comprobar existencia mediante ItemID.
 * - Exponer todas las definiciones registradas.
 * - Crear ItemStacks a partir de ItemID.
 *
 * ── NO RESPONSABILIDADES ────────────────────────────────────────────────
 *
 * - No es Singleton por sí mismo.
 * - No conoce familias concretas de items.
 * - No mantiene un catálogo global de todas las familias.
 * - No realiza conversiones entre String e ItemID.
 *
 * El ciclo de vida de cada registry concreto queda definido
 * por su propia implementación.
 *
 * @param <T> tipo concreto de ItemDefinition administrado por este registry
 */
public abstract class ItemRegistry<T extends ItemDefinition> {

    private final Map<ItemID, T> definitions = new HashMap<>();

    protected ItemRegistry() {}

    /**
     * Registra una definición.
     *
     * La definición y su ItemID deben ser válidos.
     * El ItemID debe ser único dentro de este registry.
     *
     * @param definition definición a registrar
     * @return la misma definición registrada
     *
     * @throws IllegalArgumentException si definition es null
     * @throws NullPointerException si el ItemID es null
     * @throws IllegalStateException si el ItemID ya está registrado
     */
    public T register(T definition) {

        if (definition == null) {
            throw new IllegalArgumentException(
                    "definition no puede ser null"
            );
        }

        ItemID id = definition.getItemId();

        if (definitions.containsKey(id)) {
            throw new IllegalStateException(
                    "ItemDefinition duplicada: '" +
                    id.asString() +
                    "'"
            );
        }

        definitions.put(id, definition);

        return definition;
    }

    /**
     * Obtiene una definición registrada mediante su ItemID.
     *
     * @param id identificador de la definición
     * @return definición registrada
     *
     * @throws IllegalArgumentException si id es null o no está registrado
     */
    public T get(ItemID id) {

        if (id == null) {
            throw new IllegalArgumentException(
                    "id no puede ser null"
            );
        }

        T definition = definitions.get(id);

        if (definition == null) {
            throw new IllegalArgumentException(
                    "ItemDefinition no encontrada: '" +
                    id.asString() +
                    "'"
            );
        }

        return definition;
    }

    /**
     * Busca una definición registrada mediante su ItemID.
     *
     * No lanza excepción si la definición no existe.
     *
     * @param id identificador de la definición
     * @return definición registrada o null si no existe
     */
    public T find(ItemID id) {

        if (id == null) {
            return null;
        }

        return definitions.get(id);
    }

    /**
     * Comprueba si existe una definición registrada.
     *
     * @param id identificador de la definición
     * @return true si existe, false en caso contrario
     */
    public boolean has(ItemID id) {

        if (id == null) {
            return false;
        }

        return definitions.containsKey(id);
    }

    /**
     * Devuelve todas las definiciones registradas.
     *
     * La colección devuelta es de solo lectura.
     */
    public Collection<T> getAll() {
        return Collections.unmodifiableCollection(
                definitions.values()
        );
    }
}