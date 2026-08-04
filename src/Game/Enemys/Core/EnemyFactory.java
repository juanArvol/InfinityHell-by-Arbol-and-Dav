package Game.Enemys.Core;

import Game.Enemys.EnemyTypes.Flying.FlyingEnemyAssembler;
import Game.Enemys.EnemyTypes.Hybrid.HybridAssembler;
import Game.Enemys.EnemyTypes.Zombie.ZombieAssembler;
import Game.Engine.GameMath.Logic2D.Vector2D;

/**
 * Fábrica de enemigos — delega completamente en EnemyRegistry.
 *
 * ── HRFC-006 — EnemyFactory deja de crecer ───────────────────────────────
 * EnemyFactory ya no contiene ningún switch. Agregar un nuevo tipo de enemy
 * no requiere modificar este archivo.
 *
 * EnemyFactory únicamente:
 *   1. Pre-registra los tipos base del juego en EnemyRegistry al inicializar.
 *   2. Expone create(EnemyId, position) como API de conveniencia tipada.
 *   3. Expone create(EnemyAssembler, position) para assemblers custom.
 *
 * ── Añadir un nuevo tipo de enemy ────────────────────────────────────────
 * NO modificar este archivo. En su lugar, registrar el assembler donde
 * corresponda (inicializador de módulo de contenido, nivel, o mod):
 *
 *   EnemyRegistry.register("spinner", SpinnerAssembler::new);
 *   EnemyRegistry.register("sans",    SansAssembler::new);
 *
 * Después, crear con:
 *   EnemyRegistry.create("spinner", position);
 *
 * ── EnemyId (enum tipado) ─────────────────────────────────────────────────
 * EnemyId sigue disponible para los tipos base del juego. Su único rol es
 * proveer autocompletado e impedir typos en el código que conoce los tipos
 * de antemano. No implica ninguna jerarquía de clases.
 */
public final class EnemyFactory {

    // ── IDs de los tipos base ─────────────────────────────────────────────

    /** Identificadores de los tipos de enemy base del juego. */
    public enum EnemyId {
        ZOMBIE  ("zombie"),
        FLYING  ("flying"),
        HYBRID  ("hybrid");

        public final String registryId;

        EnemyId(String registryId) {
            this.registryId = registryId;
        }
    }

    // ── Inicialización ────────────────────────────────────────────────────

    static {
        // Registrar los tipos base en EnemyRegistry al cargar la clase.
        // Tipos adicionales (Bosses, enemigos de contenido externo) se registran
        // en sus propios módulos de inicialización.
        EnemyRegistry.register(EnemyId.ZOMBIE.registryId, ZombieAssembler::new);
        EnemyRegistry.register(EnemyId.FLYING.registryId, FlyingEnemyAssembler::new);
        EnemyRegistry.register(EnemyId.HYBRID.registryId, HybridAssembler::new);
    }

    private EnemyFactory() {}

    // ── API tipada (enum) ─────────────────────────────────────────────────

    /**
     * Crea un Enemy del tipo indicado por enum.
     *
     * Garantiza que el tipo ha sido registrado (el bloque static lo hace
     * al cargar la clase). Usar para tipos base conocidos en tiempo de compilación.
     *
     * @param id       identificador tipado del tipo de enemy.
     * @param position posición inicial en el mundo.
     * @return Enemy listo para añadir al mundo.
     */
    public static Enemy create(EnemyId id, Vector2D position) {
        return EnemyRegistry.create(id.registryId, position);
    }

    /**
     * Crea un Enemy usando un Assembler externo.
     *
     * Permite crear enemies con assemblers custom (Bosses, tests, minions con
     * configuración especial) sin necesidad de registrarlos globalmente.
     *
     * @param assembler assembler a usar.
     * @param position  posición inicial en el mundo.
     * @return Enemy listo para añadir al mundo.
     */
    public static Enemy create(EnemyAssembler assembler, Vector2D position) {
        return EnemyRegistry.create(assembler, position);
    }
}
