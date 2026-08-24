package Game.Items.Core;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registro unificado de todos los tipos de Item del sistema.
 *
 * ── HRFC — Items Module Architectural Consolidation ──────────────────────
 *
 * RESPONSABILIDAD:
 *   ItemTypeRegistry es la infraestructura central que permite a cada familia
 *   de Items (BulletType, WeaponType, AmuletType) declarar sus tipos mediante
 *   el patrón:
 *
 *     NORMALBULLET = register(new BulletType(...));
 *
 *   sin tener que reimplementar su propio sistema de registro.
 *
 * DISEÑO:
 *   - Registro por familia: cada familia (BulletType.class) tiene su propio
 *     namespace aislado para evitar colisiones de IDs entre familias.
 *
 *   - Thread-safe: usa ConcurrentHashMap para permitir registro desde
 *     múltiples bloques static en orden arbitrario.
 *
 *   - Validación estricta: previene registros duplicados y IDs inválidos.
 *
 * ARQUITECTURA:
 *
 *   ItemTypeRegistry (infraestructura común)
 *         │
 *         ├── BulletType.register()  → ItemTypeRegistry.register(BulletType.class, ...)
 *         ├── WeaponType.register()  → ItemTypeRegistry.register(WeaponType.class, ...)
 *         └── AmuletType.register()  → ItemTypeRegistry.register(AmuletType.class, ...)
 *
 * USO DESDE SUBCLASES:
 *
 *   public final class BulletType extends ItemTypeBase<BulletBehavior> {
 *       private static BulletType register(BulletType type) {
 *           return ItemTypeRegistry.register(BulletType.class, type);
 *       }
 *
 *       static {
 *           NORMALBULLET = register(new BulletType(...));
 *       }
 *   }
 *
 * PRINCIPIO CLAVE:
 *   "Items proporciona la arquitectura. Los Type especializados declaran los tipos."
 *
 * @see ObjectType  clase base para tipos de Item
 */
public final class ItemTypeRegistry {

    /**
     * Almacenamiento por familia.
     * Estructura: Map<Class<? extends ItemTypeBase>, Map<String, ItemTypeBase>>
     *
     * Ejemplo:
     *   familyRegistries.get(BulletType.class).get("normal_bullet") → BulletType instance
     */
    private static final Map<Class<? extends ObjectType<?>>, Map<String, ObjectType<?>>>
            familyRegistries = new ConcurrentHashMap<>();

    // Constructor privado — clase de utilidad estática
    private ItemTypeRegistry() {}

    // ── Registro ──────────────────────────────────────────────────────────

    /**
     * Registra un tipo en su familia correspondiente.
     *
     * VALIDACIÓN:
     *   - Lanza si el ID ya está registrado en la misma familia
     *   - Lanza si el ID está vacío
     *   - Lanza si el type es null
     *
     * @param <T>    tipo concreto del ItemTypeBase
     * @param family clase de la familia (ej: BulletType.class)
     * @param type   instancia del tipo a registrar
     * @return el mismo tipo (para asignación en constantes static)
     * @throws IllegalArgumentException si type es null o ID es inválido
     * @throws IllegalStateException si el ID ya está registrado
     */
    public static <T extends ObjectType<?>> T register(Class<T> family, T type) {
        if (type == null)
            throw new IllegalArgumentException("type no puede ser null");
        if (type.id == null || type.id.isBlank())
            throw new IllegalArgumentException("ID no puede estar vacío");

        Map<String, ObjectType<?>> familyRegistry =
                familyRegistries.computeIfAbsent(family, k -> new LinkedHashMap<>());

        if (familyRegistry.containsKey(type.id)) {
            throw new IllegalStateException(
                    family.getSimpleName() + " duplicado: '" + type.id + "'");
        }

        familyRegistry.put(type.id, type);
        return type;
    }

    // ── Consulta ──────────────────────────────────────────────────────────

    /**
     * Obtiene un tipo por su ID dentro de una familia específica.
     *
     * @param <T>    tipo concreto del ItemTypeBase
     * @param family clase de la familia (ej: BulletType.class)
     * @param id     identificador del tipo
     * @return el tipo correspondiente
     * @throws IllegalArgumentException si el tipo no existe
     */
    @SuppressWarnings("unchecked")
    public static <T extends ObjectType<?>> T get(Class<T> family, String id) {
        T type = find(family, id);
        if (type == null) {
            throw new IllegalArgumentException(
                    family.getSimpleName() + " no encontrado: '" + id + "'");
        }
        return type;
    }

    /**
     * Busca un tipo por su ID sin lanzar excepción.
     *
     * @param <T>    tipo concreto del ItemTypeBase
     * @param family clase de la familia
     * @param id     identificador del tipo
     * @return el tipo correspondiente, o null si no existe
     */
    @SuppressWarnings("unchecked")
    public static <T extends ObjectType<?>> T find(Class<T> family, String id) {
        Map<String, ObjectType<?>> familyRegistry = familyRegistries.get(family);
        if (familyRegistry == null) return null;
        return (T) familyRegistry.get(id);
    }

    /**
     * Verifica si existe un tipo con el ID dado en una familia.
     *
     * @param family clase de la familia
     * @param id     identificador del tipo
     * @return true si existe, false en caso contrario
     */
    public static boolean has(Class<? extends ObjectType<?>> family, String id) {
        Map<String, ObjectType<?>> familyRegistry = familyRegistries.get(family);
        return familyRegistry != null && familyRegistry.containsKey(id);
    }

    /**
     * Retorna todos los tipos registrados de una familia.
     *
     * ORDEN: los tipos se retornan en orden de registro (LinkedHashMap).
     *
     * @param <T>    tipo concreto del ItemTypeBase
     * @param family clase de la familia
     * @return colección inmutable de todos los tipos registrados
     */
    @SuppressWarnings("unchecked")
    public static <T extends ObjectType<?>> Collection<T> values(Class<T> family) {
        Map<String, ObjectType<?>> familyRegistry = familyRegistries.get(family);
        if (familyRegistry == null) return Collections.emptyList();

        List<T> result = new ArrayList<>();
        for (ObjectType<?> type : familyRegistry.values()) {
            result.add((T) type);
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Retorna el número de tipos registrados en una familia.
     *
     * @param family clase de la familia
     * @return cantidad de tipos registrados
     */
    public static int count(Class<? extends ObjectType<?>> family) {
        Map<String, ObjectType<?>> familyRegistry = familyRegistries.get(family);
        return familyRegistry != null ? familyRegistry.size() : 0;
    }

    // ── Introspección (para debugging/testing) ────────────────────────────

    /**
     * Retorna todas las familias registradas.
     * Útil para debugging y herramientas de desarrollo.
     *
     * @return conjunto inmutable de clases de familia
     */
    public static Set<Class<? extends ObjectType<?>>> registeredFamilies() {
        return Collections.unmodifiableSet(familyRegistries.keySet());
    }

    /**
     * Limpia todos los registros.
     *
     * ⚠️ SOLO PARA TESTING — NO usar en código de producción.
     * Los bloques static se ejecutan una sola vez; limpiar el registry
     * sin reiniciar la JVM dejará el sistema en estado inconsistente.
     */
    public static void clearAll() {
        familyRegistries.clear();
    }

    /**
     * Limpia el registro de una familia específica.
     *
     * ⚠️ SOLO PARA TESTING — NO usar en código de producción.
     */
    public static void clearFamily(Class<? extends ObjectType<?>> family) {
        familyRegistries.remove(family);
    }
}
