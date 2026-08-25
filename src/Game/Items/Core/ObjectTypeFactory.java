package Game.Items.Core;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory y registro para ObjectTypes — lógica de creación y gestión.
 *
 * ── ARQUITECTURA FINAL — Items Module ────────────────────────────────────
 *
 * SEPARACIÓN:
 *   ObjectType        → Contenedor (almacena ItemDefinition + factory)
 *   ObjectTypeFactory → Lógica de creación y registro
 *
 * RESPONSABILIDAD:
 *   - Registro de tipos por familia (thread-safe)
 *   - Consulta de tipos registrados
 *   - Construcción de pools de oferta ponderados por rareza
 *
 * PATRÓN DE USO:
 *
 *   // Desde BulletType:
 *   private static BulletType register(BulletType type) {
 *       return ObjectTypeFactory.register(BulletType.class, type);
 *   }
 *
 * EXTENSIBILIDAD:
 *   Centraliza la lógica de registro para todas las familias de Items.
 *   Nuevas familias solo necesitan llamar register() con su Class token.
 */
public final class ObjectTypeFactory {

    /**
     * Almacenamiento por familia.
     * Estructura: Map<Class<? extends ObjectType>, Map<String, ObjectType>>
     */
    private static final Map<Class<? extends ObjectType<?>>, Map<String, ObjectType<?>>>
            familyRegistries = new ConcurrentHashMap<>();

    // Constructor privado — clase de utilidad estática
    private ObjectTypeFactory() {}

    // ── Registro ──────────────────────────────────────────────────────────

    /**
     * Registra un tipo en su familia correspondiente.
     *
     * VALIDACIÓN:
     *   - Lanza si el ID ya está registrado en la misma familia
     *   - Lanza si el type es null
     *
     * @param <T>    tipo concreto del ObjectType
     * @param family clase de la familia (ej: BulletType.class)
     * @param type   instancia del tipo a registrar
     * @return el mismo tipo (para asignación en constantes static)
     * @throws IllegalArgumentException si type es null
     * @throws IllegalStateException si el ID ya está registrado
     */
    public static <T extends ObjectType<?>> T register(Class<T> family, T type) {
        if (type == null)
            throw new IllegalArgumentException("type no puede ser null");

        String id = type.getItemId().asString();
        Map<String, ObjectType<?>> familyRegistry =
                familyRegistries.computeIfAbsent(family, k -> new LinkedHashMap<>());

        if (familyRegistry.containsKey(id)) {
            throw new IllegalStateException(
                    family.getSimpleName() + " duplicado: '" + id + "'");
        }

        familyRegistry.put(id, type);
        return type;
    }

    // ── Consulta ──────────────────────────────────────────────────────────

    /**
     * Obtiene un tipo por su ID dentro de una familia específica.
     *
     * @param <T>    tipo concreto del ObjectType
     * @param family clase de la familia (ej: BulletType.class)
     * @param id     identificador del tipo
     * @return el tipo correspondiente
     * @throws IllegalArgumentException si el tipo no existe
     */
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
     * @param <T>    tipo concreto del ObjectType
     * @param family clase de la familia
     * @param id     identificador del tipo
     * @return el tipo correspondiente, o null si no existe
     */
    @SuppressWarnings("unchecked") // Cast seguro debido al uso de class token
    public static <T extends ObjectType<?>> T find(Class<T> family, String id) {
        Map<String, ObjectType<?>> familyRegistry = familyRegistries.get(family);
        if (familyRegistry == null) return null;
        return (T) familyRegistry.get(id);
    }

    /**
     * Verifica si existe un tipo con el ID dado en una familia.
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
     * @param <T>    tipo concreto del ObjectType
     * @param family clase de la familia
     * @return colección inmutable de todos los tipos registrados
     */
    @SuppressWarnings("unchecked") // Cast seguro debido al uso de class token
    public static <T extends ObjectType<?>> Collection<T> values(Class<T> family) {
        Map<String, ObjectType<?>> familyRegistry = familyRegistries.get(family);
        if (familyRegistry == null) return Collections.emptyList();

        List<T> result = new ArrayList<>();
        for (ObjectType<?> type : familyRegistry.values()) {
            result.add((T) type);
        }
        return Collections.unmodifiableList(result);
    }

    // ── Limpieza (solo para testing) ──────────────────────────────────────

    /**
     * Limpia todos los registros.
     *
     * ⚠️ SOLO PARA TESTING — NO usar en código de producción.
     */
    public static void clearAll() {
        familyRegistries.clear();
    }

    // ── Pool de oferta (lógica común) ─────────────────────────────────────

    /**
     * Construye un pool de oferta con selección ponderada por rareza.
     *
     * ALGORITMO:
     *   1. Filtra según el predicado (ej: no ya obtenidos)
     *   2. Selección ponderada por ruleta según rareza
     *   3. Evita duplicados en la misma oferta
     *
     * @param <T>      tipo concreto del ObjectType
     * @param family   clase de la familia
     * @param filter   predicado para filtrar candidatos
     * @param maxCount máximo de opciones a ofrecer
     * @param random   fuente de aleatoriedad
     * @return lista inmutable de tipos seleccionados
     */
    public static <T extends ObjectType<?>> java.util.List<T> buildOfferPool(
            Class<T> family,
            java.util.function.Predicate<T> filter,
            int maxCount,
            java.util.Random random) {

        java.util.Collection<T> all = values(family);
        java.util.List<T> candidates = all.stream()
                .filter(filter)
                .collect(java.util.stream.Collectors.toList());

        if (candidates.isEmpty()) return java.util.List.of();

        // Selección ponderada por rareza
        int totalWeight = candidates.stream()
                .mapToInt(type -> type.getRarity().weight)
                .sum();

        java.util.List<T> result = new java.util.ArrayList<>();
        java.util.Set<String> selected = new java.util.HashSet<>();

        int attempts = 0;
        while (result.size() < maxCount && result.size() < candidates.size() && attempts < 100) {
            attempts++;
            int roll = random.nextInt(totalWeight);
            int acc = 0;
            for (T type : candidates) {
                acc += type.getRarity().weight;
                if (roll < acc && !selected.contains(type.getItemId().asString())) {
                    result.add(type);
                    selected.add(type.getItemId().asString());
                    break;
                }
            }
        }
        return java.util.Collections.unmodifiableList(result);
    }
}
