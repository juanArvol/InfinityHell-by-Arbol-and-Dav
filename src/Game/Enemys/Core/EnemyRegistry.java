package Game.Enemys.Core;

import Game.Engine.GameMath.SpaceLogic.Logic2D.Vector2D;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Registro de tipos de enemigos.
 *
 * ── HRFC-006 — OCP completo ───────────────────────────────────────────────
 * EnemyRegistry reemplaza el switch interno de EnemyFactory. Agregar un
 * nuevo tipo de enemy requiere únicamente:
 *
 *   1. Crear un nuevo Assembler (p.ej. SpinnerAssembler).
 *   2. Registrarlo antes de usarlo:
 *        EnemyRegistry.register("spinner", SpinnerAssembler::new);
 *
 * EnemyFactory y el Core no se modifican.
 *
 * ── Modelo de registro ────────────────────────────────────────────────────
 * Las entradas se registran por String ID. El ID es el contrato público
 * que EnemySpawner, WaveDefinition, y cualquier sistema externo usan para
 * solicitar un tipo concreto.
 *
 * Se recomienda usar IDs en snake_case y mantenerlos estables entre
 * versiones, ya que pueden almacenarse en ficheros de nivel o configuración.
 *
 * ── Supplier vs Class ─────────────────────────────────────────────────────
 * Se registra un Supplier<EnemyAssembler> en lugar de una Class para
 * soportar assemblers con dependencias o configuración (p.ej. un assembler
 * que recibe dificultad o una seed aleatoria en su constructor).
 *
 * Para assemblers sin estado (la mayoría), basta con una method reference:
 *
 *   EnemyRegistry.register("zombie", ZombieAssembler::new);
 *
 * ── Uso ───────────────────────────────────────────────────────────────────
 *   // Registro (normalmente en un inicializador de juego o módulo de contenido)
 *   EnemyRegistry.register("zombie",   ZombieAssembler::new);
 *   EnemyRegistry.register("flying",   FlyingEnemyAssembler::new);
 *   EnemyRegistry.register("sans",     SansAssembler::new);
 *
 *   // Creación
 *   Enemy e = EnemyRegistry.create("zombie", position);
 */
public final class EnemyRegistry {

    private static final Map<String, Supplier<EnemyAssembler>> registry = new HashMap<>();

    private EnemyRegistry() {}

    // ── Registro ──────────────────────────────────────────────────────────

    /**
     * Registra un tipo de enemy asociándolo a un ID.
     *
     * Si el ID ya existe, el registro anterior es sobreescrito.
     * Esto permite que mods o sistemas de contenido dinámico
     * reemplacen tipos existentes.
     *
     * @param id              identificador único del tipo (p.ej. "zombie", "sans").
     * @param assemblerSupplier fábrica que produce un assembler para ese tipo.
     */
    public static void register(String id, Supplier<EnemyAssembler> assemblerSupplier) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("EnemyRegistry: id cannot be null or blank");
        }
        if (assemblerSupplier == null) {
            throw new IllegalArgumentException("EnemyRegistry: assemblerSupplier cannot be null");
        }
        registry.put(id, assemblerSupplier);
    }

    /**
     * Elimina un tipo del registro.
     * Útil para tests o para desactivar tipos de enemigos dinámicamente.
     *
     * @param id identificador del tipo a eliminar.
     */
    public static void unregister(String id) {
        registry.remove(id);
    }

    /**
     * Devuelve true si el ID tiene un assembler registrado.
     *
     * @param id identificador a consultar.
     */
    public static boolean contains(String id) {
        return registry.containsKey(id);
    }

    // ── Creación ──────────────────────────────────────────────────────────

    /**
     * Crea un Enemy completamente ensamblado en la posición dada.
     *
     * @param id       identificador del tipo de enemy registrado.
     * @param position posición inicial en el mundo.
     * @return Enemy listo para añadir al mundo.
     * @throws IllegalArgumentException si el ID no está registrado.
     */
    public static Enemy create(String id, Vector2D position) {
        Supplier<EnemyAssembler> supplier = registry.get(id);
        if (supplier == null) {
            throw new IllegalArgumentException(
                "EnemyRegistry: no assembler registered for id='" + id + "'"
            );
        }
        return supplier.get().assemble(position);
    }

    /**
     * Crea un Enemy usando un Assembler externo sin necesidad de registrarlo.
     *
     * Útil para Bosses únicos, minions de scripting o instancias de prueba
     * que no necesitan vivir en el registro global.
     *
     * @param assembler assembler a usar.
     * @param position  posición inicial en el mundo.
     * @return Enemy listo para añadir al mundo.
     */
    public static Enemy create(EnemyAssembler assembler, Vector2D position) {
        return assembler.assemble(position);
    }
}
