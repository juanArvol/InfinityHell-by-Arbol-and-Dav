package Game.Enemys.Core;

import Game.Enemys.EnemyTypes.Flying.FlyingEnemyAssembler;
import Game.Enemys.EnemyTypes.Hybrid.HybridAssembler;
import Game.Enemys.EnemyTypes.Zombie.ZombieAssembler;
import Game.Engine.GameMath.SpaceLogic.Logic2D.Vector2D;

/**
 * Fábrica de enemigos del nuevo framework.
 *
 * ── HRFC-005 ─────────────────────────────────────────────────────────────
 * Reemplaza Game.Enemys.EnemyFactory (legacy).
 *
 * ── Diferencia con el legacy ─────────────────────────────────────────────
 * La fábrica legacy conocía cada tipo concreto (EnemyNormal, EnemyFlying)
 * y los instanciaba directamente. Agregar un nuevo tipo requería modificar
 * la fábrica — violación directa de OCP.
 *
 * La nueva fábrica solo conoce Assemblers. Agregar un nuevo tipo de enemy
 * significa crear un nuevo Assembler. La fábrica no se modifica.
 *
 * ── Punto de extensión ───────────────────────────────────────────────────
 * Para agregar un nuevo tipo (ej: Spinner):
 *   1. Crear SpinnerAssembler extends EnemyAssembler en EnemyTypes/Spinner/.
 *   2. Añadir SPINNER al enum EnemyId.
 *   3. Añadir el case en create().
 *   Solo eso. El Core no cambia.
 *
 * ── EnemyId vs EnemyType (legacy) ────────────────────────────────────────
 * EnemyType era un enum en Game.Enemys.Types que obligaba a tener subclases
 * para cada tipo. EnemyId vive en el Core y solo sirve como clave de lookup
 * para el Assembler correcto — no implica ninguna jerarquía.
 */
public final class EnemyFactory {

    private EnemyFactory() {}

    /**
     * Identificadores de tipos de enemy disponibles.
     *
     * Agregar aquí un nuevo ID no modifica ningún contrato del Core.
     */
    public enum EnemyId {
        ZOMBIE,
        FLYING,
        HYBRID
    }

    /**
     * Crea un Enemy completamente ensamblado en la posición dada.
     *
     * Delega en el Assembler correspondiente al tipo solicitado.
     * El Assembler es el único que conoce la configuración concreta.
     *
     * @param id       identificador del tipo de enemy.
     * @param position posición inicial en el mundo.
     * @return Enemy listo para añadir al mundo.
     */
    public static Enemy create(EnemyId id, Vector2D position) {
        EnemyAssembler assembler = switch (id) {
            case ZOMBIE  -> new ZombieAssembler();
            case FLYING  -> new FlyingEnemyAssembler();
            case HYBRID  -> new HybridAssembler();
        };
        return assembler.assemble(position);
    }

    /**
     * Variante de conveniencia que acepta un Assembler externo.
     *
     * Permite crear enemies con assemblers custom (tests, eventos especiales,
     * Bosses que construyen minions con configuración diferente) sin
     * necesidad de registrarlos en el enum.
     *
     * @param assembler assembler a usar.
     * @param position  posición inicial en el mundo.
     * @return Enemy listo para añadir al mundo.
     */
    public static Enemy create(EnemyAssembler assembler, Vector2D position) {
        return assembler.assemble(position);
    }
}
